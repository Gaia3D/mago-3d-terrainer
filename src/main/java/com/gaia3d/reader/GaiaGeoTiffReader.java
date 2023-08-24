package com.gaia3d.reader;

import it.geosolutions.imageio.pam.PAMDataset;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataDecoder;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.data.ServiceInfo;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.CRS;
import org.opengis.geometry.Envelope;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.sound.sampled.DataLine;
import java.awt.geom.AffineTransform;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
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
            /*
            GeoTiffIIOMetadataDecoder metadata =  reader.getMetadata();
            RenderedImage bandImage = coverage.getRenderedImage();
            Envelope env = coverage.getEnvelope();
            javax.imageio.metadata.IIOMetadataNode metadataNode = metadata.getRootNode();
            Raster raster = coverage.getRenderedImage().getData();

            double nodata = CoverageUtilities.getNoDataProperty(coverage).getAsSingleValue();

            int numBands = coverage.getNumSampleDimensions();
            String[] sampleDimensionNames = new String[numBands];
            for (int i = 0; i < numBands; i++) {
                GridSampleDimension dim = coverage.getSampleDimension(i);
                sampleDimensionNames[i] = dim.getDescription().toString();
            }

            GridGeometry2D geometry = coverage.getGridGeometry();
            String[] names = reader.getMetadataNames();
            ServiceInfo info = reader.getInfo();

            // TEST.*****************************************************************
            GridEnvelope2D ge2D = coverage.getGridGeometry().getGridRange2D();

            RenderedImage renderedImage = coverage.getRenderedImage();
            System.out.println("WIDTH : " + renderedImage.getWidth());
            System.out.println("HEIGHT : " + renderedImage.getHeight());

            int numberofBands = coverage.getNumSampleDimensions();
            System.out.println("BAND : " + numberofBands);

            GridGeometry2D gridGeometry2D = coverage.getGridGeometry();
            AffineTransform gridToWorld = (AffineTransform) gridGeometry2D.getGridToCRS2D();
            Envelope2D envelope2d = gridGeometry2D.getEnvelope2D();

            System.out.println("PIXEL SIZE X : " + gridToWorld.getScaleX());
            System.out.println("PIXEL SIZE Y : " + gridToWorld.getScaleY());

            GridCoverage2DReader gridCov2DReader = (GridCoverage2DReader) reader;

            double[] rasterData = new double[1];

            double minimum = Double.MAX_VALUE;
            double maximum = Double.MIN_VALUE;
            for (int tileX = 0; tileX < renderedImage.getNumXTiles(); tileX++) {
                for (int tileY = 0; tileY < renderedImage.getNumYTiles(); tileY++) {
                    Raster tileRs = renderedImage.getTile(tileX, tileY);

                    java.awt.Rectangle bounds = tileRs.getBounds();
                    for (int dy = bounds.y, drow = 0; drow < bounds.height; dy++, drow++) {
                        for (int dx = bounds.x, dcol = 0; dcol < bounds.width; dx++, dcol++) {
                            if (ge2D.contains(dx, dy)) {

                                double sampleVal = tileRs.getSampleDouble(dx, dy, 0);
                                if(nodata == sampleVal)
                                {
                                    continue;
                                }
                                minimum = Math.min(minimum, sampleVal);
                                maximum = Math.max(maximum, sampleVal);
                            }
                        }
                    }
                }
            }

            System.out.println("MIN_VALUE : " + minimum);
            System.out.println("MAX_VALUE : " + maximum);
            // TEST.*****************************************************************
            */
            return coverage;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
