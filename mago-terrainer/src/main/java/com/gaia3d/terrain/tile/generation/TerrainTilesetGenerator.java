package com.gaia3d.terrain.tile.generation;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.structure.GeographicExtension;
import com.gaia3d.terrain.structure.TerrainTriangle;
import com.gaia3d.terrain.tile.core.GeographicTerrainTile;
import com.gaia3d.terrain.tile.core.TileIndices;
import com.gaia3d.terrain.tile.core.TileRange;
import com.gaia3d.terrain.tile.custom.AvailableTileSet;
import com.gaia3d.terrain.tile.elevation.TerrainElevationData;
import com.gaia3d.terrain.tile.elevation.TerrainElevationModeler;
import com.gaia3d.terrain.tile.geotiff.GeoTiffCoverageStore;
import com.gaia3d.terrain.tile.layer.TerrainLayer;
import com.gaia3d.terrain.tile.mesh.TileMatrix;
import com.gaia3d.terrain.util.GeographicTerrainTileUtils;
import com.gaia3d.util.DecimalUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.joml.Vector2d;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
@Slf4j
public class TerrainTilesetGenerator {
    private final GlobalOptions globalOptions = GlobalOptions.getInstance();

    private final TerrainTilingContext tilingContext = new TerrainTilingContext();
    private final int rasterTileSize = tilingContext.getRasterTileSize();
    private final String imaginaryType = tilingContext.getImageryType(); // "CRS84" or "WEB_MERCATOR"

    // For each depth level, use the concrete GeoTIFF files selected for that level.
    private final Map<Integer, List<File>> depthGeoTiffFilesMap = new HashMap<>();
    private final Map<Integer, Double> depthDesiredPixelSizeXinMetersMap = new HashMap<>();
    private final Map<Integer, Double> depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap = new HashMap<>();
    private final List<GeographicTerrainTile> geographicTerrainTileList = new ArrayList<>();

    private final TerrainRasterPreprocessor rasterPreprocessor = new TerrainRasterPreprocessor(this);
    private final AvailableTileAnalyzer availableTileAnalyzer = new AvailableTileAnalyzer(this);
    private final TerrainTileStore terrainTileStore = new TerrainTileStore(this);

    // tileRasterSize : when triangles refinement, we use a DEM raster of this size
    private TerrainElevationModeler terrainElevationModeler = null;
    private int geoTiffFilesCount = 0;
    private double vertexCoincidentError = 1e-11;

    // Complex lunar terrain requires more refinement passes than Earth geodetic tiles
    private int triangleRefinementMaxIterations = 20;
    private TerrainLayer terrainLayer = null;
    private boolean originIsLeftUp = tilingContext.isOriginIsLeftUp(); // false = origin is left-down (Cesium Tile System)
    private List<Double> maxTriangleSizeForTileDepthList = new ArrayList<>();
    private List<Double> minTriangleSizeForTileDepthList = new ArrayList<>();

    private List<TerrainElevationData> terrainElevationDataList = new ArrayList<>();
    private List<TerrainTriangle> triangleList = new ArrayList<>();
    private Vector2d pixelSizeDegrees = new Vector2d();
    private Map<String, String> mapNoUsableGeotiffPaths = new HashMap<>();
    private GeoTiffCoverageStore geoTiffCoverageStore = new GeoTiffCoverageStore();

    // the list of standardized geotiff files. This the real input for the terrain elevation data
    private List<File> standardizedGeoTiffFiles = new ArrayList<>();

    // Available tileset
    private AvailableTileSet availableTileSet = new AvailableTileSet();

    public TerrainTilesetGenerator() {
        double intensity = globalOptions.getIntensity();

        // init the maxTriangleSizeForTileDepthMap
        for (int i = 0; i < 28; i++) {
            double tileSizeMeters = GeographicTerrainTileUtils.getTileSizeInMetersByDepth(i);
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
            double tileSizeMeters = GeographicTerrainTileUtils.getTileSizeInMetersByDepth(depth);
            double desiredPixelSizeXinMeters = tileSizeMeters / 256.0;
            this.depthDesiredPixelSizeXinMetersMap.put(depth, desiredPixelSizeXinMeters);
        }
    }

    public void setOriginIsLeftUp(boolean originIsLeftUp) {
        this.originIsLeftUp = originIsLeftUp;
        this.tilingContext.setOriginIsLeftUp(originIsLeftUp);
    }

    public void deleteObjects() {
        if (this.terrainElevationModeler != null) {
            this.terrainElevationModeler.deleteObjects();
            this.terrainElevationModeler = null;
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

        this.geographicTerrainTileList.clear();
        this.standardizedGeoTiffFiles.clear();
        this.geoTiffCoverageStore.clear();

        this.depthGeoTiffFilesMap.clear();
        this.depthDesiredPixelSizeXinMetersMap.clear();
        this.depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap.clear();
        this.maxTriangleSizeForTileDepthList.clear();
        this.minTriangleSizeForTileDepthList.clear();
        this.mapNoUsableGeotiffPaths.clear();
        this.availableTileSet.getMapDepthAvailableTileRanges().clear();
    }

    public void makeTempFilesFromQuantizedMeshes(int depth) {
        terrainTileStore.writeTempTilesFromQuantizedMeshes(depth);
    }

    public void makeTempFilesFromQuantizedMeshes(int depth, TileRange tileRange) {
        terrainTileStore.writeTempTilesFromQuantizedMeshes(depth, tileRange);
    }

    private void makeChildrenTempFiles(int depth) {
        terrainTileStore.writeChildTempTiles(depth);
    }

    private int determineExistentTileSetMaxDepth(String tileSetDirectory) {
        return terrainTileStore.determineExistentTileSetMaxDepth(tileSetDirectory);
    }

    private boolean existTempFiles(int depth) {
        return terrainTileStore.existTempFiles(depth);
    }

    public void generateFullTreeTileMeshes() throws IOException, TransformException, FactoryException {
        GeographicExtension geographicExtension = this.terrainElevationModeler.getRootGeographicExtension();

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
                GeographicTerrainTileUtils.selectTileIndicesArray(depth, minLon, maxLon, minLat, maxLat, tilesRange, originIsLeftUp);
            }

            // Set terrainLayer.available of tileSet JSON
            terrainLayer.getAvailable().add(tilesRange); // this is used to save the terrainLayer.json
            this.triangleRefinementMaxIterations = GeographicTerrainTileUtils.getRefinementIterations(depth);
            this.terrainElevationModeler.deleteObjects();
            this.terrainElevationModeler = new TerrainElevationModeler();
            this.terrainElevationModeler.setTerrainTilesetGenerator(this);
            this.terrainElevationModeler.setTerrainElevationDataFiles(resolveRasterFilesForDepth(depth));
            this.terrainElevationModeler.generateTerrainQuadTree(depth);

            int mosaicSize = globalOptions.getMosaicSize();
            List<TileRange> subDividedTilesRanges = GeographicTerrainTileUtils.subDivideTileRange(tilesRange, mosaicSize, mosaicSize, null);

            log.info("[Tile][{}/{}] Start generating tile meshes - Divided Tiles Size: {}", depth, maxTileDepth, subDividedTilesRanges.size());
            AtomicInteger counter = new AtomicInteger(0);

            int total = subDividedTilesRanges.size();
            for (TileRange subDividedTilesRange : subDividedTilesRanges) {
                int progress = counter.incrementAndGet();
                log.info("[Tile][{}/{}][{}/{}] generate raster tiles...", depth, maxTileDepth, progress, total);
                TileRange expandedTilesRange = subDividedTilesRange.expand1();
                this.terrainElevationModeler.prepareTileRastersForRange(expandedTilesRange, this);

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

            this.terrainElevationModeler.deleteGeoTiffManager();
            this.terrainElevationModeler.deleteTileRaster();
            this.terrainElevationModeler.deleteCoverage();

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

    public void generateModifiedAvailableTileMeshes() throws IOException, TransformException, FactoryException {
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

        GeographicExtension geographicExtension = this.terrainElevationModeler.getRootGeographicExtension();

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
        if (availableMaxDepth < maxTileDepth) {
            maxTileDepth = availableMaxDepth;
        }

        // delete available tile ranges over maxTileDepth
        this.availableTileSet.deleteTileRangesOverDepth(maxTileDepth);
        saveLayerJsonBeforeTileGeneration();

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
            this.triangleRefinementMaxIterations = GeographicTerrainTileUtils.getRefinementIterations(depth);
            this.terrainElevationModeler.deleteObjects();
            this.terrainElevationModeler = new TerrainElevationModeler();
            this.terrainElevationModeler.setTerrainTilesetGenerator(this);
            this.terrainElevationModeler.setTerrainElevationDataFiles(resolveRasterFilesForDepth(depth));
            this.terrainElevationModeler.generateTerrainQuadTree(depth);
            int mosaicSize = globalOptions.getMosaicSize();

            //List<TileRange> subDividedTilesRanges = GeographicTerrainTileUtils.subDivideTileRange(tilesRange, mosaicSize, mosaicSize, null);
            List<TileRange> availableTileRangesAtDepth = this.availableTileSet.getAvailableTileRangesAtDepth(depth);
            List<TileRange> subDividedTilesRanges = new ArrayList<>();
            // for each available tile range, subdivide it into mosaicSize x mosaicSize tiles, and put them into subDividedTilesRanges
            for (TileRange availableTileRange : availableTileRangesAtDepth) {
                GeographicTerrainTileUtils.subDivideTileRange(availableTileRange, mosaicSize, mosaicSize, subDividedTilesRanges);
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

                this.terrainElevationModeler.prepareTileRastersForRange(expandedTilesRange, this);

                log.info("[Tile][{}/{}][{}/{}] process tiling...", depth, maxTileDepth, progress, total);
                TileMatrix tileMatrix = new TileMatrix(subDividedTilesRange, this);

                boolean isFirstGeneration = (depth == 0);
                tileMatrix.makeMatrixMeshModifyMode(isFirstGeneration);
                tileMatrix.deleteObjects();
            }

            if (!GlobalOptions.getInstance().isLeaveTemp()) {
                this.deleteTempFilesByDepth(depth);
            }

            this.terrainElevationModeler.deleteGeoTiffManager();
            this.terrainElevationModeler.deleteTileRaster();
            this.terrainElevationModeler.deleteCoverage();

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

    public void continueAvailableTileMeshes() throws IOException, TransformException, FactoryException {
        String outputDirectory = globalOptions.getOutputPath();
        int existentMaxDepth = determineExistentTileSetMaxDepth(outputDirectory);
        log.info("existent max depth: {}", existentMaxDepth);

        GeographicExtension geographicExtension = this.terrainElevationModeler.getRootGeographicExtension();

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
        saveLayerJsonBeforeTileGeneration();

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
            this.triangleRefinementMaxIterations = GeographicTerrainTileUtils.getRefinementIterations(depth);
            this.terrainElevationModeler.deleteObjects();
            this.terrainElevationModeler = new TerrainElevationModeler();
            this.terrainElevationModeler.setTerrainTilesetGenerator(this);
            this.terrainElevationModeler.setTerrainElevationDataFiles(resolveRasterFilesForDepth(depth));
            this.terrainElevationModeler.generateTerrainQuadTree(depth);
            int mosaicSize = globalOptions.getMosaicSize();

            //List<TileRange> subDividedTilesRanges = GeographicTerrainTileUtils.subDivideTileRange(tilesRange, mosaicSize, mosaicSize, null);
            List<TileRange> availableTileRangesAtDepth = this.availableTileSet.getAvailableTileRangesAtDepth(depth);
            List<TileRange> subDividedTilesRanges = new ArrayList<>();
            // for each available tile range, subdivide it into mosaicSize x mosaicSize tiles, and put them into subDividedTilesRanges
            for (TileRange availableTileRange : availableTileRangesAtDepth) {
                GeographicTerrainTileUtils.subDivideTileRange(availableTileRange, mosaicSize, mosaicSize, subDividedTilesRanges);
            }

            log.info("[Tile][{}/{}] Start generating tile meshes - Divided Tiles Size: {}", depth, maxTileDepth, subDividedTilesRanges.size());
            AtomicInteger counter = new AtomicInteger(0);

            int total = subDividedTilesRanges.size();
            for (TileRange subDividedTilesRange : subDividedTilesRanges) {
                int progress = counter.incrementAndGet();
                log.info("[Tile][{}/{}][{}/{}] generate wgs84 raster all tiles...", depth, maxTileDepth, progress, total);
                TileRange expandedTilesRange = subDividedTilesRange.expand1();
                this.terrainElevationModeler.prepareTileRastersForRange(expandedTilesRange, this);

                log.info("[Tile][{}/{}][{}/{}] process tiling...", depth, maxTileDepth, progress, total);
                TileMatrix tileMatrix = new TileMatrix(subDividedTilesRange, this);

                boolean isFirstGeneration = (depth == 0);
                tileMatrix.makeMatrixMesh(isFirstGeneration);
                tileMatrix.deleteObjects();
            }

            if (!GlobalOptions.getInstance().isLeaveTemp()) {
                this.deleteTempFilesByDepth(depth);
            }

            this.terrainElevationModeler.deleteGeoTiffManager();
            this.terrainElevationModeler.deleteTileRaster();
            this.terrainElevationModeler.deleteCoverage();

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

    public void generateAvailableTileMeshes() throws IOException, TransformException, FactoryException {
        GeographicExtension geographicExtension = this.terrainElevationModeler.getRootGeographicExtension();

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
        saveLayerJsonBeforeTileGeneration();

        for (int depth = minTileDepth; depth <= maxTileDepth; depth += 1) {
            long startTime = System.currentTimeMillis();

            // Set terrainLayer.available of tileSet JSON
            //terrainLayer.getAvailable().add(tilesRange); // this is used to save the terrainLayer.json
            this.triangleRefinementMaxIterations = GeographicTerrainTileUtils.getRefinementIterations(depth);
            this.terrainElevationModeler.deleteObjects();
            this.terrainElevationModeler = new TerrainElevationModeler();
            this.terrainElevationModeler.setTerrainTilesetGenerator(this);
            this.terrainElevationModeler.setTerrainElevationDataFiles(resolveRasterFilesForDepth(depth));
            this.terrainElevationModeler.generateTerrainQuadTree(depth);
            int mosaicSize = globalOptions.getMosaicSize();

            List<TileRange> availableTileRangesAtDepth = this.availableTileSet.getAvailableTileRangesAtDepth(depth);
            List<TileRange> subDividedTilesRanges = new ArrayList<>();
            // for each available tile range, subdivide it into mosaicSize x mosaicSize tiles, and put them into subDividedTilesRanges
            for (TileRange availableTileRange : availableTileRangesAtDepth) {
                GeographicTerrainTileUtils.subDivideTileRange(availableTileRange, mosaicSize, mosaicSize, subDividedTilesRanges);
            }

            log.info("[Tile][{}/{}] Start generating tile meshes - Divided Tiles Size: {}", depth, maxTileDepth, subDividedTilesRanges.size());
            AtomicInteger counter = new AtomicInteger(0);

            int total = subDividedTilesRanges.size();
            for (TileRange subDividedTilesRange : subDividedTilesRanges) {
                int progress = counter.incrementAndGet();
                log.info("[Tile][{}/{}][{}/{}] generate raster tiles...", depth, maxTileDepth, progress, total);
                TileRange expandedTilesRange = subDividedTilesRange.expand1();
                this.terrainElevationModeler.prepareTileRastersForRange(expandedTilesRange, this);

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

            this.terrainElevationModeler.deleteGeoTiffManager();
            this.terrainElevationModeler.deleteTileRaster();
            this.terrainElevationModeler.deleteCoverage();

            if (!GlobalOptions.getInstance().isLeaveTemp()) {
                this.deleteTempFilesByDepth(depth);
            }

            long endTime = System.currentTimeMillis();
            log.info("[Tile][{}/{}] - End making tile meshes : Duration: {}", depth, maxTileDepth, DecimalUtils.millisecondToDisplayTime(endTime - startTime));

            String javaHeapSize = System.getProperty("java.vm.name") + " " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "MB";
            String maxMem = DecimalUtils.byteCountToDisplaySize(Runtime.getRuntime().maxMemory());
            String totalMem = DecimalUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory());
            String freeMem = DecimalUtils.byteCountToDisplaySize(Runtime.getRuntime().freeMemory());
            String usedMem = DecimalUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            log.debug("[Tile][{}/{}] Java Heap Size: {} - MaxMem: {}MB / TotalMem: {}MB / FreeMem: {}MB / UsedMem: {}MB ({}%)", depth, maxTileDepth, javaHeapSize, maxMem, totalMem, freeMem, usedMem);
            log.info("----------------------------------------");
        }
        terrainLayer.saveJsonFileCustom(globalOptions.getOutputPath(), "layer.json", this.availableTileSet);
    }

    private void saveLayerJsonBeforeTileGeneration() {
        log.info("[Pre][layer.json] Save layer.json before terrain tile generation.");
        terrainLayer.saveJsonFileCustom(globalOptions.getOutputPath(), "layer.json", this.availableTileSet);
    }

    private void deleteTempFilesByDepth(int depth) {
        terrainTileStore.deleteTempFilesByDepth(depth);
    }

    private void deleteTempFilesByTileRange(TileRange tileRange) {
        terrainTileStore.deleteTempFilesByTileRange(tileRange);
    }

    public void continueFullTreeTileMeshes() throws IOException, TransformException, FactoryException {
        String outputDirectory = globalOptions.getOutputPath();
        int existentMaxDepth = determineExistentTileSetMaxDepth(outputDirectory);
        log.info("existent max depth: {}", existentMaxDepth);

        GeographicExtension geographicExtension = this.terrainElevationModeler.getRootGeographicExtension();

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
                GeographicTerrainTileUtils.selectTileIndicesArray(depth, minLon, maxLon, minLat, maxLat, tilesRange, originIsLeftUp);
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
                GeographicTerrainTileUtils.selectTileIndicesArray(depth, minLon, maxLon, minLat, maxLat, tilesRange, originIsLeftUp);
            }

            // check if the temp folder exists
            if (!existTempFiles(depth)) {
                log.info("making tempFiles from quantized meshes... depth: {}", depth - 1);
                makeTempFilesFromQuantizedMeshes(depth - 1);
                makeChildrenTempFiles(depth - 1);
            }

            // Set terrainLayer.available of tileSet json
            terrainLayer.getAvailable().add(tilesRange); // this is used to save the terrainLayer.json
            this.triangleRefinementMaxIterations = GeographicTerrainTileUtils.getRefinementIterations(depth);
            this.terrainElevationModeler.deleteObjects();
            this.terrainElevationModeler = new TerrainElevationModeler();
            this.terrainElevationModeler.setTerrainTilesetGenerator(this);
            this.terrainElevationModeler.setTerrainElevationDataFiles(resolveRasterFilesForDepth(depth));
            this.terrainElevationModeler.generateTerrainQuadTree(depth);

            int mosaicSize = globalOptions.getMosaicSize();
            List<TileRange> subDividedTilesRanges = GeographicTerrainTileUtils.subDivideTileRange(tilesRange, mosaicSize, mosaicSize, null);

            log.info("[Tile][{}/{}] Start generating tile meshes - Divided Tiles Size: {}", depth, maxTileDepth, subDividedTilesRanges.size());
            AtomicInteger counter = new AtomicInteger(0);

            int total = subDividedTilesRanges.size();
            for (TileRange subDividedTilesRange : subDividedTilesRanges) {
                int progress = counter.incrementAndGet();
                log.info("[Tile][{}/{}][{}/{}] generate wgs84 raster all tiles...", depth, maxTileDepth, progress, total);
                TileRange expandedTilesRange = subDividedTilesRange.expand1();
                this.terrainElevationModeler.prepareTileRastersForRange(expandedTilesRange, this);

                log.info("[Tile][{}/{}][{}/{}] process tiling...", depth, maxTileDepth, progress, total);
                TileMatrix tileMatrix = new TileMatrix(subDividedTilesRange, this);

                boolean isFirstGeneration = (depth == 0);
                tileMatrix.makeMatrixMesh(isFirstGeneration);
                tileMatrix.deleteObjects();
            }

            this.terrainElevationModeler.deleteGeoTiffManager();
            this.terrainElevationModeler.deleteTileRaster();
            this.terrainElevationModeler.deleteCoverage();

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

    /**
     * @deprecated use {@link #generateFullTreeTileMeshes()}
     */
    @Deprecated
    public void makeTileMeshes() throws IOException, TransformException, FactoryException {
        generateFullTreeTileMeshes();
    }

    /**
     * @deprecated use {@link #generateModifiedAvailableTileMeshes()}
     */
    @Deprecated
    public void makeTileMeshesCustomModifyMode() throws IOException, TransformException, FactoryException {
        generateModifiedAvailableTileMeshes();
    }

    /**
     * @deprecated use {@link #continueAvailableTileMeshes()}
     */
    @Deprecated
    public void makeTileMeshesContinueCustom() throws IOException, TransformException, FactoryException {
        continueAvailableTileMeshes();
    }

    /**
     * @deprecated use {@link #generateAvailableTileMeshes()}
     */
    @Deprecated
    public void makeTileMeshesCustom() throws IOException, TransformException, FactoryException {
        generateAvailableTileMeshes();
    }

    /**
     * @deprecated use {@link #continueFullTreeTileMeshes()}
     */
    @Deprecated
    public void makeTileMeshesContinue() throws IOException, TransformException, FactoryException {
        continueFullTreeTileMeshes();
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
            double maxDiff = GeographicTerrainTileUtils.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(depth);
            depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap.put(depth, maxDiff);
            return depthMaxDiffBetweenGeoTiffSampleAndTrianglePlaneMap.get(depth);
        }
    }

    public String getTilePath(TileIndices tileIndices) {
        return terrainTileStore.getTilePath(tileIndices);
    }

    public String getQuantizedMeshTileFolderPath(TileIndices tileIndices) {
        return terrainTileStore.getQuantizedMeshTileFolderPath(tileIndices);
    }

    public String getQuantizedMeshTilePath(TileIndices tileIndices) {
        return terrainTileStore.getQuantizedMeshTilePath(tileIndices);
    }

    public GeographicTerrainTile loadOrCreateGeographicTerrainTile(TileIndices tileIndices) throws IOException, TransformException {
        return terrainTileStore.loadOrCreateGeographicTerrainTile(tileIndices);
    }

    public GeographicTerrainTile loadGeographicTerrainTile(TileIndices tileIndices) throws IOException {
        return terrainTileStore.loadGeographicTerrainTile(tileIndices);
    }

    private void addNoUsableGeotiffPath(String noUsableGeotiffPath) {
        this.mapNoUsableGeotiffPaths.put(noUsableGeotiffPath, noUsableGeotiffPath);
    }

    public void standardizeInputRasters() {
        rasterPreprocessor.standardizeInputRasters();
    }

    public void loadPreparedRasters() {
        rasterPreprocessor.loadPreparedRasters();
    }

    public void standardizeRasterFiles(List<String> geoTiffFileNames) {
        rasterPreprocessor.standardizeRasterFiles(geoTiffFileNames);
    }

    public void prepareDepthRasters(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException {
        rasterPreprocessor.prepareDepthRasters(terrainElevationDataFolderPath, currentFolderPath);
    }

    public void resizeRastersByDepth(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException {
        rasterPreprocessor.resizeRastersByDepth(terrainElevationDataFolderPath, currentFolderPath);
    }

    public List<File> resolveRasterFilesForDepth(int depth) {
        return rasterPreprocessor.resolveRasterFilesForDepth(depth);
    }

    /**
     * @deprecated use {@link #standardizeInputRasters()}
     */
    @Deprecated
    public void processStandardizeRasters() {
        standardizeInputRasters();
    }

    /**
     * @deprecated use {@link #loadPreparedRasters()}
     */
    @Deprecated
    public void loadExistingStandardizedAndResizedRasters() {
        loadPreparedRasters();
    }

    /**
     * @deprecated use {@link #standardizeRasterFiles(List)}
     */
    @Deprecated
    public void standardizeRasters(List<String> geoTiffFileNames) {
        standardizeRasterFiles(geoTiffFileNames);
    }

    /**
     * @deprecated use {@link #prepareDepthRasters(String, String)}
     */
    @Deprecated
    public void processResizeRasters(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException {
        prepareDepthRasters(terrainElevationDataFolderPath, currentFolderPath);
    }

    /**
     * @deprecated use {@link #resizeRastersByDepth(String, String)}
     */
    @Deprecated
    public void resizeRasters(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException {
        resizeRastersByDepth(terrainElevationDataFolderPath, currentFolderPath);
    }

    public boolean originIsLeftUp() {
        return this.originIsLeftUp;
    }

    /**
     * @deprecated use {@link #resolveRasterFilesForDepth(int)}
     */
    @Deprecated
    public List<File> resolveTerrainElevationDataFiles(int depth) {
        return resolveRasterFilesForDepth(depth);
    }

    public void calculateAvailableTilesForEachDepth() throws IOException, FactoryException {
        availableTileAnalyzer.calculateAvailableTilesForEachDepth();
    }

    public List<String> resolveInputRasterFileNames() {
        return availableTileAnalyzer.resolveInputRasterFileNames();
    }

    private List<String> resolveConfiguredInputPaths() {
        if (globalOptions.getInputPaths() != null && !globalOptions.getInputPaths().isEmpty()) {
            return globalOptions.getInputPaths();
        }
        if (globalOptions.getInputPath() != null && !globalOptions.getInputPath().isBlank()) {
            return List.of(globalOptions.getInputPath());
        }
        return List.of();
    }
}
