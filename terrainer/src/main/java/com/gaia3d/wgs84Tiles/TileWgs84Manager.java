package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GaiaTriangle;
import com.gaia3d.basic.structure.GeographicExtension;
import com.gaia3d.command.GlobalOptions;
import com.gaia3d.reader.FileUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffException;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
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
    // For each depth level, use a different folder
    private final Map<Integer, String> depthGeoTiffFolderPathMap = new HashMap<>();
    private final Map<Integer, Double> depthDesiredPixelSizeXinMetersMap = new HashMap<>();
    private final Map<Integer, Double> depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap = new HashMap<>();
    private final List<TileWgs84> tileWgs84List = new ArrayList<>();
    // tileRasterSize : when triangles refinement, we use a DEM raster of this size
    private final int tileRasterSize = 256;
    private TerrainElevationDataManager terrainElevationDataManager = null;
    private String imageryType = "CRS84"; // "CRS84" or "WEB_MERCATOR"
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
    private List<TerrainElevationData> memSaveTerrainElevDataList = new ArrayList<>();
    private List<GaiaTriangle> memSaveTrianglesList = new ArrayList<>();
    private Vector2d memSavePixelSizeDegrees = new Vector2d();
    private Map<String, String> mapNoUsableGeotiffPaths = new HashMap<>();

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

        if (this.memSaveTerrainElevDataList != null) {
            this.memSaveTerrainElevDataList.clear();
        }

        if (this.memSaveTrianglesList != null) {
            this.memSaveTrianglesList.clear();
        }

        this.depthGeoTiffFolderPathMap.clear();
        this.depthDesiredPixelSizeXinMetersMap.clear();
        this.depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap.clear();
        this.maxTriangleSizeForTileDepthMap.clear();
        this.minTriangleSizeForTileDepthMap.clear();
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

        if (this.calculateNormals) {
            terrainLayer.addExtension("octvertexnormals");
        }

        int minTileDepth = globalOptions.getMinimumTileDepth();
        int maxTileDepth = globalOptions.getMaximumTileDepth();

        for (int depth = minTileDepth; depth <= maxTileDepth; depth += 1) {
            long startTime = System.currentTimeMillis();
            Date startDate = new Date(startTime);

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
            int maxCol = mosaicSize;
            int maxRow = mosaicSize;

//            int maxCol = 30;
//            int maxRow = 30;
            List<TilesRange> subDividedTilesRanges = TileWgs84Utils.subDivideTileRange(tilesRange, maxCol, maxRow, null);

            log.info("------------------------------------");
            log.info("[Tile] Start making tile meshes for depth: {} - DividedTilesRanges.size: {}", depth, subDividedTilesRanges.size());
            AtomicInteger counter = new AtomicInteger(0);
            int total = subDividedTilesRanges.size();
            for (TilesRange subDividedTilesRange : subDividedTilesRanges) {
                // 1rst, make all tilew rastyers.***
                TilesRange expandedTilesRange = subDividedTilesRange.expand1();
                this.terrainElevationDataManager.makeAllTileWgs84Rasters(expandedTilesRange, this);
                if (this.geoTiffFilesCount > 1) {
                    this.terrainElevationDataManager.deleteGeoTiffManager();
                    this.terrainElevationDataManager.deleteCoverage();
                }

                int progress = counter.incrementAndGet();
                log.info("[Tile] - {} depth tile progress... [{}/{}]", depth, progress, total);
                TileMatrix tileMatrix = new TileMatrix(subDividedTilesRange, this);
                boolean is1rstGeneration = depth == minTileDepth;
                tileMatrix.makeMatrixMesh(is1rstGeneration);

                // now delete the tileMatrix to free memory.
                tileMatrix.deleteObjects();

                if (this.geoTiffFilesCount > 1) {
                    this.terrainElevationDataManager.deleteGeoTiffManager();
                    this.terrainElevationDataManager.deleteCoverage();
                }
                this.terrainElevationDataManager.deleteTileRasters();
            }

            long endTime = System.currentTimeMillis();
            Date endDate = new Date(endTime);


            log.info("[Tile] End making tile meshes for depth: {} - Duration: {} ms}", depth, timeFormat(endTime - startTime));

            String javaHeapSize = System.getProperty("java.vm.name") + " " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "MB";
            // java vm이 사용할수 있는 총 메모리(bytes), -Xmx
            long maxMem = Runtime.getRuntime().maxMemory() / 1024 / 1024;
            // java vm에 할당된 총 메모리
            long totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024;
            // java vm이 추가로 할당 가능한 메모리
            long freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024;
            // 현재 사용중인 메모리
            long usedMem = totalMem - freeMem;
            // 퍼센트
            double pct = usedMem * 100 / maxMem;
            log.info("[Tile] - Java Heap Size: {} - MaxMem: {}MB - TotalMem: {}MB - FreeMem: {}MB - UsedMem: {}MB - Pct: {}%", javaHeapSize, maxMem, totalMem, freeMem, usedMem, pct);
        }

        // finally save the terrainLayer.json
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
        return maxTriangleSizeForTileDepthMap.get(depth);
    }

    public double getMinTriangleSizeForTileDepth(int depth) {
        return minTriangleSizeForTileDepthMap.get(depth);
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

    public boolean isNotExistsTileFile(TileIndices tileIndices) {
        String neighborFullPath = getTilePath(tileIndices);
        return !FileUtils.isFileExists(neighborFullPath);
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

    private void addNoUsableGeotiffPath(String noUsableGeotiffPath) {
        this.mapNoUsableGeotiffPaths.put(noUsableGeotiffPath, noUsableGeotiffPath);
    }

    public void processSplitGeotiffs(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException, TransformException {
        File terrainElevationDataFolder = new File(terrainElevationDataFolderPath);
        if (!terrainElevationDataFolder.exists()) {
            log.error("terrainElevationDataFolder is not exist: " + terrainElevationDataFolderPath);
            throw new RuntimeException("Error: terrainElevationDataFolder is not exist: " + terrainElevationDataFolderPath);
        } else if (!terrainElevationDataFolder.isDirectory()) {
            log.error("terrainElevationDataFolder is not a directory: " + terrainElevationDataFolderPath);
            throw new RuntimeException("Error: terrainElevationDataFolder is not a directory: " + terrainElevationDataFolderPath);
        }

        // 2nd resize the geotiffs
        splitGeotiffSet(terrainElevationDataFolderPath, currentFolderPath);

    }

    public void splitGeotiffSet(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException {
        // load all geoTiffFiles
        List<String> geoTiffFileNames = new ArrayList<>();
        FileUtils.getFileNames(terrainElevationDataFolderPath, ".tif", geoTiffFileNames);

        if (currentFolderPath == null) {
            currentFolderPath = "";
        }

        int maxPixelsWidth = globalOptions.getMaxRasterSize();

        GaiaGeoTiffManager gaiaGeoTiffManager = new GaiaGeoTiffManager();
        String splitTiffTempPath = globalOptions.getSplitTiffTempPath();

        // create the splitTiffTempPath if no exists
        FileUtils.createAllFoldersIfNoExist(splitTiffTempPath);

        // now load all geotiff and make geotiff geoExtension data
        for (String geoTiffFileName : geoTiffFileNames) {
            String geoTiffFilePath = terrainElevationDataFolderPath + File.separator + geoTiffFileName;
            GridCoverage2D originalGridCoverage2D = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(geoTiffFilePath);

            // check the size of the raster image of the coverage.***
            int width = originalGridCoverage2D.getRenderedImage().getWidth();
            int height = originalGridCoverage2D.getRenderedImage().getHeight();

            ReferencedEnvelope envelope = new ReferencedEnvelope(originalGridCoverage2D.getEnvelope());

            int cols = (int) Math.ceil(width / maxPixelsWidth);
            int rows = (int) Math.ceil(height / maxPixelsWidth);

            if (cols == 1 && rows == 1) {
                // in this case, do nothing
                continue;
            }

            if (width <= maxPixelsWidth && height <= maxPixelsWidth) {
                // in this case, do nothing
            } else {
                // the "geoTiffFileName" is no usable. Instead, use the split geoTiffs.***
                this.addNoUsableGeotiffPath(geoTiffFilePath);
                double tileWidth = envelope.getWidth() / cols;
                double tileHeight = envelope.getHeight() / rows;

                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < cols; col++) {
                        String geoTiffRawFileName = geoTiffFileName.substring(0, geoTiffFileName.length() - 4);
                        String outputFilePath = geoTiffRawFileName + "_" + row + "_" + col + ".tif";

                        // check if "outputFilePath" already exists
                        File outputFile = new File(splitTiffTempPath, outputFilePath);
                        if (outputFile.exists()) {
                            continue;
                        }

                        // Calcular los límites de cada subdivisión
                        double minX = envelope.getMinX() + col * tileWidth;
                        double minY = envelope.getMinY() + row * tileHeight;
                        double maxX = minX + tileWidth;
                        double maxY = minY + tileHeight;

                        ReferencedEnvelope tileEnvelope = new ReferencedEnvelope(minX, maxX, minY, maxY, envelope.getCoordinateReferenceSystem());

                        // Extraer la subdivisión
                        GridCoverage2D tileCoverage = gaiaGeoTiffManager.extractSubGridCoverage2D(originalGridCoverage2D, tileEnvelope);

                        try {
                            GeoTiffWriter writer = new GeoTiffWriter(outputFile);
                            writer.write(tileCoverage, null);
                            writer.dispose();
                        } catch (IOException e) {
                            System.err.println("Error al guardar el archivo GeoTIFF: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        // now check if exist folders inside the terrainElevationDataFolderPath
        List<String> folderNames = new ArrayList<>();
        com.gaia3d.reader.FileUtils.getFolderNames(currentFolderPath, folderNames);
        String auxFolderPath = currentFolderPath;
        for (String folderName : folderNames) {
            auxFolderPath = currentFolderPath + File.separator + folderName;
            String folderPath = terrainElevationDataFolderPath + File.separator + folderName;
            splitGeotiffSet(folderPath, auxFolderPath);
        }

        System.gc();
    }


    public void processResizeGeotiffs(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException, TransformException {
        File terrainElevationDataFolder = new File(terrainElevationDataFolderPath);
        if (!terrainElevationDataFolder.exists()) {
            log.error("terrainElevationDataFolder is not exist: " + terrainElevationDataFolderPath);
            throw new RuntimeException("Error: terrainElevationDataFolder is not exist: " + terrainElevationDataFolderPath);
        } else if (!terrainElevationDataFolder.isDirectory()) {
            log.error("terrainElevationDataFolder is not a directory: " + terrainElevationDataFolderPath);
            throw new RuntimeException("Error: terrainElevationDataFolder is not a directory: " + terrainElevationDataFolderPath);
        }

        // 1rst check geoTiff files count
        List<String> geoTiffFilePaths = new ArrayList<>();
        FileUtils.getFilePathsByExtension(terrainElevationDataFolderPath, "tif", geoTiffFilePaths, true);

        int geotiffCount = geoTiffFilePaths.size();
        this.setGeoTiffFilesCount(geotiffCount);

        log.info("[Pre][Resize GeoTiff] resizing geoTiffs Count : {} ", geotiffCount);

        if (geotiffCount == 1) {
            this.uniqueGeoTiffFilePath = geoTiffFilePaths.get(0);
        } else {
            // 2nd resize the geotiffs
            resizeGeotiffSet(terrainElevationDataFolderPath, currentFolderPath);
        }

    }

    public void resizeGeotiffSet(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException {
        // load all geoTiffFiles
        List<String> geoTiffFileNames = new ArrayList<>();
        com.gaia3d.reader.FileUtils.getFileNames(terrainElevationDataFolderPath, ".tif", geoTiffFileNames);

        if (currentFolderPath == null) {
            currentFolderPath = "";
        }

        GaiaGeoTiffManager gaiaGeoTiffManager = new GaiaGeoTiffManager();

        // now load all geotiff and make geotiff geoExtension data
        for (String geoTiffFileName : geoTiffFileNames) {
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
                //continue;
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
