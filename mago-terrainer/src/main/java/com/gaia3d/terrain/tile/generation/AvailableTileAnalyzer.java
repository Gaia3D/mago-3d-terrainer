package com.gaia3d.terrain.tile.generation;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.structure.GeographicExtension;
import com.gaia3d.terrain.tile.core.TileRange;
import com.gaia3d.terrain.tile.custom.AvailableTileSet;
import com.gaia3d.terrain.tile.geotiff.GeoTiffCoverageStore;
import com.gaia3d.terrain.util.GaiaGeoTiffUtils;
import com.gaia3d.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.crs.ProjectedCRS;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffException;
import org.geotools.referencing.CRS;
import org.joml.Vector2d;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class AvailableTileAnalyzer {
    private static GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final TerrainTilesetGenerator generator;

    public void calculateAvailableTilesForEachDepth() throws IOException, FactoryException {
        List<String> rasterFileNames = resolveInputRasterFileNames();

        if (rasterFileNames.isEmpty()) {
            log.error("No GeoTiff files found in input paths: {}", resolveConfiguredInputPaths());
            throw new RuntimeException("Error: No GeoTiff files found in input paths: " + resolveConfiguredInputPaths());
        }

        int geoTiffFilesSize = rasterFileNames.size();
        int geoTiffFilesCount = 0;

        TileRange tilesRange = new TileRange();
        tilesRange.setMinTileX(0);
        tilesRange.setMaxTileX(1);
        tilesRange.setMinTileY(0);
        tilesRange.setMaxTileY(0);
        AvailableTileSet availableTileSet = generator.getAvailableTileSet();
        List<TileRange> tileRanges = availableTileSet.getMapDepthAvailableTileRanges().computeIfAbsent(0, k -> new java.util.ArrayList<>());
        tileRanges.add(tilesRange);

        GeoTiffCoverageStore geoTiffCoverageStore = generator.getGeoTiffCoverageStore();
        for (String geoTiffFileName : rasterFileNames) {
            log.info("[Pre][Resize GeoTiff][{}/{}] resizing geoTiff : {} ", ++geoTiffFilesCount, geoTiffFilesSize, geoTiffFileName);
            if (generator.getMapNoUsableGeotiffPaths().containsKey(geoTiffFileName)) {
                continue;
            }

            GridCoverage2D originalGridCoverage2D = geoTiffCoverageStore.loadGeoTiffGridCoverage2D(geoTiffFileName);
            CoordinateReferenceSystem crsTarget = originalGridCoverage2D.getCoordinateReferenceSystem2D();
            if (!(crsTarget instanceof ProjectedCRS || crsTarget instanceof GeographicCRS)) {
                log.error("The supplied grid coverage uses an unsupported crs! You are allowed to use only projected and geographic coordinate reference systems");
                throw new GeoTiffException(null, "The supplied grid coverage uses an unsupported crs! You are allowed to use only projected and geographic coordinate reference systems", null);
            }

            Vector2d pixelSizeMeters = GaiaGeoTiffUtils.getPixelSizeMeters(originalGridCoverage2D);

            GeometryFactory gf = new GeometryFactory();
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

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        if (globalOptions.getMaximumTileDepth() < 0) {
            globalOptions.setMaximumTileDepth(availableTileSet.getMaxAvailableDepth());
        }
        System.gc();
    }

    public List<String> resolveInputRasterFileNames() {
        Set<String> rasterFileNames = new LinkedHashSet<>();
        for (String inputPath : resolveConfiguredInputPaths()) {
            File inputFile = new File(inputPath);
            if (inputFile.exists() && inputFile.isDirectory()) {
                List<String> paths = new ArrayList<>();
                FileUtils.getFilePathsByExtension(inputFile.getAbsolutePath(), ".tif", paths, true);
                rasterFileNames.addAll(paths);

                paths.clear();
                FileUtils.getFilePathsByExtension(inputFile.getAbsolutePath(), ".tiff", paths, true);
                rasterFileNames.addAll(paths);
            } else if (inputFile.exists() && inputFile.isFile()) {
                String lowerName = inputFile.getName().toLowerCase(Locale.ROOT);
                if (lowerName.endsWith(".tif") || lowerName.endsWith(".tiff")) {
                    rasterFileNames.add(inputFile.getAbsolutePath());
                }
            } else {
                log.error("Input path is not exist or not a directory: {}", inputPath);
                throw new RuntimeException("Error: Input path is not exist or not a directory: " + inputPath);
            }
        }
        return new ArrayList<>(rasterFileNames);
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
