package com.gaia3d.terrain.tile.geotiff;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class RasterStandardizerTest {

    @Test
    void resample() {
        File inputFile = new File("E:\\resample\\input\\dem05-parts-5186-crop.tif"); // 5186
        File outputFile = new File("E:\\resample\\output", inputFile.getName());
        CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;

        RasterStandardizer rasterStandardizer = new RasterStandardizer();
        RasterStandardizer reprojector = new RasterStandardizer();
        GridCoverage2D source = reprojector.readGeoTiff(inputFile);

        GridCoverage2D coverage2D = rasterStandardizer.resample(source, targetCRS);
        rasterStandardizer.writeGeotiff(coverage2D, outputFile);
    }


    /*
            final ParameterValueGroup param =(ParameterValueGroup) processor.getOperation("Resample").getParameters();
            param.parameter("Source").setValue(coverage);
            param.parameter("CoordinateReferenceSystem").setValue(targetCRS);
            param.parameter("GridGeometry").setValue(null);
            param.parameter("InterpolationType").setValue(interpolation);
            coverage = (GridCoverage2D) ((Resample) processor.getOperation("Resample")).doOperation(param, hints);
     */
}