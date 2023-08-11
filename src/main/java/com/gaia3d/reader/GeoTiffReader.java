package com.gaia3d.reader;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.opengis.geometry.Envelope;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

public class GeoTiffReader {

    public void read() {
        System.out.println("GeoTiffReader.read()");

        File file = new File("test.tiff"); //path

        AbstractGridFormat format = GridFormatFinder.findFormat(file);
        GridCoverage2DReader reader = format.getReader( file );

        try {
            GridCoverage2D coverage = (GridCoverage2D) reader.read(null);
            CoordinateReferenceSystem crs = (CoordinateReferenceSystem) coverage.getCoordinateReferenceSystem2D();
            Envelope env = coverage.getEnvelope();
            RenderedImage image = coverage.getRenderedImage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
