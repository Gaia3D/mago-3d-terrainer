package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GeographicExtension;
import com.gaia3d.reader.GaiaGeoTiffReader;
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
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.awt.image.Raster;


public class TerrainElevationData {

    // the terrain elevation data is stored in a geotiff file.***

    public String geotiffFilePath = "";
    public GeographicExtension geographicExtension = new GeographicExtension();

    GridCoverage2D coverage = null;

    Raster raster = null;

    double minAltitude = Double.MAX_VALUE;
    double maxAltitude = Double.MIN_VALUE;

    public void loadGeoTiffFile(String geotiffFilePath) throws FactoryException, TransformException {
        // load the geotiff file.***
        this.geotiffFilePath = geotiffFilePath;

        // create coverage.***
        GaiaGeoTiffReader reader = new GaiaGeoTiffReader();

        this.coverage = reader.read(this.geotiffFilePath);
        this.raster = this.coverage.getRenderedImage().getData();

        GridEnvelope gridRange2D = coverage.getGridGeometry().getGridRange();
        Envelope envelope = coverage.getEnvelope();

        CoordinateReferenceSystem crsTarget = coverage.getCoordinateReferenceSystem2D();
        CoordinateReferenceSystem crsWgs84 = CRS.decode("EPSG:4326", true);

        GeometryFactory gf = new GeometryFactory();
        MathTransform targetToWgs = CRS.findMathTransform(crsTarget, crsWgs84);

        GridCoordinates2D coord = new GridCoordinates2D(1, 1);
        DirectPosition p = coverage.getGridGeometry().gridToWorld(coord);
        Point point = gf.createPoint(new Coordinate(p.getOrdinate(0), p.getOrdinate(1)));
        Geometry wgsP = (Geometry) JTS.transform(point, targetToWgs);
        double maxLon = wgsP.getCentroid().getCoordinate().x;
        double minLat = wgsP.getCentroid().getCoordinate().y;

        coord = new GridCoordinates2D(0, 0);
        p = coverage.getGridGeometry().gridToWorld(coord);
        point = gf.createPoint(new Coordinate(p.getOrdinate(0), p.getOrdinate(1)));
        wgsP = (Geometry) JTS.transform(point, targetToWgs);
        double minLon = wgsP.getCentroid().getCoordinate().x;
        double maxLat = wgsP.getCentroid().getCoordinate().y;

        this.geographicExtension.setDegrees(minLon, minLat, 0.0, maxLon, maxLat, 0.0);

        int hola = 0;
    }

    public Vector2d getPixelSizeDegree()
    {
        double imageWidth = this.coverage.getRenderedImage().getWidth();
        double imageHeight = this.coverage.getRenderedImage().getHeight();
        double longitudeRange = this.geographicExtension.getLongitudeRangeDegree();
        double latitudeRange = this.geographicExtension.getLatitudeRangeDegree();
        double pixelSizeX = longitudeRange / imageWidth;
        double pixelSizeY = latitudeRange / imageHeight;
        Vector2d pixelSize = new Vector2d(pixelSizeX, pixelSizeY);
        return pixelSize;
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

    public double getElevation(double lonDeg, double latDeg) throws TransformException {
        double resultAltitude = 0.0;

        // 1rst check if lon, lat intersects with geoExtension.***
        if(!this.geographicExtension.intersects(lonDeg, latDeg))
        {
            return resultAltitude;
        }

        // https://taylor.callsen.me/parsing-geotiff-files-in-java/
        CoordinateReferenceSystem wgs84 = DefaultGeographicCRS.WGS84;
        GridGeometry2D gg = coverage.getGridGeometry();
        double nodata = CoverageUtilities.getNoDataProperty(coverage).getAsSingleValue();
        DirectPosition2D posWorld = new DirectPosition2D(wgs84, lonDeg, latDeg); // longitude supplied first
        GridCoordinates2D posGrid = gg.worldToGrid(posWorld);

        int coverageW = coverage.getRenderedImage().getWidth();
        int coverageH = coverage.getRenderedImage().getHeight();
        int pixelX = (int)((lonDeg - this.geographicExtension.getMinLongitudeDeg())/(this.geographicExtension.getLongitudeRangeDegree())*coverageW);
        int pixelY = (int)((latDeg - this.geographicExtension.getMinLatitudeDeg())/(this.geographicExtension.getLatitudeRangeDegree())*coverageH);

        // sample tiff data with at pixel coordinate
        double[] rasterData = new double[1];
        //this.raster.getPixel(posGrid.x, posGrid.y, rasterData);
        this.raster.getPixel(pixelX, pixelY, rasterData);
        if(rasterData[0] == nodata)
        {
            return resultAltitude;
        }
        resultAltitude = rasterData[0];

        // update min, max altitude.***
        minAltitude = Math.min(minAltitude, resultAltitude);
        maxAltitude = Math.max(maxAltitude, resultAltitude);

        return resultAltitude;
    }

}
