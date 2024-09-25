package org.example;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.ParseException;


@Slf4j
class MainTest
{
    @Test
    void main_B_NO2_20240919() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirpollution_20240919\\B_NO2";
        String outputFolderPath = inputFolderPath + "\\output";
        String[] testArgs = new String[]{
                "-input", inputFolderPath,
                "-output", outputFolderPath
        };

        Main.main(testArgs);
    }

    @Test
    void main_B_PM10_20240919() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirpollution_20240919\\B_PM10";
        String outputFolderPath = inputFolderPath + "\\output";
        String[] testArgs = new String[]{
                "-input", inputFolderPath,
                "-output", outputFolderPath
        };

        Main.main(testArgs);
    }

    @Test
    void main_B_PM25_20240919() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirpollution_20240919\\B_PM25";
        String outputFolderPath = inputFolderPath + "\\output";
        String[] testArgs = new String[]{
                "-input", inputFolderPath,
                "-output", outputFolderPath
        };

        Main.main(testArgs);
    }

    @Test
    void main_O_NO2_20240919() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirpollution_20240919\\O_NO2";
        String outputFolderPath = inputFolderPath + "\\output";
        String[] testArgs = new String[]{
                "-input", inputFolderPath,
                "-output", outputFolderPath
        };

        Main.main(testArgs);
    }

    @Test
    void main_O_PM10_20240919() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirpollution_20240919\\O_PM10";
        String outputFolderPath = inputFolderPath + "\\output";
        String[] testArgs = new String[]{
                "-input", inputFolderPath,
                "-output", outputFolderPath
        };

        Main.main(testArgs);
    }

    @Test
    void main_O_PM25_20240919() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirpollution_20240919\\O_PM25";
        String outputFolderPath = inputFolderPath + "\\output";
        String[] testArgs = new String[]{
                "-input", inputFolderPath,
                "-output", outputFolderPath
        };

        Main.main(testArgs);
    }

    @Test
    void main_OD_H2S_20240919() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirpollution_20240919\\OD_H2S";
        String outputFolderPath = inputFolderPath + "\\output";
        String[] testArgs = new String[]{
                "-input", inputFolderPath,
                "-output", outputFolderPath
        };

        Main.main(testArgs);
    }

    @Test
    void main_OD_NH3_20240919() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirpollution_20240919\\OD_NH3";
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

    @Test
    void main_OD_OU_20240919() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirpollution_20240919\\OD_OU";
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

    // Scale test.***
    @Test
    void main_B_NO2_20240919_scaled() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = "D:\\data\\simulation-data\\AIRPOLLUTION\\newAirpollution_20240919\\B_NO2";
        String outputFolderPath = inputFolderPath + "\\output_scaled";
        String inputDataStructurePath = inputFolderPath + "\\dataStructure.json";
        int maxDatesCount = -1; // if negative value, then all dates will be converted.
        // 720 = 30 days * 24 hours.
        String[] testArgs = new String[]{
                "-type", "AIR-POLLUTION",
                "-input", inputFolderPath,
                "-output", outputFolderPath,
                "-inputDataStructurePath", inputDataStructurePath,
                "-maxDatesCount", String.valueOf(maxDatesCount),
                "-scale", "0.5"
        };

        Main.main(testArgs);
    }


}