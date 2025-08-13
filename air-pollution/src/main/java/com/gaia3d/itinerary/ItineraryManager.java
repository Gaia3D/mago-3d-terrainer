package com.gaia3d.itinerary;

import com.gaia3d.geometry.BoundingRectangle;
import com.gaia3d.globe.Globe;
import com.gaia3d.utils.StringModifier;
import org.geotools.referencing.CRS;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

public class ItineraryManager
{
    // itinerary file is *.csv file type.
    // itinerary file sample:
    // 구분,,A,B,C,D,E,F,G,H,I,J
    // 시간,,INDEX_ID,INDEX_ID,INDEX_ID,INDEX_ID,INDEX_ID,INDEX_ID,INDEX_ID,INDEX_ID,INDEX_ID,INDEX_ID
    // 11,00,GK189,GK189,GK186,GK189,GK189,GK189,GK189,GK189,GK189,GK189
    // 11,01,GK189,GK189,GK186,GK189,GK189,GK189,GK189,GK189,GK189,GK189
    // 11,02,GK189,GK189,GK186,GK189,GK189,GK189,GK189,GK189,GK189,GK189
    // 11,03,GK189,GK189,GK186,GK189,GK189,GK189,GK189,GK189,GK189,GK189
    // 11,04,GK189,GK189,GK186,GK189,GK189,GK189,GK189,GK189,GK189,GK189
    // 11,05,GK189,GK189,GK186,GK189,GK189,GK189,GK189,GK189,GK189,GK189
    // 11,06,GK189,GK189,GK186,GK189,GK189,GK189,GK189,GK189,GK189,GK189
    // ...

    HashMap<String, Itinerary> map_personName_itinerary = new HashMap<>();
    // each person has itineraryNodes array.***
    public ItineraryManager()
    {

    }

    public void loadItineraryFile(String itineraryFilePath) throws IOException {
        Charset charset = StandardCharsets.UTF_8;
        File file = new File(itineraryFilePath);    //creates a new file instance
        FileReader fr = new FileReader(file, charset);   //reads the file
        BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream

        String line;
        Boolean finished = false;
        Integer counter = 0;
        String delimiter = ",";

        int columnsCount = 0;
        int rowsCount = 0;

        // read lines.***
        // CSV file sample.***********************************************************************************
        // 구분,,A,B,C,D,E,F,G,H,I,J
        // 시간,,INDEX_ID,INDEX_ID,INDEX_ID,INDEX_ID,INDEX_ID,INDEX_ID,INDEX_ID,INDEX_ID,INDEX_ID,INDEX_ID
        // 11,00,GK189,GK189,GK186,GK189,GK189,GK189,GK189,GK189,GK189,GK189
        // 11,01,GK189,GK189,GK186,GK189,GK189,GK189,GK189,GK189,GK189,GK189
        // 11,02,GK189,GK189,GK186,GK189,GK189,GK189,GK189,GK189,GK189,GK189
        // 11,03,GK189,GK189,GK186,GK189,GK189,GK189,GK189,GK189,GK189,GK189
        // ...
        //----------------------------------------------------------------------------------------------------
        Vector<String> vecStrings = new Vector<>();
        ArrayList<String> vecTitles_1 = new ArrayList<>();
        ArrayList<String> vecTitles_2 = new ArrayList<>();
        boolean skipEmptyStrings = false;
        int lastHour = -1;
        int currDay = 10;
        int currMonth = 4; // month starts in 1.***
        int currYear = 2023;
        while (!finished) {
            line = br.readLine();
            if (line == null) {
                finished = true;
                break;
            }

            counter += 1;
            vecStrings.clear();
            StringModifier.splitString(line, delimiter, vecStrings, skipEmptyStrings);

            if (counter == 1) {
                // this is the 1rst line.***
                columnsCount = vecStrings.size();
                vecTitles_1.addAll(vecStrings); // person names.***
            }
            else if(counter == 2)
            {
                vecTitles_2.addAll(vecStrings); // titles.***
            }
            else
            {
                rowsCount += 1;
                int stringsCount = vecStrings.size();
                if (stringsCount != columnsCount) {
                    break;
                }

                int hour = Integer.parseInt(vecStrings.get(0));
                int minute = Integer.parseInt(vecStrings.get(1));

                if(hour < lastHour)
                {
                    // add 24 hours, so add 1 day.***
                    currDay += 1;

                    int feburaryDaysCount = 28;
                    if(currYear % 4 == 0)
                    {
                        feburaryDaysCount = 29;
                    }

                    if(currMonth == 2)
                    {
                        if(currDay > feburaryDaysCount)
                        {
                            currDay = 1;
                            currMonth += 1;
                        }
                    }
                    else if(currMonth == 4 || currMonth == 6 || currMonth == 9 || currMonth == 11)
                    {
                        if(currDay > 30)
                        {
                            currDay = 1;
                            currMonth += 1;
                        }
                    }
                    else
                    {
                        if(currDay > 31)
                        {
                            currDay = 1;
                            currMonth += 1;
                        }
                    }

                    if(currMonth > 12)  // month starts in 1.***
                    {
                        currMonth = 1; // month starts in 1.***
                        currYear += 1;
                    }
                }

                for(int i=2; i<stringsCount; i++)
                {
                    String person = vecTitles_1.get(i);
                    String indexId = vecStrings.get(i);

                    ItineraryNode itineraryNode = new ItineraryNode();
                    itineraryNode.year = currYear;
                    itineraryNode.month = currMonth;
                    itineraryNode.day = currDay;
                    itineraryNode.hour = hour;
                    itineraryNode.minute = minute;
                    itineraryNode.indexId = indexId;

                    if(map_personName_itinerary.containsKey(person) == false)
                    {
                        map_personName_itinerary.put(person, new Itinerary());
                    }

                    Itinerary itinerary = map_personName_itinerary.get(person);
                    itinerary.itineraryNodes.add(itineraryNode);
                }

                lastHour = hour;
            }
        }
        br.close();
        fr.close();
    }

    public void convertItineraryData(LocationIndicesManager locationIndicesManager)
    {
        int personsCount = map_personName_itinerary.size();
        int personIndex = 0;
        for(String personName : map_personName_itinerary.keySet())
        {
            personIndex += 1;
            System.out.println("converting " + personIndex + "/" + personsCount + " : " + personName);
            Itinerary itinerary = map_personName_itinerary.get(personName);
            int itineraryNodesCount = itinerary.itineraryNodes.size();
            for(int i=0; i<itineraryNodesCount; i++)
            {
                ItineraryNode itineraryNode = itinerary.itineraryNodes.get(i);
                LocationIndex locationIndex = locationIndicesManager.locationIndices.get(itineraryNode.indexId);

                itineraryNode.latitudeDeg = locationIndex.latitudeDeg;
                itineraryNode.longitudeDeg = locationIndex.longitudeDeg;
            }
        }

        // now find the center geographic coordinates for each person & calculate itineraryNodes in local coords.***
        for(String personName : map_personName_itinerary.keySet())
        {
            personIndex += 1;
            System.out.println("converting " + personIndex + "/" + personsCount + " : " + personName);
            Itinerary itinerary = map_personName_itinerary.get(personName);
            BoundingRectangle geoCoordsBoundingRectangle = new BoundingRectangle();
            int itineraryNodesCount = itinerary.itineraryNodes.size();
            for(int i=0; i<itineraryNodesCount; i++)
            {
                ItineraryNode itineraryNode = itinerary.itineraryNodes.get(i);
                if(i == 0)
                {
                    geoCoordsBoundingRectangle.init(itineraryNode.longitudeDeg, itineraryNode.latitudeDeg);
                }
                else
                {
                    geoCoordsBoundingRectangle.addPoint(itineraryNode.longitudeDeg, itineraryNode.latitudeDeg);
                }
            }

            Vector2d centerGeoCoord = geoCoordsBoundingRectangle.GetCenterPosition();
            itinerary.centerGeoCoords.set(centerGeoCoord.x, centerGeoCoord.y, 0.0);
            Vector3d posWC = Globe.GeographicToCartesianWGS84(Math.toRadians(centerGeoCoord.x), Math.toRadians(centerGeoCoord.y), 0.0);

            Matrix4d tMat = Globe.TransformMatrixAtCartesianPointWgs84(posWC.x, posWC.y, posWC.z);
            Matrix4d tMatInv = new Matrix4d();
            tMat.invert(tMatInv);
            Vector3d nodePosWC;
            Vector4d nodePosLC = new Vector4d(0.0, 0.0, 0.0, 1.0);
            for(int i=0; i<itineraryNodesCount; i++)
            {
                ItineraryNode itineraryNode = itinerary.itineraryNodes.get(i);
                nodePosWC = Globe.GeographicToCartesianWGS84(Math.toRadians(itineraryNode.longitudeDeg), Math.toRadians(itineraryNode.latitudeDeg), 0.0);
                nodePosLC.set(nodePosWC.x, nodePosWC.y, nodePosWC.z, 1.0);
                tMatInv.transform(nodePosLC, nodePosLC);

                Vector3d posLC = new Vector3d(nodePosLC.x, nodePosLC.y, nodePosLC.z);
                itinerary.positionsLC.add(posLC);
                int hola = 0;
            }

            int hola = 0;
        }

        int hola = 0;
    }

    public void writeItineraryFile(String outputItineraryFolderPath) throws IOException {
        // save each personItinerary in a file.***
        for(String personName : map_personName_itinerary.keySet())
        {
            Itinerary itinerary = map_personName_itinerary.get(personName);
            String outputItineraryFilePath = outputItineraryFolderPath + "\\" + personName + ".json";

            StringModifier.createAllFoldersIfNoExist(outputItineraryFolderPath);
            itinerary.saveJsonFile(outputItineraryFilePath);
        }
    }
}
