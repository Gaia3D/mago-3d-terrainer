package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GeographicExtension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class TileWgs84Manager {
    public GeographicExtension geographicExtension = new GeographicExtension();
    public GridCoverage2D coverage = null;

    public int minTileDepth = 26;
    public int maxTileDepth = 27;

    public void startTiling() {

        double minLon = geographicExtension.getMinLongitudeDeg();
        double maxLon = geographicExtension.getMaxLongitudeDeg();
        double minLat = geographicExtension.getMinLatitudeDeg();
        double maxLat = geographicExtension.getMaxLatitudeDeg();

        for(int depth = minTileDepth; depth <= maxTileDepth; depth+=1)
        {
            ArrayList<TileIndices> resultTileIndicesArray = TileWgs84Utils.selectTileIndicesArray(depth, minLon, maxLon, minLat, maxLat, null);

            for (TileIndices tileIndices : resultTileIndicesArray)
            {
                // for current tile, create the 8 neighbor tiles.
                //  +----------+----------+----------+
                //  |          |          |          |
                //  |  LUTile  |   UTile  |  RUTile  |
                //  |          |          |          |
                //  +----------+----------+----------+
                //  |          |          |          |
                //  |  LTile   | currTile |  RTile   |
                //  |          |          |          |
                //  +----------+----------+----------+
                //  |          |          |          |
                //  |  LDTile  |  DTile   |  RDTile  |
                //  |          |          |          |
                //  +----------+----------+----------+

                TileWgs84 tile = new TileWgs84(this);
                //tile.tileIndices = tileIndices;
                //tile.geographicExtension = geographicExtension;
                //tile.coverage = coverage;
                //tile.startTiling();
            }

            int hola = 0;
        }

    }


}
