package com.gaia3d.terrain.tile.elevation;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.structure.GeographicExtension;
import com.gaia3d.terrain.structure.TerrainTriangle;
import com.gaia3d.terrain.tile.core.GeographicTerrainTileRaster;
import com.gaia3d.terrain.tile.core.TileIndices;
import com.gaia3d.terrain.tile.core.TileRange;
import com.gaia3d.terrain.tile.generation.TerrainTilesetGenerator;
import com.gaia3d.terrain.tile.geotiff.GeoTiffCoverageStore;
import com.gaia3d.terrain.types.PriorityType;
import com.gaia3d.terrain.util.GaiaGeoTiffUtils;
import com.gaia3d.terrain.util.GeographicTerrainTileUtils;
import com.gaia3d.util.FileUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.referencing.CRS;
import org.joml.Vector2d;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class TerrainElevationModeler {
    private static final boolean PRELOAD_TERRAIN_RASTERS = false;
    private static final long MIN_PRELOAD_BUDGET_BYTES = 128L * 1024L * 1024L;
    private static final long MAX_PRELOAD_BUDGET_BYTES = 1024L * 1024L * 1024L;
    private static final long HEAP_RESERVE_BYTES = 2L * 1024L * 1024L * 1024L;
    private static final long MIN_LIVE_RASTER_BUDGET_BYTES = 512L * 1024L * 1024L;
    private static final long MAX_LIVE_RASTER_BUDGET_BYTES = 2L * 1024L * 1024L * 1024L;

    private GlobalOptions globalOptions = GlobalOptions.getInstance();

    private TerrainTilesetGenerator terrainTilesetGenerator = null;
    private List<TerrainElevationData> terrainElevationDataArray = new ArrayList<>();
    private List<TerrainTriangle> trianglesArray = new ArrayList<>();
    private Map<Long, GeographicTerrainTileRaster> mapIndicesTileRaster = new HashMap<>();
    private Map<String, Double> gridAreaMap = new HashMap<>();
    private Map<String, Vector2d> priorityPixelSizeByGeoTiffName = new HashMap<>();

    // Inside the folder, there are multiple geoTiff files
    private String terrainElevationDataFolderPath;
    private List<File> terrainElevationDataFiles = new ArrayList<>();
    private int geoTiffFilesCount = 0;

    // if there are multiple geoTiff files, use this
    private int quadtreeMaxDepth = 10;
    private TerrainElevationDataQuadTree rootTerrainElevationDataQuadTree = null;
    private GeoTiffCoverageStore geoTiffManager = null;
    private boolean[] intersects = {false};
    private List<String> geoTiffFileNames = new ArrayList<>();
    private int terrainElevationDataQueryMark = 1;

    public void generateTerrainQuadTree(int depth) throws FactoryException, TransformException {
        List<File> standardizedGeoTiffFiles = terrainTilesetGenerator.getStandardizedGeoTiffFiles();
        loadTerrainElevationData(resolveConfiguredElevationFiles(), standardizedGeoTiffFiles);
        rootTerrainElevationDataQuadTree.makeQuadTree(quadtreeMaxDepth);
    }

    /**
     * @deprecated use {@link #generateTerrainQuadTree(int)}
     */
    @Deprecated
    public void makeTerrainQuadTree(int depth) throws FactoryException, TransformException {
        generateTerrainQuadTree(depth);
    }

    public GeoTiffCoverageStore getGaiaGeoTiffManager() {
        if (terrainTilesetGenerator != null) {
            return terrainTilesetGenerator.getGeoTiffCoverageStore();
        }
        if (geoTiffManager == null) {
            geoTiffManager = new GeoTiffCoverageStore();
        }
        return geoTiffManager;
    }

    public GeographicTerrainTileRaster getGeographicTerrainTileRaster(TileIndices tileIndices, TerrainTilesetGenerator terrainTilesetGenerator) {
        long tileKey = tileIndices.toCacheKey();
        GeographicTerrainTileRaster GeographicTerrainTileRaster = mapIndicesTileRaster.get(tileKey);
        if (GeographicTerrainTileRaster == null) {
            GeographicTerrainTileRaster = new GeographicTerrainTileRaster(tileIndices, terrainTilesetGenerator);
            int tileRasterWidth = terrainTilesetGenerator.getRasterTileSize();
            int tileRasterHeight = terrainTilesetGenerator.getRasterTileSize();
            GeographicTerrainTileRaster.generateElevations(this, tileRasterWidth, tileRasterHeight);
            mapIndicesTileRaster.put(tileKey, GeographicTerrainTileRaster);
        }
        return GeographicTerrainTileRaster;
    }

    public void prepareTileRastersForRange(TileRange tileRange, TerrainTilesetGenerator terrainTilesetGenerator) {
        List<TileIndices> tileIndicesList = tileRange.getTileIndices(null);

        // 1rst, delete from the mapIndicesTileRaster the tiles that are not in the tileIndicesList
        List<Long> tileIndicesKeyList = new ArrayList<>(mapIndicesTileRaster.keySet());
        Map<Long, Long> requestedTileKeys = new HashMap<>();
        for (TileIndices tileIndices : tileIndicesList) {
            long tileKey = tileIndices.toCacheKey();
            requestedTileKeys.put(tileKey, tileKey);
        }
        int initialSize = mapIndicesTileRaster.size();
        int reusedRasterTilesCount = 0;
        for (Long tileKey : tileIndicesKeyList) {
            if (requestedTileKeys.containsKey(tileKey)) {
                reusedRasterTilesCount++;
            } else {
                GeographicTerrainTileRaster GeographicTerrainTileRaster = mapIndicesTileRaster.get(tileKey);
                GeographicTerrainTileRaster.deleteObjects();
                mapIndicesTileRaster.remove(tileKey);
            }
        }

        log.debug("ReusedRasterTilesCount = {}", reusedRasterTilesCount + " / " + initialSize);

        // now, delete TerrainElevationData's coverage that are not intersecting with the tileRange
        GeographicExtension geoExtensionTotal = null;
        for (TileIndices tileIndices : tileIndicesList) {
            String imageryType = terrainTilesetGenerator.getImaginaryType();
            boolean originIsLeftUp = terrainTilesetGenerator.isOriginIsLeftUp();
            GeographicExtension geoExtension = GeographicTerrainTileUtils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, imageryType, originIsLeftUp);
            if (geoExtensionTotal == null) {
                geoExtensionTotal = new GeographicExtension();
                geoExtensionTotal.copyFrom(geoExtension);
            } else {
                geoExtensionTotal.union(geoExtension);
            }
        }
        if (geoExtensionTotal != null) {
            this.rootTerrainElevationDataQuadTree.deleteCoverageIfNoIntersectsGeoExtension(geoExtensionTotal);
        }

        for (TileIndices tileIndices : tileIndicesList) {
            long tileKey = tileIndices.toCacheKey();
            GeographicTerrainTileRaster GeographicTerrainTileRaster = mapIndicesTileRaster.get(tileKey);
            if (GeographicTerrainTileRaster == null) {
                GeographicTerrainTileRaster = new GeographicTerrainTileRaster(tileIndices, terrainTilesetGenerator);
                int tileRasterWidth = terrainTilesetGenerator.getRasterTileSize();
                int tileRasterHeight = terrainTilesetGenerator.getRasterTileSize();
                GeographicTerrainTileRaster.generateElevations(this, tileRasterWidth, tileRasterHeight);
                mapIndicesTileRaster.put(tileKey, GeographicTerrainTileRaster);
                enforceLiveRasterBudget(geoExtensionTotal, GeographicTerrainTileRaster.getGeographicExtension());
            }
        }

    }

    /**
     * @deprecated use {@link #prepareTileRastersForRange(TileRange, TerrainTilesetGenerator)}
     */
    @Deprecated
    public void makeAllGeographicTerrainTileRaster(TileRange tileRange, TerrainTilesetGenerator terrainTilesetGenerator) {
        prepareTileRastersForRange(tileRange, terrainTilesetGenerator);
    }

    public void deleteTileRaster() {
        for (GeographicTerrainTileRaster GeographicTerrainTileRaster : mapIndicesTileRaster.values()) {
            GeographicTerrainTileRaster.deleteObjects();
        }

        mapIndicesTileRaster.clear();

    }

    public GeographicExtension getRootGeographicExtension() {
        if (rootTerrainElevationDataQuadTree == null) {
            return null;
        }

        return rootTerrainElevationDataQuadTree.getGeographicExtension();
    }

    public void deleteCoverage() {
        if (rootTerrainElevationDataQuadTree == null) {
            return;
        }
        rootTerrainElevationDataQuadTree.deleteCoverage();
    }

    public void deleteCoverageIfNotIntersects(GeographicExtension geographicExtension) {
        if (rootTerrainElevationDataQuadTree == null) {
            return;
        }
        rootTerrainElevationDataQuadTree.deleteCoverageIfNotIntersects(geographicExtension);
    }

    public void deleteObjects() {
        this.deleteTileRaster();
        this.deleteCoverage();
        if (geoTiffManager != null && terrainTilesetGenerator == null) {
            geoTiffManager.deleteObjects();
        }
        geoTiffManager = null;

        if (rootTerrainElevationDataQuadTree != null) {
            rootTerrainElevationDataQuadTree.deleteObjects();
            rootTerrainElevationDataQuadTree = null;
        }

        terrainElevationDataArray.clear();
        trianglesArray.clear();
        mapIndicesTileRaster.clear();
        gridAreaMap.clear();
        priorityPixelSizeByGeoTiffName.clear();
        geoTiffFileNames.clear();
        terrainElevationDataFiles = new ArrayList<>();
        terrainTilesetGenerator = null;
    }

    public void setTerrainElevationDataFiles(List<File> terrainElevationDataFiles) {
        this.terrainElevationDataFiles = terrainElevationDataFiles == null ? new ArrayList<>() : new ArrayList<>(terrainElevationDataFiles);
    }

    public double sampleBilinearElevation(TileIndices tileIndices, TerrainTilesetGenerator terrainTilesetGenerator, double lonDeg, double latDeg, byte[] intersectionType) {
        double resultElevation = 0.0;
        GeographicTerrainTileRaster GeographicTerrainTileRaster = null;
        GeographicTerrainTileRaster = this.getGeographicTerrainTileRaster(tileIndices, terrainTilesetGenerator);
        resultElevation = GeographicTerrainTileRaster.getElevationBilinear(lonDeg, latDeg);
        return resultElevation;
    }

    /**
     * @deprecated use {@link #sampleBilinearElevation(TileIndices, TerrainTilesetGenerator, double, double, byte[])}
     */
    @Deprecated
    public double getElevationBilinearRasterTile(TileIndices tileIndices, TerrainTilesetGenerator terrainTilesetGenerator, double lonDeg, double latDeg, byte[] intersectionType) {
        return sampleBilinearElevation(tileIndices, terrainTilesetGenerator, lonDeg, latDeg, intersectionType);
    }

    public Map<TerrainElevationData, TerrainElevationData> collectTerrainElevationData(GeographicExtension geoExtension, Map<TerrainElevationData, TerrainElevationData> terrainElevDataMap) {
        if (rootTerrainElevationDataQuadTree == null) {
            return terrainElevDataMap;
        }

        if (terrainElevDataMap == null) {
            terrainElevDataMap = new HashMap<>();
        }

        rootTerrainElevationDataQuadTree.getTerrainElevationDataArray(geoExtension, terrainElevDataMap);
        return terrainElevDataMap;
    }

    public List<TerrainElevationData> collectTerrainElevationData(GeographicExtension geoExtension, List<TerrainElevationData> resultTerrainElevDataArray) {
        if (rootTerrainElevationDataQuadTree == null) {
            return resultTerrainElevDataArray;
        }

        if (resultTerrainElevDataArray == null) {
            resultTerrainElevDataArray = new ArrayList<>();
        } else {
            resultTerrainElevDataArray.clear();
        }

        rootTerrainElevationDataQuadTree.getTerrainElevationDataArray(geoExtension, resultTerrainElevDataArray, nextTerrainElevationDataQueryMark());
        return resultTerrainElevDataArray;
    }

    /**
     * @deprecated use {@link #collectTerrainElevationData(GeographicExtension, Map)}
     */
    @Deprecated
    public Map<TerrainElevationData, TerrainElevationData> getTerrainElevationDataArray(GeographicExtension geoExtension, Map<TerrainElevationData, TerrainElevationData> terrainElevDataMap) {
        return collectTerrainElevationData(geoExtension, terrainElevDataMap);
    }

    /**
     * @deprecated use {@link #collectTerrainElevationData(GeographicExtension, List)}
     */
    @Deprecated
    public List<TerrainElevationData> getTerrainElevationDataArray(GeographicExtension geoExtension, List<TerrainElevationData> resultTerrainElevDataArray) {
        return collectTerrainElevationData(geoExtension, resultTerrainElevDataArray);
    }

    private int nextTerrainElevationDataQueryMark() {
        terrainElevationDataQueryMark++;
        if (terrainElevationDataQueryMark == 0) {
            terrainElevationDataQueryMark = 1;
        }
        return terrainElevationDataQueryMark;
    }

    public double getElevation(double lonDeg, double latDeg, List<TerrainElevationData> terrainElevDataArray) {
        double resultElevation = 0.0;

        if (rootTerrainElevationDataQuadTree == null) {
            return resultElevation;
        }

        PriorityType priorityType = globalOptions.getPriorityType();

        intersects[0] = false;
        if (priorityType == PriorityType.RESOLUTION) {
            for (TerrainElevationData terrainElevationData : terrainElevDataArray) {
                double elevation = terrainElevationData.getElevation(lonDeg, latDeg, intersects);
                if (intersects[0]) {
                    return elevation;
                }
            }
            return 0.0;
        }

        double candidateElevation = 0.0;
        for (TerrainElevationData terrainElevationData : terrainElevDataArray) {
            double elevation = terrainElevationData.getElevation(lonDeg, latDeg, intersects);
            if (!intersects[0]) {
                continue;
            }
            if (elevation > candidateElevation) {
                candidateElevation = elevation;
            }
        }

        resultElevation = candidateElevation;
        return resultElevation;
    }

    private List<File> resolveConfiguredElevationFiles() {
        if (terrainElevationDataFiles != null && !terrainElevationDataFiles.isEmpty()) {
            return terrainElevationDataFiles;
        }

        List<File> result = new ArrayList<>();
        if (terrainElevationDataFolderPath == null || terrainElevationDataFolderPath.isBlank()) {
            return result;
        }

        List<String> geoTiffFilePaths = new ArrayList<>();
        FileUtils.getFilePathsByExtension(terrainElevationDataFolderPath, ".tif", geoTiffFilePaths, true);
        for (String geoTiffFilePath : geoTiffFilePaths) {
            result.add(new File(geoTiffFilePath));
        }
        return result;
    }

    private void loadTerrainElevationData(List<File> geoTiffFiles, List<File> standardizedGeoTiffFiles) throws FactoryException, TransformException {
        geoTiffFileNames.clear();

        Map<String, File> standardizedGeoTiffByName = new HashMap<>();
        for (File standardizedGeoTiffFile : standardizedGeoTiffFiles) {
            standardizedGeoTiffByName.putIfAbsent(standardizedGeoTiffFile.getName(), standardizedGeoTiffFile);
        }

        loadTerrainElevationData(geoTiffFiles, standardizedGeoTiffByName);
    }

    private void loadTerrainElevationData(List<File> geoTiffFiles, Map<String, File> standardizedGeoTiffByName) throws FactoryException, TransformException {
        if (geoTiffManager == null) {
            geoTiffManager = this.getGaiaGeoTiffManager();
        }
        GeometryFactory gf = new GeometryFactory();

        if (rootTerrainElevationDataQuadTree == null) {
            rootTerrainElevationDataQuadTree = new TerrainElevationDataQuadTree(null);
        }

        // now load all geotiff and make geotiff geoExtension data
        //GridCoverage2D gridCoverage2D = null;
        CoordinateReferenceSystem crsTarget = null;
        CoordinateReferenceSystem crsOutput = globalOptions.getOutputCRS();
        MathTransform targetToOutput = null;

        Map<String, String> mapNoUsableGeotiffPaths = this.terrainTilesetGenerator.getMapNoUsableGeotiffPaths();

        for (File currentFolderGeoTiffFile : geoTiffFiles) {
            String geoTiffFileName = currentFolderGeoTiffFile.getName();
            String geoTiffFilePath = currentFolderGeoTiffFile.getAbsolutePath();
            geoTiffFileNames.add(geoTiffFilePath);
            File standardizedGeoTiffFile = standardizedGeoTiffByName.get(geoTiffFileName);
            if (!currentFolderGeoTiffFile.exists()) {
                if (standardizedGeoTiffFile != null) {
                    geoTiffFilePath = standardizedGeoTiffFile.getAbsolutePath();
                }
            }
            String priorityReferenceGeoTiffPath = standardizedGeoTiffFile != null ? standardizedGeoTiffFile.getAbsolutePath() : geoTiffFilePath;

            // check if this geoTiff is usable
            if (mapNoUsableGeotiffPaths.containsKey(geoTiffFilePath)) {
                continue;
            }

            TerrainElevationData terrainElevationData = new TerrainElevationData(this);
            GridCoverage2D gridCoverage2D = geoTiffManager.loadGeoTiffGridCoverage2D(geoTiffFilePath);
            terrainElevationData.setGeotiffFilePath(geoTiffFilePath);
            terrainElevationData.setGeotiffFileName(geoTiffFileName);

            crsTarget = gridCoverage2D.getCoordinateReferenceSystem2D();
            targetToOutput = CRS.findMathTransform(crsTarget, crsOutput, true);

            GaiaGeoTiffUtils.getGeographicExtension(gridCoverage2D, gf, targetToOutput, terrainElevationData.getGeographicExtension());
            Vector2d priorityPixelSize = priorityPixelSizeByGeoTiffName.get(geoTiffFileName);
            if (priorityPixelSize == null) {
                if (priorityReferenceGeoTiffPath.equals(geoTiffFilePath)) {
                    priorityPixelSize = GaiaGeoTiffUtils.getPixelSizeMeters(gridCoverage2D);
                } else {
                    GridCoverage2D priorityReferenceCoverage = geoTiffManager.loadGeoTiffGridCoverage2D(priorityReferenceGeoTiffPath);
                    priorityPixelSize = GaiaGeoTiffUtils.getPixelSizeMeters(priorityReferenceCoverage);
                }
                priorityPixelSizeByGeoTiffName.put(geoTiffFileName, new Vector2d(priorityPixelSize));
            }
            terrainElevationData.setPixelSizeMeters(new Vector2d(priorityPixelSize));

            rootTerrainElevationDataQuadTree.addTerrainElevationData(terrainElevationData);
            // Important: Do not dispose the gridCoverage2D here, because it can be stored in the myGaiaGeoTiffManager cache map.
        }
    }

    public Double getOrCachePixelArea(String fileName, String path) {
        if (gridAreaMap.containsKey(fileName)) {
            return gridAreaMap.get(fileName);
        }

        double pixelArea = 0.0d;
        File standardizationTempPath = new File(globalOptions.getStandardizeTempPath());
        File tempFile = new File(standardizationTempPath, fileName);

        if (tempFile.exists()) {
            try {
                GeoTiffCoverageStore geoTiffCoverageStore = this.getGaiaGeoTiffManager();
                GridCoverage2D coverage = geoTiffCoverageStore.loadGeoTiffGridCoverage2D(tempFile.getAbsolutePath());
                Vector2d originalArea = GaiaGeoTiffUtils.getPixelSizeMeters(coverage);
                pixelArea = originalArea.x * originalArea.y;
            } catch (FactoryException e) {
                log.error("[getPixelArea : FactoryException] Error in getPixelArea", e);
            }
        }
        gridAreaMap.put(fileName, pixelArea);
        return gridAreaMap.get(fileName);
    }

    /**
     * @deprecated use {@link #getOrCachePixelArea(String, String)}
     */
    @Deprecated
    public Double putAndGetGridAreaMap(String fileName, String path) {
        return getOrCachePixelArea(fileName, path);
    }

    public void deleteGeoTiffManager() {
        if (geoTiffManager != null && terrainTilesetGenerator == null) {
            geoTiffManager.clear();
        }
        geoTiffManager = null;
    }

    public void preloadTerrainElevationRasters(List<TerrainElevationData> terrainElevDataArray) {
        if (!PRELOAD_TERRAIN_RASTERS || terrainElevDataArray == null || terrainElevDataArray.isEmpty()) {
            return;
        }

        long preloadBudgetBytes = computePreloadBudgetBytes();
        if (preloadBudgetBytes <= 0L) {
            return;
        }

        long loadedBytes = 0L;
        int loadedCount = 0;
        for (TerrainElevationData terrainElevationData : terrainElevDataArray) {
            if (terrainElevationData.isRasterLoaded()) {
                continue;
            }

            long estimatedBytes = terrainElevationData.estimateRasterBytes();
            if (estimatedBytes > preloadBudgetBytes) {
                continue;
            }

            if (loadedBytes + estimatedBytes > preloadBudgetBytes) {
                break;
            }

            if (terrainElevationData.preloadRaster()) {
                loadedBytes += estimatedBytes;
                loadedCount++;
            }
        }

        if (loadedCount > 0) {
            log.debug("[Raster][Preload] Preloaded {} rasters (~{} MB) for current tile range.", loadedCount, loadedBytes / (1024 * 1024));
        }
    }

    public void releaseTerrainElevationRasters(List<TerrainElevationData> terrainElevDataArray) {
        if (terrainElevDataArray == null || terrainElevDataArray.isEmpty()) {
            return;
        }

        for (TerrainElevationData terrainElevationData : terrainElevDataArray) {
            terrainElevationData.deleteCoverage();
        }
    }

    public void releaseTerrainElevationRastersOutsideGeographicExtension(List<TerrainElevationData> terrainElevDataArray, GeographicExtension retainArea) {
        if (terrainElevDataArray == null || terrainElevDataArray.isEmpty() || retainArea == null) {
            return;
        }

        long liveRasterBudgetBytes = computeLiveRasterBudgetBytes();
        if (liveRasterBudgetBytes <= 0L) {
            return;
        }

        long loadedRasterBytes = estimateLoadedRasterBytes(terrainElevDataArray);
        if (loadedRasterBytes <= liveRasterBudgetBytes) {
            return;
        }

        int releasedCount = 0;
        for (TerrainElevationData terrainElevationData : terrainElevDataArray) {
            if (!retainArea.intersects(terrainElevationData.getGeographicExtension())) {
                terrainElevationData.releaseTileRaster();
                releasedCount++;
            }
        }

        if (releasedCount > 0) {
            log.info("[Raster][Budget] Released {} tile rasters outside active block (~{} MB > budget {} MB).", releasedCount, loadedRasterBytes / (1024 * 1024), liveRasterBudgetBytes / (1024 * 1024));
        }
    }

    private void enforceLiveRasterBudget(GeographicExtension activeArea, GeographicExtension retainArea) {
        if (rootTerrainElevationDataQuadTree == null || activeArea == null || retainArea == null) {
            return;
        }

        long liveRasterBudgetBytes = computeLiveRasterBudgetBytes();
        if (liveRasterBudgetBytes <= 0L) {
            return;
        }

        long loadedRasterBytes = estimateLoadedRasterBytes(activeArea);
        if (loadedRasterBytes <= liveRasterBudgetBytes) {
            return;
        }

        log.warn("[Raster][Budget] Live raster working set {} MB exceeds budget {} MB. Releasing non-intersecting rasters outside current tile window.", loadedRasterBytes / (1024 * 1024), liveRasterBudgetBytes / (1024 * 1024));

        rootTerrainElevationDataQuadTree.deleteCoverageIfNoIntersectsGeoExtension(retainArea);

        long remainingRasterBytes = estimateLoadedRasterBytes(activeArea);
        log.info("[Raster][Budget] Live raster working set reduced to {} MB after trimming.", remainingRasterBytes / (1024 * 1024));
    }

    private long estimateLoadedRasterBytes(GeographicExtension geographicExtension) {
        List<TerrainElevationData> terrainElevDataArray = collectTerrainElevationData(geographicExtension, (List<TerrainElevationData>) null);
        if (terrainElevDataArray == null || terrainElevDataArray.isEmpty()) {
            return 0L;
        }

        long totalBytes = 0L;
        for (TerrainElevationData terrainElevationData : terrainElevDataArray) {
            if (!terrainElevationData.isRasterLoaded()) {
                continue;
            }
            totalBytes += terrainElevationData.estimateRasterBytes();
        }
        return totalBytes;
    }

    private long estimateLoadedRasterBytes(List<TerrainElevationData> terrainElevDataArray) {
        long totalBytes = 0L;
        for (TerrainElevationData terrainElevationData : terrainElevDataArray) {
            if (!terrainElevationData.isRasterLoaded()) {
                continue;
            }
            totalBytes += terrainElevationData.estimateRasterBytes();
        }
        return totalBytes;
    }

    private long computePreloadBudgetBytes() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long freeHeadroom = maxMemory - usedMemory - HEAP_RESERVE_BYTES;
        if (freeHeadroom <= 0L) {
            return 0L;
        }

        long desiredBudget = Math.min(MAX_PRELOAD_BUDGET_BYTES, Math.max(MIN_PRELOAD_BUDGET_BYTES, maxMemory / 8));
        return Math.min(desiredBudget, freeHeadroom);
    }

    private long computeLiveRasterBudgetBytes() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long freeHeadroom = maxMemory - usedMemory - HEAP_RESERVE_BYTES;
        if (freeHeadroom <= 0L) {
            return 0L;
        }

        long desiredBudget = Math.min(MAX_LIVE_RASTER_BUDGET_BYTES, Math.max(MIN_LIVE_RASTER_BUDGET_BYTES, maxMemory / 8));
        return Math.min(desiredBudget, freeHeadroom);
    }
}
