package com.gaia3d.terrain.tile.writer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FileSystemWriter implements TerrainWriter {
    private final String outputBaseDir;

    public FileSystemWriter(String outputBaseDir) {
        this.outputBaseDir = outputBaseDir;
    }

    @Override
    public void init() throws IOException {
        File dir = new File(outputBaseDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Override
    public void writeTile(int z, long x, long y, byte[] data) throws IOException {
        String tileDir = outputBaseDir + File.separator + z + File.separator + x;
        File dir = new File(tileDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File tileFile = new File(dir, y + ".terrain");
        try (FileOutputStream fos = new FileOutputStream(tileFile)) {
            fos.write(data);
        }
    }

    @Override
    public void writeBatch(List<TerrainWriteRequest> requests) throws IOException {
        if (requests == null) {
            return;
        }
        for (TerrainWriteRequest request : requests) {
            writeTile(request.getZ(), request.getX(), request.getY(), request.getData());
        }
    }

    @Override
    public void writeMetadata(double minX, double minY, double maxX, double maxY, 
                               int minLevel, int maxLevel, String source, String layerJson) throws IOException {
        // 在散列文件模式下，仅保存 layer.json 文件
        File file = new File(outputBaseDir, "layer.json");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(layerJson.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void close() throws IOException {
        // No-op for file system
    }
}
