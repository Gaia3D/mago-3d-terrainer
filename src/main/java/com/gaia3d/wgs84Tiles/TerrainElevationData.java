package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GeographicExtension;
import com.gaia3d.reader.GeoTiffReader;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
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

public class TerrainElevationData {

    // the terrain elevation data is stored in a geotiff file.***

    public String geotiffFilePath = "";
    public GeographicExtension geographicExtension = new GeographicExtension();

    GridCoverage2D coverage = null;

    public void loadGeoTiffFile(String geotiffFilePath) throws FactoryException, TransformException {
        // load the geotiff file.***
        this.geotiffFilePath = geotiffFilePath;

        // create coverage.***
        GeoTiffReader reader = new GeoTiffReader();
        this.coverage = reader.read(this.geotiffFilePath);

        GridEnvelope gridRange2D = coverage.getGridGeometry().getGridRange();
        Envelope envelope = coverage.getEnvelope();

        CoordinateReferenceSystem crsTarget = coverage.getCoordinateReferenceSystem2D();
        CoordinateReferenceSystem crsWgs84= CRS.decode("EPSG:4326", true);

        GeometryFactory gf = new GeometryFactory();
        MathTransform targetToWgs = CRS.findMathTransform(crsTarget, crsWgs84);

        GridCoordinates2D coord = new GridCoordinates2D(0, 0);
        DirectPosition p = coverage.getGridGeometry().gridToWorld(coord);
        Point point = gf.createPoint(new Coordinate(p.getOrdinate(0), p.getOrdinate(1)));
        Geometry wgsP = (Geometry) JTS.transform(point, targetToWgs);
        double maxLon = wgsP.getCentroid().getCoordinate().x;
        double minLat = wgsP.getCentroid().getCoordinate().y;

        coord = new GridCoordinates2D(1, 1);
        p = coverage.getGridGeometry().gridToWorld(coord);
        point = gf.createPoint(new Coordinate(p.getOrdinate(0), p.getOrdinate(1)));
        wgsP = (Geometry) JTS.transform(point, targetToWgs);
        double minLon = wgsP.getCentroid().getCoordinate().x;
        double maxLat = wgsP.getCentroid().getCoordinate().y;

        this.geographicExtension.setDegrees(minLon, minLat, 0.0, maxLon, maxLat, 0.0);

        int hola = 0;
    }

}
