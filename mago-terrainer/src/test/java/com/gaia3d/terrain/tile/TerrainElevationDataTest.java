package com.gaia3d.terrain.tile;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.structure.GeographicExtension;
import com.gaia3d.terrain.types.InterpolationType;
import org.eclipse.imagen.RasterFactory;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.joml.Vector2i;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainElevationDataTest {

    @Test
    @Tag("default")
    void getElevationUsesBilinearInterpolationOnInMemoryCoverage() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setInterpolationType(InterpolationType.BILINEAR);
        globalOptions.setNoDataValue(-9999.0);

        TerrainElevationData elevationData = new TerrainElevationData(null);
        elevationData.setCoverage(createCoverage(new float[][]{
                {0.0f, 10.0f},
                {20.0f, 30.0f}
        }));
        elevationData.setGridCoverage2DSize(new Vector2i(2, 2));
        elevationData.setGeographicExtension(createExtension());

        boolean[] intersects = new boolean[1];
        double elevation = elevationData.getElevation(0.5, 1.5, intersects);

        assertTrue(intersects[0]);
        assertEquals(15.0, elevation, 0.0001);
    }

    @Test
    @Tag("default")
    void getElevationClampsBoundaryCoordinates() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setInterpolationType(InterpolationType.BILINEAR);
        globalOptions.setNoDataValue(-9999.0);

        TerrainElevationData elevationData = new TerrainElevationData(null);
        elevationData.setCoverage(createCoverage(new float[][]{
                {0.0f, 10.0f},
                {20.0f, 30.0f}
        }));
        elevationData.setGridCoverage2DSize(new Vector2i(2, 2));
        elevationData.setGeographicExtension(createExtension());

        boolean[] intersects = new boolean[1];
        double elevation = elevationData.getElevation(2.0, 0.0, intersects);

        assertTrue(intersects[0]);
        assertEquals(30.0, elevation, 0.0001);
    }

    private GridCoverage2D createCoverage(float[][] values) {
        int height = values.length;
        int width = values[0].length;
        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, width, height, 1, null);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.setSample(x, y, 0, values[y][x]);
            }
        }

        ReferencedEnvelope envelope = new ReferencedEnvelope(0.0, width, 0.0, height, DefaultGeographicCRS.WGS84);
        return new GridCoverageFactory().create("dem", raster, envelope);
    }

    private GeographicExtension createExtension() {
        GeographicExtension extension = new GeographicExtension();
        extension.setDegrees(0.0, 0.0, 0.0, 2.0, 2.0, 0.0);
        return extension;
    }
}
