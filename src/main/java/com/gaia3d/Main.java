package com.gaia3d;

import com.gaia3d.airPollutionDataConverter.AirPollutionDataConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;

import javax.imageio.metadata.IIOInvalidTreeException;
import java.io.File;
import java.io.FileNotFoundException;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
@Slf4j
public class Main {
    public static void main(String[] args) throws ParseException, IIOInvalidTreeException, FileNotFoundException {
        Configurator.initConsoleLogger();
        printStartMessage();
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);

        if (commandLine.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("AirPollutionDataConverter", options);
            printEndMessage();
            return;
        }

        String inputFolderPath = commandLine.getOptionValue("input");
        if (inputFolderPath == null) {
            log.error("Input folder path is not provided.");
            printEndMessage();
            return;
        }

        String outputFolderPath = commandLine.getOptionValue("output");
        if (outputFolderPath == null) {
            log.error("Output folder path is not provided.");
            printEndMessage();
            return;
        }

        if (commandLine.hasOption("log")) {
            String logFilePath = commandLine.getOptionValue("log");
            File logFile = new File(logFilePath);
            if (logFile.exists()) {
                logFile.delete();
            }
            Configurator.initFileLogger(null, logFilePath);
        }

        log.info("Start the program.");

        // Is possible that inputFolderPath contains the dataStructure.json file name.
        // must check if the inputFolderPath is directory or file.
        // case 1 : String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirpollution_20241008\\O_PM10";
        // case 2 : String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirpollution_20241008\\O_PM10\\dataStructureCustom.json";
        String dataStructureJsonFileName = "dataStructure.json";
        File file = new File(inputFolderPath);
        if (file.isFile()) {
            // remove the fileName from the path and keep in a string.
            dataStructureJsonFileName = file.getName();
            inputFolderPath = file.getParent();
        }

        if (!inputFolderPath.endsWith(File.separator)) {
            inputFolderPath += File.separator;
        }

        if (!outputFolderPath.endsWith(File.separator)) {
            outputFolderPath += File.separator;
        }

        // Convert the data.
        String inputDataStructurePath = inputFolderPath + dataStructureJsonFileName;
        AirPollutionDataConverter airPollDataConverter = new AirPollutionDataConverter();

        if (commandLine.hasOption("scale")) {
            double scale = Double.parseDouble(commandLine.getOptionValue("scale"));
            airPollDataConverter.setScale(scale);
        }
        airPollDataConverter.convertDataByDataStructureFile(inputDataStructurePath, inputFolderPath, outputFolderPath);

        printEndMessage();
    }

    private static Options createOptions() {
        Options options = new Options();
        options.addOption("input", true, "input folder path");
        options.addOption("output", true, "output folder path");
        options.addOption("log", true, "output log file path");
        options.addOption("scale", true, "scales the data values");
        options.addOption("help", false, "show help");
        return options;
    }

    public static void printStartMessage() {
        log.info("=========================START==========================");
        log.info("Air Pollution Data Converter : Gaia3D, Inc.");
        log.info("========================================================");
    }

    public static void printEndMessage() {
        log.info("==========================END===========================");
    }
}