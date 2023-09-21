package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GeographicExtension;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.joml.Vector2d;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

public class GaiaGeoTiffUtils
{
    public static Vector2d getLongitudeLatitudeDegree(GridCoverage2D coverage, int coordX, int coordY, GeometryFactory gf, MathTransform targetToWgs) throws TransformException {
        GridCoordinates2D coord = new GridCoordinates2D(coordX, coordY);
        DirectPosition p = coverage.getGridGeometry().gridToWorld(coord);
        Point point = gf.createPoint(new Coordinate(p.getOrdinate(0), p.getOrdinate(1)));
        Geometry wgsP = (Geometry) JTS.transform(point, targetToWgs);
        Vector2d lonLat = new Vector2d(wgsP.getCentroid().getCoordinate().x, wgsP.getCentroid().getCoordinate().y);
        return lonLat;
    }

    public static Vector2d getPixelSizeMeters(GridCoverage2D coverage) {
        Envelope envelopeOriginal = coverage.getEnvelope();
        GridGeometry gridGeometry = coverage.getGridGeometry();
        int gridSpanX = gridGeometry.getGridRange().getSpan(0); // num of pixels.***
        int gridSpanY = gridGeometry.getGridRange().getSpan(1); // num of pixels.***
        double envelopeSpanX = envelopeOriginal.getSpan(0); // in meters.***
        double envelopeSpanY = envelopeOriginal.getSpan(1); // in meters.***
        double pixelSizeX = envelopeSpanX / gridSpanX;
        double pixelSizeY = envelopeSpanY / gridSpanY;
        return new Vector2d(pixelSizeX, pixelSizeY);
    }

    public static GeographicExtension getGeographicExtension(GridCoverage2D coverage, GeometryFactory gf, MathTransform targetToWgs, GeographicExtension resultGeoExtension) throws TransformException {
        // get geographic extension.***
        GridEnvelope gridRange2D = coverage.getGridGeometry().getGridRange();
        Envelope envelope = coverage.getEnvelope();

        int gridLow0 = gridRange2D.getLow(0);
        int gridLow1 = gridRange2D.getLow(1);
        int gridHigh0 = gridRange2D.getHigh(0);
        int gridHigh1 = gridRange2D.getHigh(1);

        // gridLow0, gridLow1.***
        Vector2d lonLat_LU = GaiaGeoTiffUtils.getLongitudeLatitudeDegree(coverage, gridLow0, gridLow1, gf, targetToWgs);

        // gridHigh0, gridHigh1.***
        Vector2d lonLat_RD = GaiaGeoTiffUtils.getLongitudeLatitudeDegree(coverage, gridHigh0, gridHigh1, gf, targetToWgs);

        // gridLow0, gridHigh1.***
        Vector2d lonLat_LD = GaiaGeoTiffUtils.getLongitudeLatitudeDegree(coverage, gridLow0, gridHigh1, gf, targetToWgs);

        // gridHigh0, gridLow1.***
        Vector2d lonLat_RU = GaiaGeoTiffUtils.getLongitudeLatitudeDegree(coverage, gridHigh0, gridLow1, gf, targetToWgs);

        double minLon = Math.min(lonLat_LU.x, lonLat_RD.x);
        minLon = Math.min(minLon, lonLat_LD.x);
        minLon = Math.min(minLon, lonLat_RU.x);

        double maxLon = Math.max(lonLat_LU.x, lonLat_RD.x);
        maxLon = Math.max(maxLon, lonLat_LD.x);
        maxLon = Math.max(maxLon, lonLat_RU.x);

        double minLat = Math.min(lonLat_LU.y, lonLat_RD.y);
        minLat = Math.min(minLat, lonLat_LD.y);
        minLat = Math.min(minLat, lonLat_RU.y);

        double maxLat = Math.max(lonLat_LU.y, lonLat_RD.y);
        maxLat = Math.max(maxLat, lonLat_LD.y);
        maxLat = Math.max(maxLat, lonLat_RU.y);

        if(resultGeoExtension == null)
            resultGeoExtension = new GeographicExtension();

        resultGeoExtension.setDegrees(minLon, minLat, 0.0, maxLon, maxLat, 0.0);


        return resultGeoExtension;
    }

    public static GeographicExtension getGeographicExtension_v2(GridCoverage2D coverage, GeometryFactory gf, MathTransform targetToWgs, GeographicExtension resultGeoExtension) throws TransformException {
        // get geographic extension.***
        double lonMin = coverage.getEnvelope().getMinimum(0);
        double lonMax = coverage.getEnvelope().getMaximum(0);
        double latMin = coverage.getEnvelope().getMinimum(1);
        double latMax = coverage.getEnvelope().getMaximum(1);

        Point pointMin = gf.createPoint(new Coordinate(lonMin, latMin));
        Geometry wgsPMin = (Geometry) JTS.transform(pointMin, targetToWgs);

        Point pointMax = gf.createPoint(new Coordinate(lonMax, latMax));
        Geometry wgsPMax = (Geometry) JTS.transform(pointMax, targetToWgs);

        if(resultGeoExtension == null)
            resultGeoExtension = new GeographicExtension();

        resultGeoExtension.setDegrees(wgsPMin.getCentroid().getCoordinate().x, wgsPMin.getCentroid().getCoordinate().y, 0.0, wgsPMax.getCentroid().getCoordinate().x, wgsPMax.getCentroid().getCoordinate().y, 0.0);
        return resultGeoExtension;
    }
}
