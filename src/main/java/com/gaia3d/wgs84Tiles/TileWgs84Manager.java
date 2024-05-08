package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GaiaTriangle;
import com.gaia3d.basic.structure.GeographicExtension;
import com.gaia3d.quantizedMesh.QuantizedMesh;
import com.gaia3d.quantizedMesh.QuantizedMeshManager;
import com.gaia3d.reader.FileUtils;
import com.gaia3d.util.io.LittleEndianDataOutputStream;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.joml.Vector2d;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.TransformException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
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
    public HashMap<Integer, Double> map_depth_maxDiffBetweenGeoTiffSampleAndTrianglePlane = new HashMap<Integer, Double>();
    public List<TileWgs84> tileWgs84List = new ArrayList<TileWgs84>();
    public String imageryType = "CRS84"; // "CRS84" or "WEB_MERCATOR"
    // tileRasterSize : when triangles refinement, we use a DEM raster of this size.***
    public int tileRasterSize = 256;
    public int refinementStrength = 1; // 1 = normal.
    @Setter
    @Getter
    int geoTiffFilesCount = 0;
    @Setter
    @Getter
    String uniqueGeoTiffFilePath = null; // use this if there is only one geoTiff file.***
    double vertexCoincidentError = 1e-11; // 1e-12 is good.***
    int triangleRefinementMaxIterations = 5;
    TerrainLayer terrainLayer = null;
    boolean originIsLeftUp = false; // false = origin is left-down (Cesium Tile System).***
    HashMap<Integer, Double> maxTriangleSizeForTileDepthMap = new HashMap<Integer, Double>();
    HashMap<Integer, Double> minTriangleSizeForTileDepthMap = new HashMap<Integer, Double>();
    @Setter
    @Getter
    boolean calculateNormals = true;
    ArrayList<TerrainElevationData> memSave_terrainElevDatasArray = new ArrayList<TerrainElevationData>();

    Vector2d memSave_pixelSizeDegrees = new Vector2d();

    ArrayList<GaiaTriangle> memSave_trianglesArray = new ArrayList<GaiaTriangle>();

    // constructor.***
    public TileWgs84Manager() {
        // Init default values.***
        // init the maxTriangleSizeForTileDepthMap.***
        for (int i = 0; i < 28; i++) {
            double tileSizeMeters = TileWgs84Utils.getTileSizeInMetersByDepth(i);
            double maxSize = tileSizeMeters / 2.5;
            if (i < 11) {
                maxSize *= 0.2;
            }
            maxTriangleSizeForTileDepthMap.put(i, maxSize);
            double minSize = tileSizeMeters * 0.05;
            minTriangleSizeForTileDepthMap.put(i, minSize);
        }

        // init the map_depth_desiredPixelSizeXinMeters.***
        for (int depth = 0; depth <= 28; depth++) {
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

        if(this.calculateNormals)
        {
            terrainLayer.extensions.add("octvertexnormals");
        }

        for (int depth = minTileDepth; depth <= maxTileDepth; depth += 1) {
            TilesRange tilesRange = new TilesRange();

            if (depth == 0) {
                // in this case, the tile is the world. L0X0Y0 & L0X1Y0.***
                tilesRange.minTileX = 0;
                tilesRange.maxTileX = 1;
                tilesRange.minTileY = 0;
                tilesRange.maxTileY = 0;
            }
            else {
                TileWgs84Utils.selectTileIndicesArray(depth, minLon, maxLon, minLat, maxLat, tilesRange, originIsLeftUp);
            }

            // Set terrainLayer.available of tileSet json.***
            terrainLayer.available.add(tilesRange); // this is used to save the terrainLayer.json.***
            this.triangleRefinementMaxIterations = TileWgs84Utils.getRefinementIterations(depth);
            if (this.geoTiffFilesCount == 1) {
                if (this.terrainElevationDataManager == null) {
                    this.terrainElevationDataManager = new TerrainElevationDataManager(); // new.***
                    this.terrainElevationDataManager.setUniqueGeoTiffFilePath(this.uniqueGeoTiffFilePath);
                    this.terrainElevationDataManager.MakeUniqueTerrainElevationData();
                }
                this.terrainElevationDataManager.deleteObjects(); // here deletes tileRasters.***
            } else {
                this.terrainElevationDataManager.deleteObjects();
                this.terrainElevationDataManager = new TerrainElevationDataManager(); // new.***
                this.terrainElevationDataManager.terrainElevationDataFolderPath = this.map_depth_geoTiffFolderPath.get(depth);
                this.terrainElevationDataManager.makeTerrainQuadTree();
            }

            // now, subdivide the tilesRange.***
            int maxCol = 80;
            int maxRow = 80;
            List<TilesRange> subDividedTilesRanges = TileWgs84Utils.subDivideTileRange(tilesRange, maxCol, maxRow, null);

            int subDividedRangesCount = subDividedTilesRanges.size();
            for (int i = 0; i < subDividedRangesCount; i++) {
                TilesRange subDividedTilesRange = subDividedTilesRanges.get(i);
                TileMatrix tileMatrix = new TileMatrix(subDividedTilesRange, this);
                boolean is1rstGeneration = depth == minTileDepth;

                tileMatrix.makeMatrixMesh(is1rstGeneration);
                int hola = 0;
            }

        }

        // finally save the terrainLayer.json.***
        terrainLayer.saveJsonFile(outputDirectory, "layer.json");
    }

    public double getMaxTriangleSizeForTileDepth(int depth) {
        return maxTriangleSizeForTileDepthMap.get(depth);
    }

    public double getMinTriangleSizeForTileDepth(int depth) {
        return minTriangleSizeForTileDepthMap.get(depth);
    }

    public double getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(int depth) {
        if (map_depth_maxDiffBetweenGeoTiffSampleAndTrianglePlane.containsKey(depth)) {
            return map_depth_maxDiffBetweenGeoTiffSampleAndTrianglePlane.get(depth);
        } else {
            double maxDiff = TileWgs84Utils.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(depth);
            map_depth_maxDiffBetweenGeoTiffSampleAndTrianglePlane.put(depth, maxDiff);
            return map_depth_maxDiffBetweenGeoTiffSampleAndTrianglePlane.get(depth);
        }

    }


    public String getTilePath(TileIndices tileIndices) {
        String tileTempDirectory = this.tileTempDirectory;
        String neighborFilePath = TileWgs84Utils.getTileFilePath(tileIndices.X, tileIndices.Y, tileIndices.L);
        String tileFullPath = tileTempDirectory + File.separator + neighborFilePath;
        return tileFullPath;
    }

    public String getQuantizedMeshTileFolderPath(TileIndices tileIndices) {
        String outputDirectory = this.outputDirectory;
        String neighborFolderPath = tileIndices.L + File.separator + tileIndices.X;
        String tileFullPath = outputDirectory + File.separator + neighborFolderPath;
        return tileFullPath;
    }

    public String getQuantizedMeshTilePath(TileIndices tileIndices) {
        String outputDirectory = this.outputDirectory;
        String neighborFilePath = tileIndices.L + File.separator + tileIndices.X + File.separator + tileIndices.Y;
        String tileFullPath = outputDirectory + File.separator + neighborFilePath + ".terrain";
        return tileFullPath;
    }

    public TileWgs84 loadOrCreateTileWgs84(TileIndices tileIndices) throws IOException, TransformException {
        // this function loads or creates a TileWgs84.***
        // check if exist LDTileFile.***

        if (!tileIndices.isValid()) {
            return null;
        }

        String neighborFullPath = getTilePath(tileIndices);
        TileWgs84 neighborTile = new TileWgs84(null, this);
        if (!FileUtils.isFileExists(neighborFullPath)) {
            // create the Tile.***
            log.debug("Creating tile: CREATE - * - CREATE : " + tileIndices.X + ", " + tileIndices.Y + ", " + tileIndices.L);
            neighborTile.tileIndices = tileIndices;
            String imageryType = this.imageryType;
            neighborTile.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, imageryType, originIsLeftUp);
            neighborTile.createInitialMesh();
            if (neighborTile.mesh == null) {
                // error.***
                log.error("Error: neighborTile.mesh == null");
            }

            neighborTile.saveFile(neighborTile.mesh, neighborFullPath);
        } else {
            // load the Tile.***
            neighborTile.tileIndices = tileIndices;
            neighborTile.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, imageryType, originIsLeftUp);
            neighborTile.loadFile(neighborFullPath);
        }


        return neighborTile;
    }

    public boolean existTileFile(TileIndices tileIndices) {
        String neighborFullPath = getTilePath(tileIndices);
        return FileUtils.isFileExists(neighborFullPath);
    }

    public TileWgs84 loadTileWgs84(TileIndices tileIndices) throws IOException, TransformException {
        // this function loads or creates a TileWgs84.***
        // check if exist LDTileFile.***

        String neighborFullPath = getTilePath(tileIndices);
        TileWgs84 neighborTile = null;
        if (!FileUtils.isFileExists(neighborFullPath)) {
            //log.error("Error: neighborFullPath is not exist: " + neighborFullPath);
            return null;
        } else {
            log.debug("Loading tile: LOAD - * - LOAD : " + tileIndices.X + ", " + tileIndices.Y + ", " + tileIndices.L);
            // load the Tile.***
            neighborTile = new TileWgs84(null, this);
            neighborTile.tileIndices = tileIndices;
            neighborTile.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, imageryType, originIsLeftUp);
            neighborTile.loadFile(neighborFullPath);
        }

        return neighborTile;
    }


    public void processResizeGeotiffs(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException, TransformException {
        // 1rst check geoTiff files count.***
        ArrayList<String> geoTiffFilePaths = new ArrayList<String>();
        FileUtils.getFilePathsByExtension(terrainElevationDataFolderPath, "tif", geoTiffFilePaths, true);

        int geotiffCount = geoTiffFilePaths.size();

        this.setGeoTiffFilesCount(geotiffCount);

        System.out.println("resizing geoTiffs: geoTiffsCount : " + geotiffCount);

        if(geotiffCount == 1)
        {
            this.uniqueGeoTiffFilePath = geoTiffFilePaths.get(0);
        } else {
            // 2nd resize the geotiffs.***
            resizeGeotiffSet(terrainElevationDataFolderPath, currentFolderPath);
        }

    }

    public void resizeGeotiffSet(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException, TransformException {
        // load all geoTiffFiles.***
        ArrayList<String> geoTiffFileNames = new ArrayList<String>();
        com.gaia3d.reader.FileUtils.getFileNames(terrainElevationDataFolderPath, ".tif", geoTiffFileNames);

        if (currentFolderPath == null) {
            currentFolderPath = "";
        }

        GaiaGeoTiffManager gaiaGeoTiffManager = new GaiaGeoTiffManager();
        GeometryFactory gf = new GeometryFactory();

        // now load all geotiff and make geotiff geoExtension data.***
        int geoTiffCount = geoTiffFileNames.size();
        for (int i = 0; i < geoTiffCount; i++) {
            String geoTiffFileName = geoTiffFileNames.get(i);
            String geoTiffFilePath = terrainElevationDataFolderPath + File.separator + geoTiffFileName;

            GridCoverage2D originalGridCoverage2D = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(geoTiffFilePath);

            CoordinateReferenceSystem crsTarget = originalGridCoverage2D.getCoordinateReferenceSystem2D();
            if (!(crsTarget instanceof ProjectedCRS || crsTarget instanceof GeographicCRS)) {
                //throw new GeoTiffException( null, "The supplied grid coverage uses an unsupported crs! You are allowed to use only projected and geographic coordinate reference systems", null);
                continue;
            }

            Vector2d pixelSizeMeters = GaiaGeoTiffUtils.getPixelSizeMeters(originalGridCoverage2D);

            int minDepth = this.minTileDepth;
            int maxDepth = this.maxTileDepth;
            for (int depth = minDepth; depth <= maxDepth; depth += 1) {
                double desiredPixelSizeXinMeters = this.map_depth_desiredPixelSizeXinMeters.get(depth);
                double desiredPixelSizeYinMeters = desiredPixelSizeXinMeters;

                //****************************************************************************************
                if (desiredPixelSizeXinMeters < pixelSizeMeters.x) {
                    // In this case just assign the originalGeoTiffFolderPath.***
                    this.map_depth_geoTiffFolderPath.put(depth, this.originalGeoTiffFolderPath);
                    continue;
                }

                String depthStr = String.valueOf(depth);
                String resizedGeoTiffFolderPath = this.tempResizedGeoTiffFolderPath + File.separator + depthStr + File.separator + currentFolderPath;
                String resizedGeoTiffFilePath = resizedGeoTiffFolderPath + File.separator + geoTiffFileName;

                // check if exist the file.***
                if (com.gaia3d.reader.FileUtils.isFileExists(resizedGeoTiffFilePath)) {
                    // in this case, just assign the resizedGeoTiffFolderPath.***
                    String resizedGeoTiffSETFolderPath_forThisDepth = this.tempResizedGeoTiffFolderPath + File.separator + depthStr;
                    this.map_depth_geoTiffFolderPath.put(depth, resizedGeoTiffSETFolderPath_forThisDepth);
                    continue;
                }

                // in this case, resize the geotiff.***
                GridCoverage2D resizedGridCoverage2D = gaiaGeoTiffManager.getResizedCoverage2D(originalGridCoverage2D, desiredPixelSizeXinMeters, desiredPixelSizeYinMeters);

                com.gaia3d.reader.FileUtils.createAllFoldersIfNoExist(resizedGeoTiffFolderPath);
                gaiaGeoTiffManager.saveGridCoverage2D(resizedGridCoverage2D, resizedGeoTiffFilePath);

                String resizedGeoTiffSETFolderPath_forThisDepth = this.tempResizedGeoTiffFolderPath + File.separator + depthStr;
                this.map_depth_geoTiffFolderPath.put(depth, resizedGeoTiffSETFolderPath_forThisDepth);
            }

            int hola = 0;
        }

        // now check if exist folders inside the terrainElevationDataFolderPath.***
        ArrayList<String> folderNames = new ArrayList<String>();
        com.gaia3d.reader.FileUtils.getFolderNames(terrainElevationDataFolderPath, folderNames);
        int folderCount = folderNames.size();
        String auxFolderPath = currentFolderPath;
        for (int i = 0; i < folderCount; i++) {
            String folderName = folderNames.get(i);
            auxFolderPath = currentFolderPath + File.separator + folderName;
            String folderPath = terrainElevationDataFolderPath + File.separator + folderName;
            resizeGeotiffSet(folderPath, auxFolderPath);
        }

        System.gc();

        int hola = 0;
    }

}
