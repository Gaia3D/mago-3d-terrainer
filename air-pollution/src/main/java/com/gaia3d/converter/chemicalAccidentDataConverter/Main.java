package com.gaia3d.converter.chemicalAccidentDataConverter;

import com.gaia3d.Configuration;
import com.gaia3d.converter.chemicalAccidentData2DConverter.AsciiTxtToJsonConverter;
import com.gaia3d.converter.chemicalAccidentData2DConverter.ChemicalContaminationData2DConverter;
import com.gaia3d.converter.chemicalAccidentData2DConverter.ChemicalContaminationData2DConverterV2;
import com.gaia3d.itinerary.ItineraryDataConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;

import java.io.IOException;
// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.

@Slf4j
@Deprecated
public class Main {
    public static void main(String[] args) throws ParseException, IOException {
        Configuration.initConsoleLogger();
        log.info("Start the program.");

        Options options = new Options();
        options.addOption("dataType", true, "input data type");
        options.addOption("type", true, "conversion type");
        options.addOption("input", true, "input folder path");
        options.addOption("inputDataStructurePath", true, "inputDataStructurePath");

        options.addOption("output", true, "output folder path");
        options.addOption("fileName", true, "input file name");

        options.addOption("locationIndicesFileName", true, "location indices file name");
        options.addOption("itineraryFileName", true, "itinerary file name");

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);

        String type = commandLine.getOptionValue("type");

        if (type.equals("CHEMICAL")) {
            // Chemical contamination*********************************
            String inputDataStructurePath = commandLine.getOptionValue("inputDataStructurePath");
            String inputFolderPath = commandLine.getOptionValue("input");
            String outputFolderPath = commandLine.getOptionValue("output");

            ChemicalContaminationDataConverter chemicalContaminationDataConverter = new ChemicalContaminationDataConverter();
            chemicalContaminationDataConverter.ConvertDataByDataStructureFile(inputDataStructurePath, inputFolderPath, outputFolderPath);
        } else if (type.equals("ITINERARY")) {
            // Itinerary**********************************************
            String inputFolderPath = commandLine.getOptionValue("input");
            String outputFolderPath = commandLine.getOptionValue("output");
            String locationIndicesFileName = commandLine.getOptionValue("locationIndicesFileName");
            String itineraryFileName = commandLine.getOptionValue("itineraryFileName");

            ItineraryDataConverter itineraryDataConverter = new ItineraryDataConverter();
            itineraryDataConverter.ConvertData(inputFolderPath, outputFolderPath, locationIndicesFileName, itineraryFileName);
        } else if (type.equals("ITINERARY_V2")) {
            // Itinerary**********************************************
            String inputFolderPath = commandLine.getOptionValue("input");
            String outputFolderPath = commandLine.getOptionValue("output");
            //String locationIndicesFileName = commandLine.getOptionValue("locationIndicesFileName");
            String itineraryFileName = commandLine.getOptionValue("itineraryFileName");

            ItineraryDataConverter itineraryDataConverter = new ItineraryDataConverter();
            itineraryDataConverter.ConvertData_V2(inputFolderPath, outputFolderPath, itineraryFileName);
        } else if (type.equals("EXAMPLE_DATA_STRUCTURE")) {
            // Write an example of dataStructure**********************
            String outputFolderPath = commandLine.getOptionValue("output");
            String fileName = commandLine.getOptionValue("fileName");
            String outputFilePath = outputFolderPath + "\\" + fileName;

            ChemicalContaminationDataConverter chemicalContaminationDataConverter = new ChemicalContaminationDataConverter();
            chemicalContaminationDataConverter.writeExampleDataStructureJson(outputFilePath);
        } else if (type.equals("CHEMICAL_2D")) {
            // Chemical contamination 2D*********************************
            String inputDataStructurePath = commandLine.getOptionValue("inputDataStructurePath");
            String inputFolderPath = commandLine.getOptionValue("input");
            String outputFolderPath = commandLine.getOptionValue("output");

            ChemicalContaminationData2DConverter txtToPngConverter = new ChemicalContaminationData2DConverter();
            txtToPngConverter.ConvertData2DByDataStructureFile(inputDataStructurePath, inputFolderPath, outputFolderPath);
        } else if (type.equals("CHEMICAL_2D_V2")) {
            // Chemical contamination 2D*********************************
            String inputDataStructurePath = commandLine.getOptionValue("inputDataStructurePath");
            String inputFolderPath = commandLine.getOptionValue("input");
            String outputFolderPath = commandLine.getOptionValue("output");

            ChemicalContaminationData2DConverterV2 txtToPngConverter = new ChemicalContaminationData2DConverterV2();
            txtToPngConverter.ConvertData2DByDataStructureFile(inputDataStructurePath, inputFolderPath, outputFolderPath);
        } else if (type.equals("CSV_TO_MATRIXTXT_2D")) {
            // Convert txt file to json 2D*********************************
            String inputDataStructurePath = commandLine.getOptionValue("inputDataStructurePath");
            String inputFolderPath = commandLine.getOptionValue("input");
            String outputFolderPath = commandLine.getOptionValue("output");

            AsciiTxtToJsonConverter txtToJsonConverter = new AsciiTxtToJsonConverter();
            txtToJsonConverter.ConvertCsvTxtToMatrixTxtInFolder(inputFolderPath, outputFolderPath);

        } else {
            log.info("Wrong type.");
        }


    }
}