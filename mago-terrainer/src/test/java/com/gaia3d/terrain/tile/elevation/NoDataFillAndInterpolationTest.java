package com.gaia3d.terrain.tile.elevation;

import com.gaia3d.command.GlobalOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("default")
class NoDataFillAndInterpolationTest {
    private static final double NO_DATA = -9999.0;

    @BeforeEach
    void setUp() {
        GlobalOptions.recreateInstance();
        GlobalOptions.getInstance().setNoDataValue(NO_DATA);
    }

    @AfterEach
    void tearDown() {
        GlobalOptions.recreateInstance();
    }

    @Test
    void pointIsNoDataWhenItsNearestSampleIsNoData() {
        TerrainElevationData data = new TerrainElevationData(null);
        // value01 is the next row, value10 the next column
        assertEquals(NO_DATA, data.interpolateValidSamples(NO_DATA, 200.0, 300.0, 400.0, 0.2, 0.2));
        assertEquals(NO_DATA, data.interpolateValidSamples(100.0, 200.0, NO_DATA, 400.0, 0.8, 0.2));
    }

    @Test
    void validSamplesAreInterpolatedWithRenormalizedWeights() {
        TerrainElevationData data = new TerrainElevationData(null);
        // the next column is NODATA: only the first column is used, NODATA is never mixed into the height (#30)
        assertEquals(150.0, data.interpolateValidSamples(100.0, 200.0, NO_DATA, NO_DATA, 0.4, 0.5), 1.0e-9);
        // not the upper-left sample, as before
        assertEquals(200.0, data.interpolateValidSamples(NO_DATA, 200.0, NO_DATA, 200.0, 0.4, 0.9), 1.0e-9);
    }

    @Test
    void nanNoDataValueIsDetectedInTheInterpolation() {
        GlobalOptions.getInstance().setNoDataValue(Double.NaN);
        TerrainElevationData data = new TerrainElevationData(null);
        assertTrue(Double.isNaN(data.interpolateValidSamples(Double.NaN, 200.0, 300.0, 400.0, 0.1, 0.1)));
        assertEquals(200.0, data.interpolateValidSamples(Double.NaN, 200.0, Double.NaN, Double.NaN, 0.1, 0.9), 1.0e-9);
    }

    /**
     * 3 x 2 samples, the first row is the north row (lat 10), the second row lat 0. Longitudes 0, 10, 20.
     */
    private static GeoidHeightSampler createGeoid(float[] heights) {
        return GeoidHeightSampler.forGrid(heights, 3, 2, 0.0, 10.0, 10.0, -10.0);
    }

    @Test
    void geoidHeightIsInterpolatedFromNorthToSouthRows() {
        GeoidHeightSampler geoid = createGeoid(new float[]{1, 2, 3, 4, 5, 6});
        assertEquals(2.0, geoid.getHeight(10.0, 10.0), 1.0e-9);
        assertEquals(5.0, geoid.getHeight(10.0, 0.0), 1.0e-9);
        assertEquals(3.0, geoid.getHeight(5.0, 5.0), 1.0e-9);
    }

    @Test
    void invalidGeoidSamplesAreIgnored() {
        GeoidHeightSampler geoid = createGeoid(new float[]{1, -9999, 3, 4, -32767, 6});
        assertEquals(2.5, geoid.getHeight(5.0, 5.0), 1.0e-9);
        GeoidHeightSampler empty = createGeoid(new float[]{Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN});
        assertEquals(0.0, empty.getHeight(5.0, 5.0), 1.0e-9);
    }

    @Test
    void outsideTheGridTheNearestEdgeIsUsed() {
        GeoidHeightSampler geoid = createGeoid(new float[]{1, 2, 3, 4, 5, 6});
        assertEquals(3.0, geoid.getHeight(100.0, 10.0), 1.0e-9);
        assertEquals(4.0, geoid.getHeight(0.0, -50.0), 1.0e-9);
        // the longitude is wrapped around the grid
        assertEquals(2.0, geoid.getHeight(370.0, 10.0), 1.0e-9);
    }
}
