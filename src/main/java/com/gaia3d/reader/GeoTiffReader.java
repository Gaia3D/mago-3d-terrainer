package com.gaia3d.reader;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

public class GeoTiffReader {

    public GridCoverage2D read(String path) {
        System.out.println("GeoTiffReader.read()");

        File file = new File(path); //path

        AbstractGridFormat format = GridFormatFinder.findFormat(file);
        //GridCoverage2DReader reader = format.getReader( file );

        try {

            //AbstractGridFormat format = GridFormatFinder.findFormat(file);
            //System.out.println("here we have format: "+format);

            AbstractGridCoverage2DReader reader = format.getReader(file);

            GridCoverage2D coverage = (GridCoverage2D) reader.read(null);
            //CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem2D();
            //Envelope env = coverage.getEnvelope();
            //RenderedImage image = coverage.getRenderedImage();
            //GridGeometry2D geometry = coverage.getGridGeometry();

            return coverage;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
