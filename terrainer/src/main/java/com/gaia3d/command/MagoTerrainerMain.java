package com.gaia3d.command;


import com.gaia3d.process.ProcessOptions;
import com.gaia3d.wgs84Tiles.TerrainElevationDataManager;
import com.gaia3d.wgs84Tiles.TerrainLayer;
import com.gaia3d.wgs84Tiles.TileWgs84Manager;
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
            boolean isHelp = command.hasOption(ProcessOptions.HELP.getArgName());
            boolean hasLogPath = command.hasOption(ProcessOptions.LOG.getArgName());

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
            Configurator.setEpsg();
            printStart();

            if (isHelp) {
                new HelpFormatter().printHelp("Mago 3D Quantized Mesher", options);
                return;
            }

            GlobalOptions.init(command);
            GlobalOptions globalOptions = GlobalOptions.getInstance();
            if (globalOptions.isLayerJsonGenerate()) {
                log.info("[Generate][layer.json] Start generating layer.json.");

                TerrainLayer terrainLayer = new TerrainLayer();
                terrainLayer.setDefault();
                terrainLayer.setBounds(new double[]{-180.0, -90.0, 180.0, 90.0}); // temp
                terrainLayer.generateAvailableTiles(globalOptions.getInputPath());
                if (globalOptions.isCalculateNormals()) {
                    terrainLayer.addExtension("octvertexnormals");
                }
                terrainLayer.saveJsonFile(globalOptions.getInputPath(), "layer.json");
                log.info("[Generate][layer.json] Finished generating layer.json.");
                return;
            }

            if (GlobalOptions.getInstance().isLayerJsonGenerate()) {
                log.info("[Generate][layer.json] Start generating layer.json.");
                executeLayerJsonGenerate();
                log.info("[Generate][layer.json] Finished generating layer.json.");
                return;
            } else {
                log.info("[Generate] Start Terrainer process.");
                execute(command);
                log.info("[Generate] Finished Terrainer process.");
                if (!globalOptions.isDebugMode()) {
                    cleanTemp();
                }
            }

            /*TileWgs84Manager tileWgs84Manager = new TileWgs84Manager();
            if (command.hasOption(ProcessOptions.CALCULATE_NORMALS.getArgName())) {
                tileWgs84Manager.setCalculateNormals(true);
            }

            log.info("[Pre][Split GeoTiff] Start GeoTiff Splitting files.");
            tileWgs84Manager.processSplitGeotiffs(globalOptions.getInputPath(), globalOptions.getSplitTiffTempPath());
            log.info("[Pre][Split GeoTiff] Finished GeoTiff Splitting files.");

            log.info("[Pre][Resize GeoTiff] Start GeoTiff Resizing files.");
            tileWgs84Manager.processResizeGeotiffs(globalOptions.getInputPath(), null);
            log.info("[Pre][Resize GeoTiff] Finished GeoTiff Resizing files.");

            log.info("[Pre][Terrain Elevation Data] Start making terrain elevation data.");
            tileWgs84Manager.setTerrainElevationDataManager(new TerrainElevationDataManager());
            tileWgs84Manager.getTerrainElevationDataManager().setTileWgs84Manager(tileWgs84Manager);
            tileWgs84Manager.getTerrainElevationDataManager().setTerrainElevationDataFolderPath(globalOptions.getResizedTiffTempPath() + File.separator + "0");
            if (tileWgs84Manager.getGeoTiffFilesCount() == 1) {
                tileWgs84Manager.getTerrainElevationDataManager().setGeoTiffFilesCount(1);
                tileWgs84Manager.getTerrainElevationDataManager().setUniqueGeoTiffFilePath(tileWgs84Manager.getUniqueGeoTiffFilePath());
                tileWgs84Manager.getTerrainElevationDataManager().MakeUniqueTerrainElevationData();
            } else {
                tileWgs84Manager.getTerrainElevationDataManager().makeTerrainQuadTree();
            }
            log.info("[Pre][Terrain Elevation Data] Finished making terrain elevation data.");

            log.info("[Tile] Start making tile meshes.");
            tileWgs84Manager.makeTileMeshes();
            log.info("[Tile] Finished making tile meshes.");

            tileWgs84Manager.deleteObjects();
            System.gc();*/

            // Finally delete temp folder.***
            /*File tileTempFolder = new File(globalOptions.getTileTempPath());
            if (tileTempFolder.exists() && tileTempFolder.isDirectory()) {
                try {
                    log.info("[Post]Deleting tileTempFolder");
                    FileUtils.deleteDirectory(tileTempFolder);
                } catch (IOException e) {
                    log.error("[Post]Failed to delete tileTempFolder.", e);
                    throw new RuntimeException(e);
                }
            }

            File splitTempFolder = new File(globalOptions.getSplitTiffTempPath());
            if (splitTempFolder.exists() && splitTempFolder.isDirectory()) {
                try {
                    log.info("[Post]Deleting splitTempFolder");
                    FileUtils.deleteDirectory(splitTempFolder);
                } catch (IOException e) {
                    log.error("[Post]Failed to delete splitTempFolder.", e);
                    throw new RuntimeException(e);
                }
            }

            File resizedTempFolder = new File(globalOptions.getResizedTiffTempPath());
            if (resizedTempFolder.exists() && resizedTempFolder.isDirectory()) {
                try {
                    log.info("[Post]Deleting resizedTempFolder");
                    FileUtils.deleteDirectory(resizedTempFolder);
                } catch (IOException e) {
                    log.error("[Post]Failed to delete resizedTempFolder.", e);
                    throw new RuntimeException(e);
                }
            }*/
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
        }
        printEnd();
        Configurator.destroyLogger();
    }

    /**
     * Executes the terrainer process.
     * @param command the command line options.
     * @throws IOException if an I/O error occurs.
     * @throws FactoryException if a factory error occurs.
     * @throws TransformException if a transform error occurs.
     */
    private static void execute(CommandLine command) throws IOException, FactoryException, TransformException {
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        TileWgs84Manager tileWgs84Manager = new TileWgs84Manager();
        if (command.hasOption(ProcessOptions.CALCULATE_NORMALS.getArgName())) {
            tileWgs84Manager.setCalculateNormals(true);
        }

        log.info("[Pre][Split GeoTiff] Start GeoTiff Splitting files.");
        tileWgs84Manager.processSplitGeotiffs(globalOptions.getInputPath(), globalOptions.getSplitTiffTempPath());
        log.info("[Pre][Split GeoTiff] Finished GeoTiff Splitting files.");

        log.info("[Pre][Resize GeoTiff] Start GeoTiff Resizing files.");
        tileWgs84Manager.processResizeGeotiffs(globalOptions.getInputPath(), null);
        log.info("[Pre][Resize GeoTiff] Finished GeoTiff Resizing files.");

        log.info("[Pre][Terrain Elevation Data] Start making terrain elevation data.");
        tileWgs84Manager.setTerrainElevationDataManager(new TerrainElevationDataManager());
        tileWgs84Manager.getTerrainElevationDataManager().setTileWgs84Manager(tileWgs84Manager);
        tileWgs84Manager.getTerrainElevationDataManager().setTerrainElevationDataFolderPath(globalOptions.getResizedTiffTempPath() + File.separator + "0");
        if (tileWgs84Manager.getGeoTiffFilesCount() == 1) {
            tileWgs84Manager.getTerrainElevationDataManager().setGeoTiffFilesCount(1);
            tileWgs84Manager.getTerrainElevationDataManager().setUniqueGeoTiffFilePath(tileWgs84Manager.getUniqueGeoTiffFilePath());
            tileWgs84Manager.getTerrainElevationDataManager().MakeUniqueTerrainElevationData();
        } else {
            tileWgs84Manager.getTerrainElevationDataManager().makeTerrainQuadTree();
        }
        log.info("[Pre][Terrain Elevation Data] Finished making terrain elevation data.");

        log.info("[Tile] Start making tile meshes.");
        tileWgs84Manager.makeTileMeshes();
        log.info("[Tile] Finished making tile meshes.");

        tileWgs84Manager.deleteObjects();
        System.gc();
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
                log.info("[Post]Deleting tileTempFolder");
                FileUtils.deleteDirectory(tileTempFolder);
            } catch (IOException e) {
                log.error("[Post]Failed to delete tileTempFolder.", e);
                throw new RuntimeException(e);
            }
        }

        File splitTempFolder = new File(globalOptions.getSplitTiffTempPath());
        if (splitTempFolder.exists() && splitTempFolder.isDirectory()) {
            try {
                log.info("[Post]Deleting splitTempFolder");
                FileUtils.deleteDirectory(splitTempFolder);
            } catch (IOException e) {
                log.error("[Post]Failed to delete splitTempFolder.", e);
                throw new RuntimeException(e);
            }
        }

        File resizedTempFolder = new File(globalOptions.getResizedTiffTempPath());
        if (resizedTempFolder.exists() && resizedTempFolder.isDirectory()) {
            try {
                log.info("[Post]Deleting resizedTempFolder");
                FileUtils.deleteDirectory(resizedTempFolder);
            } catch (IOException e) {
                log.error("[Post]Failed to delete resizedTempFolder.", e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Prints the program information and the java version information.
     */
    private static void printStart() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        String programInfo = globalOptions.getProgramInfo();
        log.info("\n" + "┳┳┓┏┓┏┓┏┓  ┏┓┳┓  ┏┳┓┏┓┳┓┳┓┏┓┳┳┓┏┓┳┓\n" + "┃┃┃┣┫┃┓┃┃   ┫┃┃   ┃ ┣ ┣┫┣┫┣┫┃┃┃┣ ┣┫\n" + "┛ ┗┛┗┗┛┗┛  ┗┛┻┛   ┻ ┗┛┛┗┛┗┛┗┻┛┗┗┛┛┗\n" + programInfo + "\n" + "----------------------------------------");
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
        long startTime = globalOptions.getStartTime();
        long endTime = System.currentTimeMillis();
        log.info("----------------------------------------");
        log.info("End Process Time : {}", millisecondToDisplayTime(endTime - startTime));
        log.info("----------------------------------------");
    }

    /**
     * Converts the byte size to the display size.
     */
    private static String byteCountToDisplaySize(long size) {
        String displaySize;
        if (size / 1073741824L > 0L) {
            displaySize = size / 1073741824L + "GB";
        } else if (size / 1048576L > 0L) {
            displaySize = size / 1048576L + "MB";
        } else if (size / 1024L > 0L) {
            displaySize = size / 1024L + "KB";
        } else {
            displaySize = size + "bytes";
        }
        return displaySize;
    }

    /**
     * Converts the millisecond to the display time.
     */
    private static String millisecondToDisplayTime(long millis) {
        String displayTime = "";
        if (millis / 3600000L > 0L) {
            displayTime += millis / 3600000L + "h ";
        }
        if (millis / 60000L > 0L) {
            displayTime += millis / 60000L + "m ";
        }
        if (millis / 1000L > 0L) {
            displayTime += millis / 1000L + "s ";
        }
        displayTime += millis % 1000L + "ms";
        return displayTime;
    }
}