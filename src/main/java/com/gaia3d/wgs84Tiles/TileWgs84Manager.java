package com.gaia3d.wgs84Tiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gaia3d.basic.structure.GaiaMesh;
import com.gaia3d.basic.structure.GaiaTriangle;
import com.gaia3d.basic.structure.GeographicExtension;
import com.gaia3d.quantizedMesh.QuantizedMesh;
import com.gaia3d.quantizedMesh.QuantizedMeshManager;
import com.gaia3d.reader.FileUtils;
import com.gaia3d.util.io.LittleEndianDataOutputStream;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffException;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.TransformException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class TileWgs84Manager {
    public int minTileDepth = 0;
    public int maxTileDepth = 15;

    public String tileTempDirectory = null;
    public String outputDirectory = null;
    public TerrainElevationDataManager terrainElevationDataManager = null; // new.***

    public String originalGeoTiffFolderPath;
    public String tempResizedGeoTiffFolderPath;

    // For each depth level, use a different folder.***
    public HashMap<Integer, String> map_depth_geoTiffFolderPath = new HashMap<Integer, String>();
    public HashMap<Integer, Double> map_depth_desiredPixelSizeXinMeters = new HashMap<Integer, Double>();

    public List<TileWgs84> tileWgs84List = new ArrayList<TileWgs84>();

    public String imageryType = "CRS84"; // "CRS84" or "WEB_MERCATOR"

    double vertexCoincidentError = 1e-11; // 1e-12 is good.***

    int triangleRefinementMaxIterations = 4;

    TerrainLayer terrainLayer = null;

    boolean originIsLeftUp = false; // false = origin is left-down (Cesium Tile System).***

    HashMap<Integer, Double> maxTriangleSizeForTileDepthMap = new HashMap<Integer, Double>();

    // constructor.***
    public TileWgs84Manager()
    {
        // Init default values.***
        // init the maxTriangleSizeForTileDepthMap.***
        for(int i=0; i<28; i++)
        {
            double tileSizeMeters = TileWgs84Utils.getTileSizeInMetersByDepth(i);
            double maxSize = tileSizeMeters/2.5;
            if(i < 11)
            {
                maxSize *= 0.2;
            }
            maxTriangleSizeForTileDepthMap.put(i, maxSize);
        }

        // init the map_depth_desiredPixelSizeXinMeters.***
        for(int depth = 0; depth <= 28; depth++)
        {
            double tileSizeMeters = TileWgs84Utils.getTileSizeInMetersByDepth(depth);
            double desiredPixelSizeXinMeters = tileSizeMeters / 256.0;
            this.map_depth_desiredPixelSizeXinMeters.put(depth, desiredPixelSizeXinMeters);
        }
    }
    public void makeTileMeshes() throws IOException, TransformException, FactoryException {

        GeographicExtension geographicExtension = this.terrainElevationDataManager.getRootGeographicExtension();


        double minLon = geographicExtension.getMinLongitudeDeg();
        double maxLon = geographicExtension.getMaxLongitudeDeg();
        double minLat = geographicExtension.getMinLatitudeDeg();
        double maxLat = geographicExtension.getMaxLatitudeDeg();

        // create the terrainLayer.***
        terrainLayer = new TerrainLayer();
        terrainLayer.bounds[0] = minLon;
        terrainLayer.bounds[1] = minLat;
        terrainLayer.bounds[2] = maxLon;
        terrainLayer.bounds[3] = maxLat;


        for(int depth = minTileDepth; depth <= maxTileDepth; depth += 1)
        {
            TilesRange tilesRange = new TilesRange();
            ArrayList<TileIndices> resultTileIndicesArray = null;

            if(depth == 0)
            {
                // in this case, the tile is the world. L0X0Y0 & L0X1Y0.***
                TileIndices tileIndices = new TileIndices();
                tileIndices.set(0, 0, 0);
                resultTileIndicesArray = new ArrayList<TileIndices>();
                resultTileIndicesArray.add(tileIndices);

                tileIndices = new TileIndices();
                tileIndices.set(1, 0, 0);
                resultTileIndicesArray.add(tileIndices);

                tilesRange.minTileX = 0;
                tilesRange.maxTileX = 1;
                tilesRange.minTileY = 0;
                tilesRange.maxTileY = 0;
            }
            else
            {
                resultTileIndicesArray = TileWgs84Utils.selectTileIndicesArray(depth, minLon, maxLon, minLat, maxLat, null, tilesRange, originIsLeftUp);
            }

            // Set terrainLayer.available of tileSet json.***
            terrainLayer.available.add(tilesRange);
            this.triangleRefinementMaxIterations = TileWgs84Utils.getRefinementIterations(depth);
            this.terrainElevationDataManager.deleteObjects();
            this.terrainElevationDataManager = new TerrainElevationDataManager(); // new.***
            this.terrainElevationDataManager.terrainElevationDataFolderPath = this.map_depth_geoTiffFolderPath.get(depth);
            this.terrainElevationDataManager.makeTerrainQuadTree();

            for (TileIndices tileIndices : resultTileIndicesArray)
            {
                //*********************************************************
                // For each tile, reset the terrainElevationDataManager.***
                //*********************************************************
                this.terrainElevationDataManager.deleteCoverage();

                TileWgs84 tile = new TileWgs84(null, this);
                tile.tileIndices = tileIndices;
                tile.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, imageryType, originIsLeftUp);
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
                    tile.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, imageryType, originIsLeftUp);

                    tile.loadTileAndSave4Children(tileIndices);
                    tile.deleteObjects();
                }
            }
        }

        // finally save the terrainLayer.json.***
        saveQuantizedMeshes();
    }

    public double getMaxTriangleSizeForTileDepth(int depth)
    {
        return maxTriangleSizeForTileDepthMap.get(depth);
    }
    public void makeSimpleTileMeshes_test() throws IOException, TransformException {

        GeographicExtension geographicExtension = this.terrainElevationDataManager.getRootGeographicExtension();
        double minLon = geographicExtension.getMinLongitudeDeg();
        double maxLon = geographicExtension.getMaxLongitudeDeg();
        double minLat = geographicExtension.getMinLatitudeDeg();
        double maxLat = geographicExtension.getMaxLatitudeDeg();

        // create the terrainLayer.***
        terrainLayer = new TerrainLayer();
        terrainLayer.bounds[0] = minLon;
        terrainLayer.bounds[1] = minLat;
        terrainLayer.bounds[2] = maxLon;
        terrainLayer.bounds[3] = maxLat;



        for(int depth = minTileDepth; depth <= maxTileDepth; depth += 1)
        {
            TilesRange tilesRange = new TilesRange();
            ArrayList<TileIndices> resultTileIndicesArray = TileWgs84Utils.selectTileIndicesArray(depth, minLon, maxLon, minLat, maxLat, null, tilesRange, originIsLeftUp);
            terrainLayer.available.add(tilesRange);

            this.triangleRefinementMaxIterations = TileWgs84Utils.getRefinementIterations(depth);

            if(depth == minTileDepth) {
                for (TileIndices tileIndices : resultTileIndicesArray) {
                    TileWgs84 tile = new TileWgs84(null, this);
                    tile.tileIndices = tileIndices;
                    tile.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, imageryType, originIsLeftUp);
                    boolean is1rstGeneration = false;
                    if (depth == minTileDepth) {
                        is1rstGeneration = true;
                    }
                    //tile.makeBigMesh(is1rstGeneration); // no.
                    // save a simple mesh.***
                    String tileTempDirectory = this.tileTempDirectory;
                    String outputDirectory = this.outputDirectory;
                    String tileFilePath = TileWgs84Utils.getTileFilePath(tileIndices.X, tileIndices.Y, tileIndices.L);
                    String tileFullPath = tileTempDirectory + "\\" + tileFilePath;

                    try {
                        TileWgs84 simpleTile = new TileWgs84(null, this);
                        simpleTile.tileIndices = tileIndices;
                        simpleTile.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, this.imageryType, this.originIsLeftUp);
                        simpleTile.createInitialMesh_test();
                        simpleTile.saveFile(simpleTile.mesh, tileFullPath);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    tileWgs84List.add(tile);
                }
            }

            if(depth < maxTileDepth)
            {
                // once finished the current depth, make initial tiles of the children.***
                for (TileIndices tileIndices : resultTileIndicesArray)
                {
                    // for each tile, load the tile, make 4 children, and save the children.***
                    TileWgs84 tile = new TileWgs84(null, this);
                    tile.tileIndices = tileIndices;
                    tile.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, imageryType, originIsLeftUp);

                    tile.save4Children_test(tileIndices);
                }
            }
        }

        // finally save the terrainLayer.json.***
        saveQuantizedMeshes();
    }

    public void saveQuantizedMeshes() throws IOException, TransformException {
        // 1rst save terrainLayer.json.***
        terrainLayer.saveJsonFile(outputDirectory, "layer.json");

        // 2nd save the quantized meshes.***
        // load each tile, and save the quantized mesh.***
        QuantizedMeshManager quantizedMeshManager = new QuantizedMeshManager();
        HashMap<Integer, TilesRange> tilesRangeMap = terrainLayer.getTilesRangeMap();
        for (Integer tileDepth : tilesRangeMap.keySet())
        {
            TilesRange tilesRange = tilesRangeMap.get(tileDepth);
            int minX = tilesRange.minTileX;
            int maxX = tilesRange.maxTileX;
            int minY = tilesRange.minTileY;
            int maxY = tilesRange.maxTileY;

            if(tileDepth >= 12)
            {
                int hola = 0;
            }

            for(int x = minX; x <= maxX; x++)
            {
                for(int y = minY; y <= maxY; y++)
                {
                    TileIndices tileIndices = new TileIndices();
                    tileIndices.X = x;
                    tileIndices.Y = y;
                    tileIndices.L = tileDepth;

                    TileWgs84 tile = loadTileWgs84(tileIndices);
                    if(tile != null)
                    {
                        // save the quantizedMesh.***
                        QuantizedMesh quantizedMesh = quantizedMeshManager.getQuantizedMeshFromTile(tile);
                        String tileFullPath = getQuantizedMeshTilePath(tileIndices);
                        String tileFolderPath = getQuantizedMeshTileFolderPath(tileIndices);
                        FileUtils.createAllFoldersIfNoExist(tileFolderPath);

                        FileOutputStream fileOutputStream = new FileOutputStream(tileFullPath);
                        LittleEndianDataOutputStream dataOutputStream = new LittleEndianDataOutputStream(fileOutputStream);

                        // delete the file if exists before save.***
                        FileUtils.deleteFileIfExists(tileFullPath);

                        quantizedMesh.saveDataOutputStream(dataOutputStream);
                        fileOutputStream.close();
                        int hola = 0;

                    }
                }
            }

        }

    }

    public String getTilePath(TileIndices tileIndices)
    {
        String tileTempDirectory = this.tileTempDirectory;
        String neighborFilePath = TileWgs84Utils.getTileFilePath(tileIndices.X, tileIndices.Y, tileIndices.L);
        String tileFullPath = tileTempDirectory + "\\" + neighborFilePath;
        return tileFullPath;
    }

    public String getQuantizedMeshTileFolderPath(TileIndices tileIndices)
    {
        String outputDirectory = this.outputDirectory;
        String neighborFolderPath = String.valueOf(tileIndices.L) + "\\" + String.valueOf(tileIndices.X);
        String tileFullPath = outputDirectory + "\\" + neighborFolderPath;
        return tileFullPath;
    }

    public String getQuantizedMeshTilePath(TileIndices tileIndices)
     {
        String outputDirectory = this.outputDirectory;
        String neighborFilePath = String.valueOf(tileIndices.L) + "\\" + String.valueOf(tileIndices.X) + "\\" + String.valueOf(tileIndices.Y);
        String tileFullPath = outputDirectory + "\\" + neighborFilePath + ".terrain";
        return tileFullPath;
    }

    public TileWgs84 loadOrCreateTileWgs84(TileIndices tileIndices) throws IOException, TransformException {
        // this function loads or creates a TileWgs84.***
        // check if exist LDTileFile.***

        if(!tileIndices.isValid())
        {
            return null;
        }

        String neighborFullPath = getTilePath(tileIndices);
        TileWgs84 neighborTile = new TileWgs84(null, this);
        if(!FileUtils.isFileExists(neighborFullPath))
        {
            // create the Tile.***
            System.out.println("Creating tile: CREATE - * - CREATE : " + tileIndices.X + ", " + tileIndices.Y + ", " + tileIndices.L);
            neighborTile.tileIndices = tileIndices;
            String imageryType = this.imageryType;
            neighborTile.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, imageryType, originIsLeftUp);
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
            neighborTile.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, imageryType, originIsLeftUp);
            neighborTile.loadFile(neighborFullPath);
        }


        return neighborTile;
    }

    public boolean existTileFile(TileIndices tileIndices)
    {
        String neighborFullPath = getTilePath(tileIndices);
        if(!FileUtils.isFileExists(neighborFullPath))
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public TileWgs84 loadTileWgs84(TileIndices tileIndices) throws IOException, TransformException {
        // this function loads or creates a TileWgs84.***
        // check if exist LDTileFile.***

        String neighborFullPath = getTilePath(tileIndices);
        TileWgs84 neighborTile = null;
        if(!FileUtils.isFileExists(neighborFullPath))
        {
            return null;
        }
        else
        {
            // load the Tile.***
            neighborTile = new TileWgs84(null, this);
            System.out.println("Loading tile: LOAD - * - LOAD :" + tileIndices.X + ", " + tileIndices.Y + ", " + tileIndices.L);
            neighborTile.tileIndices = tileIndices;
            neighborTile.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, imageryType, originIsLeftUp);
            neighborTile.loadFile(neighborFullPath);
        }

        return neighborTile;
    }

    public void resizeGeotiffSet(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException, TransformException {
        // load all geoTiffFiles.***
        ArrayList<String> geoTiffFileNames = new ArrayList<String>();
        com.gaia3d.reader.FileUtils.getFileNames(terrainElevationDataFolderPath, ".tif", geoTiffFileNames);

        if(currentFolderPath == null)
        {
            currentFolderPath = "";
        }

        GaiaGeoTiffManager gaiaGeoTiffManager = new GaiaGeoTiffManager();
        GeometryFactory gf = new GeometryFactory();

        // now load all geotiff and make geotiff geoExtension data.***
        int geoTiffCount = geoTiffFileNames.size();
        for(int i=0; i<geoTiffCount; i++)
        {
            String geoTiffFileName = geoTiffFileNames.get(i);
            String geoTiffFilePath = terrainElevationDataFolderPath + "\\" + geoTiffFileName;

            GridCoverage2D originalGridCoverage2D = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(geoTiffFilePath);

            CoordinateReferenceSystem crsTarget = originalGridCoverage2D.getCoordinateReferenceSystem2D();
            if (!(crsTarget instanceof ProjectedCRS || crsTarget instanceof GeographicCRS)) {
                //throw new GeoTiffException( null, "The supplied grid coverage uses an unsupported crs! You are allowed to use only projected and geographic coordinate reference systems", null);
                continue;
            }

            Vector2d pixelSizeMeters = GaiaGeoTiffUtils.getPixelSizeMeters(originalGridCoverage2D);

            int minDepth = this.minTileDepth;
            int maxDepth = this.maxTileDepth;
            for(int depth = minDepth; depth <= maxDepth; depth += 1)
            {
                double desiredPixelSizeXinMeters = this.map_depth_desiredPixelSizeXinMeters.get(depth);
                double desiredPixelSizeYinMeters = desiredPixelSizeXinMeters;
                if(desiredPixelSizeXinMeters < pixelSizeMeters.x)
                {
                    // In this case just assign the originalGeoTiffFolderPath.***
                    this.map_depth_geoTiffFolderPath.put(depth, this.originalGeoTiffFolderPath);
                    continue;
                }

                String depthStr = String.valueOf(depth);
                String resizedGeoTiffFolderPath = this.tempResizedGeoTiffFolderPath + "\\" + depthStr + "\\" + currentFolderPath;
                String resizedGeoTiffFilePath = resizedGeoTiffFolderPath + "\\" + geoTiffFileName;

                // check if exist the file.***
                if(com.gaia3d.reader.FileUtils.isFileExists(resizedGeoTiffFilePath))
                {
                    // in this case, just assign the resizedGeoTiffFolderPath.***
                    String resizedGeoTiffSETFolderPath_forThisDepth = this.tempResizedGeoTiffFolderPath + "\\" + depthStr;
                    this.map_depth_geoTiffFolderPath.put(depth, resizedGeoTiffSETFolderPath_forThisDepth);
                    continue;
                }

                // in this case, resize the geotiff.***
                GridCoverage2D resizedGridCoverage2D = gaiaGeoTiffManager.getResizedCoverage2D(originalGridCoverage2D, desiredPixelSizeXinMeters, desiredPixelSizeYinMeters);

                com.gaia3d.reader.FileUtils.createAllFoldersIfNoExist(resizedGeoTiffFolderPath);
                gaiaGeoTiffManager.saveGridCoverage2D(resizedGridCoverage2D, resizedGeoTiffFilePath);

                String resizedGeoTiffSETFolderPath_forThisDepth = this.tempResizedGeoTiffFolderPath + "\\" + depthStr;
                this.map_depth_geoTiffFolderPath.put(depth, resizedGeoTiffSETFolderPath_forThisDepth);
            }

            int hola = 0;
        }

        // now check if exist folders inside the terrainElevationDataFolderPath.***
        ArrayList<String> folderNames = new ArrayList<String>();
        com.gaia3d.reader.FileUtils.getFolderNames(terrainElevationDataFolderPath, folderNames);
        int folderCount = folderNames.size();
        String auxFolderPath = currentFolderPath;
        for(int i=0; i<folderCount; i++)
        {
            String folderName = folderNames.get(i);
            auxFolderPath = currentFolderPath + "\\" + folderName;
            String folderPath = terrainElevationDataFolderPath + "\\" + folderName;
            resizeGeotiffSet(folderPath, auxFolderPath);
        }

        int hola = 0;
    }

}
