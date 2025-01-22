package com.gaia3d.terrain.tile;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.structure.GeographicExtension;
import com.gaia3d.terrain.structure.TerrainTriangle;
import com.gaia3d.terrain.tile.geotiff.GaiaGeoTiffManager;
import com.gaia3d.terrain.tile.geotiff.RasterStandardizer;
import com.gaia3d.terrain.util.GaiaGeoTiffUtils;
import com.gaia3d.terrain.util.TileWgs84Utils;
import com.gaia3d.util.DecimalUtils;
import com.gaia3d.util.FileUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffException;
import org.geotools.referencing.CRS;
import org.joml.Vector2d;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@Slf4j
public class TileWgs84Manager {
    private final static GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final int rasterTileSize = 256;
    private final String imaginaryType = "CRS84"; // "CRS84" or "WEB_MERCATOR"
    // For each depth level, use a different folder
    private final Map<Integer, String> depthGeoTiffFolderPathMap = new HashMap<>();
    private final Map<Integer, Double> depthDesiredPixelSizeXinMetersMap = new HashMap<>();
    private final Map<Integer, Double> depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap = new HashMap<>();
    private final List<TileWgs84> tileWgs84List = new ArrayList<>();
    // tileRasterSize : when triangles refinement, we use a DEM raster of this size
    private TerrainElevationDataManager terrainElevationDataManager = null;
    private int geoTiffFilesCount = 0;

    private String uniqueGeoTiffFilePath = null; // use this if there is only one geoTiff file
    private double vertexCoincidentError = 1e-11; // 1e-12 is good
    private int triangleRefinementMaxIterations = 5;
    private TerrainLayer terrainLayer = null;
    private boolean originIsLeftUp = false; // false = origin is left-down (Cesium Tile System)

    private List<Double> maxTriangleSizeForTileDepthList = new ArrayList<>();
    private List<Double> minTriangleSizeForTileDepthList = new ArrayList<>();

    //private boolean calculateNormals = true;
    private List<TerrainElevationData> terrainElevationDataList = new ArrayList<>();
    private List<TerrainTriangle> triangleList = new ArrayList<>();
    private Vector2d pixelSizeDegrees = new Vector2d();
    private Map<String, String> mapNoUsableGeotiffPaths = new HashMap<>();

    private GaiaGeoTiffManager gaiaGeoTiffManager = new GaiaGeoTiffManager();

    // constructor
    public TileWgs84Manager() {
        double intensity = globalOptions.getIntensity();

        // Init default values
        // init the maxTriangleSizeForTileDepthMap
        for (int i = 0; i < 28; i++) {
            double tileSizeMeters = TileWgs84Utils.getTileSizeInMetersByDepth(i);
            double maxSize = tileSizeMeters / 2.5;
            if (i < 11) {
                maxSize *= 0.2;
            }
            //maxTriangleSizeForTileDepthMap.put(i, maxSize);
            maxTriangleSizeForTileDepthList.add(maxSize);

            double minSize = tileSizeMeters * 0.1 / (intensity);

            if (i > 17) {
                minSize *= 0.75;
            } else if (i > 15) {
                minSize *= 1.0;
            } else if (i > 14) {
                minSize *= 1.125;
            } else if (i > 13) {
                minSize *= 1.25;
            } else if (i > 12) {
                minSize *= 1.25;
            } else if (i > 10) {
                minSize *= 1.25;
            } else {
                minSize *= 1.0;
            }
            minTriangleSizeForTileDepthList.add(minSize);
        }

        // init the map_depth_desiredPixelSizeXinMeters
        for (int depth = 0; depth <= 28; depth++) {
            double tileSizeMeters = TileWgs84Utils.getTileSizeInMetersByDepth(depth);
            double desiredPixelSizeXinMeters = tileSizeMeters / 256.0;
            this.depthDesiredPixelSizeXinMetersMap.put(depth, desiredPixelSizeXinMeters);
        }
    }

    public void deleteObjects() {
        if (this.terrainElevationDataManager != null) {
            this.terrainElevationDataManager.deleteObjects();
            this.terrainElevationDataManager = null;
        }

        if (this.terrainLayer != null) {
            //this.terrainLayer.deleteObjects();
            this.terrainLayer = null;
        }

        if (this.terrainElevationDataList != null) {
            this.terrainElevationDataList.clear();
        }

        if (this.triangleList != null) {
            this.triangleList.clear();
        }

        this.depthGeoTiffFolderPathMap.clear();
        this.depthDesiredPixelSizeXinMetersMap.clear();
        this.depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap.clear();
        this.maxTriangleSizeForTileDepthList.clear();
        this.minTriangleSizeForTileDepthList.clear();
        this.mapNoUsableGeotiffPaths.clear();
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

        if (globalOptions.isCalculateNormals()) {
            terrainLayer.addExtension("octvertexnormals");
        }

        log.info("----------------------------------------");
        int minTileDepth = globalOptions.getMinimumTileDepth();
        int maxTileDepth = globalOptions.getMaximumTileDepth();
        for (int depth = minTileDepth; depth <= maxTileDepth; depth += 1) {
            long startTime = System.currentTimeMillis();
            Date startDate = new Date(startTime);

            TileRange tilesRange = new TileRange();

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
                    this.terrainElevationDataManager.setTileWgs84Manager(this);
                    this.terrainElevationDataManager.setUniqueGeoTiffFilePath(this.uniqueGeoTiffFilePath);
                    this.terrainElevationDataManager.MakeUniqueTerrainElevationData();
                }
                this.terrainElevationDataManager.deleteObjects(); // here deletes tileRasters
            } else {
                this.terrainElevationDataManager.deleteObjects();
                this.terrainElevationDataManager = new TerrainElevationDataManager(); // new
                this.terrainElevationDataManager.setTileWgs84Manager(this);
                this.terrainElevationDataManager.setTerrainElevationDataFolderPath(this.depthGeoTiffFolderPathMap.get(depth));
                this.terrainElevationDataManager.makeTerrainQuadTree();
            }

            int mosaicSize = globalOptions.getMosaicSize();
            List<TileRange> subDividedTilesRanges = TileWgs84Utils.subDivideTileRange(tilesRange, mosaicSize, mosaicSize, null);

            log.info("[Tile][{}/{}] Start generating tile meshes - Divided Tiles Size: {}", depth, maxTileDepth, subDividedTilesRanges.size());
            AtomicInteger counter = new AtomicInteger(0);

            int total = subDividedTilesRanges.size();
            for (TileRange subDividedTilesRange : subDividedTilesRanges) {
                int progress = counter.incrementAndGet();
                log.info("[Tile][{}/{}][{}/{}] generate wgs84 raster all tiles...", depth, maxTileDepth, progress, total);
                TileRange expandedTilesRange = subDividedTilesRange.expand1();
                this.terrainElevationDataManager.makeAllTileWgs84Raster(expandedTilesRange, this);
                if (this.geoTiffFilesCount > 1) {
                    this.terrainElevationDataManager.deleteGeoTiffManager();
                    this.terrainElevationDataManager.deleteCoverage();
                }

                log.info("[Tile][{}/{}][{}/{}] process tiling...", depth, maxTileDepth, progress, total);
                TileMatrix tileMatrix = new TileMatrix(subDividedTilesRange, this);

                boolean isFirstGeneration = (depth == minTileDepth);
                tileMatrix.makeMatrixMesh(isFirstGeneration);
                tileMatrix.deleteObjects();

                if (this.geoTiffFilesCount > 1) {
                    this.terrainElevationDataManager.deleteGeoTiffManager();
                    this.terrainElevationDataManager.deleteCoverage();
                }
                this.terrainElevationDataManager.deleteTileRaster();
            }

            long endTime = System.currentTimeMillis();
            log.info("[Tile][{}/{}] - End making tile meshes : Duration: {}", depth, maxTileDepth, DecimalUtils.millisecondToDisplayTime(endTime - startTime));

            String javaHeapSize = System.getProperty("java.vm.name") + " " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "MB";
            // jvm heap size
            String maxMem = DecimalUtils.byteCountToDisplaySize(Runtime.getRuntime().maxMemory());
            // jvm total memory
            String totalMem = DecimalUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory());
            // jvm free memory
            String freeMem = DecimalUtils.byteCountToDisplaySize(Runtime.getRuntime().freeMemory());
            // jvm used memory
            String usedMem = DecimalUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            log.info("[Tile][{}/{}] Java Heap Size: {} - MaxMem: {}MB / TotalMem: {}MB / FreeMem: {}MB / UsedMem: {}MB ({}%)", depth, maxTileDepth, javaHeapSize, maxMem, totalMem, freeMem, usedMem);
            log.info("----------------------------------------");
        }
        terrainLayer.saveJsonFile(globalOptions.getOutputPath(), "layer.json");
    }

    public String timeFormat(long time) {


        long ms = time % 1000;
        long s = (time / 1000) % 60;
        long m = (time / (1000 * 60)) % 60;
        long h = (time / (1000 * 60 * 60)) % 24;
        return String.format("%02d:%02d:%02d.%03d", h, m, s, ms);
    }

    public double getMaxTriangleSizeForTileDepth(int depth) {
        //return maxTriangleSizeForTileDepthMap.get(depth);
        return maxTriangleSizeForTileDepthList.get(depth);
    }

    public double getMinTriangleSizeForTileDepth(int depth) {
        //return minTriangleSizeForTileDepthMap.get(depth);
        return minTriangleSizeForTileDepthList.get(depth);
    }

    public double getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(int depth) {
        if (depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap.containsKey(depth)) {
            return depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap.get(depth);
        } else {
            double maxDiff = TileWgs84Utils.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(depth);
            depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap.put(depth, maxDiff);
            return depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap.get(depth);
        }

    }


    public String getTilePath(TileIndices tileIndices) {
        String tileTempDirectory = globalOptions.getTileTempPath();
        String neighborFilePath = TileWgs84Utils.getTileFilePath(tileIndices.getX(), tileIndices.getY(), tileIndices.getL());
        return tileTempDirectory + File.separator + neighborFilePath;
    }

    public String getQuantizedMeshTileFolderPath(TileIndices tileIndices) {
        String outputDirectory = globalOptions.getOutputPath();
        String neighborFolderPath = tileIndices.getL() + File.separator + tileIndices.getX();
        return outputDirectory + File.separator + neighborFolderPath;
    }

    public String getQuantizedMeshTilePath(TileIndices tileIndices) {
        String outputDirectory = globalOptions.getOutputPath();
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
            neighborTile.setGeographicExtension(TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, this.imaginaryType, originIsLeftUp));
            neighborTile.createInitialMesh();
            if (neighborTile.getMesh() == null) {
                log.error("Error: neighborTile.mesh == null");
            }

            neighborTile.saveFile(neighborTile.getMesh(), neighborFullPath);
        } else {
            // load the Tile
            neighborTile.setTileIndices(tileIndices);
            neighborTile.setGeographicExtension(TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, imaginaryType, originIsLeftUp));
            neighborTile.loadFile(neighborFullPath);
        }


        return neighborTile;
    }

    public boolean isNotExistsTileFile(TileIndices tileIndices) {
        String neighborFullPath = getTilePath(tileIndices);
        return !FileUtils.isFileExists(neighborFullPath);
    }

    public TileWgs84 loadTileWgs84(TileIndices tileIndices) throws IOException {
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
            neighborTile.setGeographicExtension(TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, imaginaryType, originIsLeftUp));
            neighborTile.loadFile(neighborFullPath);
        }

        return neighborTile;
    }

    private void addNoUsableGeotiffPath(String noUsableGeotiffPath) {
        this.mapNoUsableGeotiffPaths.put(noUsableGeotiffPath, noUsableGeotiffPath);
    }

    public void processStandardizeRasters() {
        String inputPath = globalOptions.getInputPath();
        File inputFolder = new File(inputPath);

        List<String> rasterFileNames = new ArrayList<>();
        if (inputFolder.exists() && inputFolder.isDirectory()) {
            FileUtils.getFilePathsByExtension(inputPath, ".tif", rasterFileNames, true);
        } else if (inputFolder.exists() && inputFolder.isFile()) {
            if (inputPath.endsWith(".tif")) {
                rasterFileNames.add(inputPath);
            }
        } else {
            log.error("Input path is not exist or not a directory: {}", inputPath);
            throw new RuntimeException("Error: Input path is not exist or not a directory: " + inputPath);
        }

        if (rasterFileNames.isEmpty()) {
            log.error("No GeoTiff files found in the input path: {}", inputPath);
            throw new RuntimeException("Error: No GeoTiff files found in the input path: " + inputPath);
        }

        standardizeRasters(rasterFileNames);
    }

    public void standardizeRasters(List<String> geoTiffFileNames) {
        String tempPath = globalOptions.getStandardizeTempPath();
        File tempFolder = new File(tempPath);
        if (!tempFolder.exists() && tempFolder.mkdirs()) {
            log.debug("Created standardization folder: {}", tempFolder.getAbsolutePath());
        }
        globalOptions.setInputPath(tempFolder.getAbsolutePath());

        geoTiffFileNames.forEach(geoTiffFileName -> {
            GridCoverage2D originalGridCoverage2D = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(geoTiffFileName);
            RasterStandardizer rasterStandardizer = new RasterStandardizer();
            rasterStandardizer.standardize(originalGridCoverage2D, tempFolder);
        });
    }

    public void processResizeRasters(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException {
        File terrainElevationDataFolder = new File(terrainElevationDataFolderPath);
        if (!terrainElevationDataFolder.exists()) {
            log.error("terrainElevationDataFolder is not exist: " + terrainElevationDataFolderPath);
            throw new RuntimeException("Error: terrainElevationDataFolder is not exist: " + terrainElevationDataFolderPath);
        } else if (!terrainElevationDataFolder.isDirectory()) {
            log.error("terrainElevationDataFolder is not a directory: " + terrainElevationDataFolderPath);
            throw new RuntimeException("Error: terrainElevationDataFolder is not a directory: " + terrainElevationDataFolderPath);
        }

        // First check geoTiff files count
        List<String> geoTiffFilePaths = new ArrayList<>();
        FileUtils.getFilePathsByExtension(terrainElevationDataFolderPath, "tif", geoTiffFilePaths, true);

        int geotiffCount = geoTiffFilePaths.size();
        this.setGeoTiffFilesCount(geotiffCount);

        log.info("[Pre][Resize GeoTiff] resizing geoTiffs Count : {} ", geotiffCount);
        if (geotiffCount == 1) {
            this.uniqueGeoTiffFilePath = geoTiffFilePaths.get(0);
        } else {
            // 2nd resize the geotiffs
            resizeRasters(terrainElevationDataFolderPath, currentFolderPath);
        }
    }

    public void resizeRasters(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException {
        // load all geoTiffFiles
        List<String> geoTiffFileNames = new ArrayList<>();
        FileUtils.getFileNames(terrainElevationDataFolderPath, ".tif", geoTiffFileNames);
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:3857");

        if (currentFolderPath == null) {
            currentFolderPath = "";
        }

        // now load all geotiff and make geotiff geoExtension data
        int geoTiffFilesSize = geoTiffFileNames.size();
        int geoTiffFilesCount = 0;

        // TODO : Multi-threading
        for (String geoTiffFileName : geoTiffFileNames) {
            log.info("[Pre][Resize GeoTiff][{}/{}] resizing geoTiff : {} ", ++geoTiffFilesCount, geoTiffFilesSize, geoTiffFileName);
            String geoTiffFilePath = terrainElevationDataFolderPath + File.separator + geoTiffFileName;

            // check if the geotiffFileName is no usable
            if (this.mapNoUsableGeotiffPaths.containsKey(geoTiffFilePath)) {
                continue;
            }

            GridCoverage2D originalGridCoverage2D = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(geoTiffFilePath);

            CoordinateReferenceSystem crsTarget = originalGridCoverage2D.getCoordinateReferenceSystem2D();
            if (!(crsTarget instanceof ProjectedCRS || crsTarget instanceof GeographicCRS)) {
                log.error("The supplied grid coverage uses an unsupported crs! You are allowed to use only projected and geographic coordinate reference systems");
                throw new GeoTiffException(null, "The supplied grid coverage uses an unsupported crs! You are allowed to use only projected and geographic coordinate reference systems", null);
            }

            Vector2d pixelSizeMeters = GaiaGeoTiffUtils.getPixelSizeMeters(originalGridCoverage2D);

            int minTileDepth = globalOptions.getMinimumTileDepth();
            int maxTileDepth = globalOptions.getMaximumTileDepth();
            for (int depth = minTileDepth; depth <= maxTileDepth; depth += 1) {
                double desiredPixelSizeXinMeters = this.depthDesiredPixelSizeXinMetersMap.get(depth);
                double desiredPixelSizeYinMeters = desiredPixelSizeXinMeters;

                //****************************************************************************************
                if (desiredPixelSizeXinMeters < pixelSizeMeters.x) {
                    // In this case just assign the originalGeoTiffFolderPath
                    this.depthGeoTiffFolderPathMap.put(depth, globalOptions.getInputPath());
                    continue;
                }

                String depthStr = String.valueOf(depth);
                String resizedGeoTiffFolderPath = globalOptions.getResizedTiffTempPath() + File.separator + depthStr + File.separator + currentFolderPath;
                String resizedGeoTiffFilePath = resizedGeoTiffFolderPath + File.separator + geoTiffFileName;

                // check if exist the file
                if (FileUtils.isFileExists(resizedGeoTiffFilePath)) {
                    // in this case, just assign the resizedGeoTiffFolderPath
                    String resizedGeoTiffSETFolderPath_forThisDepth = globalOptions.getResizedTiffTempPath() + File.separator + depthStr;
                    this.depthGeoTiffFolderPathMap.put(depth, resizedGeoTiffSETFolderPath_forThisDepth);
                    continue;
                }

                // in this case, resize the geotiff
                GridCoverage2D resizedGridCoverage2D = gaiaGeoTiffManager.getResizedCoverage2D(originalGridCoverage2D, desiredPixelSizeXinMeters, desiredPixelSizeYinMeters);
                FileUtils.createAllFoldersIfNoExist(resizedGeoTiffFolderPath);
                gaiaGeoTiffManager.saveGridCoverage2D(resizedGridCoverage2D, resizedGeoTiffFilePath);

                resizedGridCoverage2D.dispose(true);

                String resizedGeoTiffSETFolderPath_forThisDepth = globalOptions.getResizedTiffTempPath() + File.separator + depthStr;
                this.depthGeoTiffFolderPathMap.put(depth, resizedGeoTiffSETFolderPath_forThisDepth);
            }
        }

        // now check if exist folders inside the terrainElevationDataFolderPath
        List<String> folderNames = new ArrayList<>();
        FileUtils.getFolderNames(terrainElevationDataFolderPath, folderNames);
        for (String folderName : folderNames) {
            String auxFolderPath = currentFolderPath + File.separator + folderName;
            String folderPath = terrainElevationDataFolderPath + File.separator + folderName;
            resizeRasters(folderPath, auxFolderPath);
        }

        System.gc();
    }
}
