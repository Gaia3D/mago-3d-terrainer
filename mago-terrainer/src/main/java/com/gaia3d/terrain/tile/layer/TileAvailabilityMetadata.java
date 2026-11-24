package com.gaia3d.terrain.tile.layer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gaia3d.terrain.tile.core.TileIndices;
import com.gaia3d.terrain.tile.core.TileRange;
import com.gaia3d.terrain.tile.custom.AvailableTileSet;
import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * The "metadata" extension of the quantized-mesh tiles, as used by CesiumJS:
 * the tiles at the depths 0, N, 2N, ... (N = "metadataAvailability" of layer.json) store the available tile ranges
 * of their descendants for the next N depths: available[i] is the depth (tile depth + 1 + i).
 * With "metadataAvailability", CesiumJS does not read the "available" list of layer.json, so layer.json stays small.
 */
@UtilityClass
public class TileAvailabilityMetadata {
    // the value used by Cesium World Terrain
    public static final int AVAILABILITY_LEVELS = 10;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static boolean isAvailabilityTile(int depth) {
        return depth % AVAILABILITY_LEVELS == 0;
    }

    /**
     * Returns the metadata JSON of the tile, or null if the tile does not store the availability.
     */
    public static String createMetadataJson(TileIndices tileIndices, AvailableTileSet availableTileSet, int maxTileDepth) {
        int depth = tileIndices.getL();
        if (!isAvailabilityTile(depth)) {
            return null;
        }

        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        ArrayNode available = root.putArray("available");
        int lastNonEmptyLevel = -1;
        for (int offset = 0; offset < AVAILABILITY_LEVELS; offset++) {
            int childDepth = depth + 1 + offset;
            ArrayNode levelRanges = available.addArray();
            if (childDepth > maxTileDepth) {
                continue;
            }
            int scale = 1 << (offset + 1);
            int minX = tileIndices.getX() * scale;
            int maxX = minX + scale - 1;
            int minY = tileIndices.getY() * scale;
            int maxY = minY + scale - 1;
            List<TileRange> ranges = availableTileSet.getAvailableTileRangesAtDepth(childDepth);
            if (ranges == null) {
                continue;
            }
            for (TileRange range : ranges) {
                int startX = Math.max(minX, range.getMinTileX());
                int endX = Math.min(maxX, range.getMaxTileX());
                int startY = Math.max(minY, range.getMinTileY());
                int endY = Math.min(maxY, range.getMaxTileY());
                if (startX > endX || startY > endY) {
                    continue;
                }
                ObjectNode rangeNode = levelRanges.addObject();
                rangeNode.put("startX", startX);
                rangeNode.put("startY", startY);
                rangeNode.put("endX", endX);
                rangeNode.put("endY", endY);
                lastNonEmptyLevel = offset;
            }
        }
        // the trailing empty levels are not needed
        while (available.size() > lastNonEmptyLevel + 1) {
            available.remove(available.size() - 1);
        }
        return root.toString();
    }
}
