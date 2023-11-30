package org.example;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class MainTest
{
    @Test
    void main() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirPollution\\B_PM10";
        String outputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirPollution\\B_PM10\\output";
        String inputDataStructurePath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirPollution\\B_PM10\\dataStructure.json";
        String[] testArgs = new String[]{
                "-type", "AIR-POLLUTION",
                "-input", inputFolderPath,
                "-output", outputFolderPath,
                "-inputDataStructurePath", inputDataStructurePath
        };

        Main.main(testArgs);
    }

    @Test
    void main_O_NO2() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirPollution\\O_NO2";
        String outputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirPollution\\O_NO2\\output";
        String inputDataStructurePath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirPollution\\O_NO2\\dataStructure.json";
        String[] testArgs = new String[]{
                "-type", "AIR-POLLUTION",
                "-input", inputFolderPath,
                "-output", outputFolderPath,
                "-inputDataStructurePath", inputDataStructurePath
        };

        Main.main(testArgs);
    }
}