package com.gaia3d.wgs84Tiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TilesRange {
    int tileDepth;
    int minTileX;
    int maxTileX;
    int minTileY;
    int maxTileY;

    public TilesRange() {
    }

    public List<TileIndices> getTileIndices(List<TileIndices> resultTileIndices)
    {
        if(resultTileIndices == null)
        {
            resultTileIndices = new ArrayList<>();
        }

        for(int y = minTileY; y <= maxTileY; y++)
        {
            for(int x = minTileX; x <= maxTileX; x++)
            {
                TileIndices tileIndices = new TileIndices(tileDepth, x, y);
                resultTileIndices.add(tileIndices);
            }
        }

        return resultTileIndices;
    }

    public boolean intersects(TileIndices tileIndices)
    {
        if(tileIndices.L != tileDepth)
        {
            return false;
        }

        if(tileIndices.X < minTileX || tileIndices.X > maxTileX)
        {
            return false;
        }

        if(tileIndices.Y < minTileY || tileIndices.Y > maxTileY)
        {
            return false;
        }

        return true;
    }
}
