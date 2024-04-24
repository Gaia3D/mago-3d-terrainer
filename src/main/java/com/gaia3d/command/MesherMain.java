package com.gaia3d.command;


import com.gaia3d.process.ProcessOptions;
import com.gaia3d.wgs84Tiles.TerrainElevationData;
import com.gaia3d.wgs84Tiles.TileWgs84Manager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MesherMain
{
    public static String version = "1.0.1";
    public static CommandLine command = null;

    public static void mainTest(String[] args) throws FactoryException, TransformException, IOException
    {
        Configurator.initConsoleLogger();
        Options options = Configurator.createOptions();
        CommandLineParser parser = new DefaultParser();

        GeotoolsConfigurator geotoolsConfigurator = new GeotoolsConfigurator();
        try {
            geotoolsConfigurator.setEpsg();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // create tileManager & set params.***
        TileWgs84Manager tileWgs84Manager = new TileWgs84Manager();

        try
        {
            command = parser.parse(options, args);
            if (command.hasOption(ProcessOptions.INPUT_FOLDER_PATH.getArgName())) {
                tileWgs84Manager.originalGeoTiffFolderPath = command.getOptionValue(ProcessOptions.INPUT_FOLDER_PATH.getArgName());
            }

            if (command.hasOption(ProcessOptions.OUTPUT_FOLDER_PATH.getArgName())) {
                tileWgs84Manager.outputDirectory = command.getOptionValue(ProcessOptions.OUTPUT_FOLDER_PATH.getArgName());
                tileWgs84Manager.tileTempDirectory = tileWgs84Manager.outputDirectory + File.separator + "tileTempFolder";
                tileWgs84Manager.tempResizedGeoTiffFolderPath = tileWgs84Manager.outputDirectory + File.separator + "resizedGeoTiffFolder";
            }

            if (command.hasOption(ProcessOptions.MINIMUM_TILE_DEPTH.getArgName())) {
                tileWgs84Manager.minTileDepth = Integer.parseInt(command.getOptionValue(ProcessOptions.MINIMUM_TILE_DEPTH.getArgName()));
            }

            if (command.hasOption(ProcessOptions.MAXIMUM_TILE_DEPTH.getArgName())) {
                tileWgs84Manager.maxTileDepth = Integer.parseInt(command.getOptionValue(ProcessOptions.MAXIMUM_TILE_DEPTH.getArgName()));
            }

            //if (command.hasOption(ProcessOptions.MESH_REFINEMENT_STRENGTH.getArgName())) {
            //tileWgs84Manager.refinementStrength = Integer.parseInt(command.getOptionValue(ProcessOptions.MESH_REFINEMENT_STRENGTH.getArgName()));
            //}
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try {
            tileWgs84Manager.resizeGeotiffSet(tileWgs84Manager.originalGeoTiffFolderPath, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        }

        tileWgs84Manager.terrainElevationDataManager = new com.gaia3d.wgs84Tiles.TerrainElevationDataManager();
        // set the terrainElevation data folder path.***
        tileWgs84Manager.terrainElevationDataManager.setTerrainElevationDataFolderPath(tileWgs84Manager.tempResizedGeoTiffFolderPath + "\\0");
        try {
            tileWgs84Manager.terrainElevationDataManager.makeTerrainQuadTree();
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        double lonDeg_1 = 126.968080;
        double latDeg_1 = 37.457330;
        double elev_1 = 0.0;

        double lonDeg_2 = 126.967607;
        double latDeg_2 = 37.455300;
        double elev_2 = 0.0;

        tileWgs84Manager.terrainElevationDataManager.setTerrainElevationDataFolderPath(tileWgs84Manager.map_depth_geoTiffFolderPath.get(17));

        ArrayList<TerrainElevationData> memSave_terrainElevDatasArray = new ArrayList<TerrainElevationData>();
        elev_1 = tileWgs84Manager.terrainElevationDataManager.getElevation(lonDeg_1, latDeg_1, memSave_terrainElevDatasArray);
        elev_2 = tileWgs84Manager.terrainElevationDataManager.getElevation(lonDeg_2, latDeg_2, memSave_terrainElevDatasArray);

        int hola = 0;
    }
    public static void main(String[] args)
    {
        Configurator.initConsoleLogger();
        Options options = Configurator.createOptions();
        CommandLineParser parser = new DefaultParser();

        GeotoolsConfigurator geotoolsConfigurator = new GeotoolsConfigurator();
        try {
            geotoolsConfigurator.setEpsg();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // create tileManager & set params.***
        TileWgs84Manager tileWgs84Manager = new TileWgs84Manager();

        try
        {
            command = parser.parse(options, args);
            if (command.hasOption(ProcessOptions.INPUT_FOLDER_PATH.getArgName())) {
                tileWgs84Manager.originalGeoTiffFolderPath = command.getOptionValue(ProcessOptions.INPUT_FOLDER_PATH.getArgName());
            }

            if (command.hasOption(ProcessOptions.OUTPUT_FOLDER_PATH.getArgName())) {
                tileWgs84Manager.outputDirectory = command.getOptionValue(ProcessOptions.OUTPUT_FOLDER_PATH.getArgName());
                tileWgs84Manager.tileTempDirectory = tileWgs84Manager.outputDirectory + File.separator + "tileTempFolder";
                tileWgs84Manager.tempResizedGeoTiffFolderPath = tileWgs84Manager.outputDirectory + File.separator + "resizedGeoTiffFolder";
            }

            if (command.hasOption(ProcessOptions.MINIMUM_TILE_DEPTH.getArgName())) {
                tileWgs84Manager.minTileDepth = Integer.parseInt(command.getOptionValue(ProcessOptions.MINIMUM_TILE_DEPTH.getArgName()));
            }

            if (command.hasOption(ProcessOptions.MAXIMUM_TILE_DEPTH.getArgName())) {
                tileWgs84Manager.maxTileDepth = Integer.parseInt(command.getOptionValue(ProcessOptions.MAXIMUM_TILE_DEPTH.getArgName()));
            }

            //if (command.hasOption(ProcessOptions.MESH_REFINEMENT_STRENGTH.getArgName())) {
                //tileWgs84Manager.refinementStrength = Integer.parseInt(command.getOptionValue(ProcessOptions.MESH_REFINEMENT_STRENGTH.getArgName()));
            //}
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try {
            tileWgs84Manager.resizeGeotiffSet(tileWgs84Manager.originalGeoTiffFolderPath, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        }

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
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(16, tileWgs84Manager.originalGeoTiffFolderPath);
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(17, tileWgs84Manager.originalGeoTiffFolderPath);
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(18, tileWgs84Manager.originalGeoTiffFolderPath);
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(19, tileWgs84Manager.originalGeoTiffFolderPath);
        tileWgs84Manager.map_depth_geoTiffFolderPath.put(20, tileWgs84Manager.originalGeoTiffFolderPath);
        */

        tileWgs84Manager.terrainElevationDataManager = new com.gaia3d.wgs84Tiles.TerrainElevationDataManager();
        // set the terrainElevation data folder path.***
        tileWgs84Manager.terrainElevationDataManager.setTerrainElevationDataFolderPath(tileWgs84Manager.tempResizedGeoTiffFolderPath + "\\0");
        try {
            tileWgs84Manager.terrainElevationDataManager.makeTerrainQuadTree();
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // start quantized mesh tiling.***
        try {
            tileWgs84Manager.makeTileMeshes(); // original.***
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TransformException e) {
            throw new RuntimeException(e);
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }


        int hola2 = 0;
    }
}