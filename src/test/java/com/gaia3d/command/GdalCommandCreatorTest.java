package com.gaia3d.command;

import com.gaia3d.reader.GaiaGdalTranslator;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

class GdalCommandCreatorTest {

    @Test
    void gdalTest() throws FileNotFoundException {
        String inputFolderPath = "D:\\QuantizedMesh_JavaProjects\\generalDatas\\TerrainElevData\\5m\\38816";
        String outputFolderPath = "D:\\QuantizedMesh_JavaProjects\\output";
        List<String> commandsList = new ArrayList();

        GaiaGdalTranslator gaiaGdalTranslator = new GaiaGdalTranslator();
        gaiaGdalTranslator.createGdalCommand_convertImgToGeoTiff(inputFolderPath, outputFolderPath, commandsList);

        // now save the commands to a file
        String commandsFilePath = "D:\\QuantizedMesh_JavaProjects\\commands.txt";
        PrintWriter out = new PrintWriter(commandsFilePath);

        for (String command : commandsList) {
            out.println(command);
        }
    }
}
