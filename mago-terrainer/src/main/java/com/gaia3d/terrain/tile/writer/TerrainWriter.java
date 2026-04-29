package com.gaia3d.terrain.tile.writer;

import java.io.IOException;
import java.util.List;

public interface TerrainWriter {
    void init() throws IOException;
    void writeTile(int z, long x, long y, byte[] data) throws IOException;

    default void writeBatch(List<TerrainWriteRequest> requests) throws IOException {
        if (requests == null) {
            return;
        }
        for (TerrainWriteRequest request : requests) {
            writeTile(request.getZ(), request.getX(), request.getY(), request.getData());
        }
    }
    
    /**
     * 写入元数据到 infos 表
     */
    void writeMetadata(double minX, double minY, double maxX, double maxY, 
                       int minLevel, int maxLevel, String source, String layerJson) throws IOException;
    
    void close() throws IOException;
}
