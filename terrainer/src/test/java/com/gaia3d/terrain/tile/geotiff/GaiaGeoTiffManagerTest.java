package com.gaia3d.terrain.tile.geotiff;

import com.gaia3d.command.Configurator;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.referencing.operation.transform.WarpTransform2D;
import org.geotools.util.factory.Hints;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;

import javax.media.jai.Warp;
import javax.media.jai.WarpAffine;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;

@Slf4j
class GaiaGeoTiffManagerTest {

    @Test
    void warp() {
        Configurator.initConsoleLogger();

        File inputFile = new File("D:\\dem30-5186\\dem30-5186-part.tif"); // 5186
        //File inputFile = new File("D:\\dem05-5186-part-real\\dem05-parts-5186.tif"); // 5186
        //File inputFile = new File("D:\\dem05-5186\\dem05.tif"); // 5186
        File outputFile = new File("D:\\result-01.tif"); // 4326

        GridCoverage2D gridCoverage2D = null;
        try {
            GeoTiffReader geoTiffReader = new GeoTiffReader(inputFile);
            gridCoverage2D = geoTiffReader.read(null);
        } catch (IOException e) {
            log.error("Failed to read GeoTiff file: {}", inputFile);
            log.error("Exception: ", e);
            throw new RuntimeException(e);
        }

        // Reprojection
        // GridCoverage2D

        CoordinateReferenceSystem sourceCRS = gridCoverage2D.getCoordinateReferenceSystem();
        CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;
        try {
            targetCRS = CRS.decode("EPSG:3857");
        } catch (FactoryException e) {
            log.error("Failed to decode EPSG:4326", e);
            throw new RuntimeException(e);
        }


        // Warp를 사용한 재투영
        Operations ops = Operations.DEFAULT;
        //
        GridGeometry2D gridGeometry2D = gridCoverage2D.getGridGeometry();
        GridEnvelope2D gridEnvelope2D = gridGeometry2D.getGridRange2D();

        //WarpTransform2DProvider warpTransform2DProvider = new WarpTransform2DProvider();
        //warpTransform2DProvider.
        //WarpTransform2D warpTransform2D = new WarpTransform2D(gridEnvelope2D, gridEnvelope2D);
        //ops.warp(gridCoverage2D, )

        /*WarpTransform2D warpTransform = null;
        MathTransform2D transform = null;
        Warp warp = new WarpAffine(new AffineTransform());
        try {
            transform = (MathTransform2D) CRS.findMathTransform(sourceCRS, targetCRS, true);
            warp = WarpTransform2D.getWarp(null, transform);
            warpTransform = WarpTransform2D.create(warp);
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }

        AffineTransform affineTransform = new AffineTransform();
        AffineTransform2D affineTransform2D = new AffineTransform2D(affineTransform);
        affineTransform2D = new AffineTransform2D(affineTransform2D);

        *//*AffineTransform affineTransform = new AffineTransform();
        AffineTransform2D affineTransform2D = new AffineTransform2D(affineTransform);
        affineTransform2D = new AffineTransform2D(affineTransform2D);
        //warp = new WarpTransform2D();*//*

        warp = new WarpAffine(affineTransform2D);*/

        //Warp warp = createWarp(sourceCRS, targetCRS);
        //GridCoverage2D targetCoverage = (GridCoverage2D) Operations.DEFAULT.warp(gridCoverage2D, warp);



        GridCoverage2D targetCoverage = (GridCoverage2D) Operations.DEFAULT.resample(gridCoverage2D, targetCRS);


        //separate(gridCoverage2D);
        /*ReferencedEnvelope bbox = new ReferencedEnvelope(sourceCRS);
        bbox.setBounds(gridCoverage2D.getEnvelope2D());
        boolean lenient = true;
        MathTransform transform = null;
        try {
            transform = CRS.findMathTransform(sourceCRS, targetCRS, lenient);
            ReferencedEnvelope res = new ReferencedEnvelope(JTS.transform(bbox, transform), targetCRS);
            Operations ops = Operations.DEFAULT;
            //targetCoverage = (GridCoverage2D) ops.resample(gridCoverage2D, targetCRS);
            targetCoverage = (GridCoverage2D) ops.resample(gridCoverage2D, res, Interpolation.getInstance(Interpolation.INTERP_NEAREST));
        } catch (FactoryException | TransformException e) {
            log.error("Failed to transform CRS", e);
            throw new RuntimeException(e);
        }*/

        try {
            Hints hint = new Hints();
            hint.put(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, DefaultGeographicCRS.WGS84);
            hint.put(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);

            GeoTiffWriter writer = new GeoTiffWriter(outputFile, hint);
            GeoTiffWriteParams params = new GeoTiffWriteParams();

            log.info("Writing GeoTiff file: {}", outputFile);
            writer.write(targetCoverage, null);
            log.info("GeoTiff file written: {}", outputFile);
            writer.dispose();
            log.info("GeoTiff writer disposed");
        } catch (IOException e) {
            log.error("Failed to write GeoTiff file: {}", outputFile);
            log.error("Exception: ", e);
            throw new RuntimeException(e);
        }
    }

    Warp createWarp(CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS) {
        // 좌표 변환(MathTransform) 생성
        MathTransform2D transform = null;
        try {
            transform = (MathTransform2D) CRS.findMathTransform(sourceCRS, targetCRS, true);
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
        return WarpTransform2D.getWarp(null, transform);
    }
}