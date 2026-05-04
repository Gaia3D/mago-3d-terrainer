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
import org.geotools.geometry.Position2D;
import org.joml.Vector2d;
import org.joml.Vector2i;

import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

@Slf4j
@Getter
@Setter
public class TerrainElevationData {
    private GlobalOptions globalOptions = GlobalOptions.getInstance();

    private Vector2d pixelSizeMeters;
    private TerrainElevationDataManager terrainElevDataManager = null;
    private String geotiffFilePath = "";
    private String geotiffFileName = "";
    private GeographicExtension geographicExtension = new GeographicExtension();
    private GridCoverage2D coverage = null;
    private Raster raster = null;
    private double minAltitude = Double.MAX_VALUE;
    private double maxAltitude = Double.MIN_VALUE;
    private double[] altitude = new double[1];
    private NoDataContainer noDataContainer = null;
    private Position2D worldPosition = null; // longitude supplied first
    private int geoTiffWidth = -1;
    private int geoTiffHeight = -1;
    private Vector2i gridCoverage2DSize = null;
    private int rasterMinX = 0;
    private int rasterMinY = 0;
    private int rasterSampleModelTranslateX = 0;
    private int rasterSampleModelTranslateY = 0;
    private int rasterWidth = -1;
    private int rasterHeight = -1;
    private boolean hasCoverageNoData = false;
    private double coverageNoDataValue = Double.NaN;
    private boolean rasterInitializationLogged = false;
    private boolean useDirectRasterAccess = false;
    private int rasterDataKind = 0;
    private int rasterBaseOffset = 0;
    private int rasterPixelStride = 1;
    private int rasterScanlineStride = 0;
    private float[] rasterFloatData = null;
    private double[] rasterDoubleData = null;
    private int[] rasterIntData = null;
    private short[] rasterShortData = null;
    private byte[] rasterByteData = null;

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

        if (this.noDataContainer != null) {
            this.noDataContainer = null;
        }
        this.raster = null;
        this.rasterMinX = 0;
        this.rasterMinY = 0;
        this.rasterWidth = -1;
        this.rasterHeight = -1;
        this.rasterSampleModelTranslateX = 0;
        this.rasterSampleModelTranslateY = 0;
        this.hasCoverageNoData = false;
        this.coverageNoDataValue = Double.NaN;
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
        double imageWidth = this.coverage.getRenderedImage().getWidth();
        double imageHeight = this.coverage.getRenderedImage().getHeight();
        double longitudeRange = this.geographicExtension.getLongitudeRangeDegree();
        double latitudeRange = this.geographicExtension.getLatitudeRangeDegree();
        double pixelSizeX = longitudeRange / imageWidth;
        double pixelSizeY = latitudeRange / imageHeight;
        resultPixelSize.set(pixelSizeX, pixelSizeY);
    }

    public double getGridValue(int x, int y) {
        double value = 0.0;
        if (!ensureRasterInitialized()) {
            return globalOptions.getNoDataValue();
        }

        if (raster != null) {
            if (x < rasterMinX || y < rasterMinY || x >= rasterMinX + rasterWidth || y >= rasterMinY + rasterHeight) {
                return globalOptions.getNoDataValue();
            }

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
        double resultAltitude = 0.0;

        // First check if lon, lat intersects with geoExtension
        if (!this.geographicExtension.intersects(lonDeg, latDeg)) {
            intersects[0] = false;
            return resultAltitude;
        }

        if (gridCoverage2DSize == null) {
            gridCoverage2DSize = this.terrainElevDataManager.getGaiaGeoTiffManager().getGridCoverage2DSize(this.geotiffFilePath);
        }
        Vector2i size = gridCoverage2DSize;

        double unitaryX = (lonDeg - this.geographicExtension.getMinLongitudeDeg()) / this.geographicExtension.getLongitudeRangeDegree();
        double unitaryY = 1.0 - (latDeg - this.geographicExtension.getMinLatitudeDeg()) / this.geographicExtension.getLatitudeRangeDegree();

        int geoTiffRasterHeight = size.y;
        int geoTiffRasterWidth = size.x;
        if (!ensureRasterInitialized()) {
            intersects[0] = false;
            return globalOptions.getNoDataValue();
        }

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        if (globalOptions.getInterpolationType().equals(InterpolationType.BILINEAR)) {
            intersects[0] = true;
            resultAltitude = calcBilinearInterpolation(unitaryX, unitaryY, geoTiffRasterWidth, geoTiffRasterHeight);
        } else {
            intersects[0] = true;
            int column = (int) Math.floor(unitaryX * geoTiffRasterWidth);
            int row = (int) Math.floor(unitaryY * geoTiffRasterHeight);
            resultAltitude = calcNearestInterpolation(column, row);
        }

        double noDataValue = globalOptions.getNoDataValue();
        if (resultAltitude == noDataValue) {
            intersects[0] = false;
            return resultAltitude;
        }

        // update min, max altitude
        minAltitude = Math.min(minAltitude, resultAltitude);
        maxAltitude = Math.max(maxAltitude, resultAltitude);

        return resultAltitude;
    }

    private double calcNearestInterpolation(int column, int row) {
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

        double value00 = readGridValueDirect(column, row);
        double value01 = readGridValueDirect(column, rowNext);
        double value10 = readGridValueDirect(columnNext, row);
        double value11 = readGridValueDirect(columnNext, rowNext);

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

    private boolean ensureRasterInitialized() {
        if (this.raster != null) {
            return true;
        }

        if (this.coverage == null) {
            if (this.terrainElevDataManager == null) {
                return false;
            }
            GaiaGeoTiffManager gaiaGeoTiffManager = this.terrainElevDataManager.getGaiaGeoTiffManager();
            this.coverage = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(this.geotiffFilePath);
        }

        if (this.noDataContainer == null && this.coverage != null) {
            this.noDataContainer = CoverageUtilities.getNoDataProperty(coverage);
            if (this.noDataContainer != null) {
                this.coverageNoDataValue = this.noDataContainer.getAsSingleValue();
                this.hasCoverageNoData = true;
            }
        }

        RenderedImage renderedImage = coverage.getRenderedImage();
        if (renderedImage == null) {
            log.error("RenderedImage is null");
            return false;
        }

        long startNs = System.nanoTime();
        boolean wasCached = false;
        if (this.terrainElevDataManager != null && this.geotiffFilePath != null && !this.geotiffFilePath.isBlank()) {
            GaiaGeoTiffManager gaiaGeoTiffManager = this.terrainElevDataManager.getGaiaGeoTiffManager();
            wasCached = gaiaGeoTiffManager.hasCachedRaster(this.geotiffFilePath);
            this.raster = gaiaGeoTiffManager.loadGeoTiffRaster(this.geotiffFilePath);
        } else {
            this.raster = renderedImage.getData();
        }
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
        initializeRasterAccessor();
        if (!rasterInitializationLogged) {
            rasterInitializationLogged = true;
            log.info("[Raster][SampleInit] Loaded raster for {} in {} ms ({}x{}, cached={})",
                geotiffFilePath, elapsedMs, rasterWidth, rasterHeight, wasCached);
        }
        this.coverage = null;
        return true;
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
        initializeRawRasterAccess(this.raster);
        validateDirectRasterAccess();
    }

    private void initializeRawRasterAccess(Raster sourceRaster) {
        this.useDirectRasterAccess = false;
        if (sourceRaster.getNumBands() != 1) {
            return;
        }

        if (!(sourceRaster.getSampleModel() instanceof ComponentSampleModel componentSampleModel)) {
            return;
        }

        ComponentSampleModel sampleModel = (ComponentSampleModel) sourceRaster.getSampleModel();
        DataBuffer dataBuffer = sourceRaster.getDataBuffer();
        int bankIndex = sampleModel.getBankIndices()[0];
        this.rasterBaseOffset = dataBuffer.getOffsets()[bankIndex] + sampleModel.getBandOffsets()[0];
        this.rasterPixelStride = sampleModel.getPixelStride();
        this.rasterScanlineStride = sampleModel.getScanlineStride();

        if (dataBuffer instanceof DataBufferFloat floatBuffer) {
            this.rasterFloatData = floatBuffer.getData(bankIndex);
            this.rasterDataKind = RASTER_KIND_FLOAT;
            return;
        }

        if (dataBuffer instanceof DataBufferDouble doubleBuffer) {
            this.rasterDoubleData = doubleBuffer.getData(bankIndex);
            this.rasterDataKind = RASTER_KIND_DOUBLE;
            return;
        }

        if (dataBuffer instanceof DataBufferInt intBuffer) {
            this.rasterIntData = intBuffer.getData(bankIndex);
            this.rasterDataKind = RASTER_KIND_INT;
            return;
        }

        if (dataBuffer instanceof DataBufferUShort ushortBuffer) {
            this.rasterShortData = ushortBuffer.getData(bankIndex);
            this.rasterDataKind = RASTER_KIND_USHORT;
            return;
        }

        if (dataBuffer instanceof DataBufferShort shortBuffer) {
            this.rasterShortData = shortBuffer.getData(bankIndex);
            this.rasterDataKind = RASTER_KIND_SHORT;
            return;
        }

        if (dataBuffer instanceof DataBufferByte byteBuffer) {
            this.rasterByteData = byteBuffer.getData(bankIndex);
            this.rasterDataKind = RASTER_KIND_BYTE;
            return;
        }
    }

    private double readGridValueDirect(int x, int y) {
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

    private double readSample(int x, int y) {
        if (!useDirectRasterAccess) {
            return raster.getSampleDouble(x, y, 0);
        }

        int localX = x - rasterSampleModelTranslateX;
        int localY = y - rasterSampleModelTranslateY;
        int index = rasterBaseOffset + localY * rasterScanlineStride + localX * rasterPixelStride;
        switch (rasterDataKind) {
            case RASTER_KIND_FLOAT:
                return rasterFloatData[index];
            case RASTER_KIND_DOUBLE:
                return rasterDoubleData[index];
            case RASTER_KIND_INT:
                return rasterIntData[index];
            case RASTER_KIND_USHORT:
                return rasterShortData[index] & 0xFFFF;
            case RASTER_KIND_SHORT:
                return rasterShortData[index];
            case RASTER_KIND_BYTE:
                return rasterByteData[index] & 0xFF;
            default:
                return raster.getSampleDouble(x, y, 0);
        }
    }

    private void validateDirectRasterAccess() {
        if (this.raster == null || this.rasterDataKind == RASTER_KIND_GENERIC) {
            this.useDirectRasterAccess = false;
            return;
        }

        int sampleX0 = this.rasterMinX;
        int sampleY0 = this.rasterMinY;
        int sampleX1 = this.rasterMinX + Math.max(0, this.rasterWidth / 2);
        int sampleY1 = this.rasterMinY + Math.max(0, this.rasterHeight / 2);

        try {
            boolean matchesOrigin = isDirectSampleMatch(sampleX0, sampleY0);
            boolean matchesCenter = isDirectSampleMatch(sampleX1, sampleY1);
            this.useDirectRasterAccess = matchesOrigin && matchesCenter;
            if (!this.useDirectRasterAccess) {
                log.warn("[Raster][SampleInit] Direct raster access disabled for {} due to sample mismatch.", geotiffFilePath);
            }
        } catch (Exception e) {
            this.useDirectRasterAccess = false;
            log.warn("[Raster][SampleInit] Direct raster access validation failed for {}. Falling back to Raster API.", geotiffFilePath, e);
        }
    }

    private boolean isDirectSampleMatch(int x, int y) {
        double rasterValue = raster.getSampleDouble(x, y, 0);
        double directValue = readSampleUnchecked(x, y);
        return Double.compare(rasterValue, directValue) == 0 || Math.abs(rasterValue - directValue) < 1e-6;
    }

    private double readSampleUnchecked(int x, int y) {
        int localX = x - rasterSampleModelTranslateX;
        int localY = y - rasterSampleModelTranslateY;
        int index = rasterBaseOffset + localY * rasterScanlineStride + localX * rasterPixelStride;
        switch (rasterDataKind) {
            case RASTER_KIND_FLOAT:
                return rasterFloatData[index];
            case RASTER_KIND_DOUBLE:
                return rasterDoubleData[index];
            case RASTER_KIND_INT:
                return rasterIntData[index];
            case RASTER_KIND_USHORT:
                return rasterShortData[index] & 0xFFFF;
            case RASTER_KIND_SHORT:
                return rasterShortData[index];
            case RASTER_KIND_BYTE:
                return rasterByteData[index] & 0xFF;
            default:
                return raster.getSampleDouble(x, y, 0);
        }
    }

    public boolean preloadRaster() {
        return ensureRasterInitialized();
    }

    public boolean isRasterLoaded() {
        return this.raster != null;
    }

    public long estimateRasterBytes() {
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
