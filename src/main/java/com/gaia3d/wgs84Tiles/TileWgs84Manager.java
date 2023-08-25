package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GeographicExtension;
import com.gaia3d.reader.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.joml.Vector3d;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TileWgs84Manager {
    public int minTileDepth = 26;
    public int maxTileDepth = 27;

    public String tileTempDirectory = null;
    public String outputDirectory = null;

    public TerrainElevationData terrainElevationData = null;

    public List<TileWgs84> tileWgs84List = new ArrayList<TileWgs84>();

    public String imageryType = "CRS84"; // "CRS84" or "WEB_MERCATOR"

    double vertexCoincidentError = 0.0000000000001;


    public void makeTileMeshes() throws IOException, TransformException {

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
                tile.makeBigMesh();
                tileWgs84List.add(tile);
            }

            int hola = 0;
        }

    }

    public TileWgs84 loadOrCreateTileWgs84(TileIndices tileIndices) throws IOException, TransformException {
        // this function loads or creates a TileWgs84.***
        // check if exist LDTileFile.***
        String tileTempDirectory = this.tileTempDirectory;
        String outputDirectory = this.outputDirectory;
        String neighborFilePath = TileWgs84Utils.getTileFilePath(tileIndices.X, tileIndices.Y, tileIndices.L);
        String neighborFullPath = tileTempDirectory + "\\" + neighborFilePath;
        TileWgs84 neighborTile = new TileWgs84(null, this);
        if(!FileUtils.isFileExists(neighborFullPath))
        {
            // create the Tile.***
            neighborTile.tileIndices = tileIndices;
            String imageryType = this.imageryType;
            neighborTile.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, imageryType);
            neighborTile.createInitialMesh();
            neighborTile.saveFile(neighborFullPath);
        }
        else
        {
            // load the Tile.***
            neighborTile.tileIndices = tileIndices;
            neighborTile.loadFile(neighborFullPath);
        }
        return neighborTile;
    }

}
