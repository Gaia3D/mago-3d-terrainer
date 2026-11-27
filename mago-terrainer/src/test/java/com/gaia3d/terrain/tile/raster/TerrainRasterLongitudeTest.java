package com.gaia3d.terrain.tile.raster;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("default")
class TerrainRasterLongitudeTest {
    @TempDir
    Path tempDir;

    /**
     * 4 columns x 1 row with the values 1, 2, 3, 4 from the west to the east.
     */
    private static TerrainRasterData raster(double minLongitude, double maxLongitude) {
        return new TerrainRasterData(4, 1, minLongitude, 0.0, maxLongitude, 1.0, Float.NaN, new float[]{1, 2, 3, 4});
    }

    private TerrainRasterData read(Path path) throws IOException {
        return new TerrainRasterReader().read(path);
    }

    @Test
    void rasterInsideTheRangeIsUnchanged() throws IOException {
        Path path = tempDir.resolve("a.raster");
        List<Path> written = new TerrainRasterWriter().writeNormalizedLongitude(path, raster(10.0, 50.0));
        assertEquals(List.of(path), written);
        TerrainRasterData data = read(path);
        assertEquals(10.0, data.minLongitude());
        assertEquals(50.0, data.maxLongitude());
    }

    @Test
    void rasterBeyond180IsShifted() throws IOException {
        Path path = tempDir.resolve("b.raster");
        new TerrainRasterWriter().writeNormalizedLongitude(path, raster(200.0, 240.0));
        TerrainRasterData data = read(path);
        assertEquals(-160.0, data.minLongitude(), 1.0e-9);
        assertEquals(-120.0, data.maxLongitude(), 1.0e-9);
        assertArrayEquals(new float[]{1, 2, 3, 4}, data.elevations());
    }

    @Test
    void rasterCrossingTheAntimeridianIsSplit() throws IOException {
        Path path = tempDir.resolve("c.raster");
        List<Path> written = new TerrainRasterWriter().writeNormalizedLongitude(path, raster(160.0, 200.0));
        assertEquals(2, written.size());
        TerrainRasterData west = read(written.get(0));
        TerrainRasterData east = read(written.get(1));
        assertEquals(160.0, west.minLongitude(), 1.0e-9);
        assertEquals(180.0, west.maxLongitude(), 1.0e-9);
        assertArrayEquals(new float[]{1, 2}, west.elevations());
        assertEquals(-180.0, east.minLongitude(), 1.0e-9);
        assertEquals(-160.0, east.maxLongitude(), 1.0e-9);
        assertArrayEquals(new float[]{3, 4}, east.elevations());
    }

    @Test
    void globalRasterIn0To360IsRotatedIntoOneRaster() throws IOException {
        Path path = tempDir.resolve("d.raster");
        List<Path> written = new TerrainRasterWriter().writeNormalizedLongitude(path, raster(0.0, 360.0));
        assertEquals(List.of(path), written);
        TerrainRasterData data = read(path);
        assertEquals(-180.0, data.minLongitude(), 1.0e-9);
        assertEquals(180.0, data.maxLongitude(), 1.0e-9);
        // the columns 180..360 come first
        assertArrayEquals(new float[]{3, 4, 1, 2}, data.elevations());
    }
}
