package com.gaia3d.terrain.tile.geotiff;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.util.GaiaGeoTiffUtils;
import com.gaia3d.util.FileUtils;
import com.gaia3d.util.StringUtils;
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
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
        return loadGeoTiffGridCoverage2D(geoTiffFilePath, false);
    }

    public GridCoverage2D loadGeoTiffGridCoverage2D(String geoTiffFilePath, boolean reprojection) {
        if (mapPathGridCoverage2d.containsKey(geoTiffFilePath)) {
            return mapPathGridCoverage2d.get(geoTiffFilePath);
        }

        if (mapGeoTiffToGeoTiff4326.containsKey(geoTiffFilePath)) {
            String geoTiff4326FilePath = mapGeoTiffToGeoTiff4326.get(geoTiffFilePath);
            if (mapPathGridCoverage2d.containsKey(geoTiff4326FilePath)) {
                return mapPathGridCoverage2d.get(geoTiff4326FilePath);
            }
        }

        log.info("Loading the geoTiff file: {}", geoTiffFilePath);

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

        // check if the coverage is in EPSG:4326. If not, reproject it.***
        CoordinateReferenceSystem crs = null;
        try {
            crs = coverage.getCoordinateReferenceSystem();
        } catch (Exception e) {
            log.error("Error:", e);
        }

        if (crs != null) {
            String crsCode = null;
            Set<ReferenceIdentifier> identifiers = crs.getIdentifiers();
            if (identifiers != null) {
                Iterator<ReferenceIdentifier> iterator = identifiers.iterator();
                if (iterator.hasNext()) {
                    crsCode = iterator.next().toString();
                } else {
                    crsCode = crs.getName().toString();
                }
            } else {
                crsCode = crs.getName().toString();
            }

            /*if (reprojection && crsCode != null && !crsCode.equals(PROJECTION_CRS)) {
                // reproject the coverage to EPSG:4326
                log.info("Reprojecting the coverage to {}", PROJECTION_CRS);
                String geoTiff4326FolderPath = GlobalOptions.getInstance().getTileTempPath() + File.separator + "reprojection";
                File originalFile = new File(geoTiffFilePath);
                String originalFileName = originalFile.getName();
                String geoTiff4326FileName = StringUtils.getRawFileName(originalFileName) + "_reprojection.tif";
                String geoTiff4326FilePath = geoTiff4326FolderPath + File.separator + geoTiff4326FileName;
                // check if exist the file "geoTiff4326FilePath".***
                if (!FileUtils.isFileExists(geoTiff4326FilePath)) {
                    FileUtils.createAllFoldersIfNoExist(geoTiff4326FolderPath);
                    try {
                        GeoTiffReprojector reprojector = new GeoTiffReprojector();
                        coverage = reprojector.reproject(coverage, CRS.decode(PROJECTION_CRS));
                        //coverage = executeResampling(coverage, CRS.decode(PROJECTION_CRS));
                        log.info("Saving the reprojected coverage to {}", PROJECTION_CRS);
                        saveGridCoverage2D(coverage, geoTiff4326FilePath);
                        mapGeoTiffToGeoTiff4326.put(geoTiffFilePath, geoTiff4326FilePath);
                    } catch (Exception e) {
                        log.error("Error:", e);
                    }
                } else {
                    // load the coverage from the file
                    try {
                        File file = new File(geoTiff4326FilePath);
                        GeoTiffReader reader = new GeoTiffReader(file);
                        log.info("Loading the reprojected coverage from {}", PROJECTION_CRS);
                        coverage = reader.read(null);
                        reader.dispose();
                        mapGeoTiffToGeoTiff4326.put(geoTiffFilePath, geoTiff4326FilePath);
                    } catch (Exception e) {
                        log.error("Error:", e);
                    }
                }
            }*/
        }

        // check if the coverage is in EPSG:4326. If not, reproject it.***
            /*if (crsCode != null && !crsCode.equals("EPSG:4326")) {
                // reproject the coverage to EPSG:4326
                log.info("Reprojecting the coverage to EPSG:4326...");
                String geoTiff4326FolderPath = GlobalOptions.getInstance().getTileTempPath() + File.separator + "temp4326";
                File originalFile = new File(geoTiffFilePath);
                String originalFileName = originalFile.getName();
                String geoTiff4326FileName = StringUtils.getRawFileName(originalFileName) + "_4326.tif";
                String geoTiff4326FilePath = geoTiff4326FolderPath + File.separator + geoTiff4326FileName;
                // check if exist the file "geoTiff4326FilePath".***
                if (!FileUtils.isFileExists(geoTiff4326FilePath)) {
                    FileUtils.createAllFoldersIfNoExist(geoTiff4326FolderPath);
                    try {
                        coverage = reprojectGridCoverage(coverage, CRS.decode("EPSG:4326"));
                        log.debug("Saving the reprojected coverage to EPSG:4326...");
                        saveGridCoverage2D(coverage, geoTiff4326FilePath);
                        mapGeoTiffToGeoTiff4326.put(geoTiffFilePath, geoTiff4326FilePath);
                    } catch (Exception e) {
                        log.error("Error:", e);
                    }
                }
                else {
                    // load the coverage from the file
                    try {
                        File file = new File(geoTiff4326FilePath);
                        GeoTiffReader reader = new GeoTiffReader(file);
                        log.debug("Loading the reprojected coverage from EPSG:4326...");
                        coverage = reader.read(null);
                        reader.dispose();
                    } catch (Exception e) {
                        log.error("Error:", e);
                    }
                }
            }
        }*/
        // end check if the coverage is in EPSG:4326. If not, reproject it.***

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

    /*public GridCoverage2D warpCoverage(GridCoverage2D srcCoverage) {
        // Warp를 사용한 재투영
        //GridGeometry2D gridGeometry2D = srcCoverage.getGridGeometry();
        *//*AffineTransform affineTransform = new AffineTransform();
        AffineTransform2D affineTransform2D = new AffineTransform2D(affineTransform);
        WarpAffine warpAffine = new WarpAffine(affineTransform2D);*//*

        //WarpTransform2DProvider warpTransform2DProvider = new WarpTransform2DProvider();

        MathTransform2D transform = null;
        try {
            transform = (MathTransform2D) CRS.findMathTransform(srcCoverage.getCoordinateReferenceSystem(), DefaultGeographicCRS.WGS84, true);
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }


        WarpTransform2D warpTransform2D = (WarpTransform2D) transform;
        Warp warp = warpTransform2D.getWarp();


        AffineTransform affineTransform = new AffineTransform();
        AffineTransform2D affineTransform2D = (AffineTransform2D) transform;
        //WarpTransform2D warpTransform2D = new WarpTransform2D(transform);
        WarpAffine warpAffine = new WarpAffine((AffineTransform) transform);

        Operations operations = Operations.DEFAULT;
        return (GridCoverage2D) operations.warp(srcCoverage, warpAffine);
    }*/


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

    public GridCoverage2D extractSubGridCoverage2D(GridCoverage2D originalGridCoverage2D, ReferencedEnvelope tileEnvelope) {
        GridCoverage2D subGridCoverage2D = null;
        try {
            Operations ops = new Operations(null);
            subGridCoverage2D = (GridCoverage2D) ops.crop(originalGridCoverage2D, tileEnvelope);
        } catch (Exception e) {
            log.error("Error:", e);
        }
        return subGridCoverage2D;
    }

    public GridCoverage2D executeResampling(GridCoverage2D sourceCoverage, CoordinateReferenceSystem targetCRS) {
        RenderedImage image = sourceCoverage.getRenderedImage();
        int width = image.getWidth();
        int height = image.getHeight();
        GeneralGridEnvelope newGridRange = new GeneralGridEnvelope(new int[]{0, 0}, new int[]{width, height});
        GridGeometry2D newGridGeometry = new GridGeometry2D(newGridRange, sourceCoverage.getEnvelope());

        //CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84_3D;
        Resample resample = new Resample();
        ParameterValueGroup resampleParameters = resample.getParameters();
        resampleParameters.parameter("Source").setValue(sourceCoverage);
        if (targetCRS != null) {
            resampleParameters.parameter("CoordinateReferenceSystem").setValue(targetCRS);
        }
        resampleParameters.parameter("GridGeometry").setValue(newGridGeometry);
        return (GridCoverage2D) resample.doOperation(resampleParameters, null);
    }

    public GridCoverage2D reprojectGridCoverage(GridCoverage2D sourceCoverage, CoordinateReferenceSystem targetCRS) throws Exception {
        // Obtén los CRS de origen y destino
        CoordinateReferenceSystem sourceCRS = sourceCoverage.getCoordinateReferenceSystem();

        // Crea la transformación entre CRS
        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);

        // Reproyecta el GridCoverage2D
        Operations ops = Operations.DEFAULT;
        GridCoverage2D targetCoverage = (GridCoverage2D) ops.resample(sourceCoverage, targetCRS);

        return targetCoverage;
    }

}
