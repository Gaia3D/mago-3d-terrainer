package com.gaia3d.terrain.tile.elevation;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.util.NoDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.geometry.Bounds;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.gce.geotiff.GeoTiffReader;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.Raster;
import java.io.File;
import java.util.Iterator;

/**
 * Samples the geoid height (N) at a geographic position.
 * It is used as the height of the areas without DEM data, so they are at the mean sea level instead of the ellipsoid.
 * The geoid grid is read with a subsampling of at most MAX_GRID_WIDTH columns, the geoid undulation being smooth.
 */
@Slf4j
public class GeoidHeightSampler {
    private static final int MAX_GRID_WIDTH = 1440;
    private static String cachedGeoidPath = null;
    private static GeoidHeightSampler cachedSampler = null;

    private final float[] heights;
    private final int width;
    private final int height;
    // the longitude/latitude of the center of the first sample, and the distance between two samples
    private final double firstLonDeg;
    private final double firstLatDeg;
    private final double stepLonDeg;
    private final double stepLatDeg;

    private GeoidHeightSampler(float[] heights, int width, int height, double firstLonDeg, double firstLatDeg, double stepLonDeg, double stepLatDeg) {
        this.heights = heights;
        this.width = width;
        this.height = height;
        this.firstLonDeg = firstLonDeg;
        this.firstLatDeg = firstLatDeg;
        this.stepLonDeg = stepLonDeg;
        this.stepLatDeg = stepLatDeg;
    }

    /**
     * Returns the sampler of the configured geoid, or null when no geoid is used or it cannot be read.
     */
    public static synchronized GeoidHeightSampler getConfiguredSampler() {
        String geoidPath = GlobalOptions.getInstance().getGeoidPath();
        if (geoidPath == null || geoidPath.isEmpty()) {
            return null;
        }
        if (!geoidPath.equals(cachedGeoidPath)) {
            cachedGeoidPath = geoidPath;
            cachedSampler = load(new File(geoidPath));
        }
        return cachedSampler;
    }

    private static GeoidHeightSampler load(File geoidFile) {
        GeoTiffReader geoTiffReader = null;
        try {
            geoTiffReader = new GeoTiffReader(geoidFile);
            CoordinateReferenceSystem crs = geoTiffReader.getCoordinateReferenceSystem();
            if (!(crs instanceof GeographicCRS)) {
                log.warn("[Geoid] The geoid {} is not in a geographic CRS, the areas without data are generated at height 0.", geoidFile.getName());
                return null;
            }
            Bounds envelope = geoTiffReader.getOriginalEnvelope();
            int sourceWidth = geoTiffReader.getOriginalGridRange().getSpan(0);
            int sourceHeight = geoTiffReader.getOriginalGridRange().getSpan(1);
            int subsampling = Math.max(1, (int) Math.ceil((double) sourceWidth / MAX_GRID_WIDTH));

            Raster raster = readSubsampled(geoidFile, subsampling);
            int width = raster.getWidth();
            int height = raster.getHeight();
            float[] heights = new float[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    heights[y * width + x] = raster.getSampleFloat(raster.getMinX() + x, raster.getMinY() + y, 0);
                }
            }

            double pixelLonDeg = (envelope.getMaximum(0) - envelope.getMinimum(0)) / sourceWidth;
            double pixelLatDeg = (envelope.getMaximum(1) - envelope.getMinimum(1)) / sourceHeight;
            // the image rows go from north to south
            double firstLonDeg = envelope.getMinimum(0) + pixelLonDeg * 0.5;
            double firstLatDeg = envelope.getMaximum(1) - pixelLatDeg * 0.5;
            log.info("[Geoid] Areas without data use the geoid height of {} ({}x{} samples).", geoidFile.getName(), width, height);
            return new GeoidHeightSampler(heights, width, height, firstLonDeg, firstLatDeg, pixelLonDeg * subsampling, -pixelLatDeg * subsampling);
        } catch (Exception e) {
            log.warn("[Geoid] Failed to read the geoid {}, the areas without data are generated at height 0.", geoidFile.getName(), e);
            return null;
        } finally {
            if (geoTiffReader != null) {
                geoTiffReader.dispose();
            }
        }
    }

    private static Raster readSubsampled(File geoidFile, int subsampling) throws Exception {
        try (ImageInputStream inputStream = ImageIO.createImageInputStream(geoidFile)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(inputStream);
            if (!readers.hasNext()) {
                throw new IllegalStateException("No image reader for " + geoidFile);
            }
            ImageReader imageReader = readers.next();
            try {
                imageReader.setInput(inputStream, true, true);
                ImageReadParam readParam = imageReader.getDefaultReadParam();
                readParam.setSourceSubsampling(subsampling, subsampling, 0, 0);
                return imageReader.read(0, readParam).getData();
            } finally {
                imageReader.dispose();
            }
        }
    }

    static GeoidHeightSampler forGrid(float[] heights, int width, int height, double firstLonDeg, double firstLatDeg, double stepLonDeg, double stepLatDeg) {
        return new GeoidHeightSampler(heights, width, height, firstLonDeg, firstLatDeg, stepLonDeg, stepLatDeg);
    }

    /**
     * Returns the geoid height at the position. Outside the geoid grid (a regional geoid), the nearest edge of the grid is used,
     * so there is no step at the border of the grid. The invalid samples (NODATA, background) are ignored in the bilinear
     * interpolation, and 0 is returned when there is no valid sample.
     */
    public double getHeight(double lonDeg, double latDeg) {
        double gridX = (normalizeLongitude(lonDeg) - firstLonDeg) / stepLonDeg;
        double gridY = (latDeg - firstLatDeg) / stepLatDeg;
        gridX = Math.max(0.0, Math.min(width - 1.0, gridX));
        gridY = Math.max(0.0, Math.min(height - 1.0, gridY));
        int x0 = (int) Math.floor(gridX);
        int y0 = (int) Math.floor(gridY);
        int x1 = Math.min(x0 + 1, width - 1);
        int y1 = Math.min(y0 + 1, height - 1);
        double fx = gridX - x0;
        double fy = gridY - y0;

        double sum = 0.0;
        double weightSum = 0.0;
        double[] weights = {(1 - fx) * (1 - fy), fx * (1 - fy), (1 - fx) * fy, fx * fy};
        int[] indices = {y0 * width + x0, y0 * width + x1, y1 * width + x0, y1 * width + x1};
        double configuredNoDataValue = GlobalOptions.getInstance().getNoDataValue();
        for (int i = 0; i < 4; i++) {
            double value = heights[indices[i]];
            if (weights[i] > 0.0 && NoDataUtils.isValidGeoidHeight(value, Double.NaN, configuredNoDataValue)) {
                sum += value * weights[i];
                weightSum += weights[i];
            }
        }
        return weightSum > 0.0 ? sum / weightSum : 0.0;
    }

    private double normalizeLongitude(double lonDeg) {
        double centerLonDeg = firstLonDeg + stepLonDeg * (width - 1) * 0.5;
        while (lonDeg < centerLonDeg - 180.0) {
            lonDeg += 360.0;
        }
        while (lonDeg > centerLonDeg + 180.0) {
            lonDeg -= 360.0;
        }
        return lonDeg;
    }
}
