package com.gaia3d.converter.chemicalAccidentDataConverter;

import com.gaia3d.basic.legend.LegendColors;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@NoArgsConstructor
public class DataContainer {
    public String date = "";

    public int year = 0;
    public int month = 0;
    public int day = 0;
    public int hour = 0;
    public int minute = 0;
    public int second = 0;
    public int millisecond = 0;
    public double geoCoordCenterLongitudeDeg = 0.0;
    public double geoCoordCenterLatitudeDeg = 0.0;
    public double geoCoordCenterAltitude = 0.0;

    public double width_km = 0.0;
    public double height_km = 0.0;

    public String timeIntervalUnits = "";
    public double timeInterval = 0.0;

    public double totalMinValue = 0.0;
    public double totalMaxValue = 0.0;
    public LegendColors legendColors = new LegendColors();
    public List<Double> marchingCubesIsoValues = new ArrayList<>();

    public ArrayList<DataLayer> dataLayers = new ArrayList<>();

    public ArrayList<String> mosaicTexMetaDataFileNames = new ArrayList<>();
    public List<String> glbMetaDataFileNames = new ArrayList<>();

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

    public void addMosaicTexMetaDataFileName(String mosaicTexMetaDataFileName) {
        mosaicTexMetaDataFileNames.add(mosaicTexMetaDataFileName);
    }

    public String getYYYYMMDDHHmmss(int idx) {
        double timeInterval = this.timeInterval;
        if (timeIntervalUnits.equals("hour")) {
            return getYYYYMMDDHHmmss(year, month, day, hour + idx * timeInterval, 0, 0);
        } else if (timeIntervalUnits.equals("minute")) {
            return getYYYYMMDDHHmmss(year, month, day, hour, minute + idx * timeInterval, 0);
        } else if (timeIntervalUnits.equals("second")) {
            return getYYYYMMDDHHmmss(year, month, day, hour, minute, second + idx * timeInterval);
        } else if (timeIntervalUnits.equals("days")) {
            double totalSeconds = timeInterval * 24 * 60 * 60; // Convert days to seconds
            double totalMinutes = totalSeconds / 60;
            double totalHours = totalMinutes / 60;
            double newHour = hour + (int) totalHours;
            double newMinute = minute + (int) (totalMinutes % 60);
            double newSecond = second + (int) (totalSeconds % 60);
            return getYYYYMMDDHHmmss(year, month, day, newHour, newMinute, newSecond);
        }
        return null;
    }

    String getYYYYMMDDHHmmss(double year, double month, double day, double i, double i1, double i2) {
        return String.format("%04d%02d%02d%02d%02d%02d", (int) year, (int) month, (int) day, (int) i, (int) i1, (int) i2);
    }

    public Collection<String> getGlbMetaDataFileNames() {
        return glbMetaDataFileNames;
    }
}
