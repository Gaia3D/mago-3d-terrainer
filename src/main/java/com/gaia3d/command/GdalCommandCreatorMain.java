package com.gaia3d.command;

import com.gaia3d.reader.GaiaGdalTranslator;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class GdalCommandCreatorMain
{
    public static void main(String[] args) throws FileNotFoundException {
        String inputFolderPath = "D:\\QuantizedMesh_JavaProjects\\generalDatas\\TerrainElevData\\5m\\38816";
        String outputFolderPath = "D:\\QuantizedMesh_JavaProjects\\output";
        ArrayList<String> commandsList = new ArrayList();

        GaiaGdalTranslator gaiaGdalTranslator = new GaiaGdalTranslator();
        gaiaGdalTranslator.createGdalCommand_convertImgToGeoTiff(inputFolderPath, outputFolderPath, commandsList);

        // now save the commands to a file.***
        String commandsFilePath = "D:\\QuantizedMesh_JavaProjects\\commands.txt";
        PrintWriter out = new PrintWriter(commandsFilePath);

        for (int i = 0; i < commandsList.size(); i++)
        {
            String command = commandsList.get(i);
            out.println(command);
        }

        int hola = 0;
    }
}
