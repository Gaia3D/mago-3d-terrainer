package com.gaia3d.terrain.tile.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TileRangeTest {

    @Test
    void expandClampsToGeographicTileMatrixBounds() {
        TileRange worldRange = new TileRange();
        worldRange.set(8, 0, 511, 0, 255);

        TileRange expanded = worldRange.expand(1);

        assertEquals(0, expanded.getMinTileX());
        assertEquals(511, expanded.getMaxTileX());
        assertEquals(0, expanded.getMinTileY());
        assertEquals(255, expanded.getMaxTileY());
    }

    @Test
    void expandStillAddsMarginInsideWorldBounds() {
        TileRange range = new TileRange();
        range.set(3, 5, 7, 2, 4);

        TileRange expanded = range.expand1();

        assertEquals(4, expanded.getMinTileX());
        assertEquals(8, expanded.getMaxTileX());
        assertEquals(1, expanded.getMinTileY());
        assertEquals(5, expanded.getMaxTileY());
    }
}
