package com.gaia3d.airPollutionDataConverter;

import com.gaia3d.geometry.BoundingBox;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.TreeMap;

@NoArgsConstructor
public class DataContainer {
    public double centerGeoCoordLongitudeDegree = 0.0;
    public double centerGeoCoordLatitudeDegree = 0.0;
    public double centerGeoCoordAltitude = 0.0;

    public double width_km = 0.0;
    public double height_km = 0.0;

    public String sourceProj = "";
    public String targetProj = "";
    public ArrayList<DataLayer> dataLayers = new ArrayList<>();

    public ArrayList<String> datesArray = new ArrayList<>();

    public TreeMap<String, String> dateAndMosaicTexFileNames = new TreeMap<>();

    public int mosaicColumnsCount = 0;
    public int mosaicRowsCount = 0;

    public double totalMinValue = 0.0;
    public double totalMaxValue = 0.0;

    public int year = 0;
    public int month = 1; // 1 ~ 12
    public int day = 1; // 1 ~ 31
    public int hour = 0;

    public int minute = 0;
    public int second = 0;
    public int millisecond = 0;

    public String timeIntervalUnits = "";
    public double timeInterval = 0.0;
    public ArrayList<PngsBinaryBlockData> pngsBinDataArray = new ArrayList<>();
    public ArrayList<String> mosaicTexMetaDataFileNames = new ArrayList<>();

    public void addDataLayer(DataLayer dataLayer) {
        dataLayers.add(dataLayer);
    }

    public DataLayer getDataLayer(int index) {
        return dataLayers.get(index);
    }

    public int getDataLayersCount() {
        return dataLayers.size();
    }

    public DataLayer getLastDataLayer() {
        return dataLayers.get(dataLayers.size() - 1);
    }

    public void getGeoCoordBoundingBox(BoundingBox resultBBox) {
        int i = 0;
        for (DataLayer dataLayer : dataLayers) {
            if (i == 0) {
                resultBBox.copyFrom(dataLayer.geoCoordBBox);
            } else {
                resultBBox.addBox(dataLayer.geoCoordBBox);
            }

            i++;
        }
    }

    public void getTotalMinMaxValues(double[] resultMinMaxValues) {
        int i = 0;
        for (DataLayer dataLayer : dataLayers) {
            if (i == 0) {
                dataLayer.getMinMaxValues(resultMinMaxValues);
            } else {
                double[] minMaxValues = new double[2];
                dataLayer.getMinMaxValues(minMaxValues);
                if (minMaxValues[0] < resultMinMaxValues[0]) {
                    resultMinMaxValues[0] = minMaxValues[0];
                }
                if (minMaxValues[1] > resultMinMaxValues[1]) {
                    resultMinMaxValues[1] = minMaxValues[1];
                }
            }
            i++;
        }
    }
}
