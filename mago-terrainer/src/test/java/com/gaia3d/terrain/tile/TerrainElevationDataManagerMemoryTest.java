package com.gaia3d.terrain.tile;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TerrainElevationDataManagerMemoryTest {

    @Test
    @Tag("default")
    void preloadTerrainElevationRastersIsDisabledByDefault() {
        TerrainElevationDataManager manager = new TerrainElevationDataManager();
        CountingTerrainElevationData elevationData = new CountingTerrainElevationData(manager);

        manager.preloadTerrainElevationRasters(List.of(elevationData));

        assertEquals(0, elevationData.preloadCalls);
    }

    private static class CountingTerrainElevationData extends TerrainElevationData {
        private int preloadCalls = 0;

        private CountingTerrainElevationData(TerrainElevationDataManager terrainElevationDataManager) {
            super(terrainElevationDataManager);
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
