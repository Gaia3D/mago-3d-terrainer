package com.gaia3d.terrain.tile.raster;

import java.util.Arrays;
import java.util.Objects;

public record TerrainRasterData(int width, int height, double minLongitude, double minLatitude, double maxLongitude, double maxLatitude, float noDataValue, float[] elevations) {
    public TerrainRasterData(int width, int height, double minLongitude, double minLatitude, double maxLongitude, double maxLatitude, float noDataValue, float[] elevations) {
        validate(width, height, minLongitude, minLatitude, maxLongitude, maxLatitude, elevations);
        this.width = width;
        this.height = height;
        this.minLongitude = minLongitude;
        this.minLatitude = minLatitude;
        this.maxLongitude = maxLongitude;
        this.maxLatitude = maxLatitude;
        this.noDataValue = noDataValue;
        this.elevations = Arrays.copyOf(elevations, elevations.length);
    }

    private static void validate(int width, int height, double minLongitude, double minLatitude, double maxLongitude, double maxLatitude, float[] elevations) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Raster dimensions must be positive.");
        }

        long sampleCount = (long) width * (long) height;
        if (sampleCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Raster sample count exceeds Java array limit: " + sampleCount);
        }

        Objects.requireNonNull(elevations, "elevations");
        if (elevations.length != (int) sampleCount) {
            throw new IllegalArgumentException("Elevation sample count does not match dimensions: expected=" + sampleCount + ", actual=" + elevations.length);
        }

        if (!Double.isFinite(minLongitude) || !Double.isFinite(minLatitude) || !Double.isFinite(maxLongitude) || !Double.isFinite(maxLatitude)) {
            throw new IllegalArgumentException("Raster bounds must be finite.");
        }
        if (minLongitude >= maxLongitude || minLatitude >= maxLatitude) {
            throw new IllegalArgumentException("Raster bounds must have positive width and height.");
        }
    }

    @Override
    public float[] elevations() {
        return Arrays.copyOf(elevations, elevations.length);
    }

    public float getElevation(int column, int row) {
        if (column < 0 || column >= width || row < 0 || row >= height) {
            throw new IndexOutOfBoundsException("column=" + column + ", row=" + row);
        }
        return elevations[row * width + column];
    }

    int getSampleCount() {
        return elevations.length;
    }

    float getElevationAtIndex(int index) {
        return elevations[index];
    }
}
