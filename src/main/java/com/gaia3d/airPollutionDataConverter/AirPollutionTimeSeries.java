package com.gaia3d.airPollutionDataConverter;

import java.util.TreeMap;

public class AirPollutionTimeSeries
{
    TreeMap<String, AirPollutionVolume> timeSeriesData = new TreeMap<String, AirPollutionVolume>();

    public AirPollutionVolume getOrNewAirPollutionVolume(String date)
    {
        AirPollutionVolume volume = timeSeriesData.get(date);
        if (volume == null)
        {
            volume = new AirPollutionVolume();
            timeSeriesData.put(date, volume);
        }
        return volume;
    }
}
