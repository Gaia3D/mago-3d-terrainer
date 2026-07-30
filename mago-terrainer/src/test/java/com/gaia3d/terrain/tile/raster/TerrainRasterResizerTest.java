package com.gaia3d.terrain.tile.raster;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TerrainRasterResizerTest {
    @Test
    @Tag("default")
    void resizePreservesNearestNoDataMaskAndInterpolatesValidSamples() {
        TerrainRasterData source = new TerrainRasterData(2, 2, 0.0, 0.0, 2.0, 2.0, -9999.0f,
                new float[]{-9999.0f, 10.0f, 20.0f, 30.0f});

        TerrainRasterData resized = new TerrainRasterResizer().resize(source, 4, 4);

        assertEquals(-9999.0f, resized.getElevation(0, 0));
        assertEquals(-9999.0f, resized.getElevation(1, 1));
        assertEquals(10.0f, resized.getElevation(3, 0), 0.0001f);
        assertEquals(30.0f, resized.getElevation(3, 3), 0.0001f);
        assertEquals(2, resized.originalWidth());
        assertEquals(2, resized.originalHeight());
    }
}
