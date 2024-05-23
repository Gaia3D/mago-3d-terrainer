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
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import java.awt.image.Raster;
import java.io.IOException;

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
            value = raster.getSampleDouble(x, y, 0);
        }
        return value;
    }

    public double getElevationNearest(double lonDeg, double latDeg, boolean[] intersects) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        InterpolationType interpolationType = globalOptions.getInterpolationType();

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

        // https://taylor.callsen.me/parsing-geotiff-files-in-java/
        memSaveWgs84 = DefaultGeographicCRS.WGS84;

        memSaveNoDataContainer = CoverageUtilities.getNoDataProperty(coverage);
        //note :  DirectPosition2D(memSavewgs84, lonDeg, latDeg); // longitude supplied first
        if (memSavePosWorld == null) {
            memSavePosWorld = new DirectPosition2D(memSaveWgs84, 0.0, 0.0);
        }
        memSavePosWorld.x = lonDeg;
        memSavePosWorld.y = latDeg;

        memSaveAlt[0] = 0.0;
        try {
            coverage.evaluate((DirectPosition) memSavePosWorld, memSaveAlt);
            intersects[0] = true;

            // check if is NoData
            if (memSaveNoDataContainer != null) {
                double nodata = memSaveNoDataContainer.getAsSingleValue();
                if (memSaveAlt[0] == nodata) {
                    return 0.0;
                }
            }
        } catch (RuntimeException e) {
            // out of bounds coverage coordinates
            intersects[0] = false;
            return resultAltitude;
        }
        // update min, max altitude
        resultAltitude = memSaveAlt[0];
        minAltitude = Math.min(minAltitude, resultAltitude);
        maxAltitude = Math.max(maxAltitude, resultAltitude);

        return resultAltitude;
    }

    public double getElevation(double lonDeg, double latDeg, boolean[] intersects) throws TransformException, IOException {
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

        // https://taylor.callsen.me/parsing-geotiff-files-in-java/
        memSaveWgs84 = DefaultGeographicCRS.WGS84;

        memSaveNoDataContainer = CoverageUtilities.getNoDataProperty(coverage);
        //note :  DirectPosition2D(memSavewgs84, lonDeg, latDeg); // longitude supplied first
        if (memSavePosWorld == null) {
            memSavePosWorld = new DirectPosition2D(memSaveWgs84, 0.0, 0.0);
        }
        memSavePosWorld.x = lonDeg;
        memSavePosWorld.y = latDeg;


        memSaveAlt[0] = 0.0;
        try {
            coverage.evaluate((DirectPosition) memSavePosWorld, memSaveAlt);
            intersects[0] = true;

            // check if is NoData
            if (memSaveNoDataContainer != null) {
                double nodata = memSaveNoDataContainer.getAsSingleValue();
                if (memSaveAlt[0] == nodata) {
                    return 0.0;
                }
            }
        } catch (Exception e) {
            // out of bounds coverage coordinates
            intersects[0] = false;
            return resultAltitude;
        }
        // update min, max altitude
        resultAltitude = memSaveAlt[0];
        minAltitude = Math.min(minAltitude, resultAltitude);
        maxAltitude = Math.max(maxAltitude, resultAltitude);

        return resultAltitude;
    }

}
