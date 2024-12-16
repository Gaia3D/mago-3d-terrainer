package com.gaia3d.tile;

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
public class TilesRange {
    private int tileDepth;
    private int minTileX;
    private int maxTileX;
    private int minTileY;
    private int maxTileY;

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

    public TilesRange expand1() {
        TilesRange expandedTilesRange = new TilesRange();
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
