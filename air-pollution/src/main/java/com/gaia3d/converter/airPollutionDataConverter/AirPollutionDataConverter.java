package com.gaia3d.converter.airPollutionDataConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gaia3d.basic.geometry.voxel.VoxelCP;
import com.gaia3d.basic.geometry.voxel.VoxelCPGrid3D;
import com.gaia3d.basic.marchingcube.MarchingCube;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.converter.jgltf.GltfWriter;
import com.gaia3d.coordSystem.CoordManager;
import com.gaia3d.geometry.BoundingBox;
import com.gaia3d.image.Texture2D;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.utils.StringModifier;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import javax.imageio.metadata.IIOInvalidTreeException;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
public class AirPollutionDataConverter {
//     The input data file format is *.TXT.
//     sample of data file (*.plt).*
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
//            ...

    private final Map<String, AirPollutionResultData> airPollutionResultDataMap = new HashMap<>();
    private final BoundingBox geoCoordBBox = new BoundingBox(); // minLongitude, minLatitude, minAltitude, maxLongitude, maxLatitude, maxAltitude
    //     DataStructure sample.
//    {
//        "layersCount": 7,
//            "proj4" : "+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs +type=crs",
//            "maxDatesCount" : 720,
//            "layers": [
//        {
//            "altitude": 0.0,
//                "fileName": "OD_01_H2S_TS_F000.pst"
//        },
//        {
//            "altitude": 10.0,
//                "fileName": "OD_01_H2S_TS_F010.pst"
//        },
//        {
//            "altitude": 20.0,
//                "fileName": "OD_01_H2S_TS_F020.pst"
//        },
//        {
//            "altitude": 30.0,
//                "fileName": "OD_01_H2S_TS_F030.pst"
//        },
//        {
//            "altitude": 60.0,
//                "fileName": "OD_01_H2S_TS_F060.pst"
//        },
//        {
//            "altitude": 100.0,
//                "fileName": "OD_01_H2S_TS_F100.pst"
//        },
//        {
//            "altitude": 200.0,
//                "fileName": "OD_01_H2S_TS_F200.pst"
//        }
//	]
//    }
    public DataContainer dataContainer = null;
    public int maxDatesAllowed = -1; // if negative value, then no limit.
    public double scale = 1.0;
    private double totalMinValue = Double.MAX_VALUE;
    private double totalMaxValue = Double.MIN_VALUE;

    private void getAllDatesInFile(String filePath, ArrayList<String> resultDatesArray) {
        log.info("==================Start Reading ASCII===================");
        HashMap<String, Integer> mapDates = new HashMap<>();
        try {
            File file = new File(filePath);    //creates a new file instance
            BufferedReader br = new BufferedReader(new FileReader(file, Charset.forName("EUC-KR")));  //creates a buffering character input stream

            String line;
            boolean finished = false;
            String delimiter = " ";
            int rowsCount = 0;

            // read lines.
            // Hard Coding : read 8 lines that is the header.
            log.info("--------------Reading ASCII. Header lines---------------");
            for (int i = 0; i < 8; i++) {
                line = br.readLine();
                log.info(line);
            }
            log.info("--------------Reading ASCII. Data lines---------------");

            List<String> vecStrings = new ArrayList<>();
            boolean skipEmptyStrings = true;
            int counterAux = 0;
            while (!finished) {
                line = br.readLine();
                if (line == null) {
                    finished = true;
                    break;
                }

                rowsCount += 1;

                if (counterAux > 1000000) {
                    log.info("Reading ASCII. Rows count : {}", rowsCount);
                    counterAux = 0;
                }

                vecStrings.clear();
                StringModifier.splitString(line, delimiter, vecStrings, skipEmptyStrings);
                int vecStringSize = vecStrings.size();

                // skip rows that vecStrings.size() < 8.
                if (vecStringSize < 8) {
                    continue;
                }

                // check "NET ID".
                if (vecStringSize < 10) {
                    // here discards the rows that have no "NET ID".
                    continue;
                }

                String date = vecStrings.get(8);
                mapDates.put(date, 1);

                // check if maxDatasAllowed is reached.
                if (maxDatesAllowed > 0) {
                    int datesCount = mapDates.size();
                    if (datesCount >= maxDatesAllowed) {
                        break;
                    }
                }

                counterAux += 1;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // now, take keysArray from hashMap.
        List<String> keysList = new ArrayList<>(mapDates.keySet());
        Collections.sort(keysList);

        resultDatesArray.addAll(keysList);
        log.info("===================End Reading ASCII====================");
    }

    private void makeTempFiles(String filePath, String outputFolderPath, DataLayer dataLayer) {
        // make a temp folder inside of outputFolderPath.
        String tempFolderPath = outputFolderPath + File.separator + "temp";
        StringModifier.createFolderIfNoExists(Paths.get(tempFolderPath));

        int layerNumber = dataLayer.layerIndex + 1;
        int maxLayersCount = this.dataContainer.getDataLayersCount();

        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem source = factory.createFromParameters("source", this.dataContainer.sourceProj);
        CoordinateReferenceSystem target = factory.createFromParameters("target", this.dataContainer.targetProj);

        double[] srcPts = new double[2];

        try {
            String inputFilePath = filePath;
            File file = new File(inputFilePath);
            BufferedReader br = new BufferedReader(new FileReader(file, Charset.forName("EUC-KR")));

            String line;
            boolean finished = false;
            String delimiter = " ";
            int rowsCount = 0;
            int currDate = 0;

            // Read lines.
            // Hard Coding: read 8 lines that is the header.
            log.info("==================Start Reading ASCII===================");
            log.info("--------------Reading ASCII. Header lines---------------");
            for (int i = 0; i < 8; i++) {
                line = br.readLine();
                log.info(line);
            }
            log.info("----------------Saving Temp File lines------------------");

            int lastDate = 0;

            List<String> vecStrings = new ArrayList<>();
            boolean skipEmptyStrings = true;
            AirPollutionSliceData airPollutionSliceData = new AirPollutionSliceData();

            // There are points that are not in the net (has no "NET ID").
            List<AirPollutionNoNetPixelData> noNetPixelDataList = new ArrayList<>();

            boolean is1rstPoint = true;

            while (!finished) {
                line = br.readLine();
                if (line == null) {
                    finished = true;

                    // file finished, so save the last temp file.
                    // here, save the temp file.
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

                // skip rows that vecStrings.size() < 8.
                if (vecStringSize < 8) {
                    continue;
                }

                // check "NET ID".
                if (vecStringSize < 10) {
                    // This point is not in the net.
                    // now, transform strings to values.
                    double px = Double.parseDouble(vecStrings.get(0));
                    double py = Double.parseDouble(vecStrings.get(1));
                    double pz = Double.parseDouble(vecStrings.get(3));

                    ProjCoordinate coordinate = new ProjCoordinate(px, py, pz);
                    ProjCoordinate result = CoordManager.transform(source, target, coordinate);

                    double convertedX = result.x;
                    double convertedY = result.y;
                    double convertedZ = result.z;

                    double pollutionValue = Double.parseDouble(vecStrings.get(2)) * this.scale;

                    // calculate the total min and max values.
                    if (pollutionValue < this.totalMinValue) {
                        this.totalMinValue = pollutionValue;
                    }
                    if (pollutionValue > this.totalMaxValue) {
                        this.totalMaxValue = pollutionValue;
                    }

                    String key = convertedX + "," + convertedY;

                    AirPollutionResultData newAirPollutionNoNetPixelData = AirPollutionResultData.builder().xPosition(convertedX).yPosition(convertedY).maximumValue(pollutionValue).date(vecStrings.get(8)).build();

                    AirPollutionResultData airPollutionResultData = airPollutionResultDataMap.get(key);
                    if (airPollutionResultData == null) {
                        airPollutionResultDataMap.put(key, newAirPollutionNoNetPixelData);
                    } else {
                        if (pollutionValue > airPollutionResultData.getMaximumValue()) {
                            airPollutionResultData.setMaximumValue(pollutionValue);
                            airPollutionResultData.setDate(vecStrings.get(8));
                        }
                    }

                    AirPollutionNoNetPixelData airPollutionNoNetPixelData = new AirPollutionNoNetPixelData();
                    airPollutionNoNetPixelData.X = convertedX;
                    airPollutionNoNetPixelData.Y = convertedY;
                    airPollutionNoNetPixelData.Z = pz;
                    airPollutionNoNetPixelData.averageConcentration = pollutionValue;
                    airPollutionNoNetPixelData.ZELEV = Double.parseDouble(vecStrings.get(3));
                    airPollutionNoNetPixelData.ZHILL = Double.parseDouble(vecStrings.get(4));
                    airPollutionNoNetPixelData.ZFLAG = Double.parseDouble(vecStrings.get(5));
                    airPollutionNoNetPixelData.AVE = vecStrings.get(6);
                    airPollutionNoNetPixelData.GRP = vecStrings.get(7);
                    airPollutionNoNetPixelData.DATE = vecStrings.get(8);

                    noNetPixelDataList.add(airPollutionNoNetPixelData);
                    continue;
                }

                String dateKey = vecStrings.get(8);
                currDate = Integer.parseInt(dateKey);

                if (lastDate == 0) {
                    lastDate = currDate;
                } else if (lastDate != currDate) {
                    // here, save the temp file.
                    log.info("[{}/{}] Saving temp file. Date : {}", layerNumber, maxLayersCount, lastDate);
                    double altitude = dataLayer.altitude;
                    String altitudeString = "Alt" + String.format("%.2f", altitude);
                    String outputFileName = "airPollution_" + altitudeString + "_" + lastDate + ".bin";
                    String outputFilePath = tempFolderPath + File.separator + outputFileName;
                    dataLayer.tempFilesMap.put(lastDate, outputFilePath);
                    dataLayer.rowsCount = airPollutionSliceData.getRowsCount();
                    dataLayer.columnsCount = airPollutionSliceData.getColumnsCount();
                    airPollutionSliceData.saveTempFile(outputFilePath);

                    airPollutionSliceData = new AirPollutionSliceData(); // reset.
                    lastDate = currDate;
                }

                if (this.maxDatesAllowed > 0) {
                    // check if currDate exist into the datesArray.
                    if (!this.dataContainer.datesArray.contains(dateKey)) {
                        // here, discard this date.
                        //continue;
                        break;
                    }
                }

                // now, transform strings to values.
                double px = Double.parseDouble(vecStrings.get(0));
                double py = Double.parseDouble(vecStrings.get(1));
                double pz = Double.parseDouble(vecStrings.get(3));

                double pollutionValue = Double.parseDouble(vecStrings.get(2)) * this.scale;

                if (pollutionValue < dataLayer.minPollutionValue) {
                    dataLayer.minPollutionValue = pollutionValue;
                }
                if (pollutionValue > dataLayer.maxPollutionValue) {
                    dataLayer.maxPollutionValue = pollutionValue;
                }

                AirPollutionPixelData airPollutionPixelData = new AirPollutionPixelData();
                airPollutionPixelData.X = px;
                airPollutionPixelData.Y = py;
                airPollutionPixelData.Z = pz;
                airPollutionPixelData.averageConcentration = pollutionValue;

                airPollutionSliceData.addPixelData(airPollutionPixelData);

            } // end while.

            br.close();

            // check if exist noNetPixelDataList.
            if (!noNetPixelDataList.isEmpty()) {
                // save a json file for noNetPixelDataList.
                String originalFileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
                String outputNoNetJsonFolderPath = outputFolderPath + File.separator + "noNetJson";
                StringModifier.createFolderIfNoExists(Paths.get(outputNoNetJsonFolderPath));

                //String altitudeString = "Alt" + String.format("%.2f", dataLayer.altitude);
                String outputFileName = originalFileName + "_noNet.json";
                String outputFilePath = outputNoNetJsonFolderPath + File.separator + outputFileName;

                ObjectMapper objectMapper = new ObjectMapper();
                ArrayNode objectNodeRoot = objectMapper.createArrayNode();
                for (AirPollutionNoNetPixelData airPollutionPixelData : noNetPixelDataList) {
                    ObjectNode objectNode = objectMapper.createObjectNode();
                    objectNode.put("X", airPollutionPixelData.X);
                    objectNode.put("Y", airPollutionPixelData.Y);
                    objectNode.put("AVERAGE_CONC", airPollutionPixelData.averageConcentration);
                    objectNode.put("ZELEV", airPollutionPixelData.ZELEV);
                    objectNode.put("ZHILL", airPollutionPixelData.ZHILL);
                    objectNode.put("ZFLAG", airPollutionPixelData.ZFLAG);
                    objectNode.put("AVE", airPollutionPixelData.AVE);
                    objectNode.put("GRP", airPollutionPixelData.GRP);
                    objectNode.put("DATE", airPollutionPixelData.DATE);
                    objectNodeRoot.add(objectNode);
                }
                objectMapper.writeValue(new File(outputFilePath), objectNodeRoot);
            }

        } catch (IOException e) {
            log.error("", e);
        }
    }

    /*private void makeNoNetPointsJsonFiles(String filePath, String outputFolderPath, DataLayer dataLayer) {
        // make a temp folder inside of outputFolderPath.
        String tempFolderPath = outputFolderPath + File.separator + "temp";
        StringModifier.createFolderIfNoExists(Paths.get(tempFolderPath));

        double[] srcPts = new double[2];

        try {
            String inputFilePath = filePath;
            File file = new File(inputFilePath);    //creates a new file instance
            BufferedReader br = new BufferedReader(new FileReader(file, Charset.forName("EUC-KR")));  //creates a buffering character input stream

            String line;
            boolean finished = false;
            String delimiter = " ";
            int rowsCount = 0;

            double pollutionValueMAX = 0.0;

            // read lines.
            // Hard Coding : read 8 lines that is the header.
            log.info("==================Start Reading ASCII===================");
            log.info("--------------Reading ASCII. Header lines---------------");
            for (int i = 0; i < 8; i++) {
                line = br.readLine();
                log.info(line);
            }
            log.info("--------------Reading ASCII. Data lines---------------");

            int lastDate = 0;

            List<String> vecStrings = new ArrayList<>();
            boolean skipEmptyStrings = true;
            AirPollutionSliceData airPollutionSliceData = new AirPollutionSliceData();

            // There are points that are not in the net (has no "NET ID").
            List<AirPollutionNoNetPixelData> noNetPixelDataList = new ArrayList<>();

            boolean is1rstPoint = true;

            while (!finished) {
                line = br.readLine();
                if (line == null) {
                    finished = true;

                    break;
                }

                rowsCount += 1;

                vecStrings.clear();
                StringModifier.splitString(line, delimiter, vecStrings, skipEmptyStrings);
                int vecStringSize = vecStrings.size();

                // skip rows that vecStrings.size() < 8.
                if (vecStringSize < 8) {
                    continue;
                }

                // check "NET ID".
                if (vecStringSize < 10) {
                    // This point is not in the net.
                    // now, transform strings to values.
                    double px = Double.parseDouble(vecStrings.get(0));
                    double py = Double.parseDouble(vecStrings.get(1));
                    double pz = Double.parseDouble(vecStrings.get(3));

                    double pollutionValue = Double.parseDouble(vecStrings.get(2));

                    AirPollutionNoNetPixelData airPollutionNoNetPixelData = new AirPollutionNoNetPixelData();
                    airPollutionNoNetPixelData.X = px;
                    airPollutionNoNetPixelData.Y = py;
                    airPollutionNoNetPixelData.Z = pz;
                    airPollutionNoNetPixelData.averageConcentration = pollutionValue;
                    airPollutionNoNetPixelData.ZELEV = Double.parseDouble(vecStrings.get(3));
                    airPollutionNoNetPixelData.ZHILL = Double.parseDouble(vecStrings.get(4));
                    airPollutionNoNetPixelData.ZFLAG = Double.parseDouble(vecStrings.get(5));
                    airPollutionNoNetPixelData.AVE = vecStrings.get(6);
                    airPollutionNoNetPixelData.GRP = vecStrings.get(7);
                    airPollutionNoNetPixelData.DATE = vecStrings.get(8);

                    noNetPixelDataList.add(airPollutionNoNetPixelData);
                    continue;
                }

            } // end while.

            br.close();

            // check if exist noNetPixelDataList.
            if (!noNetPixelDataList.isEmpty()) {
                // save a json file for noNetPixelDataList.
                String outputNoNetJsonFolderPath = outputFolderPath + File.separator + "noNetJson";
                StringModifier.createFolderIfNoExists(Paths.get(outputNoNetJsonFolderPath));

                String altitudeString = "Alt" + String.format("%.2f", dataLayer.altitude);
                String outputFileName = "airPollution_" + altitudeString + "_noNet.json";
                String outputFilePath = outputNoNetJsonFolderPath + File.separator + outputFileName;

                ObjectMapper objectMapper = new ObjectMapper();
                ArrayNode objectNodeRoot = objectMapper.createArrayNode();
                for (AirPollutionNoNetPixelData airPollutionPixelData : noNetPixelDataList) {
                    ObjectNode objectNode = objectMapper.createObjectNode();
                    objectNode.put("X", airPollutionPixelData.X);
                    objectNode.put("Y", airPollutionPixelData.Y);
                    objectNode.put("Z", airPollutionPixelData.Z);
                    objectNode.put("averageConcentration", airPollutionPixelData.averageConcentration);
                    objectNode.put("ZELEV", airPollutionPixelData.ZELEV);
                    objectNode.put("ZHILL", airPollutionPixelData.ZHILL);
                    objectNode.put("ZFLAG", airPollutionPixelData.ZFLAG);
                    objectNode.put("AVE", airPollutionPixelData.AVE);
                    objectNode.put("GRP", airPollutionPixelData.GRP);
                    objectNode.put("DATE", airPollutionPixelData.DATE);

                    objectNodeRoot.add(objectNode);
                }

                objectMapper.writeValue(new File(outputFilePath), objectNodeRoot);
            }

        } catch (IOException e) {
            log.error("", e);
        }
    }*/

    private void makeTempFilesForLayers(String inputFolderPath, String outputFolderPath) {
        int layersCount = this.dataContainer.getDataLayersCount();
        log.info("Making temp files. Layers count : " + layersCount);
        for (int layer = 0; layer < layersCount; layer++) {
            log.info("Current layer : {}", layer);
            DataLayer dataLayer = this.dataContainer.getDataLayer(layer);
            String filePath = dataLayer.filePath;
            if (dataLayer.tempFilesMap == null) {
                dataLayer.tempFilesMap = new TreeMap<>();
            }
            dataLayer.tempFilesMap.clear();
            this.makeTempFiles(filePath, outputFolderPath, dataLayer);
        }
        writeResultData(outputFolderPath);
        log.info("End making temp files.");

    }

    private void writeResultData(String outputFolderPath) {
        List<AirPollutionResultData> airPollutionResultDataList = new ArrayList<>(airPollutionResultDataMap.values());
        String outputNoNetJsonFolderPath = outputFolderPath + File.separator + "noNetJson";
        StringModifier.createFolderIfNoExists(Paths.get(outputNoNetJsonFolderPath));
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(new File(outputNoNetJsonFolderPath, "result_noNet.json"), airPollutionResultDataList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadOneFileAndCalculateLocationData(String filePath, DataLayer dataLayer) {
        try {
            CoordinateReferenceSystem inputCrs = null;
            CRSFactory crsFactory = new CRSFactory();
            inputCrs = crsFactory.createFromName("EPSG:32652"); // hard coding.

            String inputFilePath = filePath;
            File file = new File(inputFilePath);    //creates a new file instance
            BufferedReader br = new BufferedReader(new FileReader(file, Charset.forName("EUC-KR")));  //creates a buffering character input stream

            String line;
            Boolean finished = false;
            String delimiter = " ";

            // read lines.
            // Hard Coding : read 8 lines that is the header.
            for (int i = 0; i < 8; i++)// Hard Coding
            {
                line = br.readLine();// Hard Coding
            }

            int lastDate = 0;

            List<String> vecStrings = new ArrayList<>();
            boolean skipEmptyStrings = true;

            boolean is1rstPoint = true;
            int currDate = 0;
            int rowsCount = 0;

            while (!finished) {
                line = br.readLine();
                if (line == null) {
                    finished = true;
                    break;
                }

                vecStrings.clear();
                StringModifier.splitString(line, delimiter, vecStrings, skipEmptyStrings);
                int vecStringSize = vecStrings.size();

                // skip rows that vecStrings.size() < 8.
                if (vecStringSize < 8) {
                    continue;
                }

                // check "NET ID".
                if (vecStringSize < 10) {
                    // here discards the rows that have no "NET ID".
                    continue;
                }

                String dateKey = vecStrings.get(8);
                currDate = Integer.parseInt(dateKey);
                if (lastDate == 0) {
                    lastDate = currDate;
                } else if (lastDate != currDate) {
                    // now calculate the geoCoordBbox.
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

                // now, transform strings to values.
                double px = Double.parseDouble(vecStrings.get(0));
                double py = Double.parseDouble(vecStrings.get(1));
                double pz = Double.parseDouble(vecStrings.get(3));

                double pollutionValue = Double.parseDouble(vecStrings.get(2));

                if (is1rstPoint) {
                    dataLayer.geoCoordBBox.init(px, py, 0.0);
                } else {
                    dataLayer.geoCoordBBox.addPoint(px, py, 0.0);
                }

                is1rstPoint = false;

                rowsCount += 1;
            }

            br.close();
        } catch (IOException e) {
            log.error("", e);
        }
    }

    public void convertDataByDataStructureFile(String inputDataStructurePath, String inputFolderPath, String outputFolderPath) throws IIOInvalidTreeException, FileNotFoundException {
        //
        // MAIN FUNCTION.
        //
        if (this.dataContainer == null) {
            this.dataContainer = new DataContainer();
        }
        // inputDataStructurePathFile = new File(inputFolderPath, "./O_24_NO2_TS_F060.pst");
        // read the data structure file.
        // the dataStructure file is json format.
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNodeRoot = null;
        try {
            objectNodeRoot = (ObjectNode) objectMapper.readTree(new File(inputDataStructurePath));
        } catch (IOException e) {
            log.error("Error reading data structure file : {}", inputDataStructurePath);
            log.error("", e);
        }

        // now, read the data.
        this.dataContainer.sourceProj = objectNodeRoot.get("sourceProj").asText();
        this.dataContainer.targetProj = objectNodeRoot.get("targetProj").asText();
        this.dataContainer.timeInterval = objectNodeRoot.get("timeInterval").asDouble();
        this.dataContainer.timeIntervalUnits = objectNodeRoot.get("timeIntervalUnits").asText();

        // check if exist "maxDatesCount" in objectNodeRoot.
        int maxDatesCount = -1;
        if (objectNodeRoot.has("maxDatesCount")) {
            maxDatesCount = objectNodeRoot.get("maxDatesCount").asInt();
        }

        this.setMaxDatesCount(maxDatesCount);

        int layersCount = objectNodeRoot.get("layersCount").asInt();

        log.info("==================Input Data Structure==================");
        log.info("LayersCount : {}", layersCount);
        log.info("SourceProj : {}", this.dataContainer.sourceProj);
        log.info("TargetProj : {}", this.dataContainer.targetProj);
        log.info("MaxDatesCount : {}", this.maxDatesAllowed);
        log.info("TimeInterval : {}", this.dataContainer.timeInterval);
        log.info("TimeIntervalUnits : {}", this.dataContainer.timeIntervalUnits);
        log.info("========================================================");

        log.info("Reading data. Layers count : {}", layersCount);
        ArrayNode objectLayersArrayNode = (ArrayNode) objectNodeRoot.get("layers");
        for (int layer = 0; layer < layersCount; layer++) {
            log.info("Current layer : {}", layer);
            ObjectNode objectLayersNode = (ObjectNode) objectLayersArrayNode.get(layer);
            String fileName = objectLayersNode.get("fileName").asText();
            // if the fileName contains "./", then remove it.
            if (fileName.startsWith("./")) {
                fileName = fileName.substring(2);
            }
            String filePath = inputFolderPath + fileName;

            DataLayer dataLayer = new DataLayer();
            dataLayer.altitude = objectLayersNode.get("altitude").asDouble();
            dataLayer.filePath = filePath;
            dataLayer.layerIndex = layer;
            this.dataContainer.addDataLayer(dataLayer);
        }

        // now, read one of the files to know the dates.
        DataLayer firstDataLayer = this.dataContainer.getDataLayer(0);
        String somefilePath = firstDataLayer.filePath;
        this.getAllDatesInFile(somefilePath, this.dataContainer.datesArray);

        // take the 1rst date as the start date.
        String startDate = this.dataContainer.datesArray.get(0);
        // 20081824 = 2020-08-18 24:00:00.
        this.dataContainer.year = 2000 + Integer.parseInt(startDate.substring(0, 2));
        this.dataContainer.month = Integer.parseInt(startDate.substring(2, 4));
        this.dataContainer.day = Integer.parseInt(startDate.substring(4, 6));
        this.dataContainer.hour = Integer.parseInt(startDate.substring(6, 8));

        // load one file and calculate the location data.
        log.info("Calculating location data.");
        this.loadOneFileAndCalculateLocationData(somefilePath, firstDataLayer);

        // copy the geoCoordBBox from the first data layer.
        this.geoCoordBBox.copyFrom(firstDataLayer.getGeoCoordBBox());

        // calculate the center position from the first data layer.
        BoundingBox bbox = firstDataLayer.geoCoordBBox;
        Vector3d centerPos = bbox.GetCenterPosition();
        this.dataContainer.centerGeoCoordLongitudeDegree = centerPos.x;
        this.dataContainer.centerGeoCoordLatitudeDegree = centerPos.y;
        this.dataContainer.centerGeoCoordAltitude = 0.0;

        // make temp files for each layer.
        this.makeTempFilesForLayers(inputFolderPath, outputFolderPath);

        // marching cubes************************************************
        // make isoValues
        int isoValuesCount = 15;
        double[] isoValuesArray = new double[isoValuesCount];

        // make isoValuesArray
        double isoValuesIncrement = (this.totalMaxValue - this.totalMinValue) / (double) (isoValuesCount - 1);
        for (int i = 0; i < isoValuesCount; i++) {
            isoValuesArray[i] = this.totalMinValue + (double) (i) * isoValuesIncrement;
        }

        // once read the data, now convert the data.
        log.info("Converting data Marching-Cubes. Datas count : {}", this.dataContainer.datesArray.size());
        int datesCount = this.dataContainer.datesArray.size();
        for (int date = 0; date < datesCount; date++) {
            String currDate = this.dataContainer.datesArray.get(date);
            this.convertDataByDateMarchingCubes(currDate, inputFolderPath, outputFolderPath, isoValuesArray, date);
        }

        // now save indexJson file.
        log.info("Saving index.json file.");
        this.saveIndexJsonFileMC(outputFolderPath);
        // end marching cubes************************************************

        // once read the data, now convert the data.
        log.info("Converting data. Datas count : {}", this.dataContainer.datesArray.size());
        //int datesCount = this.dataContainer.datesArray.size();
        for (int date = 0; date < datesCount; date++) {
            String currDate = this.dataContainer.datesArray.get(date);
            this.convertDataByDate(currDate, inputFolderPath, outputFolderPath);
        }

        // now, with the stored mosaicTexturesFilePaths, make the mosaicTexture's pngsBinaryBlock.
        // The pngsBinaryBlock is a binary file that contains all mosaicTextures, limited to 60MB.
        ArrayList<String> mosaicTexturesFilePaths = new ArrayList<>();
        int mosaicTexturesCount = this.dataContainer.dateAndMosaicTexFileNames.size();
        // traverse map.
        log.info("Making mosaic textures. Mosaic textures count : {}", mosaicTexturesCount);
        for (Map.Entry<String, String> entry : this.dataContainer.dateAndMosaicTexFileNames.entrySet()) {
            String date = entry.getKey();
            String mosaicTextureFileName = entry.getValue();
            String mosaicTextureFilePath = outputFolderPath + File.separator + mosaicTextureFileName;
            mosaicTexturesFilePaths.add(mosaicTextureFilePath);
        }

        this.dataContainer.pngsBinDataArray.clear();
        log.info("Making pngs binary blocks.");
        makePngsBinaryBlocks(mosaicTexturesFilePaths, outputFolderPath, this.dataContainer.pngsBinDataArray);

        log.info("Saving index.json file.");
        this.saveIndexJsonFile(outputFolderPath);

        // Finally, delete the temp folder.
        log.info("Deleting temp folder.");
        String tempFolderPath = outputFolderPath + File.separator + "temp";
        StringModifier.deleteFolder(tempFolderPath);

        // delete all mosaicTextures.
        log.info("Deleting mosaic textures.");
        for (String mosaicTextureFilePath : mosaicTexturesFilePaths) {
            StringModifier.deleteFile(mosaicTextureFilePath);
        }

        // delete the jsonFiles in this.dataContainer.mosaicTexMetaDataFileNames.
        log.info("Deleting json files.");
        int mosaicTexMetaDataFileNamesCount = this.dataContainer.mosaicTexMetaDataFileNames.size();
        for (int i = 0; i < mosaicTexMetaDataFileNamesCount; i++) {
            String mosaicTexMetaDataFileName = this.dataContainer.mosaicTexMetaDataFileNames.get(i);
            String mosaicTexMetaDataFilePath = outputFolderPath + File.separator + mosaicTexMetaDataFileName;
            StringModifier.deleteFile(mosaicTexMetaDataFilePath);
        }

    }

    private void saveIndexJsonFileMC(String outputFolderPath) {
        // save the index.json file.
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNodeRoot = objectMapper.createObjectNode();
        // centerGeographicCoords.
        ObjectNode objectCenterGeographicCoordsNode = objectMapper.createObjectNode();
        objectNodeRoot.set("centerGeographicCoord", objectCenterGeographicCoordsNode);
        objectCenterGeographicCoordsNode.put("longitude", this.dataContainer.centerGeoCoordLongitudeDegree);
        objectCenterGeographicCoordsNode.put("latitude", this.dataContainer.centerGeoCoordLatitudeDegree);
        objectCenterGeographicCoordsNode.put("altitude", this.dataContainer.centerGeoCoordAltitude);

        // date as YYYYMMDD + "T" + hhmmss.
        String dateString = String.format("%04d%02d%02dT%02d%02d%02d", this.dataContainer.year, this.dataContainer.month, this.dataContainer.day, this.dataContainer.hour, this.dataContainer.minute, this.dataContainer.second);

        objectNodeRoot.put("startDate", dateString);

        // height_km, width_km.
        BoundingBox bbox = new BoundingBox();
        this.dataContainer.getGeoCoordBoundingBox(bbox);
        double lengthX = 13100; // hard coding.
        double lengthY = 13100; // hard coding.
        objectNodeRoot.put("width_km", lengthX / 1000.0);
        objectNodeRoot.put("height_km", lengthY / 1000.0);

        // timeInterval & timeUnit.
        objectNodeRoot.put("timeInterval", this.dataContainer.timeInterval);
        objectNodeRoot.put("timeIntervalUnits", this.dataContainer.timeIntervalUnits);

        // minMaxValues.
        objectNodeRoot.put("totalMinValue", this.totalMinValue);
        objectNodeRoot.put("totalMaxValue", this.totalMaxValue);

        // glbMetaData.
        List<String> glbMetaDataFileNames = this.dataContainer.getGlbMetaDataFileNames();
        ArrayNode glbMetaDataFileNamesArrayNode = objectMapper.createArrayNode();
        int glbMetaDataFileNamesCount = glbMetaDataFileNames.size();
        for (int i = 0; i < glbMetaDataFileNamesCount; i++) {
            String glbMetaDataFileName = glbMetaDataFileNames.get(i);

            // load the jsonFile.
            String glbMetaDataFilePath = outputFolderPath + File.separator + glbMetaDataFileName;
            ObjectNode glbMetaDataObjectNode = null;
            try {
                glbMetaDataObjectNode = (ObjectNode) objectMapper.readTree(new File(glbMetaDataFilePath));

                // now, delete the glbMetaDataFile
                StringModifier.deleteFile(glbMetaDataFilePath);
            } catch (IOException e) {
                log.error("", e);
            }
            glbMetaDataFileNamesArrayNode.add(glbMetaDataObjectNode);
        }

        objectNodeRoot.put("glbMetaDataFileNames", glbMetaDataFileNamesArrayNode);

        String outputFilePath = outputFolderPath + File.separator + "indexMC.json";
        try {
            objectMapper.writeValue(new File(outputFilePath), objectNodeRoot);
        } catch (IOException e) {
            log.error("", e);
        }
    }

    private void convertDataByDateMarchingCubes(String date, String inputFolderPath, String outputFolderPath, double[] isoValuesArray, int idx) throws FileNotFoundException {
        // Check if exist "outputFolderPath". Create if no exist folder.
        StringModifier.createFolderIfNoExists(Paths.get(outputFolderPath));

        AirPollutionVolume airPollutionVolume = new AirPollutionVolume();
        airPollutionVolume.date = date;
        airPollutionVolume.setIdx(idx);
        airPollutionVolume.getGeoCoordBBox().copyFrom(this.geoCoordBBox);

        int layersCount = this.dataContainer.getDataLayersCount();
        for (int layer = 0; layer < layersCount; layer++) {
            DataLayer dataLayer = this.dataContainer.getDataLayer(layer);
            double altitude = dataLayer.altitude;
            String tempFilePath = dataLayer.tempFilesMap.get(Integer.parseInt(date));

            if (tempFilePath == null) {
                log.error("tempFilePath is null");
            }

            AirPollutionSliceData airPollutionSliceData = airPollutionVolume.getOrNewAirPollutionSliceData(altitude);
            airPollutionSliceData.minAltitude = altitude;

            if (layer < layersCount - 1) {
                DataLayer dataLayerNext = this.dataContainer.getDataLayer(layer + 1);
                airPollutionSliceData.maxAltitude = dataLayerNext.altitude;
            } else {
                airPollutionSliceData.maxAltitude = altitude + 1.0;
            }

            File file = new File(tempFilePath);
            if (!file.exists()) {
                // error.
                throw new FileNotFoundException();
            }

            airPollutionSliceData.loadTempFile(tempFilePath);
        }

        int isoValuesCount = isoValuesArray.length;

        GaiaScene gaiaSceneMaster = null;
        boolean addLastTopSlice = false;
        VoxelCPGrid3D voxelCPGrid3D = airPollutionVolume.makeVoxelCPGrid3D(addLastTopSlice); // new
        // now, transform the geoCoords of the voxelCPGrid3D to the target projection.
        BoundingBox geoCoordBBox = airPollutionVolume.getGeoCoordBBox();
        Vector3d centerCartographic = geoCoordBBox.GetCenterPosition();
        transformCoordsOfVoxelCpGrid3DFromGeoCoordsToLocalCoords(voxelCPGrid3D, centerCartographic);

        gaiaSceneMaster = MarchingCube.makeGaiaSceneOnion(voxelCPGrid3D, isoValuesArray);

        if (gaiaSceneMaster != null) {
            GltfWriter gltfWriter = new GltfWriter();
            String glbFileName = "airPollution_" + date + ".glb";

            // set the glbFileName to airPollutionVolume.
            airPollutionVolume.setGlbFileName(glbFileName);

            // save the glb file.
            String glbFilePath = outputFolderPath + glbFileName;
            gltfWriter.writeGlb(gaiaSceneMaster, glbFilePath);

            // save the glbMetaData.
            String jsonFileName = "airPollution_" + date + ".json";
            String jsonFilePath = outputFolderPath + File.separator + jsonFileName;
            airPollutionVolume.saveAsJsonMC(jsonFilePath);
            this.dataContainer.getGlbMetaDataFileNames().add(jsonFileName);
        }
    }

    private void transformCoordsOfVoxelCpGrid3DFromGeoCoordsToLocalCoords(VoxelCPGrid3D voxelCPGrid3D, Vector3d centerCartographic) {
        //*************************************************
        // input voxelCPGrid3D is in geographic coords.
        // output voxelCPGrid3D will be in local coords.
        //*************************************************
        Vector3d centerCartesian = GlobeUtils.geographicToCartesianWgs84(centerCartographic);
        Matrix4d transformationMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerCartesian);
        Matrix4d inverseTransformationMatrix = new Matrix4d(transformationMatrix);
        inverseTransformationMatrix.invert();

        // transform the coords of the voxelCPGrid3D to the local coords.
        int gridsCountX = voxelCPGrid3D.getGridsCountX();
        int gridsCountY = voxelCPGrid3D.getGridsCountY();
        int gridsCountZ = voxelCPGrid3D.getGridsCountZ();
        for (int i = 0; i < gridsCountX; i++) {
            for (int j = 0; j < gridsCountY; j++) {
                for (int k = 0; k < gridsCountZ; k++) {
                    VoxelCP voxel = voxelCPGrid3D.getVoxel(i, j, k);
                    if (voxel != null) {
                        Vector3d voxelPos = voxel.getPosition(); // this is the geographic coord.
                        Vector3d cartesianWC = GlobeUtils.geographicToCartesianWgs84(voxelPos);
                        Vector4d local = new Vector4d(cartesianWC.x, cartesianWC.y, cartesianWC.z, 1.0);
                        // transform the coords to the local coords.
                        inverseTransformationMatrix.transform(local, local);
                        voxelPos.set(local.x, local.y, local.z);

                        int hola = 0;
                    }
                }
            }
        }

    }

    private void getPngsGroupLimitedByMaxByteSize(ArrayList<String> mosaicTexturesFilePaths, long maxByteSize, ArrayList<ArrayList<String>> resultPngsGroup) {
        int mosaicTexturesFilePathsCount = mosaicTexturesFilePaths.size();
        ArrayList<String> pngsGroup = new ArrayList<>();
        long currentPngsGroupSize = 0;
        for (String mosaicTextureFilePath : mosaicTexturesFilePaths) {
            File mosaicTextureFile = new File(mosaicTextureFilePath);
            long mosaicTextureFileSize = mosaicTextureFile.length();
            if (currentPngsGroupSize + mosaicTextureFileSize > maxByteSize) {
                // save the current pngsGroup.
                resultPngsGroup.add(pngsGroup);
                pngsGroup = new ArrayList<>();
                currentPngsGroupSize = 0;
            }
            pngsGroup.add(mosaicTextureFilePath);
            currentPngsGroupSize += mosaicTextureFileSize;
        }
        if (!pngsGroup.isEmpty()) {
            resultPngsGroup.add(pngsGroup);
        }
    }

    public void makePngsBinaryBlocks(ArrayList<String> mosaicTexturesFilePaths, String outputFolderPath, ArrayList<PngsBinaryBlockData> pngsBinDataArray) {
        // This function makes a pngsBinaryBlock.
        // The pngsBinaryBlock is a binary file that contains all mosaicTextures, limited to 60MB.
        double pngsBinaryBlockSizeLimit = 60.0; // MB.
        long currentPngsBinaryBlockSize = 0;
        long pngsBinaryBlockSizeLimitBytes = (long) (pngsBinaryBlockSizeLimit * 1024.0 * 1024.0);
        ArrayList<ArrayList<String>> pngsGroups = new ArrayList<>();
        getPngsGroupLimitedByMaxByteSize(mosaicTexturesFilePaths, pngsBinaryBlockSizeLimitBytes, pngsGroups);
        int groupsCount = pngsGroups.size();

        for (int group = 0; group < groupsCount; group++) {
            String pngsBinaryBlockFileName = "pngsBinaryBlock_" + group + ".bin";
            currentPngsBinaryBlockSize = 0;
            ArrayList<String> mosaicTextureFilePathArray = pngsGroups.get(group);
            int pngsCount = mosaicTextureFilePathArray.size();
            try (DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFolderPath + File.separator + pngsBinaryBlockFileName)))) {
                for (int j = 0; j < pngsCount; j++) {
                    String mosaicTextureFilePath = mosaicTextureFilePathArray.get(j);
                    File mosaicTextureFile = new File(mosaicTextureFilePath);
                    long mosaicTextureFileSize = mosaicTextureFile.length();

                    PngsBinaryBlockData pngsBinData = new PngsBinaryBlockData();
                    pngsBinData.originalPngFileName = mosaicTextureFile.getName();
                    pngsBinData.pngsBinaryBlockDataFileName = pngsBinaryBlockFileName;
                    pngsBinData.startByteIndex = (int) currentPngsBinaryBlockSize;
                    pngsBinData.endByteIndex = (int) (currentPngsBinaryBlockSize + mosaicTextureFileSize);
                    currentPngsBinaryBlockSize += mosaicTextureFileSize;
                    pngsBinDataArray.add(pngsBinData);

                    try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(mosaicTextureFile))) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            dataOutputStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
            } catch (IOException e) {
                log.error("", e);
            }
        }
    }

    private void saveIndexJsonFile(String outputFolderPath) {
        // save the index.json file.
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNodeRoot = objectMapper.createObjectNode();
        // centerGeographicCoords.
        ObjectNode objectCenterGeographicCoordsNode = objectMapper.createObjectNode();
        objectNodeRoot.set("centerGeographicCoord", objectCenterGeographicCoordsNode);
        objectCenterGeographicCoordsNode.put("longitude", this.dataContainer.centerGeoCoordLongitudeDegree);
        objectCenterGeographicCoordsNode.put("latitude", this.dataContainer.centerGeoCoordLatitudeDegree);
        objectCenterGeographicCoordsNode.put("altitude", this.dataContainer.centerGeoCoordAltitude);

        // date : year, month, day, hour, minute, second.
        objectNodeRoot.put("year", this.dataContainer.year);
        objectNodeRoot.put("month", this.dataContainer.month);
        objectNodeRoot.put("day", this.dataContainer.day);
        objectNodeRoot.put("hour", this.dataContainer.hour);
        objectNodeRoot.put("minute", this.dataContainer.minute);
        objectNodeRoot.put("second", this.dataContainer.second);
        objectNodeRoot.put("millisecond", this.dataContainer.millisecond);

        // height_km, width_km.
        BoundingBox bbox = new BoundingBox();
        this.dataContainer.getGeoCoordBoundingBox(bbox);
        double lengthX = 13100; // hard coding.
        double lengthY = 13100; // hard coding.
        objectNodeRoot.put("width_km", lengthX / 1000.0);
        objectNodeRoot.put("height_km", lengthY / 1000.0);

        // timeInterval & timeUnit.
        objectNodeRoot.put("timeInterval", this.dataContainer.timeInterval);
        objectNodeRoot.put("timeIntervalUnits", this.dataContainer.timeIntervalUnits);

        // minMaxValues.
        objectNodeRoot.put("totalMinValue", this.dataContainer.totalMinValue);
        objectNodeRoot.put("totalMaxValue", this.dataContainer.totalMaxValue);

        // "mosaicTexMetaDataFileNamesCount".
        objectNodeRoot.put("mosaicTexMetaDataFileNamesCount", this.dataContainer.dateAndMosaicTexFileNames.size());

        // "mosaicTexMetaDataJsonArray".
        ArrayNode mosaicTexMetaDataJsonArrayNode = objectMapper.createArrayNode();
        int mosaicTexMetaDataFileNamesCount = this.dataContainer.mosaicTexMetaDataFileNames.size();
        for (int i = 0; i < mosaicTexMetaDataFileNamesCount; i++) {
            String mosaicTexMetaDataFileName = this.dataContainer.mosaicTexMetaDataFileNames.get(i);

            // load the jsonFile.
            String mosaicTexMetaDataFilePath = outputFolderPath + File.separator + mosaicTexMetaDataFileName;
            ObjectNode mosaicTexMetaDataObjectNode = null;
            try {
                mosaicTexMetaDataObjectNode = (ObjectNode) objectMapper.readTree(new File(mosaicTexMetaDataFilePath));
            } catch (IOException e) {
                log.error("", e);
            }
            mosaicTexMetaDataJsonArrayNode.add(mosaicTexMetaDataObjectNode);
        }
        //objectNodeRoot.put("mosaicTexMetaDataFileNames", mosaicTexMetaDataFileNamesArrayNode);
        objectNodeRoot.put("mosaicTexMetaDataFileNamesCount", mosaicTexMetaDataFileNamesCount);
        objectNodeRoot.put("mosaicTexMetaDataJsonArray", mosaicTexMetaDataJsonArrayNode);

        // now write the pngsBinaryBlockData.
        HashMap<String, Integer> pngsBinDataMap = new HashMap<>();
        ArrayNode pngsBinDataArrayNode = objectMapper.createArrayNode();
        int pngsBinDataArrayCount = this.dataContainer.pngsBinDataArray.size();
        for (int i = 0; i < pngsBinDataArrayCount; i++) {
            PngsBinaryBlockData pngsBinData = this.dataContainer.pngsBinDataArray.get(i);
            ObjectNode pngsBinDataObjectNode = objectMapper.createObjectNode();
            pngsBinDataObjectNode.put("originalPngFileName", pngsBinData.originalPngFileName);
            pngsBinDataObjectNode.put("pngsBinaryBlockDataFileName", pngsBinData.pngsBinaryBlockDataFileName);
            pngsBinDataObjectNode.put("startByteIndex", pngsBinData.startByteIndex);
            pngsBinDataObjectNode.put("endByteIndex", pngsBinData.endByteIndex);
            pngsBinDataArrayNode.add(pngsBinDataObjectNode);

            pngsBinDataMap.put(pngsBinData.pngsBinaryBlockDataFileName, i);
        }
        objectNodeRoot.put("pngsBinDataArray", pngsBinDataArrayNode);

        // now save the pngsBinaryBlockDataFileNameMap.
        ArrayNode pngsBinDataMapArrayNode = objectMapper.createArrayNode();
        Set<String> keys = pngsBinDataMap.keySet();
        for (String key : keys) {
            ObjectNode pngsBinDataMapObjectNode = objectMapper.createObjectNode();
            pngsBinDataMapObjectNode.put("fileName", key);
            pngsBinDataMapArrayNode.add(pngsBinDataMapObjectNode);
        }

        objectNodeRoot.put("pngsBinBlockFileNames", pngsBinDataMapArrayNode);

        String outputFilePath = outputFolderPath + File.separator + "index.json";
        try {
            objectMapper.writeValue(new File(outputFilePath), objectNodeRoot);
        } catch (IOException e) {
            log.error("", e);
        }
    }

    private void convertDataByDate(String date, String inputFolderPath, String outputFolderPath) throws IIOInvalidTreeException, FileNotFoundException {
        // Check if exist "outputFolderPath". Create if no exist folder.
        StringModifier.createFolderIfNoExists(Paths.get(outputFolderPath));

        AirPollutionVolume airPollutionVolume = new AirPollutionVolume();
        airPollutionVolume.date = date;

        int layersCount = this.dataContainer.getDataLayersCount();
        for (int layer = 0; layer < layersCount; layer++) {
            DataLayer dataLayer = this.dataContainer.getDataLayer(layer);
            double altitude = dataLayer.altitude;
            String tempFilePath = dataLayer.tempFilesMap.get(Integer.parseInt(date));

            if (tempFilePath == null) {
                log.error("tempFilePath is null");
            }

            AirPollutionSliceData airPollutionSliceData = airPollutionVolume.getOrNewAirPollutionSliceData(altitude);
            airPollutionSliceData.minAltitude = altitude;

            if (layer < layersCount - 1) {
                DataLayer dataLayerNext = this.dataContainer.getDataLayer(layer + 1);
                airPollutionSliceData.maxAltitude = dataLayerNext.altitude;
            } else {
                airPollutionSliceData.maxAltitude = altitude + 1.0;
            }

            File file = new File(tempFilePath);
            if (!file.exists()) {
                // error.
                throw new FileNotFoundException();
            }

            airPollutionSliceData.loadTempFile(tempFilePath);
        }
        Texture2D resultMosaicTexture = new Texture2D();
        airPollutionVolume.makeMosaicTexture(resultMosaicTexture); // here calculates volumeMinMaxValues
        double[] minMaxValues = new double[2];
        airPollutionVolume.getMinMaxValues(minMaxValues);

        if (minMaxValues[1] > 0.8) {
            int hola = 0;
        }

        // now, save the mosaic texture.
        String outputFileName = "airPollution_" + date + ".png";
        String outputFilePath = outputFolderPath + "/" + outputFileName;
        resultMosaicTexture.saveAsPNG(outputFilePath);
        airPollutionVolume.mosaicPngFileName = outputFileName;

        this.dataContainer.dateAndMosaicTexFileNames.put(date, outputFileName);
        this.dataContainer.mosaicColumnsCount = airPollutionVolume.mosaicColumnsCount;
        this.dataContainer.mosaicRowsCount = airPollutionVolume.mosaicRowsCount;
        if (this.dataContainer.totalMinValue > minMaxValues[0]) {
            this.dataContainer.totalMinValue = minMaxValues[0];
        }
        if (this.dataContainer.totalMaxValue < minMaxValues[1]) {
            this.dataContainer.totalMaxValue = minMaxValues[1];
        }

        // Now, save the json for this volume.
        String jsonFileName = "airPollution_" + date + ".json";
        String jsonFilePath = outputFolderPath + File.separator + jsonFileName;
        airPollutionVolume.saveAsJson(jsonFilePath);
        this.dataContainer.mosaicTexMetaDataFileNames.add(jsonFileName);
    }

    public void setMaxDatesCount(int maxDatesCount) {
        this.maxDatesAllowed = maxDatesCount;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }
}
