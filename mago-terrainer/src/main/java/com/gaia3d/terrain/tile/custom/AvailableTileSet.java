package com.gaia3d.terrain.tile.custom;

import com.gaia3d.terrain.structure.GeographicExtension;
import com.gaia3d.terrain.tile.TileIndices;
import com.gaia3d.terrain.tile.TileRange;
import com.gaia3d.terrain.tile.geotiff.TileRangeIntersectionType;
import com.gaia3d.terrain.util.TileWgs84Utils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Slf4j
public class AvailableTileSet {
    private final Map<Integer, List<TileRange>> mapDepthAvailableTileRanges; // Key: tile depth, Value: list of tile ranges

    public AvailableTileSet() {
        mapDepthAvailableTileRanges = new HashMap<>();
    }

    public List<TileRange> getAvailableTileRangesAtDepth(int depth) {
        return mapDepthAvailableTileRanges.get(depth);
    }

    public int getMaxAvailableDepth() {
        return mapDepthAvailableTileRanges.keySet().stream().max(Integer::compareTo).orElse(-1);
    }

    public void deleteTileRangesOverDepth(int maxDepth) {
        mapDepthAvailableTileRanges.keySet().removeIf(depth -> depth > maxDepth);
    }

    public void addAvailableExtensions(double pixelSizeMeters, GeographicExtension extension) {
        int maxDepth = TileWgs84Utils.getMaxTileDepthByPixelSizeMeters(pixelSizeMeters);
        boolean originIsLeftUp = false;
        for (int depth = 0; depth <= maxDepth; depth++) {
            List<TileRange> tileRanges = mapDepthAvailableTileRanges.computeIfAbsent(depth, k -> new java.util.ArrayList<>());

            GeographicExtension extensionCopy = new GeographicExtension();
            extensionCopy.copyFrom(extension);
            double minLon = extension.getMinLongitudeDeg();
            double maxLon = extension.getMaxLongitudeDeg();
            double minLat = extension.getMinLatitudeDeg();
            double maxLat = extension.getMaxLatitudeDeg();
            TileRange tilesRange = new TileRange();
            TileWgs84Utils.selectTileIndicesArray(depth, minLon, maxLon, minLat, maxLat, tilesRange, originIsLeftUp);

            tilesRange = tilesRange.expand(1); // add one tile margin to avoid big difference on edges between different depth tiles.
            tileRanges.add(tilesRange);
        }
    }

    public void recombineTileRanges() {
        // for each depth, recombine tile ranges to remove intersections
        for (Integer depth : mapDepthAvailableTileRanges.keySet()) {
            List<TileRange> tileRanges = mapDepthAvailableTileRanges.get(depth);
            List<TileRange> noIntersectedTileRanges = recombineTileRanges(tileRanges);
            mapDepthAvailableTileRanges.put(depth, noIntersectedTileRanges);
        }
    }

    private List<TileRange> recombineTileRanges(List<TileRange> tileRanges) {
        List<TileRange> noIntersectedTileRanges = new java.util.ArrayList<>(tileRanges);
        int tileRangesCount = noIntersectedTileRanges.size();
        if (tileRangesCount <= 1) {
            return noIntersectedTileRanges;
        }

        for (int i = 0; i < noIntersectedTileRanges.size(); i++) {
            TileRange tileRangeA = noIntersectedTileRanges.get(i);
            for (int j = i + 1; j < noIntersectedTileRanges.size(); j++) {
                TileRange tileRangeB = noIntersectedTileRanges.get(j);

                if (tileRangeA.intersects(tileRangeB)) {
                    TileRangeIntersectionType tileRangeIntersectionType = tileRangeA.getIntersectionType(tileRangeB);
                    //      A_CONTAINS_2_B_0                A_CONTAINS_2_B_1                    PARTIAL_OVERLAP                 A_CONTAINS_1_B_1
                    //     +--------------+                 +--------------+                 +---------+----+------+            +--------------+
                    //     |              |                 |              |                 |         |    |      |            |              |
                    //     |         +----+----+            |         +----+----+            |         |    |      |            |         +----+----+
                    //     |    A    |    | B  |            |    A    |    |    |            |    A    |    |  B   |            |    A    |    |    |
                    //     |         +----+----+            |         |    |  B |            |         |    |      |            |         |    |    |
                    //     |              |                 |         |    |    |            |         |    |      |            |         |    |    |
                    //     +--------------+                 +---------+----+----+            +---------+----+------+            +---------+----+  B |
                    //                                                                                                                    |         |
                    //                                                                                                                    +----+----+
                    //
                    //     B_CONTAINS_2_A_0                 B_CONTAINS_2_A_1                   A_CONTAINS_B                   B_CONTAINS_A
                    //     +--------------+                 +--------------+                 +------------------+            +------------------+
                    //     |              |                 |              |                 |        A         |            |        B         |
                    //     |         +----+----+            |         +----+----+            |    +--------+    |            |    +--------+    |
                    //     |    B    |    | A  |            |    B    |    |    |            |    |   B    |    |            |    |   A    |    |
                    //     |         +----+----+            |         |    |  A |            |    |        |    |            |    |        |    |
                    //     |              |                 |         |    |    |            |    +--------+    |            |    +--------+    |
                    //     +--------------+                 +---------+----+----+            +------------------+            +------------------+
                    //
                    //
                    //     HORIZONTAL_FULL_TOUCHING
                    //     +------------++-------------+
                    //     |            ||             |
                    //     |     A      ||      B      |
                    //     |            ||             |
                    //     +------------++-------------+
                    //
                    //     VERTICAL_FULL_TOUCHING
                    //     +-------------+
                    //     |             |
                    //     |      A      |
                    //     |             |
                    //     +-------------+
                    //     +-------------+
                    //     |             |
                    //     |      B      |
                    //     |             |
                    //     +-------------+

                    if (tileRangeIntersectionType == TileRangeIntersectionType.PARTIAL_OVERLAP ||
                            tileRangeIntersectionType == TileRangeIntersectionType.A_CONTAINS_B ||
                            tileRangeIntersectionType == TileRangeIntersectionType.B_CONTAINS_A ||
                            tileRangeIntersectionType == TileRangeIntersectionType.HORIZONTAL_FULL_TOUCHING ||
                            tileRangeIntersectionType == TileRangeIntersectionType.VERTICAL_FULL_TOUCHING) {
                        // In this case we can unify both ranges
                        tileRangeA.union(tileRangeB);
                        noIntersectedTileRanges.remove(tileRangeB);
                        //j--;
                        i = -1;
                        break;
                    } else if (tileRangeIntersectionType == TileRangeIntersectionType.A_CONTAINS_2_B_0 ||
                            tileRangeIntersectionType == TileRangeIntersectionType.A_CONTAINS_2_B_1) {
                        // In this case must split the tiles.
                        List<TileRange> splitTileRanges = split2ZonesTileRange(tileRangeA, tileRangeB, tileRangeIntersectionType);
                        noIntersectedTileRanges.remove(tileRangeA);
                        noIntersectedTileRanges.remove(tileRangeB);
                        noIntersectedTileRanges.addAll(splitTileRanges);
                        i = -1;
                        break;
                    } else if (tileRangeIntersectionType == TileRangeIntersectionType.B_CONTAINS_2_A_0 ||
                            tileRangeIntersectionType == TileRangeIntersectionType.B_CONTAINS_2_A_1) {
                        // In this case must split the tiles.
                        List<TileRange> splitTileRanges = split2ZonesTileRange(tileRangeB, tileRangeA, tileRangeIntersectionType);
                        noIntersectedTileRanges.remove(tileRangeA);
                        noIntersectedTileRanges.remove(tileRangeB);
                        noIntersectedTileRanges.addAll(splitTileRanges);
                        i = -1;
                        break;
                    } else if (tileRangeIntersectionType == TileRangeIntersectionType.A_CONTAINS_1_B_1) {
                        // In this case must split the tiles.
                        List<TileRange> splitTileRanges = split3ZonesTileRange(tileRangeA, tileRangeB, tileRangeIntersectionType);
                        noIntersectedTileRanges.remove(tileRangeA);
                        noIntersectedTileRanges.remove(tileRangeB);
                        noIntersectedTileRanges.addAll(splitTileRanges);
                        i = -1;
                        break;
                    }

                } else {
                    // check touching cases
                    TileRangeIntersectionType tileRangeIntersectionType = tileRangeA.getIntersectionType(tileRangeB);
                    if (tileRangeIntersectionType == TileRangeIntersectionType.HORIZONTAL_FULL_TOUCHING ||
                            tileRangeIntersectionType == TileRangeIntersectionType.VERTICAL_FULL_TOUCHING) {
                        // In this case we can unify both ranges
                        tileRangeA.union(tileRangeB);
                        noIntersectedTileRanges.remove(tileRangeB);
                        //j--;
                        i = -1;
                        break;
                    }
                }
            }
        }

        return noIntersectedTileRanges;
    }

    private List<TileRange> split3ZonesTileRange(TileRange tileRangeBig, TileRange tileRangeSmall, TileRangeIntersectionType intersectionType) {
        // A_CONTAINS_1_B_1
        if (intersectionType != TileRangeIntersectionType.A_CONTAINS_1_B_1) {
            throw new IllegalArgumentException("Invalid intersection type for split3ZonesTileRange: " + intersectionType);
        }

        List<TileRange> resultTileRanges = new java.util.ArrayList<>();
        resultTileRanges.add(tileRangeBig); // start with the big range

        // must determine in what corner is the small range respect the big one
        // check horizontally and vertically
        if (tileRangeSmall.getMinTileX() < tileRangeBig.getMinTileX()) {
            if (tileRangeSmall.getMinTileY() < tileRangeBig.getMinTileY()) {
                // small(B) is on the left-bottom corner of big(A)
                //                                              split vertically                      split horizontally
                //                 +--------------+                    +--------------+                    +--------------+
                //                 |              |                    |              |                    |              |
                //            +----+----+         |         +-------+  +----+         |         +-------+  +----+         |
                //            |    |    |    A    |         |       |  |    |    A    |         |       |  |    |    A    |
                //            | B  |    |         |   ==>   |   B1  |  | B2 |         |   ==>   |   B1  |  | B2 |         |
                //            |    +----+---------+         |       |  +----+---------+         |       |  +----+---------+
                //            |         |                   |       |  |    |                   |       |  +----+
                //            |         |                   |       |  |    |                   |       |  | B3 |
                //            +---------+                   +-------+  +----+                   +-------+  +----+
                //

                // 1rst, split vertically
                int bigMinX = tileRangeBig.getMinTileX();
                int leftX = bigMinX - 1; // just one tile outside
                int rightX = bigMinX; // first tile of big
                List<TileRange> splitTileRangesX = tileRangeSmall.splitTileRangeByX(leftX, rightX);

                // 2nd, split horizontally the left range if intersects with big
                int bigMinY = tileRangeBig.getMinTileY();
                int bottomY = bigMinY - 1; // just one tile outside
                int topY = bigMinY; // first tile of big
                for (TileRange splitTileRangeX : splitTileRangesX) {
                    if (splitTileRangeX.intersects(tileRangeBig)) {
                        List<TileRange> splitTileRangesY = splitTileRangeX.splitTileRangeByY(bottomY, topY);
                        // now select the no intersected ranges
                        for (TileRange splitTileRangeY : splitTileRangesY) {
                            if (!splitTileRangeY.intersects(tileRangeBig)) {
                                resultTileRanges.add(splitTileRangeY);
                            }
                        }
                    } else {
                        resultTileRanges.add(splitTileRangeX);
                    }
                }
            } else if (tileRangeSmall.getMaxTileY() > tileRangeBig.getMaxTileY()) {
                // small(B) is on the left-top corner of big(A)
                //                                              split vertically                      split horizontally
                //            +---------+                   +-------+  +----+                   +-------+  +----+
                //            |         |                   |       |  |    |                   |       |  | B3 |
                //            |         |                   |       |  |    |                   |       |  +----+
                //            |    +----+---------+         |       |  +----+---------+         |       |  +----+---------+
                //            | B  |    |         |   ==>   |   B1  |  | B2 |         |   ==>   |   B1  |  | B2 |         |
                //            |    |    |    A    |         |       |  |    |    A    |         |       |  |    |    A    |
                //            +----+----+         |         +-------+  +----+         |         +-------+  +----+         |
                //                 |              |                    |              |                    |              |
                //                 +--------------+                    +--------------+                    +--------------+

                // 1rst, split vertically
                int bigMinX = tileRangeBig.getMinTileX();
                int leftX = bigMinX - 1; // just one tile outside
                int rightX = bigMinX; // first tile of big
                List<TileRange> splitTileRangesX = tileRangeSmall.splitTileRangeByX(leftX, rightX);

                // 2nd, split horizontally the left range if intersects with big
                int bigMaxY = tileRangeBig.getMaxTileY();
                int bottomY = bigMaxY; // first tile of big
                int topY = bigMaxY + 1; // just one tile outside
                for (TileRange splitTileRangeX : splitTileRangesX) {
                    if (splitTileRangeX.intersects(tileRangeBig)) {
                        List<TileRange> splitTileRangesY = splitTileRangeX.splitTileRangeByY(bottomY, topY);
                        // now select the no intersected ranges
                        for (TileRange splitTileRangeY : splitTileRangesY) {
                            if (!splitTileRangeY.intersects(tileRangeBig)) {
                                resultTileRanges.add(splitTileRangeY);
                            }
                        }
                    } else {
                        resultTileRanges.add(splitTileRangeX);
                    }
                }
            }
        } else if (tileRangeSmall.getMaxTileX() > tileRangeBig.getMaxTileX()) {
            if (tileRangeSmall.getMinTileY() < tileRangeBig.getMinTileY()) {
                // small(B) is on the right-bottom corner of big(A)
                //                                                  split vertically                      split horizontally
                //         +--------------+                      +--------------+                           +--------------+
                //         |              |                      |              |                           |              |
                //         |         +----+----+                 |         +----+  +----+                   |         +----+  +----+
                //         |    A    |    |  B |                 |    A    | B2 |  | B1 |                   |    A    | B2 |  | B1 |
                //         |         |    |    |     ==>         |         |    |  |    |       ==>         |         |    |  |    |
                //         +---------+----+    |                 +---------+----+  |    |                   +---------+----+  |    |
                //                   |         |                           |    |  |    |                             +----+  |    |
                //                   |         |                           |    |  |    |                             | B3 |  |    |
                //                   +---------+                           +----+  +----+                             +----+  +----+

                // 1rst, split vertically
                int bigMaxX = tileRangeBig.getMaxTileX();
                int leftX = bigMaxX; // first tile of big
                int rightX = bigMaxX + 1; // just one tile outside
                List<TileRange> splitTileRangesX = tileRangeSmall.splitTileRangeByX(leftX, rightX);

                // 2nd, split horizontally the range if intersects with big
                int bigMinY = tileRangeBig.getMinTileY();
                int bottomY = bigMinY - 1; // just one tile outside
                int topY = bigMinY; // first tile of big
                for (TileRange splitTileRangeX : splitTileRangesX) {
                    if (splitTileRangeX.intersects(tileRangeBig)) {
                        List<TileRange> splitTileRangesY = splitTileRangeX.splitTileRangeByY(bottomY, topY);
                        // now select the no intersected ranges
                        for (TileRange splitTileRangeY : splitTileRangesY) {
                            if (!splitTileRangeY.intersects(tileRangeBig)) {
                                resultTileRanges.add(splitTileRangeY);
                            }
                        }
                    } else {
                        resultTileRanges.add(splitTileRangeX);
                    }
                }
            } else if (tileRangeSmall.getMaxTileY() > tileRangeBig.getMaxTileY()) {
                // small(B) is on the right-top corner of big(A)
                //                                                  split vertically                      split horizontally
                //                   +---------+                           +----+  +----+                             +----+  +----+
                //                   |         |                           |    |  |    |                             | B3 |  |    |
                //                   |         |                           |    |  |    |                             +----+  |    |
                //         +---------+----+    |                 +---------+----+  |    |                   +---------+----+  |    |
                //         |         |    |    |     ==>         |         |    |  |    |       ==>         |         |    |  |    |
                //         |    A    |    |  B |                 |    A    | B2 |  | B1 |                   |    A    | B2 |  | B1 |
                //         |         +----+----+                 |         +----+  +----+                   |         +----+  +----+
                //         |              |                      |              |                           |              |
                //         +--------------+                      +--------------+                           +--------------+

                // 1rst, split vertically
                int bigMaxX = tileRangeBig.getMaxTileX();
                int leftX = bigMaxX; // first tile of big
                int rightX = bigMaxX + 1; // just one tile outside
                List<TileRange> splitTileRangesX = tileRangeSmall.splitTileRangeByX(leftX, rightX);

                // 2nd, split horizontally the range if intersects with big
                int bigMaxY = tileRangeBig.getMaxTileY();
                int bottomY = bigMaxY; // first tile of big
                int topY = bigMaxY + 1; // just one tile outside
                for (TileRange splitTileRangeX : splitTileRangesX) {
                    if (splitTileRangeX.intersects(tileRangeBig)) {
                        List<TileRange> splitTileRangesY = splitTileRangeX.splitTileRangeByY(bottomY, topY);
                        // now select the no intersected ranges
                        for (TileRange splitTileRangeY : splitTileRangesY) {
                            if (!splitTileRangeY.intersects(tileRangeBig)) {
                                resultTileRanges.add(splitTileRangeY);
                            }
                        }
                    } else {
                        resultTileRanges.add(splitTileRangeX);
                    }
                }
            }
        }
        return resultTileRanges;
    }

    private List<TileRange> split2ZonesTileRange(TileRange tileRangeBig, TileRange tileRangeSmall, TileRangeIntersectionType intersectionType) {
        // A_CONTAINS_2_B_0 || A_CONTAINS_2_B_1 || B_CONTAINS_2_A_0 || B_CONTAINS_2_A_1
        if (intersectionType != TileRangeIntersectionType.A_CONTAINS_2_B_0 &&
                intersectionType != TileRangeIntersectionType.A_CONTAINS_2_B_1 &&
                intersectionType != TileRangeIntersectionType.B_CONTAINS_2_A_0 &&
                intersectionType != TileRangeIntersectionType.B_CONTAINS_2_A_1) {
            throw new IllegalArgumentException("Invalid intersection type for split2ZonesTileRange: " + intersectionType);
        }

        List<TileRange> resultTileRanges = new java.util.ArrayList<>();
        resultTileRanges.add(tileRangeBig); // start with the big range

        // must determine in what side is the small range respect the big one
        // check horizontally and vertically
        if (tileRangeSmall.getMinTileX() < tileRangeBig.getMinTileX()) {
            // small(B) is on the left side of big(A)
            //                 +--------------+
            //                 |              |
            //            +----+----+         |
            //            | B  |    |    A    |
            //            +----+----+         |
            //                 |              |
            //                 +--------------+
            int bigMinX = tileRangeBig.getMinTileX();
            int leftX = bigMinX - 1; // just one tile outside
            int rightX = bigMinX; // first tile of big
            List<TileRange> splitTileRanges = tileRangeSmall.splitTileRangeByX(leftX, rightX);

            // now select the no intersected ranges
            for (TileRange splitTileRange : splitTileRanges) {
                if (!splitTileRange.intersects(tileRangeBig)) {
                    resultTileRanges.add(splitTileRange);
                }
            }

        } else if (tileRangeSmall.getMaxTileX() > tileRangeBig.getMaxTileX()) {
            // small(B) is on the right side of big(A)
            //                 +--------------+
            //                 |              |
            //                 |         +----+----+
            //                 |    A    |    |  B |
            //                 |         +----+----+
            //                 |              |
            //                 +--------------+
            int bigMaxX = tileRangeBig.getMaxTileX();
            int leftX = bigMaxX; // first tile of big
            int rightX = bigMaxX + 1; // just one tile outside
            List<TileRange> splitTileRanges = tileRangeSmall.splitTileRangeByX(leftX, rightX);

            // now select the no intersected ranges
            for (TileRange splitTileRange : splitTileRanges) {
                if (!splitTileRange.intersects(tileRangeBig)) {
                    resultTileRanges.add(splitTileRange);
                }
            }

        } else if (tileRangeSmall.getMinTileY() < tileRangeBig.getMinTileY()) {
            // small(B) is on the bottom side of big(A)
            //                 +--------------+
            //                 |         A    |
            //                 |   +------+   |
            //                 |   |      |   |
            //                 +---+------+---+
            //                     |  B   |
            //                     +------+
            int bigMinY = tileRangeBig.getMinTileY();
            int bottomY = bigMinY - 1; // just one tile outside
            int topY = bigMinY; // first tile of big
            List<TileRange> splitTileRanges = tileRangeSmall.splitTileRangeByY(bottomY, topY);

            // now select the no intersected ranges
            for (TileRange splitTileRange : splitTileRanges) {
                if (!splitTileRange.intersects(tileRangeBig)) {
                    resultTileRanges.add(splitTileRange);
                }
            }

        } else if (tileRangeSmall.getMaxTileY() > tileRangeBig.getMaxTileY()) {
            // small(B) is on the top side of big(A)
            //                    +--------+
            //                    |   B    |
            //                 +--+--------+--+
            //                 |  |        |  |
            //                 |  +--------+  |
            //                 |      A       |
            //                 +--------------+
            int bigMaxY = tileRangeBig.getMaxTileY();
            int bottomY = bigMaxY; // first tile of big
            int topY = bigMaxY + 1; // just one tile outside
            List<TileRange> splitTileRanges = tileRangeSmall.splitTileRangeByY(bottomY, topY);

            // now select the no intersected ranges
            for (TileRange splitTileRange : splitTileRanges) {
                if (!splitTileRange.intersects(tileRangeBig)) {
                    resultTileRanges.add(splitTileRange);
                }
            }
        }

        return resultTileRanges;
    }
}
