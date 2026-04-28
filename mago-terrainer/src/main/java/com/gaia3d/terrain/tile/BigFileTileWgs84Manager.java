package com.gaia3d.terrain.tile;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.structure.GeographicExtension;
import com.gaia3d.terrain.tile.custom.AvailableTileSet;
import com.gaia3d.terrain.util.GaiaGeoTiffUtils;
import com.gaia3d.terrain.util.TileWgs84Utils;
import com.gaia3d.util.DecimalUtils;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BigFileTileWgs84Manager - 重构版本
 * 采用 Spatial-Trunk-First + In-Memory Pyramid 的极致优化策略
 * 核心目标：消灭大文件切片过程中的中间 .tif 磁盘文件生成，实现全内存金字塔与切片流转。
 */
@Slf4j
public class BigFileTileWgs84Manager {
    private final GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final TileWgs84Manager tileManager = new TileWgs84Manager();
    private final AvailableTileSet availableTileSet = new AvailableTileSet();
    private final TerrainLayer terrainLayer = new TerrainLayer();
    private boolean boundsInitialized = false;

    public void process(List<File> inputFiles) throws Exception {
        // ⚠️ 关键改变：跳过全局 standardization，改为 Trunk 级别处理
        // 原因：全局 standardization 对 60GB 文件会导致 OOM
        // 参考 CesiumLab 的实现：逐 Trunk 加载和处理
        
        log.info("[Pre][Metadata] Start scanning file metadata (without pixel loading).");
        
        // 步骤 1: 仅加载文件元数据（不加载像素数据）
        // 这样即使对 60GB 文件也只需要几 MB 内存
        List<BigFileSource> sources = openSourcesMetadataOnly(inputFiles);
        try {
            // 步骤 2: 扫描元数据并划分 Trunks
            sources = scanSources(sources);
            initializeTileManagerState();

            terrainLayer.getAvailable().clear();
            terrainLayer.getAvailable().addAll(availableTileSet.getAvailableTileRanges());

            int minTileDepth = globalOptions.getMinimumTileDepth();
            int maxTileDepth = globalOptions.getMaximumTileDepth();
            int availableMaxDepth = availableTileSet.getMaxAvailableDepth();
            if (maxTileDepth < 0) {
                maxTileDepth = availableMaxDepth;
            } else if (availableMaxDepth < maxTileDepth) {
                maxTileDepth = availableMaxDepth;
            }
            availableTileSet.deleteTileRangesOverDepth(maxTileDepth);
            globalOptions.setMaximumTileDepth(maxTileDepth);

            // 步骤 3: 逐 Trunk 处理（模仿 CesiumLab 的流水线）
            // 每个 Trunk 会执行：
            //   1. 从源文件局部读取此 Trunk 范围的像素
            //   2. 进行 CRS 转换（仅此 Trunk）
            //   3. 生成金字塔和瓷砖
            //   4. 输出到磁盘
            //   5. 立即释放此 Trunk 的内存
            processSpatialTrunks(sources, minTileDepth, maxTileDepth);

            tileManager.getTerrainWriter().writeMetadata(
                    terrainLayer.getBounds()[0], terrainLayer.getBounds()[1],
                    terrainLayer.getBounds()[2], terrainLayer.getBounds()[3],
                    minTileDepth, maxTileDepth,
                    globalOptions.getOriginalInputPath(), terrainLayer.getJsonString()
            );
            tileManager.getTerrainWriter().close();
        } finally {
            for (BigFileSource source : sources) {
                source.close();
            }
        }
    }

    /**
     * 打开源文件，但仅加载元数据（无像素数据）
     * 这是处理超大文件（60GB+）的关键步骤
     * 模仿 CesiumLab 的"元数据扫描阶段"
     */
    private List<BigFileSource> openSourcesMetadataOnly(List<File> inputFiles) throws Exception {
        List<BigFileSource> sources = new ArrayList<>();
        for (File file : inputFiles) {
            // BigFileElevationProvider 打开文件时会提取元数据
            // 但不会加载像素数据到内存
            BigFileElevationProvider provider = new BigFileElevationProvider(file);
            sources.add(new BigFileSource(file, provider));
        }
        return sources;
    }

    private List<BigFileSource> openSources(List<File> inputFiles) throws Exception {
        List<BigFileSource> sources = new ArrayList<>();
        for (File file : inputFiles) {
            BigFileElevationProvider provider = new BigFileElevationProvider(file);
            sources.add(new BigFileSource(file, provider));
        }
        return sources;
    }

    private List<BigFileSource> scanSources(List<BigFileSource> sources) {
        initializeAvailableTileSet();

        for (BigFileSource source : sources) {
            updateBounds(source.geographicExtension);
            availableTileSet.addAvailableExtensions(source.pixelSizeMeters, source.geographicExtension);
        }

        availableTileSet.recombineTileRanges();
        if (globalOptions.getMaximumTileDepth() < 0) {
            globalOptions.setMaximumTileDepth(availableTileSet.getMaxAvailableDepth());
        }
        return sources;
    }

    private void initializeTileManagerState() {
        copyAvailableTileSet();
        tileManager.getMapNoUsableGeotiffPaths().clear();
        
        // Preserve the bounds that were computed during scanSources
        double[] savedBounds = terrainLayer.getBounds().clone();
        
        terrainLayer.setDefault();
        
        // Restore the bounds
        double[] currentBounds = terrainLayer.getBounds();
        currentBounds[0] = savedBounds[0];
        currentBounds[1] = savedBounds[1];
        currentBounds[2] = savedBounds[2];
        currentBounds[3] = savedBounds[3];
        
        if (globalOptions.isCalculateNormalsExtension()) {
            terrainLayer.addExtension("octvertexnormals");
        }
        if (globalOptions.isWaterMaskExtension()) {
            terrainLayer.addExtension("watermask");
        }
        if (globalOptions.isMetaDataExtension()) {
            terrainLayer.addExtension("metadata");
        }
    }

    private void processSpatialTrunks(List<BigFileSource> sources, int minTileDepth, int maxTileDepth) throws Exception {
        long startTime = System.currentTimeMillis();

        // 1. 获取全区边界和最佳分辨率
        double minPixelSizeDeg = Double.MAX_VALUE;
        for (BigFileSource source : sources) {
            double maxRes = Math.max(source.pixelSizeDegX, source.pixelSizeDegY);
            if (maxRes > 0 && maxRes < minPixelSizeDeg) {
                minPixelSizeDeg = maxRes;
            }
        }
        if (minPixelSizeDeg == Double.MAX_VALUE) minPixelSizeDeg = 0.00001;

        double[] bounds = terrainLayer.getBounds();
        double minLon = bounds[0];
        double minLat = bounds[1];
        double maxLon = bounds[2];
        double maxLat = bounds[3];
        
        double totalSpanLon = maxLon - minLon;
        double totalSpanLat = maxLat - minLat;

        // 2. 借鉴 CesiumLab：将全区划分为固定数量的 Trunk (约 200-300 个)
        // 假设目标是 15x15 或 20x10 的网格
        int xDivisions = (int) Math.ceil(Math.sqrt(250.0 * (totalSpanLon / totalSpanLat)));
        int yDivisions = (int) Math.ceil(250.0 / xDivisions);
        
        double trunkSpanLon = totalSpanLon / xDivisions;
        double trunkSpanLat = totalSpanLat / yDivisions;

        List<GeographicExtension> trunks = new ArrayList<>();
        for (int i = 0; i < yDivisions; i++) {
            for (int j = 0; j < xDivisions; j++) {
                double lonStart = minLon + j * trunkSpanLon;
                double latStart = minLat + i * trunkSpanLat;
                double lonEnd = (j == xDivisions - 1) ? maxLon : lonStart + trunkSpanLon;
                double latEnd = (i == yDivisions - 1) ? maxLat : latStart + trunkSpanLat;

                GeographicExtension ext = new GeographicExtension();
                ext.setDegrees(lonStart, latStart, 0, lonEnd, latEnd, 0);
                trunks.add(ext);
            }
        }

        log.info("[Tile][BigFile] Optimized Trunking (CesiumLab Style). Grid: {}x{}, Total Trunks: {}", xDivisions, yDivisions, trunks.size());
        AtomicInteger counter = new AtomicInteger(0);
        int total = trunks.size();

        for (GeographicExtension trunkExt : trunks) {
            int progress = counter.incrementAndGet();
            
            boolean intersects = false;
            for (BigFileSource source : sources) {
                if (source.geographicExtension.intersects(trunkExt)) {
                    intersects = true;
                    break;
                }
            }
            if (!intersects) continue;

            log.info("----------------------------------------");
            log.info("[Tile][BigFile][Trunk {}/{}] Processing... Lon[{}:{}], Lat[{}:{}]", 
                progress, total, 
                String.format("%.4f", trunkExt.getMinLongitudeDeg()), String.format("%.4f", trunkExt.getMaxLongitudeDeg()), 
                String.format("%.4f", trunkExt.getMinLatitudeDeg()), String.format("%.4f", trunkExt.getMinLatitudeDeg()));

            processTrunk(sources, trunkExt, maxTileDepth, minTileDepth, progress, total);
        }

        long endTime = System.currentTimeMillis();
        log.info("[Tile][BigFile] End processing all Trunks. Duration: {}", DecimalUtils.millisecondToDisplayTime(endTime - startTime));
    }

    private void processTrunk(List<BigFileSource> sources, GeographicExtension trunkExt, int maxDepth, int minDepth, int progress, int total) throws Exception {
        // =====================================
        // 1. 计算最小像素分辨率（用于缓冲扩展）
        // =====================================
        double minPixelSizeDeg = Double.MAX_VALUE;
        for (BigFileSource source : sources) {
            double maxRes = Math.max(source.pixelSizeDegX, source.pixelSizeDegY);
            if (maxRes > 0 && maxRes < minPixelSizeDeg) {
                minPixelSizeDeg = maxRes;
            }
        }
        
        // 添加缓冲以避免接缝裂缝 (Seam Buffer): 在四周加上额外的 2 个像素
        double bufferDeg = minPixelSizeDeg * 2.0;
        
        GeographicExtension trunkExtWithBuffer = new GeographicExtension();
        trunkExtWithBuffer.setDegrees(
                trunkExt.getMinLongitudeDeg() - bufferDeg,
                trunkExt.getMinLatitudeDeg() - bufferDeg,
                trunkExt.getMinAltitude(),
                trunkExt.getMaxLongitudeDeg() + bufferDeg,
                trunkExt.getMaxLatitudeDeg() + bufferDeg,
                trunkExt.getMaxAltitude()
        );

        ReferencedEnvelope envelopeWithBuffer = new ReferencedEnvelope(
                trunkExtWithBuffer.getMinLongitudeDeg(), trunkExtWithBuffer.getMaxLongitudeDeg(),
                trunkExtWithBuffer.getMinLatitudeDeg(), trunkExtWithBuffer.getMaxLatitudeDeg(),
                globalOptions.getOutputCRS()
        );

        // =====================================
        // 2. 加载原始高精度覆盖层到内存（单次读取）
        // =====================================
        List<GridCoverage2D> originalCoverages = new ArrayList<>();
        List<String> sourceNames = new ArrayList<>();
        for (BigFileSource source : sources) {
            if (!source.geographicExtension.intersects(trunkExtWithBuffer)) {
                continue;
            }
            
            log.info("[BigFile][Trunk] Loading high-res area for: {}", source.file.getName());
            source.provider.loadTrunk(envelopeWithBuffer, -1, -1);
            GridCoverage2D coverage = source.provider.getCurrentTrunkCoverage();
            if (coverage != null) {
                originalCoverages.add(coverage);
                sourceNames.add(source.file.getName());
            }
        }

        if (originalCoverages.isEmpty()) {
            return;
        }

        boolean originalModify = globalOptions.isModify();
        globalOptions.setModify(true); // Always use modify mode for trunk processing

        try {
            // =====================================
            // 3. 内层金字塔循环：自顶向下 (maxDepth -> minDepth)
            //    在内存中进行降采样，消灭临时文件生成
            // =====================================
            for (int depth = maxDepth; depth >= minDepth; depth--) {
                TileRange depthRange = getTileRangeForGeographicExtension(trunkExt, depth);
                List<TileRange> availableAtDepth = availableTileSet.getAvailableTileRangesAtDepth(depth);
                if (!hasIntersection(depthRange, availableAtDepth)) {
                    continue;
                }

                log.info("[Tile][BigFile][Trunk] Processing depth {}... Affected Tiles: {}", depth, depthRange.toString());

                // 为当前深度创建新的高程数据管理器
                TerrainElevationDataManager elevationDataManager = new TerrainElevationDataManager();
                tileManager.setTerrainElevationDataManager(elevationDataManager);
                elevationDataManager.setTileWgs84Manager(tileManager);
                
                // 计算当前深度的目标分辨率
                double tileAngleRange = TileWgs84Utils.selectTileAngleRangeByDepth(depth);
                double targetResDeg = tileAngleRange / tileManager.getRasterTileSize();
                
                // 用于跟踪在此深度创建的重采样覆盖层（以便后续释放）
                List<GridCoverage2D> resampledCoveragesForThisDepth = new ArrayList<>();
                
                try {
                    // --------- 步骤 3.1: 针对每个源覆盖层进行内存重采样 ---------
                    for (int i = 0; i < originalCoverages.size(); i++) {
                        GridCoverage2D sourceCov = originalCoverages.get(i);
                        String name = sourceNames.get(i);
                        String virtualPath = "memory://" + progress + "/" + depth + "/" + name;
                        
                        GridCoverage2D currentCov = sourceCov;
                        double sourceResX = GaiaGeoTiffUtils.getPixelSizeDegrees(sourceCov).x;
                        
                        // 仅当目标分辨率明显低于源分辨率时进行重采样（在内存中完成）
                        if (targetResDeg > sourceResX * 1.1) {
                            log.debug("[BigFile][Trunk] Resampling coverage {} for depth {} from {} to {}", 
                                name, depth, sourceResX, targetResDeg);
                            currentCov = GaiaGeoTiffUtils.getResampledGridCoverage2D(sourceCov, targetResDeg);
                            resampledCoveragesForThisDepth.add(currentCov);
                        }
                        
                        // 计算像素大小（米）并将内存覆盖层传给管理器
                        double pixelSizeMeters = TileWgs84Utils.getTileSizeInMetersByDepth(depth) / tileManager.getRasterTileSize();
                        elevationDataManager.loadGridCoverage(currentCov, virtualPath, pixelSizeMeters);
                    }

                    // --------- 步骤 3.2: 构建四叉树结构（内存中）---------
                    elevationDataManager.makeTerrainQuadTree(depth);

                    // --------- 步骤 3.3: 生成瓷砖栅格 (Tiling Generation) ---------
                    tileManager.setTriangleRefinementMaxIterations(TileWgs84Utils.getRefinementIterations(depth));
                    TileRange expandedRange = depthRange.expand1();
                    elevationDataManager.makeAllTileWgs84Raster(expandedRange, tileManager);

                    // --------- 步骤 3.4: 生成网格并落盘 (Mesh Generation & I/O) ---------
                    TileMatrix tileMatrix = new TileMatrix(depthRange, tileManager);
                    tileMatrix.makeMatrixMeshModifyMode(false);
                    
                    tileMatrix.deleteObjects();
                    
                } finally {
                    // --------- 步骤 3.5: 清理内存 ---------
                    // 重要：不能调用 elevationDataManager.deleteObjects()，因为它会销毁 myGaiaGeoTiffManager 里的所有 coverage
                    // 而原始的 originalCoverages 还在里面存着呢！
                    // 我们手动清理当前深度产生的临时资源。
                    elevationDataManager.deleteTileRaster();
                    elevationDataManager.deleteCoverage(); // 清理四叉树里的引用
                    
                    // 销毁并移除 GaiaGeoTiffManager 中属于该深度的 coverage
                    if (elevationDataManager.getMyGaiaGeoTiffManager() != null) {
                        // 我们只 dispose 那些新生成的（resampled）
                        for (GridCoverage2D cov : resampledCoveragesForThisDepth) {
                            cov.dispose(true);
                        }
                        // 然后清空 map 引用即可，不要对 originalCoverages 调用 dispose
                        elevationDataManager.getMyGaiaGeoTiffManager().getMapPathGridCoverage2d().clear();
                        elevationDataManager.getMyGaiaGeoTiffManager().getPathList().clear();
                    }
                    resampledCoveragesForThisDepth.clear();
                }
            }
        } finally {
            globalOptions.setModify(originalModify);
            
            // =====================================
            // 4. Trunk 处理完毕后，显式释放原始高精度覆盖层
            // =====================================
            for (GridCoverage2D cov : originalCoverages) {
                cov.dispose(true);
            }
            System.gc(); // 鼓励内存回收
            log.info("[BigFile][Trunk {}/{}] Finished processing. Memory released.", progress, total);
        }
    }

    private TileRange getTileRangeForGeographicExtension(GeographicExtension ext, int depth) {
        boolean originIsLeftUp = tileManager.isOriginIsLeftUp();
        TileRange range = new TileRange();
        TileWgs84Utils.selectTileIndicesArray(depth, ext.getMinLongitudeDeg(), ext.getMaxLongitudeDeg(), ext.getMinLatitudeDeg(), ext.getMaxLatitudeDeg(), range, originIsLeftUp);
        return range;
    }

    private boolean hasIntersection(TileRange range, List<TileRange> available) {
        if (available == null) return false;
        for (TileRange avail : available) {
            if (range.getMinTileX() <= avail.getMaxTileX() && range.getMaxTileX() >= avail.getMinTileX() &&
                range.getMinTileY() <= avail.getMaxTileY() && range.getMaxTileY() >= avail.getMinTileY()) {
                return true;
            }
        }
        return false;
    }

    private GeographicExtension toGeographicExtension(ReferencedEnvelope envelope) {
        GeographicExtension extension = new GeographicExtension();
        extension.setDegrees(envelope.getMinX(), envelope.getMinY(), 0.0, envelope.getMaxX(), envelope.getMaxY(), 0.0);
        return extension;
    }

    private GeographicExtension getIntersection(GeographicExtension a, GeographicExtension b) {
        if (!a.intersects(b)) {
            return null;
        }
        GeographicExtension result = new GeographicExtension();
        result.setDegrees(
                Math.max(a.getMinLongitudeDeg(), b.getMinLongitudeDeg()),
                Math.max(a.getMinLatitudeDeg(), b.getMinLatitudeDeg()),
                Math.max(a.getMinAltitude(), b.getMinAltitude()),
                Math.min(a.getMaxLongitudeDeg(), b.getMaxLongitudeDeg()),
                Math.min(a.getMaxLatitudeDeg(), b.getMaxLatitudeDeg()),
                Math.min(a.getMaxAltitude(), b.getMaxAltitude())
        );
        return result;
    }

    private void initializeAvailableTileSet() {
        List<TileRange> tileRanges = availableTileSet.getMapDepthAvailableTileRanges()
                .computeIfAbsent(0, k -> new ArrayList<>());
        TileRange worldRange = new TileRange();
        worldRange.set(0, 0, 1, 0, 0);
        tileRanges.add(worldRange);
    }

    private void updateBounds(GeographicExtension geographicExtension) {
        double[] bounds = terrainLayer.getBounds();
        if (!boundsInitialized) {
            bounds[0] = geographicExtension.getMinLongitudeDeg();
            bounds[1] = geographicExtension.getMinLatitudeDeg();
            bounds[2] = geographicExtension.getMaxLongitudeDeg();
            bounds[3] = geographicExtension.getMaxLatitudeDeg();
            boundsInitialized = true;
            return;
        }

        bounds[0] = Math.min(bounds[0], geographicExtension.getMinLongitudeDeg());
        bounds[1] = Math.min(bounds[1], geographicExtension.getMinLatitudeDeg());
        bounds[2] = Math.max(bounds[2], geographicExtension.getMaxLongitudeDeg());
        bounds[3] = Math.max(bounds[3], geographicExtension.getMaxLatitudeDeg());
    }

    private void copyAvailableTileSet() {
        Map<Integer, List<TileRange>> targetMap = tileManager.getAvailableTileSet().getMapDepthAvailableTileRanges();
        targetMap.clear();
        for (Map.Entry<Integer, List<TileRange>> entry : availableTileSet.getMapDepthAvailableTileRanges().entrySet()) {
            List<TileRange> clonedRanges = new ArrayList<>();
            for (TileRange range : entry.getValue()) {
                clonedRanges.add(range.clone());
            }
            targetMap.put(entry.getKey(), clonedRanges);
        }
    }

    private String sanitizeName(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static class BigFileSource {
        private final File file;
        private final GeographicExtension geographicExtension;
        private final double pixelSizeMeters;
        private final double pixelSizeDegX;
        private final double pixelSizeDegY;
        private final BigFileElevationProvider provider;

        private BigFileSource(File file, BigFileElevationProvider provider) {
            this.file = file;
            this.provider = provider;
            this.geographicExtension = new GeographicExtension();
            this.geographicExtension.copyFrom(provider.getFileExtent());
            this.pixelSizeMeters = provider.getPixelSizeMeters();
            
            org.geotools.api.coverage.grid.GridEnvelope gridRange = provider.getOriginalGridGeometry().getGridRange();
            int width = gridRange.getSpan(0);
            int height = gridRange.getSpan(1);
            this.pixelSizeDegX = this.geographicExtension.getLongitudeRangeDegree() / width;
            this.pixelSizeDegY = this.geographicExtension.getLatitudeRangeDegree() / height;
        }

        private void close() {
            provider.close();
        }
    }
}
