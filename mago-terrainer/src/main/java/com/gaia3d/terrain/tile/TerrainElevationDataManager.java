package com.gaia3d.terrain.tile;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.structure.GeographicExtension;
import com.gaia3d.terrain.structure.TerrainTriangle;
import com.gaia3d.terrain.tile.geotiff.GaiaGeoTiffManager;
import com.gaia3d.terrain.types.PriorityType;
import com.gaia3d.terrain.util.GaiaGeoTiffUtils;
import com.gaia3d.terrain.util.TileWgs84Utils;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class TerrainElevationDataManager {
    private static final boolean PRELOAD_TERRAIN_RASTERS = true;
    private static final long MIN_PRELOAD_BUDGET_BYTES = 1024L * 1024L * 1024L;
    private static final long MAX_PRELOAD_BUDGET_BYTES = 8L * 1024L * 1024L * 1024L;
    private static final long HEAP_RESERVE_BYTES = 512L * 1024L * 1024L;
    private static final long MIN_LIVE_RASTER_BUDGET_BYTES = 2L * 1024L * 1024L * 1024L;
    private static final long MAX_LIVE_RASTER_BUDGET_BYTES = 8L * 1024L * 1024L * 1024L;

    private static GlobalOptions globalOptions = GlobalOptions.getInstance();

    private TileWgs84Manager tileWgs84Manager = null;
    private List<TerrainElevationData> terrainElevationDataArray = new ArrayList<>();
    private List<TerrainTriangle> trianglesArray = new ArrayList<>();
    private Map<Long, TileWgs84Raster> mapIndicesTileRaster = new HashMap<>();
    private Map<String, Double> gridAreaMap = new HashMap<>();
    private Map<String, Vector2d> priorityPixelSizeByGeoTiffName = new HashMap<>();

    // Inside the folder, there are multiple geoTiff files
    private String terrainElevationDataFolderPath;
    private int geoTiffFilesCount = 0;

    // if there are multiple geoTiff files, use this
    private int quadtreeMaxDepth = 10;
    private TerrainElevationDataQuadTree rootTerrainElevationDataQuadTree = null;
    private GaiaGeoTiffManager myGaiaGeoTiffManager = null;
    private boolean[] intersects = {false};
    private List<String> geoTiffFileNames = new ArrayList<>();
    private int terrainElevationDataQueryMark = 1;

    public void makeTerrainQuadTree(int depth) throws FactoryException, TransformException, IOException {
        List<File> standardizedGeoTiffFiles = tileWgs84Manager.getStandardizedGeoTiffFiles();

        // load all geoTiffFiles & make a quadTree
        loadAllGeoTiff(terrainElevationDataFolderPath, standardizedGeoTiffFiles);
        rootTerrainElevationDataQuadTree.makeQuadTree(quadtreeMaxDepth);
    }

    public GaiaGeoTiffManager getGaiaGeoTiffManager() {
        if (tileWgs84Manager != null) {
            return tileWgs84Manager.getGaiaGeoTiffManager();
        }
        if (myGaiaGeoTiffManager == null) {
            myGaiaGeoTiffManager = new GaiaGeoTiffManager();
        }
        return myGaiaGeoTiffManager;
    }

    public TileWgs84Raster getTileWgs84Raster(TileIndices tileIndices, TileWgs84Manager tileWgs84Manager) {
        long tileKey = tileIndices.toCacheKey();
        TileWgs84Raster tileWgs84Raster = mapIndicesTileRaster.get(tileKey);
        if (tileWgs84Raster == null) {
            tileWgs84Raster = new TileWgs84Raster(tileIndices, tileWgs84Manager);
            int tileRasterWidth = tileWgs84Manager.getRasterTileSize();
            int tileRasterHeight = tileWgs84Manager.getRasterTileSize();
            tileWgs84Raster.makeElevations(this, tileRasterWidth, tileRasterHeight);
            mapIndicesTileRaster.put(tileKey, tileWgs84Raster);
        }
        return tileWgs84Raster;
    }

    public void makeAllTileWgs84Raster(TileRange tileRange, TileWgs84Manager tileWgs84Manager) {
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
                TileWgs84Raster tileWgs84Raster = mapIndicesTileRaster.get(tileKey);
                tileWgs84Raster.deleteObjects();
                mapIndicesTileRaster.remove(tileKey);
            }
        }

        log.debug("ReusedRasterTilesCount = {}", reusedRasterTilesCount + " / " + initialSize);

        // now, delete TerrainElevationData's coverage that are not intersecting with the tileRange
        GeographicExtension geoExtensionTotal = null;
        for (TileIndices tileIndices : tileIndicesList) {
            String imageryType = tileWgs84Manager.getImaginaryType();
            boolean originIsLeftUp = tileWgs84Manager.isOriginIsLeftUp();
            GeographicExtension geoExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, imageryType, originIsLeftUp);
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
            TileWgs84Raster tileWgs84Raster = mapIndicesTileRaster.get(tileKey);
            if (tileWgs84Raster == null) {
                tileWgs84Raster = new TileWgs84Raster(tileIndices, tileWgs84Manager);
                int tileRasterWidth = tileWgs84Manager.getRasterTileSize();
                int tileRasterHeight = tileWgs84Manager.getRasterTileSize();
                tileWgs84Raster.makeElevations(this, tileRasterWidth, tileRasterHeight);
                mapIndicesTileRaster.put(tileKey, tileWgs84Raster);
                enforceLiveRasterBudget(geoExtensionTotal, tileWgs84Raster.getGeographicExtension());
            }
        }

    }

    public void deleteTileRaster() {
        for (TileWgs84Raster tileWgs84Raster : mapIndicesTileRaster.values()) {
            tileWgs84Raster.deleteObjects();
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
        if (myGaiaGeoTiffManager != null && tileWgs84Manager == null) {
            myGaiaGeoTiffManager.deleteObjects();
        }
        myGaiaGeoTiffManager = null;

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
        tileWgs84Manager = null;
    }

    public double getElevationBilinearRasterTile(TileIndices tileIndices, TileWgs84Manager tileWgs84Manager,
                                                 double lonDeg, double latDeg, byte[] intersectionType) {
        double resultElevation = 0.0;
        TileWgs84Raster tileWgs84Raster = null;
        tileWgs84Raster = this.getTileWgs84Raster(tileIndices, tileWgs84Manager);
        resultElevation = tileWgs84Raster.getElevationBilinear(lonDeg, latDeg);
        return resultElevation;
    }

    public Map<TerrainElevationData, TerrainElevationData> getTerrainElevationDataArray(GeographicExtension geoExtension,
                                                                                        Map<TerrainElevationData, TerrainElevationData> terrainElevDataMap) {
        if (rootTerrainElevationDataQuadTree == null) {
            return terrainElevDataMap;
        }

        if (terrainElevDataMap == null) {
            terrainElevDataMap = new HashMap<>();
        }

        rootTerrainElevationDataQuadTree.getTerrainElevationDataArray(geoExtension, terrainElevDataMap);
        return terrainElevDataMap;
    }

    public List<TerrainElevationData> getTerrainElevationDataArray(
        GeographicExtension geoExtension,
        List<TerrainElevationData> resultTerrainElevDataArray
    ) {
        if (rootTerrainElevationDataQuadTree == null) {
            return resultTerrainElevDataArray;
        }

        if (resultTerrainElevDataArray == null) {
            resultTerrainElevDataArray = new ArrayList<>();
        } else {
            resultTerrainElevDataArray.clear();
        }

        rootTerrainElevationDataQuadTree.getTerrainElevationDataArray(
            geoExtension,
            resultTerrainElevDataArray,
            nextTerrainElevationDataQueryMark()
        );
        return resultTerrainElevDataArray;
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

    private void loadAllGeoTiff(String terrainElevationDataFolderPath, List<File> standardizedGeoTiffFiles) throws FactoryException, TransformException {
        // recursively load all geoTiff files
        geoTiffFileNames.clear();
        FileUtils.getFileNames(terrainElevationDataFolderPath, ".tif", geoTiffFileNames);

        Map<String, File> standardizedGeoTiffByName = new HashMap<>();
        for (File standardizedGeoTiffFile : standardizedGeoTiffFiles) {
            standardizedGeoTiffByName.putIfAbsent(standardizedGeoTiffFile.getName(), standardizedGeoTiffFile);
        }

        loadAllGeoTiff(terrainElevationDataFolderPath, standardizedGeoTiffByName);
    }

    private void loadAllGeoTiff(String terrainElevationDataFolderPath, Map<String, File> standardizedGeoTiffByName) throws FactoryException, TransformException {
        List<String> currentFolderGeoTiffFileNames = new ArrayList<>();
        FileUtils.getFileNames(terrainElevationDataFolderPath, ".tif", currentFolderGeoTiffFileNames);

        if (myGaiaGeoTiffManager == null) {
            myGaiaGeoTiffManager = this.getGaiaGeoTiffManager();
        }
        GeometryFactory gf = new GeometryFactory();

        if (rootTerrainElevationDataQuadTree == null) {
            rootTerrainElevationDataQuadTree = new TerrainElevationDataQuadTree(null);
        }

        // now load all geotiff and make geotiff geoExtension data
        //GridCoverage2D gridCoverage2D = null;
        String geoTiffFileName = null;
        String geoTiffFilePath = null;
        String priorityReferenceGeoTiffPath = null;

        CoordinateReferenceSystem crsTarget = null;
        CoordinateReferenceSystem crsOutput = globalOptions.getOutputCRS();
        MathTransform targetToOutput = null;

        Map<String, String> mapNoUsableGeotiffPaths = this.tileWgs84Manager.getMapNoUsableGeotiffPaths();

        for (String currentFolderGeoTiffFileName : currentFolderGeoTiffFileNames) {
            geoTiffFileName = currentFolderGeoTiffFileName;
            geoTiffFilePath = terrainElevationDataFolderPath + File.separator + geoTiffFileName;
            File currentFolderGeoTiffFile = new File(geoTiffFilePath);
            File standardizedGeoTiffFile = standardizedGeoTiffByName.get(geoTiffFileName);
            if (!currentFolderGeoTiffFile.exists()) {
                if (standardizedGeoTiffFile != null) {
                    geoTiffFilePath = standardizedGeoTiffFile.getAbsolutePath();
                }
            }
            priorityReferenceGeoTiffPath = standardizedGeoTiffFile != null
                ? standardizedGeoTiffFile.getAbsolutePath()
                : geoTiffFilePath;

            // check if this geoTiff is usable
            if (mapNoUsableGeotiffPaths.containsKey(geoTiffFilePath)) {
                continue;
            }

            TerrainElevationData terrainElevationData = new TerrainElevationData(this);
            GridCoverage2D gridCoverage2D = myGaiaGeoTiffManager.loadGeoTiffGridCoverage2D(geoTiffFilePath);
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
                    GridCoverage2D priorityReferenceCoverage = myGaiaGeoTiffManager.loadGeoTiffGridCoverage2D(priorityReferenceGeoTiffPath);
                    priorityPixelSize = GaiaGeoTiffUtils.getPixelSizeMeters(priorityReferenceCoverage);
                }
                priorityPixelSizeByGeoTiffName.put(geoTiffFileName, new Vector2d(priorityPixelSize));
            }
            terrainElevationData.setPixelSizeMeters(new Vector2d(priorityPixelSize));

            rootTerrainElevationDataQuadTree.addTerrainElevationData(terrainElevationData);
            // Important: Do not dispose the gridCoverage2D here, because it can be stored in the myGaiaGeoTiffManager cache map.
        }

        // now check if exist folders inside the terrainElevationDataFolderPath
        List<String> folderNames = new ArrayList<>();
        FileUtils.getFolderNames(terrainElevationDataFolderPath, folderNames);
        for (String folderName : folderNames) {
            String folderPath = terrainElevationDataFolderPath + File.separator + folderName;
            loadAllGeoTiff(folderPath, standardizedGeoTiffByName);
        }
    }

    public Double putAndGetGridAreaMap(String fileName, String path) {
        if (gridAreaMap.containsKey(fileName)) {
            return gridAreaMap.get(fileName);
        }

        double pixelArea = 0.0d;
        File standardizationTempPath = new File(globalOptions.getStandardizeTempPath());
        File tempFile = new File(standardizationTempPath, fileName);

        if (tempFile.exists()) {
            try {
                GaiaGeoTiffManager gaiaGeoTiffManager = this.getGaiaGeoTiffManager();
                GridCoverage2D coverage = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(tempFile.getAbsolutePath());
                Vector2d originalArea = GaiaGeoTiffUtils.getPixelSizeMeters(coverage);
                pixelArea = originalArea.x * originalArea.y;
            } catch (FactoryException e) {
                log.error("[getPixelArea : FactoryException] Error in getPixelArea", e);
            }
        }
        gridAreaMap.put(fileName, pixelArea);
        return gridAreaMap.get(fileName);
    }

    public void deleteGeoTiffManager() {
        if (myGaiaGeoTiffManager != null && tileWgs84Manager == null) {
            myGaiaGeoTiffManager.clear();
        }
        myGaiaGeoTiffManager = null;
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
            if (loadedCount > 0 && loadedBytes + estimatedBytes > preloadBudgetBytes) {
                break;
            }

            if (terrainElevationData.preloadRaster()) {
                loadedBytes += estimatedBytes;
                loadedCount++;
            }
        }

        if (loadedCount > 0) {
            log.debug("[Raster][Preload] Preloaded {} rasters (~{} MB) for current tile range.",
                loadedCount, loadedBytes / (1024 * 1024));
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

    public void releaseTerrainElevationRastersOutsideGeographicExtension(
        List<TerrainElevationData> terrainElevDataArray,
        GeographicExtension retainArea
    ) {
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
            log.info("[Raster][Budget] Released {} tile rasters outside active block (~{} MB > budget {} MB).",
                releasedCount,
                loadedRasterBytes / (1024 * 1024),
                liveRasterBudgetBytes / (1024 * 1024));
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

        log.warn("[Raster][Budget] Live raster working set {} MB exceeds budget {} MB. Releasing non-intersecting rasters outside current tile window.",
            loadedRasterBytes / (1024 * 1024),
            liveRasterBudgetBytes / (1024 * 1024));

        rootTerrainElevationDataQuadTree.deleteCoverageIfNoIntersectsGeoExtension(retainArea);

        long remainingRasterBytes = estimateLoadedRasterBytes(activeArea);
        log.info("[Raster][Budget] Live raster working set reduced to {} MB after trimming.",
            remainingRasterBytes / (1024 * 1024));
    }

    private long estimateLoadedRasterBytes(GeographicExtension geographicExtension) {
        List<TerrainElevationData> terrainElevDataArray = getTerrainElevationDataArray(geographicExtension, (List<TerrainElevationData>) null);
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

        long desiredBudget = Math.min(MAX_PRELOAD_BUDGET_BYTES, Math.max(MIN_PRELOAD_BUDGET_BYTES, maxMemory / 2));
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

        long desiredBudget = Math.min(MAX_LIVE_RASTER_BUDGET_BYTES, Math.max(MIN_LIVE_RASTER_BUDGET_BYTES, maxMemory / 2));
        return Math.min(desiredBudget, freeHeadroom);
    }
}
