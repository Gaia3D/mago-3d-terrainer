package com.gaia3d.comand;


import com.gaia3d.wgs84Tiles.GaiaGeoTiffManager;
import com.gaia3d.wgs84Tiles.TerrainElevationData;
import com.gaia3d.wgs84Tiles.TileWgs84Manager;

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

        // terrain elevation data.***
        String geoTiffFilePath = "D:\\QuantizedMesh_JavaProjects\\ws2_merged_dem.tif";
        tileWgs84Manager.terrainElevationData = new TerrainElevationData();
        //tileWgs84Manager.terrainElevationData.loadGeoTiffFile(geoTiffFilePath);

        // terrain elevation data in *.img format.***
        //String imageFilePath = "D:\\QuantizedMesh_JavaProjects\\35902008.img";
        //tileWgs84Manager.terrainElevationData.loadImageFile(imageFilePath);

        tileWgs84Manager.minTileDepth = 0;
        tileWgs84Manager.maxTileDepth = 17;

        // do resizing test.***
        GaiaGeoTiffManager gaiaGeoTiffManager = new GaiaGeoTiffManager();
        String geoTiffFilePathTest = "D:\\QuantizedMesh_JavaProjects\\output_geoTiff\\5m\\33612(표선)\\33612010.tif";
        String outputFilePathTest = "D:\\QuantizedMesh_JavaProjects\\resizedTiffTest.tif";

        gaiaGeoTiffManager.resiseGeoTiff_test(geoTiffFilePathTest, outputFilePathTest, 1000, 1000);

        // start quantized mesh tiling.***
        tileWgs84Manager.makeTileMeshes(); // original.***


        int hola2 = 0;
    }
}