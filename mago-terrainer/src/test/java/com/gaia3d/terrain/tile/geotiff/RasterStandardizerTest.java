package com.gaia3d.terrain.tile.geotiff;

import com.gaia3d.terrain.tile.core.*;
import com.gaia3d.terrain.tile.elevation.*;
import com.gaia3d.terrain.tile.generation.*;
import com.gaia3d.terrain.tile.layer.*;
import com.gaia3d.terrain.tile.mesh.*;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.tile.raster.TerrainRasterData;
import com.gaia3d.terrain.tile.raster.TerrainRasterFormat;
import com.gaia3d.terrain.tile.raster.TerrainRasterReader;
import org.eclipse.imagen.PlanarImage;
import org.eclipse.imagen.RasterFactory;
import org.eclipse.imagen.TiledImage;
import org.eclipse.imagen.media.range.NoDataContainer;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.util.CoverageUtilities;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RasterStandardizerTest {

    @Test
    @Tag("default")
    void zeroBasedSharedRasterPreservesCroppedDataOffset() {
        WritableRaster source = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, 8, 8, 1, null);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                source.setSample(x, y, 0, y * 10 + x);
            }
        }
        WritableRaster cropped = source.createWritableChild(3, 2, 3, 4, 3, 2, null);

        WritableRaster translated = new RasterStandardizer().createSharedZeroBasedWritableRaster(cropped);

        assertEquals(0, translated.getMinX());
        assertEquals(0, translated.getMinY());
        assertEquals(23.0, translated.getSampleDouble(0, 0, 0));
        assertEquals(55.0, translated.getSampleDouble(2, 3, 0));
    }

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

            long rasterFileCount;
            try (var paths = Files.walk(tempDir)) {
                rasterFileCount = paths.filter(path -> path.getFileName().toString().endsWith(TerrainRasterFormat.EXTENSION)).count();
            }

            assertEquals(4, rasterFileCount);
            try (var paths = Files.list(tempDir)) {
                assertEquals(1, paths.filter(Files::isDirectory).count());
            }
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
            try (var paths = Files.walk(tempDir)) {
                outputTile = paths
                        .filter(path -> path.getFileName().toString().endsWith(TerrainRasterFormat.EXTENSION))
                        .findFirst()
                        .orElseThrow();
            }

            TerrainRasterData output = new TerrainRasterReader().read(outputTile);
            assertEquals(101.0, output.getElevation(0, 0), 0.0001);
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

    @Test
    @Tag("default")
    void addGeoidPreservesCoverageNoDataValue() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setNoDataValue(-9999.0);

        RasterStandardizer rasterStandardizer = new RasterStandardizer();
        GridCoverage2D demCoverage = createCoverage("dem", new float[][]{
                {-32768.0f, 2.0f},
                {3.0f, 4.0f}
        }, -32768.0);
        GridCoverage2D geoidCoverage = createCoverage("geoid", new float[][]{
                {10.0f, 10.0f},
                {10.0f, 10.0f}
        });

        GridCoverage2D adjustedCoverage = rasterStandardizer.addGeoidPreserveDemNoData(demCoverage, geoidCoverage);
        var raster = adjustedCoverage.getRenderedImage().getData();
        NoDataContainer noDataContainer = CoverageUtilities.getNoDataProperty(adjustedCoverage);

        assertEquals(-32768.0, raster.getSampleDouble(0, 0, 0), 0.0001);
        assertEquals(12.0, raster.getSampleDouble(1, 0, 0), 0.0001);
        assertNotNull(noDataContainer);
        assertEquals(-32768.0, noDataContainer.getAsSingleValue(), 0.0001);

        adjustedCoverage.dispose(true);
        demCoverage.dispose(true);
        geoidCoverage.dispose(true);
    }

    @Test
    @Tag("default")
    void standardizeSkipsAllNoDataTiles() throws Exception {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setOutputCRS(DefaultGeographicCRS.WGS84);
        globalOptions.setMaxRasterSize(4);
        globalOptions.setNoDataValue(-9999.0);

        RasterStandardizer rasterStandardizer = new RasterStandardizer();
        GridCoverage2D coverage = createCoverage("nodata", new float[][]{
                {-9999.0f, -9999.0f},
                {-9999.0f, -9999.0f}
        });
        Path tempDir = Files.createTempDirectory("raster-standardize-nodata-");

        try {
            rasterStandardizer.standardize(coverage, tempDir.toFile());

            long rasterFileCount;
            try (var paths = Files.walk(tempDir)) {
                rasterFileCount = paths.filter(path -> path.getFileName().toString().endsWith(TerrainRasterFormat.EXTENSION)).count();
            }

            assertEquals(0, rasterFileCount);
        } finally {
            coverage.dispose(true);
            deleteRecursively(tempDir);
        }
    }

    @Test
    @Tag("default")
    void standardizePreservesCoverageNoDataValue() throws Exception {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setOutputCRS(DefaultGeographicCRS.WGS84);
        globalOptions.setMaxRasterSize(4);
        globalOptions.setNoDataValue(-9999.0);

        RasterStandardizer rasterStandardizer = new RasterStandardizer();
        GridCoverage2D coverage = createCoverage("nodata32768", new float[][]{
                {-32768.0f, 10.0f},
                {20.0f, 30.0f}
        }, -32768.0);
        Path tempDir = Files.createTempDirectory("raster-standardize-nodata32768-");

        try {
            rasterStandardizer.standardize(coverage, tempDir.toFile());

            Path outputTile;
            try (var paths = Files.walk(tempDir)) {
                outputTile = paths
                        .filter(path -> path.getFileName().toString().endsWith(TerrainRasterFormat.EXTENSION))
                        .findFirst()
                        .orElseThrow();
            }

            TerrainRasterData output = new TerrainRasterReader().read(outputTile);
            assertEquals(-9999.0f, output.noDataValue());
            assertEquals(-9999.0f, output.getElevation(0, 0));
        } finally {
            coverage.dispose(true);
            deleteRecursively(tempDir);
        }
    }

    @Test
    @Tag("default")
    void geoTiffManagerWritesFloat32NoDataCoverageWithColorModelFallback() throws Exception {
        GridCoverage2D coverage = createCoverage("resize-nodata32768", new float[][]{
                {-32768.0f, 10.0f},
                {20.0f, 30.0f}
        }, -32768.0);
        Path tempDir = Files.createTempDirectory("geotiff-manager-nodata32768-");
        Path outputFile = tempDir.resolve("resized.tif");

        try {
            new GeoTiffCoverageStore().saveGridCoverage2D(coverage, outputFile.toString());

            GeoTiffReader reader = new GeoTiffReader(outputFile.toFile());
            try {
                GridCoverage2D outputCoverage = reader.read(null);
                NoDataContainer noDataContainer = CoverageUtilities.getNoDataProperty(outputCoverage);
                double sample = outputCoverage.getRenderedImage().getData().getSampleDouble(0, 0, 0);

                assertEquals(-32768.0, sample, 0.0001);
                assertNotNull(noDataContainer);
                assertEquals(-32768.0, noDataContainer.getAsSingleValue(), 0.0001);

                outputCoverage.dispose(true);
            } finally {
                reader.dispose();
            }
        } finally {
            coverage.dispose(true);
            deleteRecursively(tempDir);
        }
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

    private GridCoverage2D createCoverage(String name, float[][] values, double noDataValue) {
        int height = values.length;
        int width = values[0].length;
        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, width, height, 1, null);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.setSample(x, y, 0, values[y][x]);
            }
        }

        ReferencedEnvelope envelope = new ReferencedEnvelope(0.0, width, 0.0, height, DefaultGeographicCRS.WGS84);
        TiledImage image = new TiledImage(0, 0, width, height, 0, 0, raster.getSampleModel(), PlanarImage.createColorModel(raster.getSampleModel()));
        image.setData(raster);
        Map<String, Object> properties = new HashMap<>();
        CoverageUtilities.setNoDataProperty(properties, new NoDataContainer(noDataValue));
        return new GridCoverageFactory().create(name, image, envelope, null, null, properties);
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
