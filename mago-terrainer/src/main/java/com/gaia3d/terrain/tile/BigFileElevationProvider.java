package com.gaia3d.terrain.tile;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.structure.GeographicExtension;
import com.gaia3d.terrain.util.GaiaGeoTiffUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.GeometryFactory;
import org.joml.Vector2d;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Optimized Elevation Provider for Large GeoTIFF files.
 * It reads only the required portion of the GeoTIFF file into memory.
 */
@Slf4j
@Getter
public class BigFileElevationProvider implements AutoCloseable {
    private final File geoTiffFile;
    private final GeoTiffReader reader;
    private final CoordinateReferenceSystem sourceCrs;
    private final CoordinateReferenceSystem targetCrs;
    private final GeographicExtension fileExtent;
    private final GridGeometry2D originalGridGeometry;
    private final double pixelSizeMeters;
    private MathTransform targetToSourceTransform;
    private GridCoverage2D currentTrunkCoverage;
    private ReferencedEnvelope currentTrunkEnvelope;

    public BigFileElevationProvider(File file) throws Exception {
        this.geoTiffFile = file;
        this.reader = new GeoTiffReader(file);
        this.sourceCrs = reader.getCoordinateReferenceSystem();
        this.targetCrs = GlobalOptions.getInstance().getOutputCRS();
        this.targetToSourceTransform = CRS.findMathTransform(targetCrs, sourceCrs, true);
        
        // 核心修复：直接从 reader 获取原始的 GridEnvelope 和 Envelope。
        GridEnvelope gridRange = reader.getOriginalGridRange();
        ReferencedEnvelope envelope = new ReferencedEnvelope(reader.getOriginalEnvelope());
        this.originalGridGeometry = new GridGeometry2D(gridRange, envelope);
        
        // 计算像元大小（度）
        double resX = envelope.getSpan(0) / gridRange.getSpan(0);
        double resY = envelope.getSpan(1) / gridRange.getSpan(1);
        
        // 估计 pixelSizeMeters
        double centerLat = envelope.getMedian(1);
        this.pixelSizeMeters = resX * Math.cos(Math.toRadians(centerLat)) * 111319.9; 

        this.fileExtent = new GeographicExtension();
        ReferencedEnvelope targetEnvelope = envelope.transform(targetCrs, true);
        this.fileExtent.setDegrees(targetEnvelope.getMinX(), targetEnvelope.getMinY(), 0.0, targetEnvelope.getMaxX(), targetEnvelope.getMaxY(), 0.0);
        
        log.info("Initialized BigFileProvider (Metadata Only) for: {}, Extent: {}", file.getName(), fileExtent.toString());
    }

    /**
     * Loads a specific geographic area into memory with resolution control.
     */
    public void loadTrunk(ReferencedEnvelope envelope, int targetWidth, int targetHeight) throws Exception {
        if (currentTrunkCoverage != null) {
            currentTrunkCoverage.dispose(true);
            currentTrunkCoverage = null;
        }
        
        log.info("[BigFile] Loading area: {} at resolution {}x{}", envelope.toString(), targetWidth, targetHeight);
        
        // Transform target envelope back to source CRS
        ReferencedEnvelope sourceEnvelope = envelope.transform(sourceCrs, true);
        
        // 1. Calculate pixel range using GeoTools built-in worldToGrid
        org.geotools.api.coverage.grid.GridEnvelope fullRange = originalGridGeometry.getGridRange();
        GridEnvelope worldGridRange;
        try {
            worldGridRange = originalGridGeometry.worldToGrid(sourceEnvelope);
        } catch (Exception e) {
            log.warn("[BigFile] worldToGrid failed, falling back to manual coordinate calculation.");
            ReferencedEnvelope fullEnv = new ReferencedEnvelope(reader.getOriginalEnvelope());
            double resX = fullEnv.getSpan(0) / fullRange.getSpan(0);
            double resY = fullEnv.getSpan(1) / fullRange.getSpan(1);
            int lowX = (int) Math.floor((sourceEnvelope.getMinX() - fullEnv.getMinX()) / resX);
            int highX = (int) Math.ceil((sourceEnvelope.getMaxX() - fullEnv.getMinX()) / resX);
            int lowY = (int) Math.floor((fullEnv.getMaxY() - sourceEnvelope.getMaxY()) / resY);
            int highY = (int) Math.ceil((fullEnv.getMaxY() - sourceEnvelope.getMinY()) / resY);
            worldGridRange = new GeneralGridEnvelope(new int[]{lowX, lowY}, new int[]{highX, highY});
        }
        
        // 2. Safely extract and clamp coordinates (CRITICAL: clamp to 0 based)
        int gMinX = Math.max(fullRange.getLow(0), worldGridRange.getLow(0));
        int gMinY = Math.max(fullRange.getLow(1), worldGridRange.getLow(1));
        int gMaxX = Math.min(fullRange.getHigh(0), worldGridRange.getHigh(0));
        int gMaxY = Math.min(fullRange.getHigh(1), worldGridRange.getHigh(1));

        if (gMaxX < gMinX) gMaxX = gMinX;
        if (gMaxY < gMinY) gMaxY = gMinY;

        // Ensure we don't request a zero-sized raster
        int readWidth = Math.max(1, gMaxX - gMinX + 1);
        int readHeight = Math.max(1, gMaxY - gMinY + 1);
        
        // Final safety limit for single raster constraints
        if (readWidth > 16384) readWidth = 16384;
        if (readHeight > 16384) readHeight = 16384;
        gMaxX = gMinX + readWidth - 1;
        gMaxY = gMinY + readHeight - 1;

        GeneralGridEnvelope subGridRange = new GeneralGridEnvelope(new int[]{gMinX, gMinY}, new int[]{gMaxX, gMaxY});
        
        // 3. Create the sub-grid geometry for reading. 
        GridGeometry2D readGG = new GridGeometry2D(subGridRange, sourceEnvelope);
        ParameterValue readGridGeometry = AbstractGridFormat.READ_GRIDGEOMETRY2D.createValue();
        readGridGeometry.setValue(readGG);
        
        GeneralParameterValue[] params = new GeneralParameterValue[]{readGridGeometry};
        GridCoverage2D tempCoverage = reader.read(params);
        GridCoverage2D materializedCoverage = GaiaGeoTiffUtils.materializeGridCoverage2D(tempCoverage);
        if (materializedCoverage != tempCoverage && tempCoverage != null) {
            tempCoverage.dispose(true);
        }

        this.currentTrunkCoverage = materializedCoverage;
        this.currentTrunkEnvelope = envelope;
    }

    /**
     * Loads a specific geographic area at full resolution.
     */
    public void loadTrunkFullRes(ReferencedEnvelope envelope) throws Exception {
        loadTrunk(envelope, -1, -1);
    }

    public float getElevation(double lon, double lat) {
        if (currentTrunkCoverage == null) return Float.NaN;
        
        try {
            // Transform input point (target CRS) back to source CRS for evaluation
            double[] sourceCoord = new double[]{lon, lat};
            targetToSourceTransform.transform(sourceCoord, 0, sourceCoord, 0, 1);

            java.awt.geom.Point2D point = new java.awt.geom.Point2D.Double(sourceCoord[0], sourceCoord[1]);
            float[] result = new float[1];
            currentTrunkCoverage.evaluate(point, result);
            return result[0];
        } catch (Exception e) {
            return Float.NaN;
        }
    }

    @Override
    public void close() {
        if (currentTrunkCoverage != null) {
            currentTrunkCoverage.dispose(true);
        }
        if (reader != null) {
            reader.dispose();
        }
    }
}
