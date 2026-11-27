package com.gaia3d.terrain.tile.generation;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.structure.GeographicExtension;
import com.gaia3d.terrain.tile.core.TileRange;
import com.gaia3d.terrain.tile.custom.AvailableTileSet;
import com.gaia3d.terrain.tile.geotiff.GeoTiffCoverageStore;
import com.gaia3d.terrain.util.GaiaGeoTiffUtils;
import com.gaia3d.util.CelestialBody;
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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.joml.Vector2d;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class AvailableTileAnalyzer {
    private final GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final TerrainTilesetGenerator generator;

    public void calculateAvailableTilesForEachDepth() throws IOException, FactoryException {
        List<String> rasterFileNames = resolveInputRasterFileNames();

        if (rasterFileNames.isEmpty()) {
            log.error("No GeoTiff files found in input paths: {}", resolveConfiguredInputPaths());
            throw new RuntimeException("Error: No GeoTiff files found in input paths: " + resolveConfiguredInputPaths());
        }

        generator.setRasterPriority(RasterPriority.create(rasterFileNames));

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
                if (globalOptions.getCelestialBody() != null && globalOptions.getCelestialBody() != CelestialBody.EARTH) {
                    getPlanetaryGeographicExtension(originalGridCoverage2D, crsTarget, geographicExtension);
                } else {
                    GaiaGeoTiffUtils.getGeographicExtension(originalGridCoverage2D, gf, targetToWgs, geographicExtension);
                }
            } catch (Exception ex) {
                log.error("Error calculating geographic extension for geotiff: {}", geoTiffFileName, ex);
                continue;
            }

            for (GeographicExtension normalizedExtension : normalizeLongitudeRange(geographicExtension)) {
                availableTileSet.addAvailableExtensions(pixelSizeMeters.x, normalizedExtension);
            }
        }
        availableTileSet.recombineTileRanges();

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        if (globalOptions.getMaximumTileDepth() < 0) {
            globalOptions.setMaximumTileDepth(availableTileSet.getMaxAvailableDepth());
        }
        System.gc();
    }

    /**
     * The geographic extension of a raster of another body than the Earth, in the geographic CRS of that body.
     * The transform to EPSG:4326 wraps the longitudes into -180..180, so a raster in the 0..360 convention (common for
     * planetary data) would get the extension of a thin band around 0. Here the west and east edges are transformed to the
     * base geographic CRS, and the east edge is unwrapped when it is not east of the west edge.
     */
    static void getPlanetaryGeographicExtension(GridCoverage2D coverage, CoordinateReferenceSystem crs, GeographicExtension resultExtension) throws Exception {
        ReferencedEnvelope envelope = new ReferencedEnvelope(coverage.getEnvelope2D());
        double minLon;
        double maxLon;
        double minLat;
        double maxLat;
        if (crs instanceof ProjectedCRS projectedCRS) {
            MathTransform toGeographic = CRS.findMathTransform(projectedCRS, projectedCRS.getBaseCRS(), true);
            double midY = (envelope.getMinimum(1) + envelope.getMaximum(1)) * 0.5;
            double midX = (envelope.getMinimum(0) + envelope.getMaximum(0)) * 0.5;
            // the centers of the first and last columns: the edges of a global raster are both on the same meridian
            double halfPixelX = envelope.getSpan(0) / coverage.getGridGeometry().getGridRange2D().getSpan(0) * 0.5;
            double[] points = {
                    envelope.getMinimum(0) + halfPixelX, midY, envelope.getMaximum(0) - halfPixelX, midY,
                    midX, midY, midX, envelope.getMinimum(1), midX, envelope.getMaximum(1)};
            double[] geographic = new double[points.length];
            toGeographic.transform(points, 0, geographic, 0, points.length / 2);
            // the transform wraps the longitudes: unwrap the west edge to the west and the east edge to the east of the center
            double centerLon = geographic[4];
            double westOffset = geographic[0] - centerLon;
            while (westOffset > 0.0) {
                westOffset -= 360.0;
            }
            double eastOffset = geographic[2] - centerLon;
            while (eastOffset < 0.0) {
                eastOffset += 360.0;
            }
            minLon = centerLon + westOffset - halfPixelX / projectedCRS.getBaseCRS().getDatum().getEllipsoid().getSemiMajorAxis() * 180.0 / Math.PI;
            maxLon = centerLon + eastOffset + halfPixelX / projectedCRS.getBaseCRS().getDatum().getEllipsoid().getSemiMajorAxis() * 180.0 / Math.PI;
            minLat = Math.max(-90.0, Math.min(geographic[7], geographic[9]));
            maxLat = Math.min(90.0, Math.max(geographic[7], geographic[9]));
        } else {
            minLon = envelope.getMinimum(0);
            maxLon = envelope.getMaximum(0);
            minLat = envelope.getMinimum(1);
            maxLat = envelope.getMaximum(1);
        }
        resultExtension.setDegrees(minLon, minLat, 0.0, maxLon, maxLat, 0.0);
    }

    /**
     * Returns the extension in -180..180 longitudes: an extension in the 0..360 convention (planetary data) is shifted,
     * and an extension crossing the antimeridian is split in two. The standardized rasters are normalized the same way
     * (TerrainRasterWriter.writeNormalizedLongitude).
     */
    static List<GeographicExtension> normalizeLongitudeRange(GeographicExtension extension) {
        double minLon = extension.getMinLongitudeDeg();
        double maxLon = extension.getMaxLongitudeDeg();
        double minLat = extension.getMinLatitudeDeg();
        double maxLat = extension.getMaxLatitudeDeg();
        if (minLon >= 180.0) {
            minLon -= 360.0;
            maxLon -= 360.0;
        } else if (maxLon <= -180.0) {
            minLon += 360.0;
            maxLon += 360.0;
        }
        List<GeographicExtension> result = new ArrayList<>();
        if (maxLon > 180.0) {
            result.add(createExtension(minLon, minLat, 180.0, maxLat));
            result.add(createExtension(-180.0, minLat, Math.min(180.0, maxLon - 360.0), maxLat));
        } else if (minLon < -180.0) {
            result.add(createExtension(-180.0, minLat, maxLon, maxLat));
            result.add(createExtension(Math.max(-180.0, minLon + 360.0), minLat, 180.0, maxLat));
        } else {
            result.add(createExtension(minLon, minLat, maxLon, maxLat));
        }
        return result;
    }

    private static GeographicExtension createExtension(double minLon, double minLat, double maxLon, double maxLat) {
        GeographicExtension extension = new GeographicExtension();
        extension.setDegrees(minLon, minLat, 0.0, maxLon, maxLat, 0.0);
        return extension;
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
