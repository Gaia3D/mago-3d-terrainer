package com.gaia3d.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

@Slf4j
public class FileUtils {
    public static FileInputStream loadFileInputStream(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }

        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
        } catch (Exception e) {
            log.error("", e);
        }

        return fileInputStream;
    }

    public static void loadAndReSaveFile(String inputFilePath, String outputFilePath) {
        try (FileInputStream inputStream = new FileInputStream(inputFilePath); FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            log.error("", e);
        }
    }
}
