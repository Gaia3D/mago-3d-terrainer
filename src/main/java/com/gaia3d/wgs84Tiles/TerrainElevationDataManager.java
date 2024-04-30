package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GaiaTriangle;
import com.gaia3d.basic.structure.GeographicExtension;
import lombok.Getter;
import lombok.Setter;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.referencing.CRS;
import org.joml.Vector2d;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TerrainElevationDataManager
{
    // Inside the folder, there are multiple geoTiff files.***
    @Setter
    @Getter
    String terrainElevationDataFolderPath;
    @Setter
    @Getter
    int geoTiffFilesCount = 0;
    @Setter
    @Getter
    String uniqueGeoTiffFilePath = null; // use this if there is only one geoTiff file.***
    TerrainElevationData uniqueTerrainElevationData = null; // use this if there is only one geoTiff file.***

    // if there are multiple geoTiff files, use this.***
    int quadtreesMaxDepth = 10;
    TerrainElevationDataQuadTree rootTerrainElevationDataQuadTree;

    GaiaGeoTiffManager gaiaGeoTiffManager = null;

    public ArrayList<TerrainElevationData> memSave_terrainElevDatasArray = new ArrayList<TerrainElevationData>();

    boolean memSave_intersects[] = {false};

    ArrayList<String> memSave_geoTiffFileNames = new ArrayList<String>();
    public ArrayList<GaiaTriangle> memSave_trianglesArray = new ArrayList<GaiaTriangle>();

    public Map<String, TileWgs84Raster> mapIndicesTileRaster = new HashMap<String, TileWgs84Raster>();

    public TerrainElevationDataManager()
    {
        rootTerrainElevationDataQuadTree = null;
    }

    public void makeTerrainQuadTree() throws FactoryException, TransformException, IOException {
        // load all geoTiffFiles & make a quadTree.***
        loadAllGeoTiff(terrainElevationDataFolderPath);
        rootTerrainElevationDataQuadTree.makeQuadTree(quadtreesMaxDepth);
        int hola = 0;
    }

    public void MakeUniqueTerrainElevationData() throws FactoryException, TransformException, IOException {
        if(uniqueGeoTiffFilePath == null)
        {
            return;
        }

        if(gaiaGeoTiffManager == null)
        {
            gaiaGeoTiffManager = new GaiaGeoTiffManager();
        }

        uniqueTerrainElevationData = new TerrainElevationData(this);
        GridCoverage2D gridCoverage2D = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(uniqueGeoTiffFilePath);
        uniqueTerrainElevationData.geotiffFilePath = uniqueGeoTiffFilePath;

        CoordinateReferenceSystem crsTarget = gridCoverage2D.getCoordinateReferenceSystem2D();
        CoordinateReferenceSystem crsWgs84 = CRS.decode("EPSG:4326", true);
        MathTransform targetToWgs = CRS.findMathTransform(crsTarget, crsWgs84);

        GeometryFactory gf = new GeometryFactory();
        GaiaGeoTiffUtils.getGeographicExtension(gridCoverage2D, gf, targetToWgs, uniqueTerrainElevationData.geographicExtension);
        uniqueTerrainElevationData.pixelSizeMeters = GaiaGeoTiffUtils.getPixelSizeMeters(gridCoverage2D);

        gridCoverage2D.dispose(true);
    }

    public TileWgs84Raster getTileWgs84Raster(TileIndices tileIndices, TileWgs84Manager tileWgs84Manager) throws TransformException, IOException {
        TileWgs84Raster tileWgs84Raster = mapIndicesTileRaster.get(tileIndices.getString());
        if(tileWgs84Raster == null)
        {
            tileWgs84Raster = new TileWgs84Raster(tileIndices, tileWgs84Manager);
            int tileRasterWidth = tileWgs84Manager.tileRasterSize;
            int tileRasterHeight = tileWgs84Manager.tileRasterSize;
            tileWgs84Raster.makeElevations(this, tileRasterWidth, tileRasterHeight);
            mapIndicesTileRaster.put(tileIndices.getString(), tileWgs84Raster);
        }

        return tileWgs84Raster;
    }

    public void deleteTileRasters()
    {
        for(TileWgs84Raster tileWgs84Raster : mapIndicesTileRaster.values())
        {
            tileWgs84Raster.deleteObjects();
        }

        mapIndicesTileRaster.clear();

    }


    public GeographicExtension getRootGeographicExtension() {
        if(this.geoTiffFilesCount == 1)
        {
            if(uniqueTerrainElevationData == null)
            {
                return null;
            }

            return uniqueTerrainElevationData.geographicExtension;
        }

        if(rootTerrainElevationDataQuadTree == null)
        {
            return null;
        }

        return rootTerrainElevationDataQuadTree.geographicExtension;
    }

    public void deleteCoverage()
    {
        if(rootTerrainElevationDataQuadTree == null)
        {
            return;
        }

        rootTerrainElevationDataQuadTree.deleteCoverage();
    }

    public void deleteCoverageIfNotIntersects(GeographicExtension geographicExtension)
    {
        if(rootTerrainElevationDataQuadTree == null)
        {
            return;
        }

        rootTerrainElevationDataQuadTree.deleteCoverageIfNotIntersects(geographicExtension);
    }

    public void deleteObjects()
    {
        this.deleteTileRasters();
        if(rootTerrainElevationDataQuadTree == null)
        {
            return;
        }

        rootTerrainElevationDataQuadTree.deleteObjects();
        rootTerrainElevationDataQuadTree = null;
    }

    public double getElevation(double lonDeg, double latDeg, ArrayList<TerrainElevationData> memSave_terrainElevDatasArray) throws TransformException, IOException {
        double resultElevation = 0.0;

        if(this.geoTiffFilesCount == 1)
        {
            if(uniqueTerrainElevationData == null)
            {
                return resultElevation;
            }

            resultElevation = uniqueTerrainElevationData.getElevation(lonDeg, latDeg, memSave_intersects);
            return resultElevation;
        }

        if(rootTerrainElevationDataQuadTree == null)
        {
            return resultElevation;
        }

        memSave_terrainElevDatasArray.clear();
        rootTerrainElevationDataQuadTree.getTerrainElevationDatasArray(lonDeg, latDeg, memSave_terrainElevDatasArray);

        memSave_intersects[0] = false;
        int terrainElevDatasCount = memSave_terrainElevDatasArray.size();
        for(int i=0; i<terrainElevDatasCount; i++)
        {
            TerrainElevationData terrainElevationData = memSave_terrainElevDatasArray.get(i);

            double elevation = terrainElevationData.getElevation(lonDeg, latDeg, memSave_intersects);
            if(memSave_intersects[0] == false)
            {
                continue;
            }

            if(elevation < 0.0)
            {
                elevation = 0.0;
            }

            resultElevation = elevation;
            break;
        }

        return resultElevation;
    }

    private void loadAllGeoTiff(String terrainElevationDataFolderPath) throws IOException, FactoryException, TransformException {
        // load all geoTiffFiles.***
        memSave_geoTiffFileNames.clear();
        com.gaia3d.reader.FileUtils.getFileNames(terrainElevationDataFolderPath, ".tif", memSave_geoTiffFileNames);

        if(gaiaGeoTiffManager == null)
        {
            gaiaGeoTiffManager = new GaiaGeoTiffManager();
        }
        GeometryFactory gf = new GeometryFactory();

        if(rootTerrainElevationDataQuadTree == null)
        {
            rootTerrainElevationDataQuadTree = new TerrainElevationDataQuadTree(null);
        }

        // now load all geotiff and make geotiff geoExtension data.***
        int geoTiffCount = memSave_geoTiffFileNames.size();
        GridCoverage2D gridCoverage2D = null;
        String geoTiffFileName = null;
        String geoTiffFilePath = null;

        CoordinateReferenceSystem crsTarget = null;
        CoordinateReferenceSystem crsWgs84 = null;
        MathTransform targetToWgs = null;

        for(int i=0; i<geoTiffCount; i++)
        {
            geoTiffFileName = memSave_geoTiffFileNames.get(i);
            geoTiffFilePath = terrainElevationDataFolderPath + "\\" + geoTiffFileName;
            TerrainElevationData terrainElevationData = new TerrainElevationData(this);

            gridCoverage2D = gaiaGeoTiffManager.loadGeoTiffGridCoverage2D(geoTiffFilePath);
            terrainElevationData.geotiffFilePath = geoTiffFilePath;

            crsTarget = gridCoverage2D.getCoordinateReferenceSystem2D();
            crsWgs84 = CRS.decode("EPSG:4326", true);
            targetToWgs = CRS.findMathTransform(crsTarget, crsWgs84);

            GaiaGeoTiffUtils.getGeographicExtension(gridCoverage2D, gf, targetToWgs, terrainElevationData.geographicExtension);
            terrainElevationData.pixelSizeMeters = GaiaGeoTiffUtils.getPixelSizeMeters(gridCoverage2D);

            rootTerrainElevationDataQuadTree.addTerrainElevationData(terrainElevationData);
            gridCoverage2D.dispose(true);
            int hola = 0;
        }

        // now check if exist folders inside the terrainElevationDataFolderPath.***
        ArrayList<String> folderNames = new ArrayList<String>();
        com.gaia3d.reader.FileUtils.getFolderNames(terrainElevationDataFolderPath, folderNames);
        int folderCount = folderNames.size();
        for(int i=0; i<folderCount; i++)
        {
            String folderName = folderNames.get(i);
            String folderPath = terrainElevationDataFolderPath + "\\" + folderName;
            loadAllGeoTiff(folderPath);
        }

        int hola = 0;
    }



}
