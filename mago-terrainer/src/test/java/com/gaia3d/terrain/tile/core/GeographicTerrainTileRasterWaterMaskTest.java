package com.gaia3d.terrain.tile.core;

import com.gaia3d.terrain.structure.GeographicExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("default")
class GeographicTerrainTileRasterWaterMaskTest {

    /**
     * A 2 x 2 raster over lon 0..1 / lat 0..1. The raster rows go from the south (row 0) to the north.
     */
    private static GeographicTerrainTileRaster createRaster(boolean[] dataMask) {
        GeographicExtension extension = new GeographicExtension();
        extension.setDegrees(0.0, 0.0, 0.0, 1.0, 1.0, 0.0);
        GeographicTerrainTileRaster raster = new GeographicTerrainTileRaster();
        raster.setGeographicExtension(extension);
        raster.setRasterWidth(2);
        raster.setRasterHeight(2);
        raster.setDeltaLonDeg(0.5);
        raster.setDeltaLatDeg(0.5);
        raster.setElevations(new float[4]);
        raster.setDataMask(dataMask);
        return raster;
    }

    @Test
    void waterMaskStartsAtTheNorthWestCorner() {
        // data only in the south row: the north half of the mask is water
        byte[] mask = createRaster(new boolean[]{true, true, false, false}).createWaterMask();
        assertEquals(256 * 256, mask.length);
        assertEquals((byte) 255, mask[0]); // north-west
        assertEquals((byte) 255, mask[255]); // north-east
        assertEquals(0, mask[255 * 256]); // south-west
        assertEquals(0, mask[255 * 256 + 255]); // south-east
    }

    @Test
    void waterMaskRowsGoFromWestToEast() {
        // data only in the east column
        byte[] mask = createRaster(new boolean[]{false, true, false, true}).createWaterMask();
        assertEquals((byte) 255, mask[0]);
        assertEquals(0, mask[255]);
    }

    @Test
    void uniformTilesUseOneByte() {
        assertArrayEquals(new byte[]{0}, createRaster(new boolean[]{true, true, true, true}).createWaterMask());
        assertArrayEquals(new byte[]{(byte) 255}, createRaster(new boolean[]{false, false, false, false}).createWaterMask());
        // without the water mask option there is no data mask: all land
        assertArrayEquals(new byte[]{0}, createRaster(null).createWaterMask());
    }
}
