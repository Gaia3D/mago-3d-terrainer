package com.gaia3d.command;

import com.gaia3d.terrain.tile.elevation.TerrainElevationModeler;
import com.gaia3d.terrain.tile.generation.TerrainTilesetGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;

@Slf4j
public class TerrainerPipeline {
    private final GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final TerrainTilesetGenerator terrainTilesetGenerator;

    public TerrainerPipeline() {
        this(new TerrainTilesetGenerator());
    }

    TerrainerPipeline(TerrainTilesetGenerator terrainTilesetGenerator) {
        this.terrainTilesetGenerator = terrainTilesetGenerator;
    }

    public void execute() throws IOException, FactoryException, TransformException {
        preprocessRasters();
        runTilingProcess();
        cleanupMemory();
        cleanupTempFiles();
        System.gc();
    }

    private void preprocessRasters() throws IOException, FactoryException {
        log.info("[Pre][AvailableTileSet] Start calculating available tiles for each depth.");
        terrainTilesetGenerator.calculateAvailableTilesForEachDepth();
        log.info("[Pre][AvailableTileSet] Finished calculating available tiles for each depth.");

        if (!globalOptions.isSkipStandardizationAndResize()) {
            log.info("[Pre][Standardization] Start GeoTiff Standardization files.");
            terrainTilesetGenerator.standardizeInputRasters();
            log.info("[Pre][Standardization] Finished GeoTiff Standardization files.");

            log.info("[Pre][Resize] Start GeoTiff Resizing files.");
            terrainTilesetGenerator.prepareDepthRasters(globalOptions.getInputPath(), null);
            log.info("[Pre][Resize] Finished GeoTiff Resizing files.");
            return;
        }

        log.info("[Pre][Standardization] Skip standardization and resize files.");
        log.info("[Pre][Standardization] Load existing standardization and resized GeoTiff files.");
        terrainTilesetGenerator.loadPreparedRasters();
        log.info("[Pre][Standardization] Finished loading existing standardization and resized GeoTiff files.");
    }

    private void runTilingProcess() throws FactoryException, TransformException, IOException {
        prepareTerrainElevationData();
        runTileMeshGeneration(resolveProcessType());
    }

    private void prepareTerrainElevationData() throws FactoryException, TransformException {
        log.info("[Tile] Start generate terrain elevation data.");
        TerrainElevationModeler terrainElevationModeler = new TerrainElevationModeler();
        terrainTilesetGenerator.setTerrainElevationModeler(terrainElevationModeler);
        terrainElevationModeler.setTerrainTilesetGenerator(terrainTilesetGenerator);
        terrainElevationModeler.setTerrainElevationDataFiles(terrainTilesetGenerator.resolveRasterFilesForDepth(0));

        int depth = 0;
        terrainElevationModeler.generateTerrainQuadTree(depth);
        log.info("[Tile] Finished generate terrain elevation data.");
    }

    private ProcessType resolveProcessType() {
        if (globalOptions.isContinue()) {
            return ProcessType.CONTINUE;
        }
        if (globalOptions.isModify()) {
            return ProcessType.MODIFICATION;
        }
        return ProcessType.CREATION;
    }

    private void runTileMeshGeneration(ProcessType processType) throws IOException, TransformException, FactoryException {
        if (processType == ProcessType.CONTINUE) {
            log.info("[Tile] Continuing making tile meshes.");
            terrainTilesetGenerator.continueAvailableTileMeshes();
            log.info("[Tile] Finished making tile meshes.");
            return;
        }

        if (processType == ProcessType.MODIFICATION) {
            log.info("[Tile] Start making tile meshes.");
            terrainTilesetGenerator.generateModifiedAvailableTileMeshes();
            log.info("[Tile] Finished making tile meshes.");
            return;
        }

        log.info("[Tile] Start making tile meshes.");
        terrainTilesetGenerator.generateAvailableTileMeshes();
        log.info("[Tile] Finished making tile meshes.");
    }

    private void cleanupMemory() {
        log.info("[Post][Cleanup] Start deleting memory objects.");
        terrainTilesetGenerator.deleteObjects();
        log.info("[Post][Cleanup] Finished deleting memory objects.");
    }

    private void cleanupTempFiles() {
        if (globalOptions.isLeaveTemp()) {
            return;
        }

        File tileTempFolder = new File(globalOptions.getTileTempPath());
        if (tileTempFolder.exists() && tileTempFolder.isDirectory()) {
            FileUtils.deleteQuietly(tileTempFolder);
        }

        File splitTempFolder = new File(globalOptions.getSplitTiffTempPath());
        if (splitTempFolder.exists() && splitTempFolder.isDirectory()) {
            FileUtils.deleteQuietly(splitTempFolder);
        }

        File resizedTempFolder = new File(globalOptions.getResizedTiffTempPath());
        if (resizedTempFolder.exists() && resizedTempFolder.isDirectory()) {
            FileUtils.deleteQuietly(resizedTempFolder);
        }
    }
}
