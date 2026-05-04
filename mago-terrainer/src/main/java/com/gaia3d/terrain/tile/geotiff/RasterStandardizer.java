package com.gaia3d.terrain.tile.geotiff;

import com.gaia3d.command.GlobalOptions;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.imagen.Interpolation;
import org.eclipse.imagen.RasterFactory;
import org.eclipse.imagen.media.range.NoDataContainer;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.coverage.processing.Operation;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.ReferenceIdentifier;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.datum.Ellipsoid;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.Operations;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Standardizes raster CRS and size for terrain processing.
 */
@Slf4j
@NoArgsConstructor
public class RasterStandardizer {
    private static final int MAX_FILES_PER_DIRECTORY = 10_000;

    private final GlobalOptions globalOptions = GlobalOptions.getInstance();
    @FunctionalInterface
    interface TileProcessor {
        void process(String tileName, ReferencedEnvelope tileEnvelope) throws TransformException, IOException;
    }

    @FunctionalInterface
    private interface TileDescriptorProcessor {
        void process(TileDescriptor tileDescriptor) throws TransformException, IOException;
    }

    private record TileDescriptor(String tileName, GridEnvelope2D gridEnvelope, ReferencedEnvelope tileEnvelope) {}

    public void standardize(GridCoverage2D source, File outputPath) {
        CoordinateReferenceSystem targetCRS = globalOptions.getOutputCRS();
        try {
            log.info("[Pre][Standardization] Splitting source raster into tiles... {}", outputPath.getName());
            int total = countTiles(source, globalOptions.getMaxRasterSize());
            log.info("[Pre][Standardization] Splitting completed. Total tiles: {}", total);
            AtomicInteger count = new AtomicInteger(0);
            processTileDescriptors(source, globalOptions.getMaxRasterSize(), tileDescriptor -> {
                int current = count.incrementAndGet();
                log.info("[Pre][Standardization][{}/{}] Resampling tile {}", current, total, tileDescriptor.tileName());

                GridCoverage2D croppedTile = extractTileCoverage(source, tileDescriptor.gridEnvelope(), tileDescriptor.tileEnvelope());
                GridCoverage2D outputTile = croppedTile;
                try {
                    CoordinateReferenceSystem sourceCRS = croppedTile.getCoordinateReferenceSystem();
                    if (!isSameCRS(sourceCRS, targetCRS)) {
                        GridCoverage2D resampledTile = resample(croppedTile, targetCRS);
                        outputTile = materializeCoverage(resampledTile);
                        if (resampledTile != outputTile) {
                            disposeCoverage(resampledTile);
                        }
                    }

                    File tileFile = createOutputTileFile(outputPath, tileDescriptor.tileName(), current);
                    writeGeotiff(outputTile, tileFile);
                } finally {
                    disposeCoverage(outputTile);
                    if (outputTile != croppedTile) {
                        disposeCoverage(croppedTile);
                    }
                }
                log.info("[Pre][Standardization][{}/{}] Completed tile {}", current, total, tileDescriptor.tileName());
            });
        } catch (TransformException | IOException e) {
            log.error("Failed to standardization.", e);
            throw new RuntimeException(e);
        }
    }

    public void standardizeWithGeoid(GridCoverage2D source, File outputPath, File geoidFile) {
        // load geoid data

        GeoTiffReader reader = null;
        GridCoverage2D geoidCoverage = null;
        try {
            reader = new GeoTiffReader(geoidFile);
            geoidCoverage = reader.read(null);

            //GridCoverage2D geoidCoverage = readGeoTiff(geoidFile);
            CoordinateReferenceSystem targetCRS = globalOptions.getOutputCRS();
            try {
                final GridCoverage2D geoidCoverageRef = geoidCoverage;
                log.info("[Pre][Standardization][with Geoid] Splitting source raster into tiles... {}", outputPath.getName());
                int total = countTiles(source, globalOptions.getMaxRasterSize());
                log.info("[Pre][Standardization][with Geoid] Splitting completed. Total tiles: {}", total);
                AtomicInteger count = new AtomicInteger(0);

                processTileDescriptors(source, globalOptions.getMaxRasterSize(), tileDescriptor -> {
                    int current = count.incrementAndGet();
                    log.info("[Pre][Standardization][with Geoid][{}/{}] Resampling tile {}", current, total, tileDescriptor.tileName());

                    GridCoverage2D croppedTile = extractTileCoverage(source, tileDescriptor.gridEnvelope(), tileDescriptor.tileEnvelope());
                    GridCoverage2D resampledTile = croppedTile;
                    GridCoverage2D geoidAligned = null;
                    GridCoverage2D ellipsoidalDem = null;
                    try {
                        CoordinateReferenceSystem sourceCRS = croppedTile.getCoordinateReferenceSystem();
                        if (!isSameCRS(sourceCRS, targetCRS)) {
                            GridCoverage2D lazyResampledTile = resample(croppedTile, targetCRS);
                            resampledTile = materializeCoverage(lazyResampledTile);
                            if (lazyResampledTile != resampledTile) {
                                disposeCoverage(lazyResampledTile);
                            }
                        }

                        GridGeometry2D demGrid = resampledTile.getGridGeometry();
                        GridCoverage2D lazyGeoidAligned = resampleGeoid(geoidCoverageRef, demGrid, demGrid.getCoordinateReferenceSystem());
                        geoidAligned = materializeCoverage(lazyGeoidAligned);
                        if (lazyGeoidAligned != geoidAligned) {
                            disposeCoverage(lazyGeoidAligned);
                        }
                        ellipsoidalDem = addGeoidPreserveDemNoData(resampledTile, geoidAligned);

                        File tileFile = createOutputTileFile(outputPath, tileDescriptor.tileName(), current);
                        writeGeotiff(ellipsoidalDem, tileFile);
                    } finally {
                        disposeCoverage(ellipsoidalDem);
                        disposeCoverage(geoidAligned);
                        disposeCoverage(resampledTile);
                        if (resampledTile != croppedTile) {
                            disposeCoverage(croppedTile);
                        }
                    }
                    log.info("[Pre][Standardization][with Geoid][{}/{}] Completed tile {}", current, total, tileDescriptor.tileName());
                });
            } catch (TransformException | IOException e) {
                throw new RuntimeException(e);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            disposeCoverage(geoidCoverage);
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Exception ex) {
                    log.error("Error:", ex);
                }
            }
        }
    }

    public void writeGeotiff(GridCoverage2D coverage, File outputFile) {
        try {
            if (outputFile.exists() && outputFile.length() > 0) {
                log.info("[Raster][I/O] File already exists and not Empty : {}", outputFile.getAbsolutePath());
                return;
            }
            GridCoverage2D materializedCoverage = materializeCoverage(coverage);
            try {
                if (isAllNoData(materializedCoverage)) {
                    log.info("[Raster][I/O] Skip all-noData tile: {}", outputFile.getAbsolutePath());
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
                disposeCoverage(materializedCoverage);
            }
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("Unable to map projection")) {
                log.warn("[Raster][I/O] IAU CRS cannot be encoded in GeoTIFF. Writing with WGS84 carrier CRS: {}",
                         outputFile.getName());
                writeGeotiffWithCarrierCrs(coverage, outputFile);
            } else {
                log.error("Failed to write GeoTiff file : {}", outputFile.getAbsolutePath());
                log.error("Error : ", e);
            }
        } catch (Exception e) {
            log.error("Failed to write GeoTiff file : {}", outputFile.getAbsolutePath());
            log.error("Error : ", e);
        }
    }

    private void writeGeotiffWithCarrierCrs(GridCoverage2D coverage, File outputFile) {
        try {
            GridCoverageFactory coverageFactory = new GridCoverageFactory();
            GridCoverage2D materializedCoverage = materializeCoverage(coverage);
            ReferencedEnvelope carrierEnvelope = new ReferencedEnvelope(
                materializedCoverage.getEnvelope2D().getMinimum(0),
                materializedCoverage.getEnvelope2D().getMaximum(0),
                materializedCoverage.getEnvelope2D().getMinimum(1),
                materializedCoverage.getEnvelope2D().getMaximum(1),
                DefaultGeographicCRS.WGS84
            );
            GridCoverage2D carrierCoverage = coverageFactory.create(
                materializedCoverage.getName(),
                materializedCoverage.getRenderedImage(),
                carrierEnvelope
            );

            try (FileOutputStream outputStream = new FileOutputStream(outputFile);
                 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
                GeoTiffWriter writer = new GeoTiffWriter(bufferedOutputStream);
                try {
                    writer.write(carrierCoverage, createWriteParameters(carrierCoverage));
                    outputStream.flush();
                } finally {
                    writer.dispose();
                }
            } finally {
                carrierCoverage.dispose(true);
                materializedCoverage.dispose(true);
            }
        } catch (Exception e) {
            log.error("Failed to write GeoTiff with carrier CRS: {}", outputFile.getAbsolutePath(), e);
        }
    }

    @Deprecated
    public void getImageBuffer(GridCoverage2D coverage) {
        RenderedImage image = coverage.getRenderedImage();
        Raster raster = image.getData();
        int width = raster.getWidth();
        int height = raster.getHeight();
        float[] pixels = new float[width * height];

        int minX = raster.getMinX();
        int minY = raster.getMinY();
        raster.getPixels(minX, minY, width, height, pixels);
    }

    public RasterInfo cropTile(GridCoverage2D coverage, ReferencedEnvelope envelope, String tileName) {
        GridCoverage2D gridCoverage2D = crop(coverage, envelope);
        return new RasterInfo(tileName, gridCoverage2D);
    }

    public int countTiles(GridCoverage2D coverage, int tileSize) throws TransformException, IOException {
        AtomicInteger count = new AtomicInteger(0);
        processTileDescriptors(coverage, tileSize, tileDescriptor -> count.incrementAndGet());
        return count.get();
    }

    /**
     * Split GridCoverage2D into tiles with tileSize
     * @param coverage source GridCoverage2D
     * @param tileSize tile size
     */
    public void processTiles(GridCoverage2D coverage, int tileSize, TileProcessor processor) throws TransformException, IOException {
        processTileDescriptors(coverage, tileSize, tileDescriptor -> processor.process(tileDescriptor.tileName(), tileDescriptor.tileEnvelope()));
    }

    private void processTileDescriptors(GridCoverage2D coverage, int tileSize, TileDescriptorProcessor processor) throws TransformException, IOException {
        GridGeometry2D gridGeometry = coverage.getGridGeometry();
        GridEnvelope gridRange = gridGeometry.getGridRange();
        int width = gridRange.getSpan(0);
        int height = gridRange.getSpan(1);

        int margin = 4; // 4 pixel margin
        int marginX = Math.max((int) (tileSize * 0.01), margin);
        int marginY = Math.max((int) (tileSize * 0.01), margin);
        for (int x = 0; x < width; x += tileSize) {
            for (int y = 0; y < height; y += tileSize) {
                int xMax = Math.min(x + tileSize, width);
                int yMax = Math.min(y + tileSize, height);

                // when the tile is not at the edge, add margin
                if ((x + tileSize) < width) {
                    xMax = Math.min(xMax + marginX, width);
                }
                if ((y + tileSize) < height) {
                    yMax = Math.min(yMax + marginY, height);
                }

                int xAux = x;
                if (xAux > marginX) {
                    xAux -= marginX;
                }

                int yAux = y;
                if (yAux > marginY) {
                    yAux -= marginY;
                }

                GridEnvelope2D gridEnvelope = new GridEnvelope2D(xAux, yAux, xMax - xAux, yMax - yAux);
                ReferencedEnvelope tileEnvelope = new ReferencedEnvelope(
                        gridGeometry.gridToWorld(gridEnvelope),
                        coverage.getCoordinateReferenceSystem()
                );
                String tileName = coverage.getName() + "-" + x / tileSize + "-" + y / tileSize;
                processor.process(new TileDescriptor(tileName, gridEnvelope, tileEnvelope));
            }
        }
    }

    /**
     * Crop GridCoverage2D with envelope
     * @param coverage source GridCoverage2D
     * @param envelope crop envelope
     * @return cropped GridCoverage2D
     */
    public GridCoverage2D crop(GridCoverage2D coverage, ReferencedEnvelope envelope) {
        try {
            Operations ops = Operations.DEFAULT;
            return (GridCoverage2D) ops.crop(coverage, envelope);
        } catch (Exception e) {
            log.error("Failed to crop coverage : {}", coverage.getName());
            log.error("Error : ", e);
            throw new RuntimeException("Failed to crop coverage", e);
        }
    }

    /**
     * Reproject GridCoverage2D to targetCRS
     * @param sourceCoverage source GridCoverage2D
     * @param targetCRS target CoordinateReferenceSystem
     * @return reprojected GridCoverage2D
     */
    public GridCoverage2D resample(GridCoverage2D sourceCoverage, CoordinateReferenceSystem targetCRS) {
        try {
            CoverageProcessor.updateProcessors();
            CoverageProcessor processor = CoverageProcessor.getInstance();

            Operation operation = processor.getOperation("Resample");
            ParameterValueGroup params = operation.getParameters();
            params.parameter("Source").setValue(sourceCoverage);
            params.parameter("CoordinateReferenceSystem").setValue(targetCRS);
            params.parameter("InterpolationType").setValue(Interpolation.getInstance(Interpolation.INTERP_NEAREST)); // INTERP_BILINEAR

            NoDataContainer noDataContainer = CoverageUtilities.getNoDataProperty(sourceCoverage);
            if (noDataContainer != null) {
                double[] backgroundValues = noDataContainer.getAsArray();
                params.parameter("BackgroundValues").setValue(backgroundValues);
            } else {
                double[] backgroundValues = new double[]{globalOptions.getNoDataValue()};
                params.parameter("BackgroundValues").setValue(backgroundValues);
            }
            return (GridCoverage2D) processor.doOperation(params);
        } catch (Exception e) {
            log.error("Failed to reproject tile : {}", sourceCoverage.getName());
            log.error("Error : ", e);
            return sourceCoverage;
        }
    }

    /**
     * Reproject GridCoverage2D to targetCRS
     * @param sourceCoverage source GridCoverage2D
     * @param targetCRS target CoordinateReferenceSystem
     * @return reprojected GridCoverage2D
     */
    public GridCoverage2D resampleGeoid(GridCoverage2D sourceCoverage, GridGeometry2D gridGeometry, CoordinateReferenceSystem targetCRS) {
        try {
            CoverageProcessor.updateProcessors();
            CoverageProcessor processor = CoverageProcessor.getInstance();

            Operation operation = processor.getOperation("Resample");
            ParameterValueGroup params = operation.getParameters();
            params.parameter("Source").setValue(sourceCoverage);
            params.parameter("CoordinateReferenceSystem").setValue(targetCRS);
            params.parameter("GridGeometry").setValue(gridGeometry);
            params.parameter("InterpolationType").setValue(Interpolation.getInstance(Interpolation.INTERP_BILINEAR));

            NoDataContainer noDataContainer = CoverageUtilities.getNoDataProperty(sourceCoverage);
            if (noDataContainer != null) {
                double[] backgroundValues = noDataContainer.getAsArray();
                params.parameter("BackgroundValues").setValue(backgroundValues);
            } else {
                double[] backgroundValues = new double[]{globalOptions.getNoDataValue()};
                params.parameter("BackgroundValues").setValue(backgroundValues);
            }
            return (GridCoverage2D) processor.doOperation(params);
        } catch (Exception e) {
            log.error("Failed to reproject tile : {}", sourceCoverage.getName());
            log.error("Error : ", e);
            return sourceCoverage;
        }
    }

    /**
     * Get NoData value from GridCoverage2D
     * @param coverage GridCoverage2D
     * @return NoData value or null
     */
    public Double getNodata(GridCoverage2D coverage) {
        NoDataContainer noDataContainer = CoverageUtilities.getNoDataProperty(coverage);
        if (noDataContainer != null) {
            double[] noDataValues = noDataContainer.getAsArray();
            return noDataValues[0];
        } else {
            return null;
        }
    }

    /**
     * Add Calculate Geoid to DEM value
     * when DEM value is NoData, preserve NoData value
     * @param dem digital elevation model
     * @param alignedGeoid same grid geometry with dem
     * @return GridCoverage2D with geoid applied
     */
    public GridCoverage2D addGeoidPreserveDemNoData(GridCoverage2D dem, GridCoverage2D alignedGeoid) {
        double globalNodata = globalOptions.getNoDataValue();

        RenderedImage demImg = dem.getRenderedImage();
        RenderedImage geoidImg = alignedGeoid.getRenderedImage();

        Double demNoDataVal = getNodata(dem);
        boolean hasDemNoDataVal = demNoDataVal != null;
        double demNoData = hasDemNoDataVal ? demNoDataVal : Double.NaN;

        Raster demRaster = demImg.getData();
        Raster geoRaster = geoidImg.getData();

        Rectangle demRectangle = demRaster.getBounds();
        Rectangle geoidRectangle = geoRaster.getBounds();
        Rectangle intersection = demRectangle.intersection(geoidRectangle);

        WritableRaster outRaster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, demRectangle.width, demRectangle.height, 1, null);

        for (int y = demRectangle.y; y < demRectangle.y + demRectangle.height; y++) {
            for (int x = demRectangle.x; x < demRectangle.x + demRectangle.width; x++) {
                int outputX = x - demRectangle.x;
                int outputY = y - demRectangle.y;
                outRaster.setSample(outputX, outputY, 0, (float) demRaster.getSampleDouble(x, y, 0));
            }
        }

        for (int y = intersection.y; y < intersection.y + intersection.height; y++) {
            for (int x = intersection.x; x < intersection.x + intersection.width; x++) {
                int outputX = x - demRectangle.x;
                int outputY = y - demRectangle.y;

                double H = demRaster.getSampleDouble(x, y, 0);
                if (H <= -9999) {
                    outRaster.setSample(outputX, outputY, 0, (float) globalNodata);
                } else if (Double.isNaN(H) || (hasDemNoDataVal && Double.compare(H, demNoData) == 0)) {
                    outRaster.setSample(outputX, outputY, 0, (float) globalNodata);
                } else {
                    double N = geoRaster.getSampleDouble(x, y, 0);
                    outRaster.setSample(outputX, outputY, 0, (float) (H + N));
                }
            }
        }

        // dispose
        demRaster = null;
        geoRaster = null;

        return new GridCoverageFactory().create(dem.getName(), outRaster, dem.getEnvelope());
    }

    private void disposeCoverage(GridCoverage2D coverage) {
        if (coverage != null) {
            coverage.dispose(true);
        }
    }

    private boolean isAllNoData(GridCoverage2D coverage) {
        Raster raster = coverage.getRenderedImage().getData();
        NoDataContainer noDataContainer = CoverageUtilities.getNoDataProperty(coverage);
        double noDataValue = noDataContainer != null ? noDataContainer.getAsSingleValue() : globalOptions.getNoDataValue();
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

    private GridCoverage2D extractTileCoverage(GridCoverage2D sourceCoverage, GridEnvelope2D gridEnvelope, ReferencedEnvelope tileEnvelope) {
        Raster sourceRaster = sourceCoverage.getRenderedImage().getData(gridEnvelope);
        WritableRaster materializedRaster = sourceRaster.createCompatibleWritableRaster();
        materializedRaster.setRect(sourceRaster);
        return new GridCoverageFactory().create(sourceCoverage.getName(), materializedRaster, tileEnvelope);
    }

    private File createOutputTileFile(File sourceOutputDirectory, String tileName, int tileIndex) {
        File shardDirectory = new File(sourceOutputDirectory, shardDirectoryName(tileIndex));
        if (!shardDirectory.exists() && !shardDirectory.mkdirs()) {
            throw new RuntimeException("Failed to create shard directory: " + shardDirectory.getAbsolutePath());
        }
        String uniqueTileName = tileName + "-" + UUID.randomUUID();
        return new File(shardDirectory, uniqueTileName + ".tif");
    }

    public static String sourceDirectoryName(String sourceIdentifier) {
        String fileName = new File(sourceIdentifier).getName();
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
        String sanitized = sanitizePathSegment(baseName);
        String suffix = Integer.toHexString(sourceIdentifier.hashCode());
        return sanitized + "-" + suffix;
    }

    public static String sanitizePathSegment(String value) {
        String sanitized = value.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (sanitized.isBlank()) {
            return "raster";
        }
        return sanitized;
    }

    private static String shardDirectoryName(int itemIndex) {
        int shardIndex = Math.max(0, (itemIndex - 1) / MAX_FILES_PER_DIRECTORY);
        int shardStart = shardIndex * MAX_FILES_PER_DIRECTORY;
        int shardEnd = shardStart + MAX_FILES_PER_DIRECTORY - 1;
        return String.format("batch-%05d-%05d", shardStart, shardEnd);
    }

    /**
     * Check if two CRS are the same
     * @param sourceCRS source CoordinateReferenceSystem
     * @param targetCRS target CoordinateReferenceSystem
     * @return true if same, false otherwise
     */
    public boolean isSameCRS(CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS) {
        // Try identifier-based comparison first (fast path)
        Iterator<ReferenceIdentifier> sourceCRSIterator = sourceCRS.getIdentifiers().iterator();
        Iterator<ReferenceIdentifier> targetCRSIterator = targetCRS.getIdentifiers().iterator();

        if (sourceCRSIterator.hasNext() && targetCRSIterator.hasNext()) {
            String sourceCRSCode = sourceCRSIterator.next().getCode();
            String targetCRSCode = targetCRSIterator.next().getCode();
            if (sourceCRSCode.equals(targetCRSCode)) {
                return true;
            }
        }
        // Fallback: metadata-based comparison (handles IAU CRS and other edge cases)
        if (CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
            return true;
        }
        // Handle unknown source CRS (e.g., lunar GeoTIFFs without IAU authority):
        // If the source has no identifiers but both are geographic CRS with matching ellipsoids,
        // treat them as equivalent — the data is already in the correct coordinate space.
        if (!sourceCRSIterator.hasNext() && sourceCRS instanceof GeographicCRS && targetCRS instanceof GeographicCRS) {
            Ellipsoid sourceEllipsoid = ((GeographicCRS) sourceCRS).getDatum().getEllipsoid();
            Ellipsoid targetEllipsoid = ((GeographicCRS) targetCRS).getDatum().getEllipsoid();
            double sourceSemiMajor = sourceEllipsoid.getSemiMajorAxis();
            double targetSemiMajor = targetEllipsoid.getSemiMajorAxis();
            double sourceSemiMinor = sourceEllipsoid.getSemiMinorAxis();
            double targetSemiMinor = targetEllipsoid.getSemiMinorAxis();
            if (Math.abs(sourceSemiMajor - targetSemiMajor) < 1.0 && Math.abs(sourceSemiMinor - targetSemiMinor) < 1.0) {
                return true;
            }
        }
        return false;
    }
}
