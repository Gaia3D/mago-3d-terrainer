package com.gaia3d.terrain.tile.raster;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TerrainRasterReaderWriterTest {

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
