package com.gaia3d.terrain.util;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("default")
class NoDataUtilsTest {

    @Test
    void sampleMatchingTheConfiguredValueIsNoDataEvenWhenTheTagIsNaN() {
        // a DEM tagged "nan" whose voids are filled with -9999 (#30)
        assertTrue(NoDataUtils.isNoData(-9999.0, Double.NaN, -9999.0));
        assertTrue(NoDataUtils.isNoData(Double.NaN, Double.NaN, -9999.0));
        assertFalse(NoDataUtils.isNoData(-9998.0, Double.NaN, -9999.0));
        assertFalse(NoDataUtils.isNoData(125.5, Double.NaN, -9999.0));
    }

    @Test
    void implausibleHeightsAreNoData() {
        // fill values of rasters without a NODATA tag
        assertTrue(NoDataUtils.isNoData(-32768.0, Double.NaN, -9999.0));
        assertTrue(NoDataUtils.isNoData(-3.4028234663852886E38, Double.NaN, -9999.0));
        assertTrue(NoDataUtils.isNoData(Double.POSITIVE_INFINITY, Double.NaN, -9999.0));
        assertFalse(NoDataUtils.isNoData(-10994.0, Double.NaN, -9999.0));
        assertFalse(NoDataUtils.isNoData(8848.0, Double.NaN, -9999.0));
    }

    @Test
    void float32NoDataTagMatchesTheDoubleSample() {
        float tag = -3.40282e+38f;
        assertTrue(NoDataUtils.matchesNoData(tag, (double) -3.40282e+38));
        assertTrue(NoDataUtils.matchesNoData(-32767.0, -32767.0));
        assertFalse(NoDataUtils.matchesNoData(1.0, Double.NaN));
    }

    @Test
    void geoidHeightOutsideTheGeoidGridIsInvalid() {
        assertTrue(NoDataUtils.isValidGeoidHeight(25.3, Double.NaN, -9999.0));
        assertTrue(NoDataUtils.isValidGeoidHeight(-106.0, -32767.0, -9999.0));
        // background values and the values mixed with them at the geoid border
        assertFalse(NoDataUtils.isValidGeoidHeight(-9999.0, Double.NaN, -9999.0));
        assertFalse(NoDataUtils.isValidGeoidHeight(-32767.0, -32767.0, -9999.0));
        assertFalse(NoDataUtils.isValidGeoidHeight(-4987.0, Double.NaN, -9999.0));
        assertFalse(NoDataUtils.isValidGeoidHeight(Double.NaN, Double.NaN, -9999.0));
    }

    @Test
    void parsesNanInAnyCase() {
        assertTrue(Double.isNaN(NoDataUtils.parseNoDataValue("nan")));
        assertTrue(Double.isNaN(NoDataUtils.parseNoDataValue("NaN")));
        assertTrue(Double.isNaN(NoDataUtils.parseNoDataValue(" NAN ")));
        assertEquals(-9999.0, NoDataUtils.parseNoDataValue("-9999"));
        assertEquals(0.0, NoDataUtils.parseNoDataValue("0"));
    }
}
