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
    private final String PROJECTION_CRS = "EPSG:3857";
    private final double defaultNoDataValue = GlobalOptions.getInstance().getNoDataValue();
    private int[] pixel = new int[1];
    private double[] originalUpperLeftCorner = new double[2];
    private Map<String, GridCoverage2D> mapPathGridCoverage2d = new HashMap<>();
    private Map<String, Vector2i> mapPathGridCoverage2dSize = new HashMap<>();
    private Map<String, String> mapGeoTiffToGeoTiff4326 = new HashMap<>();
    private List<String> pathList = new ArrayList<>(); // used to delete the oldest coverage

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
            log.error("Failed to read GeoTIFF file: {}", geoTiffFilePath, e);
            throw new RuntimeException("Failed to read GeoTIFF file: " + geoTiffFilePath, e);
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }

        // save the coverage
        mapPathGridCoverage2d.put(geoTiffFilePath, coverage);
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

    public void deleteObjects() {
        for (GridCoverage2D coverage : mapPathGridCoverage2d.values()) {
            coverage.dispose(true);
        }
        mapPathGridCoverage2d.clear();

    }

    public void clear() {
        for (GridCoverage2D c : mapPathGridCoverage2d.values()) {
            if (c != null) {c.dispose(true);}
        }
        mapPathGridCoverage2d.clear();
        mapPathGridCoverage2dSize.clear();
        pathList.clear();
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
        GridCoverage2D materializedCoverage = materializeCoverage(coverage);
        try {
            if (isAllNoData(materializedCoverage)) {
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
            if (materializedCoverage != coverage) {
                materializedCoverage.dispose(true);
            }
        }
    }

    private GridCoverage2D materializeCoverage(GridCoverage2D coverage) {
        RenderedImage renderedImage = coverage.getRenderedImage();
        if (renderedImage instanceof WritableRenderedImage) {
            return coverage;
        }

        Raster sourceRaster = renderedImage.getData();
        WritableRaster materializedRaster = sourceRaster.createCompatibleWritableRaster();
        materializedRaster.setRect(sourceRaster);
        return new GridCoverageFactory().create(coverage.getName(), materializedRaster, coverage.getEnvelope());
    }

    private GeneralParameterValue[] createWriteParameters(GridCoverage2D coverage) {
        GeoTiffWriteParams writeParams = new GeoTiffWriteParams();
        writeParams.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
        writeParams.setCompressionType("Deflate");
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
        NoDataContainer noDataContainer = CoverageUtilities.getNoDataProperty(coverage);
        double noDataValue = noDataContainer != null ? noDataContainer.getAsSingleValue() : defaultNoDataValue;
        int minX = raster.getMinX();
        int minY = raster.getMinY();
        int maxX = minX + raster.getWidth();
        int maxY = minY + raster.getHeight();
        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                double value = raster.getSampleDouble(x, y, 0);
                if (!Double.isNaN(value) && Double.compare(value, noDataValue) != 0) {
                    return false;
                }
            }
        }
        return true;
    }
}
