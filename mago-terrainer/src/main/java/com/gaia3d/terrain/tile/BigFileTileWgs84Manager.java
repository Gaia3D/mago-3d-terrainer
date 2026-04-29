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
    private static final int MACRO_LEVEL_THRESHOLD = 4;
    private static final int MEMORY_SAFE_PIXEL_LIMIT = 10_000;
    private static final int SOURCE_IO_PIXEL_LIMIT = 50_000;
    private static final int TARGET_TRUNK_TILES_PER_AXIS = 40;
    private static final int MAX_TRUNK_COUNT = 256;
    private static final long LARGE_TIFF_BYTES_THRESHOLD = 8L * 1024L * 1024L * 1024L; // 8GB
    private static final double[] LEVEL_RESOLUTION_DEGREES = {
            2.8125,
            1.40625,
            0.703125,
            0.351562,
            0.175781,
            0.0878906,
            0.0439453,
            0.0219727,
            0.0109863,
            0.00549316,
            0.00274658,
            0.00137329,
            0.000686646,
            0.000343323,
            0.000171661,
            8.58307e-05,
            4.29153e-05,
            2.14577e-05,
            1.07288e-05
    };
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
            }
            extendAvailableTileRangesToDepth(sources, maxTileDepth);
            globalOptions.setMaximumTileDepth(maxTileDepth);

            // 步骤 3: 逐 Trunk 处理（模仿 CesiumLab 的流水线）
            // 每个 Trunk 会执行：
            //   1. 从源文件局部读取此 Trunk 范围的像素
            //   2. 进行 CRS 转换（仅此 Trunk）
            //   3. 生成金字塔和瓷砖
            //   4. 输出到磁盘
            //   5. 立即释放此 Trunk 的内存
            processTwoTierPipeline(sources, minTileDepth, maxTileDepth);

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

    private void extendAvailableTileRangesToDepth(List<BigFileSource> sources, int maxDepth) {
        if (maxDepth < 0) {
            return;
        }

        for (BigFileSource source : sources) {
            addAvailableRangesForDepths(source.geographicExtension, 0, maxDepth);
        }
        availableTileSet.recombineTileRanges();
    }

    private void addAvailableRangesForDepths(GeographicExtension extension, int minDepth, int maxDepth) {
        for (int depth = minDepth; depth <= maxDepth; depth++) {
            List<TileRange> tileRanges = availableTileSet.getMapDepthAvailableTileRanges()
                    .computeIfAbsent(depth, key -> new ArrayList<>());
            TileRange range = getTileRangeForGeographicExtension(extension, depth).expand(1);
            tileRanges.add(range);
        }
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

    private void processTwoTierPipeline(List<BigFileSource> sources, int minTileDepth, int maxTileDepth) throws Exception {
        long startTime = System.currentTimeMillis();

        int macroMinDepth = Math.min(minTileDepth, MACRO_LEVEL_THRESHOLD);
        int macroMaxDepth = Math.min(maxTileDepth, MACRO_LEVEL_THRESHOLD);
        if (macroMinDepth <= macroMaxDepth) {
            processMacroLevels(sources, macroMinDepth, macroMaxDepth);
        }

        int microMinDepth = Math.max(minTileDepth, MACRO_LEVEL_THRESHOLD + 1);
        if (microMinDepth <= maxTileDepth) {
            processMicroLevels(sources, microMinDepth, maxTileDepth);
        }

        long endTime = System.currentTimeMillis();
        log.info("[Tile][BigFile] End processing all Trunks. Duration: {}", DecimalUtils.millisecondToDisplayTime(endTime - startTime));
    }

    private void processMacroLevels(List<BigFileSource> sources, int minTileDepth, int maxTileDepth) throws Exception {
        BigFileSource macroSource = resolveRequiredMacroSource();
        List<BigFileSource> macroSources = Collections.singletonList(macroSource);

        GeographicExtension worldExtension = new GeographicExtension();
        worldExtension.copyFrom(macroSource.geographicExtension);
        log.info("[Tile][Macro] Use globe raster directly for depths {}-{}: {}", minTileDepth, maxTileDepth, macroSource.file.getAbsolutePath());

        try {
            processTrunk(macroSources, worldExtension, maxTileDepth, minTileDepth, 1, 1);
        } finally {
            macroSource.close();
        }
    }

    private void processMicroLevels(List<BigFileSource> sources, int minTileDepth, int maxTileDepth) throws Exception {
        List<AlignedTrunk> trunks = calculateAlignedTrunks(maxDepthForTrunking(maxTileDepth), sources);
        log.info("[Tile][BigFile] Aligned trunking for depth {}. Total trunks: {}", maxTileDepth, trunks.size());

        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger processedCounter = new AtomicInteger(0);
        int total = trunks.size();
        for (AlignedTrunk trunk : trunks) {
            int progress = counter.incrementAndGet();
            log.info("----------------------------------------");
            log.info("[Tile][BigFile][Trunk {}/{}] Processing... Lon[{}:{}], Lat[{}:{}], TileRange={}",
                    progress, total,
                    String.format("%.4f", trunk.geographicExtension.getMinLongitudeDeg()),
                    String.format("%.4f", trunk.geographicExtension.getMaxLongitudeDeg()),
                    String.format("%.4f", trunk.geographicExtension.getMinLatitudeDeg()),
                    String.format("%.4f", trunk.geographicExtension.getMaxLatitudeDeg()),
                    trunk.tileRange);

            processTrunk(sources, trunk.geographicExtension, maxTileDepth, minTileDepth, progress, total);
            processedCounter.incrementAndGet();
        }

        log.info("[Tile][BigFile] Micro phase finished. Processed trunks: {} / {}", processedCounter.get(), total);
    }

    private int maxDepthForTrunking(int maxTileDepth) {
        return Math.min(LEVEL_RESOLUTION_DEGREES.length - 1, Math.max(0, maxTileDepth));
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
                double targetResDeg = getLevelResolutionDegrees(depth);
                
                // 用于跟踪当前深度创建的临时内存 coverage（重采样/物化）以便及时释放
                List<GridCoverage2D> transientCoveragesForThisDepth = new ArrayList<>();
                
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
                            transientCoveragesForThisDepth.add(currentCov);
                        }

                        // 将 coverage 预先物化为稳定的内存 raster，避免后续采样阶段触发 JAI 懒执行链异常。
                        GridCoverage2D materializedCov = GaiaGeoTiffUtils.materializeGridCoverage2D(currentCov);
                        if (materializedCov != currentCov) {
                            transientCoveragesForThisDepth.add(materializedCov);
                            currentCov = materializedCov;
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
                        // 我们只 dispose 当前深度新生成的内存 coverage，原始 trunk coverage 留到 trunk 结束后统一释放。
                        for (GridCoverage2D cov : transientCoveragesForThisDepth) {
                            cov.dispose(true);
                        }
                        // 然后清空 map 引用即可，不要对 originalCoverages 调用 dispose
                        elevationDataManager.getMyGaiaGeoTiffManager().getMapPathGridCoverage2d().clear();
                        elevationDataManager.getMyGaiaGeoTiffManager().getPathList().clear();
                    }
                    transientCoveragesForThisDepth.clear();
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

    public List<AlignedTrunk> calculateAlignedTrunks(int maxDepth, List<BigFileSource> sources) {
        double[] bounds = terrainLayer.getBounds();
        GeographicExtension fullExtension = new GeographicExtension();
        fullExtension.setDegrees(bounds[0], bounds[1], 0.0, bounds[2], bounds[3], 0.0);

        TileRange fullRange = getTileRangeForGeographicExtension(fullExtension, maxDepth);
        double resolution = getLevelResolutionDegrees(maxDepth);
        double totalPixelWidth = Math.max(1.0, Math.ceil((bounds[2] - bounds[0]) / resolution));
        double totalPixelHeight = Math.max(1.0, Math.ceil((bounds[3] - bounds[1]) / resolution));

        int xTileCount = fullRange.getMaxTileX() - fullRange.getMinTileX() + 1;
        int yTileCount = fullRange.getMaxTileY() - fullRange.getMinTileY() + 1;
        int xDivisionsByPixels = Math.max(1, (int) Math.ceil(totalPixelWidth / MEMORY_SAFE_PIXEL_LIMIT));
        int yDivisionsByPixels = Math.max(1, (int) Math.ceil(totalPixelHeight / MEMORY_SAFE_PIXEL_LIMIT));
        int xDivisionsByTiles = Math.max(1, (int) Math.ceil((double) xTileCount / TARGET_TRUNK_TILES_PER_AXIS));
        int yDivisionsByTiles = Math.max(1, (int) Math.ceil((double) yTileCount / TARGET_TRUNK_TILES_PER_AXIS));
        SourceGridConstraint sourceGridConstraint = calculateSourceGridConstraint(bounds, sources);
        int xDivisions = Math.max(Math.max(xDivisionsByPixels, xDivisionsByTiles), sourceGridConstraint.xDivisions);
        int yDivisions = Math.max(Math.max(yDivisionsByPixels, yDivisionsByTiles), sourceGridConstraint.yDivisions);
        FileSizeConstraint fileSizeConstraint = calculateFileSizeConstraint(sources, xDivisions, yDivisions);
        xDivisions = Math.max(xDivisions, fileSizeConstraint.xDivisions);
        yDivisions = Math.max(yDivisions, fileSizeConstraint.yDivisions);
        TrunkGrid cappedGrid = applyTrunkCountCap(xDivisions, yDivisions);
        xDivisions = cappedGrid.xDivisions;
        yDivisions = cappedGrid.yDivisions;

        log.info("[Tile][BigFile] calculateAlignedTrunks depth={}, resolution={}, totalPixels={}x{}, tiles={}x{}, memoryLimit={}, targetTilesPerAxis={}, grid={}x{} (pixelGrid={}x{}, tileGrid={}x{}, sourceGrid={}x{}, fileGrid={}x{}, capped={})",
                maxDepth,
                resolution,
                (long) totalPixelWidth, (long) totalPixelHeight,
                xTileCount, yTileCount,
                MEMORY_SAFE_PIXEL_LIMIT,
                TARGET_TRUNK_TILES_PER_AXIS,
                xDivisions, yDivisions,
                xDivisionsByPixels, yDivisionsByPixels,
                xDivisionsByTiles, yDivisionsByTiles,
                sourceGridConstraint.xDivisions, sourceGridConstraint.yDivisions,
                fileSizeConstraint.xDivisions, fileSizeConstraint.yDivisions,
                cappedGrid.capped);

        List<AlignedTrunk> trunks = new ArrayList<>();
        for (int yIndex = 0; yIndex < yDivisions; yIndex++) {
            int yStartOffset = (int) Math.floor((double) yIndex * yTileCount / yDivisions);
            int yEndOffset = (int) Math.floor((double) (yIndex + 1) * yTileCount / yDivisions) - 1;
            int minTileY = fullRange.getMinTileY() + yStartOffset;
            int maxTileY = fullRange.getMinTileY() + Math.max(yStartOffset, yEndOffset);

            for (int xIndex = 0; xIndex < xDivisions; xIndex++) {
                int xStartOffset = (int) Math.floor((double) xIndex * xTileCount / xDivisions);
                int xEndOffset = (int) Math.floor((double) (xIndex + 1) * xTileCount / xDivisions) - 1;
                int minTileX = fullRange.getMinTileX() + xStartOffset;
                int maxTileX = fullRange.getMinTileX() + Math.max(xStartOffset, xEndOffset);

                TileRange tileRange = new TileRange();
                tileRange.set(maxDepth, minTileX, maxTileX, minTileY, maxTileY);
                GeographicExtension trunkExtension = getGeographicExtensionFromTileRange(tileRange);
                trunks.add(new AlignedTrunk(tileRange, trunkExtension));
            }
        }

        return trunks;
    }

    private SourceGridConstraint calculateSourceGridConstraint(double[] bounds, List<BigFileSource> sources) {
        double minSourcePixelDeg = Double.MAX_VALUE;
        for (BigFileSource source : sources) {
            double sourceDeg = Math.max(source.pixelSizeDegX, source.pixelSizeDegY);
            if (sourceDeg > 0 && sourceDeg < minSourcePixelDeg) {
                minSourcePixelDeg = sourceDeg;
            }
        }

        if (minSourcePixelDeg == Double.MAX_VALUE) {
            return new SourceGridConstraint(1, 1);
        }

        double sourcePixelWidth = Math.max(1.0, Math.ceil((bounds[2] - bounds[0]) / minSourcePixelDeg));
        double sourcePixelHeight = Math.max(1.0, Math.ceil((bounds[3] - bounds[1]) / minSourcePixelDeg));
        int xDivisions = Math.max(1, (int) Math.ceil(sourcePixelWidth / SOURCE_IO_PIXEL_LIMIT));
        int yDivisions = Math.max(1, (int) Math.ceil(sourcePixelHeight / SOURCE_IO_PIXEL_LIMIT));
        // Avoid source-resolution-only over-splitting for low target levels.
        xDivisions = Math.min(xDivisions, 16);
        yDivisions = Math.min(yDivisions, 16);
        return new SourceGridConstraint(xDivisions, yDivisions);
    }

    private FileSizeConstraint calculateFileSizeConstraint(List<BigFileSource> sources, int currentXDivisions, int currentYDivisions) {
        long totalBytes = 0L;
        for (BigFileSource source : sources) {
            totalBytes += Math.max(0L, source.file.length());
        }

        if (totalBytes < LARGE_TIFF_BYTES_THRESHOLD || currentXDivisions * currentYDivisions > 1) {
            return new FileSizeConstraint(1, 1);
        }

        // For large compressed TIFF where one trunk causes huge decode latency, force at least 2x2 windows.
        return new FileSizeConstraint(2, 2);
    }

    private TrunkGrid applyTrunkCountCap(int xDivisions, int yDivisions) {
        long total = (long) xDivisions * (long) yDivisions;
        if (total <= MAX_TRUNK_COUNT) {
            return new TrunkGrid(xDivisions, yDivisions, false);
        }

        double scale = Math.sqrt((double) total / MAX_TRUNK_COUNT);
        int reducedX = Math.max(1, (int) Math.ceil(xDivisions / scale));
        int reducedY = Math.max(1, (int) Math.ceil(yDivisions / scale));

        while ((long) reducedX * (long) reducedY > MAX_TRUNK_COUNT) {
            if (reducedX >= reducedY && reducedX > 1) {
                reducedX--;
            } else if (reducedY > 1) {
                reducedY--;
            } else {
                break;
            }
        }

        return new TrunkGrid(reducedX, reducedY, true);
    }

    private double getLevelResolutionDegrees(int depth) {
        if (depth >= 0 && depth < LEVEL_RESOLUTION_DEGREES.length) {
            return LEVEL_RESOLUTION_DEGREES[depth];
        }

        if (depth < 0) {
            throw new IllegalArgumentException("Depth must be >= 0");
        }

        double baseResolution = LEVEL_RESOLUTION_DEGREES[LEVEL_RESOLUTION_DEGREES.length - 1];
        int extraLevels = depth - (LEVEL_RESOLUTION_DEGREES.length - 1);
        return baseResolution / Math.pow(2.0, extraLevels);
    }

    private GeographicExtension getGeographicExtensionFromTileRange(TileRange tileRange) {
        boolean originIsLeftUp = tileManager.isOriginIsLeftUp();
        String imageryType = tileManager.getImaginaryType();
        GeographicExtension minTileExtent = TileWgs84Utils.getGeographicExtentOfTileLXY(
                tileRange.getTileDepth(), tileRange.getMinTileX(), tileRange.getMinTileY(), null, imageryType, originIsLeftUp);
        GeographicExtension maxTileExtent = TileWgs84Utils.getGeographicExtentOfTileLXY(
                tileRange.getTileDepth(), tileRange.getMaxTileX(), tileRange.getMaxTileY(), null, imageryType, originIsLeftUp);

        GeographicExtension extension = new GeographicExtension();
        extension.setDegrees(
                minTileExtent.getMinLongitudeDeg(),
                Math.min(minTileExtent.getMinLatitudeDeg(), maxTileExtent.getMinLatitudeDeg()),
                0.0,
                maxTileExtent.getMaxLongitudeDeg(),
                Math.max(minTileExtent.getMaxLatitudeDeg(), maxTileExtent.getMaxLatitudeDeg()),
                0.0
        );
        return extension;
    }

    private BigFileSource resolveRequiredMacroSource() throws Exception {
        String globeTiffPath = globalOptions.getGlobeTiffPath();
        if (globeTiffPath == null || globeTiffPath.isBlank()) {
            throw new IllegalStateException("Macro phase requires --globeTiffPath during testing.");
        }

        File globeTiffFile = new File(globeTiffPath);
        if (!globeTiffFile.exists() || !globeTiffFile.isFile()) {
            throw new IllegalStateException("Configured globe.tif path does not exist: " + globeTiffPath);
        }

        return new BigFileSource(globeTiffFile, new BigFileElevationProvider(globeTiffFile));
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

    public static class AlignedTrunk {
        private final TileRange tileRange;
        private final GeographicExtension geographicExtension;

        public AlignedTrunk(TileRange tileRange, GeographicExtension geographicExtension) {
            this.tileRange = tileRange;
            this.geographicExtension = geographicExtension;
        }
    }

    private record SourceGridConstraint(int xDivisions, int yDivisions) {
    }

    private record FileSizeConstraint(int xDivisions, int yDivisions) {
    }

    private record TrunkGrid(int xDivisions, int yDivisions, boolean capped) {
    }
}
