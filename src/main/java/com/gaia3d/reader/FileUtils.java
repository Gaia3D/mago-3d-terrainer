package com.gaia3d.reader;

import java.io.File;
import java.util.ArrayList;

public class FileUtils {

    public static boolean isFileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    public static boolean createAllFoldersIfNoExist(String filePath) {
        File file = new File(filePath);
        return file.mkdirs();
    }

    public static String removeFileNameFromPath(String filePath) {
        File file = new File(filePath);
        return file.getParent();
    }

    public static boolean deleteFileIfExists(String filePath) {
        File file = new File(filePath);
        return file.delete();
    }

    public static void getFileNames(String folderPath, String extension, ArrayList<String> fileNames)
    {
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();

        assert listOfFiles != null;
        for (File file : listOfFiles) {
            if (file.isFile()) {
                String fileName = file.getName();
                if (fileName.endsWith(extension)) {
                    fileNames.add(fileName);
                }
            }
        }
    }

    public static void getFolderNames(String folderPath, ArrayList<String> folderNames)
    {
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();

        assert listOfFiles != null;
        for (File file : listOfFiles) {
            if (file.isDirectory()) {
                String folderName = file.getName();
                folderNames.add(folderName);
            }
        }
    }
}
