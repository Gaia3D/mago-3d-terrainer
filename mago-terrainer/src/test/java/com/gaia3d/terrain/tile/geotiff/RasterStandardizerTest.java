package com.gaia3d.terrain.tile.geotiff;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Deprecated
class RasterStandardizerTest {

    @Disabled
    @Test
    void resample() throws TransformException {
        File inputFile = new File("G:\\workspace\\dem05-all-5186.tif"); // 5186
        File outputFile = new File("E:\\", inputFile.getName().replace(".tif", "-resampled.tif"));
        CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;
        RasterStandardizer rasterStandardizer = new RasterStandardizer();
    }
}