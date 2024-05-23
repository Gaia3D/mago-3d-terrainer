package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GaiaTriangle;
import com.gaia3d.basic.structure.GeographicExtension;
import com.gaia3d.reader.FileUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.joml.Vector2d;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Slf4j
public class TileWgs84Manager {
    private int minTileDepth = 0;
    private int maxTileDepth = 15;

    private String tileTempDirectory = null;
    private String outputDirectory = null;
    private TerrainElevationDataManager terrainElevationDataManager = null; // new
    private String originalGeoTiffFolderPath;
    private String tempResizedGeoTiffFolderPath;
    // For each depth level, use a different folder
    private Map<Integer, String> mapDepthGeoTiffFolderPath = new HashMap<>();
    private Map<Integer, Double> mapDepthDesiredPixelSizeXinMeters = new HashMap<>();
    private Map<Integer, Double> mapDepthMaxDiffBetweenGeoTiffSampleAndTrianglePlane = new HashMap<>();
    private List<TileWgs84> tileWgs84List = new ArrayList<>();
    private String imageryType = "CRS84"; // "CRS84" or "WEB_MERCATOR"
    // tileRasterSize : when triangles refinement, we use a DEM raster of this size
    private int tileRasterSize = 256;
    private int refinementStrength = 1; // 1 = normal.

    private int geoTiffFilesCount = 0;

    private String uniqueGeoTiffFilePath = null; // use this if there is only one geoTiff file
    private double vertexCoincidentError = 1e-11; // 1e-12 is good
    private int triangleRefinementMaxIterations = 5;
    private TerrainLayer terrainLayer = null;
    private boolean originIsLeftUp = false; // false = origin is left-down (Cesium Tile System)
    private Map<Integer, Double> maxTriangleSizeForTileDepthMap = new HashMap<>();
    private Map<Integer, Double> minTriangleSizeForTileDepthMap = new HashMap<>();

    private boolean calculateNormals = true;
    private List<TerrainElevationData> memSaveTerrainElevDataArray = new ArrayList<>();

    private Vector2d memSavePixelSizeDegrees = new Vector2d();

    private List<GaiaTriangle> memSaveTrianglesArray = new ArrayList<>();

    // constructor
    public TileWgs84Manager() {
        // Init default values
        // init the maxTriangleSizeForTileDepthMap
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

        // init the map_depth_desiredPixelSizeXinMeters
        for (int depth = 0; depth <= 28; depth++) {
            double tileSizeMeters = TileWgs84Utils.getTileSizeInMetersByDepth(depth);
            double desiredPixelSizeXinMeters = tileSizeMeters / 256.0;
            this.mapDepthDesiredPixelSizeXinMeters.put(depth, desiredPixelSizeXinMeters);
        }
    }

    public void makeTileMeshes() throws IOException, TransformException, FactoryException {
        GeographicExtension geographicExtension = this.terrainElevationDataManager.getRootGeographicExtension();

        double minLon = geographicExtension.getMinLongitudeDeg();
        double maxLon = geographicExtension.getMaxLongitudeDeg();
        double minLat = geographicExtension.getMinLatitudeDeg();
        double maxLat = geographicExtension.getMaxLatitudeDeg();

        // create the terrainLayer
        terrainLayer = new TerrainLayer();
        double[] bounds = terrainLayer.getBounds();
        bounds[0] = minLon;
        bounds[1] = minLat;
        bounds[2] = maxLon;
        bounds[3] = maxLat;

        if (this.calculateNormals) {
            terrainLayer.addExtension("octvertexnormals");
        }

        for (int depth = minTileDepth; depth <= maxTileDepth; depth += 1) {
            TilesRange tilesRange = new TilesRange();

            if (depth == 0) {
                // in this case, the tile is the world. L0X0Y0 & L0X1Y0
                tilesRange.setMinTileX(0);
                tilesRange.setMaxTileX(1);
                tilesRange.setMinTileY(0);
                tilesRange.setMaxTileY(0);
            } else {
                TileWgs84Utils.selectTileIndicesArray(depth, minLon, maxLon, minLat, maxLat, tilesRange, originIsLeftUp);
            }

            // Set terrainLayer.available of tileSet json
            terrainLayer.getAvailable().add(tilesRange); // this is used to save the terrainLayer.json
            this.triangleRefinementMaxIterations = TileWgs84Utils.getRefinementIterations(depth);
            if (this.geoTiffFilesCount == 1) {
                if (this.terrainElevationDataManager == null) {
                    this.terrainElevationDataManager = new TerrainElevationDataManager(); // new
                    this.terrainElevationDataManager.setUniqueGeoTiffFilePath(this.uniqueGeoTiffFilePath);
                    this.terrainElevationDataManager.MakeUniqueTerrainElevationData();
                }
                this.terrainElevationDataManager.deleteObjects(); // here deletes tileRasters
            } else {
                this.terrainElevationDataManager.deleteObjects();
                this.terrainElevationDataManager = new TerrainElevationDataManager(); // new
                this.terrainElevationDataManager.setTerrainElevationDataFolderPath(this.mapDepthGeoTiffFolderPath.get(depth));
                this.terrainElevationDataManager.makeTerrainQuadTree();
            }

            // now, subdivide the tilesRange
            int maxCol = 80;
            int maxRow = 80;
            List<TilesRange> subDividedTilesRanges = TileWgs84Utils.subDivideTileRange(tilesRange, maxCol, maxRow, null);

            for (TilesRange subDividedTilesRange : subDividedTilesRanges) {
                TileMatrix tileMatrix = new TileMatrix(subDividedTilesRange, this);
                boolean is1rstGeneration = depth == minTileDepth;

                tileMatrix.makeMatrixMesh(is1rstGeneration);

            }

        }

        // finally save the terrainLayer.json
        terrainLayer.saveJsonFile(outputDirectory, "layer.json");
    }

    public double getMaxTriangleSizeForTileDepth(int depth) {
        return maxTriangleSizeForTileDepthMap.get(depth);
    }

    public double getMinTriangleSizeForTileDepth(int depth) {
        return minTriangleSizeForTileDepthMap.get(depth);
    }

    public double getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(int depth) {
        if (mapDepthMaxDiffBetweenGeoTiffSampleAndTrianglePlane.containsKey(depth)) {
            return mapDepthMaxDiffBetweenGeoTiffSampleAndTrianglePlane.get(depth);
        } else {
            double maxDiff = TileWgs84Utils.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(depth);
            mapDepthMaxDiffBetweenGeoTiffSampleAndTrianglePlane.put(depth, maxDiff);
            return mapDepthMaxDiffBetweenGeoTiffSampleAndTrianglePlane.get(depth);
        }

    }


    public String getTilePath(TileIndices tileIndices) {
        String tileTempDirectory = this.tileTempDirectory;
        String neighborFilePath = TileWgs84Utils.getTileFilePath(tileIndices.getX(), tileIndices.getY(), tileIndices.getL());
        return tileTempDirectory + File.separator + neighborFilePath;
    }

    public String getQuantizedMeshTileFolderPath(TileIndices tileIndices) {
        String outputDirectory = this.outputDirectory;
        String neighborFolderPath = tileIndices.getL() + File.separator + tileIndices.getX();
        return outputDirectory + File.separator + neighborFolderPath;
    }

    public String getQuantizedMeshTilePath(TileIndices tileIndices) {
        String outputDirectory = this.outputDirectory;
        String neighborFilePath = tileIndices.getL() + File.separator + tileIndices.getX() + File.separator + tileIndices.getY();
        return outputDirectory + File.separator + neighborFilePath + ".terrain";
    }

    public TileWgs84 loadOrCreateTileWgs84(TileIndices tileIndices) throws IOException, TransformException {
        // this function loads or creates a TileWgs84
        // check if exist LDTileFile

        if (!tileIndices.isValid()) {
            return null;
        }

        String neighborFullPath = getTilePath(tileIndices);
        TileWgs84 neighborTile = new TileWgs84(null, this);
        if (!FileUtils.isFileExists(neighborFullPath)) {
            log.debug("Creating tile: CREATE - * - CREATE : " + tileIndices.getX() + ", " + tileIndices.getY() + ", " + tileIndices.getL());
            neighborTile.setTileIndices(tileIndices);
            String imageryType = this.imageryType;
            neighborTile.setGeographicExtension(TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, imageryType, originIsLeftUp));
            neighborTile.createInitialMesh();
            if (neighborTile.getMesh() == null) {
                log.error("Error: neighborTile.mesh == null");
            }

            neighborTile.saveFile(neighborTile.getMesh(), neighborFullPath);
        } else {
            // load the Tile
            neighborTile.setTileIndices(tileIndices);
            neighborTile.setGeographicExtension(TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, imageryType, originIsLeftUp));
            neighborTile.loadFile(neighborFullPath);
        }


        return neighborTile;
    }

    public boolean existTileFile(TileIndices tileIndices) {
        String neighborFullPath = getTilePath(tileIndices);
        return FileUtils.isFileExists(neighborFullPath);
    }

    public TileWgs84 loadTileWgs84(TileIndices tileIndices) throws IOException, TransformException {
        // this function loads or creates a TileWgs84
        // check if exist LDTileFile

        String neighborFullPath = getTilePath(tileIndices);
        TileWgs84 neighborTile = null;
        if (!FileUtils.isFileExists(neighborFullPath)) {
            //log.error("Error: neighborFullPath is not exist: " + neighborFullPath);
            return null;
        } else {
            log.debug("Loading tile: LOAD - * - LOAD : " + tileIndices.getX() + ", " + tileIndices.getY() + ", " + tileIndices.getL());
            // load the Tile
            neighborTile = new TileWgs84(null, this);
            neighborTile.setTileIndices(tileIndices);
            neighborTile.setGeographicExtension(TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, imageryType, originIsLeftUp));
            neighborTile.loadFile(neighborFullPath);
        }

        return neighborTile;
    }


    public void processResizeGeotiffs(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException, TransformException {
        // 1rst check geoTiff files count
        List<String> geoTiffFilePaths = new ArrayList<String>();
        FileUtils.getFilePathsByExtension(terrainElevationDataFolderPath, "tif", geoTiffFilePaths, true);

        int geotiffCount = geoTiffFilePaths.size();

        this.setGeoTiffFilesCount(geotiffCount);

        log.info("resizing geoTiffs: geoTiffsCount : " + geotiffCount);

        if (geotiffCount == 1) {
            this.uniqueGeoTiffFilePath = geoTiffFilePaths.get(0);
        } else {
            // 2nd resize the geotiffs
            resizeGeotiffSet(terrainElevationDataFolderPath, currentFolderPath);
        }

    }

    public void resizeGeotiffSet(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException, TransformException {
        // load all geoTiffFiles
        List<String> geoTiffFileNames = new ArrayList<String>();
        com.gaia3d.reader.FileUtils.getFileNames(terrainElevationDataFolderPath, ".tif", geoTiffFileNames);

        if (currentFolderPath == null) {
            currentFolderPath = "";
        }

        GaiaGeoTiffManager gaiaGeoTiffManager = new GaiaGeoTiffManager();

        // now load all geotiff and make geotiff geoExtension data
        for (String geoTiffFileName : geoTiffFileNames) {
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
                double desiredPixelSizeXinMeters = this.mapDepthDesiredPixelSizeXinMeters.get(depth);
                double desiredPixelSizeYinMeters = desiredPixelSizeXinMeters;

                //****************************************************************************************
                if (desiredPixelSizeXinMeters < pixelSizeMeters.x) {
                    // In this case just assign the originalGeoTiffFolderPath
                    this.mapDepthGeoTiffFolderPath.put(depth, this.originalGeoTiffFolderPath);
                    continue;
                }

                String depthStr = String.valueOf(depth);
                String resizedGeoTiffFolderPath = this.tempResizedGeoTiffFolderPath + File.separator + depthStr + File.separator + currentFolderPath;
                String resizedGeoTiffFilePath = resizedGeoTiffFolderPath + File.separator + geoTiffFileName;

                // check if exist the file
                if (FileUtils.isFileExists(resizedGeoTiffFilePath)) {
                    // in this case, just assign the resizedGeoTiffFolderPath
                    String resizedGeoTiffSETFolderPath_forThisDepth = this.tempResizedGeoTiffFolderPath + File.separator + depthStr;
                    this.mapDepthGeoTiffFolderPath.put(depth, resizedGeoTiffSETFolderPath_forThisDepth);
                    continue;
                }

                // in this case, resize the geotiff
                GridCoverage2D resizedGridCoverage2D = gaiaGeoTiffManager.getResizedCoverage2D(originalGridCoverage2D, desiredPixelSizeXinMeters, desiredPixelSizeYinMeters);

                FileUtils.createAllFoldersIfNoExist(resizedGeoTiffFolderPath);
                gaiaGeoTiffManager.saveGridCoverage2D(resizedGridCoverage2D, resizedGeoTiffFilePath);

                String resizedGeoTiffSETFolderPath_forThisDepth = this.tempResizedGeoTiffFolderPath + File.separator + depthStr;
                this.mapDepthGeoTiffFolderPath.put(depth, resizedGeoTiffSETFolderPath_forThisDepth);
            }


        }

        // now check if exist folders inside the terrainElevationDataFolderPath
        List<String> folderNames = new ArrayList<>();
        com.gaia3d.reader.FileUtils.getFolderNames(terrainElevationDataFolderPath, folderNames);
        String auxFolderPath = currentFolderPath;
        for (String folderName : folderNames) {
            auxFolderPath = currentFolderPath + File.separator + folderName;
            String folderPath = terrainElevationDataFolderPath + File.separator + folderName;
            resizeGeotiffSet(folderPath, auxFolderPath);
        }

        System.gc();
    }
}
