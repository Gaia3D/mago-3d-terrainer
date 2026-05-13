package com.gaia3d.terrain.tile.geotiff;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.util.GaiaGeoTiffUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.imagen.media.range.NoDataContainer;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.coverage.grid.GridGeometry;
import org.geotools.api.referencing.FactoryException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.processing.Operations;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.joml.Vector2i;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class GaiaGeoTiffManager {
    private static final boolean ENABLE_RASTER_LRU_CACHE = false;
    private record PreparedCoverage(GridCoverage2D coverage, boolean disposeAfterUse, boolean allNoData) {}

    private final String PROJECTION_CRS = "EPSG:3857";
    private final double defaultNoDataValue = GlobalOptions.getInstance().getNoDataValue();
    private static final long MIN_RASTER_CACHE_BYTES = 128L * 1024L * 1024L;
    private static final long MAX_RASTER_CACHE_BYTES = 768L * 1024L * 1024L;
    private static final long MAX_SINGLE_RASTER_CACHE_BYTES = 96L * 1024L * 1024L;
    private static final double RASTER_CACHE_HEAP_RATIO = 0.15d;
    private int[] pixel = new int[1];
    private double[] originalUpperLeftCorner = new double[2];
    private Map<String, GridCoverage2D> mapPathGridCoverage2d = new HashMap<>();
    private Map<String, GeoTiffReader> mapPathGeoTiffReader = new HashMap<>();
    private Map<String, Vector2i> mapPathGridCoverage2dSize = new HashMap<>();
    private Map<String, String> mapGeoTiffToGeoTiff4326 = new HashMap<>();
    private List<String> pathList = new ArrayList<>(); // used to delete the oldest coverage
    private Map<String, RasterCacheEntry> mapPathRasterCache = new LinkedHashMap<>(16, 0.75f, true);
    private long maxRasterCacheBytes = computeRasterCacheBudgetBytes();
    private long currentRasterCacheBytes = 0L;

    {
        if (!ENABLE_RASTER_LRU_CACHE) {
            log.info("[Raster][LRU] Raster cache disabled.");
        } else {
            log.info("[Raster][LRU] Raster cache budget set to {} MB (maxHeap={} MB, ratio={}, maxSingle={} MB)",
                maxRasterCacheBytes / (1024 * 1024),
                Runtime.getRuntime().maxMemory() / (1024 * 1024),
                RASTER_CACHE_HEAP_RATIO,
                MAX_SINGLE_RASTER_CACHE_BYTES / (1024 * 1024));
        }
    }

    public GridCoverage2D loadGeoTiffGridCoverage2D(String geoTiffFilePath) {
        if (mapPathGridCoverage2d.containsKey(geoTiffFilePath)) {
            log.debug("ReUsing the GeoTiff coverage : {}", geoTiffFilePath);
            return mapPathGridCoverage2d.get(geoTiffFilePath);
        }

        if (mapGeoTiffToGeoTiff4326.containsKey(geoTiffFilePath)) {
            String geoTiff4326FilePath = mapGeoTiffToGeoTiff4326.get(geoTiffFilePath);
            if (mapPathGridCoverage2d.containsKey(geoTiff4326FilePath)) {
                log.debug("ReUsing the GeoTiff coverage 4326: {}", geoTiffFilePath);
                return mapPathGridCoverage2d.get(geoTiff4326FilePath);
            }
        }

        while (mapPathGridCoverage2d.size() > 4) {
            // delete the old coverage. Check the pathList. the 1rst is the oldest
            String oldestPath = pathList.get(0);
            GridCoverage2D oldestCoverage = mapPathGridCoverage2d.get(oldestPath);
            oldestCoverage.dispose(true);
            mapPathGridCoverage2d.remove(oldestPath);
            GeoTiffReader oldestReader = mapPathGeoTiffReader.remove(oldestPath);
            if (oldestReader != null) {
                oldestReader.dispose();
            }
            mapPathGridCoverage2dSize.remove(oldestPath);
            pathList.remove(0);
        }

        log.info("[Raster][I/O] loading the geoTiff file: {}", geoTiffFilePath);
        GeoTiffReader reader = null;
        GridCoverage2D coverage;
        try {
            File file = new File(geoTiffFilePath);
            reader = new GeoTiffReader(file);
            coverage = reader.read(null);
        } catch (Exception e) {
            if (reader != null) {
                reader.dispose();
            }
            log.error("Failed to read GeoTIFF file: {}", geoTiffFilePath, e);
            throw new RuntimeException("Failed to read GeoTIFF file: " + geoTiffFilePath, e);
        }

        // save the coverage
        mapPathGridCoverage2d.put(geoTiffFilePath, coverage);
        mapPathGeoTiffReader.put(geoTiffFilePath, reader);
        pathList.add(geoTiffFilePath);

        // save the width and height of the coverage
        GridGeometry gridGeometry = coverage.getGridGeometry();
        int width = gridGeometry.getGridRange().getSpan(0);
        int height = gridGeometry.getGridRange().getSpan(1);
        Vector2i size = new Vector2i(width, height);
        mapPathGridCoverage2dSize.put(geoTiffFilePath, size);

        log.debug("Loaded the geoTiff file ok");
        return coverage;
    }

    public Vector2i getGridCoverage2DSize(String geoTiffFilePath) {
        if (!mapPathGridCoverage2dSize.containsKey(geoTiffFilePath)) {
            GridCoverage2D coverage = loadGeoTiffGridCoverage2D(geoTiffFilePath);
            //coverage.dispose(true);
        }
        return mapPathGridCoverage2dSize.get(geoTiffFilePath);
    }

    public boolean hasCachedRaster(String geoTiffFilePath) {
        if (!ENABLE_RASTER_LRU_CACHE) {
            return false;
        }
        return mapPathRasterCache.containsKey(geoTiffFilePath);
    }

    public Raster loadGeoTiffRaster(String geoTiffFilePath) {
        if (ENABLE_RASTER_LRU_CACHE) {
            RasterCacheEntry cacheEntry = mapPathRasterCache.get(geoTiffFilePath);
            if (cacheEntry != null) {
                return cacheEntry.raster();
            }
        }

        GridCoverage2D coverage = loadGeoTiffGridCoverage2D(geoTiffFilePath);
        Raster raster = coverage.getRenderedImage().getData();
        if (ENABLE_RASTER_LRU_CACHE) {
            cacheRaster(geoTiffFilePath, raster);
        }
        return raster;
    }

    public void deleteObjects() {
        for (GridCoverage2D coverage : mapPathGridCoverage2d.values()) {
            coverage.dispose(true);
        }
        for (GeoTiffReader reader : mapPathGeoTiffReader.values()) {
            if (reader != null) {
                reader.dispose();
            }
        }
        mapPathGridCoverage2d.clear();
        mapPathGeoTiffReader.clear();
        clearRasterCache();

    }

    public void clear() {
        for (GridCoverage2D c : mapPathGridCoverage2d.values()) {
            if (c != null) {c.dispose(true);}
        }
        for (GeoTiffReader reader : mapPathGeoTiffReader.values()) {
            if (reader != null) {
                reader.dispose();
            }
        }
        mapPathGridCoverage2d.clear();
        mapPathGeoTiffReader.clear();
        mapPathGridCoverage2dSize.clear();
        pathList.clear();
        clearRasterCache();
    }

    public GridCoverage2D getResizedCoverage2D(String geoTiffFilePath, GridCoverage2D originalCoverage, double desiredPixelSizeXinMeters, double desiredPixelSizeYinMeters) throws FactoryException {
        GridCoverage2D resizedCoverage = null;

        GridGeometry originalGridGeometry = originalCoverage.getGridGeometry();
        ReferencedEnvelope envelopeOriginal = originalCoverage.getEnvelope2D();

        int gridSpanX = originalGridGeometry.getGridRange().getSpan(0); // num of pixels
        int gridSpanY = originalGridGeometry.getGridRange().getSpan(1); // num of pixels
        double[] envelopeSpanMeters = new double[2];
        GaiaGeoTiffUtils.getEnvelopeSpanInMetersOfGridCoverage2D(originalCoverage, envelopeSpanMeters);
        double envelopeSpanX = envelopeSpanMeters[0]; // in meters
        double envelopeSpanY = envelopeSpanMeters[1]; // in meters

        double desiredPixelsCountX = envelopeSpanX / desiredPixelSizeXinMeters;
        double desiredPixelsCountY = envelopeSpanY / desiredPixelSizeYinMeters;
        int minXSize = 24;
        int minYSize = 24;
        int desiredImageWidth = Math.max((int) desiredPixelsCountX, minXSize);
        int desiredImageHeight = Math.max((int) desiredPixelsCountY, minYSize);

        resizedCoverage = readSubsampledCoverage(geoTiffFilePath, envelopeOriginal, desiredImageWidth, desiredImageHeight);
        if (resizedCoverage != null) {
            GridGeometry resizedGridGeometry = resizedCoverage.getGridGeometry();
            int resizedSpanX = resizedGridGeometry.getGridRange().getSpan(0);
            int resizedSpanY = resizedGridGeometry.getGridRange().getSpan(1);
            if (resizedSpanX != desiredImageWidth || resizedSpanY != desiredImageHeight) {
                double scaleX = (double) desiredImageWidth / (double) resizedSpanX;
                double scaleY = (double) desiredImageHeight / (double) resizedSpanY;
                Operations ops = new Operations(null);
                GridCoverage2D exactSizedCoverage = (GridCoverage2D) ops.scale(resizedCoverage, scaleX, scaleY, 0, 0);
                resizedCoverage.dispose(true);
                resizedCoverage = exactSizedCoverage;
            }
        } else {
            double scaleX = (double) desiredImageWidth / (double) gridSpanX;
            double scaleY = (double) desiredImageHeight / (double) gridSpanY;

            Operations ops = new Operations(null);
            resizedCoverage = (GridCoverage2D) ops.scale(originalCoverage, scaleX, scaleY, 0, 0);
        }

        originalUpperLeftCorner[0] = envelopeOriginal.getMinimum(0);
        originalUpperLeftCorner[1] = envelopeOriginal.getMinimum(1);

        return resizedCoverage;
    }

    public void saveGridCoverage2D(GridCoverage2D coverage, String outputFilePath) throws IOException {
        // now save the newCoverage as geotiff
        File outputFile = new File(outputFilePath);
        PreparedCoverage preparedCoverage = prepareCoverageForWrite(coverage);
        GridCoverage2D materializedCoverage = preparedCoverage.coverage();
        try {
            if (preparedCoverage.allNoData()) {
                log.info("[Raster][I/O] Skip all-noData resize output: {}", outputFilePath);
                return;
            }
            try (FileOutputStream outputStream = new FileOutputStream(outputFile);
                 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
                GeoTiffWriter writer = new GeoTiffWriter(bufferedOutputStream);
                try {
                    writer.write(materializedCoverage, createWriteParameters(materializedCoverage));
                    outputStream.flush();
                } finally {
                    writer.dispose();
                }
            }
        } finally {
            if (preparedCoverage.disposeAfterUse()) {
                materializedCoverage.dispose(true);
            }
        }
    }

    private PreparedCoverage prepareCoverageForWrite(GridCoverage2D coverage) {
        RenderedImage renderedImage = coverage.getRenderedImage();
        double noDataValue = resolveNoDataValue(coverage);
        if (renderedImage instanceof WritableRenderedImage) {
            return new PreparedCoverage(coverage, false, isAllNoData(renderedImage.getData(), noDataValue));
        }

        RasterCopyResult copyResult = copyZeroBasedRasterAndCheckNoData(renderedImage.getData(), noDataValue);
        GridCoverage2D materializedCoverage = new GridCoverageFactory().create(coverage.getName(), copyResult.raster(), coverage.getEnvelope());
        return new PreparedCoverage(materializedCoverage, true, copyResult.allNoData());
    }

    private GridCoverage2D materializeCoverage(GridCoverage2D coverage) {
        RenderedImage renderedImage = coverage.getRenderedImage();
        if (renderedImage instanceof WritableRenderedImage) {
            return coverage;
        }

        Raster sourceRaster = renderedImage.getData();
        WritableRaster materializedRaster = createZeroBasedWritableRaster(sourceRaster);
        return new GridCoverageFactory().create(coverage.getName(), materializedRaster, coverage.getEnvelope());
    }

    private GeneralParameterValue[] createWriteParameters(GridCoverage2D coverage) {
        GeoTiffWriteParams writeParams = new GeoTiffWriteParams();
        writeParams.setTilingMode(GeoTiffWriteParams.MODE_EXPLICIT);

        RenderedImage renderedImage = coverage.getRenderedImage();
        int tileWidth = Math.min(Math.max(renderedImage.getWidth(), 1), 512);
        int tileHeight = Math.min(Math.max(renderedImage.getHeight(), 1), 512);
        writeParams.setTiling(tileWidth, tileHeight);

        GeoTiffFormat format = new GeoTiffFormat();
        ParameterValueGroup params = format.getWriteParameters();
        params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(writeParams);
        return params.values().toArray(new GeneralParameterValue[0]);
    }

    private GridCoverage2D readSubsampledCoverage(String geoTiffFilePath, ReferencedEnvelope envelope, int width, int height) {
        File geoTiffFile = new File(geoTiffFilePath);
        try (ImageInputStream inputStream = ImageIO.createImageInputStream(geoTiffFile)) {
            if (inputStream == null) {
                return null;
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(inputStream);
            if (!readers.hasNext()) {
                return null;
            }

            ImageReader imageReader = readers.next();
            try {
                imageReader.setInput(inputStream, true, true);
                int sourceWidth = imageReader.getWidth(0);
                int sourceHeight = imageReader.getHeight(0);
                int subsamplingX = Math.max(1, (int) Math.floor((double) sourceWidth / (double) width));
                int subsamplingY = Math.max(1, (int) Math.floor((double) sourceHeight / (double) height));

                if (subsamplingX == 1 && subsamplingY == 1) {
                    return null;
                }

                ImageReadParam readParam = imageReader.getDefaultReadParam();
                readParam.setSourceSubsampling(subsamplingX, subsamplingY, 0, 0);
                RenderedImage subsampledImage = imageReader.read(0, readParam);
                return new GridCoverageFactory().create(geoTiffFile.getName(), subsampledImage, envelope);
            } finally {
                imageReader.dispose();
            }
        } catch (Exception e) {
            log.debug("Falling back to scale-based resize for {}.", geoTiffFilePath, e);
            return null;
        }
    }

    private boolean isAllNoData(GridCoverage2D coverage) {
        Raster raster = coverage.getRenderedImage().getData();
        double noDataValue = resolveNoDataValue(coverage);
        return isAllNoData(raster, noDataValue);
    }

    private boolean isAllNoData(Raster raster, double noDataValue) {
        int minX = raster.getMinX();
        int minY = raster.getMinY();
        int maxX = minX + raster.getWidth();
        int maxY = minY + raster.getHeight();
        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                double value = raster.getSampleDouble(x, y, 0);
                if (!isNoDataValue(value, noDataValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    private double resolveNoDataValue(GridCoverage2D coverage) {
        NoDataContainer noDataContainer = CoverageUtilities.getNoDataProperty(coverage);
        return noDataContainer != null ? noDataContainer.getAsSingleValue() : defaultNoDataValue;
    }

    private RasterCopyResult copyZeroBasedRasterAndCheckNoData(Raster sourceRaster, double noDataValue) {
        Raster translatedRaster = sourceRaster.createTranslatedChild(0, 0);
        WritableRaster materializedRaster = translatedRaster.createCompatibleWritableRaster(translatedRaster.getWidth(), translatedRaster.getHeight());
        boolean allNoData = true;
        int width = translatedRaster.getWidth();
        int height = translatedRaster.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double value = translatedRaster.getSampleDouble(x, y, 0);
                materializedRaster.setSample(x, y, 0, value);
                if (allNoData && !isNoDataValue(value, noDataValue)) {
                    allNoData = false;
                }
            }
        }
        return new RasterCopyResult(materializedRaster, allNoData);
    }

    private boolean isNoDataValue(double value, double noDataValue) {
        return Double.isNaN(value) || Double.compare(value, noDataValue) == 0;
    }

    private WritableRaster createZeroBasedWritableRaster(Raster sourceRaster) {
        Raster translatedRaster = sourceRaster.createTranslatedChild(0, 0);
        WritableRaster materializedRaster = translatedRaster.createCompatibleWritableRaster(translatedRaster.getWidth(), translatedRaster.getHeight());
        materializedRaster.setRect(translatedRaster);
        return materializedRaster;
    }

    private record RasterCopyResult(WritableRaster raster, boolean allNoData) {}

    private void cacheRaster(String geoTiffFilePath, Raster raster) {
        if (!ENABLE_RASTER_LRU_CACHE) {
            return;
        }

        long rasterBytes = estimateRasterBytes(raster);
        if (rasterBytes <= 0L) {
            return;
        }

        if (rasterBytes > MAX_SINGLE_RASTER_CACHE_BYTES) {
            log.info("[Raster][LRU] Skip caching large raster: {} (~{} MB > {} MB)",
                geoTiffFilePath,
                rasterBytes / (1024 * 1024),
                MAX_SINGLE_RASTER_CACHE_BYTES / (1024 * 1024));
            return;
        }

        while (!mapPathRasterCache.isEmpty() && currentRasterCacheBytes + rasterBytes > maxRasterCacheBytes) {
            String eldestPath = mapPathRasterCache.keySet().iterator().next();
            RasterCacheEntry removed = mapPathRasterCache.remove(eldestPath);
            if (removed != null) {
                currentRasterCacheBytes -= removed.bytes();
                log.debug("[Raster][LRU] Evicted cached raster: {}", eldestPath);
            }
        }

        mapPathRasterCache.put(geoTiffFilePath, new RasterCacheEntry(raster, rasterBytes));
        currentRasterCacheBytes += rasterBytes;
        log.debug("[Raster][LRU] Cached raster: {} (~{} MB, cache usage {} MB)",
            geoTiffFilePath,
            rasterBytes / (1024 * 1024),
            currentRasterCacheBytes / (1024 * 1024));
    }

    private long estimateRasterBytes(Raster raster) {
        if (raster == null) {
            return 0L;
        }
        int dataType = raster.getSampleModel().getDataType();
        int bytesPerSample = switch (dataType) {
            case java.awt.image.DataBuffer.TYPE_BYTE -> 1;
            case java.awt.image.DataBuffer.TYPE_SHORT, java.awt.image.DataBuffer.TYPE_USHORT -> 2;
            case java.awt.image.DataBuffer.TYPE_INT, java.awt.image.DataBuffer.TYPE_FLOAT -> 4;
            case java.awt.image.DataBuffer.TYPE_DOUBLE -> 8;
            default -> 4;
        };
        return (long) raster.getWidth() * raster.getHeight() * raster.getNumBands() * bytesPerSample;
    }

    private void clearRasterCache() {
        if (!ENABLE_RASTER_LRU_CACHE) {
            mapPathRasterCache.clear();
            currentRasterCacheBytes = 0L;
            return;
        }
        mapPathRasterCache.clear();
        currentRasterCacheBytes = 0L;
    }

    private long computeRasterCacheBudgetBytes() {
        long maxHeapBytes = Runtime.getRuntime().maxMemory();
        long desiredBytes = (long) (maxHeapBytes * RASTER_CACHE_HEAP_RATIO);
        return Math.min(MAX_RASTER_CACHE_BYTES, Math.max(MIN_RASTER_CACHE_BYTES, desiredBytes));
    }

    private record RasterCacheEntry(Raster raster, long bytes) {}
}
