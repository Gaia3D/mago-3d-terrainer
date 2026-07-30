package com.gaia3d.terrain.tile.elevation;

import com.gaia3d.terrain.tile.core.*;
import com.gaia3d.terrain.tile.elevation.*;
import com.gaia3d.terrain.tile.generation.*;
import com.gaia3d.terrain.tile.layer.*;
import com.gaia3d.terrain.tile.mesh.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TerrainElevationModelerMemoryTest {

    @Test
    @Tag("default")
    void preloadTerrainElevationRastersIsDisabledByDefault() {
        TerrainElevationModeler manager = new TerrainElevationModeler();
        CountingTerrainElevationData elevationData = new CountingTerrainElevationData(manager);

        manager.preloadTerrainElevationRasters(List.of(elevationData));

        assertEquals(0, elevationData.preloadCalls);
    }

    private static class CountingTerrainElevationData extends TerrainElevationData {
        private int preloadCalls = 0;

        private CountingTerrainElevationData(TerrainElevationModeler terrainElevationModeler) {
            super(terrainElevationModeler);
        }

        @Override
        public boolean isRasterLoaded() {
            return false;
        }

        @Override
        public long estimateRasterBytes() {
            return 1L;
        }

        @Override
        public boolean preloadRaster() {
            preloadCalls++;
            return true;
        }
    }
}
