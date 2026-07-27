package com.gaia3d.terrain.tile.raster;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TerrainRasterWriter {
    private static final int FLOATS_PER_CHUNK = 8192;

    public void write(Path path, TerrainRasterData data) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(path))) {
            outputStream.write(createHeader(data).array());
            writePayload(outputStream, data);
        }
    }

    private ByteBuffer createHeader(TerrainRasterData data) {
        ByteBuffer header = ByteBuffer.allocate(TerrainRasterFormat.HEADER_SIZE_BYTES).order(TerrainRasterFormat.BYTE_ORDER);
        header.put(TerrainRasterFormat.MAGIC);
        header.putInt(TerrainRasterFormat.VERSION);
        header.putInt(TerrainRasterFormat.HEADER_SIZE_BYTES);
        header.putInt(data.width());
        header.putInt(data.height());
        header.putDouble(data.minLongitude());
        header.putDouble(data.minLatitude());
        header.putDouble(data.maxLongitude());
        header.putDouble(data.maxLatitude());
        header.putFloat(data.noDataValue());
        header.putInt(0);
        return header;
    }

    private void writePayload(BufferedOutputStream outputStream, TerrainRasterData data) throws IOException {
        ByteBuffer payload = ByteBuffer.allocate(FLOATS_PER_CHUNK * TerrainRasterFormat.FLOAT_BYTES).order(TerrainRasterFormat.BYTE_ORDER);

        for (int i = 0; i < data.getSampleCount(); i++) {
            payload.putFloat(data.getElevationAtIndex(i));
            if (!payload.hasRemaining()) {
                outputStream.write(payload.array());
                payload.clear();
            }
        }

        int remainingBytes = payload.position();
        if (remainingBytes > 0) {
            outputStream.write(payload.array(), 0, remainingBytes);
        }
    }
}
