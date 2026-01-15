package com.gaia3d.terrain.tile.geotiff;

import com.gaia3d.command.GlobalOptions;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.imagen.Interpolation;
import org.eclipse.imagen.RasterFactory;
import org.eclipse.imagen.media.range.NoDataContainer;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.coverage.processing.Operation;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.ReferenceIdentifier;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.Operations;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;

import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RasterStandardizer
 * This Class for Standardization data CRS and size.
 */
@Slf4j
@NoArgsConstructor
public class RasterStandardizer {

    static {
        /*JAIExt.registerJAIDescriptor("Warp");
        JAIExt.registerJAIDescriptor("Affine");
        JAIExt.registerJAIDescriptor("Rescale");
        JAIExt.registerJAIDescriptor("Warp/Affine");
        JAIExt.initJAIEXT();

        JAI jaiInstance = JAI.getDefaultInstance();
        TileCache tileCache = jaiInstance.getTileCache();
        tileCache.setMemoryCapacity(1024 * 1024 * 1024);
        tileCache.setMemoryThreshold(0.75f);

        TileScheduler tileScheduler = jaiInstance.getTileScheduler();
        // availableProcessors = Runtime.getRuntime().availableProcessors();
        tileScheduler.setParallelism(Runtime.getRuntime().availableProcessors());
        tileScheduler.setPriority(Thread.NORM_PRIORITY);*/
    }

    private final GlobalOptions globalOptions = GlobalOptions.getInstance();

    public void standardize(GridCoverage2D source, File outputPath) {
        CoordinateReferenceSystem targetCRS = globalOptions.getOutputCRS();
        try {
            /* split */
            log.info("[Pre][Standardization] Splitting source raster into tiles... {}", outputPath.getName());
            List<RasterInfo> splitTiles = split(source, globalOptions.getMaxRasterSize());
            log.info("[Pre][Standardization] Splitting completed. Total tiles: {}", splitTiles.size());

            /* resampling */
            int total = splitTiles.size();
            AtomicInteger count = new AtomicInteger(0);
            splitTiles.forEach(tile -> {
                log.info("[Pre][Standardization][{}/{}] Resampling tile {}", count.incrementAndGet(), total, tile.getName());

                GridCoverage2D gridCoverage2D = tile.getGridCoverage2D();
                CoordinateReferenceSystem sourceCRS = gridCoverage2D.getCoordinateReferenceSystem();
                GridCoverage2D resampledGridCoverage2D;
                if (isSameCRS(sourceCRS, targetCRS)) {
                    resampledGridCoverage2D = gridCoverage2D;
                } else {
                    resampledGridCoverage2D = resample(gridCoverage2D, targetCRS);
                }
                tile.setGridCoverage2D(resampledGridCoverage2D);
                File tileFile = new File(outputPath, tile.getName() + ".tif");
                writeGeotiff(tile.getGridCoverage2D(), tileFile);

                resampledGridCoverage2D.dispose(true);
                tile.getGridCoverage2D().dispose(true);
                tile.setGridCoverage2D(null);
                log.info("[Pre][Standardization][{}/{}] Completed tile {}", count.get(), total, tile.getName());
            });
        } catch (TransformException | IOException e) {
            log.error("Failed to standardization.", e);
            throw new RuntimeException(e);
        }
    }

    public void standardizeWithGeoid(GridCoverage2D source, File outputPath, File geoidFile) {
        // load geoid data

        GeoTiffReader reader = null;
        try {
            reader = new GeoTiffReader(geoidFile);
            GridCoverage2D geoidCoverage = reader.read(null);

            //GridCoverage2D geoidCoverage = readGeoTiff(geoidFile);
            CoordinateReferenceSystem targetCRS = globalOptions.getOutputCRS();
            try {
                /* split */
                log.info("[Pre][Standardization][with Geoid] Splitting source raster into tiles... {}", outputPath.getName());
                List<RasterInfo> splitTiles = split(source, globalOptions.getMaxRasterSize());
                log.info("[Pre][Standardization][with Geoid] Splitting completed. Total tiles: {}", splitTiles.size());

                int total = splitTiles.size();
                AtomicInteger count = new AtomicInteger(0);

                /* resampling */
                splitTiles.forEach(tile -> {
                    log.info("[Pre][Standardization][with Geoid][{}/{}] Resampling tile {}", count.incrementAndGet(), total, tile.getName());

                    GridCoverage2D gridCoverage = tile.getGridCoverage2D();
                    CoordinateReferenceSystem sourceCRS = gridCoverage.getCoordinateReferenceSystem();
                    GridCoverage2D resampledGridCoverage2D;
                    if (isSameCRS(sourceCRS, targetCRS)) {
                        resampledGridCoverage2D = gridCoverage;
                    } else {
                        resampledGridCoverage2D = resample(gridCoverage, targetCRS);
                    }
                    tile.setGridCoverage2D(resampledGridCoverage2D);

                    GridGeometry2D demGrid = resampledGridCoverage2D.getGridGeometry();
                    GridCoverage2D geoidAligned = resampleGeoid(geoidCoverage, demGrid, demGrid.getCoordinateReferenceSystem());
                    GridCoverage2D ellipsoidalDem = addGeoidPreserveDemNoData(gridCoverage, geoidAligned);
                    tile.setGridCoverage2D(ellipsoidalDem);

                    GridCoverage2D reprojectedTile = tile.getGridCoverage2D();
                    File tileFile = new File(outputPath, tile.getName() + ".tif");
                    writeGeotiff(reprojectedTile, tileFile);

                    reprojectedTile.dispose(true);
                    geoidAligned.dispose(true);
                    ellipsoidalDem.dispose(true);
                    gridCoverage.dispose(true);
                    tile.getGridCoverage2D().dispose(true);
                    tile.setGridCoverage2D(null);
                    log.info("[Pre][Standardization][with Geoid][{}/{}] Completed tile {}", count.get(), total, tile.getName());
                });
            } catch (TransformException | IOException e) {
                throw new RuntimeException(e);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
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
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            GeoTiffWriter writer = new GeoTiffWriter(bufferedOutputStream);
            writer.write(coverage, null);
            outputStream.flush();
            outputStream.close();
            writer.dispose();
        } catch (Exception e) {
            log.error("Failed to write GeoTiff file : {}", outputFile.getAbsolutePath());
            log.error("Error : ", e);
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

    /**
     * Split GridCoverage2D into tiles with tileSize
     * @param coverage source GridCoverage2D
     * @param tileSize tile size
     * @return List<RasterInfo> tiles
     */
    public List<RasterInfo> split(GridCoverage2D coverage, int tileSize) throws TransformException, IOException {
        List<RasterInfo> tiles = new ArrayList<>();

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
                    xMax += marginX;
                }
                if ((y + tileSize) < height) {
                    yMax += marginY;
                }

                int xAux = x;
                if (xAux > marginX) {
                    xAux -= marginX;
                }

                int yAux = y;
                if (yAux > marginY) {
                    yAux -= marginY;
                }

                ReferencedEnvelope tileEnvelope = new ReferencedEnvelope(gridGeometry.gridToWorld(new GridEnvelope2D(xAux, yAux, xMax - x, yMax - y)), coverage.getCoordinateReferenceSystem());
                GridCoverage2D gridCoverage2D = crop(coverage, tileEnvelope);
                String tileName = gridCoverage2D.getName() + "-" + x / tileSize + "-" + y / tileSize;
                RasterInfo tile = new RasterInfo(tileName, gridCoverage2D);
                tiles.add(tile);
            }
        }
        return tiles;
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
                double[] backgroundValues = new double[] {globalOptions.getNoDataValue()};
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
                double[] backgroundValues = new double[] {globalOptions.getNoDataValue()};
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

    /**
     * Check if two CRS are the same
     * @param sourceCRS source CoordinateReferenceSystem
     * @param targetCRS target CoordinateReferenceSystem
     * @return true if same, false otherwise
     */
    public boolean isSameCRS(CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS) {
        Iterator<ReferenceIdentifier> sourceCRSIterator = sourceCRS.getIdentifiers().iterator();
        Iterator<ReferenceIdentifier> targetCRSIterator = targetCRS.getIdentifiers().iterator();

        if (sourceCRSIterator.hasNext() && targetCRSIterator.hasNext()) {
            String sourceCRSCode = sourceCRSIterator.next().getCode();
            String targetCRSCode = targetCRSIterator.next().getCode();
            return sourceCRSCode.equals(targetCRSCode);
        } else {
            return false;
        }
    }
}
