package com.gaia3d.terrain.tile;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.io.LittleEndianDataInputStream;
import com.gaia3d.quantized.mesh.QuantizedMesh;
import com.gaia3d.quantized.mesh.QuantizedMeshManager;
import com.gaia3d.terrain.structure.GeographicExtension;
import com.gaia3d.terrain.structure.TerrainTriangle;
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
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffException;
import org.geotools.referencing.CRS;
import org.joml.Vector2d;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.crs.ProjectedCRS;
import org.geotools.api.referencing.operation.TransformException;

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

    // the list of standardized geotiff files. This the real input for the terrain elevation data
    private List<File> standardizedGeoTiffFiles = new ArrayList<>();

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

        this.depthGeoTiffFolderPathMap.clear();
        this.depthDesiredPixelSizeXinMetersMap.clear();
        this.depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap.clear();
        this.maxTriangleSizeForTileDepthList.clear();
        this.minTriangleSizeForTileDepthList.clear();
        this.mapNoUsableGeotiffPaths.clear();
        this.gaiaGeoTiffManager.deleteObjects();
        this.gaiaGeoTiffManager.clear();
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
                log.info("[Tile][{}/{}][{}/{}] generate wgs84 raster all tiles...", depth, maxTileDepth, progress, total);
                TileRange expandedTilesRange = subDividedTilesRange.expand1();
                this.terrainElevationDataManager.makeAllTileWgs84Raster(expandedTilesRange, this);

                log.info("[Tile][{}/{}][{}/{}] process tiling...", depth, maxTileDepth, progress, total);
                TileMatrix tileMatrix = new TileMatrix(subDividedTilesRange, this);

                boolean isFirstGeneration = (depth == minTileDepth);
                tileMatrix.makeMatrixMesh(isFirstGeneration);
                tileMatrix.deleteObjects();

                if(!GlobalOptions.getInstance().isLeaveTemp()) {
                    // now, delete tempFiles of subDividedTilesRange
                    TileRange tilesToDeleteRange = subDividedTilesRange.clone();
                    tilesToDeleteRange.translate(-1, -1);
                    deleteTempFilesByTileRange(tilesToDeleteRange);
                }
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
            log.info("[Tile][{}/{}] Java Heap Size: {} - MaxMem: {}MB / TotalMem: {}MB / FreeMem: {}MB / UsedMem: {}MB ({}%)", depth, maxTileDepth, javaHeapSize, maxMem, totalMem, freeMem, usedMem);
            log.info("----------------------------------------");
        }
        terrainLayer.saveJsonFile(globalOptions.getOutputPath(), "layer.json");
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
        FileUtils.deleteDirectory(depthTempFolder);
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

        if (rasterFileNames.isEmpty()) {
            log.error("No GeoTiff files found in the input path: {}", inputPath);
            throw new RuntimeException("Error: No GeoTiff files found in the input path: " + inputPath);
        }

        standardizeRasters(rasterFileNames);

        // now make the list of standardized geotiff files (20250311 jinho seongdo)
        File tempFolder = new File(globalOptions.getStandardizeTempPath());
        File[] children = tempFolder.listFiles();
        if (children == null || children.length == 0) {
            log.error("No standardized GeoTiff files found in the standardization temp path: {}", tempFolder.getAbsolutePath());
            throw new RuntimeException("Error: No standardized GeoTiff files found in the standardization temp path: " + tempFolder.getAbsolutePath());
        }
        this.getStandardizedGeoTiffFiles().clear();
        for (File child : children) {
            this.getStandardizedGeoTiffFiles().add(child);
        }
    }

    public void standardizeRasters(List<String> geoTiffFileNames) {
        String tempPath = globalOptions.getStandardizeTempPath();
        File tempFolder = new File(tempPath);
        if (!tempFolder.exists() && tempFolder.mkdirs()) {
            log.debug("Created standardization folder: {}", tempFolder.getAbsolutePath());
        }
        globalOptions.setInputPath(tempFolder.getAbsolutePath());

        /* check if geoid is provided */
        String geoidPath = globalOptions.getGeoidPath();
        boolean hasGeoid = geoidPath != null && !geoidPath.isEmpty();

        if (hasGeoid) {
            File geoidFile = new File(geoidPath);
            geoTiffFileNames.forEach(geoTiffFileName -> {
                GridCoverage2D originalGridCoverage2D = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(geoTiffFileName);
                RasterStandardizer rasterStandardizer = new RasterStandardizer();
                rasterStandardizer.standardizeWithGeoid(originalGridCoverage2D, tempFolder, geoidFile);
                originalGridCoverage2D.dispose(true);
            });
        } else {
            geoTiffFileNames.forEach(geoTiffFileName -> {
                GridCoverage2D originalGridCoverage2D = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(geoTiffFileName);
                RasterStandardizer rasterStandardizer = new RasterStandardizer();
                rasterStandardizer.standardize(originalGridCoverage2D, tempFolder);
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
                String resizedGeoTiffFolderPath = globalOptions.getResizedTiffTempPath() + File.separator + depthStr + File.separator + currentFolderPath;
                String resizedGeoTiffFilePath = resizedGeoTiffFolderPath + File.separator + geoTiffFileName;

                // check if exist the file
                if (FileUtils.isFileExists(resizedGeoTiffFilePath)) {
                    // in this case, just assign the resizedGeoTiffFolderPath
                    String resizedGeoTiffSetFolderPathForThisDepth = globalOptions.getResizedTiffTempPath() + File.separator + depthStr;
                    this.depthGeoTiffFolderPathMap.put(depth, resizedGeoTiffSetFolderPathForThisDepth);
                    continue;
                }

                // in this case, resize the geotiff
                GridCoverage2D resizedGridCoverage2D = gaiaGeoTiffManager.getResizedCoverage2D(originalGridCoverage2D, desiredPixelSizeXinMeters, desiredPixelSizeYinMeters);
                FileUtils.createAllFoldersIfNoExist(resizedGeoTiffFolderPath);
                gaiaGeoTiffManager.saveGridCoverage2D(resizedGridCoverage2D, resizedGeoTiffFilePath);
                //resizedGridCoverage2D.dispose(true);

                String resizedGeoTiffSetFolderPathForThisDepth = globalOptions.getResizedTiffTempPath() + File.separator + depthStr;
                this.depthGeoTiffFolderPathMap.put(depth, resizedGeoTiffSetFolderPathForThisDepth);
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

    public boolean originIsLeftUp() {
        return this.originIsLeftUp;
    }
}
