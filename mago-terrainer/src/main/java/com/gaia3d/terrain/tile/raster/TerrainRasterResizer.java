package com.gaia3d.terrain.tile.raster;

public final class TerrainRasterResizer {
    public TerrainRasterData resize(TerrainRasterData source, int targetWidth, int targetHeight) {
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new IllegalArgumentException("Target dimensions must be positive.");
        }
        if (source.width() == targetWidth && source.height() == targetHeight) {
            return source;
        }

        float[] result = new float[Math.multiplyExact(targetWidth, targetHeight)];
        for (int targetY = 0; targetY < targetHeight; targetY++) {
            double sourceY = ((targetY + 0.5) * source.height() / targetHeight) - 0.5;
            for (int targetX = 0; targetX < targetWidth; targetX++) {
                double sourceX = ((targetX + 0.5) * source.width() / targetWidth) - 0.5;
                result[targetY * targetWidth + targetX] = sample(source, sourceX, sourceY);
            }
        }

        return new TerrainRasterData(targetWidth, targetHeight, source.originalWidth(), source.originalHeight(),
                source.minLongitude(), source.minLatitude(), source.maxLongitude(), source.maxLatitude(),
                source.noDataValue(), result);
    }

    private float sample(TerrainRasterData source, double x, double y) {
        x = Math.max(0.0, Math.min(source.width() - 1.0, x));
        y = Math.max(0.0, Math.min(source.height() - 1.0, y));
        int nearestX = clamp((int) Math.round(x), 0, source.width() - 1);
        int nearestY = clamp((int) Math.round(y), 0, source.height() - 1);
        if (isNoData(source, source.getElevation(nearestX, nearestY))) {
            return source.noDataValue();
        }

        int x0 = clamp((int) Math.floor(x), 0, source.width() - 1);
        int y0 = clamp((int) Math.floor(y), 0, source.height() - 1);
        int x1 = Math.min(x0 + 1, source.width() - 1);
        int y1 = Math.min(y0 + 1, source.height() - 1);
        double fx = Math.max(0.0, Math.min(1.0, x - Math.floor(x)));
        double fy = Math.max(0.0, Math.min(1.0, y - Math.floor(y)));

        float[] values = {
                source.getElevation(x0, y0), source.getElevation(x1, y0),
                source.getElevation(x0, y1), source.getElevation(x1, y1)
        };
        double[] weights = {(1.0 - fx) * (1.0 - fy), fx * (1.0 - fy), (1.0 - fx) * fy, fx * fy};
        double weightedValue = 0.0;
        double validWeight = 0.0;
        for (int i = 0; i < values.length; i++) {
            if (!isNoData(source, values[i])) {
                weightedValue += values[i] * weights[i];
                validWeight += weights[i];
            }
        }
        return validWeight == 0.0 ? source.noDataValue() : (float) (weightedValue / validWeight);
    }

    private boolean isNoData(TerrainRasterData source, float value) {
        return !Float.isFinite(value) || Float.compare(value, source.noDataValue()) == 0;
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
