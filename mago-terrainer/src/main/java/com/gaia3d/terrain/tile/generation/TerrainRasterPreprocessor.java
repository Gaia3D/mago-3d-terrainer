package com.gaia3d.terrain.tile.generation;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.tile.geotiff.GeoTiffCoverageStore;
import com.gaia3d.terrain.tile.geotiff.RasterStandardizer;
import com.gaia3d.terrain.util.GaiaGeoTiffUtils;
import com.gaia3d.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.crs.ProjectedCRS;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffException;
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
    private static GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final TerrainTilesetGenerator manager;

    public void standardizeInputRasters() {
        List<String> rasterFileNames = manager.resolveInputRasterFileNames();
        log.info("[Pre][Standardization] Found Total {} GeoTiff files in input paths", rasterFileNames.size());

        if (rasterFileNames.isEmpty()) {
            throw new RuntimeException("No GeoTiff files found in input paths: " + resolveConfiguredInputPaths());
        }

        standardizeRasterFiles(rasterFileNames);

        File tempFolder = new File(globalOptions.getStandardizeTempPath());
        List<String> standardizedGeoTiffPaths = new ArrayList<>();
        FileUtils.getFilePathsByExtension(tempFolder.getAbsolutePath(), ".tif", standardizedGeoTiffPaths, true);
        if (standardizedGeoTiffPaths.isEmpty()) {
            throw new RuntimeException("No standardized GeoTiff files found in the standardization temp path: " + tempFolder.getAbsolutePath());
        }

        manager.getStandardizedGeoTiffFiles().clear();
        for (String standardizedGeoTiffPath : standardizedGeoTiffPaths) {
            manager.getStandardizedGeoTiffFiles().add(new File(standardizedGeoTiffPath));
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

        List<String> geoTiffFilePaths = new ArrayList<>();
        FileUtils.getFilePathsByExtension(terrainElevationDataFolderPath, "tif", geoTiffFilePaths, true);

        int geotiffCount = geoTiffFilePaths.size();
        manager.setGeoTiffFilesCount(geotiffCount);

        log.info("[Pre][Resize GeoTiff] resizing geoTiffs Count : {} ", geotiffCount);
        resizeRastersByDepth(terrainElevationDataFolderPath, currentFolderPath);
    }

    public void resizeRastersByDepth(String terrainElevationDataFolderPath, String currentFolderPath) throws IOException, FactoryException {
        List<String> geoTiffFileNames = new ArrayList<>();
        FileUtils.getFileNames(terrainElevationDataFolderPath, ".tif", geoTiffFileNames);

        if (currentFolderPath == null) {
            currentFolderPath = "";
        }

        int geoTiffFilesSize = geoTiffFileNames.size();
        int geoTiffFilesCount = 0;

        for (String geoTiffFileName : geoTiffFileNames) {
            log.info("[Pre][Resize GeoTiff][{}/{}] resizing geoTiff : {} ", ++geoTiffFilesCount, geoTiffFilesSize, geoTiffFileName);
            String geoTiffFilePath = terrainElevationDataFolderPath + File.separator + geoTiffFileName;

            if (manager.getMapNoUsableGeotiffPaths().containsKey(geoTiffFilePath)) {
                continue;
            }

            GridCoverage2D originalGridCoverage2D = getGeoTiffCoverageStore().loadGeoTiffGridCoverage2D(geoTiffFilePath);
            CoordinateReferenceSystem crsTarget = originalGridCoverage2D.getCoordinateReferenceSystem2D();
            if (!(crsTarget instanceof ProjectedCRS || crsTarget instanceof GeographicCRS)) {
                log.error("The supplied grid coverage uses an unsupported crs! You are allowed to use only projected and geographic coordinate reference systems");
                throw new GeoTiffException(null, "The supplied grid coverage uses an unsupported crs! You are allowed to use only projected and geographic coordinate reference systems", null);
            }

            Vector2d pixelSizeMeters = GaiaGeoTiffUtils.getPixelSizeMeters(originalGridCoverage2D);

            int minTileDepth = globalOptions.getMinimumTileDepth();
            int maxTileDepth = globalOptions.getMaximumTileDepth();
            for (int depth = minTileDepth; depth <= maxTileDepth; depth += 1) {
                double desiredPixelSizeXinMeters = manager.getDepthDesiredPixelSizeXinMetersMap().get(depth);
                double desiredPixelSizeYinMeters = desiredPixelSizeXinMeters;

                if (desiredPixelSizeXinMeters < pixelSizeMeters.x) {
                    addDepthGeoTiffFile(depth, new File(geoTiffFilePath));
                    continue;
                }

                String depthStr = String.valueOf(depth);
                String resizedGeoTiffFolderPath = resolveResizedGeoTiffFolderPath(depthStr, currentFolderPath, geoTiffFilePath);
                String resizedGeoTiffFilePath = resizedGeoTiffFolderPath + File.separator + geoTiffFileName;

                if (FileUtils.isFileExists(resizedGeoTiffFilePath)) {
                    addDepthGeoTiffFile(depth, new File(resizedGeoTiffFilePath));
                    continue;
                }

                GridCoverage2D resizedGridCoverage2D = getGeoTiffCoverageStore().getResizedCoverage2D(geoTiffFilePath, originalGridCoverage2D, desiredPixelSizeXinMeters, desiredPixelSizeYinMeters);
                FileUtils.createAllFoldersIfNoExist(resizedGeoTiffFolderPath);
                getGeoTiffCoverageStore().saveGridCoverage2D(resizedGridCoverage2D, resizedGeoTiffFilePath);

                resizedGridCoverage2D.dispose(true);

                addDepthGeoTiffFile(depth, new File(resizedGeoTiffFilePath));
            }
        }

        getGeoTiffCoverageStore().clear();

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

        List<String> standardizedGeoTiffPaths = new ArrayList<>();
        FileUtils.getFilePathsByExtension(standardizeFolder.getAbsolutePath(), ".tif", standardizedGeoTiffPaths, true);
        if (standardizedGeoTiffPaths.isEmpty()) {
            throw new RuntimeException("Error: No standardized GeoTiff files found in the standardization temp path: " + standardizeFolder.getAbsolutePath());
        }

        manager.getStandardizedGeoTiffFiles().clear();
        for (String standardizedGeoTiffPath : standardizedGeoTiffPaths) {
            manager.getStandardizedGeoTiffFiles().add(new File(standardizedGeoTiffPath));
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
            FileUtils.getFilePathsByExtension(depthFolder.getAbsolutePath(), ".tif", resizedGeoTiffPaths, true);
        }

        List<File> result = new ArrayList<>();
        Set<String> resizedFileNames = new HashSet<>();
        for (String resizedGeoTiffPath : resizedGeoTiffPaths) {
            File resizedFile = new File(resizedGeoTiffPath);
            result.add(resizedFile);
            resizedFileNames.add(resizedFile.getName());
        }

        for (File standardizedGeoTiffFile : manager.getStandardizedGeoTiffFiles()) {
            if (!resizedFileNames.contains(standardizedGeoTiffFile.getName())) {
                result.add(standardizedGeoTiffFile);
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
