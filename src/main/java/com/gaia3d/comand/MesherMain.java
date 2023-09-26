package com.gaia3d.comand;


import com.gaia3d.wgs84Tiles.GaiaGeoTiffManager;
import com.gaia3d.wgs84Tiles.TerrainElevationData;
import com.gaia3d.wgs84Tiles.TileWgs84Manager;

import com.gaia3d.wgs84Tiles.TileWgs84Utils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;

public class MesherMain {
    public static void main(String[] args) throws FactoryException, TransformException, IOException {

        // create tileManager & set params.***
        TileWgs84Manager tileWgs84Manager = new TileWgs84Manager();

        String tileTempDirectory = "D:\\QuantizedMesh_JavaProjects\\tileTempFolder";
        tileWgs84Manager.tileTempDirectory = tileTempDirectory;

        String outputDirectory = "D:\\QuantizedMesh_JavaProjects\\output";
        tileWgs84Manager.outputDirectory = outputDirectory;

        String terrainElevationDataFolderPath = "D:\\QuantizedMesh_JavaProjects\\output_geoTiff\\5m";
        String terrainElevationDataFolderPath2 = "D:\\QuantizedMesh_JavaProjects\\output_geoTiff\\10m";
        String terrainElevationDataFolderPath3 = "D:\\QuantizedMesh_JavaProjects\\ws_geoTiff";

        tileWgs84Manager.minTileDepth = 0;
        tileWgs84Manager.maxTileDepth = 13;

        // Set geoTiff resizing folder paths.***
        tileWgs84Manager.tempResizedGeoTiffFolderPath = "D:\\QuantizedMesh_JavaProjects\\resizedGeoTiffFolder";
        tileWgs84Manager.originalGeoTiffFolderPath = "D:\\QuantizedMesh_JavaProjects\\output_geoTiff\\5m";
        tileWgs84Manager.resizeGeotiffSet(terrainElevationDataFolderPath, null);

        // Set geoTiff folder paths directly.***
        /*
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(0, "D:\\QuantizedMesh_JavaProjects\\resizedGeoTiffFolder\\0");
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(1, "D:\\QuantizedMesh_JavaProjects\\resizedGeoTiffFolder\\1");
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(2, "D:\\QuantizedMesh_JavaProjects\\resizedGeoTiffFolder\\2");
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(3, "D:\\QuantizedMesh_JavaProjects\\resizedGeoTiffFolder\\3");
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(4, "D:\\QuantizedMesh_JavaProjects\\resizedGeoTiffFolder\\4");
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(5, "D:\\QuantizedMesh_JavaProjects\\resizedGeoTiffFolder\\5");
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(6, "D:\\QuantizedMesh_JavaProjects\\resizedGeoTiffFolder\\6");
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(7, "D:\\QuantizedMesh_JavaProjects\\resizedGeoTiffFolder\\7");
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(8, "D:\\QuantizedMesh_JavaProjects\\resizedGeoTiffFolder\\8");
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(9, "D:\\QuantizedMesh_JavaProjects\\resizedGeoTiffFolder\\9");
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(10, "D:\\QuantizedMesh_JavaProjects\\resizedGeoTiffFolder\\10");
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(11, "D:\\QuantizedMesh_JavaProjects\\resizedGeoTiffFolder\\11");
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(12, "D:\\QuantizedMesh_JavaProjects\\resizedGeoTiffFolder\\12");
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(13, "D:\\QuantizedMesh_JavaProjects\\resizedGeoTiffFolder\\13");
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(14, tileWgs84Manager.originalGeoTiffFolderPath);
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(15, tileWgs84Manager.originalGeoTiffFolderPath);

         */



        tileWgs84Manager.terrainElevationDataManager = new com.gaia3d.wgs84Tiles.TerrainElevationDataManager();
        // set the terrainElevation data folder path.***
        tileWgs84Manager.terrainElevationDataManager.setTerrainElevationDataFolderPath("D:\\QuantizedMesh_JavaProjects\\resizedGeoTiffFolder\\0");
        tileWgs84Manager.terrainElevationDataManager.makeTerrainQuadTree();



        // do resizing test.**************************************************************************************************************
        GaiaGeoTiffManager gaiaGeoTiffManager = new GaiaGeoTiffManager();
        String geoTiffFilePathTest = "D:\\QuantizedMesh_JavaProjects\\output_geoTiff\\5m\\33612(표선)\\33612010.tif";
        String outputFilePathTest = "D:\\QuantizedMesh_JavaProjects\\resizedTiffTest.tif";
        double desiredPixelSizeXinMeters = 5000.0; // in meters.***
        double desiredPixelSizeYinMeters = 5000.0; // in meters.***
        //gaiaGeoTiffManager.resizeGeoTiff(geoTiffFilePathTest, outputFilePathTest, desiredPixelSizeXinMeters, desiredPixelSizeYinMeters);
        // End do resizing test.**************************************************************************************************************

        // start quantized mesh tiling.***
        tileWgs84Manager.makeTileMeshes(); // original.***


        int hola2 = 0;
    }
}