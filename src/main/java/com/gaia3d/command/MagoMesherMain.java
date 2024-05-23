package com.gaia3d.command;


import com.gaia3d.process.ProcessOptions;
import com.gaia3d.wgs84Tiles.TileWgs84Manager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.Level;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;

@Slf4j
public class MagoMesherMain {
    public static String version = "1.0.2";
    public static int DEFAULT_MINIMUM_TILE_DEPTH = 0;
    public static int DEFAULT_MAXIMUM_TILE_DEPTH = 16;

    public static void main(String[] args) {
        try {
            Options options = Configurator.createOptions();
            CommandLineParser parser = new DefaultParser();
            CommandLine command = parser.parse(Configurator.createOptions(), args);
            boolean isHelp = command.hasOption(ProcessOptions.HELP.getArgName());
            boolean hasLogPath = command.hasOption(ProcessOptions.LOG.getArgName());
            boolean isVersion = command.hasOption(ProcessOptions.VERSION.getArgName());

            GeotoolsConfigurator geotoolsConfigurator = new GeotoolsConfigurator();
            TileWgs84Manager tileWgs84Manager = new TileWgs84Manager();
            geotoolsConfigurator.setEpsg();
            tileWgs84Manager.setOriginalGeoTiffFolderPath(command.getOptionValue(ProcessOptions.INPUT.getArgName()));

            if (command.hasOption(ProcessOptions.DEBUG.getArgName())) {
                Configurator.initConsoleLogger("[%p][%d{HH:mm:ss}][%C{2}(%M:%L)]::%message%n");
                if (hasLogPath) {
                    Configurator.initFileLogger("[%p][%d{HH:mm:ss}][%C{2}(%M:%L)]::%message%n", command.getOptionValue(ProcessOptions.LOG.getArgName()));
                }
                Configurator.setLevel(Level.DEBUG);
            } else {
                Configurator.initConsoleLogger();
                if (hasLogPath) {
                    Configurator.initFileLogger(null, command.getOptionValue(ProcessOptions.LOG.getArgName()));
                }
                Configurator.setLevel(Level.INFO);
            }


            printStart();

            if (isHelp) {
                new HelpFormatter().printHelp("Mago 3D Quantized Mesher", options);
                return;
            }

            if (isVersion) {
                log.info("Version: " + version);
            }

            if (command.hasOption(ProcessOptions.INPUT.getArgName())) {
                tileWgs84Manager.setOriginalGeoTiffFolderPath(command.getOptionValue(ProcessOptions.INPUT.getArgName()));
            } else {
                throw new RuntimeException("Input folder path is required.");
            }

            if (command.hasOption(ProcessOptions.OUTPUT.getArgName())) {
                tileWgs84Manager.setOutputDirectory(command.getOptionValue(ProcessOptions.OUTPUT.getArgName()));
                tileWgs84Manager.setTileTempDirectory(tileWgs84Manager.getOutputDirectory() + File.separator + "TileTemp");
                tileWgs84Manager.setTempResizedGeoTiffFolderPath(tileWgs84Manager.getOutputDirectory() + File.separator + "ResizedGeoTiff");
            } else {
                throw new RuntimeException("Output folder path is required.");
            }

            if (command.hasOption(ProcessOptions.MINIMUM_TILE_DEPTH.getArgName())) {
                tileWgs84Manager.setMinTileDepth(Integer.parseInt(command.getOptionValue(ProcessOptions.MINIMUM_TILE_DEPTH.getArgName())));
            } else {
                log.info("Minimum tile depth is not set. Default value is " + DEFAULT_MINIMUM_TILE_DEPTH);
                tileWgs84Manager.setMinTileDepth(DEFAULT_MINIMUM_TILE_DEPTH);
            }

            if (command.hasOption(ProcessOptions.MAXIMUM_TILE_DEPTH.getArgName())) {
                tileWgs84Manager.setMaxTileDepth(Integer.parseInt(command.getOptionValue(ProcessOptions.MAXIMUM_TILE_DEPTH.getArgName())));
            } else {
                log.info("Maximum tile depth is not set. Default value is " + DEFAULT_MAXIMUM_TILE_DEPTH);
                tileWgs84Manager.setMaxTileDepth(DEFAULT_MAXIMUM_TILE_DEPTH);
            }

            GlobalOptions.init(command);

            if (command.hasOption(ProcessOptions.CALCULATE_NORMALS.getArgName())) {
                tileWgs84Manager.setCalculateNormals(true);
            }

            log.info("[Resize GeoTiff] Start resizing GeoTiff files.");
            tileWgs84Manager.processResizeGeotiffs(tileWgs84Manager.getOriginalGeoTiffFolderPath(), null);
            log.info("[Resize GeoTiff] Finished resizing GeoTiff files.");

            log.info("[Make Terrain Elevation Data] Start making terrain elevation data.");
            tileWgs84Manager.setTerrainElevationDataManager(new com.gaia3d.wgs84Tiles.TerrainElevationDataManager());
            tileWgs84Manager.getTerrainElevationDataManager().setTerrainElevationDataFolderPath(tileWgs84Manager.getTempResizedGeoTiffFolderPath() + File.separator + "0");
            if (tileWgs84Manager.getGeoTiffFilesCount() == 1) {
                tileWgs84Manager.getTerrainElevationDataManager().setGeoTiffFilesCount(1);
                tileWgs84Manager.getTerrainElevationDataManager().setUniqueGeoTiffFilePath(tileWgs84Manager.getUniqueGeoTiffFilePath());
                tileWgs84Manager.getTerrainElevationDataManager().MakeUniqueTerrainElevationData();
            } else {
                tileWgs84Manager.getTerrainElevationDataManager().makeTerrainQuadTree();
            }
            log.info("[Make Terrain Elevation Data] Finished making terrain elevation data.");

            log.info("[Make Tile Meshes] Start making tile meshes.");
            tileWgs84Manager.makeTileMeshes();
            log.info("[Make Tile Meshes] Finished making tile meshes.");
        } catch (FactoryException e) {
            log.error("Failed to set EPSG.", e);
            throw new RuntimeException(e);
        } catch (TransformException e) {
            log.error("Failed to transform coordinates.", e);
            throw new RuntimeException(e);
        } catch (ParseException e) {
            log.error("Failed to parse command line options, Please check the arguments.", e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("Failed to run process, Please check the arguments.", e);
            throw new RuntimeException(e);
        } finally {
            log.info("----------------------------------------");
            log.info("Finished.");
        }
    }

    private static void printStart() {
        String programInfo = "Mago Quantized Mesher by Gaia3D, Inc.";
        log.info("\n" +
                "┳┳┓┏┓┏┓┏┓  ┏┓┳┓  ┳┳┓┏┓┏┓┓┏┏┓┳┓\n" +
                "┃┃┃┣┫┃┓┃┃   ┫┃┃  ┃┃┃┣ ┗┓┣┫┣ ┣┫\n" +
                "┛ ┗┛┗┗┛┗┛  ┗┛┻┛  ┛ ┗┗┛┗┛┛┗┗┛┛┗\n" +
                programInfo + "\n" +
                "----------------------------------------");
    }
}