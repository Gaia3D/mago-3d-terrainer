package com.gaia3d.airPollutionDataConverter;

import com.gaia3d.geometry.BoundingBox;

import java.util.ArrayList;
import java.util.TreeMap;

public class DataLayer
{
    public String filePath;
    public double altitude = 0.0;

    public int columnsCount = 0; // textureWidth.***
    public int rowsCount = 0; // textureHeight.***

    public BoundingBox geoCoordBBox = new BoundingBox(); // minLongitude, minLatitude, minAltitude, maxLongitude, maxLatitude, maxAltitude

    public ArrayList<String> timeSlicesFileNames = new ArrayList<>();
    public TreeMap<Integer, String> tempFilesMap;

    public double minPollutionValue = 0.0;
    public double maxPollutionValue = 0.0;

    public DataLayer()
    {

    }

    public void addTimeSliceFileName(String timeSliceFileName)
    {
        timeSlicesFileNames.add(timeSliceFileName);
    }

    public String getTimeSliceFileName(int index)
    {
        return timeSlicesFileNames.get(index);
    }

    public int getTimeSlicesCount()
    {
        return timeSlicesFileNames.size();
    }

    public void getMinMaxValues(double[] minMaxValues)
    {
        minMaxValues[0] = this.minPollutionValue;
        minMaxValues[1] = this.maxPollutionValue;
    }
}
