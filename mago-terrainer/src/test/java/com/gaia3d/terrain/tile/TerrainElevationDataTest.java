package com.gaia3d.terrain.tile;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.command.LoggingConfiguration;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.*;
import java.util.List;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainElevationDataTest {
    private static final Logger log = LoggerFactory.getLogger(TerrainElevationDataTest.class);

    static {
        LoggingConfiguration.initConsoleLogger();
    }

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

    @Test
    @Tag("default")
    void getElevationDoesNotMaterializeFullRenderedImage() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setInterpolationType(InterpolationType.BILINEAR);
        globalOptions.setNoDataValue(-9999.0);

        GetDataFailingImage image = new GetDataFailingImage(new int[][]{
                {0, 10},
                {20, 30}
        });
        TerrainElevationData elevationData = new TerrainElevationData(null);
        elevationData.setCoverage(createCoverage(image));
        elevationData.setGridCoverage2DSize(new Vector2i(2, 2));
        elevationData.setGeographicExtension(createExtension());

        boolean[] intersects = new boolean[1];
        double elevation = elevationData.getElevation(0.5, 1.5, intersects);

        assertTrue(intersects[0]);
        assertEquals(15.0, elevation, 0.0001);
        assertTrue(image.getTileCalls > 0);
        assertEquals(0, image.getDataCalls);
    }

    @Test
    @Tag("load")
    void getElevationKeepsVirtualLargeRasterWorkingSetBounded() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setInterpolationType(InterpolationType.BILINEAR);
        globalOptions.setNoDataValue(-9999.0);

        VirtualTiledImage image = new VirtualTiledImage(65536, 65536, 256, 256);
        TerrainElevationData elevationData = new TerrainElevationData(null);
        elevationData.setCoverage(createCoverage(image));
        elevationData.setGridCoverage2DSize(new Vector2i(image.getWidth(), image.getHeight()));
        elevationData.setGeographicExtension(createExtension(image.getWidth(), image.getHeight()));

        boolean[] intersects = new boolean[1];
        int samples = 0;
        long maxEstimatedBytes = 0L;
        long maxUsedHeapBytes = usedHeapBytes();
        long startNanos = System.nanoTime();
        for (int pass = 0; pass < 8; pass++) {
            int yOffset = pass * 31;
            int xOffset = pass * 17;
            for (int y = yOffset; y < image.getHeight() - 1; y += 257) {
                for (int x = xOffset; x < image.getWidth() - 1; x += 263) {
                    elevationData.getElevation(x + 0.25, y + 0.25, intersects);
                    assertTrue(intersects[0]);
                    samples++;

                    if ((samples & 0x3fff) == 0) {
                        maxEstimatedBytes = Math.max(maxEstimatedBytes, elevationData.estimateRasterBytes());
                        maxUsedHeapBytes = Math.max(maxUsedHeapBytes, usedHeapBytes());
                    }
                }
            }
        }

        long tileBytes = (long) image.getTileWidth() * image.getTileHeight();
        maxEstimatedBytes = Math.max(maxEstimatedBytes, elevationData.estimateRasterBytes());
        maxUsedHeapBytes = Math.max(maxUsedHeapBytes, usedHeapBytes());
        long durationMillis = (System.nanoTime() - startNanos) / 1_000_000L;
        log.info("[VirtualRasterLoad] samples={}, getTileCalls={}, getDataCalls={}, maxEstimatedBytes={} KB, duration={} ms",
                samples, image.getTileCalls, image.getDataCalls, maxEstimatedBytes / 1024L, durationMillis);
        System.out.printf(
                "[VirtualRasterLoad] maxHeap=%d MB, samples=%d, getTileCalls=%d, getDataCalls=%d, maxEstimatedRaster=%d KB, maxUsedHeap=%d MB, duration=%d ms%n",
                Runtime.getRuntime().maxMemory() / (1024L * 1024L),
                samples,
                image.getTileCalls,
                image.getDataCalls,
                maxEstimatedBytes / 1024L,
                maxUsedHeapBytes / (1024L * 1024L),
                durationMillis);

        assertEquals(0, image.getDataCalls);
        assertTrue(samples > 500_000);
        assertTrue(image.getTileCalls > 500_000);
        assertTrue(maxEstimatedBytes <= tileBytes * 4L);
    }

    @Test
    @Tag("load")
    void managerSamplesMultipleVirtualRastersWithBoundedWorkingSet() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setInterpolationType(InterpolationType.BILINEAR);
        globalOptions.setNoDataValue(-9999.0);

        int rasterCount = 16;
        int rasterSize = 32768 * 2;
        int tileSize = 256;
        TerrainElevationDataManager manager = new TerrainElevationDataManager();
        TerrainElevationDataQuadTree root = new TerrainElevationDataQuadTree(null);
        VirtualTiledImage[] images = new VirtualTiledImage[rasterCount];

        for (int i = 0; i < rasterCount; i++) {
            images[i] = new VirtualTiledImage(rasterSize, rasterSize, tileSize, tileSize);
            TerrainElevationData elevationData = new TerrainElevationData(manager);
            elevationData.setCoverage(createCoverage(images[i]));
            elevationData.setGridCoverage2DSize(new Vector2i(rasterSize, rasterSize));
            elevationData.setGeographicExtension(createExtension(rasterSize));
            root.addTerrainElevationData(elevationData);
        }
        root.makeQuadTree(4);
        manager.setRootTerrainElevationDataQuadTree(root);

        GeographicExtension queryArea = createExtension(rasterSize);
        int samples = 0;
        long maxEstimatedBytes = 0L;
        long maxUsedHeapBytes = usedHeapBytes();
        long startNanos = System.nanoTime();
        for (int pass = 0; pass < 4; pass++) {
            int yOffset = pass * 29;
            int xOffset = pass * 13;
            for (int y = yOffset; y < rasterSize - 1; y += 389) {
                for (int x = xOffset; x < rasterSize - 1; x += 397) {
                    List<TerrainElevationData> elevationDataList = manager.getTerrainElevationDataArray(queryArea, (List<TerrainElevationData>) null);
                    for (TerrainElevationData elevationData : elevationDataList) {
                        elevationData.getElevation(x + 0.25, y + 0.25, new boolean[1]);
                    }
                    samples++;

                    if ((samples & 0x7ff) == 0) {
                        long estimatedBytes = 0L;
                        for (TerrainElevationData elevationData : elevationDataList) {
                            estimatedBytes += elevationData.estimateRasterBytes();
                        }
                        maxEstimatedBytes = Math.max(maxEstimatedBytes, estimatedBytes);
                        maxUsedHeapBytes = Math.max(maxUsedHeapBytes, usedHeapBytes());
                    }
                }
            }
        }

        long totalGetTileCalls = 0L;
        long totalGetDataCalls = 0L;
        for (VirtualTiledImage image : images) {
            totalGetTileCalls += image.getTileCalls;
            totalGetDataCalls += image.getDataCalls;
        }

        long maxAllowedEstimatedBytes = (long) rasterCount * tileSize * tileSize * 4L;
        long durationMillis = (System.nanoTime() - startNanos) / 1_000_000L;
        System.out.printf(
                "[MultiVirtualRasterLoad] maxHeap=%d MB, rasters=%d, samples=%d, getTileCalls=%d, getDataCalls=%d, maxEstimatedRaster=%d KB, maxUsedHeap=%d MB, duration=%d ms%n",
                Runtime.getRuntime().maxMemory() / (1024L * 1024L),
                rasterCount,
                samples,
                totalGetTileCalls,
                totalGetDataCalls,
                maxEstimatedBytes / 1024L,
                maxUsedHeapBytes / (1024L * 1024L),
                durationMillis);

        assertEquals(0, totalGetDataCalls);
        assertTrue(samples > 25_000);
        assertTrue(totalGetTileCalls >= rasterCount);
        assertTrue(totalGetTileCalls <= (long) samples * rasterCount * 4L);
        assertTrue(maxEstimatedBytes <= maxAllowedEstimatedBytes,
                "Estimated raster working set exceeded tile-cache bound. expected <= "
                        + maxAllowedEstimatedBytes + " bytes, actual=" + maxEstimatedBytes + " bytes");
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

    private GridCoverage2D createCoverage(BufferedImage image) {
        ReferencedEnvelope envelope = new ReferencedEnvelope(0.0, image.getWidth(), 0.0, image.getHeight(), DefaultGeographicCRS.WGS84);
        return new GridCoverageFactory().create("dem", image, envelope);
    }

    private GridCoverage2D createCoverage(RenderedImage image) {
        ReferencedEnvelope envelope = new ReferencedEnvelope(0.0, image.getWidth(), 0.0, image.getHeight(), DefaultGeographicCRS.WGS84);
        return new GridCoverageFactory().create("dem", image, envelope);
    }

    private GeographicExtension createExtension() {
        return createExtension(2.0, 2.0);
    }

    private GeographicExtension createExtension(double width, double height) {
        GeographicExtension extension = new GeographicExtension();
        extension.setDegrees(0.0, 0.0, 0.0, width, height, 0.0);
        return extension;
    }

    private GeographicExtension createExtension(double size) {
        return createExtension(size, size);
    }

    private long usedHeapBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static class GetDataFailingImage extends BufferedImage {
        private int getDataCalls = 0;
        private int getTileCalls = 0;

        private GetDataFailingImage(int[][] values) {
            super(values[0].length, values.length, BufferedImage.TYPE_BYTE_GRAY);
            WritableRaster raster = getRaster();
            for (int y = 0; y < values.length; y++) {
                for (int x = 0; x < values[y].length; x++) {
                    raster.setSample(x, y, 0, values[y][x]);
                }
            }
        }

        @Override
        public Raster getData() {
            getDataCalls++;
            throw new AssertionError("Full raster materialization must not be used for terrain elevation sampling.");
        }

        @Override
        public Raster getData(Rectangle rect) {
            getDataCalls++;
            throw new AssertionError("Full raster materialization must not be used for terrain elevation sampling.");
        }

        @Override
        public Raster getTile(int tileX, int tileY) {
            getTileCalls++;
            return super.getTile(tileX, tileY);
        }
    }

    private static class VirtualTiledImage implements RenderedImage {
        private final int width;
        private final int height;
        private final int tileWidth;
        private final int tileHeight;
        private final SampleModel sampleModel;
        private int getDataCalls = 0;
        private int getTileCalls = 0;

        private VirtualTiledImage(int width, int height, int tileWidth, int tileHeight) {
            this.width = width;
            this.height = height;
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;
            this.sampleModel = new BandedSampleModel(DataBuffer.TYPE_BYTE, tileWidth, tileHeight, 1);
        }

        @Override
        public Vector<RenderedImage> getSources() {
            return null;
        }

        @Override
        public Object getProperty(String name) {
            return java.awt.Image.UndefinedProperty;
        }

        @Override
        public String[] getPropertyNames() {
            return new String[0];
        }

        @Override
        public ColorModel getColorModel() {
            return null;
        }

        @Override
        public SampleModel getSampleModel() {
            return sampleModel;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public int getMinX() {
            return 0;
        }

        @Override
        public int getMinY() {
            return 0;
        }

        @Override
        public int getNumXTiles() {
            return Math.ceilDiv(width, tileWidth);
        }

        @Override
        public int getNumYTiles() {
            return Math.ceilDiv(height, tileHeight);
        }

        @Override
        public int getMinTileX() {
            return 0;
        }

        @Override
        public int getMinTileY() {
            return 0;
        }

        @Override
        public int getTileWidth() {
            return tileWidth;
        }

        @Override
        public int getTileHeight() {
            return tileHeight;
        }

        @Override
        public int getTileGridXOffset() {
            return 0;
        }

        @Override
        public int getTileGridYOffset() {
            return 0;
        }

        @Override
        public Raster getTile(int tileX, int tileY) {
            getTileCalls++;
            int x = tileX * tileWidth;
            int y = tileY * tileHeight;
            int widthInTile = Math.min(tileWidth, width - x);
            int heightInTile = Math.min(tileHeight, height - y);
            SampleModel tileSampleModel = sampleModel.createCompatibleSampleModel(widthInTile, heightInTile);
            return Raster.createWritableRaster(tileSampleModel, new Point(x, y));
        }

        @Override
        public Raster getData() {
            getDataCalls++;
            throw new AssertionError("Virtual load test must not materialize the full raster.");
        }

        @Override
        public Raster getData(Rectangle rect) {
            getDataCalls++;
            throw new AssertionError("Virtual load test must not materialize the full raster.");
        }

        @Override
        public WritableRaster copyData(WritableRaster raster) {
            throw new AssertionError("Virtual load test must not copy the full raster.");
        }
    }
}
