package com.gaia3d.command;


import com.gaia3d.process.ProcessOptions;
import com.gaia3d.wgs84Tiles.TileWgs84Manager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;

@Slf4j
@Deprecated
public class MesherMain
{
    public static String version = "1.0.1";
    public static CommandLine command = null;
    public static void main(String[] args)
    {
        Configurator.initConsoleLogger();
        Options options = Configurator.createOptions();
        CommandLineParser parser = new DefaultParser();

        GeotoolsConfigurator geotoolsConfigurator = new GeotoolsConfigurator();
        try {
            geotoolsConfigurator.setEpsg();
        } catch (IOException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        }

        // create tileManager & set params.***
        TileWgs84Manager tileWgs84Manager = new TileWgs84Manager();

        try
        {
            command = parser.parse(options, args);
            if (command.hasOption(ProcessOptions.INPUT.getArgName())) {
                tileWgs84Manager.originalGeoTiffFolderPath = command.getOptionValue(ProcessOptions.INPUT.getArgName());
            }

            if (command.hasOption(ProcessOptions.OUTPUT.getArgName())) {
                tileWgs84Manager.outputDirectory = command.getOptionValue(ProcessOptions.OUTPUT.getArgName());
                tileWgs84Manager.tileTempDirectory = tileWgs84Manager.outputDirectory + File.separator + "tileTempFolder";
                tileWgs84Manager.tempResizedGeoTiffFolderPath = tileWgs84Manager.outputDirectory + File.separator + "resizedGeoTiffFolder";
            }

            if (command.hasOption(ProcessOptions.MINIMUM_TILE_DEPTH.getArgName())) {
                tileWgs84Manager.minTileDepth = Integer.parseInt(command.getOptionValue(ProcessOptions.MINIMUM_TILE_DEPTH.getArgName()));
            }

            if (command.hasOption(ProcessOptions.MAXIMUM_TILE_DEPTH.getArgName())) {
                tileWgs84Manager.maxTileDepth = Integer.parseInt(command.getOptionValue(ProcessOptions.MAXIMUM_TILE_DEPTH.getArgName()));
            }

            // check calculate normals.***
            if (command.hasOption(ProcessOptions.CALCULATE_NORMALS.getArgName())) {
                tileWgs84Manager.setCalculateNormals(true);
            }

            //if (command.hasOption(ProcessOptions.MESH_REFINEMENT_STRENGTH.getArgName())) {
                //tileWgs84Manager.refinementStrength = Integer.parseInt(command.getOptionValue(ProcessOptions.MESH_REFINEMENT_STRENGTH.getArgName()));
            //}
        }
        catch (Exception e) {
            log.error("Error parsing command line arguments: " + e.getMessage());
        }

        try {
            tileWgs84Manager.processResizeGeotiffs(tileWgs84Manager.originalGeoTiffFolderPath, null);
        } catch (IOException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (FactoryException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (TransformException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        }

        tileWgs84Manager.terrainElevationDataManager = new com.gaia3d.wgs84Tiles.TerrainElevationDataManager();
        // set the terrainElevation data folder path.***
        tileWgs84Manager.terrainElevationDataManager.setTerrainElevationDataFolderPath(tileWgs84Manager.tempResizedGeoTiffFolderPath + File.separator + "0");
        try {
            if(tileWgs84Manager.getGeoTiffFilesCount() == 1)
            {
                tileWgs84Manager.terrainElevationDataManager.setGeoTiffFilesCount(1);
                tileWgs84Manager.terrainElevationDataManager.setUniqueGeoTiffFilePath(tileWgs84Manager.getUniqueGeoTiffFilePath());
                tileWgs84Manager.terrainElevationDataManager.MakeUniqueTerrainElevationData();
            }
            else
            {
                System.out.println("making geoTiff quadtree.");
                tileWgs84Manager.terrainElevationDataManager.makeTerrainQuadTree();
            }

        } catch (FactoryException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (TransformException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        }

        // start quantized mesh tiling.***
        try {
            tileWgs84Manager.makeTileMeshes(); // original.***
        } catch (IOException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (TransformException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (FactoryException e) {
            log.error("Error : {}", e.getMessage());
            throw new RuntimeException(e);
        }


        int hola2 = 0;
    }
}