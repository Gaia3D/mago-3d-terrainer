package com.gaia3d.chemicalAccidentDataConverter;

import java.util.ArrayList;

public class DataLayer
{
    public String folderName;
    public double minAltitude = 0.0;
    public double maxAltitude = 0.0;

    public ArrayList<String> timeSlicesFileNames = new ArrayList<>();

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
}
