package com.gaia3d.terrain.tile;

import com.gaia3d.terrain.tile.geotiff.TileRangeIntersectionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class TileRange {
    private int tileDepth;
    private int minTileX;
    private int maxTileX;
    private int minTileY;
    private int maxTileY;

    public void set(int tileDepth, int minTileX, int maxTileX, int minTileY, int maxTileY) {
        this.tileDepth = tileDepth;
        this.minTileX = minTileX;
        this.maxTileX = maxTileX;
        this.minTileY = minTileY;
        this.maxTileY = maxTileY;
    }

    public TileRange clone() {
        TileRange tileRange = new TileRange();
        tileRange.set(tileDepth, minTileX, maxTileX, minTileY, maxTileY);
        return tileRange;
    }

    public void translate(int translateX, int translateY) {
        this.minTileX += translateX;
        this.maxTileX += translateX;
        this.minTileY += translateY;
        this.maxTileY += translateY;

        if (this.minTileX < 0) {
            this.minTileX = 0;
        }

        if (this.minTileY < 0) {
            this.minTileY = 0;
        }
    }

    public List<TileIndices> getTileIndices(List<TileIndices> resultTileIndices) {
        if (resultTileIndices == null) {
            resultTileIndices = new ArrayList<>();
        }

        for (int y = minTileY; y <= maxTileY; y++) {
            for (int x = minTileX; x <= maxTileX; x++) {
                TileIndices tileIndices = new TileIndices();
                tileIndices.set(x, y, tileDepth);
                resultTileIndices.add(tileIndices);
            }
        }

        return resultTileIndices;
    }

    public TileRange expand(int expandTiles) {
        TileRange expandedTilesRange = new TileRange();
        expandedTilesRange.setTileDepth(tileDepth);
        int expandedMinTileX = minTileX - expandTiles;
        if (expandedMinTileX < 0) {
            expandedMinTileX = 0;
        }
        int expandedMaxTileX = maxTileX + expandTiles;
        int expandedMinTileY = minTileY - expandTiles;
        if (expandedMinTileY < 0) {
            expandedMinTileY = 0;
        }
        int expandedMaxTileY = maxTileY + expandTiles;
        expandedTilesRange.setMinTileX(expandedMinTileX);
        expandedTilesRange.setMaxTileX(expandedMaxTileX);
        expandedTilesRange.setMinTileY(expandedMinTileY);
        expandedTilesRange.setMaxTileY(expandedMaxTileY);
        return expandedTilesRange;

    }

    public TileRange expand1() {
        TileRange expandedTilesRange = new TileRange();
        expandedTilesRange.setTileDepth(tileDepth);
        int expandedMinTileX = minTileX - 1;
        if (expandedMinTileX < 0) {
            expandedMinTileX = 0;
        }
        int expandedMaxTileX = maxTileX + 1;
        int expandedMinTileY = minTileY - 1;
        if (expandedMinTileY < 0) {
            expandedMinTileY = 0;
        }
        int expandedMaxTileY = maxTileY + 1;
        expandedTilesRange.setMinTileX(expandedMinTileX);
        expandedTilesRange.setMaxTileX(expandedMaxTileX);
        expandedTilesRange.setMinTileY(expandedMinTileY);
        expandedTilesRange.setMaxTileY(expandedMaxTileY);
        return expandedTilesRange;

    }

    public boolean contains(TileRange tileRange) {
        if (tileRange.getTileDepth() != this.tileDepth) {
            return false;
        }

        if (tileRange.getMinTileX() < this.minTileX || tileRange.getMaxTileX() > this.maxTileX) {
            return false;
        }

        if (tileRange.getMinTileY() < this.minTileY || tileRange.getMaxTileY() > this.maxTileY) {
            return false;
        }

        return true;
    }

    public boolean intersects(TileIndices tileIndices) {
        if (tileIndices.getL() != tileDepth) {
            return false;
        }

        if (tileIndices.getX() < minTileX || tileIndices.getX() > maxTileX) {
            return false;
        }

        return tileIndices.getY() >= minTileY && tileIndices.getY() <= maxTileY;
    }

    public boolean intersects(TileRange tileRange) {
        if (tileRange.getTileDepth() != this.tileDepth) {
            return false;
        }

        if (tileRange.getMinTileX() > this.maxTileX || tileRange.getMaxTileX() < this.minTileX) {
            return false;
        }

        if (tileRange.getMinTileY() > this.maxTileY || tileRange.getMaxTileY() < this.minTileY) {
            return false;
        }

        return true;
    }

    public List<TileRange> splitTileRangeByX(int leftX, int rightX) {
        List<TileRange> resultTileRanges = new ArrayList<>();

        if (leftX >= minTileX) {
            TileRange leftTileRange = new TileRange();
            leftTileRange.set(tileDepth, minTileX, leftX, minTileY, maxTileY);
            resultTileRanges.add(leftTileRange);
        }

        if (rightX <= maxTileX) {
            TileRange rightTileRange = new TileRange();
            rightTileRange.set(tileDepth, rightX, maxTileX, minTileY, maxTileY);
            resultTileRanges.add(rightTileRange);
        }

        return resultTileRanges;
    }

    public List<TileRange> splitTileRangeByY(int bottomY, int topY) {
        List<TileRange> resultTileRanges = new ArrayList<>();

        if (bottomY >= minTileY) {
            TileRange topTileRange = new TileRange();
            topTileRange.set(tileDepth, minTileX, maxTileX, minTileY, bottomY);
            resultTileRanges.add(topTileRange);
        }

        if (topY <= maxTileY) {
            TileRange bottomTileRange = new TileRange();
            bottomTileRange.set(tileDepth, minTileX, maxTileX, topY, maxTileY);
            resultTileRanges.add(bottomTileRange);
        }

        return resultTileRanges;
    }


    public boolean intersectsXAxis(TileRange tileRange) {
        if (tileRange.getTileDepth() != this.tileDepth) {
            return false;
        }

        if (tileRange.getMinTileX() > this.maxTileX || tileRange.getMaxTileX() < this.minTileX) {
            return false;
        }

        return true;
    }

    public boolean intersectsYAxis(TileRange tileRange) {
        if (tileRange.getTileDepth() != this.tileDepth) {
            return false;
        }

        if (tileRange.getMinTileY() > this.maxTileY || tileRange.getMaxTileY() < this.minTileY) {
            return false;
        }

        return true;
    }

    public TileIndices getLeftDownTileIndices() {
        TileIndices tileIndices = new TileIndices();
        tileIndices.set(minTileX, minTileY, tileDepth);
        return tileIndices;
    }

    public TileIndices getRightDownTileIndices() {
        TileIndices tileIndices = new TileIndices();
        tileIndices.set(maxTileX, minTileY, tileDepth);
        return tileIndices;
    }

    public TileIndices getLeftUpTileIndices() {
        TileIndices tileIndices = new TileIndices();
        tileIndices.set(minTileX, maxTileY, tileDepth);
        return tileIndices;
    }

    public TileIndices getRightUpTileIndices() {
        TileIndices tileIndices = new TileIndices();
        tileIndices.set(maxTileX, maxTileY, tileDepth);
        return tileIndices;
    }

    public int howMuchVerticesContained(TileRange tileRange) {
        int containedVertices = 0;

        TileIndices leftDown = tileRange.getLeftDownTileIndices();
        if (this.intersects(leftDown)) {
            containedVertices++;
        }

        TileIndices rightDown = tileRange.getRightDownTileIndices();
        if (this.intersects(rightDown)) {
            containedVertices++;
        }

        TileIndices leftUp = tileRange.getLeftUpTileIndices();
        if (this.intersects(leftUp)) {
            containedVertices++;
        }

        TileIndices rightUp = tileRange.getRightUpTileIndices();
        if (this.intersects(rightUp)) {
            containedVertices++;
        }

        return containedVertices;
    }

    public TileRangeIntersectionType isFullTouching(TileRange tileRange) {
        if (tileRange.getTileDepth() != this.tileDepth) {
            return TileRangeIntersectionType.NO_INTERSECTION;
        }

        // The full touching case is when both tiles are almost partial overlapping.
        //     +------------++-------------+
        //     |            ||             |
        //     |     A      ||      B      |
        //     |            ||             |
        //     +------------++-------------+ horizontally touching
        //
        //     +-------------+
        //     |             |
        //     |      A      |
        //     |             |
        //     +-------------+
        //     +-------------+
        //     |             |
        //     |      B      |
        //     |             |
        //     +-------------+ vertically touching

        // check if the tile is horizontally touching in right or left side
        if (this.maxTileX + 1 == tileRange.getMinTileX() ||
            tileRange.getMaxTileX() + 1 == this.minTileX) {
            // check if they have same Y range
            if (this.minTileY == tileRange.getMinTileY() && this.maxTileY == tileRange.getMaxTileY()) {
                return TileRangeIntersectionType.HORIZONTAL_FULL_TOUCHING;
        }

        // check if the tile is vertically touching in top or bottom side
        } else if (this.maxTileY + 1 == tileRange.getMinTileY() ||
                   tileRange.getMaxTileY() + 1 == this.minTileY) {
            // check if they have same X range
            if (this.minTileX == tileRange.getMinTileX() && this.maxTileX == tileRange.getMaxTileX()) {
                return TileRangeIntersectionType.VERTICAL_FULL_TOUCHING;
            }
        }

        return TileRangeIntersectionType.UNKNOWN;
    }

    public TileRangeIntersectionType getIntersectionType(TileRange tileRange) {
        if (tileRange.getTileDepth() != this.tileDepth) {
            return TileRangeIntersectionType.NO_INTERSECTION; // different depth, no intersection
        }

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
        // A contains 2 vertexes of B and B no contains any vertex of A (and vice versa)
        // A contains 2 vertexes of B and B contains 1 vertex of A (and vice versa)
        // A contains 2 vertexes of B and B contains 2 vertexes of A (partial overlap)
        // A contains all 4 vertexes of B (and vice versa)
        // Both contains 1 vertex of each other

        int AContainsB = this.howMuchVerticesContained(tileRange);
        int BContainsA = tileRange.howMuchVerticesContained(this);

        if (AContainsB == 4) {
            return TileRangeIntersectionType.A_CONTAINS_B;
        } else if (BContainsA == 4) {
            return TileRangeIntersectionType.B_CONTAINS_A;
        } else if (AContainsB == 2 && BContainsA == 0) {
            return TileRangeIntersectionType.A_CONTAINS_2_B_0;
        } else if (AContainsB == 2 && BContainsA == 1) {
            return TileRangeIntersectionType.A_CONTAINS_2_B_1;
        } else if (AContainsB == 2 && BContainsA == 2) {
            return TileRangeIntersectionType.PARTIAL_OVERLAP;
        } else if (AContainsB == 1 && BContainsA == 1) {
            return TileRangeIntersectionType.A_CONTAINS_1_B_1;
        } else if (BContainsA == 2 && AContainsB == 0) {
            return TileRangeIntersectionType.B_CONTAINS_2_A_0;
        } else if (BContainsA == 2 && AContainsB == 1) {
            return TileRangeIntersectionType.B_CONTAINS_2_A_1;
        }

        if(!this.intersects(tileRange)) {
            // check if they are fully touching
            TileRangeIntersectionType fullTouchingType = this.isFullTouching(tileRange);
            if (fullTouchingType != TileRangeIntersectionType.UNKNOWN) {
                return fullTouchingType;
            }
            return TileRangeIntersectionType.NO_INTERSECTION; // no intersection
        }

        return TileRangeIntersectionType.UNKNOWN; // unknown case
    }

    public void union(TileRange tileRange) {
        if (tileRange.getTileDepth() != this.tileDepth) {
            log.warn("Can't union tile ranges with different depth: {} and {}", this.tileDepth, tileRange.getTileDepth());
            return;
        }

        if (tileRange.getMinTileX() < this.minTileX) {
            this.minTileX = tileRange.getMinTileX();
        }

        if (tileRange.getMaxTileX() > this.maxTileX) {
            this.maxTileX = tileRange.getMaxTileX();
        }

        if (tileRange.getMinTileY() < this.minTileY) {
            this.minTileY = tileRange.getMinTileY();
        }

        if (tileRange.getMaxTileY() > this.maxTileY) {
            this.maxTileY = tileRange.getMaxTileY();
        }
    }
}
