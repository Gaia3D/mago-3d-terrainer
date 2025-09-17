package com.gaia3d.itinerary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joml.Vector3d;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Itinerary {
    public String personName;
    ArrayList<ItineraryNode> itineraryNodes = new ArrayList<>();

    public Vector3d centerGeoCoords = new Vector3d();
    public ArrayList<Vector3d> positionsLC = new ArrayList<>();

    public Itinerary() {

    }

    public void saveJsonFile(String jsonFilePath) throws IOException {
        /*
        {
            "centerGeographicCoord" : {
            "altitude" : 100.0,
                    "latitude" : 37.57482350,
                    "longitude" : 126.95332850
        },
            "localPositions" : [
            -145.4103010964965,
                    2099.073603081633,
                    -0.3480926370248199,
                    -288.2044704982466,
                    2411.177903349162,
                    -0.4636205434799194,
                    -510.2899762040390,
                    2700.980533836060,
                    -0.5939913708716631,
                    -999.0841783841527,
                    1907.121881552157,
                    -0.3641254277899861,
                    -1533.078646993024,
         ...
   ],
            "nodes" : [
            {
                "adress" : "서울특별시 서대문구 홍제3동 5-112",
                    "date" : {
                "day" : "15",
                        "hour" : "0",
                        "minute" : "10",
                        "month" : "9",
                        "year" : "2022"
            },
                "geographicCoord" : {
                "latitude" : 37.5937360,
                        "longitude" : 126.9516820
            },
                "personName" : "A",
                    "pm10" : 2.0,
                    "pm25" : 15.0,
                    "region" : "서대문구"
            },
            {
                "adress" : "서울특별시 서대문구 홍제3동 283-40",
                    "date" : {
                "day" : "15",
                        "hour" : "1",
                        "minute" : "33",
                        "month" : "9",
                        "year" : "2022"
            },
                "geographicCoord" : {
                "latitude" : 37.5965480,
                        "longitude" : 126.9500650
            },
                "personName" : "A",
                    "pm10" : 27.0,
                    "pm25" : 35.0,
                    "region" : "서대문구"
            }
          ...
   ],
            "nodesCount" : 24
        }
        */
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNodeRoot = objectMapper.createObjectNode();

        // centerGeographicCoord
        ObjectNode geographicCoordNode = objectMapper.createObjectNode();
        geographicCoordNode.put("longitude", this.centerGeoCoords.x);
        geographicCoordNode.put("latitude", this.centerGeoCoords.y);
        geographicCoordNode.put("altitude", this.centerGeoCoords.z);
        objectNodeRoot.put("centerGeographicCoord", geographicCoordNode);

        // localPositions
        ArrayNode localPositionsArrayNode = objectMapper.createArrayNode();
        for (int i = 0; i < this.positionsLC.size(); i++) {
            Vector3d positionLC = this.positionsLC.get(i);
            localPositionsArrayNode.add(positionLC.x);
            localPositionsArrayNode.add(positionLC.y);
            localPositionsArrayNode.add(positionLC.z);
        }

        objectNodeRoot.put("localPositions", localPositionsArrayNode);

        // nodes
        ArrayNode nodesArrayNode = objectMapper.createArrayNode();
        for (int i = 0; i < this.itineraryNodes.size(); i++) {
            ItineraryNode itineraryNode = this.itineraryNodes.get(i);
            ObjectNode node = objectMapper.createObjectNode();
            node.put("personName", this.personName);

            ObjectNode geographicCoordNode2 = objectMapper.createObjectNode();
            geographicCoordNode2.put("longitude", itineraryNode.longitudeDeg);
            geographicCoordNode2.put("latitude", itineraryNode.latitudeDeg);
            node.put("geographicCoord", geographicCoordNode2);

            node.put("region", "unknown");
            node.put("adress", "unknown");
            node.put("pm10", "unknown");
            node.put("pm25", "unknown");

            ObjectNode dateNode = objectMapper.createObjectNode();
            dateNode.put("year", itineraryNode.year);
            dateNode.put("month", itineraryNode.month);
            dateNode.put("day", itineraryNode.day);
            dateNode.put("hour", itineraryNode.hour);
            dateNode.put("minute", itineraryNode.minute);
            node.put("date", dateNode);

            nodesArrayNode.add(node);
        }

        objectNodeRoot.put("nodes", nodesArrayNode);

        objectNodeRoot.put("nodesCount", this.itineraryNodes.size());

        //File outputFolder = new File(outputFolderPath);
        //if (!outputFolder.exists()) {
        //    outputFolder.mkdirs();
        //}
        // now write json file
        //String rawFileName = StringModifier.getRawFileName(inputFileName);
        //String outputFilePath = outputFolderPath + "\\" + rawFileName + ".json";
        JsonNode jsonNode = new ObjectMapper().readTree(objectNodeRoot.toString());
        objectMapper.writeValue(new File(jsonFilePath), jsonNode);


    }
}
