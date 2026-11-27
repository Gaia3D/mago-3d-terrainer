package com.gaia3d.terrain.tile.generation;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.io.BigEndianDataInputStream;
import com.gaia3d.io.BigEndianDataOutputStream;
import com.gaia3d.terrain.structure.TerrainMesh;
import com.gaia3d.terrain.tile.core.TileIndices;
import com.gaia3d.terrain.util.MemoryMonitor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Stores the intermediate tile meshes (temp tiles) of the tiling.
 * The serialized tiles are kept in memory while the heap allows it, and are written to one pack file per depth
 * ({@link TempTilePack}) when the memory budget is exceeded, when the heap is low, and at the end of each depth (checkpoint).
 */
@Slf4j
public class TempTileCache {
    // ratio of the max heap that the in-memory tiles can use. "-Dmago.tempTileCacheRatio=0" writes every tile to the pack.
    private static final double MEMORY_BUDGET_RATIO = Double.parseDouble(System.getProperty("mago.tempTileCacheRatio", "0.25"));
    // the refinement stops when the free heap is under 10% (MemoryMonitor), so the cache yields the heap well before that.
    private static final double MIN_AVAILABLE_RATIO_TO_CACHE = 0.35;
    private static final double MIN_AVAILABLE_RATIO_TO_KEEP = 0.25;

    // the latest bytes of the tiles kept in memory
    private final Map<Long, byte[]> memoryTiles = new HashMap<>();
    // the in-memory tiles whose latest bytes are also in the pack
    private final Set<Long> writtenTiles = new HashSet<>();
    private final Map<Integer, TempTilePack> packs = new HashMap<>();
    private long memoryBytes = 0;
    private final long memoryBudget = (long) (Runtime.getRuntime().maxMemory() * MEMORY_BUDGET_RATIO);

    public TempTileCache() {
        MemoryMonitor.setMemoryReleaser(this::releaseMemory);
    }

    private static String getTempPath() {
        return GlobalOptions.getInstance().getTileTempPath();
    }

    private static int depthOf(long key) {
        return (int) (key >>> 58);
    }

    private static double getAvailableHeapRatio() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return (double) (runtime.maxMemory() - usedMemory) / runtime.maxMemory();
    }

    private TempTilePack pack(int depth) throws IOException {
        TempTilePack pack = packs.get(depth);
        if (pack == null) {
            pack = TempTilePack.open(getTempPath(), depth);
            packs.put(depth, pack);
        }
        return pack;
    }

    public boolean exists(TileIndices tileIndices) throws IOException {
        long key = tileIndices.toCacheKey();
        return memoryTiles.containsKey(key) || pack(tileIndices.getL()).contains(key);
    }

    /**
     * Returns true if there are temp tiles of the depth (in memory, or in a pack left by a previous run).
     */
    public boolean hasTiles(int depth) throws IOException {
        for (long key : memoryTiles.keySet()) {
            if (depthOf(key) == depth) {
                return true;
            }
        }
        if (!packs.containsKey(depth) && !TempTilePack.existsOnDisk(getTempPath(), depth)) {
            return false;
        }
        return !pack(depth).isEmpty();
    }

    /**
     * Returns the tiles of the depth, sorted by key.
     */
    public List<TileIndices> listTiles(int depth) throws IOException {
        Set<Long> keys = new TreeSet<>(pack(depth).keys());
        for (long key : memoryTiles.keySet()) {
            if (depthOf(key) == depth) {
                keys.add(key);
            }
        }
        List<TileIndices> tiles = new ArrayList<>(keys.size());
        for (long key : keys) {
            tiles.add(new TileIndices((int) ((key >>> 29) & 0x1FFFFFFFL), (int) (key & 0x1FFFFFFFL), depthOf(key)));
        }
        return tiles;
    }

    public void save(TerrainMesh mesh, TileIndices tileIndices) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(64 * 1024);
        try (BigEndianDataOutputStream dataOutputStream = new BigEndianDataOutputStream(byteArrayOutputStream)) {
            mesh.saveDataOutputStream(dataOutputStream);
        }
        byte[] bytes = byteArrayOutputStream.toByteArray();
        long key = tileIndices.toCacheKey();

        removeFromMemory(key);
        double availableHeapRatio = getAvailableHeapRatio();
        if (availableHeapRatio < MIN_AVAILABLE_RATIO_TO_KEEP && memoryBytes > 0) {
            spillAll();
        }
        if (memoryBytes + bytes.length > memoryBudget || availableHeapRatio < MIN_AVAILABLE_RATIO_TO_CACHE) {
            pack(tileIndices.getL()).write(key, bytes);
            return;
        }

        // an older record of the tile in the pack is ignored, the memory one is read first
        memoryTiles.put(key, bytes);
        memoryBytes += bytes.length;
    }

    public TerrainMesh load(TileIndices tileIndices) throws IOException {
        long key = tileIndices.toCacheKey();
        byte[] bytes = memoryTiles.get(key);
        if (bytes == null) {
            bytes = pack(tileIndices.getL()).read(key);
        }
        if (bytes == null) {
            throw new IOException("No temp tile: " + tileIndices.getString());
        }
        TerrainMesh mesh = new TerrainMesh();
        try (BigEndianDataInputStream dataInputStream = new BigEndianDataInputStream(new ByteArrayInputStream(bytes))) {
            mesh.loadDataInputStream(dataInputStream);
        }
        return mesh;
    }

    public void remove(TileIndices tileIndices) throws IOException {
        long key = tileIndices.toCacheKey();
        removeFromMemory(key);
        pack(tileIndices.getL()).remove(key);
    }

    private void removeFromMemory(long key) {
        byte[] previous = memoryTiles.remove(key);
        if (previous != null) {
            memoryBytes -= previous.length;
        }
        writtenTiles.remove(key);
    }

    /**
     * Removes the temp tiles of the depth, and deletes its pack.
     */
    public void removeDepth(int depth) {
        Iterator<Map.Entry<Long, byte[]>> iterator = memoryTiles.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, byte[]> entry = iterator.next();
            if (depthOf(entry.getKey()) == depth) {
                memoryBytes -= entry.getValue().length;
                writtenTiles.remove(entry.getKey());
                iterator.remove();
            }
        }
        TempTilePack pack = packs.remove(depth);
        if (pack != null) {
            pack.delete();
        } else {
            try {
                TempTilePack.open(getTempPath(), depth).delete();
            } catch (IOException e) {
                log.warn("Failed to delete the temp tiles of the depth {}", depth, e);
            }
        }
    }

    /**
     * Writes the in-memory tiles to the packs too (keeping them in memory) and the indexes of the packs,
     * so "-continue" can resume from them. Returns the number of written tiles.
     */
    public int checkpoint() throws IOException {
        int writtenCount = 0;
        for (Map.Entry<Long, byte[]> entry : memoryTiles.entrySet()) {
            if (writtenTiles.add(entry.getKey())) {
                pack(depthOf(entry.getKey())).write(entry.getKey(), entry.getValue());
                writtenCount++;
            }
        }
        for (TempTilePack pack : packs.values()) {
            pack.writeIndex();
        }
        return writtenCount;
    }

    private void spillAll() throws IOException {
        int spilledCount = 0;
        for (Map.Entry<Long, byte[]> entry : memoryTiles.entrySet()) {
            if (!writtenTiles.contains(entry.getKey())) {
                pack(depthOf(entry.getKey())).write(entry.getKey(), entry.getValue());
            }
            spilledCount++;
        }
        memoryTiles.clear();
        writtenTiles.clear();
        memoryBytes = 0;
        log.debug("[TempTileCache] Low heap memory, spilled {} in-memory tiles to the temp packs.", spilledCount);
    }

    /**
     * Writes all in-memory tiles to the packs and releases them. Returns the released bytes.
     */
    public long releaseMemory() {
        long previousBytes = memoryBytes;
        if (previousBytes <= 0) {
            return 0;
        }
        try {
            spillAll();
        } catch (IOException e) {
            log.error("[TempTileCache] Failed to spill the in-memory tiles.", e);
        }
        return previousBytes - memoryBytes;
    }

    /**
     * Closes the packs. With "-leaveTemp", the remaining tiles and the indexes are written first, so the packs can be read.
     */
    public void clear() {
        if (GlobalOptions.getInstance().isLeaveTemp()) {
            try {
                checkpoint();
            } catch (IOException e) {
                log.warn("[TempTileCache] Failed to write the temp tile indexes.", e);
            }
        }
        for (TempTilePack pack : packs.values()) {
            pack.close();
        }
        packs.clear();
        memoryTiles.clear();
        writtenTiles.clear();
        memoryBytes = 0;
    }
}
