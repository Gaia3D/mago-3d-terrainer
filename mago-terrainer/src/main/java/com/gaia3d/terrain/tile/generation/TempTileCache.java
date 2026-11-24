package com.gaia3d.terrain.tile.generation;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.io.BigEndianDataInputStream;
import com.gaia3d.io.BigEndianDataOutputStream;
import com.gaia3d.terrain.structure.TerrainMesh;
import com.gaia3d.terrain.util.MemoryMonitor;
import com.gaia3d.util.FileUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps the intermediate tile meshes (".til") in memory instead of writing them to the temp folder.
 * The serialized bytes are identical to the temp file contents, so the tiling result does not change.
 * When the memory budget is exceeded, the tiles are spilled to the temp folder as before.
 * At the end of each depth, the tiles are also written to the temp folder (checkpoint), so an interrupted run can be continued.
 * Continue/modify mode and "leaveTemp" rely on the temp files, so the cache is disabled for them.
 */
@Slf4j
public class TempTileCache {
    // ratio of the max heap that the in-memory tiles can use. "-Dmago.tempTileCacheRatio=0" disables the cache.
    private static final double MEMORY_BUDGET_RATIO = Double.parseDouble(System.getProperty("mago.tempTileCacheRatio", "0.25"));
    // the refinement stops when the free heap is under 10% (MemoryMonitor), so the cache yields the heap well before that.
    private static final double MIN_AVAILABLE_RATIO_TO_CACHE = 0.35;
    private static final double MIN_AVAILABLE_RATIO_TO_KEEP = 0.25;

    private final Map<String, byte[]> memoryTiles = new ConcurrentHashMap<>();
    // tiles that may exist in the temp folder. If a tile is also in memory, the memory one is the latest.
    private final Set<String> diskTiles = ConcurrentHashMap.newKeySet();
    private final AtomicLong memoryBytes = new AtomicLong(0);
    private final long memoryBudget = (long) (Runtime.getRuntime().maxMemory() * MEMORY_BUDGET_RATIO);

    public TempTileCache() {
        MemoryMonitor.setMemoryReleaser(this::releaseMemory);
    }

    private boolean isEnabled() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        return memoryBudget > 0 && !globalOptions.isLeaveTemp() && !globalOptions.isContinue() && !globalOptions.isModify();
    }

    private static double getAvailableHeapRatio() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return (double) (runtime.maxMemory() - usedMemory) / runtime.maxMemory();
    }

    public boolean exists(String filePath) {
        if (!isEnabled()) {
            return FileUtils.isFileExists(filePath);
        }
        return memoryTiles.containsKey(filePath) || diskTiles.contains(filePath);
    }

    public void save(TerrainMesh mesh, String filePath) throws IOException {
        if (!isEnabled()) {
            mesh.saveFile(filePath);
            return;
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(64 * 1024);
        try (BigEndianDataOutputStream dataOutputStream = new BigEndianDataOutputStream(byteArrayOutputStream)) {
            mesh.saveDataOutputStream(dataOutputStream);
        }
        byte[] bytes = byteArrayOutputStream.toByteArray();

        byte[] previous = memoryTiles.remove(filePath);
        if (previous != null) {
            memoryBytes.addAndGet(-previous.length);
        }

        double availableHeapRatio = getAvailableHeapRatio();
        if (availableHeapRatio < MIN_AVAILABLE_RATIO_TO_KEEP && memoryBytes.get() > 0) {
            spillAll();
        }
        if (memoryBytes.get() + bytes.length > memoryBudget || availableHeapRatio < MIN_AVAILABLE_RATIO_TO_CACHE) {
            writeToDisk(filePath, bytes);
            return;
        }

        // an older file of the tile in the temp folder is kept until the tile is removed. The memory one is loaded first.
        memoryTiles.put(filePath, bytes);
        memoryBytes.addAndGet(bytes.length);
    }

    /**
     * Writes the in-memory tiles to the temp folder too, keeping them in memory. Returns the number of written tiles.
     */
    public int checkpoint() throws IOException {
        if (!isEnabled()) {
            return 0;
        }
        int writtenCount = 0;
        for (Map.Entry<String, byte[]> entry : memoryTiles.entrySet()) {
            writeToDisk(entry.getKey(), entry.getValue());
            writtenCount++;
        }
        return writtenCount;
    }

    private void writeToDisk(String filePath, byte[] bytes) throws IOException {
        String foldersPath = FileUtils.removeFileNameFromPath(filePath);
        FileUtils.createAllFoldersIfNoExist(foldersPath);
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(filePath))) {
            outputStream.write(bytes);
        }
        diskTiles.add(filePath);
    }

    private void spillAll() throws IOException {
        int spilledCount = 0;
        for (Map.Entry<String, byte[]> entry : memoryTiles.entrySet()) {
            writeToDisk(entry.getKey(), entry.getValue());
            if (memoryTiles.remove(entry.getKey(), entry.getValue())) {
                memoryBytes.addAndGet(-entry.getValue().length);
            }
            spilledCount++;
        }
        log.debug("[TempTileCache] Low heap memory, spilled {} in-memory tiles to the temp folder.", spilledCount);
    }

    /**
     * Spills all in-memory tiles to the temp folder. Returns the released bytes.
     */
    public long releaseMemory() {
        long previousBytes = memoryBytes.get();
        if (previousBytes <= 0) {
            return 0;
        }
        try {
            spillAll();
        } catch (IOException e) {
            log.error("[TempTileCache] Failed to spill the in-memory tiles.", e);
        }
        return previousBytes - memoryBytes.get();
    }

    public TerrainMesh load(String filePath) throws IOException {
        byte[] bytes = memoryTiles.get(filePath);
        InputStream inputStream = bytes != null ? new ByteArrayInputStream(bytes) : new BufferedInputStream(new FileInputStream(filePath));
        TerrainMesh mesh = new TerrainMesh();
        try (BigEndianDataInputStream dataInputStream = new BigEndianDataInputStream(inputStream)) {
            mesh.loadDataInputStream(dataInputStream);
        }
        return mesh;
    }

    /**
     * Removes the tile from memory. Returns true if the tile may still exist on disk.
     */
    public boolean remove(String filePath) {
        byte[] previous = memoryTiles.remove(filePath);
        if (previous != null) {
            memoryBytes.addAndGet(-previous.length);
        }
        return !isEnabled() || diskTiles.remove(filePath);
    }

    public void removeByPrefix(String pathPrefix) {
        memoryTiles.entrySet().removeIf(entry -> {
            if (!entry.getKey().startsWith(pathPrefix)) {
                return false;
            }
            memoryBytes.addAndGet(-entry.getValue().length);
            return true;
        });
        diskTiles.removeIf(path -> path.startsWith(pathPrefix));
    }

    public void clear() {
        memoryTiles.clear();
        diskTiles.clear();
        memoryBytes.set(0);
    }
}
