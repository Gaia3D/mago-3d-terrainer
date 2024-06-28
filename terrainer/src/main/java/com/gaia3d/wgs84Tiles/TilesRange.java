package com.gaia3d.wgs84Tiles;

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
                TileIndices tileIndices = new TileIndices(tileDepth, x, y);
                resultTileIndices.add(tileIndices);
            }
        }

        return resultTileIndices;
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
