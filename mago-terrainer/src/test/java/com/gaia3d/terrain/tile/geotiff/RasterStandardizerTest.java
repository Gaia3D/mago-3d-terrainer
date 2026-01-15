package com.gaia3d.terrain.tile.geotiff;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.jupiter.api.Test;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;

import java.io.File;

class RasterStandardizerTest {

    @Test
    void resample() throws TransformException {
        File inputFile = new File("G:\\workspace\\dem05-all-5186.tif"); // 5186
        //File inputFile = new File("G:\\workspace\\lccdem.tif"); // lccdem.tif
        //File croppedFile = new File("G:\\workspace\\dem05-all-5186-cropped.tif");
        File outputFile = new File("E:\\", inputFile.getName().replace(".tif", "-resampled.tif"));
        CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;

        RasterStandardizer rasterStandardizer = new RasterStandardizer();

        //GridCoverage2D source = rasterStandardizer.readGeoTiff(inputFile);




        /*GridGeometry2D gridGeometry = source.getGridGeometry();
        int width = 8192;
        int height = 8192;
        ReferencedEnvelope tileEnvelope = new ReferencedEnvelope(gridGeometry.gridToWorld(new GridEnvelope2D(0, 0, width, height)), source.getCoordinateReferenceSystem());
        GridCoverage2D crop = rasterStandardizer.crop(source, tileEnvelope);*/

        //rasterStandardizer.writeGeotiff(crop, croppedFile);
        //crop = null;
        //crop = rasterStandardizer.readGeoTiff(croppedFile);

        //GridCoverage2D coverage2D = rasterStandardizer.resample(source, targetCRS);
        //rasterStandardizer.writeGeotiff(coverage2D, outputFile);
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