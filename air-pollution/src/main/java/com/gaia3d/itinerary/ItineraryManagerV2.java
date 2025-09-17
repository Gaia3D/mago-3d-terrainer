package com.gaia3d.itinerary;

import com.gaia3d.geometry.BoundingRectangle;
import com.gaia3d.globe.Globe;
import com.gaia3d.utils.StringModifier;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

@Slf4j
@NoArgsConstructor
public class ItineraryManagerV2 {
    // itinerary file is *.csv file type.
    // itinerary file sample:
    //    개인아이디,시간,UTMK_X,UTMK_Y,INDEX_ID,GENDER,AGE,
    //    USER001,202402061100,923285.825718,1877331.084042,다바232772,M,30-39,
    //    USER001,202402061110,923285.825718,1877331.084042,다바232772,M,30-39,
    //    USER001,202402061120,923285.825718,1877331.084042,다바232772,M,30-39,
    //    USER001,202402061130,923285.825718,1877331.084042,다바232772,M,30-39,
    //    USER002,202402061140,923285.825718,1877331.084042,다바232772,M,30-39,
    //    USER002,202402061150,923285.825718,1877331.084042,다바232772,M,30-39,
    //    USER002,202402061200,923285.825718,1877331.084042,다바232772,M,30-39,
    //    USER002,202402061210,923285.825718,1877331.084042,다바232772,M,30-39,
    //    USER003,202402061220,923285.825718,1877331.084042,다바232772,M,30-39,
    //    USER003,202402061230,923285.825718,1877331.084042,다바232772,M,30-39,
    //    USER003,202402061240,923285.825718,1877331.084042,다바232772,M,30-39,
    // ...

    HashMap<String, Itinerary> map_personName_itinerary = new HashMap<>();

    private ProjCoordinate transformToWGS84(CoordinateReferenceSystem source, ProjCoordinate coordinate) {
        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem wgs84 = factory.createFromParameters("WGS84", "+proj=longlat +datum=WGS84 +no_defs");
        BasicCoordinateTransform transformer = new BasicCoordinateTransform(source, wgs84);
        ProjCoordinate result = new ProjCoordinate();
        transformer.transform(coordinate, result);
        return result;
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

        // itinerary file is *.csv file type.
        // itinerary file sample:
        //    개인아이디,시간,UTMK_X,UTMK_Y,INDEX_ID,GENDER,AGE,
        //    USER001,202402061100,923285.825718,1877331.084042,다바232772,M,30-39,
        //    USER001,202402061110,923285.825718,1877331.084042,다바232772,M,30-39,
        //    USER001,202402061120,923285.825718,1877331.084042,다바232772,M,30-39,
        //    USER001,202402061130,923285.825718,1877331.084042,다바232772,M,30-39,

        // read lines
        List<String> vecTitles = new ArrayList<>(); // for the 1rst line
        Vector<String> vecStrings = new Vector<>(); // for the rest of lines

        boolean skipEmptyStrings = false;

        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem inputCrs = null;
        String proj = "+proj=tmerc +lat_0=38 +lon_0=127.5 +k=0.9996 +x_0=1000000 +y_0=2000000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43";
        if (proj != null && !proj.isEmpty()) {
            inputCrs = factory.createFromParameters("CUSTOM", proj);
        }

        double[] srcPts = new double[2];

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
                // this is the 1rst line
                columnsCount = vecStrings.size();
                vecTitles.addAll(vecStrings);
            } else {
                // this is a data line
                rowsCount += 1;

                int stringsCount = vecStrings.size();
                if (stringsCount != columnsCount) {
                    // error
                    log.info("Error. Line= " + counter);
                    break;
                }

                String personName = vecStrings.get(0);
                String time = vecStrings.get(1); // yyyyMMddHHmm
                int year = Integer.parseInt(time.substring(0, 4));
                int month = Integer.parseInt(time.substring(4, 6));
                int day = Integer.parseInt(time.substring(6, 8));
                int hour = Integer.parseInt(time.substring(8, 10));
                int minute = Integer.parseInt(time.substring(10, 12));

                double centroidX = Double.parseDouble(vecStrings.get(2));
                double centroidY = Double.parseDouble(vecStrings.get(3));

                srcPts[0] = centroidX;
                srcPts[1] = centroidY;

                ProjCoordinate projCoordinate = new ProjCoordinate(srcPts[0], srcPts[1], 0.0);
                ProjCoordinate result = transformToWGS84(inputCrs, projCoordinate); // result[0] = longitude, result[1] = latitude

                String indexId = vecStrings.get(4);

                // check if the personName is already in the map
                Itinerary itinerary = map_personName_itinerary.get(personName);
                if (itinerary == null) {
                    itinerary = new Itinerary();
                    map_personName_itinerary.put(personName, itinerary);
                }

                ItineraryNode itineraryNode = new ItineraryNode();
                itineraryNode.year = year;
                itineraryNode.month = month;
                itineraryNode.day = day;
                itineraryNode.hour = hour;
                itineraryNode.minute = minute;
                itineraryNode.longitudeDeg = result.x;
                itineraryNode.latitudeDeg = result.y;
                itineraryNode.indexId = indexId;

                itinerary.itineraryNodes.add(itineraryNode);

            }

            counter += 1;
        }

        int hola = 0;

    }

    public void convertItineraryData() {
        // find the center geographic coordinates for each person & calculate itineraryNodes in local coords
        int personsCount = map_personName_itinerary.size();
        int personIndex = 0;
        for (String personName : map_personName_itinerary.keySet()) {
            personIndex += 1;
            log.info("converting " + personIndex + "/" + personsCount + " : " + personName);
            Itinerary itinerary = map_personName_itinerary.get(personName);
            BoundingRectangle geoCoordsBoundingRectangle = new BoundingRectangle();
            int itineraryNodesCount = itinerary.itineraryNodes.size();
            for (int i = 0; i < itineraryNodesCount; i++) {
                ItineraryNode itineraryNode = itinerary.itineraryNodes.get(i);
                if (i == 0) {
                    geoCoordsBoundingRectangle.init(itineraryNode.longitudeDeg, itineraryNode.latitudeDeg);
                } else {
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
            for (int i = 0; i < itineraryNodesCount; i++) {
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
        // save each personItinerary in a file
        for (String personName : map_personName_itinerary.keySet()) {
            Itinerary itinerary = map_personName_itinerary.get(personName);
            String outputItineraryFilePath = outputItineraryFolderPath + "\\" + personName + ".json";

            StringModifier.createAllFoldersIfNoExist(outputItineraryFolderPath);
            itinerary.saveJsonFile(outputItineraryFilePath);
        }
    }
}
