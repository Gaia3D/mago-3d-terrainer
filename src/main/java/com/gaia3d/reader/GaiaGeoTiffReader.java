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
}
