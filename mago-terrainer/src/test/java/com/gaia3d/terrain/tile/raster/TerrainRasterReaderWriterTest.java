package com.gaia3d.terrain.tile.raster;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.tile.core.*;
import com.gaia3d.terrain.tile.elevation.*;
import com.gaia3d.terrain.tile.generation.*;
import com.gaia3d.terrain.tile.layer.*;
import com.gaia3d.terrain.tile.mesh.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.eclipse.imagen.PlanarImage;
import org.eclipse.imagen.RasterFactory;
import org.eclipse.imagen.TiledImage;
import org.eclipse.imagen.media.range.NoDataContainer;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TerrainRasterReaderWriterTest {

    @Test
    @Tag("default")
    void coverageWriterPreservesDoubleNoDataMarkerForComparison() throws Exception {
        Path tempDir = Files.createTempDirectory("terrain-raster-double-nodata-");
        Path rasterPath = tempDir.resolve("tile" + TerrainRasterFormat.EXTENSION);
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        double previousNoData = globalOptions.getNoDataValue();
        globalOptions.setNoDataValue(-9999.0);
        GridCoverage2D coverage = createDoubleCoverage(
                new double[]{-Double.MAX_VALUE, -9999.0, Double.NaN, 123.5}, -Double.MAX_VALUE);

        try {
            new TerrainRasterWriter().write(rasterPath, coverage);
            TerrainRasterData read = new TerrainRasterReader().read(rasterPath);

            assertTrue(Float.isNaN(read.noDataValue()));
            assertTrue(Float.isNaN(read.getElevation(0, 0)));
            assertTrue(Float.isNaN(read.getElevation(1, 0)));
            assertTrue(Float.isNaN(read.getElevation(2, 0)));
            assertEquals(123.5f, read.getElevation(3, 0));
        } finally {
            coverage.dispose(true);
            globalOptions.setNoDataValue(previousNoData);
            deleteRecursively(tempDir);
        }
    }

    @Test
    @Tag("default")
    void writerAndReaderRoundTripFloat32TerrainRaster() throws Exception {
        Path tempDir = Files.createTempDirectory("terrain-raster-");
        Path rasterPath = tempDir.resolve("tile" + TerrainRasterFormat.EXTENSION);
        float[] elevations = new float[]{10.0f, 11.5f, -32768.0f, 13.25f, 14.0f, 15.75f};
        TerrainRasterData data = new TerrainRasterData(3, 2, 126.0, 37.0, 127.0, 38.0, -32768.0f, elevations);

        try {
            new TerrainRasterWriter().write(rasterPath, data);
            TerrainRasterData read = new TerrainRasterReader().read(rasterPath);

            assertEquals(3, read.width());
            assertEquals(2, read.height());
            assertEquals(3, read.originalWidth());
            assertEquals(2, read.originalHeight());
            assertEquals(126.0, read.minLongitude());
            assertEquals(37.0, read.minLatitude());
            assertEquals(127.0, read.maxLongitude());
            assertEquals(38.0, read.maxLatitude());
            assertEquals(-32768.0f, read.noDataValue());
            assertArrayEquals(elevations, read.elevations());
            assertEquals(-32768.0f, read.getElevation(2, 0));
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    @Tag("default")
    void readerRejectsInvalidMagic() throws Exception {
        Path tempDir = Files.createTempDirectory("terrain-raster-invalid-");
        Path rasterPath = tempDir.resolve("bad" + TerrainRasterFormat.EXTENSION);

        try {
            ByteBuffer header = ByteBuffer.allocate(TerrainRasterFormat.HEADER_SIZE_BYTES).order(TerrainRasterFormat.BYTE_ORDER);
            header.put(new byte[]{'B', 'A', 'D', '!'});
            header.putInt(TerrainRasterFormat.VERSION);
            Files.write(rasterPath, header.array());

            assertThrows(IOException.class, () -> new TerrainRasterReader().read(rasterPath));
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    @Tag("default")
    void readerRejectsTruncatedPayloadBeforeAllocatingSamples() throws Exception {
        Path tempDir = Files.createTempDirectory("terrain-raster-truncated-");
        Path rasterPath = tempDir.resolve("truncated" + TerrainRasterFormat.EXTENSION);

        try {
            ByteBuffer header = ByteBuffer.allocate(TerrainRasterFormat.HEADER_SIZE_BYTES).order(TerrainRasterFormat.BYTE_ORDER);
            header.put(TerrainRasterFormat.MAGIC);
            header.putInt(TerrainRasterFormat.VERSION);
            header.putInt(TerrainRasterFormat.HEADER_SIZE_BYTES);
            header.putInt(4);
            header.putInt(4);
            header.putDouble(126.0);
            header.putDouble(37.0);
            header.putDouble(127.0);
            header.putDouble(38.0);
            header.putFloat(-32768.0f);
            header.putInt(0);
            Files.write(rasterPath, header.array());

            assertThrows(IOException.class, () -> new TerrainRasterReader().read(rasterPath));
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    @Tag("default")
    void rasterDataRejectsMismatchedSampleCount() {
        assertThrows(IllegalArgumentException.class, () -> new TerrainRasterData(3, 2, 126.0, 37.0, 127.0, 38.0, -32768.0f, new float[]{1.0f, 2.0f}));
    }

    @Test
    @Tag("default")
    void publicRasterDataConstructorRetainsDefensiveCopy() {
        float[] elevations = {10.0f};
        TerrainRasterData data = new TerrainRasterData(1, 1, 126.0, 37.0, 127.0, 38.0, -9999.0f, elevations);

        elevations[0] = 20.0f;

        assertEquals(10.0f, data.getElevation(0, 0));
    }

    private GridCoverage2D createDoubleCoverage(double[] values, double noDataValue) {
        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_DOUBLE, values.length, 1, 1, null);
        for (int x = 0; x < values.length; x++) {
            raster.setSample(x, 0, 0, values[x]);
        }

        TiledImage image = new TiledImage(0, 0, values.length, 1, 0, 0,
                raster.getSampleModel(), PlanarImage.createColorModel(raster.getSampleModel()));
        image.setData(raster);
        Map<String, Object> properties = new HashMap<>();
        CoverageUtilities.setNoDataProperty(properties, new NoDataContainer(noDataValue));
        ReferencedEnvelope envelope = new ReferencedEnvelope(0.0, values.length, 0.0, 1.0, DefaultGeographicCRS.WGS84);
        return new GridCoverageFactory().create("double-dem", image, envelope, null, null, properties);
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var paths = Files.walk(path)) {
            for (Path child : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                Files.deleteIfExists(child);
            }
        }
    }
}
