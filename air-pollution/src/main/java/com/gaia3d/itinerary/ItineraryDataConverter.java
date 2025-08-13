package com.gaia3d.itinerary;

import java.io.IOException;

public class ItineraryDataConverter
{
    public ItineraryDataConverter()
    {

    }

    public void ConvertData_V2(String inputFolderPath, String outputFolderPath, String itineraryFileName) {
        // 1. Read itinerary file v2.
        // 2. Convert data.
        // 3. Write converted data.
        String itineraryFilePath = inputFolderPath + "\\" + itineraryFileName;
        ItineraryManagerV2 itineraryManagerV2 = new ItineraryManagerV2();
        try {
            itineraryManagerV2.loadItineraryFile(itineraryFilePath);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        // 2. Convert data.
        try
        {
            itineraryManagerV2.convertItineraryData();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // 3. Write converted data.
        try
        {
            itineraryManagerV2.writeItineraryFile(outputFolderPath);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


        int hola = 0;
    }

    public void ConvertData(String inputFolderPath, String outputFolderPath, String locationIndicesFileName, String itineraryFileName) {
        // 1. Read location indices file.
        // 2. Read itinerary file.
        // 3. Convert data.
        // 4. Write converted data.

        LocationIndicesManager locationIndicesManager = new LocationIndicesManager();
        String locationIndicesFilePath = inputFolderPath + "\\" + locationIndicesFileName;
        try {
            locationIndicesManager.loadLocationIndicesFile(locationIndicesFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // location indices are in 5186. Convert to 4326.
        try{
            locationIndicesManager.convertLocationIndicesTo4326();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        String itineraryFilePath = inputFolderPath + "\\" + itineraryFileName;
        ItineraryManager itineraryManager = new ItineraryManager();
        try {
            itineraryManager.loadItineraryFile(itineraryFilePath);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        // 3. Convert data.
        try
        {
            itineraryManager.convertItineraryData(locationIndicesManager);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // 4. Write converted data.
        //String outputItineraryFilePath = outputFolderPath + "\\" + itineraryFileName;
        try
        {
            itineraryManager.writeItineraryFile(outputFolderPath);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


        int hola = 0;
    }
}
