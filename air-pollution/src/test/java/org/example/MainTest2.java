package org.example;

import com.gaia3d.Main;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.ParseException;


@Slf4j
class MainTest2 {
    private final String mainPath = "G:\\datas\\db37b5ec-0778-4a17-82c3-448cf4e25664\\BDI001\\";

    @Test
    void main_B_NO2_20240919_24() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = mainPath + "output\\B_NO2\\index24.json";
        String outputFolderPath = mainPath + "result\\B_NO2\\24\\";
        String logFilePath = mainPath + "result\\B_NO2\\24\\log.txt";
        String[] testArgs = new String[]{"-input", inputFolderPath, "-output", outputFolderPath, "-log", logFilePath};

        Main.main(testArgs);
    }

    @Test
    void main_B_NO2_20240919_01() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = mainPath + "output\\B_NO2\\index01.json";
        String outputFolderPath = mainPath + "result\\temp\\";
        String[] testArgs = new String[]{"-input", inputFolderPath, "-output", outputFolderPath};

        Main.main(testArgs);
    }
}