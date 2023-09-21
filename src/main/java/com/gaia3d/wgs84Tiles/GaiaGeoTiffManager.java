package com.gaia3d.wgs84Tiles;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverageWriter;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.Envelope2D;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;

import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GaiaGeoTiffManager
{
     public GaiaGeoTiffManager()
    {
        System.out.println("GaiaGeoTiffManager.constructor()");
    }

    public GridCoverage2D loadGeoTiffGridCoverage2D(String geoTiffFilePath) throws IOException {
         // this function only loads the geotiff coverage.***
        System.out.println("GaiaGeoTiffManager.loadGeoTiffCoverage2D()");
        File file = new File(geoTiffFilePath);
        GeoTiffReader reader = new GeoTiffReader(file);
        GridCoverage2D coverage = reader.read(null);
        return coverage;
    }

    public void resiseGeoTiff_test(String inputFilePath, String outputFilePath, int width, int height) throws IOException, IOException {
        // https://www.javatips.net/api/org.geotools.coverage.processing.operations
        System.out.println("GeoTiffReader.resizeGeoTiff()");
        File inputFile = new File(inputFilePath); //path
        File outputFile = new File(outputFilePath);
        GeoTiffReader reader = new GeoTiffReader(inputFile);
        GridCoverage2D coverage = reader.read(null);
        GridGeometry gridGeometry = coverage.getGridGeometry();
        GridGeometry2D gridGeometry2D = (GridGeometry2D)gridGeometry;
        RenderedImage originalImage = coverage.getRenderedImage();

        Envelope envelopeOriginal = coverage.getEnvelope();
        int gridSpanX = gridGeometry.getGridRange().getSpan(0); // num of pixels.***
        int gridSpanY = gridGeometry.getGridRange().getSpan(1); // num of pixels.***
        double envelopeSpanX = envelopeOriginal.getSpan(0); // in meters.***
        double envelopeSpanY = envelopeOriginal.getSpan(1); // in meters.***
        double pixelSizeX = envelopeSpanX / gridSpanX;
        double pixelSizeY = envelopeSpanY / gridSpanY;
        int imageWidth = gridGeometry.getGridRange().getHigh(0);
        int imageHeight = gridGeometry.getGridRange().getHigh(1);

        double desiredPixelSizeX = 30.0;
        double desiredPixelSizeY = 30.0;
        double desiredPixelsCountX = envelopeSpanX / desiredPixelSizeX;
        double desiredPixelsCountY = envelopeSpanY / desiredPixelSizeY;
        int desiredImageWidth = (int)(envelopeSpanX / desiredPixelSizeX);
        int desiredImageHeight = (int)(envelopeSpanY / desiredPixelSizeY);

        double scaleX = gridSpanX / desiredPixelsCountX;
        double scaleY = gridSpanY / desiredPixelsCountY;

        double[] originalUpperLeftCorner = new double[2];
        originalUpperLeftCorner[0] = envelopeOriginal.getMinimum(0);
        originalUpperLeftCorner[1] = envelopeOriginal.getMinimum(1);

        int dataType = originalImage.getSampleModel().getDataType();
        WritableRaster newRaster = originalImage.getData().createCompatibleWritableRaster(desiredImageWidth, desiredImageHeight);

        Raster originalImageData = originalImage.getData();
        for(int y=0; y<desiredImageHeight; y++)
        {
            for(int x=0; x<desiredImageWidth; x++)
            {
                double originalPosX = x * scaleX;
                double originalPosY = y * scaleY;

                int originalX = (int)(originalPosX);
                int originalY = (int)(originalPosY);

                //int sample = originalImageData.getSample(originalX, originalY, 0);
                int pixel[] = new int[1];
                originalImageData.getPixel(originalX, originalY, pixel);
                newRaster.setPixel(x, y, pixel);

                int hola = 0;
            }
        }

        GridCoverageFactory factory = new GridCoverageFactory();
        GridCoverage2D newCoverage = factory.create("newCoverage", newRaster, envelopeOriginal);

        // now save the newCoverage as geotiff.***
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        GeoTiffWriter writer = new GeoTiffWriter(outputStream);
        writer.write(newCoverage, null);
        writer.dispose();


        int hola = 0;
    }
}
