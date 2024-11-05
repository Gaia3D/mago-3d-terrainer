package com.gaia3d.airPollutionDataConverter;

import java.util.TreeMap;

public class AirPollutionRowData {
    // TreeMap <X, AirPollutionPixelData>. X = column.
    public TreeMap<Double, AirPollutionPixelData> rowData = new TreeMap<>();
    public double[] rowValues = null;

    public void addPixelData(AirPollutionPixelData pixelData) {
        rowData.put(pixelData.X, pixelData);
    }

    public void getMinMaxValues(double[] resultMinMaxValues) {
        resultMinMaxValues[0] = Double.MAX_VALUE;
        resultMinMaxValues[1] = Double.MIN_VALUE;
        for (AirPollutionPixelData pixelData : rowData.values()) {
            if (pixelData.averageConcentration < resultMinMaxValues[0]) {
                resultMinMaxValues[0] = (int) pixelData.averageConcentration;
            }
            if (pixelData.averageConcentration > resultMinMaxValues[1]) {
                resultMinMaxValues[1] = (int) pixelData.averageConcentration;
            }
        }
    }

    public double getValue(int column) {
        if (this.rowValues == null) {
            this.rowValues = new double[this.rowData.size()];
            int index = 0;
            for (AirPollutionPixelData pixelData : rowData.values()) {
                this.rowValues[index] = pixelData.averageConcentration;
                index++;
            }
        }
        return this.rowValues[column];
    }
}
