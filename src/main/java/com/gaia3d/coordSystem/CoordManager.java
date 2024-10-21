package com.gaia3d.coordSystem;

import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

public class CoordManager {
    public static ProjCoordinate transformToWGS84(CoordinateReferenceSystem source, ProjCoordinate coordinate) {
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem wgs84 = factory.createFromParameters("WGS84", "+proj=longlat +datum=WGS84 +no_defs");
        BasicCoordinateTransform transformer = new BasicCoordinateTransform(source, wgs84);
        ProjCoordinate result = new ProjCoordinate();
        transformer.transform(coordinate, result);
        return result;
    }

    public static ProjCoordinate transform(CoordinateReferenceSystem source, CoordinateReferenceSystem target, ProjCoordinate coordinate) {
        BasicCoordinateTransform transformer = new BasicCoordinateTransform(source, target);
        ProjCoordinate result = new ProjCoordinate();
        transformer.transform(coordinate, result);
        return transformToWGS84(source, coordinate);
    }

    public void convertLocationIndicesTo4326() throws FactoryException, TransformException {
        // convert location indices to 4326.
        // https://www.osgeo.kr/17
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem inputCrs = null;
        String proj = "+proj=tmerc +lat_0=38 +lon_0=127.5 +k=0.9996 +x_0=1000000 +y_0=2000000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43";
        if (proj != null && !proj.isEmpty()) {
            inputCrs = factory.createFromParameters("CUSTOM", proj);
        }

        CoordinateReferenceSystem crs4326 = null;


        //CoordinateReferenceSystem inputCrs = CRS.decode("+proj=tmerc +lat_0=38 +lon_0=127.5 +k=0.9996 +x_0=1000000 +y_0=2000000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43"); // korea 2000.
        //CoordinateReferenceSystem inputCrs = CRS.decode("EPSG:5179"); // UTMK.
        //CoordinateReferenceSystem crs4326 = CRS.decode("EPSG:4326");
        //MathTransform transform = CRS.findMathTransform(inputCrs, crs4326);

//        double[] srcPts = new double[2];
//        double[] dstPts = new double[2];
//        for (LocationIndex locationIndex : locationIndices.values())
//        {
//            srcPts[0] = locationIndex.centroidX;
//            srcPts[1] = locationIndex.centroidY;
//
//            //transform.transform(srcPts, 0, dstPts, 0, 1);
//            ProjCoordinate projCoordinate = new ProjCoordinate(srcPts[0], srcPts[1], 0.0);
//            ProjCoordinate result = transformToWGS84(inputCrs, projCoordinate);
//            locationIndex.latitudeDeg = result.y;
//            locationIndex.longitudeDeg = result.x;

//        }
    }
}
