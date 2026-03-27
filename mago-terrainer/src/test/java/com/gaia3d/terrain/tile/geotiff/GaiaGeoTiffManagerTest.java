package com.gaia3d.terrain.tile.geotiff;

import com.gaia3d.command.LoggingConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.UUID;

@Slf4j
@Deprecated
class GaiaGeoTiffManagerTest {

    @Disabled
    @Test
    void reprojection() {
        LoggingConfiguration.initConsoleLogger();

        File inputFile = new File("D:\\dem30-5186\\dem30-5186-part.tif"); // 5186
        File outputFile = new File("D:\\temp", inputFile.getName());
        CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;
        RasterStandardizer reprojector = new RasterStandardizer();
        //GridCoverage2D source = reprojector.readGeoTiff(inputFile);
        //GridCoverage2D target = reprojector.wrap(source, targetCRS);
        //GridCoverage2D target = reprojector.affine(source, targetCRS);
        //GridCoverage2D target = reprojector.reproject(source, targetCRS);
        //GridCoverage2D target = reprojector.reproject2(source, targetCRS);

        List<GridCoverage2D> reprojectedTiles = null;
        reprojectedTiles.forEach(tile -> {
            File tileFile = new File(outputFile.getParent(), UUID.randomUUID() + ".tif");
            reprojector.writeGeotiff(tile, tileFile);
        });
    }
}