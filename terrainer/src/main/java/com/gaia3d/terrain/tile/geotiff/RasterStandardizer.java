package com.gaia3d.terrain.tile.geotiff;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.tile.GaiaThreadPool;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.processing.Operation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.Interpolation;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * RasterStandardizer
 * This Class for Standardization data CRS and size.
 */
@Slf4j
@NoArgsConstructor
public class RasterStandardizer {

    private final GlobalOptions globalOptions = GlobalOptions.getInstance();

    public void standardize(GridCoverage2D source, File outputPath) {
        CoordinateReferenceSystem targetCRS = globalOptions.getTargetCRS();
        try {
            /* split */
            List<RasterInfo> splitTiles = split(source, globalOptions.getMaxRasterSize());

            /* resampling */
            ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
            List<RasterInfo> resampledTiles = pool.submit(() -> splitTiles.parallelStream().map(tile -> {
                GridCoverage2D gridCoverage2D = tile.getGridCoverage2D();
                CoordinateReferenceSystem sourceCRS = gridCoverage2D.getCoordinateReferenceSystem();
                if (isSameCRS(sourceCRS, targetCRS)) {
                    return tile;
                } else {
                    tile.setGridCoverage2D(resample(gridCoverage2D, targetCRS));
                }
                return tile;
            }
            ).collect(Collectors.toList())).get();

            /* save */
            int total = resampledTiles.size();
            AtomicInteger count = new AtomicInteger(0);
            GaiaThreadPool gaiaThreadPool = new GaiaThreadPool();
            List<Runnable> tasks = resampledTiles.stream().map(tile -> (Runnable) () -> {
                int index = count.incrementAndGet();
                GridCoverage2D reprojectedTile = tile.getGridCoverage2D();
                File tileFile = new File(outputPath, tile.getName() + ".tif");

                log.info("[Pre][Standardization][{}/{}] : {}", index, total, tileFile.getAbsolutePath());
                writeGeotiff(reprojectedTile, tileFile);
            }).collect(Collectors.toList());
            gaiaThreadPool.execute(tasks);
        } catch (TransformException | IOException | InterruptedException | ExecutionException e) {
            log.error("Failed to standardization.", e);
            throw new RuntimeException(e);
        }
    }

    public GridCoverage2D readGeoTiff(File file) {
        try {
            GeoTiffReader reader = new GeoTiffReader(file);
            return reader.read(null);
        } catch (Exception e) {
            log.error("Failed to read GeoTiff file : {}", file.getAbsolutePath());
            log.error("Error : ", e);
            throw new RuntimeException(e);
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
            writer.dispose();
            outputStream.close();
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

    public List<RasterInfo> split(GridCoverage2D coverage, int tileSize) throws TransformException, IOException {
        List<RasterInfo> tiles = new ArrayList<>();

        GridGeometry2D gridGeometry = coverage.getGridGeometry();
        GridEnvelope gridRange = gridGeometry.getGridRange();
        int width = gridRange.getSpan(0);
        int height = gridRange.getSpan(1);

        int margin = 4; // 4 pixel margin
        for (int x = 0; x < width; x += tileSize) {
            for (int y = 0; y < height; y += tileSize) {
                int xMax = Math.min(x + tileSize, width);
                int yMax = Math.min(y + tileSize, height);

                // when the tile is not at the edge, add margin
                if ((x + tileSize) < width) {
                    xMax += margin;
                }
                if ((y + tileSize) < height) {
                    yMax += margin;
                }

                ReferencedEnvelope tileEnvelope = new ReferencedEnvelope(gridGeometry.gridToWorld(new GridEnvelope2D(x, y, xMax - x, yMax - y)), coverage.getCoordinateReferenceSystem());
                GridCoverage2D gridCoverage2D = crop(coverage, tileEnvelope);
                String tileName = gridCoverage2D.getName() + "-" + x / tileSize + "-" + y / tileSize;
                RasterInfo tile = new RasterInfo(tileName, gridCoverage2D);
                tiles.add(tile);
            }
        }
        return tiles;
    }

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

    public GridCoverage2D resample(GridCoverage2D sourceCoverage, CoordinateReferenceSystem targetCRS) {
        try {
            CoverageProcessor processor = CoverageProcessor.getInstance();
            Operation operation = processor.getOperation("Resample");

            ParameterValueGroup params = operation.getParameters();
            params.parameter("Source").setValue(sourceCoverage);
            params.parameter("CoordinateReferenceSystem").setValue(targetCRS);
            params.parameter("InterpolationType").setValue(Interpolation.getInstance(Interpolation.INTERP_NEAREST)); // INTERP_BILINEAR

            return (GridCoverage2D) processor.doOperation(params);
        } catch (Exception e) {
            log.error("Failed to reproject tile : {}", sourceCoverage.getName());
            log.error("Error : ", e);
            return sourceCoverage;
        }
    }

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
