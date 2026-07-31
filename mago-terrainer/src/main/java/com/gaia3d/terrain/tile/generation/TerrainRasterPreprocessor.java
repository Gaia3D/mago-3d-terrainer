package com.gaia3d.terrain.tile.generation;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.tile.geotiff.GeoTiffCoverageStore;
import com.gaia3d.terrain.tile.geotiff.RasterStandardizer;
import com.gaia3d.terrain.tile.raster.TerrainRasterFormat;
import com.gaia3d.terrain.tile.raster.TerrainRasterData;
import com.gaia3d.terrain.tile.raster.TerrainRasterReader;
import com.gaia3d.terrain.tile.raster.TerrainRasterResizer;
import com.gaia3d.terrain.tile.raster.TerrainRasterWriter;
import com.gaia3d.terrain.util.GaiaGeoTiffUtils;
import com.gaia3d.util.FileUtils;
import com.gaia3d.util.GlobeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.referencing.FactoryException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.joml.Vector2d;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class TerrainRasterPreprocessor {
    private final GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final TerrainTilesetGenerator manager;

    public void standardizeInputRasters() {
        List<String> rasterFileNames = manager.resolveInputRasterFileNames();
        log.info("[Pre][Standardization] Found Total {} GeoTiff files in input paths", rasterFileNames.size());

        if (rasterFileNames.isEmpty()) {
            throw new RuntimeException("No GeoTiff files found in input paths: " + resolveConfiguredInputPaths());
        }

        standardizeRasterFiles(rasterFileNames);

        File tempFolder = new File(globalOptions.getStandardizeTempPath());
        List<String> standardizedRasterPaths = new ArrayList<>();
        FileUtils.getFilePathsByExtension(tempFolder.getAbsolutePath(), TerrainRasterFormat.EXTENSION, standardizedRasterPaths, true);
        if (standardizedRasterPaths.isEmpty()) {
            throw new RuntimeException("No standardized terrain raster files found in: " + tempFolder.getAbsolutePath());
        }

        manager.getStandardizedGeoTiffFiles().clear();
        for (String standardizedRasterPath : standardizedRasterPaths) {
            manager.getStandardizedGeoTiffFiles().add(new File(standardizedRasterPath));
        }
    }

    public void loadPreparedRasters() {
        loadExistingStandardizedRasters();
        loadExistingResizedRasters();
    }

    public void standardizeRasterFiles(List<String> geoTiffFileNames) {
        String tempPath = globalOptions.getStandardizeTempPath();
        File tempFolder = new File(tempPath);
        if (!tempFolder.exists() && tempFolder.mkdirs()) {
            log.debug("Created standardization folder: {}", tempFolder.getAbsolutePath());
        }
        globalOptions.setInputPath(tempFolder.getAbsolutePath());
        double maxUsefulPixelSizeMeters = getMaxUsefulPixelSizeMeters();

        String geoidPath = globalOptions.getGeoidPath();
        boolean hasGeoid = geoidPath != null && !geoidPath.isEmpty();

        if (hasGeoid) {
            File geoidFile = new File(geoidPath);
            geoTiffFileNames.forEach(geoTiffFileName -> {
                GridCoverage2D originalGridCoverage2D = getGeoTiffCoverageStore().loadGeoTiffGridCoverage2D(geoTiffFileName);
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
                GridCoverage2D originalGridCoverage2D = getGeoTiffCoverageStore().loadGeoTiffGridCoverage2D(geoTiffFileName);
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

    public void prepareDepthRasters(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException {
        File terrainElevationDataFolder = new File(terrainElevationDataFolderPath);
        if (!terrainElevationDataFolder.exists()) {
            log.error("terrainElevationDataFolder is not exist: " + terrainElevationDataFolderPath);
            throw new RuntimeException("Error: terrainElevationDataFolder is not exist: " + terrainElevationDataFolderPath);
        } else if (!terrainElevationDataFolder.isDirectory()) {
            log.error("terrainElevationDataFolder is not a directory: " + terrainElevationDataFolderPath);
            throw new RuntimeException("Error: terrainElevationDataFolder is not a directory: " + terrainElevationDataFolderPath);
        }

        List<String> rasterFilePaths = new ArrayList<>();
        FileUtils.getFilePathsByExtension(terrainElevationDataFolderPath, TerrainRasterFormat.EXTENSION, rasterFilePaths, true);

        manager.setGeoTiffFilesCount(rasterFilePaths.size());

        log.info("[Pre][Resize] Resizing terrain rasters count: {}", rasterFilePaths.size());
        resizeRastersByDepth(terrainElevationDataFolderPath, currentFolderPath);
    }

    public void resizeRastersByDepth(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException {
        List<String> rasterFileNames = new ArrayList<>();
        FileUtils.getFileNames(terrainElevationDataFolderPath, TerrainRasterFormat.EXTENSION, rasterFileNames);

        if (currentFolderPath == null) {
            currentFolderPath = "";
        }

        int rasterFilesCount = 0;

        for (String rasterFileName : rasterFileNames) {
            log.info("[Pre][Resize][{}/{}] Resizing terrain raster: {}", ++rasterFilesCount, rasterFileNames.size(), rasterFileName);
            File sourceFile = new File(terrainElevationDataFolderPath, rasterFileName);
            String rasterFilePath = sourceFile.getAbsolutePath();

            if (manager.getMapNoUsableGeotiffPaths().containsKey(rasterFilePath)) {
                continue;
            }

            TerrainRasterData source = new TerrainRasterReader().read(sourceFile.toPath());
            Vector2d pixelSizeMeters = getPixelSizeMeters(source);

            int minTileDepth = globalOptions.getMinimumTileDepth();
            int maxTileDepth = globalOptions.getMaximumTileDepth();
            for (int depth = minTileDepth; depth <= maxTileDepth; depth += 1) {
                double desiredPixelSizeXinMeters = manager.getDepthDesiredPixelSizeXinMetersMap().get(depth);
                double desiredPixelSizeYinMeters = desiredPixelSizeXinMeters;

                if (desiredPixelSizeXinMeters < pixelSizeMeters.x) {
                    addDepthGeoTiffFile(depth, sourceFile);
                    continue;
                }

                String depthStr = String.valueOf(depth);
                String resizedFolderPath = resolveResizedGeoTiffFolderPath(depthStr, currentFolderPath, rasterFilePath);
                File resizedFile = new File(resizedFolderPath, rasterFileName);

                if (resizedFile.isFile()) {
                    addDepthGeoTiffFile(depth, resizedFile);
                    continue;
                }

                int targetWidth = Math.max((int) ((pixelSizeMeters.x * source.width()) / desiredPixelSizeXinMeters), 24);
                int targetHeight = Math.max((int) ((pixelSizeMeters.y * source.height()) / desiredPixelSizeYinMeters), 24);
                TerrainRasterData resized = new TerrainRasterResizer().resize(source, targetWidth, targetHeight);
                new TerrainRasterWriter().write(resizedFile.toPath(), resized);
                addDepthGeoTiffFile(depth, resizedFile);
            }
        }

        List<String> folderNames = new ArrayList<>();
        FileUtils.getFolderNames(terrainElevationDataFolderPath, folderNames);
        for (String folderName : folderNames) {
            String auxFolderPath = currentFolderPath + File.separator + folderName;
            String folderPath = terrainElevationDataFolderPath + File.separator + folderName;
            resizeRastersByDepth(folderPath, auxFolderPath);
        }

        System.gc();
    }

    public List<File> resolveRasterFilesForDepth(int depth) {
        List<File> geoTiffFiles = manager.getDepthGeoTiffFilesMap().get(depth);
        if (geoTiffFiles != null && !geoTiffFiles.isEmpty()) {
            return geoTiffFiles;
        }

        for (int fallbackDepth = depth - 1; fallbackDepth >= 0; fallbackDepth--) {
            geoTiffFiles = manager.getDepthGeoTiffFilesMap().get(fallbackDepth);
            if (geoTiffFiles != null && !geoTiffFiles.isEmpty()) {
                log.warn("[Raster][DepthPath] Missing raster files for depth {}. Reusing depth {} files: {}", depth, fallbackDepth, geoTiffFiles.size());
                return geoTiffFiles;
            }
        }

        throw new IllegalStateException("No terrain raster files are available for depth " + depth);
    }

    private void loadExistingStandardizedRasters() {
        File standardizeFolder = new File(globalOptions.getStandardizeTempPath());
        if (!standardizeFolder.exists() || !standardizeFolder.isDirectory()) {
            throw new RuntimeException("Error: Standardization temp path does not exist: " + standardizeFolder.getAbsolutePath());
        }

        List<String> standardizedRasterPaths = new ArrayList<>();
        FileUtils.getFilePathsByExtension(standardizeFolder.getAbsolutePath(), TerrainRasterFormat.EXTENSION, standardizedRasterPaths, true);
        if (standardizedRasterPaths.isEmpty()) {
            throw new RuntimeException("Error: No standardized terrain raster files found in: " + standardizeFolder.getAbsolutePath());
        }

        manager.getStandardizedGeoTiffFiles().clear();
        for (String standardizedRasterPath : standardizedRasterPaths) {
            manager.getStandardizedGeoTiffFiles().add(new File(standardizedRasterPath));
        }
    }

    private void loadExistingResizedRasters() {
        File resizedRoot = new File(globalOptions.getResizedTiffTempPath());
        if (!resizedRoot.exists() || !resizedRoot.isDirectory()) {
            throw new RuntimeException("Error: Resized GeoTiff temp path does not exist: " + resizedRoot.getAbsolutePath());
        }

        manager.getDepthGeoTiffFilesMap().clear();
        int minTileDepth = globalOptions.getMinimumTileDepth();
        int maxTileDepth = globalOptions.getMaximumTileDepth();
        for (int depth = minTileDepth; depth <= maxTileDepth; depth++) {
            File depthFolder = new File(resizedRoot, String.valueOf(depth));
            List<File> depthGeoTiffFiles = resolveExistingDepthGeoTiffFiles(depthFolder);
            if (!depthGeoTiffFiles.isEmpty()) {
                manager.getDepthGeoTiffFilesMap().put(depth, depthGeoTiffFiles);
            }
        }

        if (!manager.getDepthGeoTiffFilesMap().containsKey(minTileDepth)) {
            throw new RuntimeException("Error: No resized GeoTiff files found for minimum depth " + minTileDepth + " in " + resizedRoot.getAbsolutePath());
        }
    }

    private void addDepthGeoTiffFile(int depth, File geoTiffFile) {
        List<File> geoTiffFiles = manager.getDepthGeoTiffFilesMap().computeIfAbsent(depth, k -> new ArrayList<>());
        String absolutePath = geoTiffFile.getAbsolutePath();
        for (File existingFile : geoTiffFiles) {
            if (existingFile.getAbsolutePath().equals(absolutePath)) {
                return;
            }
        }
        geoTiffFiles.add(geoTiffFile);
    }

    private List<File> resolveExistingDepthGeoTiffFiles(File depthFolder) {
        List<String> resizedGeoTiffPaths = new ArrayList<>();
        if (depthFolder.exists() && depthFolder.isDirectory()) {
            FileUtils.getFilePathsByExtension(depthFolder.getAbsolutePath(), TerrainRasterFormat.EXTENSION, resizedGeoTiffPaths, true);
        }

        List<File> result = new ArrayList<>();
        Set<String> resizedFileNames = new HashSet<>();
        for (String resizedGeoTiffPath : resizedGeoTiffPaths) {
            File resizedFile = new File(resizedGeoTiffPath);
            result.add(resizedFile);
            resizedFileNames.add(resizedFile.getName());
        }

        for (File standardizedRasterFile : manager.getStandardizedGeoTiffFiles()) {
            if (!resizedFileNames.contains(standardizedRasterFile.getName())) {
                result.add(standardizedRasterFile);
            }
        }
        return result;
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

    private Vector2d getPixelSizeMeters(TerrainRasterData data) {
        double middleLatitude = Math.toRadians((data.minLatitude() + data.maxLatitude()) * 0.5);
        double widthMeters = GlobeUtils.distanceBetweenLongitudesRad(middleLatitude,
                Math.toRadians(data.minLongitude()), Math.toRadians(data.maxLongitude()));
        double heightMeters = GlobeUtils.distanceBetweenLatitudesRad(
                Math.toRadians(data.minLatitude()), Math.toRadians(data.maxLatitude()));
        return new Vector2d(widthMeters / data.width(), heightMeters / data.height());
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

            log.info("[Pre][Standardization] Downsampling {} before split. pixelSize={}m -> {}m", geoTiffFilePath, pixelSizeMeters.x, maxUsefulPixelSizeMeters);
            return getGeoTiffCoverageStore().getResizedCoverage2D(geoTiffFilePath, originalCoverage, maxUsefulPixelSizeMeters, maxUsefulPixelSizeMeters);
        } catch (Exception e) {
            log.warn("[Pre][Standardization] Failed to pre-downsample {}. Using original coverage.", geoTiffFilePath, e);
            return originalCoverage;
        }
    }

    private double getMaxUsefulPixelSizeMeters() {
        int maxDepth = globalOptions.getMaximumTileDepth();
        if (maxDepth < 0) {
            maxDepth = manager.getAvailableTileSet().getMaxAvailableDepth();
        } else {
            int availableMaxDepth = manager.getAvailableTileSet().getMaxAvailableDepth();
            if (availableMaxDepth >= 0) {
                maxDepth = Math.min(maxDepth, availableMaxDepth);
            }
        }

        if (maxDepth < 0) {
            return 0.0;
        }

        double desiredPixelSize = manager.getDepthDesiredPixelSizeXinMetersMap().getOrDefault(maxDepth, 0.0);
        if (desiredPixelSize <= 0.0) {
            return 0.0;
        }

        return desiredPixelSize * 0.5;
    }

    private List<String> resolveConfiguredInputPaths() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        if (globalOptions.getInputPaths() != null && !globalOptions.getInputPaths().isEmpty()) {
            return globalOptions.getInputPaths();
        }
        if (globalOptions.getInputPath() != null && !globalOptions.getInputPath().isBlank()) {
            return List.of(globalOptions.getInputPath());
        }
        return List.of();
    }

    private GeoTiffCoverageStore getGeoTiffCoverageStore() {
        return manager.getGeoTiffCoverageStore();
    }
}
