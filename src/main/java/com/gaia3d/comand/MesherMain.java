package com.gaia3d.comand;


import com.gaia3d.wgs84Tiles.TerrainElevationData;
import com.gaia3d.wgs84Tiles.TileWgs84Manager;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

public class MesherMain {
    public static void main(String[] args) throws FactoryException, TransformException {

        // create tileManager & set params.***
        TileWgs84Manager tileWgs84Manager = new TileWgs84Manager();

        String tileTempDirectory = "D:\\QuantizedMesh_JavaProjects\\tileTempFolder";
        tileWgs84Manager.tileTempDirectory = tileTempDirectory;

        String outputDirectory = "D:\\QuantizedMesh_JavaProjects\\outputFolder";
        tileWgs84Manager.outputDirectory = outputDirectory;

        // terrain elevation data.***
        String geoTiffFilePath = "D:\\QuantizedMesh_JavaProjects\\ws2_merged_dem.tif";
        tileWgs84Manager.terrainElevationData = new TerrainElevationData();
        tileWgs84Manager.terrainElevationData.loadGeoTiffFile(geoTiffFilePath);


        // start quantized mesh tiling.***
        tileWgs84Manager.makeTileMeshes();


        int hola2 = 0;
    }
}