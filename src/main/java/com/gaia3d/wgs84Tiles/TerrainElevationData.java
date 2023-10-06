package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GeographicExtension;
import com.gaia3d.reader.GaiaGeoTiffReader;
import it.geosolutions.jaiext.range.NoDataContainer;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;

import org.geotools.coverage.util.CoverageUtilities;

import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.joml.Vector2d;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.awt.image.Raster;

import java.io.IOException;



public class TerrainElevationData {

    // the terrain elevation data is stored in a geotiff file.***
    public TerrainElevationDataManager terrainElevDataManager = null;
    public String geotiffFilePath = "";

    public GeographicExtension geographicExtension = new GeographicExtension();

    public Vector2d pixelSizeMeters = null;

    GridCoverage2D coverage = null;

    Raster raster = null;

    double minAltitude = Double.MAX_VALUE;
    double maxAltitude = Double.MIN_VALUE;

    double memSave_alt[] = new double[1];

    CoordinateReferenceSystem memSave_wgs84 = DefaultGeographicCRS.WGS84;

    NoDataContainer memSave_noDataContainer = null;
    DirectPosition2D memSave_posWorld = null; // longitude supplied first

    public TerrainElevationData(TerrainElevationDataManager terrainElevationDataManager) {
        this.terrainElevDataManager = terrainElevationDataManager;
    }

    public void deleteCoverage()
    {
        this.pixelSizeMeters = null;
        if(this.coverage != null)
        {
            this.coverage.dispose(true);
            this.coverage = null;
        }

        this.raster = null;
    }

    public void deleteObjects()
    {
        this.deleteCoverage();
        this.terrainElevDataManager = null;
        this.geotiffFilePath = null;
        if(this.geographicExtension != null)
        {
            this.geographicExtension.deleteObjects();
            this.geographicExtension = null;
        }

        memSave_alt = null;
        memSave_wgs84 = null;
        memSave_noDataContainer = null;
        memSave_posWorld = null;
    }

    public void loadGeoTiffFile(String geotiffFilePath) throws FactoryException, TransformException {
        // load the geotiff file.***
        this.geotiffFilePath = geotiffFilePath;

        // create coverage.***
        GaiaGeoTiffReader reader = new GaiaGeoTiffReader();

        this.coverage = reader.read(this.geotiffFilePath);
        this.raster = this.coverage.getRenderedImage().getData();

        CoordinateReferenceSystem crsTarget = coverage.getCoordinateReferenceSystem2D();
        CoordinateReferenceSystem crsWgs84 = CRS.decode("EPSG:4326", true);
        GeometryFactory gf = new GeometryFactory();
        MathTransform targetToWgs = CRS.findMathTransform(crsTarget, crsWgs84);

        // 127.1672976210000030,37.6009237940000034
        // 127.1985472009999967,37.6293537840000027

        GaiaGeoTiffUtils.getGeographicExtension(this.coverage, gf, targetToWgs, this.geographicExtension);

        Vector2d pixelSizeMeters = GaiaGeoTiffUtils.getPixelSizeMeters(this.coverage);

        int hola = 0;
    }

    public void getPixelSizeDegree(Vector2d resultPixelSize)
    {
        double imageWidth = this.coverage.getRenderedImage().getWidth();
        double imageHeight = this.coverage.getRenderedImage().getHeight();
        double longitudeRange = this.geographicExtension.getLongitudeRangeDegree();
        double latitudeRange = this.geographicExtension.getLatitudeRangeDegree();
        double pixelSizeX = longitudeRange / imageWidth;
        double pixelSizeY = latitudeRange / imageHeight;
        resultPixelSize.set(pixelSizeX, pixelSizeY);
    }

    public double getGridValue(int x, int y)
    {
        double value = 0.0;
        if (raster != null)
        {
            value = raster.getSampleDouble(x, y, 0);
        }
        return value;
    }

    public double getElevation(double lonDeg, double latDeg, boolean[] intersects) throws TransformException, IOException {
        double resultAltitude = 0.0;

        // 1rst check if lon, lat intersects with geoExtension.***
        if(!this.geographicExtension.intersects(lonDeg, latDeg))
        {
            intersects[0] = false;
            return resultAltitude;
        }

        if(this.coverage == null)
        {
            GaiaGeoTiffManager gaiaGeoTiffManager = new GaiaGeoTiffManager();
            this.coverage = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(this.geotiffFilePath);
        }

        // https://taylor.callsen.me/parsing-geotiff-files-in-java/
        memSave_wgs84 = DefaultGeographicCRS.WGS84;

        memSave_noDataContainer = CoverageUtilities.getNoDataProperty(coverage);
        //DirectPosition2D posWorld = new DirectPosition2D(memSave_wgs84, lonDeg, latDeg); // longitude supplied first
        if(memSave_posWorld == null)
        {
            memSave_posWorld = new DirectPosition2D(memSave_wgs84, 0.0, 0.0);
        }
        memSave_posWorld.x = lonDeg;
        memSave_posWorld.y = latDeg;
        //GridCoordinates2D posGrid = gg.worldToGrid(posWorld);

        memSave_alt[0] = 0.0;
        try{
            coverage.evaluate((DirectPosition) memSave_posWorld, memSave_alt);
            intersects[0] = true;

            // check if is NoData.***
            if(memSave_noDataContainer != null) {
                double nodata = memSave_noDataContainer.getAsSingleValue();
                if (memSave_alt[0] == nodata) {
                    return 0.0;
                }
            }
        }
        catch (Exception e)
        {
            //e.printStackTrace();
            intersects[0] = false;
            return resultAltitude;
        }
        // update min, max altitude.***
        resultAltitude = memSave_alt[0];
        minAltitude = Math.min(minAltitude, resultAltitude);
        maxAltitude = Math.max(maxAltitude, resultAltitude);

        return resultAltitude;
        /*
        //double altDouble = altitude.

        int coverageW = coverage.getRenderedImage().getWidth();
        int coverageH = coverage.getRenderedImage().getHeight();
        int pixelX = (int)((lonDeg - this.geographicExtension.getMinLongitudeDeg())/(this.geographicExtension.getLongitudeRangeDegree())*coverageW);
        int pixelY = (int)((latDeg - this.geographicExtension.getMinLatitudeDeg())/(this.geographicExtension.getLatitudeRangeDegree())*coverageH);

        // invert y.***
        pixelY = coverageH - pixelY - 1;

        // sample tiff data with at pixel coordinate
        double[] rasterData = new double[1];
        if(this.raster == null)
        {
            this.raster = this.coverage.getRenderedImage().getData();
        }
        this.raster.getPixel(pixelX, pixelY, rasterData);

        if(noDataContainer != null)
        {
            double nodata = noDataContainer.getAsSingleValue();
            if(rasterData[0] == nodata)
            {
                return resultAltitude;
            }
        }

        resultAltitude = rasterData[0];

        // update min, max altitude.***
        minAltitude = Math.min(minAltitude, resultAltitude);
        maxAltitude = Math.max(maxAltitude, resultAltitude);

        return resultAltitude;

         */
    }

}
