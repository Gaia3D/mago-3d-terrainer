package com.gaia3d.terrain.tile.raster;

import java.util.Arrays;
import java.util.Objects;

public final class TerrainRasterData {
    private final int width;
    private final int height;
    private final int originalWidth;
    private final int originalHeight;
    private final double minLongitude;
    private final double minLatitude;
    private final double maxLongitude;
    private final double maxLatitude;
    private final float noDataValue;
    private final float[] elevations;

    public TerrainRasterData(int width, int height, double minLongitude, double minLatitude, double maxLongitude, double maxLatitude, float noDataValue, float[] elevations) {
        this(width, height, width, height, minLongitude, minLatitude, maxLongitude, maxLatitude, noDataValue, elevations);
    }

    public TerrainRasterData(int width, int height, int originalWidth, int originalHeight, double minLongitude, double minLatitude, double maxLongitude, double maxLatitude, float noDataValue, float[] elevations) {
        this(width, height, originalWidth, originalHeight, minLongitude, minLatitude, maxLongitude, maxLatitude, noDataValue, elevations, true);
    }

    private TerrainRasterData(int width, int height, int originalWidth, int originalHeight, double minLongitude, double minLatitude, double maxLongitude, double maxLatitude, float noDataValue, float[] elevations, boolean copyElevations) {
        validate(width, height, originalWidth, originalHeight, minLongitude, minLatitude, maxLongitude, maxLatitude, elevations);
        this.width = width;
        this.height = height;
        this.originalWidth = originalWidth == 0 ? width : originalWidth;
        this.originalHeight = originalHeight == 0 ? height : originalHeight;
        this.minLongitude = minLongitude;
        this.minLatitude = minLatitude;
        this.maxLongitude = maxLongitude;
        this.maxLatitude = maxLatitude;
        this.noDataValue = noDataValue;
        this.elevations = copyElevations ? Arrays.copyOf(elevations, elevations.length) : elevations;
    }

    static TerrainRasterData takeOwnership(int width, int height, int originalWidth, int originalHeight,
                                           double minLongitude, double minLatitude, double maxLongitude, double maxLatitude,
                                           float noDataValue, float[] elevations) {
        return new TerrainRasterData(width, height, originalWidth, originalHeight,
                minLongitude, minLatitude, maxLongitude, maxLatitude, noDataValue, elevations, false);
    }

    private static void validate(int width, int height, int originalWidth, int originalHeight, double minLongitude, double minLatitude, double maxLongitude, double maxLatitude, float[] elevations) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Raster dimensions must be positive.");
        }
        if (originalWidth < 0 || originalHeight < 0) {
            throw new IllegalArgumentException("Original raster dimensions cannot be negative.");
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

    public float[] elevations() {
        return Arrays.copyOf(elevations, elevations.length);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int originalWidth() {
        return originalWidth;
    }

    public int originalHeight() {
        return originalHeight;
    }

    public double minLongitude() {
        return minLongitude;
    }

    public double minLatitude() {
        return minLatitude;
    }

    public double maxLongitude() {
        return maxLongitude;
    }

    public double maxLatitude() {
        return maxLatitude;
    }

    public float noDataValue() {
        return noDataValue;
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
