package com.gaia3d.terrain.tile.layer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.terrain.tile.core.TileIndices;
import com.gaia3d.terrain.tile.core.TileRange;
import com.gaia3d.terrain.tile.custom.AvailableTileSet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("default")
class TileAvailabilityMetadataTest {

    private static TileRange range(int depth, int minX, int maxX, int minY, int maxY) {
        TileRange range = new TileRange();
        range.setTileDepth(depth);
        range.setMinTileX(minX);
        range.setMaxTileX(maxX);
        range.setMinTileY(minY);
        range.setMaxTileY(maxY);
        return range;
    }

    @Test
    void onlyTheAvailabilityDepthsStoreMetadata() {
        AvailableTileSet availableTileSet = new AvailableTileSet();
        assertNull(TileAvailabilityMetadata.createMetadataJson(new TileIndices(0, 0, 3), availableTileSet, 12));
        assertNotNull(TileAvailabilityMetadata.createMetadataJson(new TileIndices(0, 0, 0), availableTileSet, 12));
        assertNotNull(TileAvailabilityMetadata.createMetadataJson(new TileIndices(0, 0, 10), availableTileSet, 12));
    }

    @Test
    void availableListsTheDescendantsOfTheTileForTheNextDepths() throws Exception {
        AvailableTileSet availableTileSet = new AvailableTileSet();
        // depth 1: the whole world, depth 2: a range crossing the descendants of the tile (1, 0, 0)
        availableTileSet.getMapDepthAvailableTileRanges().put(1, new ArrayList<>(List.of(range(1, 0, 3, 0, 1))));
        availableTileSet.getMapDepthAvailableTileRanges().put(2, new ArrayList<>(List.of(range(2, 1, 6, 0, 3))));

        // the tile (x=1, y=0) at depth 0 covers x 2..3 / y 0..1 at depth 1, x 4..7 / y 0..3 at depth 2
        String json = TileAvailabilityMetadata.createMetadataJson(new TileIndices(1, 0, 0), availableTileSet, 12);
        JsonNode available = new ObjectMapper().readTree(json).get("available");
        // available[0] = depth 1, available[1] = depth 2, the empty depths after are not written
        assertEquals(2, available.size());
        JsonNode depth1 = available.get(0).get(0);
        assertEquals(2, depth1.get("startX").asInt());
        assertEquals(3, depth1.get("endX").asInt());
        assertEquals(0, depth1.get("startY").asInt());
        assertEquals(1, depth1.get("endY").asInt());
        JsonNode depth2 = available.get(1).get(0);
        assertEquals(4, depth2.get("startX").asInt());
        assertEquals(6, depth2.get("endX").asInt());
        assertEquals(0, depth2.get("startY").asInt());
        assertEquals(3, depth2.get("endY").asInt());
    }

    @Test
    void depthsDeeperThanTheMaxDepthAreNotListed() throws Exception {
        AvailableTileSet availableTileSet = new AvailableTileSet();
        availableTileSet.getMapDepthAvailableTileRanges().put(1, new ArrayList<>(List.of(range(1, 0, 3, 0, 1))));
        availableTileSet.getMapDepthAvailableTileRanges().put(2, new ArrayList<>(List.of(range(2, 0, 7, 0, 3))));
        String json = TileAvailabilityMetadata.createMetadataJson(new TileIndices(0, 0, 0), availableTileSet, 1);
        assertEquals(1, new ObjectMapper().readTree(json).get("available").size());
    }
}
