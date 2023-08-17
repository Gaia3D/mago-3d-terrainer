package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GeographicExtension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class TileWgs84Manager {
    public int minTileDepth = 26;
    public int maxTileDepth = 27;

    public String tileTempDirectory = null;
    public String outputDirectory = null;

    public TerrainElevationData terrainElevationData = null;

    public List<TileWgs84> tileWgs84List = new ArrayList<TileWgs84>();


    public void makeTileMeshes() {

        GeographicExtension geographicExtension = this.terrainElevationData.geographicExtension;
        double minLon = geographicExtension.getMinLongitudeDeg();
        double maxLon = geographicExtension.getMaxLongitudeDeg();
        double minLat = geographicExtension.getMinLatitudeDeg();
        double maxLat = geographicExtension.getMaxLatitudeDeg();

        for(int depth = minTileDepth; depth <= maxTileDepth; depth+=1)
        {
            ArrayList<TileIndices> resultTileIndicesArray = TileWgs84Utils.selectTileIndicesArray(depth, minLon, maxLon, minLat, maxLat, null);

            for (TileIndices tileIndices : resultTileIndicesArray)
            {


                TileWgs84 tile = new TileWgs84(null, this);
                tile.tileIndices = tileIndices;
                tile.makeMesh();
                tileWgs84List.add(tile);
            }

            int hola = 0;
        }

    }


}
