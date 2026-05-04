package com.gaia3d.terrain.tile.geotiff;

import com.gaia3d.command.GlobalOptions;
import org.eclipse.imagen.RasterFactory;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RasterStandardizerTest {

    @Test
    @Tag("default")
    void processTilesCountsExpectedTilesAndNames() throws Exception {
        RasterStandardizer rasterStandardizer = new RasterStandardizer();
        GridCoverage2D coverage = createCoverage("dem", 8, 6);

        List<String> tileNames = new ArrayList<>();
        rasterStandardizer.processTiles(coverage, 4, (tileName, tileEnvelope) -> {
            tileNames.add(tileName);
            assertTrue(tileEnvelope.getWidth() > 0.0);
            assertTrue(tileEnvelope.getHeight() > 0.0);
        });

        assertEquals(4, rasterStandardizer.countTiles(coverage, 4));
        assertEquals(List.of("dem-0-0", "dem-0-1", "dem-1-0", "dem-1-1"), tileNames);

        coverage.dispose(true);
    }

    @Test
    @Tag("default")
    void standardizeWritesOneFilePerTile() throws Exception {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setOutputCRS(DefaultGeographicCRS.WGS84);
        globalOptions.setMaxRasterSize(4);

        RasterStandardizer rasterStandardizer = new RasterStandardizer();
        GridCoverage2D coverage = createCoverage("dem", 8, 8);
        Path tempDir = Files.createTempDirectory("raster-standardize-");

        try {
            rasterStandardizer.standardize(coverage, tempDir.toFile());

            long tifFileCount;
            try (var paths = Files.list(tempDir)) {
                tifFileCount = paths.filter(path -> path.getFileName().toString().endsWith(".tif")).count();
            }

            assertEquals(4, tifFileCount);
        } finally {
            coverage.dispose(true);
            deleteRecursively(tempDir);
        }
    }

    @Test
    @Tag("default")
    void standardizeWithGeoidWritesAdjustedRaster() throws Exception {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setOutputCRS(DefaultGeographicCRS.WGS84);
        globalOptions.setMaxRasterSize(8);
        globalOptions.setNoDataValue(-9999.0);

        RasterStandardizer rasterStandardizer = new RasterStandardizer();
        GridCoverage2D demCoverage = createCoverage("dem", new float[][]{
                {1.0f, 2.0f},
                {3.0f, 4.0f}
        });
        GridCoverage2D geoidCoverage = createCoverage("geoid", new float[][]{
                {100.0f, 100.0f},
                {100.0f, 100.0f}
        });
        Path tempDir = Files.createTempDirectory("raster-standardize-geoid-");

        try {
            Path geoidFile = tempDir.resolve("geoid.tif");
            rasterStandardizer.writeGeotiff(geoidCoverage, geoidFile.toFile());
            rasterStandardizer.standardizeWithGeoid(demCoverage, tempDir.toFile(), geoidFile.toFile());

            Path outputTile;
            try (var paths = Files.list(tempDir)) {
                outputTile = paths
                        .filter(path -> path.getFileName().toString().endsWith(".tif"))
                        .filter(path -> !path.getFileName().toString().equals("geoid.tif"))
                        .findFirst()
                        .orElseThrow();
            }

            GeoTiffReader reader = new GeoTiffReader(outputTile.toFile());
            try {
                GridCoverage2D outputCoverage = reader.read(null);
                double sample = outputCoverage.getRenderedImage().getData().getSampleDouble(0, 0, 0);
                assertEquals(101.0, sample, 0.0001);
                outputCoverage.dispose(true);
            } finally {
                reader.dispose();
            }
        } finally {
            demCoverage.dispose(true);
            geoidCoverage.dispose(true);
            deleteRecursively(tempDir);
        }
    }

    @Test
    @Tag("default")
    void addGeoidPreserveDemNoDataPreservesNoDataValue() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setNoDataValue(-9999.0);

        RasterStandardizer rasterStandardizer = new RasterStandardizer();
        GridCoverage2D demCoverage = createCoverage("dem", new float[][]{
                {-9999.0f, 2.0f},
                {3.0f, 4.0f}
        });
        GridCoverage2D geoidCoverage = createCoverage("geoid", new float[][]{
                {10.0f, 10.0f},
                {10.0f, 10.0f}
        });

        GridCoverage2D adjustedCoverage = rasterStandardizer.addGeoidPreserveDemNoData(demCoverage, geoidCoverage);
        var raster = adjustedCoverage.getRenderedImage().getData();

        assertEquals(-9999.0, raster.getSampleDouble(0, 0, 0), 0.0001);
        assertEquals(12.0, raster.getSampleDouble(1, 0, 0), 0.0001);

        adjustedCoverage.dispose(true);
        demCoverage.dispose(true);
        geoidCoverage.dispose(true);
    }

    private GridCoverage2D createCoverage(String name, int width, int height) {
        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, width, height, 1, null);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.setSample(x, y, 0, x + y);
            }
        }

        ReferencedEnvelope envelope = new ReferencedEnvelope(0.0, width, 0.0, height, DefaultGeographicCRS.WGS84);
        return new GridCoverageFactory().create(name, raster, envelope);
    }

    private GridCoverage2D createCoverage(String name, float[][] values) {
        int height = values.length;
        int width = values[0].length;
        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, width, height, 1, null);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.setSample(x, y, 0, values[y][x]);
            }
        }

        ReferencedEnvelope envelope = new ReferencedEnvelope(0.0, width, 0.0, height, DefaultGeographicCRS.WGS84);
        return new GridCoverageFactory().create(name, raster, envelope);
    }

    private void deleteRecursively(Path directory) throws Exception {
        if (directory == null || Files.notExists(directory)) {
            return;
        }

        try (var paths = Files.walk(directory)) {
            paths.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                    // Best-effort cleanup for Windows file locking in GeoTools tests.
                }
            });
        }
    }
}
