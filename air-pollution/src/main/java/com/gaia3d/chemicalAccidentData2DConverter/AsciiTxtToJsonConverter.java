package com.gaia3d.chemicalAccidentData2DConverter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gaia3d.chemicalAccidentDataConverter.DataContainer;
import com.gaia3d.chemicalAccidentDataConverter.DataLayer;
import com.gaia3d.utils.GeometryUtils;
import com.gaia3d.utils.StringModifier;
import org.joml.Vector2d;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class AsciiTxtToJsonConverter {
    public void ConvertCsvTxtToMatrixTxt(String inputFolderPath, String outputFolderPath, String inputFileName) {
        // Txt sample to parse :
        //no;"accident_no";"analysis_time";"grid_id";"concentration";"aegl1_eval";"aegl2_eval";"aegl3_eval";"victim_count";"acu_assment_cd"
        //27473185;CA201905001;"201905271100";"1";"0.000E+0";"0.000E+0";"0.000E+0";"0.000E+0";"0";"0"
        //27473186;CA201905001;"201905271110";"1";"0.000E+0";"0.000E+0";"0.000E+0";"0.000E+0";"0";"0"
        //27473187;CA201905001;"201905271120";"1";"0.000E+0";"0.000E+0";"0.000E+0";"0.000E+0";"0";"0"
        //27473188;CA201905001;"201905271130";"1";"0.000E+0";"0.000E+0";"0.000E+0";"0.000E+0";"0";"0"
        //27473189;CA201905001;"201905271140";"1";"0.000E+0";"0.000E+0";"0.000E+0";"0.000E+0";"0";"0"
        //27473190;CA201905001;"201905271150";"1";"0.000E+0";"0.000E+0";"0.000E+0";"0.000E+0";"0";"0"
        //27473191;CA201905001;"201905271200";"1";"0.000E+0";"0.000E+0";"0.000E+0";"0.000E+0";"0";"0"
        //27473192;CA201905001;"201905271210";"1";"0.000E+0";"0.000E+0";"0.000E+0";"0.000E+0";"0";"0"
        //27473193;CA201905001;"201905271220";"1";"0.000E+0";"0.000E+0";"0.000E+0";"0.000E+0";"0";"0"
        //27473194;CA201905001;"201905271230";"1";"0.000E+0";"0.000E+0";"0.000E+0";"0.000E+0";"0";"0"
        //27473195;CA201905001;"201905271240";"1";"0.000E+0";"0.000E+0";"0.000E+0";"0.000E+0";"0";"0"
        //27473196;CA201905001;"201905271250";"1";"0.000E+0";"0.000E+0";"0.000E+0";"0.000E+0";"0";"0"
        //27473197;CA201905001;"201905271300";"1";"0.000E+0";"0.000E+0";"0.000E+0";"0.000E+0";"0";"0"
        //27473198;CA201905001;"201905271310";"1";"0.000E+0";"0.000E+0";"0.000E+0";"0.000E+0";"0";"0"
        //27473199;CA201905001;"201905271320";"1";"0.000E+0";"0.000E+0";"0.000E+0";"0.000E+0";"0";"0"
        //27473200;CA201905001;"201905271330";"1";"0.000E+0";"0.000E+0";"0.000E+0";"0.000E+0";"0";"0"
        //27473201;CA201905001;"201905271340";"1";"0.000E+0";"0.000E+0";"0.000E+0";"0.000E+0";"0";"0"
        //27473202;CA201905001;"201905271350";"1";"0.000E+0";"0.000E+0";"0.000E+0";"0.000E+0";"0";"0"
        //27473203;CA201905001;"201905271400";"1";"5.821E-23";"5.292E-26";"3.424E-27";"4.733E-28";"0";"0"
        //27473204;CA201905001;"201905271410";"1";"2.045E-14";"1.859E-17";"1.203E-18";"1.662E-19";"0";"0"
        //27473205;CA201905001;"201905271420";"1";"3.219E-14";"2.926E-17";"1.894E-18";"2.617E-19";"0";"0"
        //27473206;CA201905001;"201905271430";"1";"3.213E-14";"2.921E-17";"1.890E-18";"2.612E-19";"0";"0"
        // ...

        try {
            System.out.println("Converting : " + inputFileName + " to json.");

            // Check if exist "outputFolderPath". Create if no exist folder.***
            StringModifier.createFolderIfNoExists(Paths.get(outputFolderPath));

            String inputFilePath = inputFolderPath + "\\" + inputFileName;
            File file = new File(inputFilePath);    //creates a new file instance
            FileReader fr = new FileReader(file);   //reads the file
            BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream

            String line;
            Boolean finished = false;
            Integer counter = 0;
            Integer pointsCount = 0;
            String delimiter = ";";
            Vector<Double> vecValues = new Vector<>();
            int columnsCount = 0;
            int rowsCount = 0;
            boolean bIsMatrix = true;

            Vector<String> resultSplittedStrings = new Vector<>();
            Vector<String> columnNames = new Vector<>();
            Map<String, DataSlice2D> dataSlice2DMap = new HashMap<>(); // analysis_time_string, DataSlice2D.***

            // read lines.***
            while (!finished) {
                line = br.readLine();
                if (line == null) {
                    //finished = true;
                    break;
                }

                resultSplittedStrings.clear();
                boolean skipEmptyStrings = true;
                StringModifier.splitString(line, delimiter, resultSplittedStrings, skipEmptyStrings);
                if (columnsCount == 0) {
                    columnsCount = resultSplittedStrings.size();
                } else {
                    int stringsCount = resultSplittedStrings.size();
                    if (stringsCount != columnsCount) {
                        bIsMatrix = false;
                        break;
                    }
                }

                if(counter == 0)
                {
                    // this line contains the columns names.***
                    for(int col = 0; col < columnsCount; col++)
                    {
                        columnNames.add(resultSplittedStrings.get(col));
                    }
                    counter += 1;
                    continue;
                }

                String no_string = resultSplittedStrings.get(0).replace("\"", "");
                String accident_no_string = resultSplittedStrings.get(1).replace("\"", "");
                String analysis_time_string = resultSplittedStrings.get(2).replace("\"", "");
                String grid_id_string = resultSplittedStrings.get(3).replace("\"", "");
                String concentration_string = resultSplittedStrings.get(4).replace("\"", "");
                String aegl1_eval_string = resultSplittedStrings.get(5).replace("\"", "");
                String aegl2_eval_string = resultSplittedStrings.get(6).replace("\"", "");
                String aegl3_eval_string = resultSplittedStrings.get(7).replace("\"", "");
                String victim_count_string = resultSplittedStrings.get(8).replace("\"", "");
                String acu_assment_cd_string = resultSplittedStrings.get(9).replace("\"", "");

                // check if the analysis_time_string is already in the dataSlice2DMap.***
                DataSlice2D dataSlice2D = dataSlice2DMap.get(analysis_time_string);
                if(dataSlice2D == null)
                {
                    dataSlice2D = new DataSlice2D();
                    dataSlice2DMap.put(analysis_time_string, dataSlice2D);
                }

                DataGrid2D dataGrid2D = dataSlice2D.newDataGrid2D(grid_id_string);
                dataGrid2D.setGridId(grid_id_string);
                dataGrid2D.setNumber(no_string);
                dataGrid2D.setAccident_no(accident_no_string);
                dataGrid2D.setAnalysis_time(analysis_time_string);
                dataGrid2D.setConcentration(concentration_string);
                dataGrid2D.setAegl1_eval(aegl1_eval_string);
                dataGrid2D.setAegl2_eval(aegl2_eval_string);
                dataGrid2D.setAegl3_eval(aegl3_eval_string);
                dataGrid2D.setVictim_count(victim_count_string);
                dataGrid2D.setAcu_assment_cd(acu_assment_cd_string);

                counter += 1;
            }

            fr.close();
            br.close();

            int dataSlicesCount = dataSlice2DMap.size();
            for(Map.Entry<String, DataSlice2D> entry : dataSlice2DMap.entrySet())
            {
                String analysis_time_string = entry.getKey();
                DataSlice2D dataSlice2D = entry.getValue();

                dataSlice2D.setColumnsCount(150); // hard coded.***
                dataSlice2D.setRowsCount(150); // hard coded.***
                dataSlice2D.writeMatrixTxtFile(outputFolderPath + "\\" + analysis_time_string + ".TXT");
            }

        } catch (IOException e) {
            System.out.println("ERROR *** *** in ConvertMatrixAsciTxtToJson.***");
            e.printStackTrace();
        }
    }

    public void ConvertCsvTxtToMatrixTxtInFolder(String inputFolderPath, String outputFolderPath) {
        // 1rst, must know all fileNames in input folder.***
        ArrayList<String> vecExtensions = new ArrayList<>();
        vecExtensions.add("CSV");
        vecExtensions.add("csv");
        ArrayList<String> vecFileNamesInFolder = new ArrayList<>();
//        DataLayer currentDataLayer = this.dataContainer.getLastDataLayer();
//
        StringModifier.getFileNamesInFolder(inputFolderPath, vecExtensions, vecFileNamesInFolder);
        int filesCount = vecFileNamesInFolder.size();
        for (int i = 0; i < filesCount; i++) {
            String fileName = vecFileNamesInFolder.get(i);
            String rawFileName = StringModifier.getRawFileName(fileName);
            //currentDataLayer.addTimeSliceFileName(rawFileName);

            ConvertCsvTxtToMatrixTxt(inputFolderPath, outputFolderPath, fileName);
        }
    }

    public void ConvertMatrixAsciTxtToJsonByDataStructureFile(String inputDataStructurePath, String outputFolderPath)
    {
//        if(this.dataContainer == null)
//        {
//            this.dataContainer = new DataContainer();
//        }
//
//        // read the data structure file.***
//        // the dataStructure file is json format.***
//        ObjectMapper objectMapper = new ObjectMapper();
//        ObjectNode objectNodeRoot = null;
//        try {
//            objectNodeRoot = (ObjectNode) objectMapper.readTree(new File(inputDataStructurePath));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        // now, read the data.***
//        this.dataContainer.date = objectNodeRoot.get("date").asText();
//        ObjectNode centerGeoCoordNode = (ObjectNode) objectNodeRoot.get("centerGeographicCoord");
//        this.dataContainer.geoCoordCenterLongitudeDeg = centerGeoCoordNode.get("longitude").asDouble();
//        this.dataContainer.geoCoordCenterLatitudeDeg = centerGeoCoordNode.get("latitude").asDouble();
//        this.dataContainer.geoCoordCenterAltitude = centerGeoCoordNode.get("altitude").asDouble();
//        this.dataContainer.width_km = objectNodeRoot.get("width_km").asDouble();
//        this.dataContainer.height_km = objectNodeRoot.get("height_km").asDouble();
//
//        int layersCount = objectNodeRoot.get("layersCount").asInt();
//        ArrayNode objectLayersArrayNode = (ArrayNode) objectNodeRoot.get("layers");
//        for(int layer = 0; layer < layersCount; layer++)
//        {
//            ObjectNode objectLayersNode = (ObjectNode) objectLayersArrayNode.get(layer);
//            String folderPath = objectLayersNode.get("folderPath").asText();
//            String folderName = StringModifier.getLastNameFromPath(folderPath);
//            String layerOutputFolderPath = outputFolderPath + "\\" + folderName;
//
//            DataLayer dataLayer = new DataLayer();
//            dataLayer.minAltitude = objectLayersNode.get("minAltitude").asDouble();
//            dataLayer.maxAltitude = objectLayersNode.get("maxAltitude").asDouble();
//            dataLayer.folderName = folderName;
//            this.dataContainer.addDataLayer(dataLayer);
//
//            ConvertMatrixAsciTxtToJsonInFolder(folderPath, layerOutputFolderPath);
//
//            int hola = 0;
//        }

        int hola = 0;
    }
}
