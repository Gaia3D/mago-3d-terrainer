package com.gaia3d.comand;


import com.gaia3d.process.ProcessOptions;
import com.gaia3d.wgs84Tiles.GaiaGeoTiffManager;
import com.gaia3d.wgs84Tiles.TerrainElevationData;
import com.gaia3d.wgs84Tiles.TileWgs84Manager;

import com.gaia3d.wgs84Tiles.TileWgs84Utils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;

public class MesherMain
{
    public static String version = "1.0.1";
    public static CommandLine command = null;
    public static void main(String[] args) throws FactoryException, TransformException, IOException
    {
        Configurator.initConsoleLogger();
        Options options = Configurator.createOptions();
        CommandLineParser parser = new DefaultParser();

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
                tileWgs84Manager.tileTempDirectory = tileWgs84Manager.outputDirectory + "\\tileTempFolder";
                tileWgs84Manager.tempResizedGeoTiffFolderPath = tileWgs84Manager.outputDirectory + "\\resizedGeoTiffFolder";
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

        //String tileTempDirectory = "D:\\QuantizedMesh_JavaProjects\\tileTempFolder";
        //tileWgs84Manager.tileTempDirectory = tileTempDirectory;

        //String outputDirectory = "D:\\QuantizedMesh_JavaProjects\\output";
        //tileWgs84Manager.outputDirectory = outputDirectory;


        //tileWgs84Manager.minTileDepth = 0;
        //tileWgs84Manager.maxTileDepth = 17;

        // Set geoTiff resizing folder paths.***
        //tileWgs84Manager.tempResizedGeoTiffFolderPath = "D:\\QuantizedMesh_JavaProjects\\resizedGeoTiffFolder";
        //tileWgs84Manager.originalGeoTiffFolderPath = "D:\\QuantizedMesh_JavaProjects\\output_geoTiff\\5m";
        tileWgs84Manager.resizeGeotiffSet(tileWgs84Manager.originalGeoTiffFolderPath, null);

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
        tileWgs84Manager.terrainElevationDataManager.makeTerrainQuadTree();

        // start quantized mesh tiling.***
        tileWgs84Manager.makeTileMeshes(); // original.***


        int hola2 = 0;
    }
}