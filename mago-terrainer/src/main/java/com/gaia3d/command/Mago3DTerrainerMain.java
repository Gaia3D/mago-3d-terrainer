package com.gaia3d.command;

import com.gaia3d.terrain.tile.layer.TerrainLayer;
import com.gaia3d.util.DecimalUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;

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
            boolean isVerbose = command.hasOption(CommandOptions.VERBOSE.getLongName());

            if (isQuiet) {
                LoggingConfiguration.setLevel(Level.OFF);
            } else if (isDebug) {
                LoggingConfiguration.initConsoleLogger("[%p][%d{HH:mm:ss}][%C{2}(%M:%L)]::%message%n");
                if (hasLogPath) {
                    LoggingConfiguration.initFileLogger("[%p][%d{HH:mm:ss}][%C{2}(%M:%L)]::%message%n", command.getOptionValue(CommandOptions.LOG.getLongName()));
                }
                LoggingConfiguration.setLevel(Level.DEBUG);
            } else if (isVerbose) {
                LoggingConfiguration.initConsoleLogger();
                if (hasLogPath) {
                    LoggingConfiguration.initFileLogger(null, command.getOptionValue(CommandOptions.LOG.getLongName()));
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
                new TerrainerPipeline().execute();
                log.info("[Generate] Finished Terrainer process.");
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
        } catch (Throwable e) {
            log.error("An unexpected error occurred.", e);
            throw new RuntimeException(e);
        }
        printEnd();
        LoggingConfiguration.destroyLogger();
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
