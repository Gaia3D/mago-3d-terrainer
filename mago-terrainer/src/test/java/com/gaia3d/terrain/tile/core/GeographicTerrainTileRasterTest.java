package com.gaia3d.terrain.tile.core;

import com.gaia3d.terrain.structure.GeographicExtension;
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
}
