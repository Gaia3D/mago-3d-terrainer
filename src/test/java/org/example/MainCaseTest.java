package org.example;

import com.gaia3d.Main;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.ParseException;


@Slf4j
class MainCaseTest {

    private final String MAIN_PATH = "D:\\data\\deia\\mock-nineeco-ftp\\datas\\815ba274-658a-4056-ae7f-bb474c816115\\BDI006\\";

    @Test
    void testHelp() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String[] testArgs = new String[]{
                "-help"
        };
        Main.main(testArgs);
    }

    @Test
    void testNoInput() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String outputFolderPath = MAIN_PATH + "result\\B_NO2\\24\\";
        String[] testArgs = new String[]{
                "-output", outputFolderPath,
        };

        Main.main(testArgs);
    }

    @Test
    void testNoOutput() throws ParseException, IOException, org.apache.commons.cli.ParseException {
        String inputFolderPath = MAIN_PATH + "output\\B_NO2\\index24.json";
        String outputFolderPath = MAIN_PATH + "result\\B_NO2\\24\\";
        String[] testArgs = new String[]{
                "-input", outputFolderPath,
        };

        Main.main(testArgs);
    }

}