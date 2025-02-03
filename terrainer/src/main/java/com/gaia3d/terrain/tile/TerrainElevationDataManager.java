package com.gaia3d.terrain.tile;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.structure.TerrainTriangle;
import com.gaia3d.terrain.structure.GeographicExtension;
import com.gaia3d.terrain.tile.geotiff.GaiaGeoTiffManager;
import com.gaia3d.terrain.types.PriorityType;
import com.gaia3d.terrain.util.GaiaGeoTiffUtils;
import com.gaia3d.util.FileUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.referencing.CRS;
import org.joml.Vector2d;
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
    private static GlobalOptions globalOptions = GlobalOptions.getInstance();

    private TileWgs84Manager tileWgs84Manager = null;
    private List<TerrainElevationData> terrainElevationDataArray = new ArrayList<>();
    private List<TerrainTriangle> trianglesArray = new ArrayList<>();
    private Map<String, TileWgs84Raster> mapIndicesTileRaster = new HashMap<>();
    private Map<String, Double> gridAreaMap = new HashMap<>();

    // Inside the folder, there are multiple geoTiff files
    private String terrainElevationDataFolderPath;
    private int geoTiffFilesCount = 0;
    private String uniqueGeoTiffFilePath = null; // use this if there is only one geoTiff file
    private TerrainElevationData uniqueTerrainElevationData = null; // use this if there is only one geoTiff file

    // if there are multiple geoTiff files, use this
    private int quadtreeMaxDepth = 10;
    private TerrainElevationDataQuadTree rootTerrainElevationDataQuadTree = null;
    private GaiaGeoTiffManager myGaiaGeoTiffManager = null;
    private boolean[] intersects = {false};
    private List<String> geoTiffFileNames = new ArrayList<>();

    public void makeTerrainQuadTree() throws FactoryException, TransformException, IOException {
        // load all geoTiffFiles & make a quadTree
        loadAllGeoTiff(terrainElevationDataFolderPath);
        rootTerrainElevationDataQuadTree.makeQuadTree(quadtreeMaxDepth);
    }

    public GaiaGeoTiffManager getGaiaGeoTiffManager() {
        if (myGaiaGeoTiffManager == null) {
            myGaiaGeoTiffManager = new GaiaGeoTiffManager();
        }
        return myGaiaGeoTiffManager;
    }

    public void MakeUniqueTerrainElevationData() throws IOException, FactoryException, TransformException {
        log.debug("MakeUniqueTerrainElevationData() started");

        if (uniqueGeoTiffFilePath == null) {
            return;
        }

        if (myGaiaGeoTiffManager == null) {
            myGaiaGeoTiffManager = this.getGaiaGeoTiffManager();
        }

        uniqueTerrainElevationData = new TerrainElevationData(this);
        GridCoverage2D gridCoverage2D = myGaiaGeoTiffManager.loadGeoTiffGridCoverage2D(uniqueGeoTiffFilePath);
        uniqueTerrainElevationData.setGeotiffFilePath(uniqueGeoTiffFilePath);

        CoordinateReferenceSystem crsTarget = gridCoverage2D.getCoordinateReferenceSystem2D();
        CoordinateReferenceSystem crsWgs84 = CRS.decode("EPSG:4326", true);
        MathTransform targetToWgs = CRS.findMathTransform(crsTarget, crsWgs84);

        GeometryFactory gf = new GeometryFactory();
        GaiaGeoTiffUtils.getGeographicExtension(gridCoverage2D, gf, targetToWgs, uniqueTerrainElevationData.getGeographicExtension());
        uniqueTerrainElevationData.setPixelSizeMeters(GaiaGeoTiffUtils.getPixelSizeMeters(gridCoverage2D));

        gridCoverage2D.dispose(true);
        log.debug("MakeUniqueTerrainElevationData() ended");
    }

    public TileWgs84Raster getTileWgs84Raster(TileIndices tileIndices, TileWgs84Manager tileWgs84Manager) {
        TileWgs84Raster tileWgs84Raster = mapIndicesTileRaster.get(tileIndices.getString());
        if (tileWgs84Raster == null) {
            tileWgs84Raster = new TileWgs84Raster(tileIndices, tileWgs84Manager);
            int tileRasterWidth = tileWgs84Manager.getRasterTileSize();
            int tileRasterHeight = tileWgs84Manager.getRasterTileSize();
            tileWgs84Raster.makeElevations(this, tileRasterWidth, tileRasterHeight);
            mapIndicesTileRaster.put(tileIndices.getString(), tileWgs84Raster);
        }
        return tileWgs84Raster;
    }

    public void makeAllTileWgs84Raster(TileRange tileRange, TileWgs84Manager tileWgs84Manager) {
        List<TileIndices> tileIndicesList = tileRange.getTileIndices(null);
        for (TileIndices tileIndices : tileIndicesList) {
            TileWgs84Raster tileWgs84Raster = mapIndicesTileRaster.get(tileIndices.getString());
            if (tileWgs84Raster == null) {
                tileWgs84Raster = new TileWgs84Raster(tileIndices, tileWgs84Manager);
                int tileRasterWidth = tileWgs84Manager.getRasterTileSize();
                int tileRasterHeight = tileWgs84Manager.getRasterTileSize();
                tileWgs84Raster.makeElevations(this, tileRasterWidth, tileRasterHeight);
                mapIndicesTileRaster.put(tileIndices.getString(), tileWgs84Raster);
            }
        }
    }

    public void deleteTileRaster() {
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
        this.deleteTileRaster();
        this.deleteCoverage();
        if (myGaiaGeoTiffManager != null) {
            myGaiaGeoTiffManager.deleteObjects();
            myGaiaGeoTiffManager = null;
        }

        if (rootTerrainElevationDataQuadTree == null) {
            return;
        }

        rootTerrainElevationDataQuadTree.deleteObjects();
        rootTerrainElevationDataQuadTree = null;

        terrainElevationDataArray.clear();
    }

    public double getElevationBilinearRasterTile(TileIndices tileIndices, TileWgs84Manager tileWgs84Manager, double lonDeg, double latDeg) {
        double resultElevation = 0.0;
        TileWgs84Raster tileWgs84Raster = null;
        tileWgs84Raster = this.getTileWgs84Raster(tileIndices, tileWgs84Manager);
        resultElevation = tileWgs84Raster.getElevationBilinear(lonDeg, latDeg);
        return resultElevation;
    }

    public double getElevation(double lonDeg, double latDeg, List<TerrainElevationData> terrainElevDataArray) {
        double resultElevation = 0.0;

        if (this.geoTiffFilesCount == 1) {
            if (uniqueTerrainElevationData == null) {
                return resultElevation;
            }
            resultElevation = uniqueTerrainElevationData.getElevation(lonDeg, latDeg, intersects);
            return resultElevation;
        }

        if (rootTerrainElevationDataQuadTree == null) {
            return resultElevation;
        }

        terrainElevDataArray.clear();
        rootTerrainElevationDataQuadTree.getTerrainElevationDataArray(lonDeg, latDeg, terrainElevDataArray);

        double noDataValue = globalOptions.getNoDataValue();
        PriorityType priorityType = globalOptions.getPriorityType();

        intersects[0] = false;
        double pixelAreaAux = Double.MAX_VALUE;
        double candidateElevation = 0.0;
        for (TerrainElevationData terrainElevationData : terrainElevDataArray) {
            double elevation = terrainElevationData.getElevation(lonDeg, latDeg, intersects);
            if (!intersects[0]) {
                continue;
            }

            /* check if the priority is resolution */
            if (priorityType.equals(PriorityType.RESOLUTION)) {
                String fileName = new File(terrainElevationData.getGeotiffFilePath()).getName();
                double pixelArea = putAndGetGridAreaMap(terrainElevationData.getGeotiffFilePath());
                //double pixelArea = terrainElevationData.getPixelArea();
                boolean isHigherResolution = pixelAreaAux > pixelArea; // smaller pixelArea is higher resolution
                if (isHigherResolution) {
                    if (noDataValue != 0.0) {
                        candidateElevation = elevation;
                        pixelAreaAux = pixelArea;
                    }
                }
            } else {
                candidateElevation = Math.max(candidateElevation, elevation);
            }
        }

        resultElevation = candidateElevation;
        return resultElevation;
    }

    private void loadAllGeoTiff(String terrainElevationDataFolderPath) throws FactoryException, TransformException {
        // load all geoTiffFiles
        geoTiffFileNames.clear();
        FileUtils.getFileNames(terrainElevationDataFolderPath, ".tif", geoTiffFileNames);

        if (myGaiaGeoTiffManager == null) {
            myGaiaGeoTiffManager = this.getGaiaGeoTiffManager();
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

        Map<String, String> mapNoUsableGeotiffPaths = this.tileWgs84Manager.getMapNoUsableGeotiffPaths();

        for (String memSaveGeoTiffFileName : geoTiffFileNames) {
            geoTiffFileName = memSaveGeoTiffFileName;
            geoTiffFilePath = terrainElevationDataFolderPath + File.separator + geoTiffFileName;

            // check if this geoTiff is usable
            if (mapNoUsableGeotiffPaths.containsKey(geoTiffFilePath)) {
                continue;
            }

            TerrainElevationData terrainElevationData = new TerrainElevationData(this);

            gridCoverage2D = myGaiaGeoTiffManager.loadGeoTiffGridCoverage2D(geoTiffFilePath);
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

    public Double putAndGetGridAreaMap(String path) {
        Double pixelArea = 0.0d;
        File file = new File(path);
        String fileName = file.getName();
        if (gridAreaMap.containsKey(fileName)) {
            return gridAreaMap.get(fileName);
        }

        File standardizationTempPath = new File(globalOptions.getStandardizeTempPath());
        File tempFile = new File(standardizationTempPath, fileName);

        if (tempFile.exists() && !file.equals(tempFile)) {
            try {
                GaiaGeoTiffManager gaiaGeoTiffManager = this.getGaiaGeoTiffManager();
                GridCoverage2D coverage = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(tempFile.getAbsolutePath());
                Vector2d originalArea = GaiaGeoTiffUtils.getPixelSizeMeters(coverage);
                pixelArea = originalArea.x * originalArea.y;
            } catch (FactoryException e) {
                log.error("[getPixelArea : FactoryException] Error in getPixelArea", e);
            }
        }
        gridAreaMap.put(fileName, pixelArea);
        return gridAreaMap.get(fileName);
    }

    public void deleteGeoTiffManager() {
        if (myGaiaGeoTiffManager != null) {
            myGaiaGeoTiffManager.deleteObjects();
            myGaiaGeoTiffManager = null;
        }
    }
}
