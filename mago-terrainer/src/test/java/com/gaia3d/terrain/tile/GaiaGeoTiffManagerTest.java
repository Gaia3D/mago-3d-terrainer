package com.gaia3d.terrain.tile;

import com.gaia3d.command.Configurator;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.imagen.Interpolation;
import org.geotools.api.geometry.Position;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Position2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
class GaiaGeoTiffManagerTest {

    @Test
    void test() {
        Configurator.initConsoleLogger();
        GridCoverage2D coverage = null;
        try {
            File file = new File("C:\\Users\\znkim\\Downloads\\seoul-100.tif");
            GeoTiffReader reader = new GeoTiffReader(file);
            Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            coverage = (GridCoverage2D) Operations.DEFAULT.interpolate(reader.read(null), interpolation);
            double[] resolution = new double[2];

            Position worldPosition = new Position2D(DefaultGeographicCRS.WGS84, 126.977491, 37.659025);
            double[] altitude = new double[1];
            try {
                coverage.evaluate(worldPosition, altitude);
            } catch (Exception e) {
                log.error("Error : {}", e.getMessage());
                log.warn("Failed to evaluate terrain height", e);
            }

            log.info("Altitude: {}", altitude[0]);
            reader.dispose();
        } catch (Exception e) {
            log.error("Error:", e);
        }
    }

}