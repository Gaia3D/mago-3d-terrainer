package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GaiaTriangle;
import com.gaia3d.basic.structure.GeographicExtension;
import com.gaia3d.reader.FileUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Slf4j
public class TerrainElevationDataManager {
    private List<TerrainElevationData> memSaveTerrainElevDatasArray = new ArrayList<>();
    private List<GaiaTriangle> memSaveTrianglesArray = new ArrayList<>();
    private Map<String, TileWgs84Raster> mapIndicesTileRaster = new HashMap<>();
    // Inside the folder, there are multiple geoTiff files

    private String terrainElevationDataFolderPath;

    private int geoTiffFilesCount = 0;

    private String uniqueGeoTiffFilePath = null; // use this if there is only one geoTiff file
    private TerrainElevationData uniqueTerrainElevationData = null; // use this if there is only one geoTiff file
    // if there are multiple geoTiff files, use this
    private int quadtreeMaxDepth = 10;
    private TerrainElevationDataQuadTree rootTerrainElevationDataQuadTree = null;
    private GaiaGeoTiffManager gaiaGeoTiffManager = null;
    private boolean[] memSaveIntersects = {false};
    private List<String> memSaveGeoTiffFileNames = new ArrayList<>();

    public void makeTerrainQuadTree() throws FactoryException, TransformException, IOException {
        // load all geoTiffFiles & make a quadTree
        loadAllGeoTiff(terrainElevationDataFolderPath);
        rootTerrainElevationDataQuadTree.makeQuadTree(quadtreeMaxDepth);
    }

    public void MakeUniqueTerrainElevationData() throws FactoryException, TransformException, IOException {
        log.info("MakeUniqueTerrainElevationData() started");

        if (uniqueGeoTiffFilePath == null) {
            return;
        }


        if (gaiaGeoTiffManager == null) {
            gaiaGeoTiffManager = new GaiaGeoTiffManager();
        }

        uniqueTerrainElevationData = new TerrainElevationData(this);
        GridCoverage2D gridCoverage2D = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(uniqueGeoTiffFilePath);
        uniqueTerrainElevationData.setGeotiffFilePath(uniqueGeoTiffFilePath);

        CoordinateReferenceSystem crsTarget = gridCoverage2D.getCoordinateReferenceSystem2D();
        CoordinateReferenceSystem crsWgs84 = CRS.decode("EPSG:4326", true);
        MathTransform targetToWgs = CRS.findMathTransform(crsTarget, crsWgs84);

        GeometryFactory gf = new GeometryFactory();
        GaiaGeoTiffUtils.getGeographicExtension(gridCoverage2D, gf, targetToWgs, uniqueTerrainElevationData.getGeographicExtension());
        uniqueTerrainElevationData.setPixelSizeMeters(GaiaGeoTiffUtils.getPixelSizeMeters(gridCoverage2D));

        gridCoverage2D.dispose(true);
        log.info("MakeUniqueTerrainElevationData() ended");
    }

    public TileWgs84Raster getTileWgs84Raster(TileIndices tileIndices, TileWgs84Manager tileWgs84Manager) throws TransformException, IOException {
        TileWgs84Raster tileWgs84Raster = mapIndicesTileRaster.get(tileIndices.getString());
        if (tileWgs84Raster == null) {
            tileWgs84Raster = new TileWgs84Raster(tileIndices, tileWgs84Manager);
            int tileRasterWidth = tileWgs84Manager.getTileRasterSize();
            int tileRasterHeight = tileWgs84Manager.getTileRasterSize();
            tileWgs84Raster.makeElevations(this, tileRasterWidth, tileRasterHeight);
            mapIndicesTileRaster.put(tileIndices.getString(), tileWgs84Raster);
        }

        return tileWgs84Raster;
    }

    public void deleteTileRasters() {
        for (TileWgs84Raster tileWgs84Raster : mapIndicesTileRaster.values()) {
            tileWgs84Raster.deleteObjects();
        }

        mapIndicesTileRaster.clear();

    }


    public GeographicExtension getRootGeographicExtension() {
        if (this.geoTiffFilesCount == 1) {
            if (uniqueTerrainElevationData == null) {
                return null;
            }

            return uniqueTerrainElevationData.getGeographicExtension();
        }

        if (rootTerrainElevationDataQuadTree == null) {
            return null;
        }

        return rootTerrainElevationDataQuadTree.getGeographicExtension();
    }

    public void deleteCoverage() {
        if (rootTerrainElevationDataQuadTree == null) {
            return;
        }
        rootTerrainElevationDataQuadTree.deleteCoverage();
    }

    public void deleteCoverageIfNotIntersects(GeographicExtension geographicExtension) {
        if (rootTerrainElevationDataQuadTree == null) {
            return;
        }
        rootTerrainElevationDataQuadTree.deleteCoverageIfNotIntersects(geographicExtension);
    }

    public void deleteObjects() {
        this.deleteTileRasters();
        if (rootTerrainElevationDataQuadTree == null) {
            return;
        }

        rootTerrainElevationDataQuadTree.deleteObjects();
        rootTerrainElevationDataQuadTree = null;
    }

    public double getElevation(double lonDeg, double latDeg, List<TerrainElevationData> memSaveTerrainElevDatasArray) throws TransformException, IOException {
        double resultElevation = 0.0;

        if (this.geoTiffFilesCount == 1) {
            if (uniqueTerrainElevationData == null) {
                return resultElevation;
            }

            resultElevation = uniqueTerrainElevationData.getElevation(lonDeg, latDeg, memSaveIntersects);
            return resultElevation;
        }

        if (rootTerrainElevationDataQuadTree == null) {
            return resultElevation;
        }

        memSaveTerrainElevDatasArray.clear();
        rootTerrainElevationDataQuadTree.getTerrainElevationDatasArray(lonDeg, latDeg, memSaveTerrainElevDatasArray);

        memSaveIntersects[0] = false;
        for (TerrainElevationData terrainElevationData : memSaveTerrainElevDatasArray) {
            double elevation = terrainElevationData.getElevation(lonDeg, latDeg, memSaveIntersects);
            if (!memSaveIntersects[0]) {
                continue;
            }

            if (elevation < 0.0) {
                elevation = 0.0;
            }

            resultElevation = elevation;
            break;
        }

        return resultElevation;
    }

    public double getElevationNearest(double lonDeg, double latDeg, List<TerrainElevationData> memSaveTerrainElevDatasArray) throws TransformException, IOException {
        double resultElevation = 0.0;

        if (this.geoTiffFilesCount == 1) {
            if (uniqueTerrainElevationData == null) {
                return resultElevation;
            }

            resultElevation = uniqueTerrainElevationData.getElevationNearest(lonDeg, latDeg, memSaveIntersects);
            return resultElevation;
        }

        if (rootTerrainElevationDataQuadTree == null) {
            return resultElevation;
        }

        memSaveTerrainElevDatasArray.clear();
        rootTerrainElevationDataQuadTree.getTerrainElevationDatasArray(lonDeg, latDeg, memSaveTerrainElevDatasArray);

        memSaveIntersects[0] = false;
        for (TerrainElevationData terrainElevationData : memSaveTerrainElevDatasArray) {
            double elevation = terrainElevationData.getElevationNearest(lonDeg, latDeg, memSaveIntersects);
            if (!memSaveIntersects[0]) {
                continue;
            }

            if (elevation < 0.0) {
                elevation = 0.0;
            }

            resultElevation = elevation;
            break;
        }

        return resultElevation;
    }

    private void loadAllGeoTiff(String terrainElevationDataFolderPath) throws FactoryException, TransformException {
        // load all geoTiffFiles
        memSaveGeoTiffFileNames.clear();
        FileUtils.getFileNames(terrainElevationDataFolderPath, ".tif", memSaveGeoTiffFileNames);

        if (gaiaGeoTiffManager == null) {
            gaiaGeoTiffManager = new GaiaGeoTiffManager();
        }
        GeometryFactory gf = new GeometryFactory();

        if (rootTerrainElevationDataQuadTree == null) {
            rootTerrainElevationDataQuadTree = new TerrainElevationDataQuadTree(null);
        }

        // now load all geotiff and make geotiff geoExtension data
        GridCoverage2D gridCoverage2D = null;
        String geoTiffFileName = null;
        String geoTiffFilePath = null;

        CoordinateReferenceSystem crsTarget = null;
        CoordinateReferenceSystem crsWgs84 = null;
        MathTransform targetToWgs = null;

        for (String memSaveGeoTiffFileName : memSaveGeoTiffFileNames) {
            geoTiffFileName = memSaveGeoTiffFileName;
            geoTiffFilePath = terrainElevationDataFolderPath + File.separator + geoTiffFileName;
            TerrainElevationData terrainElevationData = new TerrainElevationData(this);

            gridCoverage2D = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(geoTiffFilePath);
            terrainElevationData.setGeotiffFilePath(geoTiffFilePath);

            crsTarget = gridCoverage2D.getCoordinateReferenceSystem2D();
            crsWgs84 = CRS.decode("EPSG:4326", true);
            targetToWgs = CRS.findMathTransform(crsTarget, crsWgs84);

            GaiaGeoTiffUtils.getGeographicExtension(gridCoverage2D, gf, targetToWgs, terrainElevationData.getGeographicExtension());
            terrainElevationData.setPixelSizeMeters(GaiaGeoTiffUtils.getPixelSizeMeters(gridCoverage2D));

            rootTerrainElevationDataQuadTree.addTerrainElevationData(terrainElevationData);
            gridCoverage2D.dispose(true);

        }

        // now check if exist folders inside the terrainElevationDataFolderPath
        List<String> folderNames = new ArrayList<>();
        FileUtils.getFolderNames(terrainElevationDataFolderPath, folderNames);
        for (String folderName : folderNames) {
            String folderPath = terrainElevationDataFolderPath + File.separator + folderName;
            loadAllGeoTiff(folderPath);
        }
    }
}
