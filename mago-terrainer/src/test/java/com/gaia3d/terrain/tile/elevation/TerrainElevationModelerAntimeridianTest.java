package com.gaia3d.terrain.tile.elevation;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.command.LoggingConfiguration;
import com.gaia3d.terrain.structure.GeographicExtension;
import com.gaia3d.terrain.types.InterpolationType;
import com.gaia3d.terrain.types.PriorityType;
import org.eclipse.imagen.RasterFactory;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.joml.Vector2i;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TerrainElevationModelerAntimeridianTest {

    static {
        LoggingConfiguration.initConsoleLogger();
    }

    private TerrainElevationModeler modeler;

    @BeforeEach
    void setUp() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setInterpolationType(InterpolationType.BILINEAR);
        globalOptions.setNoDataValue(-9999.0);
        globalOptions.setPriorityType(PriorityType.RESOLUTION);
        modeler = new TerrainElevationModeler();
    }

    @Test
    @Tag("default")
    void seamElevationsMatchAcrossAntimeridianForGlobalRaster() {
        // One raster covering the full globe. Without the wrap, lon = -180 samples the
        // first pixel column (100) and lon = +180 samples the last one (400), so the two
        // seam edges of the terrain end up with different elevations.
        TerrainElevationData globalRaster = createElevationData(
                new float[]{100.0f, 200.0f, 300.0f, 400.0f}, -180.0, 180.0);
        List<TerrainElevationData> dataList = registerQuadTree(globalRaster);

        double westSeam = modeler.getAntimeridianElevation(0.0, dataList);
        double eastSeam = modeler.getAntimeridianElevation(0.0, dataList);

        assertEquals(westSeam, eastSeam, 1.0e-9);
        assertEquals(250.0, westSeam, 1.0e-6,
                "Seam elevation must be the blend of both edge pixel columns");
    }

    @Test
    @Tag("default")
    void interiorSamplesAreNotAffectedByWrap() {
        TerrainElevationData globalRaster = createElevationData(
                new float[]{100.0f, 200.0f, 300.0f, 400.0f}, -180.0, 180.0);
        List<TerrainElevationData> dataList = registerQuadTree(globalRaster);

        // lon -90 : bilinear between column 0 (100) and column 1 (200), no wrap involved
        assertEquals(150.0, modeler.getElevation(-90.0, 0.0, dataList), 1.0e-6);
        assertEquals(100.0, modeler.getElevation(-180.0, 0.0, dataList), 1.0e-6,
                "Ordinary sampling must preserve the source DEM edge");
        assertEquals(400.0, modeler.getElevation(180.0, 0.0, dataList), 1.0e-6,
                "Antimeridian matching belongs only to the raster frontier post-process");
    }

    @Test
    @Tag("default")
    void seamElevationsMatchForFragmentedRasters() {
        // The globe is covered by separate fragments : the west fragment ends its coverage
        // at the seam from the -180 side, the east fragment from the +180 side.
        TerrainElevationData westFragment = createElevationData(
                new float[]{50.0f, 70.0f}, -180.0, -90.0);
        TerrainElevationData eastFragment = createElevationData(
                new float[]{10.0f, 30.0f}, 90.0, 180.0);
        List<TerrainElevationData> dataList = registerQuadTree(westFragment, eastFragment);

        double westSeam = modeler.getAntimeridianElevation(0.0, dataList);
        double eastSeam = modeler.getAntimeridianElevation(0.0, dataList);

        assertEquals(westSeam, eastSeam, 1.0e-9,
                "Elevations at lon -180 and +180 must be identical for a seamless globe");
        // blend of the west fragment first column (50) and the east fragment last column (30)
        assertEquals(40.0, westSeam, 1.0e-6);
    }

    @Test
    @Tag("default")
    void wrappedSideFillsMissingDataAtSeam() {
        // Data exists only on the +180 side. Sampling at lon -180 used to return 0
        // (a cliff at the seam) : the wrap must pick up the east-side value instead.
        TerrainElevationData eastFragment = createElevationData(
                new float[]{10.0f, 30.0f}, 90.0, 180.0);
        List<TerrainElevationData> dataList = registerQuadTree(eastFragment);

        assertEquals(30.0, modeler.getAntimeridianElevation(0.0, dataList), 1.0e-6);
    }

    @Test
    @Tag("default")
    void fallsBackToClosestSamplesWhenRasterStopsWithinOnePixelOfSeam() {
        TerrainElevationData westInset = createElevationData(
                new float[]{20.0f, 40.0f}, -179.75, -179.0);
        TerrainElevationData eastInset = createElevationData(
                new float[]{60.0f, 80.0f}, 179.0, 179.75);
        List<TerrainElevationData> dataList = registerQuadTree(westInset, eastInset);

        assertEquals(50.0, modeler.getAntimeridianElevation(0.0, dataList), 1.0e-6,
                "The closest edge samples must be blended when both rasters stop short of the seam");
    }

    private List<TerrainElevationData> registerQuadTree(TerrainElevationData... elevationDataArray) {
        TerrainElevationDataQuadTree root = new TerrainElevationDataQuadTree(null);
        List<TerrainElevationData> dataList = new ArrayList<>();
        for (TerrainElevationData elevationData : elevationDataArray) {
            root.addTerrainElevationData(elevationData);
            dataList.add(elevationData);
        }
        root.makeQuadTree(4);
        modeler.setRootTerrainElevationDataQuadTree(root);
        return dataList;
    }

    private TerrainElevationData createElevationData(float[] columnValues, double minLon, double maxLon) {
        // two identical rows so the latitude interpolation at lat 0 is value-neutral
        int width = columnValues.length;
        int height = 2;
        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, width, height, 1, null);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.setSample(x, y, 0, columnValues[x]);
            }
        }
        ReferencedEnvelope envelope = new ReferencedEnvelope(minLon, maxLon, -90.0, 90.0, DefaultGeographicCRS.WGS84);
        GridCoverage2D coverage = new GridCoverageFactory().create("dem", raster, envelope);

        TerrainElevationData elevationData = new TerrainElevationData(modeler);
        elevationData.setCoverage(coverage);
        elevationData.setGridCoverage2DSize(new Vector2i(width, height));
        GeographicExtension extension = new GeographicExtension();
        extension.setDegrees(minLon, -90.0, 0.0, maxLon, 90.0, 0.0);
        elevationData.setGeographicExtension(extension);
        return elevationData;
    }
}
