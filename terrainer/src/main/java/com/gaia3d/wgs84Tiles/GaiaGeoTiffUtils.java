package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GeographicExtension;
import com.gaia3d.util.GlobeUtils;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.joml.Vector2d;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

public class GaiaGeoTiffUtils {
    public static Vector2d getLongitudeLatitudeDegree(GridCoverage2D coverage, int coordX, int coordY, GeometryFactory gf, MathTransform targetToWgs) throws TransformException {
        GridCoordinates2D coord = new GridCoordinates2D(coordX, coordY);
        DirectPosition p = coverage.getGridGeometry().gridToWorld(coord);
        Point point = gf.createPoint(new Coordinate(p.getOrdinate(0), p.getOrdinate(1)));
        Geometry wgsP = JTS.transform(point, targetToWgs);
        Point centroid = wgsP.getCentroid();
        Coordinate coordinate = centroid.getCoordinate();
        return new Vector2d(coordinate.x, coordinate.y);
    }

    public static boolean isGridCoverage2DWGS84(GridCoverage2D coverage) throws FactoryException {
        // this function returns true if the coverage is wgs84
        CoordinateReferenceSystem crsTarget = coverage.getCoordinateReferenceSystem2D();
        CoordinateReferenceSystem crsWgs84 = DefaultGeographicCRS.WGS84;
        MathTransform targetToWgs = CRS.findMathTransform(crsTarget, crsWgs84);
        // The original src is wgs84
        // The original src is not wgs84
        return targetToWgs.isIdentity();
    }

    public static void getEnvelopeSpanInMetersOfGridCoverage2D(GridCoverage2D coverage, double[] resultEnvelopeSpanMeters) throws FactoryException {
        if (isGridCoverage2DWGS84(coverage)) {
            Envelope envelope = coverage.getEnvelope();
            double minLat = envelope.getMinimum(1);
            double maxLat = envelope.getMaximum(1);
            double midLat = (minLat + maxLat) / 2.0;
            double radius = GlobeUtils.getRadiusAtLatitude(midLat);
            double degToRadFactor = GlobeUtils.DEGREE_TO_RADIAN_FACTOR;

            Envelope envelopeOriginal = coverage.getEnvelope();
            // int degrees
            double envelopeSpanX = envelopeOriginal.getSpan(0);
            double envelopeSpanY = envelopeOriginal.getSpan(1);
            resultEnvelopeSpanMeters[0] = (envelopeSpanX * degToRadFactor) * radius;
            resultEnvelopeSpanMeters[1] = (envelopeSpanY * degToRadFactor) * radius;
        } else {
            Envelope envelopeOriginal = coverage.getEnvelope();
            resultEnvelopeSpanMeters[0] = envelopeOriginal.getSpan(0);
            resultEnvelopeSpanMeters[1] = envelopeOriginal.getSpan(1);
        }
    }

    public static Vector2d getPixelSizeMeters(GridCoverage2D coverage) throws FactoryException {
        double[] envelopeSpanInMeters = new double[2];
        getEnvelopeSpanInMetersOfGridCoverage2D(coverage, envelopeSpanInMeters);
        GridGeometry gridGeometry = coverage.getGridGeometry();
        int gridSpanX = gridGeometry.getGridRange().getSpan(0);
        int gridSpanY = gridGeometry.getGridRange().getSpan(1);
        double pixelSizeX = envelopeSpanInMeters[0] / gridSpanX;
        double pixelSizeY = envelopeSpanInMeters[1] / gridSpanY;
        return new Vector2d(pixelSizeX, pixelSizeY);
    }

    public static GeographicExtension getGeographicExtension(GridCoverage2D coverage, GeometryFactory gf, MathTransform targetToWgs, GeographicExtension resultGeoExtension) throws TransformException {
        // get geographic extension
        GridEnvelope gridRange2D = coverage.getGridGeometry().getGridRange();
        Envelope envelope = coverage.getEnvelope();

        double minLon = 0.0;
        double minLat = 0.0;
        double maxLon = 0.0;
        double maxLat = 0.0;

        // check if crsTarget is wgs84
        if (targetToWgs.isIdentity()) {
            // The original src is wgs84
            minLon = envelope.getMinimum(0);
            minLat = envelope.getMinimum(1);
            maxLon = envelope.getMaximum(0);
            maxLat = envelope.getMaximum(1);
        } else {
            GridGeometry originalGridGeometry = coverage.getGridGeometry();

            int gridSpanX = originalGridGeometry.getGridRange().getSpan(0);
            int gridSpanY = originalGridGeometry.getGridRange().getSpan(1);

            // gridLow0, gridLow1
            Vector2d lonLatLeftUp = GaiaGeoTiffUtils.getLongitudeLatitudeDegree(coverage, 0, gridSpanY - 1, gf, targetToWgs);

            // gridHigh0, gridHigh1
            Vector2d lonLatRightDown = GaiaGeoTiffUtils.getLongitudeLatitudeDegree(coverage, gridSpanX - 1, 0, gf, targetToWgs);

            // gridLow0, gridHigh1
            Vector2d lonLatLeftDown = GaiaGeoTiffUtils.getLongitudeLatitudeDegree(coverage, 0, 0, gf, targetToWgs);

            // gridHigh0, gridLow1
            Vector2d lonLatRightUp = GaiaGeoTiffUtils.getLongitudeLatitudeDegree(coverage, gridSpanX - 1, gridSpanY - 1, gf, targetToWgs);

            minLon = Math.min(lonLatLeftUp.x, lonLatRightDown.x);
            minLon = Math.min(minLon, lonLatLeftDown.x);
            minLon = Math.min(minLon, lonLatRightUp.x);

            maxLon = Math.max(lonLatLeftUp.x, lonLatRightDown.x);
            maxLon = Math.max(maxLon, lonLatLeftDown.x);
            maxLon = Math.max(maxLon, lonLatRightUp.x);

            minLat = Math.min(lonLatLeftUp.y, lonLatRightDown.y);
            minLat = Math.min(minLat, lonLatLeftDown.y);
            minLat = Math.min(minLat, lonLatRightUp.y);

            maxLat = Math.max(lonLatLeftUp.y, lonLatRightDown.y);
            maxLat = Math.max(maxLat, lonLatLeftDown.y);
            maxLat = Math.max(maxLat, lonLatRightUp.y);
        }

        if (resultGeoExtension == null) {
            resultGeoExtension = new GeographicExtension();
        }

        resultGeoExtension.setDegrees(minLon, minLat, 0.0, maxLon, maxLat, 0.0);
        return resultGeoExtension;
    }

    public static GeographicExtension getGeographicExtension_original(GridCoverage2D coverage, GeometryFactory gf, MathTransform targetToWgs, GeographicExtension resultGeoExtension) throws TransformException {
        // get geographic extension
        GridEnvelope gridRange2D = coverage.getGridGeometry().getGridRange();
        Envelope envelope = coverage.getEnvelope();

        double minLon = 0.0;
        double minLat = 0.0;
        double maxLon = 0.0;
        double maxLat = 0.0;

        // check if crsTarget is wgs84
        if (targetToWgs.isIdentity()) {
            // The original src is wgs84
            minLon = envelope.getMinimum(0);
            minLat = envelope.getMinimum(1);
            maxLon = envelope.getMaximum(0);
            maxLat = envelope.getMaximum(1);
        } else {
            int gridLow0 = gridRange2D.getLow(0);
            int gridLow1 = gridRange2D.getLow(1);
            int gridHigh0 = gridRange2D.getHigh(0);
            int gridHigh1 = gridRange2D.getHigh(1);

            // gridLow0, gridLow1
            Vector2d lonLatLeftUp = GaiaGeoTiffUtils.getLongitudeLatitudeDegree(coverage, gridLow0, gridLow1, gf, targetToWgs);

            // gridHigh0, gridHigh1
            Vector2d lonLatRightDown = GaiaGeoTiffUtils.getLongitudeLatitudeDegree(coverage, gridHigh0, gridHigh1, gf, targetToWgs);

            // gridLow0, gridHigh1
            Vector2d lonLatLeftDown = GaiaGeoTiffUtils.getLongitudeLatitudeDegree(coverage, gridLow0, gridHigh1, gf, targetToWgs);

            // gridHigh0, gridLow1
            Vector2d lonLatRightUp = GaiaGeoTiffUtils.getLongitudeLatitudeDegree(coverage, gridHigh0, gridLow1, gf, targetToWgs);

            minLon = Math.min(lonLatLeftUp.x, lonLatRightDown.x);
            minLon = Math.min(minLon, lonLatLeftDown.x);
            minLon = Math.min(minLon, lonLatRightUp.x);

            maxLon = Math.max(lonLatLeftUp.x, lonLatRightDown.x);
            maxLon = Math.max(maxLon, lonLatLeftDown.x);
            maxLon = Math.max(maxLon, lonLatRightUp.x);

            minLat = Math.min(lonLatLeftUp.y, lonLatRightDown.y);
            minLat = Math.min(minLat, lonLatLeftDown.y);
            minLat = Math.min(minLat, lonLatRightUp.y);

            maxLat = Math.max(lonLatLeftUp.y, lonLatRightDown.y);
            maxLat = Math.max(maxLat, lonLatLeftDown.y);
            maxLat = Math.max(maxLat, lonLatRightUp.y);
        }

        if (resultGeoExtension == null) {
            resultGeoExtension = new GeographicExtension();
        }
        resultGeoExtension.setDegrees(minLon, minLat, 0.0, maxLon, maxLat, 0.0);

        return resultGeoExtension;
    }

    public static GeographicExtension getGeographicExtension_v2(GridCoverage2D coverage, GeometryFactory gf, MathTransform targetToWgs, GeographicExtension resultGeoExtension) throws TransformException {
        // get geographic extension
        double lonMin = coverage.getEnvelope().getMinimum(0);
        double lonMax = coverage.getEnvelope().getMaximum(0);
        double latMin = coverage.getEnvelope().getMinimum(1);
        double latMax = coverage.getEnvelope().getMaximum(1);

        Point pointMin = gf.createPoint(new Coordinate(lonMin, latMin));
        Geometry wgsPMin = JTS.transform(pointMin, targetToWgs);

        Point pointMax = gf.createPoint(new Coordinate(lonMax, latMax));
        Geometry wgsPMax = JTS.transform(pointMax, targetToWgs);

        if (resultGeoExtension == null) {
            resultGeoExtension = new GeographicExtension();
        }
        resultGeoExtension.setDegrees(wgsPMin.getCentroid().getCoordinate().x, wgsPMin.getCentroid().getCoordinate().y, 0.0, wgsPMax.getCentroid().getCoordinate().x, wgsPMax.getCentroid().getCoordinate().y, 0.0);
        return resultGeoExtension;
    }
}
