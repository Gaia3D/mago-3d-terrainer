package com.gaia3d.terrain.tile.core;

import com.gaia3d.terrain.structure.GeographicExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeographicTerrainTileRasterTest {

    @Test
    void bilinearSamplingNormalizesVirtualAntimeridianLongitude() {
        GeographicExtension extension = new GeographicExtension();
        extension.setDegrees(179.0, 0.0, 0.0, 180.0, 1.0, 0.0);

        GeographicTerrainTileRaster raster = new GeographicTerrainTileRaster();
        raster.setGeographicExtension(extension);
        raster.setRasterWidth(2);
        raster.setRasterHeight(2);
        raster.setDeltaLonDeg(1.0);
        raster.setDeltaLatDeg(1.0);
        raster.setElevations(new float[]{100.0f, 200.0f, 300.0f, 400.0f});

        assertEquals(raster.getElevationBilinear(179.5, 0.5),
                raster.getElevationBilinear(-180.5, 0.5), 1.0e-6f);
        assertEquals(250.0f, raster.getElevationBilinear(-180.5, 0.5), 1.0e-6f);
    }

    @Test
    @Tag("default")
    void bilinearSamplingMapsTheAntimeridianOfADepthZeroEastRaster() {
        // the east tile of depth 0 covers 0..180, so the antimeridian is exactly 180 degrees away from its west edge (#32)
        GeographicTerrainTileRaster raster = createRaster(0.0, 180.0);

        assertEquals(300.0f, raster.getElevationBilinear(180.0, 0.5), 1.0e-6f);
        assertEquals(300.0f, raster.getElevationBilinear(-180.0, 0.5), 1.0e-6f);
        assertEquals(250.0f, raster.getElevationBilinear(-225.0, 0.5), 1.0e-6f);
    }

    @Test
    @Tag("default")
    void longitudeShiftMovesTheLongitudeClosestToTheRaster() {
        GeographicTerrainTileRaster eastRaster = createRaster(0.0, 180.0);
        assertEquals(360.0, eastRaster.getLongitudeShift(-180.0));
        assertEquals(360.0, eastRaster.getLongitudeShift(-270.0));
        assertEquals(0.0, eastRaster.getLongitudeShift(180.0));
        assertEquals(0.0, eastRaster.getLongitudeShift(90.0));

        GeographicTerrainTileRaster westRaster = createRaster(-180.0, 0.0);
        assertEquals(0.0, westRaster.getLongitudeShift(-180.0));
        assertEquals(-360.0, westRaster.getLongitudeShift(270.0));
        assertEquals(0.0, westRaster.getLongitudeShift(0.0));
    }

    /**
     * A raster of 3 columns (min, middle, max longitude) and 2 rows with the elevations 100, 200, 300 per row.
     */
    private static GeographicTerrainTileRaster createRaster(double minLonDeg, double maxLonDeg) {
        GeographicExtension extension = new GeographicExtension();
        extension.setDegrees(minLonDeg, 0.0, 0.0, maxLonDeg, 1.0, 0.0);

        GeographicTerrainTileRaster raster = new GeographicTerrainTileRaster();
        raster.setGeographicExtension(extension);
        raster.setRasterWidth(3);
        raster.setRasterHeight(2);
        raster.setDeltaLonDeg((maxLonDeg - minLonDeg) / 2.0);
        raster.setDeltaLatDeg(1.0);
        raster.setElevations(new float[]{100.0f, 200.0f, 300.0f, 100.0f, 200.0f, 300.0f});
        return raster;
    }
}
