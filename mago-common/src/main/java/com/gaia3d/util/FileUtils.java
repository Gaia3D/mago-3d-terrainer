package com.gaia3d.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ALL")
@Slf4j
public class FileUtils {

    public static boolean isFileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    public static void createAllFoldersIfNoExist(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.isDirectory()) {
                return;
            } else {
                // 如果路径已存在且是文件，直接返回，不尝试创建同名目录
                return;
            }
        }
        
        if (!file.mkdirs()) {
            throw new RuntimeException("Failed to create folder: " + filePath);
        }
    }

    public static String removeFileNameFromPath(String filePath) {
        File file = new File(filePath);
        return file.getParent();
    }

    public static void getFileNames(String folderPath, String extension, List<String> fileNames) {
        File file = new File(folderPath);
        if (!file.exists()) {
            return;
        }
        
        if (file.isFile()) {
            if (file.getName().toLowerCase().endsWith(extension.toLowerCase())) {
                fileNames.add(file.getName()); // 仅返回文件名，保持兼容性
            }
            return;
        }

        File[] listOfFiles = file.listFiles();
        if (listOfFiles == null) {
            log.warn("[WARN] No files in the folder: " + folderPath);
            return;
        }
        for (File f : listOfFiles) {
            if (f.isFile()) {
                String fileName = f.getName();
                if (fileName.toLowerCase().endsWith(extension.toLowerCase())) {
                    fileNames.add(fileName); // 仅返回文件名
                }
            }
        }
    }

    public static void getFolderNames(String folderPath, List<String> folderNames) {
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null) {
            log.warn("[WARN] No files in the folder: " + folderPath);
            return;
        }
        for (File file : listOfFiles) {
            if (file.isDirectory()) {
                String folderName = file.getName();
                folderNames.add(folderName);
            }
        }
    }

    public static void getFilePathsByExtension(String folderPath, String extension, List<String> fileNames, boolean isRecursive) {
        File baseFile = new File(folderPath);
        if (!baseFile.exists()) return;

        if (baseFile.isFile()) {
            if (baseFile.getName().toLowerCase().endsWith(extension.toLowerCase())) {
                fileNames.add(baseFile.getAbsolutePath());
            }
            return;
        }

        List<String> currfileNames = new ArrayList<>();
        FileUtils.getFileNames(folderPath, extension, currfileNames);
        for (String fileName : currfileNames) {
            fileNames.add(new File(baseFile, fileName).getAbsolutePath());
        }

        if (isRecursive) {
            List<String> folderNames = new ArrayList<>();
            FileUtils.getFolderNames(folderPath, folderNames);
            for (String folderName : folderNames) {
                String subFolderPath = new File(baseFile, folderName).getAbsolutePath();
                FileUtils.getFilePathsByExtension(subFolderPath, extension, fileNames, isRecursive);
            }
        }
    }

    public static void deleteDirectory(File depthTempFolder) {
        if (depthTempFolder.isDirectory()) {
            File[] children = depthTempFolder.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        } else if (depthTempFolder.isFile()) {
            if (!depthTempFolder.delete()) {
                log.warn("Failed to delete file: " + depthTempFolder.getAbsolutePath());
            }
        }
        if (!depthTempFolder.delete()) {
            log.warn("Failed to delete file or folder: " + depthTempFolder.getAbsolutePath());
        }
    }
}
