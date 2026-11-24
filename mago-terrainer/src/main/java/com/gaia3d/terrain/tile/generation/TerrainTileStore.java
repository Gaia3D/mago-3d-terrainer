package com.gaia3d.terrain.tile.generation;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.io.LittleEndianDataInputStream;
import com.gaia3d.quantized.mesh.QuantizedMesh;
import com.gaia3d.quantized.mesh.QuantizedMeshManager;
import com.gaia3d.terrain.structure.TerrainMesh;
import com.gaia3d.terrain.tile.core.GeographicTerrainTile;
import com.gaia3d.terrain.tile.core.TileIndices;
import com.gaia3d.terrain.tile.core.TileRange;
import com.gaia3d.terrain.util.GeographicTerrainTileUtils;
import com.gaia3d.terrain.util.TerrainMeshUtils;
import com.gaia3d.util.DecimalUtils;
import com.gaia3d.util.FileUtils;
import com.gaia3d.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.referencing.operation.TransformException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class TerrainTileStore {
    private final GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final TerrainTilesetGenerator generator;
    private static final String PROGRESS_FILE_NAME = "progress.properties";
    private static final String COMPLETED_DEPTH_KEY = "completedDepth";
    private static final String RUNNING_DEPTH_KEY = "runningDepth";

    private final TempTileCache tempTileCache = new TempTileCache();
    // the depth that started but whose temp tiles are not modified yet, see beginDepth()
    private int pendingRunningDepth = -1;

    public void saveTempTileMesh(TerrainMesh mesh, String filePath) throws IOException {
        markDepthRunning();
        tempTileCache.save(mesh, filePath);
    }

    /**
     * Writes the in-memory temp tiles to the temp folder, so the "continue" mode can resume from the next depth.
     */
    public void checkpointTempTiles(int depth) {
        long startTime = System.currentTimeMillis();
        try {
            int writtenCount = tempTileCache.checkpoint();
            if (writtenCount > 0) {
                log.info("[Tile][{}] Checkpoint {} temp tiles : Duration: {}", depth, writtenCount, DecimalUtils.millisecondToDisplayTime(System.currentTimeMillis() - startTime));
            }
        } catch (IOException e) {
            log.error("Failed to checkpoint the temp tiles.", e);
        }
    }

    public void clearTempTileCache() {
        tempTileCache.clear();
    }

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

    /**
     * Starts a depth. It is recorded as running only when its temp tiles are modified for the first time (overwritten or deleted):
     * until then, the temp tiles are still those of the end of the previous depth, and "-continue" can resume from them.
     */
    public void beginDepth(int depth) {
        pendingRunningDepth = depth;
    }

    private void markDepthRunning() {
        if (pendingRunningDepth >= 0) {
            int runningDepth = pendingRunningDepth;
            pendingRunningDepth = -1;
            writeDepthProgress(runningDepth - 1, runningDepth);
        }
    }

    /**
     * Records the progress of the tiling in the temp folder, so "-continue" knows if the previous run stopped in the middle of a depth.
     * @param completedDepth the last depth whose tiles (and the temp tiles of the next depth) are complete, -1 if none
     * @param runningDepth the depth being generated, equal to completedDepth between two depths
     */
    public void writeDepthProgress(int completedDepth, int runningDepth) {
        if (completedDepth == runningDepth) {
            pendingRunningDepth = -1;
        }
        Path progressPath = Path.of(globalOptions.getTileTempPath(), PROGRESS_FILE_NAME);
        Path writingPath = Path.of(globalOptions.getTileTempPath(), PROGRESS_FILE_NAME + ".tmp");
        String content = COMPLETED_DEPTH_KEY + "=" + completedDepth + System.lineSeparator() + RUNNING_DEPTH_KEY + "=" + runningDepth + System.lineSeparator();
        try {
            Files.createDirectories(progressPath.getParent());
            Files.writeString(writingPath, content, StandardCharsets.UTF_8);
            Files.move(writingPath, progressPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.warn("Failed to write the tiling progress: {}", progressPath, e);
        }
    }

    /**
     * @return {completedDepth, runningDepth} of the previous run, or null if there is no progress file
     */
    public int[] readDepthProgress() {
        Path progressPath = Path.of(globalOptions.getTileTempPath(), PROGRESS_FILE_NAME);
        if (!Files.isRegularFile(progressPath)) {
            return null;
        }
        try (var reader = Files.newBufferedReader(progressPath, StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(reader);
            int completedDepth = Integer.parseInt(properties.getProperty(COMPLETED_DEPTH_KEY).trim());
            int runningDepth = Integer.parseInt(properties.getProperty(RUNNING_DEPTH_KEY).trim());
            return new int[]{completedDepth, runningDepth};
        } catch (Exception e) {
            log.warn("Failed to read the tiling progress: {}", progressPath, e);
            return null;
        }
    }

    public void deleteTempFilesByDepth(int depth) {
        String depthTempPath = globalOptions.getTileTempPath() + File.separator + "L" + depth;
        tempTileCache.removeByPrefix(depthTempPath + File.separator);
        File depthTempFolder = new File(depthTempPath);
        if (depthTempFolder.exists()) {
            org.apache.commons.io.FileUtils.deleteQuietly(depthTempFolder);
        }
    }

    public void deleteTempFilesByTileRange(TileRange tileRange) {
        markDepthRunning();
        int depth = tileRange.getTileDepth();
        String depthTempPath = globalOptions.getTileTempPath() + File.separator + "L" + depth;

        // remove the in-memory tiles. Only the tiles that may exist on disk are deleted from the temp folder.
        List<String> diskTempFilePaths = new ArrayList<>();
        for (int x = tileRange.getMinTileX(); x <= tileRange.getMaxTileX(); x++) {
            for (int y = tileRange.getMinTileY(); y <= tileRange.getMaxTileY(); y++) {
                String tempFilePath = getTilePath(new TileIndices(x, y, depth));
                if (tempTileCache.remove(tempFilePath)) {
                    diskTempFilePaths.add(tempFilePath);
                }
            }
        }
        if (diskTempFilePaths.isEmpty()) {
            return;
        }

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

        if (!tempTileCache.exists(tilePath)) {
            log.debug("Creating tile: CREATE - * - CREATE : {}, {}, {}", tileIndices.getX(), tileIndices.getY(), tileIndices.getL());
            tile.createInitialMesh();
            if (tile.getMesh() == null) {
                log.error("Error: tile.mesh == null");
            }
            tempTileCache.save(tile.getMesh(), tilePath);
            return tile;
        }

        log.debug("Loading tile: LOAD - * - LOAD : {}, {}, {}", tileIndices.getX(), tileIndices.getY(), tileIndices.getL());
        tile.setMesh(tempTileCache.load(tilePath));
        return tile;
    }

    public GeographicTerrainTile loadGeographicTerrainTile(TileIndices tileIndices) throws IOException {
        String tilePath = getTilePath(tileIndices);
        if (!tempTileCache.exists(tilePath)) {
            return null;
        }

        log.debug("Loading tile: LOAD - * - LOAD : {}, {}, {}", tileIndices.getX(), tileIndices.getY(), tileIndices.getL());
        GeographicTerrainTile tile = new GeographicTerrainTile(null, generator);
        tile.setTileIndices(tileIndices);
        tile.setGeographicExtension(GeographicTerrainTileUtils.getGeographicExtentOfTileLXY(
                tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null,
                generator.getImaginaryType(), generator.originIsLeftUp()));
        tile.setMesh(tempTileCache.load(tilePath));
        return tile;
    }
}
