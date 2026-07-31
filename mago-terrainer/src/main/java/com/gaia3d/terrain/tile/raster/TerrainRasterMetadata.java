package com.gaia3d.terrain.tile.raster;

public record TerrainRasterMetadata(
        int width,
        int height,
        int originalWidth,
        int originalHeight,
        double minLongitude,
        double minLatitude,
        double maxLongitude,
        double maxLatitude,
        float noDataValue) {

    public long payloadSizeBytes() {
        return (long) width * height * Float.BYTES;
    }
}
