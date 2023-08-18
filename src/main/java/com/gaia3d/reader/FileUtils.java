package com.gaia3d.reader;

import java.io.File;

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
}
