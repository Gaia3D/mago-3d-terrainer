package com.gaia3d.converter.airPollutionDataConverter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.TreeMap;

@Slf4j
@Getter
@Setter
public class AirPollutionSliceData {
    public String fileName = "";
    public double minValue = 0.0;
    public double maxValue = 0.0;
    public double minAltitude = 0.0;
    public double maxAltitude = 0.0;
    // TreeMap <Y, AirPollutionRowData>. Y = row.
    TreeMap<Double, AirPollutionRowData> matrixData = new TreeMap<Double, AirPollutionRowData>();
    ArrayList<AirPollutionRowData> matrixDataArray = null;

    public void addPixelData(AirPollutionPixelData pixelData) {
        AirPollutionRowData rowData = matrixData.get(pixelData.Y);
        if (rowData == null) {
            rowData = new AirPollutionRowData();
            matrixData.put(pixelData.Y, rowData);
        }
        rowData.addPixelData(pixelData);
    }

    public int getRowsCount() {
        return matrixData.size();
    }

    public int getColumnsCount() {
        return matrixData.get(matrixData.firstKey()).rowData.size();
    }

    public void getMinMaxValues(double[] resultMinMaxValues) {
        resultMinMaxValues[0] = Double.MAX_VALUE;
        resultMinMaxValues[1] = Double.MIN_VALUE;
        for (AirPollutionRowData rowData : matrixData.values()) {
            double[] minMaxValues = new double[2];
            rowData.getMinMaxValues(minMaxValues);
            if (minMaxValues[0] < resultMinMaxValues[0]) {
                resultMinMaxValues[0] = minMaxValues[0];
            }
            if (minMaxValues[1] > resultMinMaxValues[1]) {
                resultMinMaxValues[1] = minMaxValues[1];
            }
        }
    }

    public double getValue(int column, int row) {
        if (this.matrixDataArray == null) {
            this.matrixDataArray = new ArrayList<>(this.matrixData.values());
        }
        AirPollutionRowData rowData = this.matrixDataArray.get(row);
        return rowData.getValue(column);
    }

    public void loadTempFile(String filePath) {
        // load a binary file.
        try {
            this.matrixData = new TreeMap<Double, AirPollutionRowData>();
            DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(filePath)));

            int rowsCount = dataInputStream.readInt();
            int columnsCount = dataInputStream.readInt();
            int valuesCount = rowsCount * columnsCount;

            for (int row = 0; row < rowsCount; row++) {
                AirPollutionRowData rowData = new AirPollutionRowData();
                for (int column = 0; column < columnsCount; column++) {
                    AirPollutionPixelData pixelData = new AirPollutionPixelData();
                    pixelData.averageConcentration = dataInputStream.readDouble();
                    rowData.rowData.put((double) column, pixelData);
                }
                this.matrixData.put((double) row, rowData);
            }
            dataInputStream.close();
        } catch (IOException e) {
            log.error("", e);
        }
    }

    public void saveTempFile(String filePath) {
        // save a binary file.
        if (this.matrixDataArray == null) {
            this.matrixDataArray = new ArrayList<>(this.matrixData.values());
        }
        int rowsCount = this.matrixDataArray.size();
        int columnsCount = this.matrixDataArray.get(0).rowData.size();
        int valuesCount = rowsCount * columnsCount;

        try {
            DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)));
            dataOutputStream.writeInt(rowsCount);
            dataOutputStream.writeInt(columnsCount);
            for (int row = 0; row < rowsCount; row++) {
                AirPollutionRowData rowData = this.matrixDataArray.get(row);
                for (int column = 0; column < columnsCount; column++) {
                    dataOutputStream.writeDouble(rowData.getValue(column));
                }
            }
            dataOutputStream.close();
        } catch (IOException e) {
            log.error("", e);
        }
    }
}
