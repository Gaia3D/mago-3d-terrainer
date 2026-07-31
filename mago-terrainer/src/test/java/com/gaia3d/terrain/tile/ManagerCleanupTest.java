package com.gaia3d.terrain.tile;

import com.gaia3d.terrain.tile.core.*;
import com.gaia3d.terrain.tile.elevation.*;
import com.gaia3d.terrain.tile.generation.*;
import com.gaia3d.terrain.tile.layer.*;
import com.gaia3d.terrain.tile.mesh.*;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.structure.TerrainTriangle;
import com.gaia3d.terrain.tile.geotiff.GeoTiffCoverageStore;
import com.gaia3d.util.CelestialBody;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
        TerrainElevationModeler manager = new TerrainElevationModeler();
        TerrainTilesetGenerator owner = new TerrainTilesetGenerator();
        manager.setTerrainTilesetGenerator(owner);
        manager.getTerrainElevationDataArray().add(new TerrainElevationData(manager));
        manager.getTrianglesArray().add(new TerrainTriangle());
        manager.getGridAreaMap().put("dem.tif", 1.0);
        manager.getGeoTiffFileNames().add("dem.tif");
        manager.setGeoTiffManager(new GeoTiffCoverageStore());

        manager.deleteObjects();

        assertTrue(manager.getTerrainElevationDataArray().isEmpty());
        assertTrue(manager.getTrianglesArray().isEmpty());
        assertTrue(manager.getGridAreaMap().isEmpty());
        assertTrue(manager.getGeoTiffFileNames().isEmpty());
        assertNull(manager.getGeoTiffManager());
        assertNull(manager.getTerrainTilesetGenerator());
    }

    @Test
    @Tag("default")
    void terrainTilesetGeneratorDeleteObjectsClearsAccumulatedCollections() {
        initializeGlobalOptions();
        TerrainTilesetGenerator manager = new TerrainTilesetGenerator();
        TerrainElevationModeler terrainElevationModeler = new TerrainElevationModeler();
        manager.setTerrainElevationModeler(terrainElevationModeler);
        manager.getTerrainElevationDataList().add(new TerrainElevationData(terrainElevationModeler));
        manager.getTriangleList().add(new TerrainTriangle());
        manager.getGeographicTerrainTileList().add(new GeographicTerrainTile(null, manager));
        manager.getStandardizedGeoTiffFiles().add(new File("dem.tif"));
        manager.getAvailableTileSet().getMapDepthAvailableTileRanges().put(0, new java.util.ArrayList<>());

        manager.deleteObjects();

        assertNull(manager.getTerrainElevationModeler());
        assertTrue(manager.getTerrainElevationDataList().isEmpty());
        assertTrue(manager.getTriangleList().isEmpty());
        assertTrue(manager.getGeographicTerrainTileList().isEmpty());
        assertTrue(manager.getStandardizedGeoTiffFiles().isEmpty());
        assertTrue(manager.getAvailableTileSet().getMapDepthAvailableTileRanges().isEmpty());
        assertTrue(manager.getMapNoUsableGeotiffPaths().isEmpty());
        assertEquals(0, manager.getDepthGeoTiffFilesMap().size());
    }

    @Test
    @Tag("default")
    void depthRasterSelectionCanKeepMixedResolutionFiles() {
        initializeGlobalOptions();
        TerrainTilesetGenerator manager = new TerrainTilesetGenerator();
        File coarseStandardized = new File("standardized/5m.tif");
        File fineResized = new File("resized/14/1m.tif");

        manager.getDepthGeoTiffFilesMap().put(14, new java.util.ArrayList<>(List.of(coarseStandardized, fineResized)));

        List<File> resolvedFiles = manager.resolveRasterFilesForDepth(14);

        assertEquals(2, resolvedFiles.size());
        assertTrue(resolvedFiles.contains(coarseStandardized));
        assertTrue(resolvedFiles.contains(fineResized));
    }
}
