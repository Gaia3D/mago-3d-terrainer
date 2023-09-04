package com.gaia3d.wgs84Tiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gaia3d.basic.structure.GaiaMesh;
import com.gaia3d.basic.structure.GeographicExtension;
import com.gaia3d.reader.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.joml.Vector3d;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class TileWgs84Manager {
    public int minTileDepth = 10;
    public int maxTileDepth = 15;

    public String tileTempDirectory = null;
    public String outputDirectory = null;

    public TerrainElevationData terrainElevationData = null;

    public List<TileWgs84> tileWgs84List = new ArrayList<TileWgs84>();

    public String imageryType = "CRS84"; // "CRS84" or "WEB_MERCATOR"

    double vertexCoincidentError = 1e-11; // 1e-12 is good.***

    int triangleRefinementMaxIterations = 9;

    TerrainLayer terrainLayer = null;


    public void makeTileMeshes() throws IOException, TransformException {

        GeographicExtension geographicExtension = this.terrainElevationData.geographicExtension;
        double minLon = geographicExtension.getMinLongitudeDeg();
        double maxLon = geographicExtension.getMaxLongitudeDeg();
        double minLat = geographicExtension.getMinLatitudeDeg();
        double maxLat = geographicExtension.getMaxLatitudeDeg();

        // create the terrainLayer.***
        terrainLayer = new TerrainLayer();
        terrainLayer.name = "insert name here";
        terrainLayer.description = "insert description here";
        terrainLayer.version = "1.1.0";
        terrainLayer.attribution = "insert attribution here";
        terrainLayer.template = "terrain";
        terrainLayer.legend = "insert legend here";
        terrainLayer.scheme = "tms";
        terrainLayer.tiles = new String[1];
        terrainLayer.tiles[0] = "{z}/{x}/{y}.terrain?v={version}";
        terrainLayer.projection = "EPSG:4326";
        terrainLayer.bounds = new double[4];
        terrainLayer.bounds[0] = minLon;
        terrainLayer.bounds[1] = minLat;
        terrainLayer.bounds[2] = maxLon;
        terrainLayer.bounds[3] = maxLat;


        for(int depth = minTileDepth; depth <= maxTileDepth; depth += 1)
        {
            TilesRange tilesRange = new TilesRange();
            ArrayList<TileIndices> resultTileIndicesArray = TileWgs84Utils.selectTileIndicesArray(depth, minLon, maxLon, minLat, maxLat, null, tilesRange);
            terrainLayer.available.add(tilesRange);

            if(depth == minTileDepth)
            {
                this.triangleRefinementMaxIterations = 8;
            }
            else
            {
                this.triangleRefinementMaxIterations = 1;
            }

            for (TileIndices tileIndices : resultTileIndicesArray)
            {
                TileWgs84 tile = new TileWgs84(null, this);
                tile.tileIndices = tileIndices;
                tile.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, imageryType);
                boolean is1rstGeneration = false;
                if(depth == minTileDepth)
                {
                    is1rstGeneration = true;
                }
                tile.makeBigMesh(is1rstGeneration);
                tileWgs84List.add(tile);
            }

            if(depth < maxTileDepth)
            {
                // once finished the current depth, make initial tiles of the children.***
                for (TileIndices tileIndices : resultTileIndicesArray)
                {
                    // for each tile, load the tile, make 4 children, and save the children.***
                    TileWgs84 tile = new TileWgs84(null, this);
                    tile.tileIndices = tileIndices;
                    tile.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, imageryType);

                    tile.loadTileAndSave4Children(tileIndices);
                }
            }
        }

        // finally save the terrainLayer.json.***
        saveQuantizedMeshes();
    }

    public void saveQuantizedMeshes() throws IOException, TransformException {
        // 1rst save terrainLayer.json.***
        terrainLayer.saveJsonFile(outputDirectory, "layer.json");

    }

    public String getTilePath(TileIndices tileIndices)
    {
        String tileTempDirectory = this.tileTempDirectory;
        //String outputDirectory = this.outputDirectory;
        String neighborFilePath = TileWgs84Utils.getTileFilePath(tileIndices.X, tileIndices.Y, tileIndices.L);
        String tileFullPath = tileTempDirectory + "\\" + neighborFilePath;
        return tileFullPath;
    }

    public TileWgs84 loadOrCreateTileWgs84(TileIndices tileIndices) throws IOException, TransformException {
        // this function loads or creates a TileWgs84.***
        // check if exist LDTileFile.***

        String neighborFullPath = getTilePath(tileIndices);
        TileWgs84 neighborTile = new TileWgs84(null, this);
        if(!FileUtils.isFileExists(neighborFullPath))
        {
            // create the Tile.***
            System.out.println("Creating tile: CREATE - * - CREATE : " + tileIndices.X + ", " + tileIndices.Y + ", " + tileIndices.L);
            neighborTile.tileIndices = tileIndices;
            String imageryType = this.imageryType;
            neighborTile.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, imageryType);
            neighborTile.createInitialMesh();
            if(neighborTile.mesh == null)
            {
                // error.***
                System.out.println("Error: neighborTile.mesh == null");
            }

            neighborTile.saveFile(neighborTile.mesh, neighborFullPath);
        }
        else
        {
            // load the Tile.***
            System.out.println("Loading tile: LOAD - * - LOAD :" + tileIndices.X + ", " + tileIndices.Y + ", " + tileIndices.L);
            neighborTile.tileIndices = tileIndices;
            neighborTile.loadFile(neighborFullPath);
        }


        return neighborTile;
    }

    public TileWgs84 loadTileWgs84(TileIndices tileIndices) throws IOException, TransformException {
        // this function loads or creates a TileWgs84.***
        // check if exist LDTileFile.***

        String neighborFullPath = getTilePath(tileIndices);
        TileWgs84 neighborTile = new TileWgs84(null, this);
        if(!FileUtils.isFileExists(neighborFullPath))
        {
            return null;
        }
        else
        {
            // load the Tile.***
            System.out.println("Loading tile: LOAD - * - LOAD :" + tileIndices.X + ", " + tileIndices.Y + ", " + tileIndices.L);
            neighborTile.tileIndices = tileIndices;
            neighborTile.loadFile(neighborFullPath);
        }


        return neighborTile;
    }

}
