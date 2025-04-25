package com.gaia3d.command;


import com.gaia3d.basic.exception.Reporter;
import com.gaia3d.terrain.tile.TerrainElevationDataManager;
import com.gaia3d.terrain.tile.TerrainLayer;
import com.gaia3d.terrain.tile.TileWgs84Manager;
import com.gaia3d.util.DecimalUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.File;
import java.io.IOException;

@Slf4j
public class MagoTerrainerMain {

    public static void main(String[] args) {
        try {
            Options options = Configurator.createOptions();
            CommandLineParser parser = new DefaultParser();
            CommandLine command = parser.parse(Configurator.createOptions(), args);
            boolean isHelp = command.hasOption(CommandOptions.HELP.getArgName());
            boolean hasLogPath = command.hasOption(CommandOptions.LOG.getArgName());

            if (command.hasOption(CommandOptions.DEBUG.getArgName())) {
                Configurator.initConsoleLogger("[%p][%d{HH:mm:ss}][%C{2}(%M:%L)]::%message%n");
                if (hasLogPath) {
                    Configurator.initFileLogger("[%p][%d{HH:mm:ss}][%C{2}(%M:%L)]::%message%n", command.getOptionValue(CommandOptions.LOG.getArgName()));
                }
                Configurator.setLevel(Level.DEBUG);
            } else {
                Configurator.initConsoleLogger();
                if (hasLogPath) {
                    Configurator.initFileLogger(null, command.getOptionValue(CommandOptions.LOG.getArgName()));
                }
                Configurator.setLevel(Level.INFO);
            }
            Configurator.setEpsg();
            printStart();

            if (isHelp) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.setWidth(200);
                formatter.printHelp("mago 3DTerrainer help", options);
                return;
            }

            GlobalOptions.init(command);
            GlobalOptions globalOptions = GlobalOptions.getInstance();

            if (GlobalOptions.getInstance().isLayerJsonGenerate()) {
                log.info("[Generate][layer.json] Start generating layer.json.");
                executeLayerJsonGenerate();
                log.info("[Generate][layer.json] Finished generating layer.json.");
                return;
            } else {
                log.info("[Generate] Start Terrainer process.");
                execute();
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
        Configurator.destroyLogger();
    }

    /**
     * Executes the terrainer process.
     *
     * @throws IOException        if an I/O error occurs.
     * @throws FactoryException   if a factory error occurs.
     * @throws TransformException if a transform error occurs.
     */
    private static void execute() throws Exception {
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        TileWgs84Manager tileWgs84Manager = new TileWgs84Manager();

        log.info("[Pre][Standardization] Start GeoTiff Standardization files.");
        tileWgs84Manager.processStandardizeRasters();
        log.info("[Pre][Standardization] Finished GeoTiff Standardization files.");

        log.info("[Pre][Resize] Start GeoTiff Resizing files.");
        tileWgs84Manager.processResizeRasters(globalOptions.getInputPath(), null);
        log.info("[Pre][Resize] Finished GeoTiff Resizing files.");

        log.info("[Tile] Start generate terrain elevation data.");
        tileWgs84Manager.setTerrainElevationDataManager(new TerrainElevationDataManager());
        tileWgs84Manager.getTerrainElevationDataManager().setTileWgs84Manager(tileWgs84Manager);
        tileWgs84Manager.getTerrainElevationDataManager().setTerrainElevationDataFolderPath(globalOptions.getResizedTiffTempPath() + File.separator + "0");

        int depth = 0;
        tileWgs84Manager.getTerrainElevationDataManager().makeTerrainQuadTree(depth);
        log.info("[Tile] Finished generate terrain elevation data.");

        // Check if the tile mesh generation is a continuation from an existing tileSet.***
        boolean isContinue = globalOptions.isContinue();
        //isContinue = true; // test
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

        globalOptions.getReporter().writeReportFile(new File(globalOptions.getOutputPath()));
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
        if (globalOptions.isCalculateNormals()) {
            terrainLayer.addExtension("octvertexnormals");
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
            try {
                log.info("[Post] Deleting tileTempFolder");
                FileUtils.deleteDirectory(tileTempFolder);
            } catch (IOException e) {
                log.error("[Post] Failed to delete tileTempFolder.", e);
            }
        }

        File splitTempFolder = new File(globalOptions.getSplitTiffTempPath());
        if (splitTempFolder.exists() && splitTempFolder.isDirectory()) {
            try {
                log.info("[Post] Deleting splitTempFolder");
                FileUtils.deleteDirectory(splitTempFolder);
            } catch (IOException e) {
                log.error("[Post] Failed to delete splitTempFolder.", e);
            }
        }

        File resizedTempFolder = new File(globalOptions.getResizedTiffTempPath());
        if (resizedTempFolder.exists() && resizedTempFolder.isDirectory()) {
            try {
                log.info("[Post] Deleting resizedTempFolder");
                FileUtils.deleteDirectory(resizedTempFolder);
            } catch (IOException e) {
                log.error("[Post] Failed to delete resizedTempFolder.", e);
            }
        }
    }

    /**
     * Prints the program information and the java version information.
     */
    private static void printStart() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        String programInfo = globalOptions.getProgramInfo();
        log.info("\n" +
                "┳┳┓┏┓┏┓┏┓  ┏┓┳┓  ┏┳┓┏┓┳┓┳┓┏┓┳┳┓┏┓┳┓\n" +
                "┃┃┃┣┫┃┓┃┃   ┫┃┃   ┃ ┣ ┣┫┣┫┣┫┃┃┃┣ ┣┫\n" +
                "┛ ┗┛┗┗┛┗┛  ┗┛┻┛   ┻ ┗┛┛┗┛┗┛┗┻┛┗┗┛┛┗\n" +
                programInfo + "\n" +
                "----------------------------------------");
    }

    /**
     * Prints the program information and the java version information.
     */
    private static void printVersion() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        String programInfo = globalOptions.getProgramInfo();
        String javaVersionInfo = globalOptions.getJavaVersionInfo();
        log.info(programInfo + "\n" + javaVersionInfo);
        log.info("----------------------------------------");
    }

    /**
     * Prints the total file count, total tile count, and the process time.
     */
    private static void printEnd() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        Reporter reporter = globalOptions.getReporter();
        long duration = reporter.getDuration();
        log.info("----------------------------------------");
        log.info("[Report Summary]");
        log.info("Info : {}", reporter.getInfoCount());
        log.info("Warning : {}", reporter.getWarningCount());
        log.info("Error : {}", reporter.getErrorCount());
        log.info("Fatal : {}", reporter.getFatalCount());
        log.info("Total Report Count : {}", reporter.getReportList().size());
        log.info("[Process Summary]");
        log.info("End Process Time : {}", DecimalUtils.millisecondToDisplayTime(duration));
        log.info("----------------------------------------");
    }
}