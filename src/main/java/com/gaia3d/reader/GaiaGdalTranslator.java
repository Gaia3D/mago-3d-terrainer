package com.gaia3d.reader;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GaiaGdalTranslator {

    public void createGdalCommand_convertImgToGeoTiff(String inputFolderPath, String outputFolderPath, List<String> commandsList) {
        // find all files *.img
        // for each file, create a gdal command
        List<String> fileNames = new ArrayList<>();
        FileUtils.getFileNames(inputFolderPath, ".img", fileNames);

        for (String fileName : fileNames) {
            String rawFileName = FilenameUtils.removeExtension(fileName); // remove extension. (".img"
            String inputFilePath = inputFolderPath + File.separator + fileName;
            String outputFilePath = outputFolderPath + File.separator + rawFileName + ".tif";

            String command = "gdal_translate -of GTiff " + inputFilePath + " " + outputFilePath;
            commandsList.add(command);
        }

        // now, check if exist folders
        List<String> folderNames = new ArrayList<>();
        FileUtils.getFolderNames(inputFolderPath, folderNames);

        for (String folderName : folderNames) {
            String inputFolderPath2 = inputFolderPath + "/" + folderName;
            String outputFolderPath2 = outputFolderPath + "/" + folderName;

            FileUtils.createAllFoldersIfNoExist(outputFolderPath2);

            createGdalCommand_convertImgToGeoTiff(inputFolderPath2, outputFolderPath2, commandsList);
        }
    }
}
