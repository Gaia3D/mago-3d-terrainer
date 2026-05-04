package com.gaia3d.terrain.tile;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.io.LittleEndianDataInputStream;
import com.gaia3d.quantized.mesh.QuantizedMesh;
import com.gaia3d.quantized.mesh.QuantizedMeshManager;
import com.gaia3d.terrain.structure.GeographicExtension;
import com.gaia3d.terrain.structure.TerrainTriangle;
import com.gaia3d.terrain.tile.custom.AvailableTileSet;
import com.gaia3d.terrain.tile.geotiff.GaiaGeoTiffManager;
import com.gaia3d.terrain.tile.geotiff.RasterStandardizer;
import com.gaia3d.terrain.util.GaiaGeoTiffUtils;
import com.gaia3d.terrain.util.TerrainMeshUtils;
import com.gaia3d.terrain.util.TileWgs84Utils;
import com.gaia3d.util.DecimalUtils;
import com.gaia3d.util.FileUtils;
import com.gaia3d.util.StringUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.crs.ProjectedCRS;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffException;
import org.geotools.referencing.CRS;
import org.joml.Vector2d;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
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

    private double vertexCoincidentError = 1e-11; // 1e-12 is good
    // Complex lunar terrain requires more refinement passes than Earth geodetic tiles
    private int triangleRefinementMaxIterations = 20;
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

    // the list of standardized geotiff files. This the real input for the terrain elevation data
    private List<File> standardizedGeoTiffFiles = new ArrayList<>();

    // Available tileset
    private AvailableTileSet availableTileSet = new AvailableTileSet();

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
            this.terrainLayer = null;
        }

        if (this.terrainElevationDataList != null) {
            this.terrainElevationDataList.clear();
        }

        if (this.triangleList != null) {
            this.triangleList.clear();
        }

        this.tileWgs84List.clear();
        this.standardizedGeoTiffFiles.clear();
        this.gaiaGeoTiffManager.clear();

        this.depthGeoTiffFolderPathMap.clear();
        this.depthDesiredPixelSizeXinMetersMap.clear();
        this.depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap.clear();
        this.maxTriangleSizeForTileDepthList.clear();
        this.minTriangleSizeForTileDepthList.clear();
        this.mapNoUsableGeotiffPaths.clear();
        this.availableTileSet.getMapDepthAvailableTileRanges().clear();
    }

    public void makeTempFilesFromQuantizedMeshes(int depth) {
        // make temp folder
        String tempPath = globalOptions.getTileTempPath();
        String depthStr = "L" + depth;
        String depthTempPath = tempPath + File.separator + depthStr;
        File depthTempFolder = new File(depthTempPath);
        if (!depthTempFolder.exists()) {
            if (depthTempFolder.mkdirs()) {
                log.debug("Created temp folder: {}", depthTempFolder.getAbsolutePath());
            }
        }

        // find quantized mesh files
        String quantizedMeshPath = globalOptions.getOutputPath() + File.separator + depth;
        File quantizedMeshFolder = new File(quantizedMeshPath);
        if (!quantizedMeshFolder.exists()) {
            log.error("Quantized mesh folder does not exist: {}", quantizedMeshPath);
            return;
        }

        // start to make temp files
        int L = depth;
        int X = 0;
        int Y = 0;

        TileIndices tileIndices = new TileIndices();
        QuantizedMeshManager quantizedMeshManager = new QuantizedMeshManager();
        List<String> quantizedMeshFolderNames = new ArrayList<>();
        FileUtils.getFolderNames(quantizedMeshPath, quantizedMeshFolderNames);

        int quantizedFoldersCount = quantizedMeshFolderNames.size();
        for (int i = 0; i < quantizedFoldersCount; i++) {
            String quantizedMeshFolderName = quantizedMeshFolderNames.get(i);
            X = Integer.parseInt(quantizedMeshFolderName);

            String quantizedMeshFolderPath = quantizedMeshPath + File.separator + quantizedMeshFolderName;
            File quantizedMeshSubFolder = new File(quantizedMeshFolderPath);
            if (!quantizedMeshSubFolder.exists()) {
                log.error("Quantized mesh subfolder does not exist: {}", quantizedMeshSubFolder.getAbsolutePath());
                continue;
            }

            String tempXFolderName = "X" + quantizedMeshFolderName;

            List<String> quantizedMeshFileNames = new ArrayList<>();
            FileUtils.getFileNames(quantizedMeshFolderPath, ".terrain", quantizedMeshFileNames);
            int quantizedFilesCount = quantizedMeshFileNames.size();
            for (int j = 0; j < quantizedFilesCount; j++) {
                String quantizedMeshFileName = quantizedMeshFileNames.get(j);
                Y = Integer.parseInt(quantizedMeshFileName.substring(0, quantizedMeshFileName.indexOf(".")));

                String quantizedMeshFilePath = quantizedMeshFolderPath + File.separator + quantizedMeshFileName;

                // load the quantized mesh file
                File quantizedMeshFile = new File(quantizedMeshFilePath);
                if (!quantizedMeshFile.exists()) {
                    log.error("Quantized mesh file does not exist: {}", quantizedMeshFilePath);
                    continue;
                }

                String tempFileName = "L" + depth + "_" + tempXFolderName + "_Y" + Y + ".til";
                String tempFilePath = depthTempPath + File.separator + tempXFolderName + File.separator + tempFileName;

                // check if exist temp file
                File tempFile = new File(tempFilePath);
                if (tempFile.exists()) {
                    log.debug("Temp file already exists: {}", tempFilePath);
                    continue;
                }

                try {
                    LittleEndianDataInputStream inputStream = new LittleEndianDataInputStream(new BufferedInputStream(new FileInputStream(quantizedMeshFilePath)));
                    QuantizedMesh quantizedMesh = new QuantizedMesh();
                    quantizedMesh.loadDataInputStream(inputStream);

                    tileIndices.set(X, Y, L);
                    TileWgs84 tileWgs84 = quantizedMeshManager.getTileWgs84FromQuantizedMesh(quantizedMesh, tileIndices, this);
                    tileWgs84.saveFile(tileWgs84.getMesh(), tempFilePath);
                } catch (Exception e) {
                    log.error("Error loading quantized mesh file: {}", quantizedMeshFilePath, e);
                }
            }
        }
    }

    public void makeTempFilesFromQuantizedMeshes(int depth, TileRange tileRange) {
        // make temp folder
        String tempPath = globalOptions.getTileTempPath();
        String depthStr = "L" + depth;
        String depthTempPath = tempPath + File.separator + depthStr;
        File depthTempFolder = new File(depthTempPath);
        if (!depthTempFolder.exists()) {
            if (depthTempFolder.mkdirs()) {
                log.debug("Created temp folder: {}", depthTempFolder.getAbsolutePath());
            }
        }

        // find quantized mesh files
        String quantizedMeshPath = globalOptions.getOutputPath() + File.separator + depth;
        File quantizedMeshFolder = new File(quantizedMeshPath);
        if (!quantizedMeshFolder.exists()) {
            log.error("Quantized mesh folder does not exist: {}", quantizedMeshPath);
            return;
        }

        // start to make temp files
        int L = depth;
        int X = 0;
        int Y = 0;

        TileIndices tileIndices = new TileIndices();
        QuantizedMeshManager quantizedMeshManager = new QuantizedMeshManager();
        List<String> quantizedMeshFolderNames = new ArrayList<>();
        FileUtils.getFolderNames(quantizedMeshPath, quantizedMeshFolderNames);

        int minTileX = tileRange.getMinTileX();
        int maxTileX = tileRange.getMaxTileX();
        int minTileY = tileRange.getMinTileY();
        int maxTileY = tileRange.getMaxTileY();

        for(int tileX = minTileX; tileX <= maxTileX; tileX++) {
            for(int tileY = minTileY; tileY <= maxTileY; tileY++) {
                String qMeshFolderNameY = String.valueOf(tileY);
                String qMeshFileNameXYZ = String.valueOf(tileX) + ".terrain";
                String qMeshFullPath = quantizedMeshPath + File.separator + qMeshFolderNameY + File.separator + qMeshFileNameXYZ;

                // check if exists qMeshFullPath.
                File qMeshFile = new File(qMeshFullPath);
                if (!qMeshFile.exists()) {
                    // in this case make it.
                    log.info("Quantized mesh file does not exist: {}", qMeshFullPath);
                    // TODO:
                }

                String tempXFolderName = "X" + tileX;
                String tempFileName = "L" + depth + "_" + tempXFolderName + "_Y" + Y + ".til";

                String tempFilePath = depthTempPath + File.separator + tempXFolderName + File.separator + tempFileName;

                // check if exist temp file
                File tempFile = new File(tempFilePath);
                if (tempFile.exists()) {
                    log.debug("Temp file already exists: {}", tempFilePath);
                    continue;
                }

                try {
                    LittleEndianDataInputStream inputStream = new LittleEndianDataInputStream(new BufferedInputStream(new FileInputStream(qMeshFullPath)));
                    QuantizedMesh quantizedMesh = new QuantizedMesh();
                    quantizedMesh.loadDataInputStream(inputStream);

                    tileIndices.set(X, Y, L);
                    TileWgs84 tileWgs84 = quantizedMeshManager.getTileWgs84FromQuantizedMesh(quantizedMesh, tileIndices, this);
                    tileWgs84.saveFile(tileWgs84.getMesh(), tempFilePath);

                    // save the TileWgs84 in the children temp folder
                    TerrainMeshUtils.save4ChildrenMeshes(tileWgs84.getMesh(), this, globalOptions);

                    tileWgs84.deleteObjects();
                } catch (Exception e) {
                    log.error("Error loading quantized mesh file: {}", qMeshFullPath, e);
                }

                int hola = 0;
            }
        }
    }

    private void makeChildrenTempFiles(int depth) {
        int childrenDepth = depth + 1;

        // make temp folder
        String tempPath = globalOptions.getTileTempPath();
        String depthStr = "L" + depth;
        String depthTempPath = tempPath + File.separator + depthStr;
        File depthTempFolder = new File(depthTempPath);
        if (!depthTempFolder.exists()) {
            return;
        }

        // make the children temp folder
        String childrenTempPath = tempPath + File.separator + "L" + childrenDepth;
        File childrenTempFolder = new File(childrenTempPath);
        if (!childrenTempFolder.exists()) {
            if (childrenTempFolder.mkdirs()) {
                log.debug("Created children temp folder: {}", childrenTempFolder.getAbsolutePath());
            }
        }

        // find all XFolders inside the depthTempPath folder
        List<String> xFolders = new ArrayList<>();
        FileUtils.getFolderNames(depthTempPath, xFolders);
        int xFoldersCount = xFolders.size();
        for (int i = 0; i < xFoldersCount; i++) {
            String xFolderName = xFolders.get(i);
            int X = Integer.parseInt(xFolderName.substring(1));
            String xFolderPath = depthTempPath + File.separator + xFolderName;
            File xFolder = new File(xFolderPath);
            if (!xFolder.exists()) {
                log.error("X folder does not exist: {}", xFolderPath);
                continue;
            }

            // make children XTemp folder
            String childrenXTempPath = childrenTempPath + File.separator + xFolderName;
            File childrenXTempFolder = new File(childrenXTempPath);
            if (!childrenXTempFolder.exists()) {
                if (childrenTempFolder.mkdirs()) {
                    log.debug("Created children temp folder: {}", childrenXTempFolder.getAbsolutePath());
                }
            }

            // load the TileWgs84 files
            List<String> tileWgs84FileNames = new ArrayList<>();
            FileUtils.getFileNames(xFolderPath, ".til", tileWgs84FileNames);
            int tileWgs84FilesCount = tileWgs84FileNames.size();
            for (int j = 0; j < tileWgs84FilesCount; j++) {
                String tileWgs84FileName = tileWgs84FileNames.get(j);
                List<String> splitStrings = Arrays.asList(tileWgs84FileName.split("_"));
                String YFileName = splitStrings.get(2);
                int Y = Integer.parseInt(YFileName.substring(1, YFileName.indexOf(".")));
                String tileWgs84FilePath = xFolderPath + File.separator + tileWgs84FileName;

                // load the TileWgs84 file
                File tileWgs84File = new File(tileWgs84FilePath);
                if (!tileWgs84File.exists()) {
                    log.error("TileWgs84 file does not exist: {}", tileWgs84FilePath);
                    continue;
                }

                // load the TileWgs84
                try {
                    TileIndices tileIndices = new TileIndices();
                    tileIndices.set(X, Y, depth);
                    TileWgs84 tileWgs84 = loadTileWgs84(tileIndices);
                    if (tileWgs84 == null) {
                        log.error("TileWgs84 is null: {}", tileWgs84FilePath);
                        continue;
                    }

                    // save the TileWgs84 in the children temp folder
                    TerrainMeshUtils.save4ChildrenMeshes(tileWgs84.getMesh(), this, globalOptions);
                } catch (Exception e) {
                    log.error("Error loading TileWgs84 file: {}", tileWgs84FilePath, e);
                }
            }
        }
    }

    private int determineExistentTileSetMaxDepth(String tileSetDirectory) {
        int existentTileSetMaxDepth = -1;
        List<String> folderNames = new ArrayList<>();
        FileUtils.getFolderNames(tileSetDirectory, folderNames);
        Map<Integer, Integer> depthFoldermap = new HashMap<>();
        for (String folderName : folderNames) {
            if (!StringUtils.isConvertibleToInt(folderName)) {
                continue;
            }
            int depth = Integer.parseInt(folderName);
            depthFoldermap.put(depth, depth);
        }

        int foldersCount = depthFoldermap.size();
        for (int i = 0; i < foldersCount; i++) {
            int depth = depthFoldermap.get(i);
            if (depth > existentTileSetMaxDepth) {
                existentTileSetMaxDepth = depth;
            }
        }

        return existentTileSetMaxDepth;
    }

    private boolean existTempFiles(int depth) {
        String tempPath = globalOptions.getTileTempPath();
        String depthStr = "L" + depth;
        String depthTempPath = tempPath + File.separator + depthStr;
        File depthTempFolder = new File(depthTempPath);
        return depthTempFolder.exists();
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

        if (globalOptions.isCalculateNormalsExtension()) {
            terrainLayer.addExtension("octvertexnormals");
        }
        if (globalOptions.isWaterMaskExtension()) {
            terrainLayer.addExtension("watermask");
        }
        if (globalOptions.isMetaDataExtension()) {
            terrainLayer.addExtension("metadata");
        }

        log.info("----------------------------------------");
        int minTileDepth = globalOptions.getMinimumTileDepth();
        int maxTileDepth = globalOptions.getMaximumTileDepth();

        for (int depth = minTileDepth; depth <= maxTileDepth; depth += 1) {
            long startTime = System.currentTimeMillis();

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

            // Set terrainLayer.available of tileSet JSON
            terrainLayer.getAvailable().add(tilesRange); // this is used to save the terrainLayer.json
            this.triangleRefinementMaxIterations = TileWgs84Utils.getRefinementIterations(depth);
            this.terrainElevationDataManager.deleteObjects();
            this.terrainElevationDataManager = new TerrainElevationDataManager(); // new
            this.terrainElevationDataManager.setTileWgs84Manager(this);
            this.terrainElevationDataManager.setTerrainElevationDataFolderPath(this.depthGeoTiffFolderPathMap.get(depth));
            this.terrainElevationDataManager.makeTerrainQuadTree(depth);

            int mosaicSize = globalOptions.getMosaicSize();
            List<TileRange> subDividedTilesRanges = TileWgs84Utils.subDivideTileRange(tilesRange, mosaicSize, mosaicSize, null);

            log.info("[Tile][{}/{}] Start generating tile meshes - Divided Tiles Size: {}", depth, maxTileDepth, subDividedTilesRanges.size());
            AtomicInteger counter = new AtomicInteger(0);

            int total = subDividedTilesRanges.size();
            for (TileRange subDividedTilesRange : subDividedTilesRanges) {
                int progress = counter.incrementAndGet();
                log.info("[Tile][{}/{}][{}/{}] generate raster tiles...", depth, maxTileDepth, progress, total);
                TileRange expandedTilesRange = subDividedTilesRange.expand1();
                this.terrainElevationDataManager.makeAllTileWgs84Raster(expandedTilesRange, this);

                log.info("[Tile][{}/{}][{}/{}] process quantized mesh tiling...", depth, maxTileDepth, progress, total);
                TileMatrix tileMatrix = new TileMatrix(subDividedTilesRange, this);

                boolean isFirstGeneration = (depth == minTileDepth);
                tileMatrix.makeMatrixMesh(isFirstGeneration);
                tileMatrix.deleteObjects();

                if (!GlobalOptions.getInstance().isLeaveTemp()) {
                    // now, delete tempFiles of subDividedTilesRange
                    TileRange tilesToDeleteRange = subDividedTilesRange.clone();
                    tilesToDeleteRange.translate(-1, -1);
                    deleteTempFilesByTileRange(tilesToDeleteRange);
                }
            }

            if (!GlobalOptions.getInstance().isLeaveTemp()) {
                this.deleteTempFilesByDepth(depth);
            }

            this.terrainElevationDataManager.deleteGeoTiffManager();
            this.terrainElevationDataManager.deleteTileRaster();
            this.terrainElevationDataManager.deleteCoverage();

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
            log.debug("[Tile][{}/{}] Java Heap Size: {} - MaxMem: {}MB / TotalMem: {}MB / FreeMem: {}MB / UsedMem: {}MB ({}%)", depth, maxTileDepth, javaHeapSize, maxMem, totalMem, freeMem, usedMem);
            log.info("----------------------------------------");
        }
        terrainLayer.saveJsonFile(globalOptions.getOutputPath(), "layer.json");
    }

    public void makeTileMeshesCustomModifyMode() throws IOException, TransformException, FactoryException {
        String outputDirectory = globalOptions.getOutputPath();
        int existentMaxDepth = determineExistentTileSetMaxDepth(outputDirectory);
        log.info("existent max depth: {}", existentMaxDepth);

        // in MODIFY_MODE process, 1rst load the layer.json file.************************************
        String jsonFileName = "layer.json";
        String jsonFullPath = outputDirectory + File.separator + jsonFileName;

        // create the terrainLayer and load the existent layer.json file
        terrainLayer = new TerrainLayer();
        AvailableTileSet existentAvailableTileSet = new AvailableTileSet();
        terrainLayer.loadJsonFileCustom(jsonFullPath, existentAvailableTileSet);
        // End loading existent layer.json file.-----------------------------------------------------

        GeographicExtension geographicExtension = this.terrainElevationDataManager.getRootGeographicExtension();

        double minLon = geographicExtension.getMinLongitudeDeg();
        double maxLon = geographicExtension.getMaxLongitudeDeg();
        double minLat = geographicExtension.getMinLatitudeDeg();
        double maxLat = geographicExtension.getMaxLatitudeDeg();


        double[] bounds = terrainLayer.getBounds();
        bounds[0] = minLon;
        bounds[1] = minLat;
        bounds[2] = maxLon;
        bounds[3] = maxLat;

        if (globalOptions.isCalculateNormalsExtension()) {
            terrainLayer.addExtension("octvertexnormals");
        }
        if (globalOptions.isWaterMaskExtension()) {
            terrainLayer.addExtension("watermask");
        }
        if (globalOptions.isMetaDataExtension()) {
            terrainLayer.addExtension("metadata");
        }

        log.info("----------------------------------------");
        int minTileDepth = 0;
        int maxTileDepth = globalOptions.getMaximumTileDepth();

        int availableMaxDepth = this.availableTileSet.getMaxAvailableDepth();
        if(availableMaxDepth < maxTileDepth) {
            maxTileDepth = availableMaxDepth;
        }

        // delete available tile ranges over maxTileDepth
        this.availableTileSet.deleteTileRangesOverDepth(maxTileDepth);

//        // if the maxTileDepth is less than the existent max depth, set the maxTileDepth to the existent max depth
//        minTileDepth = Math.max(minTileDepth, existentMaxDepth + 1);

        for (int depth = 0; depth <= maxTileDepth; depth += 1) {
            long startTime = System.currentTimeMillis();

//            // check if the temp folder exists
//            if (!existTempFiles(depth)) {
//                log.info("making tempFiles from quantized meshes... depth: {}", depth - 1);
//                makeTempFilesFromQuantizedMeshes(depth - 1);
//                makeChildrenTempFiles(depth - 1);
//            }

            // Set terrainLayer.available of tileSet json
            this.triangleRefinementMaxIterations = TileWgs84Utils.getRefinementIterations(depth);
            this.terrainElevationDataManager.deleteObjects();
            this.terrainElevationDataManager = new TerrainElevationDataManager(); // new
            this.terrainElevationDataManager.setTileWgs84Manager(this);
            this.terrainElevationDataManager.setTerrainElevationDataFolderPath(this.depthGeoTiffFolderPathMap.get(depth));
            this.terrainElevationDataManager.makeTerrainQuadTree(depth);
            int mosaicSize = globalOptions.getMosaicSize();

            //List<TileRange> subDividedTilesRanges = TileWgs84Utils.subDivideTileRange(tilesRange, mosaicSize, mosaicSize, null);
            List<TileRange> availableTileRangesAtDepth = this.availableTileSet.getAvailableTileRangesAtDepth(depth);
            List<TileRange> subDividedTilesRanges = new ArrayList<>();
            // for each available tile range, subdivide it into mosaicSize x mosaicSize tiles, and put them into subDividedTilesRanges
            for (TileRange availableTileRange : availableTileRangesAtDepth) {
                TileWgs84Utils.subDivideTileRange(availableTileRange, mosaicSize, mosaicSize, subDividedTilesRanges);
            }

            log.info("[Tile][{}/{}] Start generating tile meshes - Divided Tiles Size: {}", depth, maxTileDepth, subDividedTilesRanges.size());
            AtomicInteger counter = new AtomicInteger(0);

            int total = subDividedTilesRanges.size();
            for (TileRange subDividedTilesRange : subDividedTilesRanges) {
                int progress = counter.incrementAndGet();
                log.info("[Tile][{}/{}][{}/{}] generate wgs84 raster all tiles...", depth, maxTileDepth, progress, total);
                TileRange expandedTilesRange = subDividedTilesRange.expand1();

                // check if the temp folder exists
                // For each expandedTilesRange make temp files if no exists.***************************
                log.info("making tempFiles from quantized meshes... depth: {}", depth - 1);
                makeTempFilesFromQuantizedMeshes(depth, expandedTilesRange); // here makes tempFiles for children too.
                // End making tempFiles.---------------------------------------------------------------

                this.terrainElevationDataManager.makeAllTileWgs84Raster(expandedTilesRange, this);

                log.info("[Tile][{}/{}][{}/{}] process tiling...", depth, maxTileDepth, progress, total);
                TileMatrix tileMatrix = new TileMatrix(subDividedTilesRange, this);

                boolean isFirstGeneration = (depth == 0);
                tileMatrix.makeMatrixMeshModifyMode(isFirstGeneration);
                tileMatrix.deleteObjects();
            }

            if(!GlobalOptions.getInstance().isLeaveTemp()) {
                this.deleteTempFilesByDepth(depth);
            }

            this.terrainElevationDataManager.deleteGeoTiffManager();
            this.terrainElevationDataManager.deleteTileRaster();
            this.terrainElevationDataManager.deleteCoverage();

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
            log.debug("[Tile][{}/{}] Java Heap Size: {} - MaxMem: {}MB / TotalMem: {}MB / FreeMem: {}MB / UsedMem: {}MB ({}%)", depth, maxTileDepth, javaHeapSize, maxMem, totalMem, freeMem, usedMem);
            log.info("----------------------------------------");
        }
        terrainLayer.saveJsonFileCustom(globalOptions.getOutputPath(), "layer.json", this.availableTileSet);
    }

    public void makeTileMeshesContinueCustom() throws IOException, TransformException, FactoryException {
        String outputDirectory = globalOptions.getOutputPath();
        int existentMaxDepth = determineExistentTileSetMaxDepth(outputDirectory);
        log.info("existent max depth: {}", existentMaxDepth);

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

        if (globalOptions.isCalculateNormalsExtension()) {
            terrainLayer.addExtension("octvertexnormals");
        }
        if (globalOptions.isWaterMaskExtension()) {
            terrainLayer.addExtension("watermask");
        }
        if (globalOptions.isMetaDataExtension()) {
            terrainLayer.addExtension("metadata");
        }

        log.info("----------------------------------------");
        int minTileDepth = 0;
        int maxTileDepth = globalOptions.getMaximumTileDepth();

        int availableMaxDepth = this.availableTileSet.getMaxAvailableDepth();
        if (availableMaxDepth < maxTileDepth) {
            maxTileDepth = availableMaxDepth;
        }

        // delete available tile ranges over maxTileDepth
        this.availableTileSet.deleteTileRangesOverDepth(maxTileDepth);

        // if the maxTileDepth is less than the existent max depth, set the maxTileDepth to the existent max depth
        minTileDepth = Math.max(minTileDepth, existentMaxDepth + 1);

        for (int depth = minTileDepth; depth <= maxTileDepth; depth += 1) {
            long startTime = System.currentTimeMillis();

            // check if the temp folder exists
            if (!existTempFiles(depth)) {
                log.info("making tempFiles from quantized meshes... depth: {}", depth - 1);
                makeTempFilesFromQuantizedMeshes(depth - 1);
                makeChildrenTempFiles(depth - 1);
            }

            // Set terrainLayer.available of tileSet json
            //terrainLayer.getAvailable().add(tilesRange); // this is used to save the terrainLayer.json
            this.triangleRefinementMaxIterations = TileWgs84Utils.getRefinementIterations(depth);
            this.terrainElevationDataManager.deleteObjects();
            this.terrainElevationDataManager = new TerrainElevationDataManager(); // new
            this.terrainElevationDataManager.setTileWgs84Manager(this);
            this.terrainElevationDataManager.setTerrainElevationDataFolderPath(this.depthGeoTiffFolderPathMap.get(depth));
            this.terrainElevationDataManager.makeTerrainQuadTree(depth);
            int mosaicSize = globalOptions.getMosaicSize();

            //List<TileRange> subDividedTilesRanges = TileWgs84Utils.subDivideTileRange(tilesRange, mosaicSize, mosaicSize, null);
            List<TileRange> availableTileRangesAtDepth = this.availableTileSet.getAvailableTileRangesAtDepth(depth);
            List<TileRange> subDividedTilesRanges = new ArrayList<>();
            // for each available tile range, subdivide it into mosaicSize x mosaicSize tiles, and put them into subDividedTilesRanges
            for (TileRange availableTileRange : availableTileRangesAtDepth) {
                TileWgs84Utils.subDivideTileRange(availableTileRange, mosaicSize, mosaicSize, subDividedTilesRanges);
            }

            log.info("[Tile][{}/{}] Start generating tile meshes - Divided Tiles Size: {}", depth, maxTileDepth, subDividedTilesRanges.size());
            AtomicInteger counter = new AtomicInteger(0);

            int total = subDividedTilesRanges.size();
            for (TileRange subDividedTilesRange : subDividedTilesRanges) {
                int progress = counter.incrementAndGet();
                log.info("[Tile][{}/{}][{}/{}] generate wgs84 raster all tiles...", depth, maxTileDepth, progress, total);
                TileRange expandedTilesRange = subDividedTilesRange.expand1();
                this.terrainElevationDataManager.makeAllTileWgs84Raster(expandedTilesRange, this);

                log.info("[Tile][{}/{}][{}/{}] process tiling...", depth, maxTileDepth, progress, total);
                TileMatrix tileMatrix = new TileMatrix(subDividedTilesRange, this);

                boolean isFirstGeneration = (depth == 0);
                tileMatrix.makeMatrixMesh(isFirstGeneration);
                tileMatrix.deleteObjects();
            }

            if (!GlobalOptions.getInstance().isLeaveTemp()) {
                this.deleteTempFilesByDepth(depth);
            }

            this.terrainElevationDataManager.deleteGeoTiffManager();
            this.terrainElevationDataManager.deleteTileRaster();
            this.terrainElevationDataManager.deleteCoverage();

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
            log.debug("[Tile][{}/{}] Java Heap Size: {} - MaxMem: {}MB / TotalMem: {}MB / FreeMem: {}MB / UsedMem: {}MB ({}%)", depth, maxTileDepth, javaHeapSize, maxMem, totalMem, freeMem, usedMem);
            log.info("----------------------------------------");
        }
        terrainLayer.saveJsonFileCustom(globalOptions.getOutputPath(), "layer.json", this.availableTileSet);
    }

    public void makeTileMeshesCustom() throws IOException, TransformException, FactoryException {
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

        if (globalOptions.isCalculateNormalsExtension()) {
            terrainLayer.addExtension("octvertexnormals");
        }
        if (globalOptions.isWaterMaskExtension()) {
            terrainLayer.addExtension("watermask");
        }
        if (globalOptions.isMetaDataExtension()) {
            terrainLayer.addExtension("metadata");
        }

        log.info("----------------------------------------");
        int minTileDepth = 0;
        int maxTileDepth = globalOptions.getMaximumTileDepth();
        int availableMaxDepth = this.availableTileSet.getMaxAvailableDepth();
        if (maxTileDepth < 0) {
            maxTileDepth = availableMaxDepth;
        } else if (availableMaxDepth < maxTileDepth) {
            maxTileDepth = availableMaxDepth;
        }

        // delete available tile ranges over maxTileDepth
        this.availableTileSet.deleteTileRangesOverDepth(maxTileDepth);

        for (int depth = minTileDepth; depth <= maxTileDepth; depth += 1) {
            long startTime = System.currentTimeMillis();

            // Set terrainLayer.available of tileSet JSON
            //terrainLayer.getAvailable().add(tilesRange); // this is used to save the terrainLayer.json
            this.triangleRefinementMaxIterations = TileWgs84Utils.getRefinementIterations(depth);
            this.terrainElevationDataManager.deleteObjects();
            this.terrainElevationDataManager = new TerrainElevationDataManager(); // new
            this.terrainElevationDataManager.setTileWgs84Manager(this);
            this.terrainElevationDataManager.setTerrainElevationDataFolderPath(this.depthGeoTiffFolderPathMap.get(depth));
            this.terrainElevationDataManager.makeTerrainQuadTree(depth);
            int mosaicSize = globalOptions.getMosaicSize();

            List<TileRange> availableTileRangesAtDepth = this.availableTileSet.getAvailableTileRangesAtDepth(depth);
            List<TileRange> subDividedTilesRanges = new ArrayList<>();
            // for each available tile range, subdivide it into mosaicSize x mosaicSize tiles, and put them into subDividedTilesRanges
            for (TileRange availableTileRange : availableTileRangesAtDepth) {
                TileWgs84Utils.subDivideTileRange(availableTileRange, mosaicSize, mosaicSize, subDividedTilesRanges);
            }

            log.info("[Tile][{}/{}] Start generating tile meshes - Divided Tiles Size: {}", depth, maxTileDepth, subDividedTilesRanges.size());
            AtomicInteger counter = new AtomicInteger(0);

            int total = subDividedTilesRanges.size();
            for (TileRange subDividedTilesRange : subDividedTilesRanges) {
                int progress = counter.incrementAndGet();
                log.info("[Tile][{}/{}][{}/{}] generate raster tiles...", depth, maxTileDepth, progress, total);
                TileRange expandedTilesRange = subDividedTilesRange.expand1();
                this.terrainElevationDataManager.makeAllTileWgs84Raster(expandedTilesRange, this);

                log.info("[Tile][{}/{}][{}/{}] process quantized mesh tiling...", depth, maxTileDepth, progress, total);
                TileMatrix tileMatrix = new TileMatrix(subDividedTilesRange, this);

                boolean isFirstGeneration = (depth == minTileDepth);
                tileMatrix.makeMatrixMesh(isFirstGeneration);
                tileMatrix.deleteObjects();

                if (!GlobalOptions.getInstance().isLeaveTemp()) {
                    // now, delete tempFiles of subDividedTilesRange
                    TileRange tilesToDeleteRange = subDividedTilesRange.clone();
                    //tilesToDeleteRange.translate(-1, -1);
                    if (tilesToDeleteRange.getMaxTileX() - tilesToDeleteRange.getMinTileX() > 6 &&
                            tilesToDeleteRange.getMaxTileY() - tilesToDeleteRange.getMinTileY() > 6) {
                        tilesToDeleteRange = tilesToDeleteRange.expand(-1);
                        deleteTempFilesByTileRange(tilesToDeleteRange);
                    }
                }
            }

            this.terrainElevationDataManager.deleteGeoTiffManager();
            this.terrainElevationDataManager.deleteTileRaster();
            this.terrainElevationDataManager.deleteCoverage();

            if (!GlobalOptions.getInstance().isLeaveTemp()) {
                this.deleteTempFilesByDepth(depth);
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
            log.debug("[Tile][{}/{}] Java Heap Size: {} - MaxMem: {}MB / TotalMem: {}MB / FreeMem: {}MB / UsedMem: {}MB ({}%)", depth, maxTileDepth, javaHeapSize, maxMem, totalMem, freeMem, usedMem);
            log.info("----------------------------------------");
        }
        terrainLayer.saveJsonFileCustom(globalOptions.getOutputPath(), "layer.json", this.availableTileSet);
    }

    private void deleteTempFilesByDepth(int depth) {
        String tempPath = globalOptions.getTileTempPath();
        String depthStr = "L" + depth;
        String depthTempPath = tempPath + File.separator + depthStr;
        File depthTempFolder = new File(depthTempPath);
        if (!depthTempFolder.exists()) {
            return;
        }

        // delete all files and folders inside the depthTempFolder and then delete the depthTempFolder itself
        org.apache.commons.io.FileUtils.deleteQuietly(depthTempFolder);
        //FileUtils.deleteDirectory(depthTempFolder);
    }

    private void deleteTempFilesByTileRange(TileRange tileRange) {
        int depth = tileRange.getTileDepth();
        String tempPath = globalOptions.getTileTempPath();
        String depthStr = "L" + depth;
        String depthTempPath = tempPath + File.separator + depthStr;
        File depthTempFolder = new File(depthTempPath);
        if (!depthTempFolder.exists()) {
            return;
        }
        int minTileX = tileRange.getMinTileX();
        int maxTileX = tileRange.getMaxTileX();
        int minTileY = tileRange.getMinTileY();
        int maxTileY = tileRange.getMaxTileY();
        for (int x = minTileX; x <= maxTileX; x++) {
            String xFolderName = "X" + x;
            String xFolderPath = depthTempPath + File.separator + xFolderName;
            File xFolder = new File(xFolderPath);
            if (!xFolder.exists()) {
                continue;
            }

            for (int y = minTileY; y <= maxTileY; y++) {
                String tempFileName = "L" + depth + "_" + xFolderName + "_Y" + y + ".til";
                String tempFilePath = xFolderPath + File.separator + tempFileName;
                File tempFile = new File(tempFilePath);
                if (tempFile.exists()) {
                    if (tempFile.delete()) {
                        log.debug("Deleted temp file: {}", tempFilePath);
                    } else {
                        log.warn("Failed to delete temp file: {}", tempFilePath);
                    }
                }
            }

            // if the X folder is empty, delete it
            String[] remainingFiles = xFolder.list();
            if (remainingFiles != null && remainingFiles.length == 0) {
                if (xFolder.delete()) {
                    log.debug("Deleted empty X folder: {}", xFolderPath);
                } else {
                    log.warn("Failed to delete X folder: {}", xFolderPath);
                }
            }
        }
    }

    public void makeTileMeshesContinue() throws IOException, TransformException, FactoryException {
        String outputDirectory = globalOptions.getOutputPath();
        int existentMaxDepth = determineExistentTileSetMaxDepth(outputDirectory);
        log.info("existent max depth: {}", existentMaxDepth);

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

        if (globalOptions.isCalculateNormalsExtension()) {
            terrainLayer.addExtension("octvertexnormals");
        }
        if (globalOptions.isWaterMaskExtension()) {
            terrainLayer.addExtension("watermask");
        }
        if (globalOptions.isMetaDataExtension()) {
            terrainLayer.addExtension("metadata");
        }

        log.info("----------------------------------------");
        int minTileDepth = globalOptions.getMinimumTileDepth();
        int maxTileDepth = globalOptions.getMaximumTileDepth();

        // if the maxTileDepth is less than the existent max depth, set the maxTileDepth to the existent max depth
        minTileDepth = Math.max(minTileDepth, existentMaxDepth + 1);

        for (int depth = 0; depth < minTileDepth; depth++) {
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
        }

        for (int depth = minTileDepth; depth <= maxTileDepth; depth += 1) {
            long startTime = System.currentTimeMillis();
            //Date startDate = new Date(startTime);

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

            // check if the temp folder exists
            if (!existTempFiles(depth)) {
                log.info("making tempFiles from quantized meshes... depth: {}", depth - 1);
                makeTempFilesFromQuantizedMeshes(depth - 1);
                makeChildrenTempFiles(depth - 1);
            }

            // Set terrainLayer.available of tileSet json
            terrainLayer.getAvailable().add(tilesRange); // this is used to save the terrainLayer.json
            this.triangleRefinementMaxIterations = TileWgs84Utils.getRefinementIterations(depth);
            this.terrainElevationDataManager.deleteObjects();
            this.terrainElevationDataManager = new TerrainElevationDataManager(); // new
            this.terrainElevationDataManager.setTileWgs84Manager(this);
            this.terrainElevationDataManager.setTerrainElevationDataFolderPath(this.depthGeoTiffFolderPathMap.get(depth));
            this.terrainElevationDataManager.makeTerrainQuadTree(depth);

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

                log.info("[Tile][{}/{}][{}/{}] process tiling...", depth, maxTileDepth, progress, total);
                TileMatrix tileMatrix = new TileMatrix(subDividedTilesRange, this);

                boolean isFirstGeneration = (depth == 0);
                tileMatrix.makeMatrixMesh(isFirstGeneration);
                tileMatrix.deleteObjects();
            }

            this.terrainElevationDataManager.deleteGeoTiffManager();
            this.terrainElevationDataManager.deleteTileRaster();
            this.terrainElevationDataManager.deleteCoverage();

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
            log.debug("[Tile][{}/{}] Java Heap Size: {} - MaxMem: {}MB / TotalMem: {}MB / FreeMem: {}MB / UsedMem: {}MB ({}%)", depth, maxTileDepth, javaHeapSize, maxMem, totalMem, freeMem, usedMem);
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
        return maxTriangleSizeForTileDepthList.get(depth);
    }

    public double getMinTriangleSizeForTileDepth(int depth) {
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
        log.info("Found {} GeoTiff files in the input path: {}", rasterFileNames.size(), inputPath);

        if (rasterFileNames.isEmpty()) {
            log.error("No GeoTiff files found in the input path: {}", inputPath);
            throw new RuntimeException("Error: No GeoTiff files found in the input path: " + inputPath);
        }

        standardizeRasters(rasterFileNames);

        // now make the list of standardized geotiff files (20250311 jinho seongdo)
        File tempFolder = new File(globalOptions.getStandardizeTempPath());
        List<String> standardizedGeoTiffPaths = new ArrayList<>();
        FileUtils.getFilePathsByExtension(tempFolder.getAbsolutePath(), ".tif", standardizedGeoTiffPaths, true);
        if (standardizedGeoTiffPaths.isEmpty()) {
            log.error("No standardized GeoTiff files found in the standardization temp path: {}", tempFolder.getAbsolutePath());
            throw new RuntimeException("Error: No standardized GeoTiff files found in the standardization temp path: " + tempFolder.getAbsolutePath());
        }
        this.getStandardizedGeoTiffFiles().clear();
        for (String standardizedGeoTiffPath : standardizedGeoTiffPaths) {
            this.getStandardizedGeoTiffFiles().add(new File(standardizedGeoTiffPath));
        }
    }

    public void standardizeRasters(List<String> geoTiffFileNames) {
        String tempPath = globalOptions.getStandardizeTempPath();
        File tempFolder = new File(tempPath);
        if (!tempFolder.exists() && tempFolder.mkdirs()) {
            log.debug("Created standardization folder: {}", tempFolder.getAbsolutePath());
        }
        globalOptions.setInputPath(tempFolder.getAbsolutePath());
        double maxUsefulPixelSizeMeters = getMaxUsefulPixelSizeMeters();

        // Apply geoid correction only when geoid data is configured.
        String geoidPath = globalOptions.getGeoidPath();
        boolean hasGeoid = geoidPath != null && !geoidPath.isEmpty();

        if (hasGeoid) {
            File geoidFile = new File(geoidPath);
            geoTiffFileNames.forEach(geoTiffFileName -> {
                GridCoverage2D originalGridCoverage2D = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(geoTiffFileName);
                GridCoverage2D standardizeSource = prepareCoverageForStandardization(geoTiffFileName, originalGridCoverage2D, maxUsefulPixelSizeMeters);
                RasterStandardizer rasterStandardizer = new RasterStandardizer();
                File sourceOutputDirectory = resolveStandardizedSourceDirectory(tempFolder, geoTiffFileName);
                try {
                    rasterStandardizer.standardizeWithGeoid(standardizeSource, sourceOutputDirectory, geoidFile);
                } finally {
                    if (standardizeSource != originalGridCoverage2D) {
                        standardizeSource.dispose(true);
                    }
                    originalGridCoverage2D.dispose(true);
                }
            });
        } else {
            geoTiffFileNames.forEach(geoTiffFileName -> {
                GridCoverage2D originalGridCoverage2D = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(geoTiffFileName);
                GridCoverage2D standardizeSource = prepareCoverageForStandardization(geoTiffFileName, originalGridCoverage2D, maxUsefulPixelSizeMeters);
                RasterStandardizer rasterStandardizer = new RasterStandardizer();
                File sourceOutputDirectory = resolveStandardizedSourceDirectory(tempFolder, geoTiffFileName);
                try {
                    rasterStandardizer.standardize(standardizeSource, sourceOutputDirectory);
                } finally {
                    if (standardizeSource != originalGridCoverage2D) {
                        standardizeSource.dispose(true);
                    }
                }
            });
        }
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
        resizeRasters(terrainElevationDataFolderPath, currentFolderPath);
    }

    public void resizeRasters(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException {
        // load all geoTiffFiles
        List<String> geoTiffFileNames = new ArrayList<>();
        FileUtils.getFileNames(terrainElevationDataFolderPath, ".tif", geoTiffFileNames);

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

                if (desiredPixelSizeXinMeters < pixelSizeMeters.x) {
                    // In this case just assign the originalGeoTiffFolderPath
                    this.depthGeoTiffFolderPathMap.put(depth, globalOptions.getInputPath());
                    continue;
                }

                String depthStr = String.valueOf(depth);
                String resizedGeoTiffFolderPath = resolveResizedGeoTiffFolderPath(depthStr, currentFolderPath, geoTiffFilePath);
                String resizedGeoTiffFilePath = resizedGeoTiffFolderPath + File.separator + geoTiffFileName;

                // check if exist the file
                if (FileUtils.isFileExists(resizedGeoTiffFilePath)) {
                    // in this case, just assign the resizedGeoTiffFolderPath
                    String resizedGeoTiffSetFolderPathForThisDepth = globalOptions.getResizedTiffTempPath() + File.separator + depthStr;
                    this.depthGeoTiffFolderPathMap.put(depth, resizedGeoTiffSetFolderPathForThisDepth);
                    continue;
                }

                // in this case, resize the geotiff
                GridCoverage2D resizedGridCoverage2D = gaiaGeoTiffManager.getResizedCoverage2D(geoTiffFilePath, originalGridCoverage2D, desiredPixelSizeXinMeters, desiredPixelSizeYinMeters);
                FileUtils.createAllFoldersIfNoExist(resizedGeoTiffFolderPath);
                gaiaGeoTiffManager.saveGridCoverage2D(resizedGridCoverage2D, resizedGeoTiffFilePath);

                resizedGridCoverage2D.dispose(true);

                String resizedGeoTiffSetFolderPathForThisDepth = globalOptions.getResizedTiffTempPath() + File.separator + depthStr;
                this.depthGeoTiffFolderPathMap.put(depth, resizedGeoTiffSetFolderPathForThisDepth);
            }
        }

        gaiaGeoTiffManager.clear();

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

    public boolean originIsLeftUp() {
        return this.originIsLeftUp;
    }

    private File resolveStandardizedSourceDirectory(File standardizeRoot, String sourceGeoTiffPath) {
        File sourceDirectory = new File(standardizeRoot, RasterStandardizer.sourceDirectoryName(sourceGeoTiffPath));
        if (!sourceDirectory.exists() && !sourceDirectory.mkdirs()) {
            throw new RuntimeException("Failed to create standardize source directory: " + sourceDirectory.getAbsolutePath());
        }
        return sourceDirectory;
    }

    private String resolveResizedGeoTiffFolderPath(String depth, String currentFolderPath, String sourceGeoTiffPath) {
        StringBuilder relativePath = new StringBuilder();
        if (currentFolderPath != null && !currentFolderPath.isBlank()) {
            relativePath.append(currentFolderPath);
            if (!currentFolderPath.endsWith(File.separator)) {
                relativePath.append(File.separator);
            }
        }
        relativePath.append(RasterStandardizer.sourceDirectoryName(sourceGeoTiffPath));
        return globalOptions.getResizedTiffTempPath() + File.separator + depth + File.separator + relativePath;
    }

    private GridCoverage2D prepareCoverageForStandardization(String geoTiffFilePath, GridCoverage2D originalCoverage, double maxUsefulPixelSizeMeters) {
        if (maxUsefulPixelSizeMeters <= 0.0) {
            return originalCoverage;
        }

        try {
            Vector2d pixelSizeMeters = GaiaGeoTiffUtils.getPixelSizeMeters(originalCoverage);
            if (pixelSizeMeters.x <= 0.0 || pixelSizeMeters.y <= 0.0) {
                return originalCoverage;
            }

            if (pixelSizeMeters.x >= maxUsefulPixelSizeMeters && pixelSizeMeters.y >= maxUsefulPixelSizeMeters) {
                return originalCoverage;
            }

            log.info("[Pre][Standardization] Downsampling {} before split. pixelSize={}m -> {}m",
                geoTiffFilePath, pixelSizeMeters.x, maxUsefulPixelSizeMeters);
            return gaiaGeoTiffManager.getResizedCoverage2D(
                geoTiffFilePath,
                originalCoverage,
                maxUsefulPixelSizeMeters,
                maxUsefulPixelSizeMeters
            );
        } catch (Exception e) {
            log.warn("[Pre][Standardization] Failed to pre-downsample {}. Using original coverage.", geoTiffFilePath, e);
            return originalCoverage;
        }
    }

    private double getMaxUsefulPixelSizeMeters() {
        int maxDepth = globalOptions.getMaximumTileDepth();
        if (maxDepth < 0) {
            maxDepth = this.availableTileSet.getMaxAvailableDepth();
        } else {
            int availableMaxDepth = this.availableTileSet.getMaxAvailableDepth();
            if (availableMaxDepth >= 0) {
                maxDepth = Math.min(maxDepth, availableMaxDepth);
            }
        }

        if (maxDepth < 0) {
            return 0.0;
        }

        double desiredPixelSize = this.depthDesiredPixelSizeXinMetersMap.getOrDefault(maxDepth, 0.0);
        if (desiredPixelSize <= 0.0) {
            return 0.0;
        }

        return desiredPixelSize * 0.5;
    }

    public void calculateAvailableTilesForEachDepth() throws IOException, FactoryException {
        String terrainElevationDataFolderPath = globalOptions.getInputPath();
        File inputFolder = new File(terrainElevationDataFolderPath);

        List<String> rasterFileNames = new ArrayList<>();
        if (inputFolder.exists() && inputFolder.isDirectory()) {
            FileUtils.getFilePathsByExtension(terrainElevationDataFolderPath, ".tif", rasterFileNames, true);
        } else if (inputFolder.exists() && inputFolder.isFile()) {
            if (terrainElevationDataFolderPath.endsWith(".tif")) {
                rasterFileNames.add(terrainElevationDataFolderPath);
            }
        } else {
            log.error("Input path is not exist or not a directory: {}", terrainElevationDataFolderPath);
            throw new RuntimeException("Error: Input path is not exist or not a directory: " + terrainElevationDataFolderPath);
        }

        if (rasterFileNames.isEmpty()) {
            log.error("No GeoTiff files found in the input path: {}", terrainElevationDataFolderPath);
            throw new RuntimeException("Error: No GeoTiff files found in the input path: " + terrainElevationDataFolderPath);
        }

        // now load all geotiff and make geotiff geoExtension data
        int geoTiffFilesSize = rasterFileNames.size();
        int geoTiffFilesCount = 0;

        // for depth = 0, set the available tile range to the whole world
        TileRange tilesRange = new TileRange();
        tilesRange.setMinTileX(0);
        tilesRange.setMaxTileX(1);
        tilesRange.setMinTileY(0);
        tilesRange.setMaxTileY(0);
        List<TileRange> tileRanges = availableTileSet.getMapDepthAvailableTileRanges().computeIfAbsent(0, k -> new java.util.ArrayList<>());
        tileRanges.add(tilesRange);

        // TODO : Multi-threading
        for (String geoTiffFileName : rasterFileNames) {
            log.info("[Pre][Resize GeoTiff][{}/{}] resizing geoTiff : {} ", ++geoTiffFilesCount, geoTiffFilesSize, geoTiffFileName);
            // check if the geotiffFileName is no usable
            if (this.mapNoUsableGeotiffPaths.containsKey(geoTiffFileName)) {
                continue;
            }

            GridCoverage2D originalGridCoverage2D = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(geoTiffFileName);
            CoordinateReferenceSystem crsTarget = originalGridCoverage2D.getCoordinateReferenceSystem2D();
            if (!(crsTarget instanceof ProjectedCRS || crsTarget instanceof GeographicCRS)) {
                log.error("The supplied grid coverage uses an unsupported crs! You are allowed to use only projected and geographic coordinate reference systems");
                throw new GeoTiffException(null, "The supplied grid coverage uses an unsupported crs! You are allowed to use only projected and geographic coordinate reference systems", null);
            }

            // get pixel size in meters and the geographic extent
            Vector2d pixelSizeMeters = GaiaGeoTiffUtils.getPixelSizeMeters(originalGridCoverage2D);

            GeometryFactory gf = new GeometryFactory();
            crsTarget = originalGridCoverage2D.getCoordinateReferenceSystem2D();
            CoordinateReferenceSystem crsWgs84 = CRS.decode("EPSG:4326", true);
            MathTransform targetToWgs = CRS.findMathTransform(crsTarget, crsWgs84, true);

            GeographicExtension geographicExtension = new GeographicExtension();
            try {
                GaiaGeoTiffUtils.getGeographicExtension(originalGridCoverage2D, gf, targetToWgs, geographicExtension);
            } catch (Exception ex) {
                log.error("Error calculating geographic extension for geotiff: {}", geoTiffFileName, ex);
                continue;
            }

            availableTileSet.addAvailableExtensions(pixelSizeMeters.x, geographicExtension);
        }
        availableTileSet.recombineTileRanges();

        if (globalOptions.getMaximumTileDepth() < 0) {
            globalOptions.setMaximumTileDepth(availableTileSet.getMaxAvailableDepth());
        }
        System.gc();
    }
}
