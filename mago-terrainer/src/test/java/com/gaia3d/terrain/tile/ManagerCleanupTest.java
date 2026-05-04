package com.gaia3d.terrain.tile;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.structure.TerrainTriangle;
import com.gaia3d.terrain.tile.geotiff.GaiaGeoTiffManager;
import com.gaia3d.util.CelestialBody;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagerCleanupTest {

    private void initializeGlobalOptions() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setIntensity(4.0);
        globalOptions.setCelestialBody(CelestialBody.EARTH);
    }

    @Test
    @Tag("default")
    void terrainElevationDataManagerDeleteObjectsClearsStateWithoutQuadTree() {
        initializeGlobalOptions();
        TerrainElevationDataManager manager = new TerrainElevationDataManager();
        TileWgs84Manager owner = new TileWgs84Manager();
        manager.setTileWgs84Manager(owner);
        manager.getTerrainElevationDataArray().add(new TerrainElevationData(manager));
        manager.getTrianglesArray().add(new TerrainTriangle());
        manager.getGridAreaMap().put("dem.tif", 1.0);
        manager.getGeoTiffFileNames().add("dem.tif");
        manager.setMyGaiaGeoTiffManager(new GaiaGeoTiffManager());

        manager.deleteObjects();

        assertTrue(manager.getTerrainElevationDataArray().isEmpty());
        assertTrue(manager.getTrianglesArray().isEmpty());
        assertTrue(manager.getGridAreaMap().isEmpty());
        assertTrue(manager.getGeoTiffFileNames().isEmpty());
        assertNull(manager.getMyGaiaGeoTiffManager());
        assertNull(manager.getTileWgs84Manager());
    }

    @Test
    @Tag("default")
    void tileWgs84ManagerDeleteObjectsClearsAccumulatedCollections() {
        initializeGlobalOptions();
        TileWgs84Manager manager = new TileWgs84Manager();
        TerrainElevationDataManager terrainElevationDataManager = new TerrainElevationDataManager();
        manager.setTerrainElevationDataManager(terrainElevationDataManager);
        manager.getTerrainElevationDataList().add(new TerrainElevationData(terrainElevationDataManager));
        manager.getTriangleList().add(new TerrainTriangle());
        manager.getTileWgs84List().add(new TileWgs84(null, manager));
        manager.getStandardizedGeoTiffFiles().add(new File("dem.tif"));
        manager.getAvailableTileSet().getMapDepthAvailableTileRanges().put(0, new java.util.ArrayList<>());

        manager.deleteObjects();

        assertNull(manager.getTerrainElevationDataManager());
        assertTrue(manager.getTerrainElevationDataList().isEmpty());
        assertTrue(manager.getTriangleList().isEmpty());
        assertTrue(manager.getTileWgs84List().isEmpty());
        assertTrue(manager.getStandardizedGeoTiffFiles().isEmpty());
        assertTrue(manager.getAvailableTileSet().getMapDepthAvailableTileRanges().isEmpty());
        assertTrue(manager.getMapNoUsableGeotiffPaths().isEmpty());
        assertEquals(0, manager.getDepthGeoTiffFolderPathMap().size());
    }
}
