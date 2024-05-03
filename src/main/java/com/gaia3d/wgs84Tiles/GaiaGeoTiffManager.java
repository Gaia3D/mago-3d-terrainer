package com.gaia3d.wgs84Tiles;

import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
//import org.geotools.coverage.grid.Interpolator2D;
import org.geotools.coverage.grid.Interpolator2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;

import javax.media.jai.Interpolation;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Slf4j
public class GaiaGeoTiffManager {
    int[] memSave_pixel = new int[1];
    double[] memSave_originalUpperLeftCorner = new double[2];

    public GaiaGeoTiffManager() {
        log.debug("GaiaGeoTiffManager.constructor()");
    }

    public GridCoverage2D loadGeoTiffGridCoverage2D(String geoTiffFilePath) {

         // this function only loads the geotiff coverage.***
        //System.out.println("GaiaGeoTiffManager.loadGeoTiffCoverage2D()" + geoTiffFilePath);

        GridCoverage2D coverage = null;
        try {
            File file = new File(geoTiffFilePath);
            GeoTiffReader reader = new GeoTiffReader(file);
            coverage = reader.read(null);
//            Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
//            coverage = Interpolator2D.create(coverage, interpolation);
            log.debug("coverage: " + coverage);
            reader.dispose();
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return coverage;
    }

    public GridCoverage2D getResizedCoverage2D(GridCoverage2D originalCoverage, double desiredPixelSizeXinMeters, double desiredPixelSizeYinMeters) throws FactoryException {
        GridCoverage2D resizedCoverage = null;

        GridGeometry originalGridGeometry = originalCoverage.getGridGeometry();
        RenderedImage originalImage = originalCoverage.getRenderedImage();
        Envelope envelopeOriginal = originalCoverage.getEnvelope();

        int gridSpanX = originalGridGeometry.getGridRange().getSpan(0); // num of pixels.***
        int gridSpanY = originalGridGeometry.getGridRange().getSpan(1); // num of pixels.***
        double[] envelopeSpanMeters = new double[2];
        GaiaGeoTiffUtils.getEnvelopeSpanInMetersOfGridCoverage2D(originalCoverage, envelopeSpanMeters);
        double envelopeSpanX = envelopeSpanMeters[0]; // in meters.***
        double envelopeSpanY = envelopeSpanMeters[1]; // in meters.***

        double desiredPixelsCountX = envelopeSpanX / desiredPixelSizeXinMeters;
        double desiredPixelsCountY = envelopeSpanY / desiredPixelSizeYinMeters;
        int minXSize = 24;
        int minYSize = 24;
        int desiredImageWidth = Math.max((int) desiredPixelsCountX, minXSize);
        int desiredImageHeight = Math.max((int) desiredPixelsCountY, minYSize);


        double scaleX = (double) desiredImageWidth / (double) gridSpanX;
        double scaleY = (double) desiredImageHeight / (double) gridSpanY;

        Operations ops = new Operations(null);
        resizedCoverage = (GridCoverage2D) ops.scale(originalCoverage, scaleX, scaleY, 0, 0);

        memSave_originalUpperLeftCorner[0] = envelopeOriginal.getMinimum(0);
        memSave_originalUpperLeftCorner[1] = envelopeOriginal.getMinimum(1);

        return resizedCoverage;
    }


    public void saveGridCoverage2D(GridCoverage2D coverage, String outputFilePath) throws IOException {
        // now save the newCoverage as geotiff.***
        File outputFile = new File(outputFilePath);
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        GeoTiffWriter writer = new GeoTiffWriter(outputStream);
        writer.write(coverage, null);
        writer.dispose();
        outputStream.close();
    }
}
