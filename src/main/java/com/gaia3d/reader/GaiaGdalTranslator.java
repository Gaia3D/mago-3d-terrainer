package com.gaia3d.reader;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;

public class GaiaGdalTranslator
{

    public void createGdalCommand_convertImgToGeoTiff(String inputFolderPath, String outputFolderPath, ArrayList<String>commandsList)
    {
        // find all files *.img.***
        // for each file, create a gdal command.***
        ArrayList<String> fileNames = new ArrayList();
        FileUtils.getFileNames(inputFolderPath, ".img", fileNames);

        for (int i = 0; i < fileNames.size(); i++)
        {
            String fileName = fileNames.get(i);
            String rawFileName = FilenameUtils.removeExtension(fileName); // remove extension. (".img"
            String inputFilePath = inputFolderPath + "\\" + fileName;
            String outputFilePath = outputFolderPath + "\\" + rawFileName + ".tif";

            String command = "gdal_translate -of GTiff " + inputFilePath + " " + outputFilePath;
            commandsList.add(command);
        }

        // now, check if exist folders.***
        ArrayList<String> folderNames = new ArrayList();
        FileUtils.getFolderNames(inputFolderPath, folderNames);

        for (int i = 0; i < folderNames.size(); i++)
        {
            String folderName = folderNames.get(i);
            String inputFolderPath2 = inputFolderPath + "/" + folderName;
            String outputFolderPath2 = outputFolderPath + "/" + folderName;

            FileUtils.createAllFoldersIfNoExist(outputFolderPath2);

            createGdalCommand_convertImgToGeoTiff(inputFolderPath2, outputFolderPath2, commandsList);
        }
    }
}
