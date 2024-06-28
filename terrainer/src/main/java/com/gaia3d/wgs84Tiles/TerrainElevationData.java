package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GeographicExtension;
import com.gaia3d.command.GlobalOptions;
import com.gaia3d.command.InterpolationType;
import it.geosolutions.jaiext.range.NoDataContainer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.joml.Vector2d;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.image.Raster;

@Slf4j
@Getter
@Setter
public class TerrainElevationData {
    private Vector2d pixelSizeMeters;
    // the terrain elevation data is stored in a geotiff file
    private TerrainElevationDataManager terrainElevDataManager = null;
    private String geotiffFilePath = "";
    private GeographicExtension geographicExtension = new GeographicExtension();
    private GridCoverage2D coverage = null;
    private Raster raster = null;
    private double minAltitude = Double.MAX_VALUE;
    private double maxAltitude = Double.MIN_VALUE;
    private double[] memSaveAlt = new double[1];
    private CoordinateReferenceSystem memSaveWgs84 = DefaultGeographicCRS.WGS84;
    private NoDataContainer memSaveNoDataContainer = null;
    private DirectPosition2D memSavePosWorld = null; // longitude supplied first

    public TerrainElevationData(TerrainElevationDataManager terrainElevationDataManager) {
        this.terrainElevDataManager = terrainElevationDataManager;
    }

    public void deleteCoverage() {
        if (this.coverage != null) {
            this.coverage.dispose(true);
            this.coverage = null;
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

        memSaveAlt = null;
        memSaveWgs84 = null;
        memSaveNoDataContainer = null;
        memSavePosWorld = null;
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
        if (raster != null) {
            try {
                value = raster.getSampleDouble(x, y, 0);
            } catch (ArrayIndexOutOfBoundsException e) {
                log.debug("[getGridValue : ArrayIndexOutOfBoundsException] getGridValue", e);
            } catch (Exception e) {
                log.error("[getGridValue : Exception] Error in getGridValue", e);
            }
            if (this.memSaveNoDataContainer == null) {
                this.memSaveNoDataContainer = CoverageUtilities.getNoDataProperty(coverage);
            }

            if (memSaveNoDataContainer != null) {
                double nodata = memSaveNoDataContainer.getAsSingleValue();
                if (value == nodata) {
                    return 0.0;
                }
            }
        }
        return value;
    }

    public double getElevation(double lonDeg, double latDeg, boolean[] intersects) {
        double resultAltitude = 0.0;

        // 1rst check if lon, lat intersects with geoExtension
        if (!this.geographicExtension.intersects(lonDeg, latDeg)) {
            intersects[0] = false;
            return resultAltitude;
        }

        if (this.coverage == null) {
            GaiaGeoTiffManager gaiaGeoTiffManager = new GaiaGeoTiffManager();
            this.coverage = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(this.geotiffFilePath);
        }

        // determine the grid coordinates of the point
        if (this.raster == null) {
            this.raster = this.coverage.getRenderedImage().getData();
        }

        double unitaryX = (lonDeg - this.geographicExtension.getMinLongitudeDeg()) / this.geographicExtension.getLongitudeRangeDegree();
        double unitaryY = 1.0 - (latDeg - this.geographicExtension.getMinLatitudeDeg()) / this.geographicExtension.getLatitudeRangeDegree();

        int rasterHeight = this.raster.getHeight();
        int rasterWidth = this.raster.getWidth();

        int column = (int) (unitaryX * rasterWidth); // nearest column
        int row = (int) (unitaryY * rasterHeight); // nearest row

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        if (globalOptions.getInterpolationType() == InterpolationType.NEAREST) {
            intersects[0] = true;
            resultAltitude = calcNearestInterpolation(column, row);
        } else {
            intersects[0] = true;
            resultAltitude = calcBilinearInterpolation(column, row);
        }

        // update min, max altitude
        minAltitude = Math.min(minAltitude, resultAltitude);
        maxAltitude = Math.max(maxAltitude, resultAltitude);

        return resultAltitude;
    }

    private double calcNearestInterpolation(int column, int row) {
        return this.getGridValue(column, row);
    }

    private double calcBilinearInterpolation(int column, int row) {
        int rasterHeight = this.raster.getHeight();
        int rasterWidth = this.raster.getWidth();

        int columnNext = column + 1;
        int rowNext = row + 1;

        if(columnNext >= rasterWidth) {
            columnNext = rasterWidth - 1;
        }

        if(rowNext >= rasterHeight) {
            rowNext = rasterHeight - 1;
        }

        double factorX = (column + 0.5) / rasterWidth;
        double factorY = (row + 0.5) / rasterHeight;

        // interpolation bilinear.***
        double value00 = this.getGridValue(column, row);
        double value01 = this.getGridValue(column, rowNext);
        double value10 = this.getGridValue(columnNext, row);
        double value11 = this.getGridValue(columnNext, rowNext);

        double value0 = value00 * (1.0 - factorY) + value01 * factorY;
        double value1 = value10 * (1.0 - factorY) + value11 * factorY;

        return value0 + factorX * (value1 - value0);
    }

}