package com.gaia3d.terrain.tile;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.structure.GeographicExtension;
import com.gaia3d.terrain.tile.geotiff.GaiaGeoTiffManager;
import com.gaia3d.terrain.types.InterpolationType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.imagen.media.range.NoDataContainer;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.util.CoverageUtilities;
import org.joml.Vector2d;
import org.joml.Vector2i;

import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

@Slf4j
@Getter
@Setter
public class TerrainElevationData {
    private GlobalOptions globalOptions = GlobalOptions.getInstance();

    private Vector2d pixelSizeMeters;
    private TerrainElevationDataManager terrainElevDataManager = null;
    private String geotiffFilePath = "";
    private String geotiffFileName = "";
    private GeographicExtension geographicExtension = new GeographicExtension();
    private GridCoverage2D coverage = null;
    private Raster raster = null;
    private double minAltitude = Double.MAX_VALUE;
    private double maxAltitude = Double.MIN_VALUE;
    private double[] altitude = new double[1];
    private NoDataContainer noDataContainer = null;
    private int geoTiffWidth = -1;
    private int geoTiffHeight = -1;
    private Vector2i gridCoverage2DSize = null;

    public TerrainElevationData(TerrainElevationDataManager terrainElevationDataManager) {
        this.terrainElevDataManager = terrainElevationDataManager;
    }

    public void deleteCoverage() {
        if (this.coverage != null) {
            this.coverage.dispose(true);
            this.coverage = null;
        }
        if (this.noDataContainer != null) {
            this.noDataContainer = null;
        }
        this.raster = null;
    }

    public void deleteObjects() {
        this.deleteCoverage();
        this.terrainElevDataManager = null;
        this.geotiffFilePath = null;
        if (this.geographicExtension != null) {
            this.geographicExtension.deleteObjects();
            this.geographicExtension = null;
        }
        altitude = null;
        noDataContainer = null;
    }

    public double getPixelArea() {
        return this.pixelSizeMeters.x * this.pixelSizeMeters.y;
    }

    public double getGridValue(int x, int y) {
        if (raster == null) {
            if (this.coverage == null) {
                GaiaGeoTiffManager gaiaGeoTiffManager = this.terrainElevDataManager.getGaiaGeoTiffManager();
                this.coverage = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(this.geotiffFilePath);
            }
            try {
                RenderedImage ri = coverage.getRenderedImage();
                if (ri.getWidth() * ri.getHeight() < 1024 * 1024) {
                    try {
                        this.raster = ri.getData();
                    } catch (Exception rasterException) {
                        log.debug("Failed to materialize raster for {}, fallback to coverage.evaluate: {}",
                                geotiffFilePath, rasterException.getMessage());
                    }
                } else {
                    return evaluateCoverageValue(x, y);
                }
            } catch (Exception e) {
                return evaluateCoverageValue(x, y);
            }
        }

        if (raster != null) {
            try {
                double value = raster.getSampleDouble(x, y, 0);
                if (Double.isNaN(value)) return globalOptions.getNoDataValue();
                
                if (this.noDataContainer == null && coverage != null) {
                    this.noDataContainer = CoverageUtilities.getNoDataProperty(coverage);
                }
                if (noDataContainer != null && value == noDataContainer.getAsSingleValue()) {
                    return globalOptions.getNoDataValue();
                }
                return value;
            } catch (Exception e) {
                return globalOptions.getNoDataValue();
            }
        }
        return evaluateCoverageValue(x, y);
    }

    private double evaluateCoverageValue(int x, int y) {
        if (coverage == null) {
            return globalOptions.getNoDataValue();
        }

        try {
            float[] result = new float[1];
            coverage.evaluate(new Point2D.Double(getLonDeg(x), getLatDeg(y)), result);
            double value = result[0];
            if (Double.isNaN(value)) {
                return globalOptions.getNoDataValue();
            }

            if (this.noDataContainer == null) {
                this.noDataContainer = CoverageUtilities.getNoDataProperty(coverage);
            }
            if (noDataContainer != null && value == noDataContainer.getAsSingleValue()) {
                return globalOptions.getNoDataValue();
            }
            return value;
        } catch (Exception evaluateException) {
            log.debug("Coverage evaluate failed for {} at ({}, {}): {}",
                    geotiffFilePath, x, y, evaluateException.getMessage());
            return globalOptions.getNoDataValue();
        }
    }
    
    private double getLonDeg(int col) {
        if (gridCoverage2DSize == null) updateSizeInfo();
        double minLonDeg = this.geographicExtension.getMinLongitudeDeg();
        double resX = this.geographicExtension.getLongitudeRangeDegree() / gridCoverage2DSize.x;
        return minLonDeg + col * resX;
    }

    private double getLatDeg(int row) {
        if (gridCoverage2DSize == null) updateSizeInfo();
        double maxLatDeg = this.geographicExtension.getMaxLatitudeDeg();
        double resY = this.geographicExtension.getLatitudeRangeDegree() / gridCoverage2DSize.y;
        return maxLatDeg - row * resY;
    }

    private void updateSizeInfo() {
        gridCoverage2DSize = this.terrainElevDataManager.getGaiaGeoTiffManager().getGridCoverage2DSize(this.geotiffFilePath);
    }

    public double getElevation(double lonDeg, double latDeg, boolean[] intersects) {
        if (!this.geographicExtension.intersects(lonDeg, latDeg)) {
            intersects[0] = false;
            return 0.0;
        }

        if (this.coverage != null) {
            try {
                float[] result = new float[1];
                this.coverage.evaluate(new Point2D.Double(lonDeg, latDeg), result);
                
                double val = result[0];
                if (Double.isNaN(val)) {
                    intersects[0] = false;
                    return globalOptions.getNoDataValue();
                }
                
                if (this.noDataContainer == null) {
                    this.noDataContainer = CoverageUtilities.getNoDataProperty(coverage);
                }
                if (noDataContainer != null && val == noDataContainer.getAsSingleValue()) {
                    intersects[0] = false;
                    return globalOptions.getNoDataValue();
                }

                intersects[0] = true;
                minAltitude = Math.min(minAltitude, val);
                maxAltitude = Math.max(maxAltitude, val);
                return val;
            } catch (Exception e) {
            }
        }

        if (gridCoverage2DSize == null) updateSizeInfo();
        
        double unitaryX = (lonDeg - this.geographicExtension.getMinLongitudeDeg()) / this.geographicExtension.getLongitudeRangeDegree();
        double unitaryY = 1.0 - (latDeg - this.geographicExtension.getMinLatitudeDeg()) / this.geographicExtension.getLatitudeRangeDegree();

        intersects[0] = true;
        double resultAltitude;
        if (globalOptions.getInterpolationType().equals(InterpolationType.BILINEAR)) {
            resultAltitude = calcBilinearInterpolation(unitaryX, unitaryY, gridCoverage2DSize.x, gridCoverage2DSize.y);
        } else {
            int column = (int) Math.floor(unitaryX * gridCoverage2DSize.x);
            int row = (int) Math.floor(unitaryY * gridCoverage2DSize.y);
            resultAltitude = getGridValue(column, row);
        }

        if (resultAltitude == globalOptions.getNoDataValue()) {
            intersects[0] = false;
        } else {
            minAltitude = Math.min(minAltitude, resultAltitude);
            maxAltitude = Math.max(maxAltitude, resultAltitude);
        }

        return resultAltitude;
    }

    private double calcBilinearInterpolation(double x, double y, int geoTiffWidth, int geoTiffHeight) {
        int column = (int) Math.floor(x * geoTiffWidth);
        int row = (int) Math.floor(y * geoTiffHeight);
        double factorX = x * geoTiffWidth - column;
        double factorY = y * geoTiffHeight - row;

        int c1 = Math.min(column + 1, geoTiffWidth - 1);
        int r1 = Math.min(row + 1, geoTiffHeight - 1);

        double v00 = getGridValue(column, row);
        double v01 = getGridValue(column, r1);
        double v10 = getGridValue(c1, row);
        double v11 = getGridValue(c1, r1);

        double noData = globalOptions.getNoDataValue();
        if (v00 == noData || v01 == noData || v10 == noData || v11 == noData) return v00;

        double v0 = v00 * (1.0 - factorY) + v01 * factorY;
        double v1 = v10 * (1.0 - factorY) + v11 * factorY;
        return v0 + factorX * (v1 - v0);
    }
}
