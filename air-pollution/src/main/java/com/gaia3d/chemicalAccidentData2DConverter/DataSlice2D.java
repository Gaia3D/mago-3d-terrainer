package com.gaia3d.chemicalAccidentData2DConverter;

import com.gaia3d.utils.FileUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Setter
@Getter

public class DataSlice2D {
    private int columnsCount = 0;
    private int rowsCount = 0;

    private String fileName = "";
    private String imageFileName = ""; // used only in chemical accident 2D.***
    private double minValue = 0.0;
    private double maxValue = 0.0;

    private Map<String, DataGrid2D> mapGridIdGridData = new HashMap<String, DataGrid2D>();

    private String[] values = null;

    public DataGrid2D newDataGrid2D(String gridId) {
        DataGrid2D dataGrid2D = new DataGrid2D();
        dataGrid2D.setGridId(gridId);
        mapGridIdGridData.put(gridId, dataGrid2D);
        return dataGrid2D;
    }

    public void makeValuesArray() {
        values = new String[rowsCount * columnsCount];
        for (int i = 0; i < rowsCount; i++) {
            for (int j = 0; j < columnsCount; j++) {
                int index = i * columnsCount + j;
                String gridIdString = index + 1 + "";
                if (mapGridIdGridData.containsKey(gridIdString)) {
                    DataGrid2D dataGrid2D = mapGridIdGridData.get(gridIdString);
                    //values[index] = dataGrid2D.getConcentration(); // old. concentration.***
                    //values[index] = dataGrid2D.getAcu_assment_cd(); // new. acu_assment_cd.***
                    values[index] = dataGrid2D.getVictim_count(); // new. acu_assment_cd.***
                } else {
                    // error.***
                    log.error("DataGrid2D not found. gridId = " + gridIdString);
                }
            }
        }
    }


    public void writeMatrixTxtFile(String outputFilePath) {
        // In the matrixTxt file the separator is a space character.***
        // 1rst, make the values array.***
        makeValuesArray();

        try {
            int lastSeparatorIndex = outputFilePath.lastIndexOf("\\");
            String folderPath = outputFilePath.substring(0, lastSeparatorIndex);
            FileUtils.createAllFoldersIfNoExist(folderPath);
            File file = new File(outputFilePath);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();

            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            for (int i = 0; i < rowsCount; i++) {
                for (int j = 0; j < columnsCount; j++) {
                    int index = i * columnsCount + j;
                    bufferedWriter.write(values[index] + " ");
                }
                bufferedWriter.write("\n");
            }

            bufferedWriter.close();
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
