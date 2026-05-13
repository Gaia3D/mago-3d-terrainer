package com.gaia3d.terrain.tile;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.structure.GeographicExtension;
import com.gaia3d.terrain.tile.geotiff.GaiaGeoTiffManager;
import com.gaia3d.terrain.types.InterpolationType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.imagen.media.range.NoDataContainer;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Position2D;
import org.joml.Vector2d;
import org.joml.Vector2i;

import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.util.Arrays;

@Slf4j
@Getter
@Setter
public class TerrainElevationData {
    private static final int TILE_RASTER_CACHE_SIZE = 32;
    private static final long EMPTY_TILE_KEY = Long.MIN_VALUE;
    private static final long MAX_MATERIALIZED_RASTER_BYTES = 1024L * 1024L * 1024L;
    private GlobalOptions globalOptions = GlobalOptions.getInstance();

    private Vector2d pixelSizeMeters;
    private TerrainElevationDataManager terrainElevDataManager = null;
    private String geotiffFilePath = "";
    private String geotiffFileName = "";
    private GeographicExtension geographicExtension = new GeographicExtension();
    private GeoTiffReader geoTiffReader = null;
    private GridCoverage2D coverage = null;
    private RenderedImage renderedImage = null;
    private Raster raster = null;
    private double minAltitude = Double.MAX_VALUE;
    private double maxAltitude = Double.MIN_VALUE;
    private double[] altitude = new double[1];
    private NoDataContainer noDataContainer = null;
    private Position2D worldPosition = null; // longitude supplied first
    private int geoTiffWidth = -1;
    private int geoTiffHeight = -1;
    private int queryMark = 0;
    private Vector2i gridCoverage2DSize = null;
    private int rasterMinX = 0;
    private int rasterMinY = 0;
    private int rasterSampleModelTranslateX = 0;
    private int rasterSampleModelTranslateY = 0;
    private int rasterWidth = -1;
    private int rasterHeight = -1;
    private boolean hasCoverageNoData = false;
    private double coverageNoDataValue = Double.NaN;
    private double inverseLongitudeRange = Double.NaN;
    private double inverseLatitudeRange = Double.NaN;
    private double minLongitudeDeg = 0.0;
    private double minLatitudeDeg = 0.0;
    private double maxLongitudeDeg = 0.0;
    private double maxLatitudeDeg = 0.0;
    private boolean rasterInitializationLogged = false;
    private boolean useDirectRasterAccess = false;
    private int currentTileX = Integer.MIN_VALUE;
    private int currentTileY = Integer.MIN_VALUE;
    private long currentTileKey = EMPTY_TILE_KEY;
    private int rasterDataKind = 0;
    private int rasterBaseOffset = 0;
    private int rasterPixelStride = 1;
    private int rasterScanlineStride = 0;
    private int imageMinX = 0;
    private int imageMinY = 0;
    private int imageMaxX = 0;
    private int imageMaxY = 0;
    private int imageTileWidth = 0;
    private int imageTileHeight = 0;
    private int imageTileGridXOffset = 0;
    private int imageTileGridYOffset = 0;
    private float[] materializedRasterData = null;
    private int materializedRasterMinX = 0;
    private int materializedRasterMinY = 0;
    private int materializedRasterWidth = -1;
    private int materializedRasterHeight = -1;
    private float[] rasterFloatData = null;
    private double[] rasterDoubleData = null;
    private int[] rasterIntData = null;
    private short[] rasterShortData = null;
    private byte[] rasterByteData = null;
    private final long[] tileRasterCacheKeys = new long[TILE_RASTER_CACHE_SIZE];
    private final Raster[] tileRasterCacheValues = new Raster[TILE_RASTER_CACHE_SIZE];

    private static final int RASTER_KIND_GENERIC = 0;
    private static final int RASTER_KIND_FLOAT = 1;
    private static final int RASTER_KIND_DOUBLE = 2;
    private static final int RASTER_KIND_INT = 3;
    private static final int RASTER_KIND_USHORT = 4;
    private static final int RASTER_KIND_SHORT = 5;
    private static final int RASTER_KIND_BYTE = 6;

    public TerrainElevationData(TerrainElevationDataManager terrainElevationDataManager) {
        this.terrainElevDataManager = terrainElevationDataManager;
    }

    public void deleteCoverage() {
        if (this.coverage != null) {
            this.coverage.dispose(true);
            this.coverage = null;
        }
        if (this.geoTiffReader != null) {
            this.geoTiffReader.dispose();
            this.geoTiffReader = null;
        }

        if (this.noDataContainer != null) {
            this.noDataContainer = null;
        }
        this.renderedImage = null;
        this.raster = null;
        this.currentTileX = Integer.MIN_VALUE;
        this.currentTileY = Integer.MIN_VALUE;
        this.currentTileKey = EMPTY_TILE_KEY;
        this.rasterMinX = 0;
        this.rasterMinY = 0;
        this.rasterWidth = -1;
        this.rasterHeight = -1;
        this.rasterSampleModelTranslateX = 0;
        this.rasterSampleModelTranslateY = 0;
        this.hasCoverageNoData = false;
        this.coverageNoDataValue = Double.NaN;
        this.inverseLongitudeRange = Double.NaN;
        this.inverseLatitudeRange = Double.NaN;
        this.minLongitudeDeg = 0.0;
        this.minLatitudeDeg = 0.0;
        this.maxLongitudeDeg = 0.0;
        this.maxLatitudeDeg = 0.0;
        this.rasterInitializationLogged = false;
        this.useDirectRasterAccess = false;
        this.rasterDataKind = RASTER_KIND_GENERIC;
        this.rasterBaseOffset = 0;
        this.rasterPixelStride = 1;
        this.rasterScanlineStride = 0;
        this.rasterFloatData = null;
        this.rasterDoubleData = null;
        this.rasterIntData = null;
        this.rasterShortData = null;
        this.rasterByteData = null;
        clearTileRasterCache();
    }

    public void releaseTileRaster() {
        this.raster = null;
        this.currentTileX = Integer.MIN_VALUE;
        this.currentTileY = Integer.MIN_VALUE;
        this.currentTileKey = EMPTY_TILE_KEY;
        this.rasterMinX = 0;
        this.rasterMinY = 0;
        this.rasterWidth = -1;
        this.rasterHeight = -1;
        this.rasterSampleModelTranslateX = 0;
        this.rasterSampleModelTranslateY = 0;
        this.useDirectRasterAccess = false;
        this.rasterDataKind = RASTER_KIND_GENERIC;
        this.rasterBaseOffset = 0;
        this.rasterPixelStride = 1;
        this.rasterScanlineStride = 0;
        this.imageMinX = 0;
        this.imageMinY = 0;
        this.imageMaxX = 0;
        this.imageMaxY = 0;
        this.imageTileWidth = 0;
        this.imageTileHeight = 0;
        this.imageTileGridXOffset = 0;
        this.imageTileGridYOffset = 0;
        this.materializedRasterData = null;
        this.materializedRasterMinX = 0;
        this.materializedRasterMinY = 0;
        this.materializedRasterWidth = -1;
        this.materializedRasterHeight = -1;
        this.rasterFloatData = null;
        this.rasterDoubleData = null;
        this.rasterIntData = null;
        this.rasterShortData = null;
        this.rasterByteData = null;
    }

    public void deleteObjects() {
        this.deleteCoverage();
        this.terrainElevDataManager = null;
        this.geotiffFilePath = null;
        if (this.geographicExtension != null) {
            this.geographicExtension.deleteObjects();
            this.geographicExtension = null;
        }
        altitude = null;
        noDataContainer = null;
        worldPosition = null;
    }

    public double getPixelArea() {
        return this.pixelSizeMeters.x * this.pixelSizeMeters.y;
    }

    public void getPixelSizeDegree(Vector2d resultPixelSize) {
        if (!ensureRasterInitialized() || this.renderedImage == null) {
            resultPixelSize.set(0.0, 0.0);
            return;
        }

        double imageWidth = this.renderedImage.getWidth();
        double imageHeight = this.renderedImage.getHeight();
        double longitudeRange = this.geographicExtension.getLongitudeRangeDegree();
        double latitudeRange = this.geographicExtension.getLatitudeRangeDegree();
        double pixelSizeX = longitudeRange / imageWidth;
        double pixelSizeY = latitudeRange / imageHeight;
        resultPixelSize.set(pixelSizeX, pixelSizeY);
    }

    public double getGridValue(int x, int y) {
        double value = 0.0;
        if (!ensureTileForPixel(x, y)) {
            return globalOptions.getNoDataValue();
        }

        if (raster != null) {
            try {
                value = readSample(x, y);
                // check if the value is NaN
                if (Double.isNaN(value)) {
                    return globalOptions.getNoDataValue();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                log.debug("[getGridValue : ArrayIndexOutOfBoundsException] getGridValue", e);
            } catch (Exception e) {
                log.error("[getGridValue : Exception] Error in getGridValue", e);
                log.error("Error:", e);
            }

            if (this.noDataContainer == null && coverage != null) {
                this.noDataContainer = CoverageUtilities.getNoDataProperty(coverage);
                if (this.noDataContainer != null) {
                    this.coverageNoDataValue = this.noDataContainer.getAsSingleValue();
                    this.hasCoverageNoData = true;
                }
            }

            if (hasCoverageNoData && value == coverageNoDataValue) {
                return globalOptions.getNoDataValue();
            }
        }
        return value;
    }

    public double getElevation(double lonDeg, double latDeg, boolean[] intersects) {
        ensureGeographicScaleInitialized();

        if (lonDeg < minLongitudeDeg || lonDeg > maxLongitudeDeg || latDeg < minLatitudeDeg || latDeg > maxLatitudeDeg) {
            intersects[0] = false;
            return 0.0;
        }

        if (gridCoverage2DSize == null) {
            gridCoverage2DSize = this.terrainElevDataManager.getGaiaGeoTiffManager().getGridCoverage2DSize(this.geotiffFilePath);
        }
        Vector2i size = gridCoverage2DSize;

        double unitaryX = (lonDeg - minLongitudeDeg) * this.inverseLongitudeRange;
        double unitaryY = 1.0 - (latDeg - minLatitudeDeg) * this.inverseLatitudeRange;

        int geoTiffRasterHeight = size.y;
        int geoTiffRasterWidth = size.x;
        if (!ensureRasterInitialized()) {
            intersects[0] = false;
            return globalOptions.getNoDataValue();
        }

        intersects[0] = true;
        double resultAltitude;
        if (globalOptions.getInterpolationType() == InterpolationType.BILINEAR) {
            resultAltitude = calcBilinearInterpolation(unitaryX, unitaryY, geoTiffRasterWidth, geoTiffRasterHeight);
        } else {
            resultAltitude = calcNearestInterpolation((int) Math.floor(unitaryX * geoTiffRasterWidth), (int) Math.floor(unitaryY * geoTiffRasterHeight));
        }

        double noDataValue = globalOptions.getNoDataValue();
        if (resultAltitude == noDataValue) {
            intersects[0] = false;
            return resultAltitude;
        }

        if (resultAltitude < minAltitude) {
            minAltitude = resultAltitude;
        }
        if (resultAltitude > maxAltitude) {
            maxAltitude = resultAltitude;
        }

        return resultAltitude;
    }

    private double calcNearestInterpolation(int column, int row) {
        if (materializedRasterData != null) {
            return readMaterializedGridValue(column, row);
        }
        return this.getGridValue(column, row);
    }

    private double calcBilinearInterpolation(double x, double y, int geoTiffWidth, int geoTiffHeight) {
        double clampedX = Math.max(0.0, Math.min(x, Math.nextDown(1.0)));
        double clampedY = Math.max(0.0, Math.min(y, Math.nextDown(1.0)));

        double scaledX = clampedX * geoTiffWidth;
        double scaledY = clampedY * geoTiffHeight;

        int column = (int) Math.floor(scaledX);
        int row = (int) Math.floor(scaledY);

        double factorX = scaledX - column;
        double factorY = scaledY - row;

        int columnNext = column + 1;
        int rowNext = row + 1;

        if (columnNext >= geoTiffWidth) {
            columnNext = geoTiffWidth - 1;
        }

        if (rowNext >= geoTiffHeight) {
            rowNext = geoTiffHeight - 1;
        }

        double value00;
        double value01;
        double value10;
        double value11;
        if (materializedRasterData != null) {
            value00 = readMaterializedGridValue(column, row);
            value01 = readMaterializedGridValue(column, rowNext);
            value10 = readMaterializedGridValue(columnNext, row);
            value11 = readMaterializedGridValue(columnNext, rowNext);
        } else if (ensureTileForPixel(column, row) && isPixelInCurrentRaster(columnNext, rowNext)) {
            value00 = readGridValueDirect(column, row);
            value01 = readGridValueDirect(column, rowNext);
            value10 = readGridValueDirect(columnNext, row);
            value11 = readGridValueDirect(columnNext, rowNext);
        } else {
            value00 = readGridValueFast(column, row);
            value01 = readGridValueFast(column, rowNext);
            value10 = readGridValueFast(columnNext, row);
            value11 = readGridValueFast(columnNext, rowNext);
        }

        // Ignore noDataValue samples.
        double noDataValue = globalOptions.getNoDataValue();
        boolean hasNoData = (value00 == noDataValue) || (value01 == noDataValue) || (value10 == noDataValue) || (value11 == noDataValue);
        if (hasNoData) {
            if (value00 == noDataValue) {
                return noDataValue;
            } else {
                return value00;
            }
        }

        double value0 = value00 * (1.0 - factorY) + value01 * factorY;
        double value1 = value10 * (1.0 - factorY) + value11 * factorY;

        return value0 + factorX * (value1 - value0);
    }

    private void ensureGeographicScaleInitialized() {
        if (!Double.isNaN(this.inverseLongitudeRange) && !Double.isNaN(this.inverseLatitudeRange)) {
            return;
        }

        this.minLongitudeDeg = this.geographicExtension.getMinLongitudeDeg();
        this.minLatitudeDeg = this.geographicExtension.getMinLatitudeDeg();
        this.maxLongitudeDeg = this.geographicExtension.getMaxLongitudeDeg();
        this.maxLatitudeDeg = this.geographicExtension.getMaxLatitudeDeg();
        double longitudeRange = this.maxLongitudeDeg - this.minLongitudeDeg;
        double latitudeRange = this.maxLatitudeDeg - this.minLatitudeDeg;
        this.inverseLongitudeRange = longitudeRange != 0.0 ? 1.0 / longitudeRange : 0.0;
        this.inverseLatitudeRange = latitudeRange != 0.0 ? 1.0 / latitudeRange : 0.0;
    }

    private boolean ensureRasterInitialized() {
        if (this.renderedImage != null) {
            if (this.imageTileWidth <= 0 || this.imageTileHeight <= 0) {
                initializeRenderedImageAccessor();
            }
            return true;
        }

        if (this.coverage == null) {
            try {
                this.geoTiffReader = new GeoTiffReader(new File(this.geotiffFilePath));
                this.coverage = this.geoTiffReader.read(null);
            } catch (Exception e) {
                if (this.geoTiffReader != null) {
                    this.geoTiffReader.dispose();
                    this.geoTiffReader = null;
                }
                log.error("Failed to open GeoTIFF reader for {}", this.geotiffFilePath, e);
                return false;
            }
        }

        if (this.noDataContainer == null && this.coverage != null) {
            this.noDataContainer = CoverageUtilities.getNoDataProperty(coverage);
            if (this.noDataContainer != null) {
                this.coverageNoDataValue = this.noDataContainer.getAsSingleValue();
                this.hasCoverageNoData = true;
            }
        }

        this.renderedImage = coverage.getRenderedImage();
        if (this.renderedImage == null) {
            log.error("RenderedImage is null");
            return false;
        }
        initializeRenderedImageAccessor();
        materializeRasterData();

        if (!rasterInitializationLogged) {
            rasterInitializationLogged = true;
            log.info("[Raster][SampleInit] Prepared rendered image for {} (image={}x{}, tile={}x{}, materialized={})",
                geotiffFilePath,
                this.renderedImage.getWidth(),
                this.renderedImage.getHeight(),
                this.renderedImage.getTileWidth(),
                this.renderedImage.getTileHeight(),
                this.materializedRasterData != null);
        }
        return true;
    }

    private void materializeRasterData() {
        if (this.materializedRasterData != null || this.renderedImage == null) {
            return;
        }

        int width = this.renderedImage.getWidth();
        int height = this.renderedImage.getHeight();
        long sampleCount = (long) width * height;
        long estimatedBytes = sampleCount * Float.BYTES;
        if (width <= 0 || height <= 0 || sampleCount > Integer.MAX_VALUE || estimatedBytes > MAX_MATERIALIZED_RASTER_BYTES) {
            return;
        }

        Raster fullRaster = this.renderedImage.getData();
        this.materializedRasterMinX = fullRaster.getMinX();
        this.materializedRasterMinY = fullRaster.getMinY();
        this.materializedRasterWidth = fullRaster.getWidth();
        this.materializedRasterHeight = fullRaster.getHeight();
        this.materializedRasterData = new float[(int) sampleCount];

        double noDataValue = globalOptions.getNoDataValue();
        int index = 0;
        int maxY = this.materializedRasterMinY + this.materializedRasterHeight;
        int maxX = this.materializedRasterMinX + this.materializedRasterWidth;
        for (int y = this.materializedRasterMinY; y < maxY; y++) {
            for (int x = this.materializedRasterMinX; x < maxX; x++) {
                double value = fullRaster.getSampleDouble(x, y, 0);
                if (Double.isNaN(value) || (hasCoverageNoData && value == coverageNoDataValue)) {
                    value = noDataValue;
                }
                this.materializedRasterData[index++] = (float) value;
            }
        }
    }

    private void initializeRenderedImageAccessor() {
        this.imageMinX = this.renderedImage.getMinX();
        this.imageMinY = this.renderedImage.getMinY();
        this.imageMaxX = this.imageMinX + this.renderedImage.getWidth();
        this.imageMaxY = this.imageMinY + this.renderedImage.getHeight();
        this.imageTileWidth = this.renderedImage.getTileWidth();
        this.imageTileHeight = this.renderedImage.getTileHeight();
        this.imageTileGridXOffset = this.renderedImage.getTileGridXOffset();
        this.imageTileGridYOffset = this.renderedImage.getTileGridYOffset();
    }

    private boolean ensureTileForPixel(int x, int y) {
        if (!ensureRasterInitialized() || this.renderedImage == null) {
            return false;
        }

        if (x < imageMinX || y < imageMinY || x >= imageMaxX || y >= imageMaxY) {
            return false;
        }

        if (this.raster != null && x >= rasterMinX && y >= rasterMinY && x < rasterMinX + rasterWidth && y < rasterMinY + rasterHeight) {
            return true;
        }

        int tileX = Math.floorDiv(x - imageTileGridXOffset, imageTileWidth);
        int tileY = Math.floorDiv(y - imageTileGridYOffset, imageTileHeight);
        long tileKey = toTileKey(tileX, tileY);

        Raster cachedRaster = tileKey == this.currentTileKey ? this.raster : getCachedTileRaster(tileKey);
        this.raster = cachedRaster != null ? cachedRaster : this.renderedImage.getTile(tileX, tileY);
        if (this.raster == null) {
            return false;
        }

        if (cachedRaster == null) {
            putCachedTileRaster(tileKey, this.raster);
        }

        this.currentTileX = tileX;
        this.currentTileY = tileY;
        this.currentTileKey = tileKey;
        initializeRasterAccessor();
        return true;
    }

    private Raster getCachedTileRaster(long tileKey) {
        int index = tileCacheIndex(tileKey);
        return this.tileRasterCacheKeys[index] == tileKey ? this.tileRasterCacheValues[index] : null;
    }

    private void putCachedTileRaster(long tileKey, Raster tileRaster) {
        int index = tileCacheIndex(tileKey);
        this.tileRasterCacheKeys[index] = tileKey;
        this.tileRasterCacheValues[index] = tileRaster;
    }

    private void clearTileRasterCache() {
        Arrays.fill(this.tileRasterCacheKeys, EMPTY_TILE_KEY);
        Arrays.fill(this.tileRasterCacheValues, null);
    }

    private int tileCacheIndex(long tileKey) {
        int hash = Long.hashCode(tileKey);
        hash ^= hash >>> 16;
        return hash & (TILE_RASTER_CACHE_SIZE - 1);
    }

    private void initializeRasterAccessor() {
        if (this.raster == null) {
            return;
        }

        this.rasterWidth = this.raster.getWidth();
        this.rasterHeight = this.raster.getHeight();
        this.rasterMinX = this.raster.getMinX();
        this.rasterMinY = this.raster.getMinY();
        this.rasterSampleModelTranslateX = this.raster.getSampleModelTranslateX();
        this.rasterSampleModelTranslateY = this.raster.getSampleModelTranslateY();
        this.useDirectRasterAccess = false;
        this.rasterDataKind = RASTER_KIND_GENERIC;
        this.rasterBaseOffset = 0;
        this.rasterPixelStride = 1;
        this.rasterScanlineStride = 0;
        this.rasterFloatData = null;
        this.rasterDoubleData = null;
        this.rasterIntData = null;
        this.rasterShortData = null;
        this.rasterByteData = null;

        SampleModel sampleModel = this.raster.getSampleModel();
        if (!(sampleModel instanceof ComponentSampleModel componentSampleModel)) {
            return;
        }

        int[] bankIndices = componentSampleModel.getBankIndices();
        int bank = bankIndices.length > 0 ? bankIndices[0] : 0;
        int[] bandOffsets = componentSampleModel.getBandOffsets();
        int bandOffset = bandOffsets.length > 0 ? bandOffsets[0] : 0;
        this.rasterBaseOffset = bandOffset;
        this.rasterPixelStride = componentSampleModel.getPixelStride();
        this.rasterScanlineStride = componentSampleModel.getScanlineStride();

        if (this.raster.getDataBuffer() instanceof DataBufferFloat dataBuffer) {
            this.rasterFloatData = dataBuffer.getData(bank);
            this.rasterBaseOffset += dataBuffer.getOffsets()[bank];
            this.rasterDataKind = RASTER_KIND_FLOAT;
        } else if (this.raster.getDataBuffer() instanceof DataBufferDouble dataBuffer) {
            this.rasterDoubleData = dataBuffer.getData(bank);
            this.rasterBaseOffset += dataBuffer.getOffsets()[bank];
            this.rasterDataKind = RASTER_KIND_DOUBLE;
        } else if (this.raster.getDataBuffer() instanceof DataBufferInt dataBuffer) {
            this.rasterIntData = dataBuffer.getData(bank);
            this.rasterBaseOffset += dataBuffer.getOffsets()[bank];
            this.rasterDataKind = RASTER_KIND_INT;
        } else if (this.raster.getDataBuffer() instanceof DataBufferUShort dataBuffer) {
            this.rasterShortData = dataBuffer.getData(bank);
            this.rasterBaseOffset += dataBuffer.getOffsets()[bank];
            this.rasterDataKind = RASTER_KIND_USHORT;
        } else if (this.raster.getDataBuffer() instanceof DataBufferShort dataBuffer) {
            this.rasterShortData = dataBuffer.getData(bank);
            this.rasterBaseOffset += dataBuffer.getOffsets()[bank];
            this.rasterDataKind = RASTER_KIND_SHORT;
        } else if (this.raster.getDataBuffer() instanceof DataBufferByte dataBuffer) {
            this.rasterByteData = dataBuffer.getData(bank);
            this.rasterBaseOffset += dataBuffer.getOffsets()[bank];
            this.rasterDataKind = RASTER_KIND_BYTE;
        }

        this.useDirectRasterAccess = this.rasterDataKind != RASTER_KIND_GENERIC;
    }

    private double readGridValueDirect(int x, int y) {
        if (materializedRasterData != null) {
            return readMaterializedGridValue(x, y);
        }

        if (x < rasterMinX || y < rasterMinY || x >= rasterMinX + rasterWidth || y >= rasterMinY + rasterHeight) {
            return globalOptions.getNoDataValue();
        }

        double value = readSample(x, y);
        if (Double.isNaN(value)) {
            return globalOptions.getNoDataValue();
        }
        if (hasCoverageNoData && value == coverageNoDataValue) {
            return globalOptions.getNoDataValue();
        }
        return value;
    }

    private boolean isPixelInCurrentRaster(int x, int y) {
        return this.raster != null
            && x >= rasterMinX
            && y >= rasterMinY
            && x < rasterMinX + rasterWidth
            && y < rasterMinY + rasterHeight;
    }

    private double readGridValueFast(int x, int y) {
        if (materializedRasterData != null) {
            return readMaterializedGridValue(x, y);
        }

        if (!ensureTileForPixel(x, y)) {
            return globalOptions.getNoDataValue();
        }

        double value = readSample(x, y);
        if (Double.isNaN(value)) {
            return globalOptions.getNoDataValue();
        }
        if (hasCoverageNoData && value == coverageNoDataValue) {
            return globalOptions.getNoDataValue();
        }
        return value;
    }

    private double readMaterializedGridValue(int x, int y) {
        if (x < materializedRasterMinX
            || y < materializedRasterMinY
            || x >= materializedRasterMinX + materializedRasterWidth
            || y >= materializedRasterMinY + materializedRasterHeight) {
            return globalOptions.getNoDataValue();
        }

        int offset = (y - materializedRasterMinY) * materializedRasterWidth + (x - materializedRasterMinX);
        return materializedRasterData[offset];
    }

    private double readSample(int x, int y) {
        if (!useDirectRasterAccess) {
            return raster.getSampleDouble(x, y, 0);
        }

        int sampleX = x - rasterSampleModelTranslateX;
        int sampleY = y - rasterSampleModelTranslateY;
        int offset = rasterBaseOffset + sampleY * rasterScanlineStride + sampleX * rasterPixelStride;
        return switch (rasterDataKind) {
            case RASTER_KIND_FLOAT -> rasterFloatData[offset];
            case RASTER_KIND_DOUBLE -> rasterDoubleData[offset];
            case RASTER_KIND_INT -> rasterIntData[offset];
            case RASTER_KIND_USHORT -> rasterShortData[offset] & 0xffff;
            case RASTER_KIND_SHORT -> rasterShortData[offset];
            case RASTER_KIND_BYTE -> rasterByteData[offset] & 0xff;
            default -> raster.getSampleDouble(x, y, 0);
        };
    }

    private long toTileKey(int tileX, int tileY) {
        return (((long) tileX) << 32) ^ (tileY & 0xffffffffL);
    }

    public boolean preloadRaster() {
        return ensureRasterInitialized();
    }

    public boolean isRasterLoaded() {
        return this.renderedImage != null || this.raster != null;
    }

    public long estimateRasterBytes() {
        if (this.materializedRasterData != null) {
            return (long) this.materializedRasterData.length * Float.BYTES;
        }

        if (this.rasterWidth > 0 && this.rasterHeight > 0) {
            return (long) this.rasterWidth * this.rasterHeight * estimateBytesPerSample();
        }

        if (this.gridCoverage2DSize == null && this.terrainElevDataManager != null) {
            this.gridCoverage2DSize = this.terrainElevDataManager.getGaiaGeoTiffManager().getGridCoverage2DSize(this.geotiffFilePath);
        }

        if (this.gridCoverage2DSize == null) {
            return 0L;
        }
        return (long) this.gridCoverage2DSize.x * this.gridCoverage2DSize.y * 4L;
    }

    private int estimateBytesPerSample() {
        return switch (rasterDataKind) {
            case RASTER_KIND_DOUBLE -> 8;
            case RASTER_KIND_INT, RASTER_KIND_FLOAT -> 4;
            case RASTER_KIND_USHORT, RASTER_KIND_SHORT -> 2;
            case RASTER_KIND_BYTE -> 1;
            default -> 4;
        };
    }
}
