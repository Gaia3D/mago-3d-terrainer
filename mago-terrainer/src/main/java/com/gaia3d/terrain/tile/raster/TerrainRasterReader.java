package com.gaia3d.terrain.tile.raster;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class TerrainRasterReader {
    private static final int FLOATS_PER_CHUNK = 8192;

    public TerrainRasterData read(Path path) throws IOException {
        long fileSize = Files.size(path);
        if (fileSize < TerrainRasterFormat.HEADER_SIZE_BYTES) {
            throw new IOException("Terrain raster file is smaller than the header: " + path);
        }

        byte[] headerBytes = readHeader(path);
        ByteBuffer header = ByteBuffer.wrap(headerBytes).order(TerrainRasterFormat.BYTE_ORDER);

        byte[] magic = new byte[TerrainRasterFormat.MAGIC.length];
        header.get(magic);
        if (!Arrays.equals(TerrainRasterFormat.MAGIC, magic)) {
            throw new IOException("Invalid terrain raster magic: " + path);
        }

        int version = header.getInt();
        if (version != TerrainRasterFormat.VERSION) {
            throw new IOException("Unsupported terrain raster version: " + version);
        }

        int headerSize = header.getInt();
        if (headerSize != TerrainRasterFormat.HEADER_SIZE_BYTES) {
            throw new IOException("Unsupported terrain raster header size: " + headerSize);
        }

        int width = header.getInt();
        int height = header.getInt();
        double minLongitude = header.getDouble();
        double minLatitude = header.getDouble();
        double maxLongitude = header.getDouble();
        double maxLatitude = header.getDouble();
        float noDataValue = header.getFloat();
        int originalWidth = header.getInt();
        int originalHeight = header.getInt();

        long sampleCount = (long) width * (long) height;
        if (width <= 0 || height <= 0 || sampleCount > Integer.MAX_VALUE) {
            throw new IOException("Invalid terrain raster dimensions: width=" + width + ", height=" + height);
        }

        long expectedSize = TerrainRasterFormat.HEADER_SIZE_BYTES + Math.multiplyExact(sampleCount, (long) TerrainRasterFormat.FLOAT_BYTES);
        if (fileSize != expectedSize) {
            throw new IOException("Invalid terrain raster file size: expected=" + expectedSize + ", actual=" + fileSize);
        }

        float[] elevations = readPayload(path, (int) sampleCount);

        return new TerrainRasterData(width, height, originalWidth, originalHeight,
                minLongitude, minLatitude, maxLongitude, maxLatitude, noDataValue, elevations);
    }

    private byte[] readHeader(Path path) throws IOException {
        try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {
            byte[] headerBytes = inputStream.readNBytes(TerrainRasterFormat.HEADER_SIZE_BYTES);
            if (headerBytes.length != TerrainRasterFormat.HEADER_SIZE_BYTES) {
                throw new IOException("Terrain raster header is truncated: " + path);
            }
            return headerBytes;
        }
    }

    private float[] readPayload(Path path, int sampleCount) throws IOException {
        float[] elevations = new float[sampleCount];
        byte[] chunkBytes = new byte[FLOATS_PER_CHUNK * TerrainRasterFormat.FLOAT_BYTES];

        try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {
            try {
                inputStream.skipNBytes(TerrainRasterFormat.HEADER_SIZE_BYTES);
            } catch (IOException exception) {
                throw new IOException("Unable to skip terrain raster header: " + path);
            }

            int offset = 0;
            while (offset < sampleCount) {
                int floatsToRead = Math.min(FLOATS_PER_CHUNK, sampleCount - offset);
                int bytesToRead = floatsToRead * TerrainRasterFormat.FLOAT_BYTES;
                int bytesRead = inputStream.readNBytes(chunkBytes, 0, bytesToRead);
                if (bytesRead != bytesToRead) {
                    throw new IOException("Terrain raster payload is truncated: " + path);
                }

                ByteBuffer payload = ByteBuffer.wrap(chunkBytes, 0, bytesRead).order(TerrainRasterFormat.BYTE_ORDER);
                for (int i = 0; i < floatsToRead; i++) {
                    elevations[offset++] = payload.getFloat();
                }
            }
        }

        return elevations;
    }
}
