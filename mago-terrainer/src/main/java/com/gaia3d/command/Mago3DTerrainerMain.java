package com.gaia3d.command;


import com.gaia3d.terrain.tile.TerrainElevationDataManager;
import com.gaia3d.terrain.tile.TerrainLayer;
import com.gaia3d.terrain.tile.TileWgs84Manager;
import com.gaia3d.util.DecimalUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;

@Slf4j
public class Mago3DTerrainerMain {

    public static void main(String[] args) {
        try {
            GlobalOptions globalOptions = GlobalOptions.getInstance();
            CommandLineConfiguration commandLine = globalOptions.getCommandLineConfiguration();
            Options options = commandLine.createOptions();
            CommandLine command = commandLine.createCommandLine(options, args);
            boolean isHelp = command.hasOption(CommandOptions.HELP.getLongName());
            boolean isQuiet = command.hasOption(CommandOptions.QUIET.getLongName());
            boolean hasLogPath = command.hasOption(CommandOptions.LOG.getLongName());
            boolean isDebug = command.hasOption(CommandOptions.DEBUG.getLongName());

            if (isQuiet) {
                LoggingConfiguration.setLevel(Level.OFF);
            } else if (isDebug) {
                LoggingConfiguration.initConsoleLogger("[%p][%d{HH:mm:ss}][%C{2}(%M:%L)]::%message%n");
                if (hasLogPath) {
                    LoggingConfiguration.initFileLogger("[%p][%d{HH:mm:ss}][%C{2}(%M:%L)]::%message%n", command.getOptionValue(CommandOptions.LOG.getLongName()));
                }
                LoggingConfiguration.setLevel(Level.DEBUG);
            } else {
                LoggingConfiguration.initConsoleLogger();
                if (hasLogPath) {
                    LoggingConfiguration.initFileLogger(null, command.getOptionValue(CommandOptions.LOG.getLongName()));
                }
                LoggingConfiguration.setLevel(Level.INFO);
            }
            LoggingConfiguration.setEpsg();

            printStart();
            if (isHelp || args.length == 0) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.setOptionComparator(null);
                formatter.setWidth(200);
                formatter.setOptPrefix("-");
                formatter.setSyntaxPrefix("Usage: ");
                formatter.setLongOptPrefix(" --");
                formatter.setLongOptSeparator(" ");
                formatter.printHelp("command options", options);
                return;
            }

            GlobalOptions.init(command);
            if (GlobalOptions.getInstance().isLayerJsonGenerate()) {
                log.info("[Generate][layer.json] Start generating layer.json.");
                executeLayerJsonGenerate();
                log.info("[Generate][layer.json] Finished generating layer.json.");
                return;
            } else {
                log.info("[Generate] Start Terrainer process.");
                executeCustomTree();
                log.info("[Generate] Finished Terrainer process.");
                if (!globalOptions.isLeaveTemp()) {
                    cleanTemp();
                }
            }
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        printEnd();
        LoggingConfiguration.destroyLogger();
    }

    /**
     * Executes the terrainer process, in custom tree mode.
     * @throws IOException if an I/O error occurs.
     * @throws FactoryException if a factory error occurs.
     * @throws TransformException if a transform error occurs.
     */
    private static void executeCustomTree() throws Exception {
        // In custom-tree mode, leaf nodes can have different depths.
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        // Check if the tile mesh generation is a continuation/modify from an existing tileSet
        boolean isContinue = globalOptions.isContinue();
        boolean isModify = globalOptions.isModify();

        TileWgs84Manager tileWgs84Manager = new TileWgs84Manager();

        log.info("[Pre][AvailableTileSet] Start calculating available tiles for each depth.");
        tileWgs84Manager.calculateAvailableTilesForEachDepth();
        log.info("[Pre][AvailableTileSet] Finished calculating available tiles for each depth.");

        log.info("[Pre][Standardization] Start GeoTiff Standardization files.");
        tileWgs84Manager.processStandardizeRasters();
        log.info("[Pre][Standardization] Finished GeoTiff Standardization files.");

        log.info("[Pre][Resize] Start GeoTiff Resizing files.");
        tileWgs84Manager.processResizeRasters(globalOptions.getInputPath(), null);
        log.info("[Pre][Resize] Finished GeoTiff Resizing files.");

        log.info("[Tile] Start generate terrain elevation data.");
        TerrainElevationDataManager terrainElevationDataManager = new TerrainElevationDataManager();
        tileWgs84Manager.setTerrainElevationDataManager(terrainElevationDataManager);
        terrainElevationDataManager.setTileWgs84Manager(tileWgs84Manager);
        terrainElevationDataManager.setTerrainElevationDataFolderPath(globalOptions.getResizedTiffTempPath() + File.separator + "0");

        int depth = 0;
        terrainElevationDataManager.makeTerrainQuadTree(depth);
        log.info("[Tile] Finished generate terrain elevation data.");

        //*******************************************************************
        //int processType = globalOptions.getProcessType(); // 20260312
        int processType = 1; // 20260312
        // processType : 1 - Normal, 2 - continue, 3 - modify
        //*******************************************************************
        if (isContinue) {
            processType = 2;
        } else if(isModify){
            processType = 3;
        }

        if (processType == 2) {
            log.info("[Tile] Continuing making tile meshes.");
            tileWgs84Manager.makeTileMeshesContinueCustom();
            log.info("[Tile] Finished making tile meshes.");
        } else if (processType == 1){
            log.info("[Tile] Start making tile meshes.");
            tileWgs84Manager.makeTileMeshesCustom(); // New.****************
            log.info("[Tile] Finished making tile meshes.");
        } else if (processType == 3){
            log.info("[Tile] Start making tile meshes.");
            tileWgs84Manager.makeTileMeshesCustomModifyMode();
            log.info("[Tile] Finished making tile meshes.");
        }

        log.info("[Post][Clear] Start deleting memory objects.");
        tileWgs84Manager.deleteObjects();
        log.info("[Post][Clear] Finished deleting memory objects.");

        System.gc();
    }

    /**
     * Executes the terrainer process.
     * @throws IOException if an I/O error occurs.
     * @throws FactoryException if a factory error occurs.
     * @throws TransformException if a transform error occurs.
     */
    private static void executeFullTree() throws Exception {
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        TileWgs84Manager tileWgs84Manager = new TileWgs84Manager();

        log.info("[Pre][Standardization] Start GeoTiff Standardization files.");
        tileWgs84Manager.processStandardizeRasters();
        log.info("[Pre][Standardization] Finished GeoTiff Standardization files.");

        log.info("[Pre][Resize] Start GeoTiff Resizing files.");
        tileWgs84Manager.processResizeRasters(globalOptions.getInputPath(), null);
        log.info("[Pre][Resize] Finished GeoTiff Resizing files.");

        log.info("[Tile] Start generate terrain elevation data.");
        TerrainElevationDataManager terrainElevationDataManager = new TerrainElevationDataManager();
        tileWgs84Manager.setTerrainElevationDataManager(terrainElevationDataManager);
        tileWgs84Manager.getTerrainElevationDataManager().setTileWgs84Manager(tileWgs84Manager);
        tileWgs84Manager.getTerrainElevationDataManager().setTerrainElevationDataFolderPath(globalOptions.getResizedTiffTempPath() + File.separator + "0");

        int depth = 0;
        tileWgs84Manager.getTerrainElevationDataManager().makeTerrainQuadTree(depth);
        log.info("[Tile] Finished generate terrain elevation data.");

        // Check if the tile mesh generation is a continuation from an existing tileSet
        boolean isContinue = globalOptions.isContinue();
        if (isContinue) {
            log.info("[Tile] Continuing making tile meshes.");
            tileWgs84Manager.makeTileMeshesContinue();
            log.info("[Tile] Finished making tile meshes.");
        } else {
            log.info("[Tile] Start making tile meshes.");
            tileWgs84Manager.makeTileMeshes();
            log.info("[Tile] Finished making tile meshes.");
        }

        log.info("[Post][Clear] Start deleting memory objects.");
        tileWgs84Manager.deleteObjects();
        log.info("[Post][Clear] Finished deleting memory objects.");
    }

    /**
     * Executes the layer.json generation.
     */
    private static void executeLayerJsonGenerate() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        TerrainLayer terrainLayer = new TerrainLayer();
        terrainLayer.setDefault();
        terrainLayer.setBounds(new double[]{-180.0, -90.0, 180.0, 90.0});
        terrainLayer.generateAvailableTiles(globalOptions.getInputPath());
        if (globalOptions.isCalculateNormalsExtension()) {
            terrainLayer.addExtension("octvertexnormals");
        }
        if (globalOptions.isWaterMaskExtension()) {
            terrainLayer.addExtension("watermask");
        }
        if (globalOptions.isMetaDataExtension()) {
            terrainLayer.addExtension("metadata");
        }
        terrainLayer.saveJsonFile(globalOptions.getInputPath(), "layer.json");
    }

    /**
     * Cleans the temporary folders.
     */
    private static void cleanTemp() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        File tileTempFolder = new File(globalOptions.getTileTempPath());
        if (tileTempFolder.exists() && tileTempFolder.isDirectory()) {
            FileUtils.deleteQuietly(tileTempFolder);
        }

        File splitTempFolder = new File(globalOptions.getSplitTiffTempPath());
        if (splitTempFolder.exists() && splitTempFolder.isDirectory()) {
            FileUtils.deleteQuietly(splitTempFolder);
        }

        File resizedTempFolder = new File(globalOptions.getResizedTiffTempPath());
        if (resizedTempFolder.exists() && resizedTempFolder.isDirectory()) {
            FileUtils.deleteQuietly(resizedTempFolder);
        }
    }

    /**
     * Prints the program information and the java version information.
     */
    private static void printStart() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        String programInfo = globalOptions.getProgramInfo();
        drawLine();
        log.info(programInfo);
        drawLine();
    }

    /**
     * Prints the total file count, total tile count, and the process time.
     */
    private static void printEnd() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        drawLine();
        log.info("[Process Summary]");
        log.info("Total process time : {} sec", DecimalUtils.millisecondToDisplayTime(globalOptions.getProcessTimeMillis()));
        drawLine();
    }

    public static void drawLine() {
        log.info("----------------------------------------");
    }
}