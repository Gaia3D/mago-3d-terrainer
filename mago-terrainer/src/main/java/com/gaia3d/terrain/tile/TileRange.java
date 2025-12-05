package com.gaia3d.terrain.tile;

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

    public boolean intersects(TileIndices tileIndices) {
        if (tileIndices.getL() != tileDepth) {
            return false;
        }

        if (tileIndices.getX() < minTileX || tileIndices.getX() > maxTileX) {
            return false;
        }

        return tileIndices.getY() >= minTileY && tileIndices.getY() <= maxTileY;
    }
}
