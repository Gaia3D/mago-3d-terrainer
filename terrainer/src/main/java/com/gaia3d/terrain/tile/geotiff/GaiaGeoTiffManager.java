package com.gaia3d.terrain.tile.geotiff;

import com.gaia3d.terrain.util.GaiaGeoTiffUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.coverage.processing.operation.Resample;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.joml.Vector2i;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class GaiaGeoTiffManager {
    private final String PROJECTION_CRS = "EPSG:3857";
    private int[] pixel = new int[1];
    private double[] originalUpperLeftCorner = new double[2];
    private Map<String, GridCoverage2D> mapPathGridCoverage2d = new HashMap<>();
    private Map<String, Vector2i> mapPathGridCoverage2dSize = new HashMap<>();
    private Map<String, String> mapGeoTiffToGeoTiff4326 = new HashMap<>();

    public GridCoverage2D loadGeoTiffGridCoverage2D(String geoTiffFilePath) {
        if (mapPathGridCoverage2d.containsKey(geoTiffFilePath)) {
            return mapPathGridCoverage2d.get(geoTiffFilePath);
        }

        if (mapGeoTiffToGeoTiff4326.containsKey(geoTiffFilePath)) {
            String geoTiff4326FilePath = mapGeoTiffToGeoTiff4326.get(geoTiffFilePath);
            if (mapPathGridCoverage2d.containsKey(geoTiff4326FilePath)) {
                return mapPathGridCoverage2d.get(geoTiff4326FilePath);
            }
        }

        log.info("[Raster][I/O] loading the geoTiff file: {}", geoTiffFilePath);

        int gridCoverage2dCount = mapPathGridCoverage2d.size();
        if (gridCoverage2dCount > 0) {
            // delete the first one
            String firstKey = mapPathGridCoverage2d.keySet().iterator().next();
            GridCoverage2D firstCoverage = mapPathGridCoverage2d.get(firstKey);
            firstCoverage.dispose(true);
            mapPathGridCoverage2d.remove(firstKey);
        }

        GridCoverage2D coverage = null;
        try {
            File file = new File(geoTiffFilePath);
            GeoTiffReader reader = new GeoTiffReader(file);
            coverage = reader.read(null);
            reader.dispose();
        } catch (Exception e) {
            log.error("Error:", e);
        }

        // save the coverage
        mapPathGridCoverage2d.put(geoTiffFilePath, coverage);

        // save the width and height of the coverage
        GridGeometry gridGeometry = coverage.getGridGeometry();
        int width = gridGeometry.getGridRange().getSpan(0);
        int height = gridGeometry.getGridRange().getSpan(1);
        Vector2i size = new Vector2i(width, height);
        mapPathGridCoverage2dSize.put(geoTiffFilePath, size);

        log.debug("Loaded the geoTiff file ok");
        return coverage;
    }

    public Vector2i getGridCoverage2DSize(String geoTiffFilePath) {
        if (!mapPathGridCoverage2dSize.containsKey(geoTiffFilePath)) {
            GridCoverage2D coverage = loadGeoTiffGridCoverage2D(geoTiffFilePath);
            coverage.dispose(true);
        }
        return mapPathGridCoverage2dSize.get(geoTiffFilePath);
    }

    public void deleteObjects() {
        for (GridCoverage2D coverage : mapPathGridCoverage2d.values()) {
            coverage.dispose(true);
        }
        mapPathGridCoverage2d.clear();

    }

    public GridCoverage2D getResizedCoverage2D(GridCoverage2D originalCoverage, double desiredPixelSizeXinMeters, double desiredPixelSizeYinMeters) throws FactoryException {
        GridCoverage2D resizedCoverage = null;

        GridGeometry originalGridGeometry = originalCoverage.getGridGeometry();
        Envelope envelopeOriginal = originalCoverage.getEnvelope();

        int gridSpanX = originalGridGeometry.getGridRange().getSpan(0); // num of pixels
        int gridSpanY = originalGridGeometry.getGridRange().getSpan(1); // num of pixels
        double[] envelopeSpanMeters = new double[2];
        GaiaGeoTiffUtils.getEnvelopeSpanInMetersOfGridCoverage2D(originalCoverage, envelopeSpanMeters);
        double envelopeSpanX = envelopeSpanMeters[0]; // in meters
        double envelopeSpanY = envelopeSpanMeters[1]; // in meters

        double desiredPixelsCountX = envelopeSpanX / desiredPixelSizeXinMeters;
        double desiredPixelsCountY = envelopeSpanY / desiredPixelSizeYinMeters;
        int minXSize = 24;
        int minYSize = 24;
        int desiredImageWidth = Math.max((int) desiredPixelsCountX, minXSize);
        int desiredImageHeight = Math.max((int) desiredPixelsCountY, minYSize);


        double scaleX = (double) desiredImageWidth / (double) gridSpanX;
        double scaleY = (double) desiredImageHeight / (double) gridSpanY;

        Operations ops = new Operations(null);
        resizedCoverage = (GridCoverage2D) ops.scale(originalCoverage, scaleX, scaleY, 0, 0);

        originalUpperLeftCorner[0] = envelopeOriginal.getMinimum(0);
        originalUpperLeftCorner[1] = envelopeOriginal.getMinimum(1);

        return resizedCoverage;
    }

    public void saveGridCoverage2D(GridCoverage2D coverage, String outputFilePath) throws IOException {
        // now save the newCoverage as geotiff
        File outputFile = new File(outputFilePath);
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        GeoTiffWriter writer = new GeoTiffWriter(bufferedOutputStream);
        writer.write(coverage, null);
        writer.dispose();
        outputStream.close();
    }
}
