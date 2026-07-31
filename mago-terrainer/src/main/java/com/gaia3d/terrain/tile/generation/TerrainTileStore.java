package com.gaia3d.terrain.tile.generation;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.io.LittleEndianDataInputStream;
import com.gaia3d.quantized.mesh.QuantizedMesh;
import com.gaia3d.quantized.mesh.QuantizedMeshManager;
import com.gaia3d.terrain.tile.core.GeographicTerrainTile;
import com.gaia3d.terrain.tile.core.TileIndices;
import com.gaia3d.terrain.tile.core.TileRange;
import com.gaia3d.terrain.util.GeographicTerrainTileUtils;
import com.gaia3d.terrain.util.TerrainMeshUtils;
import com.gaia3d.util.FileUtils;
import com.gaia3d.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.referencing.operation.TransformException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class TerrainTileStore {
    private final GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final TerrainTilesetGenerator generator;

    public void writeTempTilesFromQuantizedMeshes(int depth) {
        String tempPath = globalOptions.getTileTempPath();
        String depthTempPath = tempPath + File.separator + "L" + depth;
        File depthTempFolder = new File(depthTempPath);
        if (!depthTempFolder.exists() && depthTempFolder.mkdirs()) {
            log.debug("Created temp folder: {}", depthTempFolder.getAbsolutePath());
        }

        String quantizedMeshPath = globalOptions.getOutputPath() + File.separator + depth;
        File quantizedMeshFolder = new File(quantizedMeshPath);
        if (!quantizedMeshFolder.exists()) {
            log.error("Quantized mesh folder does not exist: {}", quantizedMeshPath);
            return;
        }

        TileIndices tileIndices = new TileIndices();
        QuantizedMeshManager quantizedMeshManager = new QuantizedMeshManager();
        List<String> quantizedMeshFolderNames = new ArrayList<>();
        FileUtils.getFolderNames(quantizedMeshPath, quantizedMeshFolderNames);

        for (String quantizedMeshFolderName : quantizedMeshFolderNames) {
            int x = Integer.parseInt(quantizedMeshFolderName);
            String quantizedMeshFolderPath = quantizedMeshPath + File.separator + quantizedMeshFolderName;
            File quantizedMeshSubFolder = new File(quantizedMeshFolderPath);
            if (!quantizedMeshSubFolder.exists()) {
                log.error("Quantized mesh subfolder does not exist: {}", quantizedMeshSubFolder.getAbsolutePath());
                continue;
            }

            String tempXFolderName = "X" + quantizedMeshFolderName;
            List<String> quantizedMeshFileNames = new ArrayList<>();
            FileUtils.getFileNames(quantizedMeshFolderPath, ".terrain", quantizedMeshFileNames);
            for (String quantizedMeshFileName : quantizedMeshFileNames) {
                int y = Integer.parseInt(quantizedMeshFileName.substring(0, quantizedMeshFileName.indexOf(".")));
                String quantizedMeshFilePath = quantizedMeshFolderPath + File.separator + quantizedMeshFileName;
                File quantizedMeshFile = new File(quantizedMeshFilePath);
                if (!quantizedMeshFile.exists()) {
                    log.error("Quantized mesh file does not exist: {}", quantizedMeshFilePath);
                    continue;
                }

                String tempFileName = "L" + depth + "_" + tempXFolderName + "_Y" + y + ".til";
                String tempFilePath = depthTempPath + File.separator + tempXFolderName + File.separator + tempFileName;
                if (new File(tempFilePath).exists()) {
                    log.debug("Temp file already exists: {}", tempFilePath);
                    continue;
                }

                try {
                    LittleEndianDataInputStream inputStream = new LittleEndianDataInputStream(new BufferedInputStream(new FileInputStream(quantizedMeshFilePath)));
                    QuantizedMesh quantizedMesh = new QuantizedMesh();
                    quantizedMesh.loadDataInputStream(inputStream);

                    tileIndices.set(x, y, depth);
                    GeographicTerrainTile geographicTerrainTile = quantizedMeshManager.getGeographicTerrainTileFromQuantizedMesh(quantizedMesh, tileIndices, generator);
                    geographicTerrainTile.saveFile(geographicTerrainTile.getMesh(), tempFilePath);
                } catch (Exception e) {
                    log.error("Error loading quantized mesh file: {}", quantizedMeshFilePath, e);
                }
            }
        }
    }

    public void writeTempTilesFromQuantizedMeshes(int depth, TileRange tileRange) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        String tempPath = globalOptions.getTileTempPath();
        String depthTempPath = tempPath + File.separator + "L" + depth;
        File depthTempFolder = new File(depthTempPath);
        if (!depthTempFolder.exists() && depthTempFolder.mkdirs()) {
            log.debug("Created temp folder: {}", depthTempFolder.getAbsolutePath());
        }

        String quantizedMeshPath = globalOptions.getOutputPath() + File.separator + depth;
        File quantizedMeshFolder = new File(quantizedMeshPath);
        if (!quantizedMeshFolder.exists()) {
            log.error("Quantized mesh folder does not exist: {}", quantizedMeshPath);
            return;
        }

        TileIndices tileIndices = new TileIndices();
        QuantizedMeshManager quantizedMeshManager = new QuantizedMeshManager();

        for (int tileX = tileRange.getMinTileX(); tileX <= tileRange.getMaxTileX(); tileX++) {
            for (int tileY = tileRange.getMinTileY(); tileY <= tileRange.getMaxTileY(); tileY++) {
                String qMeshFullPath = quantizedMeshPath + File.separator + tileX + File.separator + tileY + ".terrain";
                if (!new File(qMeshFullPath).exists()) {
                    log.info("Quantized mesh file does not exist: {}", qMeshFullPath);
                    continue;
                }

                String tempXFolderName = "X" + tileX;
                String tempFileName = "L" + depth + "_" + tempXFolderName + "_Y" + tileY + ".til";
                String tempFilePath = depthTempPath + File.separator + tempXFolderName + File.separator + tempFileName;
                if (new File(tempFilePath).exists()) {
                    log.debug("Temp file already exists: {}", tempFilePath);
                    continue;
                }

                try {
                    LittleEndianDataInputStream inputStream = new LittleEndianDataInputStream(new BufferedInputStream(new FileInputStream(qMeshFullPath)));
                    QuantizedMesh quantizedMesh = new QuantizedMesh();
                    quantizedMesh.loadDataInputStream(inputStream);

                    tileIndices.set(tileX, tileY, depth);
                    GeographicTerrainTile geographicTerrainTile = quantizedMeshManager.getGeographicTerrainTileFromQuantizedMesh(quantizedMesh, tileIndices, generator);
                    geographicTerrainTile.saveFile(geographicTerrainTile.getMesh(), tempFilePath);
                    TerrainMeshUtils.save4ChildrenMeshes(geographicTerrainTile.getMesh(), generator, globalOptions);
                    geographicTerrainTile.deleteObjects();
                } catch (Exception e) {
                    log.error("Error loading quantized mesh file: {}", qMeshFullPath, e);
                }
            }
        }
    }

    public void writeChildTempTiles(int depth) {
        int childrenDepth = depth + 1;
        String depthTempPath = globalOptions.getTileTempPath() + File.separator + "L" + depth;
        File depthTempFolder = new File(depthTempPath);
        if (!depthTempFolder.exists()) {
            return;
        }

        String childrenTempPath = globalOptions.getTileTempPath() + File.separator + "L" + childrenDepth;
        File childrenTempFolder = new File(childrenTempPath);
        if (!childrenTempFolder.exists() && childrenTempFolder.mkdirs()) {
            log.debug("Created children temp folder: {}", childrenTempFolder.getAbsolutePath());
        }

        List<String> xFolders = new ArrayList<>();
        FileUtils.getFolderNames(depthTempPath, xFolders);
        for (String xFolderName : xFolders) {
            int x = Integer.parseInt(xFolderName.substring(1));
            String xFolderPath = depthTempPath + File.separator + xFolderName;
            File xFolder = new File(xFolderPath);
            if (!xFolder.exists()) {
                log.error("X folder does not exist: {}", xFolderPath);
                continue;
            }

            File childrenXTempFolder = new File(childrenTempPath + File.separator + xFolderName);
            if (!childrenXTempFolder.exists() && childrenXTempFolder.mkdirs()) {
                log.debug("Created children temp folder: {}", childrenXTempFolder.getAbsolutePath());
            }

            List<String> tileFileNames = new ArrayList<>();
            FileUtils.getFileNames(xFolderPath, ".til", tileFileNames);
            for (String tileFileName : tileFileNames) {
                List<String> splitStrings = Arrays.asList(tileFileName.split("_"));
                String yFileName = splitStrings.get(2);
                int y = Integer.parseInt(yFileName.substring(1, yFileName.indexOf(".")));
                String tileFilePath = xFolderPath + File.separator + tileFileName;
                if (!new File(tileFilePath).exists()) {
                    log.error("GeographicTerrainTile file does not exist: {}", tileFilePath);
                    continue;
                }

                try {
                    TileIndices tileIndices = new TileIndices();
                    tileIndices.set(x, y, depth);
                    GeographicTerrainTile geographicTerrainTile = loadGeographicTerrainTile(tileIndices);
                    if (geographicTerrainTile == null) {
                        log.error("GeographicTerrainTile is null: {}", tileFilePath);
                        continue;
                    }

                    TerrainMeshUtils.save4ChildrenMeshes(geographicTerrainTile.getMesh(), generator, globalOptions);
                } catch (Exception e) {
                    log.error("Error loading GeographicTerrainTile file: {}", tileFilePath, e);
                }
            }
        }
    }

    public int determineExistentTileSetMaxDepth(String tileSetDirectory) {
        int existentTileSetMaxDepth = -1;
        List<String> folderNames = new ArrayList<>();
        FileUtils.getFolderNames(tileSetDirectory, folderNames);
        Map<Integer, Integer> depthFoldermap = new HashMap<>();
        for (String folderName : folderNames) {
            if (!StringUtils.isConvertibleToInt(folderName)) {
                continue;
            }
            int depth = Integer.parseInt(folderName);
            depthFoldermap.put(depth, depth);
        }

        for (int i = 0; i < depthFoldermap.size(); i++) {
            int depth = depthFoldermap.get(i);
            if (depth > existentTileSetMaxDepth) {
                existentTileSetMaxDepth = depth;
            }
        }

        return existentTileSetMaxDepth;
    }

    public boolean existTempFiles(int depth) {
        String depthTempPath = globalOptions.getTileTempPath() + File.separator + "L" + depth;
        return new File(depthTempPath).exists();
    }

    public void deleteTempFilesByDepth(int depth) {
        String depthTempPath = globalOptions.getTileTempPath() + File.separator + "L" + depth;
        File depthTempFolder = new File(depthTempPath);
        if (depthTempFolder.exists()) {
            org.apache.commons.io.FileUtils.deleteQuietly(depthTempFolder);
        }
    }

    public void deleteTempFilesByTileRange(TileRange tileRange) {
        int depth = tileRange.getTileDepth();
        String depthTempPath = globalOptions.getTileTempPath() + File.separator + "L" + depth;
        File depthTempFolder = new File(depthTempPath);
        if (!depthTempFolder.exists()) {
            return;
        }

        for (int x = tileRange.getMinTileX(); x <= tileRange.getMaxTileX(); x++) {
            String xFolderName = "X" + x;
            String xFolderPath = depthTempPath + File.separator + xFolderName;
            File xFolder = new File(xFolderPath);
            if (!xFolder.exists()) {
                continue;
            }

            for (int y = tileRange.getMinTileY(); y <= tileRange.getMaxTileY(); y++) {
                String tempFilePath = xFolderPath + File.separator + "L" + depth + "_" + xFolderName + "_Y" + y + ".til";
                File tempFile = new File(tempFilePath);
                if (tempFile.exists() && tempFile.delete()) {
                    log.debug("Deleted temp file: {}", tempFilePath);
                } else if (tempFile.exists()) {
                    log.warn("Failed to delete temp file: {}", tempFilePath);
                }
            }

            String[] remainingFiles = xFolder.list();
            if (remainingFiles != null && remainingFiles.length == 0) {
                if (xFolder.delete()) {
                    log.debug("Deleted empty X folder: {}", xFolderPath);
                } else {
                    log.warn("Failed to delete X folder: {}", xFolderPath);
                }
            }
        }
    }

    public String getTilePath(TileIndices tileIndices) {
        String tileTempDirectory = globalOptions.getTileTempPath();
        String neighborFilePath = GeographicTerrainTileUtils.getTileFilePath(tileIndices.getX(), tileIndices.getY(), tileIndices.getL());
        return tileTempDirectory + File.separator + neighborFilePath;
    }

    public String getQuantizedMeshTileFolderPath(TileIndices tileIndices) {
        return globalOptions.getOutputPath() + File.separator + tileIndices.getL() + File.separator + tileIndices.getX();
    }

    public String getQuantizedMeshTilePath(TileIndices tileIndices) {
        return getQuantizedMeshTileFolderPath(tileIndices) + File.separator + tileIndices.getY() + ".terrain";
    }

    public GeographicTerrainTile loadOrCreateGeographicTerrainTile(TileIndices tileIndices) throws IOException, TransformException {
        if (!tileIndices.isValid()) {
            return null;
        }

        String tilePath = getTilePath(tileIndices);
        GeographicTerrainTile tile = new GeographicTerrainTile(null, generator);
        tile.setTileIndices(tileIndices);
        tile.setGeographicExtension(GeographicTerrainTileUtils.getGeographicExtentOfTileLXY(
                tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null,
                generator.getImaginaryType(), generator.originIsLeftUp()));

        if (!FileUtils.isFileExists(tilePath)) {
            log.debug("Creating tile: CREATE - * - CREATE : {}, {}, {}", tileIndices.getX(), tileIndices.getY(), tileIndices.getL());
            tile.createInitialMesh();
            if (tile.getMesh() == null) {
                log.error("Error: tile.mesh == null");
            }
            tile.saveFile(tile.getMesh(), tilePath);
            return tile;
        }

        log.debug("Loading tile: LOAD - * - LOAD : {}, {}, {}", tileIndices.getX(), tileIndices.getY(), tileIndices.getL());
        tile.loadFile(tilePath);
        return tile;
    }

    public GeographicTerrainTile loadGeographicTerrainTile(TileIndices tileIndices) throws IOException {
        String tilePath = getTilePath(tileIndices);
        if (!FileUtils.isFileExists(tilePath)) {
            return null;
        }

        log.debug("Loading tile: LOAD - * - LOAD : {}, {}, {}", tileIndices.getX(), tileIndices.getY(), tileIndices.getL());
        GeographicTerrainTile tile = new GeographicTerrainTile(null, generator);
        tile.setTileIndices(tileIndices);
        tile.setGeographicExtension(GeographicTerrainTileUtils.getGeographicExtentOfTileLXY(
                tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null,
                generator.getImaginaryType(), generator.originIsLeftUp()));
        tile.loadFile(tilePath);
        return tile;
    }
}
