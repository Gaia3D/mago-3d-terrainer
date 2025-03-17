package com.gaia3d.terrain.tile;

import com.gaia3d.terrain.structure.GeographicExtension;
import com.gaia3d.terrain.tile.geotiff.GaiaGeoTiffManager;
import com.gaia3d.terrain.types.InterpolationType;
import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.util.GaiaGeoTiffUtils;
import it.geosolutions.jaiext.range.NoDataContainer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.geometry.DirectPosition2D;
import org.joml.Vector2d;
import org.joml.Vector2i;
import org.opengis.coverage.Coverage;
import org.opengis.referencing.FactoryException;

import java.awt.image.Raster;
import java.io.File;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Setter
public class TerrainElevationData {
    private GlobalOptions globalOptions = GlobalOptions.getInstance();

    private Vector2d pixelSizeMeters;
    // the terrain elevation data is stored in a geotiff file
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
    private DirectPosition2D worldPosition = null; // longitude supplied first
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
        worldPosition = null;
    }

    public double getPixelArea() {
        return this.pixelSizeMeters.x * this.pixelSizeMeters.y;
    }

    public void getPixelSizeDegree(Vector2d resultPixelSize) {
        double imageWidth = this.coverage.getRenderedImage().getWidth();
        double imageHeight = this.coverage.getRenderedImage().getHeight();
        double longitudeRange = this.geographicExtension.getLongitudeRangeDegree();
        double latitudeRange = this.geographicExtension.getLatitudeRangeDegree();
        double pixelSizeX = longitudeRange / imageWidth;
        double pixelSizeY = latitudeRange / imageHeight;
        resultPixelSize.set(pixelSizeX, pixelSizeY);
    }

    public double getGridValue(int x, int y) {
        double value = 0.0;
        if (raster == null) {
            GaiaGeoTiffManager gaiaGeoTiffManager = null;

            if (this.coverage == null) {
                gaiaGeoTiffManager = this.terrainElevDataManager.getGaiaGeoTiffManager();
                this.coverage = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(this.geotiffFilePath);
            }

            if (this.noDataContainer == null) {
                this.noDataContainer = CoverageUtilities.getNoDataProperty(coverage);
            }

            // determine the grid coordinates of the point
            if (this.raster == null) {
                this.raster = this.coverage.getRenderedImage().getData(); // original.***
                this.coverage.dispose(true);
                this.coverage = null;
            }
        }

        if (raster != null) {
            try {
                value = raster.getSampleDouble(x, y, 0);
                // check if value is NaN
                if (Double.isNaN(value)) {
                    return globalOptions.getNoDataValue();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                log.debug("[getGridValue : ArrayIndexOutOfBoundsException] getGridValue", e);
            } catch (Exception e) {
                log.error("[getGridValue : Exception] Error in getGridValue", e);
                log.error("Error:", e);
            }

            if (this.noDataContainer == null && coverage != null) {
                this.noDataContainer = CoverageUtilities.getNoDataProperty(coverage);
            }

            if (noDataContainer != null) {
                double nodata = noDataContainer.getAsSingleValue();
                if (value == nodata) {
                    return globalOptions.getNoDataValue();
                }
            }
        }
        return value;
    }

    public double getElevation(double lonDeg, double latDeg, boolean[] intersects) {
        double resultAltitude = 0.0;

        // First check if lon, lat intersects with geoExtension
        if (!this.geographicExtension.intersects(lonDeg, latDeg)) {
            intersects[0] = false;
            return resultAltitude;
        }

        if(gridCoverage2DSize == null) {
            gridCoverage2DSize = this.terrainElevDataManager.getGaiaGeoTiffManager().getGridCoverage2DSize(this.geotiffFilePath);
        }
        Vector2i size = gridCoverage2DSize;

        double unitaryX = (lonDeg - this.geographicExtension.getMinLongitudeDeg()) / this.geographicExtension.getLongitudeRangeDegree();
        double unitaryY = 1.0 - (latDeg - this.geographicExtension.getMinLatitudeDeg()) / this.geographicExtension.getLatitudeRangeDegree();

        int geoTiffRasterHeight = size.y;
        int geoTiffRasterWidth = size.x;

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        if (globalOptions.getInterpolationType().equals(InterpolationType.BILINEAR)) {
            intersects[0] = true;
            resultAltitude = calcBilinearInterpolation(unitaryX, unitaryY, geoTiffRasterWidth, geoTiffRasterHeight);
        } else {
            intersects[0] = true;
            int column = (int) Math.floor(unitaryX * geoTiffRasterWidth);
            int row = (int) Math.floor(unitaryY * geoTiffRasterHeight);
            resultAltitude = calcNearestInterpolation(column, row);
        }

        double noDataValue = globalOptions.getNoDataValue();
        if (resultAltitude == noDataValue) {
            intersects[0] = false;
            return resultAltitude;
        }

        // update min, max altitude
        minAltitude = Math.min(minAltitude, resultAltitude);
        maxAltitude = Math.max(maxAltitude, resultAltitude);

        return resultAltitude;
    }

    private double calcNearestInterpolation(int column, int row) {
        return this.getGridValue(column, row);
    }

    private double calcBilinearInterpolation(double x, double y, int geoTiffWidth, int geoTiffHeight) {
        int column = (int) Math.floor(x * geoTiffWidth);
        int row = (int) Math.floor(y * geoTiffHeight);

        double factorX = x * geoTiffWidth - column;
        double factorY = y * geoTiffHeight - row;

        int columnNext = column + 1;
        int rowNext = row + 1;

        if (columnNext >= geoTiffWidth) {
            columnNext = geoTiffWidth - 1;
        }

        if (rowNext >= geoTiffHeight) {
            rowNext = geoTiffHeight - 1;
        }

        // interpolation bilinear.***
        double value00 = this.getGridValue(column, row);
        double value01 = this.getGridValue(column, rowNext);
        double value10 = this.getGridValue(columnNext, row);
        double value11 = this.getGridValue(columnNext, rowNext);

        /* check noDataValue */
        double noDataValue = globalOptions.getNoDataValue();
        boolean hasNoData = (value00 == noDataValue) || (value01 == noDataValue) || (value10 == noDataValue) || (value11 == noDataValue);
        if (hasNoData) {
            if (value00 == noDataValue) {
                return noDataValue;
            } else {
                return value00;
            }
        }

        double value0 = value00 * (1.0 - factorY) + value01 * factorY;
        double value1 = value10 * (1.0 - factorY) + value11 * factorY;

        return value0 + factorX * (value1 - value0);
    }
}