package com.gaia3d;

import com.gaia3d.airPollutionDataConverter.AirPollutionDataConverter;
import com.gaia3d.chemicalAccidentData2DConverter.ChemicalContaminationData2DConverter;
import com.gaia3d.chemicalAccidentData2DConverter.ChemicalContaminationData2DConverterV2;
import com.gaia3d.chemicalAccidentDataConverter.ChemicalContaminationDataConverter;
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

        String dataType = commandLine.getOptionValue("dataType");
        if (dataType == null) {
            log.error("Data type is not provided.");
            printEndMessage();
            return;
        }

        // dataType = AIR-POLLUTION, CHEMICAL-ACCIDENT, etc.

        log.info("Start the program.");

        if(dataType.equals("AIR-POLLUTION")) {
            log.info("Data type is AIR-POLLUTION.");
            processAirPollutionData(inputFolderPath, outputFolderPath, commandLine);
        }
        else if(dataType.equals("CHEMICAL-ACCIDENT")) {
            log.info("Data type is CHEMICAL-ACCIDENT.");
            processChemicalAccidentData(inputFolderPath, outputFolderPath, commandLine);
        }
        else {
            log.error("Unsupported data type: {}", dataType);
            printEndMessage();
            return;
        }
        printEndMessage();
    }

    private static void processChemicalAccidentData(String inputFolderPath, String outputFolderPath, CommandLine commandLine) {
        // Implement the logic to process chemical accident data.
        // This is a placeholder for the actual implementation.
        log.info("Processing chemical accident data from {} to {}", inputFolderPath, outputFolderPath);
        // Is possible that inputFolderPath contains the dataStructure.json file name.
        // must check if the inputFolderPath is directory or file.
        // case 1: String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirpollution_20241008\\O_PM10";
        // case 2: String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirpollution_20241008\\O_PM10\\dataStructureCustom.json";
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

        String inputDataStructurePath = commandLine.getOptionValue("inputDataStructurePath");
        String type = commandLine.getOptionValue("type");

        if(type.equals("CHEMICAL"))
        {
            // Chemical contamination.************************************
            ChemicalContaminationDataConverter chemicalContaminationDataConverter = new ChemicalContaminationDataConverter();
            try {
                chemicalContaminationDataConverter.ConvertDataByDataStructureFile(inputDataStructurePath, inputFolderPath, outputFolderPath);
            }
            catch (Exception e) {
                log.error("Error converting chemical contamination data: ", e);
            }
        }
        else if(type.equals("CHEMICAL_2D"))
        {
            // Chemical contamination 2D.************************************
            ChemicalContaminationData2DConverter txtToPngConverter = new ChemicalContaminationData2DConverter();
            try {
            txtToPngConverter.ConvertData2DByDataStructureFile(inputDataStructurePath, inputFolderPath, outputFolderPath);
            }
            catch (Exception e) {
                log.error("Error converting chemical contamination 2D data: ", e);
            }
        }
        else if(type.equals("CHEMICAL_2D_V2"))
        {
            // Chemical contamination 2D.************************************
            ChemicalContaminationData2DConverterV2 txtToPngConverter = new ChemicalContaminationData2DConverterV2();
            try {
            txtToPngConverter.ConvertData2DByDataStructureFile(inputDataStructurePath, inputFolderPath, outputFolderPath);
            }
            catch (Exception e) {
                log.error("Error converting chemical contamination 2D V2 data: ", e);
            }
        }
        else
        {
            System.out.println("Wrong type.");
        }
    }

    private static void processAirPollutionData(String inputFolderPath, String outputFolderPath, CommandLine commandLine) {
        // Implement the logic to process air pollution data.
        // This is a placeholder for the actual implementation.
        log.info("Processing air pollution data from {} to {}", inputFolderPath, outputFolderPath);
        // Is possible that inputFolderPath contains the dataStructure.json file name.
        // must check if the inputFolderPath is directory or file.
        // case 1: String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirpollution_20241008\\O_PM10";
        // case 2: String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirpollution_20241008\\O_PM10\\dataStructureCustom.json";
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

        String inputDataStructurePath = inputFolderPath + dataStructureJsonFileName;

        // Mode volumetric data.*****************************************************************************************
        AirPollutionDataConverter airPollDataConverter = new AirPollutionDataConverter();

        if (commandLine.hasOption("scale")) {
            double scale = Double.parseDouble(commandLine.getOptionValue("scale"));
            airPollDataConverter.setScale(scale);
        }
        try {
            airPollDataConverter.convertDataByDataStructureFile(inputDataStructurePath, inputFolderPath, outputFolderPath);
        }
        catch (Exception e) {
            log.error("Error converting air pollution data: ", e);
        }
    }

    private static Options createOptions() {
        Options options = new Options();
        options.addOption("input", true, "input folder path");
        options.addOption("output", true, "output folder path");
        options.addOption("log", true, "output log file path");
        options.addOption("scale", true, "scales the data values");
        options.addOption("help", false, "show help");
        options.addOption("dataType", true, "data type (e.g., AIR-POLLUTION, CHEMICAL-ACCIDENT, etc.)");
        options.addOption("type", true, "type (e.g., CHEMICAL, ITINERARY, ITINERARY_V2, CHEMICAL_2D, CHEMICAL_2D_V2, etc.)");
        options.addOption("inputDataStructurePath", true, "inputDataStructurePath");
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