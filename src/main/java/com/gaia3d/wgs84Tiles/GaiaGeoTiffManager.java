package com.gaia3d.wgs84Tiles;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.Interpolator2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;

import javax.media.jai.Interpolation;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class GaiaGeoTiffManager
{
    int memSave_pixel[] = new int[1];
    double[] memSave_originalUpperLeftCorner = new double[2];
     public GaiaGeoTiffManager()
    {
        System.out.println("GaiaGeoTiffManager.constructor()");
    }

    public GridCoverage2D loadGeoTiffGridCoverage2D(String geoTiffFilePath) {
         // this function only loads the geotiff coverage.***
        System.out.println("GaiaGeoTiffManager.loadGeoTiffCoverage2D()" + geoTiffFilePath);
        GridCoverage2D coverage = null;
        try
        {
            File file = new File(geoTiffFilePath);
            GeoTiffReader reader = new GeoTiffReader(file);
            coverage = reader.read(null);
            Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            coverage = Interpolator2D.create(coverage, interpolation);
            reader.dispose();
        }
        catch (Exception e)
        {
            e.printStackTrace();
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
        int desiredImageWidth = Math.max((int)desiredPixelsCountX, minXSize);
        int desiredImageHeight = Math.max((int)desiredPixelsCountY, minYSize);


        double scaleX = gridSpanX / desiredPixelsCountX;
        double scaleY = gridSpanY / desiredPixelsCountY;

        memSave_originalUpperLeftCorner[0] = envelopeOriginal.getMinimum(0);
        memSave_originalUpperLeftCorner[1] = envelopeOriginal.getMinimum(1);

        WritableRaster newRaster = null;
        Raster originalImageData = originalImage.getData();
        try {
            // now resaize the raster.***
            newRaster = originalImageData.createCompatibleWritableRaster(desiredImageWidth, desiredImageHeight);
        } catch (Exception e) {
            e.printStackTrace();  // Imprime la traza de la excepción para depuración.
            throw new RuntimeException("Error al crear el WritableRaster.", e);
        }

        for(int y=0; y<desiredImageHeight; y++)
        {
            for(int x=0; x<desiredImageWidth; x++)
            {
                double originalPosX = x * scaleX;
                double originalPosY = y * scaleY;

                int originalX = (int)(originalPosX);
                int originalY = (int)(originalPosY);

                if(originalX >= gridSpanX)
                    originalX = gridSpanX - 1;

                if(originalY >= gridSpanY)
                    originalY = gridSpanY - 1;

                originalImageData.getPixel(originalX, originalY, memSave_pixel);
                newRaster.setPixel(x, y, memSave_pixel);

                int hola = 0;
            }
        }

        GridCoverageFactory factory = new GridCoverageFactory();
        resizedCoverage = factory.create("resizedCoverage", newRaster, envelopeOriginal);

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
