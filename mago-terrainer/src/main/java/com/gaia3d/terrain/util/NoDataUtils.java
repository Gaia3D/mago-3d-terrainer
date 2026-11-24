package com.gaia3d.terrain.util;

import lombok.experimental.UtilityClass;

/**
 * The NODATA rules shared by the raster preprocessing and the elevation sampling.
 * A sample is NODATA when it is not finite, when it matches the NODATA tag of its raster or the configured NODATA value,
 * or when it is not a plausible height (fill values such as -32768 or -3.4e38 of rasters without a NODATA tag).
 */
@UtilityClass
public class NoDataUtils {
    // the highest and lowest terrain heights of the supported bodies are about +/- 11 km (Earth, Moon), 21 km for Mars
    public static final double MAX_ABS_HEIGHT = 32000.0;
    // the geoid undulation of the Earth is within about +/- 110 m
    public static final double MAX_ABS_GEOID_HEIGHT = 1000.0;

    public static boolean isNoData(double sample, double sourceNoDataValue, double configuredNoDataValue) {
        return !Double.isFinite(sample)
                || Math.abs(sample) >= MAX_ABS_HEIGHT
                || matchesNoData(sample, sourceNoDataValue)
                || matchesNoData(sample, configuredNoDataValue);
    }

    /**
     * Returns true if the geoid height can be added to a DEM height.
     * The resampled geoid has its NODATA (or background) value outside the geoid grid, and mixed values at its border.
     */
    public static boolean isValidGeoidHeight(double geoidHeight, double geoidNoDataValue, double configuredNoDataValue) {
        return Double.isFinite(geoidHeight)
                && Math.abs(geoidHeight) < MAX_ABS_GEOID_HEIGHT
                && !matchesNoData(geoidHeight, geoidNoDataValue)
                && !matchesNoData(geoidHeight, configuredNoDataValue);
    }

    /**
     * The NODATA tags are often stored as float (e.g. -3.40282e+38), so the values are also compared as float.
     */
    public static boolean matchesNoData(double sample, double noDataValue) {
        if (Double.isNaN(noDataValue)) {
            return false;
        }
        return Double.compare(sample, noDataValue) == 0 || Float.compare((float) sample, (float) noDataValue) == 0;
    }

    /**
     * Parses a NODATA value. "nan" is accepted in any case.
     */
    public static double parseNoDataValue(String text) {
        String trimmed = text.trim();
        if (trimmed.equalsIgnoreCase("nan")) {
            return Double.NaN;
        }
        return Double.parseDouble(trimmed);
    }
}
