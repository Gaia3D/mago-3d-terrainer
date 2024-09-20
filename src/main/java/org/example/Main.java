package org.example;

import com.gaia3d.airPollutionDataConverter.AirPollutionDataConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;

import javax.imageio.metadata.IIOInvalidTreeException;
import java.io.FileNotFoundException;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
@Slf4j
public class Main {
    public static void main(String[] args) throws ParseException, IIOInvalidTreeException, FileNotFoundException {
        Configurator.initConsoleLogger();

        log.info("Start the program.");

        Options options = new Options();
        options.addOption("type", true, "conversion type");
        options.addOption("input", true, "input folder path");
        options.addOption("output", true, "output folder path");
        options.addOption("inputDataStructurePath", true, "inputDataStructurePath");
        options.addOption("maxDatesCount", true, "max dates count");
        options.addOption("scale", true, "scales the data values");

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(options, args);

        String type = commandLine.getOptionValue("type");

        if (type == "AIR-POLLUTION") {
            // ICT-EIA air pollution test.*************************************
            String inputFolderPath = commandLine.getOptionValue("input");
            String outputFolderPath = commandLine.getOptionValue("output");
            String inputDataStructurePath = commandLine.getOptionValue("inputDataStructurePath");
            int maxDatesCount = Integer.parseInt(commandLine.getOptionValue("maxDatesCount"));

            AirPollutionDataConverter airPollDataConverter = new AirPollutionDataConverter();
            if(maxDatesCount > 0)
            {
                airPollDataConverter.setMaxDatesCount(maxDatesCount);
            }
            if(commandLine.hasOption("scale"))
            {
                double scale = Double.parseDouble(commandLine.getOptionValue("scale"));
                airPollDataConverter.setScale(scale);
            }
            airPollDataConverter.convertDataByDataStructureFile(inputDataStructurePath, inputFolderPath, outputFolderPath);
        }
    }
}