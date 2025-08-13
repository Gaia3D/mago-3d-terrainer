package com.gaia3d.utils;

import com.gaia3d.chemicalAccidentDataConverter.DataSlice;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;

import java.io.*;
import java.util.List;
import java.util.Vector;

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

    public static void createAllFoldersIfNoExist(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.isDirectory()) {
            return;
        } else {
            if (!file.mkdirs()) {
                throw new RuntimeException("Failed to create folder: " + filePath);
            }
        }
    }

    public static void getFileNames(String folderPath, String extension, List<String> fileNames) {
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null) {
            //log.warn("No files in the folder: " + folderPath);
            System.out.println("No files in the folder: " + folderPath);
            return;
        }
        for (File file : listOfFiles) {
            if (file.isFile()) {
                String fileName = file.getName();
                if (fileName.endsWith(extension)) {
                    fileNames.add(fileName);
                }
            }
        }
    }

    public static void loadDataSlice(String dataSliceFilePath, DataSlice resultDataSlice) throws IOException {
        // read the dataSlice file.***
        // the dataSlice file is TXT format.***
        File file = new File(dataSliceFilePath);    //creates a new file instance
        FileReader fr = new FileReader(file);   //reads the file
        BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream

        String line;
        Boolean finished = false;
        Integer counter = 0;
        String delimiter = " ";
        Vector<Double> vecValues = new Vector<>();
        int columnsCount = 0;
        int rowsCount = 0;
        boolean bIsMatrix = true;

        Vector<String> resultSplittedStrings = new Vector<>();

        // read lines.***
        while (!finished) {
            line = br.readLine();
            if (line == null) {
                //finished = true;
                break;
            }

            counter += 1;

            resultSplittedStrings.clear();
            boolean skipEmptyStrings = true;
            StringModifier.splitString(line, delimiter, resultSplittedStrings, skipEmptyStrings);
            if (columnsCount == 0) {
                columnsCount = resultSplittedStrings.size();
            } else {
                int stringsCount = resultSplittedStrings.size();
                if (stringsCount != columnsCount) {
                    bIsMatrix = false;
                    break;
                }
            }

            // now, transform strings to double values.***
            for (int col = 0; col < columnsCount; col++) {
                String stringValue = resultSplittedStrings.get(col);
                Double doubleValue = Double.parseDouble(stringValue);
                if (doubleValue != null) {
                    vecValues.add(doubleValue);
                } else {
                    bIsMatrix = false;
                    break;
                }
            }
        }

        fr.close();
        br.close();

        rowsCount = counter;

        if (!bIsMatrix || columnsCount == 0 || rowsCount == 0) {
            return;
        }

        // now, find min max values needed for quantizing.***
        Vector2d minMaxValues = GeometryUtils.GetMinMaxValuesVectorDoubles(vecValues);

        String dataSliceFileName = StringModifier.getLastNameFromPath(dataSliceFilePath);
        resultDataSlice.fileName = dataSliceFileName;
        resultDataSlice.columnsCount = columnsCount;
        resultDataSlice.rowsCount = rowsCount;
        resultDataSlice.minValue = minMaxValues.x;
        resultDataSlice.maxValue = minMaxValues.y;
        resultDataSlice.values = new double[vecValues.size()];
        for (int i = 0; i < vecValues.size(); i++) {
            resultDataSlice.values[i] = vecValues.get(i);
        }
    }
}
