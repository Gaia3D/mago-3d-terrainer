package com.gaia3d.airPollutionDataConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gaia3d.coordSystem.CoordManager;
import com.gaia3d.geometry.BoundingBox;
import com.gaia3d.geometry.Vertex;
import com.gaia3d.image.Texture2D;
import com.gaia3d.utils.StringModifier;
import org.joml.Vector3d;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import javax.imageio.metadata.IIOInvalidTreeException;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;

public class AirPollutionDataConverter
{
    // The input data file format is *.TXT.***
    // sample of data file (*.plt).*******************************************************************************************
//* AERMOD (22112 ):  대전 제2매립장조성사업 환경영향평가                                     06/23/23
//        * AERMET ( 15181):  공사시 PM-10                                                            20:48:41
//        * MODELING OPTIONS USED:   RegDFAULT  CONC  ELEV  FLGPOL  RURAL
//*         POST/PLOT FILE OF CONCURRENT 24-HR VALUES FOR SOURCE GROUP: ALL
//*         FOR A TOTAL OF 17197 RECEPTORS.
//*         FORMAT: (3(1X,F13.5),3(1X,F8.2),2X,A6,2X,A8,2X,I8.8,2X,A8)
//        *        X             Y      AVERAGE CONC    ZELEV    ZHILL    ZFLAG    AVE     GRP       DATE     NET ID
//* ____________  ____________  ____________   ______   ______   ______  ______  ________  ________  ________
//  348700.00000 4030300.00000       0.00000   110.30   560.00     0.00   24-HR  ALL       20081824  CART1
//  348800.00000 4030300.00000       0.00000   117.60   560.00     0.00   24-HR  ALL       20081824  CART1
//  348900.00000 4030300.00000       0.00000   131.60   560.00     0.00   24-HR  ALL       20081824  CART1
//  349000.00000 4030300.00000       0.00000   156.60   560.00     0.00   24-HR  ALL       20081824  CART1
//  349100.00000 4030300.00000       0.00000   190.80   560.00     0.00   24-HR  ALL       20081824  CART1
//  349200.00000 4030300.00000       0.00000   217.40   278.00     0.00   24-HR  ALL       20081824  CART1
//  349300.00000 4030300.00000       0.00000   236.20   278.00     0.00   24-HR  ALL       20081824  CART1
//  349400.00000 4030300.00000       0.00000   217.20   278.00     0.00   24-HR  ALL       20081824  CART1
//  349500.00000 4030300.00000       0.00000   196.10   278.00     0.00   24-HR  ALL       20081824  CART1
//  349600.00000 4030300.00000       0.00000   181.20   278.00     0.00   24-HR  ALL       20081824  CART1
//  349700.00000 4030300.00000       0.00000   181.20   278.00     0.00   24-HR  ALL       20081824  CART1
//  349800.00000 4030300.00000       0.00000   193.10   278.00     0.00   24-HR  ALL       20081824  CART1
    //        ...
    public DataContainer dataContainer = null;

    public int maxDatesAllowed = -1; // if negative value, then no limit.***
    public AirPollutionTimeSeries airPollutionTimeSeries = null;

    private void getAllDatesInFile(String filePath, ArrayList<String> resultDatesArray)
    {
        HashMap<String, Integer> mapDates = new HashMap<>();
        try
        {
            String inputFilePath = filePath;
            File file = new File(inputFilePath);    //creates a new file instance
            FileReader fr = new FileReader(file);   //reads the file
            BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream

            String line;
            Boolean finished = false;
            String delimiter = " ";
            int columnsCount = 0;
            int rowsCount = 0;
            boolean bIsMatrix = true;

            double pollutionValueMAX = 0.0;

            // read lines.***
            // Hard Coding : read 8 lines that is the header.***
            for (int i = 0; i < 8; i++)// Hard Coding
            {
                line = br.readLine();// Hard Coding
            }

            int lastDate = 0;

            Vector<String> vecStrings = new Vector<String>();
            boolean skipEmptyStrings = true;
            AirPollutionSliceData airPollutionSliceData = new AirPollutionSliceData();

            while (!finished)
            {
                line = br.readLine();
                if (line == null) {
                    finished = true;
                    break;
                }

                rowsCount += 1;

                vecStrings.clear();
                StringModifier.splitString(line, delimiter, vecStrings, skipEmptyStrings);
                int vecStringSize = vecStrings.size();

                // skip rows that vecStrings.size() < 8.***
                if (vecStringSize < 8) {
                    continue;
                }

                // check "NET ID".***
                if (vecStringSize < 10) {
                    // here discards the rows that have no "NET ID".***
                    continue;
                }

                String date = vecStrings.get(8);
                mapDates.put(date, 1);

                // check if maxDatasAllowed is reached.***
                if(maxDatesAllowed > 0)
                {
                    int datesCount = mapDates.size();
                    if(datesCount >= maxDatesAllowed)
                    {
                        break;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // now, take keysArray from hashMap.***
        List<String> keysList = new ArrayList<>(mapDates.keySet());
        Collections.sort(keysList);

        for(int i = 0; i < keysList.size(); i++)
        {
            String date = keysList.get(i);
            resultDatesArray.add(date);
        }
    }

    private void makeTempFiles(String filePath, String outputFolderPath, DataLayer dataLayer)
    {
        // make a temp folder inside of outputFolderPath.***
        String tempFolderPath = outputFolderPath + File.separator + "temp";
        StringModifier.createFolderIfNoExists(Paths.get(tempFolderPath));

        double[] srcPts = new double[2];

        try {
            String inputFilePath = filePath;
            File file = new File(inputFilePath);    //creates a new file instance
            FileReader fr = new FileReader(file);   //reads the file
            BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream

            String line;
            Boolean finished = false;
            String delimiter = " ";
            int columnsCount = 0;
            int rowsCount = 0;
            boolean bIsMatrix = true;
            int currDate = 0;

            double pollutionValueMAX = 0.0;

            // read lines.***
            // Hard Coding : read 8 lines that is the header.***
            for (int i = 0; i < 8; i++)// Hard Coding
            {
                line = br.readLine();// Hard Coding
            }

            int lastDate = 0;

            Vector<String> vecStrings = new Vector<String>();
            boolean skipEmptyStrings = true;
            AirPollutionSliceData airPollutionSliceData = new AirPollutionSliceData();

            boolean is1rstPoint = true;

            while (!finished)
            {
                line = br.readLine();
                if (line == null) {
                    finished = true;

                    // file finished, so save the last temp file.***
                    // here, save the temp file.***
                    double altitude = dataLayer.altitude;
                    String altitudeString = "Alt" + String.format("%.2f", altitude);
                    String outputFileName = "airPollution_" + altitudeString + "_" + lastDate + ".bin";
                    String outputFilePath = tempFolderPath + File.separator + outputFileName;
                    dataLayer.tempFilesMap.put(lastDate, outputFilePath);
                    dataLayer.rowsCount = airPollutionSliceData.getRowsCount();
                    dataLayer.columnsCount = airPollutionSliceData.getColumnsCount();
                    airPollutionSliceData.saveTempFile(outputFilePath);

                    break;
                }

                rowsCount += 1;

                vecStrings.clear();
                StringModifier.splitString(line, delimiter, vecStrings, skipEmptyStrings);
                int vecStringSize = vecStrings.size();

                // skip rows that vecStrings.size() < 8.***
                if (vecStringSize < 8) {
                    continue;
                }

                // check "NET ID".***
                if(vecStringSize < 10)
                {
                    // here discards the rows that have no "NET ID".***
                    continue;
                }

                String dateKey = vecStrings.get(8);
                currDate = Integer.parseInt(dateKey);

                if(this.maxDatesAllowed > 0)
                {
                    // check if currDate exist into the datesArray.***
                    if(!this.dataContainer.datesArray.contains(dateKey))
                    {
                        // here, discard this date.***
                        continue;
                    }
                }

                if(lastDate == 0)
                {
                    lastDate = currDate;
                }
                else if(lastDate != currDate)
                {
                    // here, save the temp file.***
                    double altitude = dataLayer.altitude;
                    String altitudeString = "Alt" + String.format("%.2f", altitude);
                    String outputFileName = "airPollution_" + altitudeString + "_" + lastDate + ".bin";
                    String outputFilePath = tempFolderPath + File.separator + outputFileName;
                    dataLayer.tempFilesMap.put(lastDate, outputFilePath);
                    dataLayer.rowsCount = airPollutionSliceData.getRowsCount();
                    dataLayer.columnsCount = airPollutionSliceData.getColumnsCount();
                    airPollutionSliceData.saveTempFile(outputFilePath);

                    airPollutionSliceData = new AirPollutionSliceData(); // reset.***
                    is1rstPoint = true;
                    lastDate = currDate;
                }

                // now, transform strings to values.***
                double px = Double.parseDouble(vecStrings.get(0));
                double py = Double.parseDouble(vecStrings.get(1));
                double pz = Double.parseDouble(vecStrings.get(3));

                double pollutionValue = Double.parseDouble(vecStrings.get(2));

                is1rstPoint = false;

                if(pollutionValue < dataLayer.minPollutionValue)
                {
                    dataLayer.minPollutionValue = pollutionValue;
                }
                if(pollutionValue > dataLayer.maxPollutionValue)
                {
                    dataLayer.maxPollutionValue = pollutionValue;
                }

                AirPollutionPixelData airPollutionPixelData = new AirPollutionPixelData();
                airPollutionPixelData.X = px;
                airPollutionPixelData.Y = py;
                airPollutionPixelData.Z = pz;
                airPollutionPixelData.averageConcentration = pollutionValue;

                airPollutionSliceData.addPixelData(airPollutionPixelData);

            } // end while.***

            br.close();
            fr.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void makeTempFilesForLayers(String inputFolderPath, String outputFolderPath)
    {
        int layersCount = this.dataContainer.getDataLayersCount();
        for(int layer = 0; layer < layersCount; layer++)
        {
            DataLayer dataLayer = this.dataContainer.getDataLayer(layer);
            String filePath = dataLayer.filePath;
            if(dataLayer.tempFilesMap == null)
            {
                dataLayer.tempFilesMap = new TreeMap<>();
            }
            dataLayer.tempFilesMap.clear();
            this.makeTempFiles(filePath, outputFolderPath, dataLayer);
        }
    }

    private void loadOneFileAndCalculateLocationData(String filePath, DataLayer dataLayer)
    {
        try {
            CoordinateReferenceSystem inputCrs = null;
            CRSFactory crsFactory = new CRSFactory();
            inputCrs = crsFactory.createFromName("EPSG:32652"); // hard coding.***

            String inputFilePath = filePath;
            File file = new File(inputFilePath);    //creates a new file instance
            FileReader fr = new FileReader(file);   //reads the file
            BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream

            String line;
            Boolean finished = false;
            String delimiter = " ";

            // read lines.***
            // Hard Coding : read 8 lines that is the header.***
            for (int i = 0; i < 8; i++)// Hard Coding
            {
                line = br.readLine();// Hard Coding
            }

            int lastDate = 0;

            Vector<String> vecStrings = new Vector<String>();
            boolean skipEmptyStrings = true;

            boolean is1rstPoint = true;
            int currDate = 0;
            int rowsCount = 0;

            while (!finished)
            {
                line = br.readLine();
                if (line == null)
                {
                    finished = true;
                    break;
                }

                vecStrings.clear();
                StringModifier.splitString(line, delimiter, vecStrings, skipEmptyStrings);
                int vecStringSize = vecStrings.size();

                // skip rows that vecStrings.size() < 8.***
                if (vecStringSize < 8) {
                    continue;
                }

                // check "NET ID".***
                if(vecStringSize < 10)
                {
                    // here discards the rows that have no "NET ID".***
                    continue;
                }

                String dateKey = vecStrings.get(8);
                currDate = Integer.parseInt(dateKey);
                if(lastDate == 0)
                {
                    lastDate = currDate;
                }
                else if(lastDate != currDate)
                {
                    // now calculate the geoCoordBbox.***
                    BoundingBox bbox = dataLayer.geoCoordBBox;
                    double[] minXY = new double[2];
                    double[] maxXY = new double[2];
                    minXY[0] = bbox.minX;
                    minXY[1] = bbox.minY;
                    maxXY[0] = bbox.maxX;
                    maxXY[1] = bbox.maxY;
                    ProjCoordinate minProjCoordinate = new ProjCoordinate(minXY[0], minXY[1], 0.0);
                    ProjCoordinate maxProjCoordinate = new ProjCoordinate(maxXY[0], maxXY[1], 0.0);
                    ProjCoordinate minResult = CoordManager.transformToWGS84(inputCrs, minProjCoordinate);
                    ProjCoordinate maxResult = CoordManager.transformToWGS84(inputCrs, maxProjCoordinate);
                    dataLayer.geoCoordBBox.init(minResult.x, minResult.y, 0.0);
                    dataLayer.geoCoordBBox.addPoint(maxResult.x, maxResult.y, 0.0);

                    break;
                }

                // now, transform strings to values.***
                double px = Double.parseDouble(vecStrings.get(0));
                double py = Double.parseDouble(vecStrings.get(1));
                double pz = Double.parseDouble(vecStrings.get(3));

                double pollutionValue = Double.parseDouble(vecStrings.get(2));

                if(is1rstPoint)
                {
                    dataLayer.geoCoordBBox.init(px, py, 0.0);
                }
                else
                {
                    dataLayer.geoCoordBBox.addPoint(px, py, 0.0);
                }

                is1rstPoint = false;

                rowsCount += 1;
            }

            br.close();
            fr.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void convertDataByDataStructureFile(String inputDataStructurePath, String inputFolderPath, String outputFolderPath) throws IIOInvalidTreeException, FileNotFoundException
    {
        //******************
        // MAIN FUNCTION.***
        //******************
        if(this.dataContainer == null)
        {
            this.dataContainer = new DataContainer();
        }

        this.dataContainer.originalSourceProj4 = "+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs +type=crs"; // hard coding.***

        // read the data structure file.***
        // the dataStructure file is json format.***
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNodeRoot = null;
        try {
            objectNodeRoot = (ObjectNode) objectMapper.readTree(new File(inputDataStructurePath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // now, read the data.***
        this.dataContainer.originalSourceProj4 = objectNodeRoot.get("proj4").asText();
        int layersCount = objectNodeRoot.get("layersCount").asInt();
        ArrayNode objectLayersArrayNode = (ArrayNode) objectNodeRoot.get("layers");
        int timeSlicesCount = 0;
        for(int layer = 0; layer < layersCount; layer++)
        {
            ObjectNode objectLayersNode = (ObjectNode) objectLayersArrayNode.get(layer);
            String filePath = objectLayersNode.get("filePath").asText();

            DataLayer dataLayer = new DataLayer();
            dataLayer.altitude = objectLayersNode.get("altitude").asDouble();
            dataLayer.filePath = filePath;
            this.dataContainer.addDataLayer(dataLayer);
        }

        // now, read one of the files to know the dates.***
        DataLayer firstDataLayer = this.dataContainer.getDataLayer(0);
        String somefilePath = firstDataLayer.filePath;
        this.getAllDatesInFile(somefilePath, this.dataContainer.datesArray);

        // take the 1rst date as the start date.***
        String startDate = this.dataContainer.datesArray.get(0);
        // 20081824 = 2020-08-18 24:00:00.***
        this.dataContainer.year = 2000 + Integer.parseInt(startDate.substring(0, 2));
        this.dataContainer.month = Integer.parseInt(startDate.substring(2, 4));
        this.dataContainer.day = Integer.parseInt(startDate.substring(4, 6));
        this.dataContainer.hour = Integer.parseInt(startDate.substring(6, 8));

        // load one file and calculate the location data.***
        this.loadOneFileAndCalculateLocationData(somefilePath, firstDataLayer);


        // make temp files for each layer.***
        this.makeTempFilesForLayers(inputFolderPath, outputFolderPath);


        // once read the data, now convert the data.***
        int datesCount = this.dataContainer.datesArray.size();
        for(int date = 0; date < datesCount; date++)
        {
            String currDate = this.dataContainer.datesArray.get(date);
            this.convertDataByDate(currDate, inputFolderPath, outputFolderPath);
        }

        // now save indexJson file.***
        BoundingBox bbox = firstDataLayer.geoCoordBBox;
        Vector3d centerPos = bbox.GetCenterPosition();
        this.dataContainer.centerGeoCoordLongitudeDegree = centerPos.x;
        this.dataContainer.centerGeoCoordLatitudeDegree = centerPos.y;
        this.dataContainer.centerGeoCoordAltitude = 0.0;

        this.saveIndexJsonFile(outputFolderPath);

        int hola = 0;
    }

    private void saveIndexJsonFile(String outputFolderPath)
    {
        // save the index.json file.***
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNodeRoot = objectMapper.createObjectNode();
        // centerGeographicCoords.***
        ObjectNode objectCenterGeographicCoordsNode = objectMapper.createObjectNode();
        objectNodeRoot.set("centerGeographicCoord", objectCenterGeographicCoordsNode);
        objectCenterGeographicCoordsNode.put("longitude", this.dataContainer.centerGeoCoordLongitudeDegree);
        objectCenterGeographicCoordsNode.put("latitude", this.dataContainer.centerGeoCoordLatitudeDegree);
        objectCenterGeographicCoordsNode.put("altitude", this.dataContainer.centerGeoCoordAltitude);

        // date : year, month, day, hour, minute, second.***
        objectNodeRoot.put("year", this.dataContainer.year);
        objectNodeRoot.put("month", this.dataContainer.month);
        objectNodeRoot.put("day", this.dataContainer.day);
        objectNodeRoot.put("hour", this.dataContainer.hour);
        objectNodeRoot.put("minute", this.dataContainer.minute);
        objectNodeRoot.put("second", this.dataContainer.second);



        // mosaicColumnsCount, mosaicRowsCount.***
        objectNodeRoot.put("mosaicColumnsCount", this.dataContainer.mosaicColumnsCount);
        objectNodeRoot.put("mosaicRowsCount", this.dataContainer.mosaicRowsCount);

        // height_km, width_km.***
        BoundingBox bbox = new BoundingBox();
        this.dataContainer.getGeoCoordBoundingBox(bbox);
        double lengthX = 13100; // hard coding.***
        double lengthY = 13100; // hard coding.***
        objectNodeRoot.put("width_km", lengthX / 1000.0);
        objectNodeRoot.put("height_km", lengthY / 1000.0);

        // minMaxValues.***
        objectNodeRoot.put("pollutionMinValue", this.dataContainer.totalMinValue);
        objectNodeRoot.put("pollutionMaxValue", this.dataContainer.totalMaxValue);

        objectNodeRoot.put("layersCount", 1);

        // timeSeries.***
        int timeSeriesCount = this.dataContainer.dateAndMosaicTexFileNames.size();
        ArrayNode objectLayersArrayNode = objectMapper.createArrayNode();
        objectNodeRoot.set("layers", objectLayersArrayNode);
        ObjectNode objectLayersNode = objectMapper.createObjectNode();
        objectLayersArrayNode.add(objectLayersNode);

        ArrayNode objectTimeSeriesArrayNode = objectMapper.createArrayNode();
        // for each layer textureWidth, textureHeight, altitude, timeSeries.***
        DataLayer someDataLayer = this.dataContainer.getDataLayer(0);
        objectLayersNode.put("textureWidth", someDataLayer.columnsCount);
        objectLayersNode.put("textureHeight", someDataLayer.rowsCount);

        objectLayersNode.set("timeSeries", objectTimeSeriesArrayNode);
        for(int timeSeries = 0; timeSeries < timeSeriesCount; timeSeries++)
        {
            ObjectNode objectTimeSeriesNode = objectMapper.createObjectNode();
            objectTimeSeriesArrayNode.add(objectTimeSeriesNode);

            String date = this.dataContainer.dateAndMosaicTexFileNames.keySet().toArray()[timeSeries].toString();
            String mosaicTexFileName = this.dataContainer.dateAndMosaicTexFileNames.get(date);

            objectTimeSeriesNode.put("date", date);
            objectTimeSeriesNode.put("mosaicTexFileName", mosaicTexFileName);
        }


        String outputFilePath = outputFolderPath + File.separator + "index.json";
        try {
            objectMapper.writeValue(new File(outputFilePath), objectNodeRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void convertDataByDate(String date, String inputFolderPath, String outputFolderPath) throws IIOInvalidTreeException, FileNotFoundException {
        // Check if exist "outputFolderPath". Create if no exist folder.***
        StringModifier.createFolderIfNoExists(Paths.get(outputFolderPath));

        AirPollutionVolume airPollutionVolume = new AirPollutionVolume();
        airPollutionVolume.date = date;

        int layersCount = this.dataContainer.getDataLayersCount();
        for(int layer = 0; layer < layersCount; layer++)
        {
            DataLayer dataLayer = this.dataContainer.getDataLayer(layer);
            double altitude = dataLayer.altitude;
            String tempFilePath = dataLayer.tempFilesMap.get(Integer.parseInt(date));

            if(tempFilePath == null)
            {
                // error.***
                int hola = 0;
            }

            AirPollutionSliceData airPollutionSliceData = airPollutionVolume.getOrNewAirPollutionSliceData(altitude);
            airPollutionSliceData.loadTempFile(tempFilePath);

            int hola = 0;
        }
        Texture2D resultMosaicTexture = new Texture2D();
        airPollutionVolume.makeMosaicTexture(resultMosaicTexture);
        double[] minMaxValues = new double[2];
        this.dataContainer.getTotalMinMaxValues(minMaxValues);

        // now, save the mosaic texture.***
        String outputFileName = "airPollution_" + date + ".png";
        String outputFilePath = outputFolderPath + "/" + outputFileName;
        resultMosaicTexture.saveAsPNG(outputFilePath);

        this.dataContainer.dateAndMosaicTexFileNames.put(date, outputFileName);
        this.dataContainer.mosaicColumnsCount = airPollutionVolume.mosaicColumnsCount;
        this.dataContainer.mosaicRowsCount = airPollutionVolume.mosaicRowsCount;
        if(this.dataContainer.totalMinValue > minMaxValues[0])
        {
            this.dataContainer.totalMinValue = minMaxValues[0];
        }
        if(this.dataContainer.totalMaxValue < minMaxValues[1])
        {
            this.dataContainer.totalMaxValue = minMaxValues[1];
        }

        int hola = 0;
    }

    public void setMaxDatesCount(int maxDatesCount)
    {
        this.maxDatesAllowed = maxDatesCount;
    }
}
