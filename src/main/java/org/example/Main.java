package org.example;

import com.gaia3d.airPollutionDataConverter.AirPollutionDataConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;

import javax.imageio.metadata.IIOInvalidTreeException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
@Slf4j
public class Main {
    public static void main(String[] args) throws ParseException, IIOInvalidTreeException, FileNotFoundException {
        Configurator.initConsoleLogger();

        log.info("Start the program.");

        Options options = new Options();
        options.addOption("input", true, "input folder path");
        options.addOption("output", true, "output folder path");
        options.addOption("scale", true, "scales the data values");

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);

        String inputFolderPath = commandLine.getOptionValue("input");

        if(inputFolderPath == null)
        {
            log.error("Input folder path is not provided.");
            return;
        }

        // Is possible that inputFolderPath contains the dataStructure.json file name.***
        // must check if the inputFolderPath is directory or file.***
        // case 1 : String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirpollution_20241008\\O_PM10";
        // case 2 : String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirpollution_20241008\\O_PM10\\dataStructureCustom.json";

        String dataStructureJsonFileName = "dataStructure.json";
        File file = new File(inputFolderPath);
        if(file.isDirectory())
        {
            // ok.***
        }
        else if(file.isFile())
        {
            // remove the fileName from the path and keep in a string.***
            dataStructureJsonFileName = file.getName();
            inputFolderPath = file.getParent();

        }

        if(!inputFolderPath.endsWith(File.separator))
        {
            inputFolderPath += File.separator;
        }

        String outputFolderPath = commandLine.getOptionValue("output");

        if(outputFolderPath == null)
        {
            log.error("Output folder path is not provided.");
            return;
        }

        if(!outputFolderPath.endsWith(File.separator))
        {
            outputFolderPath += File.separator;
        }

        // Convert the data.************************************************************************************
        String inputDataStructurePath = inputFolderPath + dataStructureJsonFileName;
        AirPollutionDataConverter airPollDataConverter = new AirPollutionDataConverter();

        if(commandLine.hasOption("scale"))
        {
            double scale = Double.parseDouble(commandLine.getOptionValue("scale"));
            airPollDataConverter.setScale(scale);
        }


        airPollDataConverter.convertDataByDataStructureFile(inputDataStructurePath, inputFolderPath, outputFolderPath);

    }
}