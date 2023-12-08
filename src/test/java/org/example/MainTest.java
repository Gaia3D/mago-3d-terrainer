package org.example;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.ParseException;


@Slf4j
class MainTest
{
    @Test
    void main_B_PM10() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirPollution\\B_PM10";
        String outputFolderPath = inputFolderPath + "\\output";
        String inputDataStructurePath = inputFolderPath + "\\dataStructure.json";
        int maxDatesCount = -1; // if negative value, then all dates will be converted.
        String[] testArgs = new String[]{
                "-type", "AIR-POLLUTION",
                "-input", inputFolderPath,
                "-output", outputFolderPath,
                "-inputDataStructurePath", inputDataStructurePath,
                "-maxDatesCount", String.valueOf(maxDatesCount)
        };

        Main.main(testArgs);
    }

    @Test
    void main_O_NO2() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirPollution\\O_NO2";
        String outputFolderPath = inputFolderPath + "\\output";
        String inputDataStructurePath = inputFolderPath + "\\dataStructure.json";
        int maxDatesCount = -1; // if negative value, then all dates will be converted.
        String[] testArgs = new String[]{
                "-type", "AIR-POLLUTION",
                "-input", inputFolderPath,
                "-output", outputFolderPath,
                "-inputDataStructurePath", inputDataStructurePath,
                "-maxDatesCount", String.valueOf(maxDatesCount)
        };

        Main.main(testArgs);
    }

    @Test
    void main_O_PM25() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirPollution\\O_PM25";
        String outputFolderPath = inputFolderPath + "\\output";
        String inputDataStructurePath = inputFolderPath + "\\dataStructure.json";
        int maxDatesCount = -1; // if negative value, then all dates will be converted.
        String[] testArgs = new String[]{
                "-type", "AIR-POLLUTION",
                "-input", inputFolderPath,
                "-output", outputFolderPath,
                "-inputDataStructurePath", inputDataStructurePath,
                "-maxDatesCount", String.valueOf(maxDatesCount)
        };

        Main.main(testArgs);
    }

    @Test
    void main_B_PM25() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirPollution\\B_PM25";
        String outputFolderPath = inputFolderPath + "\\output";
        String inputDataStructurePath = inputFolderPath + "\\dataStructure.json";
        int maxDatesCount = -1; // if negative value, then all dates will be converted.
        String[] testArgs = new String[]{
                "-type", "AIR-POLLUTION",
                "-input", inputFolderPath,
                "-output", outputFolderPath,
                "-inputDataStructurePath", inputDataStructurePath,
                "-maxDatesCount", String.valueOf(maxDatesCount)
        };

        Main.main(testArgs);
    }

    @Test
    void main_OD_H2S() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirPollution\\OD_H2S";
        String outputFolderPath = inputFolderPath + "\\output";
        String inputDataStructurePath = inputFolderPath + "\\dataStructure.json";
        int maxDatesCount = 720; // if negative value, then all dates will be converted.
        // 720 = 30 days * 24 hours.
        String[] testArgs = new String[]{
                "-type", "AIR-POLLUTION",
                "-input", inputFolderPath,
                "-output", outputFolderPath,
                "-inputDataStructurePath", inputDataStructurePath,
                "-maxDatesCount", String.valueOf(maxDatesCount)
        };

        Main.main(testArgs);
    }
}