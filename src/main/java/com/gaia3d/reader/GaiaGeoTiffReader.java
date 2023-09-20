package com.gaia3d.reader;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;

import java.io.File;
import java.io.IOException;

public class GaiaGeoTiffReader {

    public GridCoverage2D read(String path) {
        System.out.println("GeoTiffReader.read()");
        File file = new File(path); //path
        //AbstractGridFormat format = GridFormatFinder.findFormat(file);

        try {

            //AbstractGridCoverage2DReader reader = format.getReader(file);
            //GridCoverage2D coverage = (GridCoverage2D) reader.read(null);

            GeoTiffReader reader = new GeoTiffReader(file);
            GridCoverage2D coverage = reader.read(null);

            return coverage;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void resizeGeoTiff(String inputFilePath, String outputFilePath, int width, int height) throws IOException {
        // https://www.javatips.net/api/org.geotools.coverage.processing.operations
        System.out.println("GeoTiffReader.resizeGeoTiff()");
        File inputFile = new File(inputFilePath); //path
        File outputFile = new File(outputFilePath);
        GeoTiffReader reader = new GeoTiffReader(inputFile);
        GridCoverage2D coverage = reader.read(null);
        GridGeometry gridGeometry = coverage.getGridGeometry();

        Envelope envelope = coverage.getEnvelope();
        double pixelSizeX = envelope.getSpan(0) / gridGeometry.getGridRange().getSpan(0);
        double pixelSizeY = envelope.getSpan(1) / gridGeometry.getGridRange().getSpan(1);
        int imageWidth = gridGeometry.getGridRange().getHigh(0);
        int imageHeight = gridGeometry.getGridRange().getHigh(1);

        int hola = 0;



    }
}
