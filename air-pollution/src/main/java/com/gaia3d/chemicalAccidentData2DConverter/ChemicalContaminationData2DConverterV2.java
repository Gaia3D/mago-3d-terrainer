package com.gaia3d.chemicalAccidentData2DConverter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gaia3d.chemicalAccidentDataConverter.DataContainer;
import com.gaia3d.chemicalAccidentDataConverter.DataLayer;
import com.gaia3d.chemicalAccidentDataConverter.DataSlice;
import com.gaia3d.chemicalAccidentDataConverter.PngsBinaryBlockData;
import com.gaia3d.image.Texture2D;
import com.gaia3d.utils.FileUtils;
import com.gaia3d.utils.GeometryUtils;
import com.gaia3d.utils.StringModifier;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ChemicalContaminationData2DConverterV2 {
    public DataContainer dataContainer = null;

    public void ConvertData2DByDataStructureFile(String inputDataStructurePath, String inputFolderPath, String outputFolderPath) throws IOException {
        if(this.dataContainer == null)
        {
            this.dataContainer = new DataContainer();
        }

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
        this.dataContainer.year = objectNodeRoot.get("year").asInt();
        this.dataContainer.month = objectNodeRoot.get("month").asInt();
        this.dataContainer.day = objectNodeRoot.get("day").asInt();
        this.dataContainer.hour = objectNodeRoot.get("hour").asInt();
        this.dataContainer.minute = objectNodeRoot.get("minute").asInt();
        this.dataContainer.second = objectNodeRoot.get("second").asInt();
        this.dataContainer.millisecond = objectNodeRoot.get("millisecond").asInt();

        ObjectNode centerGeoCoordNode = (ObjectNode) objectNodeRoot.get("centerGeographicCoord");
        this.dataContainer.geoCoordCenterLongitudeDeg = centerGeoCoordNode.get("longitude").asDouble();
        this.dataContainer.geoCoordCenterLatitudeDeg = centerGeoCoordNode.get("latitude").asDouble();
        this.dataContainer.geoCoordCenterAltitude = centerGeoCoordNode.get("altitude").asDouble();
        this.dataContainer.width_km = objectNodeRoot.get("width_km").asDouble();
        this.dataContainer.height_km = objectNodeRoot.get("height_km").asDouble();

        this.dataContainer.timeInterval = objectNodeRoot.get("timeInterval").asDouble();
        this.dataContainer.timeIntervalUnits = objectNodeRoot.get("timeIntervalUnits").asText();

        ArrayList<String> vecExtensions = new ArrayList<>();
        vecExtensions.add("TXT");

        int layersCount = objectNodeRoot.get("layersCount").asInt();
        ArrayNode objectLayersArrayNode = (ArrayNode) objectNodeRoot.get("layers");
        int timeSlicesCount = 0;
        for(int layer = 0; layer < layersCount; layer++)
        {
            ObjectNode objectLayersNode = (ObjectNode) objectLayersArrayNode.get(layer);
            String folderPath = objectLayersNode.get("folderPath").asText();
            String folderName = StringModifier.getLastNameFromPath(folderPath);
            //String layerOutputFolderPath = outputFolderPath + "\\" + folderName;

            DataLayer dataLayer = new DataLayer();
            dataLayer.minAltitude = objectLayersNode.get("minAltitude").asDouble();
            dataLayer.maxAltitude = objectLayersNode.get("maxAltitude").asDouble();
            dataLayer.folderName = folderName;
            this.dataContainer.addDataLayer(dataLayer);
            StringModifier.getFileNamesInFolder(folderPath, vecExtensions, dataLayer.timeSlicesFileNames);

            if(layer == 0)
            {
                timeSlicesCount = dataLayer.timeSlicesFileNames.size();
            }
            else
            {
                if(timeSlicesCount != dataLayer.timeSlicesFileNames.size())
                {
                    System.out.println("ERROR *** *** in ConvertDataByDataStructureFile.***");
                    return;
                }
            }
        }

        // Convert all .txt files in the input folder to .png files in the output folder.
        // create output folder if it does not exist.
        List<String> txtFileNamesArray = new ArrayList<>();
        com.gaia3d.utils.FileUtils.getFileNames(inputFolderPath, ".TXT", txtFileNamesArray);
        String tempPngFolderPath = outputFolderPath + File.separator + "temp";

        FileUtils.createAllFoldersIfNoExist(tempPngFolderPath);

        List<DataSlice> vecDataSlices = new ArrayList<>();
        List<String> pngPaths = new ArrayList<>();

        int filesCount = txtFileNamesArray.size();
        for(int i=0; i<filesCount; i++) {
            String txtFileName = txtFileNamesArray.get(i);
            String txtFilePath = inputFolderPath + File.separator + txtFileName;
            String pngFileName = txtFileName.replace(".TXT", ".png");
            String pngFilePath = tempPngFolderPath + File.separator + pngFileName;
            DataSlice resultDataSlice = new DataSlice();
            Texture2D resultTexture2d = new Texture2D();
            convertTxtToPng(txtFilePath, resultDataSlice, resultTexture2d);
            resultDataSlice.imagefileName = pngFileName;
            resultTexture2d.saveAsPNG(pngFilePath);

            // delete the data of the dataSlice to save memory.***
            resultDataSlice.values = null;

            vecDataSlices.add(resultDataSlice);
            pngPaths.add(pngFilePath);

            //this.dataContainer.addDataLayer(dataLayer);
        }


        // now make pngsBlocksBinary.***
        ArrayList<ArrayList<String>> resultPngsGroup = new ArrayList<>();
        double pngsBinaryBlockSizeLimit = 60.0; // MB.***
        long currentPngsBinaryBlockSize = 0;
        long pngsBinaryBlockSizeLimitBytes = (long) (pngsBinaryBlockSizeLimit * 1024.0 * 1024.0);
        getPngsGroupLimitedByMaxByteSize(pngPaths, pngsBinaryBlockSizeLimitBytes, resultPngsGroup);

        ArrayList<PngsBinaryBlockData> pngsBinDataArray = new ArrayList<>();

        int groupsCount = resultPngsGroup.size();
        for(int group = 0; group < groupsCount; group++)
        {
            String pngsBinaryBlockFileName = "pngsBinaryBlock_" + group + ".bin";
            currentPngsBinaryBlockSize = 0;
            ArrayList<String> mosaicTextureFilePathArray = resultPngsGroup.get(group);
            int pngsCount = mosaicTextureFilePathArray.size();
            try ( DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFolderPath + "\\" + pngsBinaryBlockFileName))) )
            {
                for(int j=0; j<pngsCount; j++)
                {
                    String mosaicTextureFilePath = mosaicTextureFilePathArray.get(j);
                    File mosaicTextureFile = new File(mosaicTextureFilePath);
                    long mosaicTextureFileSize = mosaicTextureFile.length();

                    PngsBinaryBlockData pngsBinData = new PngsBinaryBlockData();
                    pngsBinData.originalPngFileName = mosaicTextureFile.getName();
                    pngsBinData.pngsBinaryBlockDataFileName = pngsBinaryBlockFileName;
                    pngsBinData.startByteIndex = (int)currentPngsBinaryBlockSize;
                    pngsBinData.endByteIndex = (int)(currentPngsBinaryBlockSize + mosaicTextureFileSize);
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
                e.printStackTrace();
            }
        }

        // finally save the json index file from this.dataContainer.***
        String jsonIndexFilePath = outputFolderPath + "\\" + "JsonIndex2D.json";
        makeIndexFile(jsonIndexFilePath, outputFolderPath, pngsBinDataArray, vecDataSlices);

        int hola = 0;
        if(hola ==0 )
        {
            hola = 1;
        }
    }

    private void getPngsGroupLimitedByMaxByteSize(List<String> pngFilePaths, long maxByteSize, ArrayList<ArrayList<String>> resultPngsGroup)
    {
        int pngFilePathsCount = pngFilePaths.size();

        ArrayList<String> pngsGroup = new ArrayList<>();
        long currentPngsGroupSize = 0;
        for (int i = 0; i < pngFilePathsCount; i++) {
            String pngFilePath = pngFilePaths.get(i);
            File pngFile = new File(pngFilePath);
            long pngFileSize = pngFile.length();
            if (currentPngsGroupSize + pngFileSize > maxByteSize) {
                // save the current pngsGroup.***
                resultPngsGroup.add(pngsGroup);
                pngsGroup = new ArrayList<>();
                currentPngsGroupSize = 0;
            }
            pngsGroup.add(pngFilePath);
            currentPngsGroupSize += pngFileSize;
        }
        if (pngsGroup.size() > 0) {
            resultPngsGroup.add(pngsGroup);
        }
    }

    public void convertTxtToPng(String txtFilePath, DataSlice resultDataSlice, Texture2D resultTexture2d) throws IOException {
        // Convert the .txt file to a .png file.
        // Read the .txt file and write the content to the .png file.
        // 1rst, load the txt file into a DataSlice object.
        FileUtils.loadDataSlice(txtFilePath, resultDataSlice);

        // now convert the dataSlice to a png file.
        int colsCount = resultDataSlice.columnsCount;
        int rowsCount = resultDataSlice.rowsCount;
        double minValue = resultDataSlice.minValue;
        double maxValue = resultDataSlice.maxValue;
        double quantizedValue = 0.0;
        byte []encoded = new byte[4];

        resultTexture2d.setSize(colsCount, rowsCount);

        for(int col = 0; col < colsCount; col++) {
            for(int row = 0; row < rowsCount; row++) {
                double value = resultDataSlice.getValue(col, row);
                quantizedValue = (value - minValue) / (maxValue - minValue);
                GeometryUtils.encodeFloat((float)quantizedValue, encoded);
                resultTexture2d.setPixel(col, row, encoded[0], encoded[1], encoded[2], encoded[3]);
            }
        }

    }

    public void makeIndexFile(String outputFilePath,String outputFolderPath, ArrayList<PngsBinaryBlockData> pngsBinDataArray, List<DataSlice> vecDataSlices) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNodeRoot = objectMapper.createObjectNode();

        objectNodeRoot.put("year", this.dataContainer.year);
        objectNodeRoot.put("month", this.dataContainer.month);
        objectNodeRoot.put("day", this.dataContainer.day);
        objectNodeRoot.put("hour", this.dataContainer.hour);
        objectNodeRoot.put("minute", this.dataContainer.minute);
        objectNodeRoot.put("second", this.dataContainer.second);
        objectNodeRoot.put("millisecond", this.dataContainer.millisecond);

        int layersCount = this.dataContainer.getDataLayersCount();
        objectNodeRoot.put("layersCount", layersCount);

        ObjectNode centerGeoCoordNode = objectMapper.createObjectNode();
        centerGeoCoordNode.put("longitude", this.dataContainer.geoCoordCenterLongitudeDeg);
        centerGeoCoordNode.put("latitude", this.dataContainer.geoCoordCenterLatitudeDeg);
        centerGeoCoordNode.put("altitude", this.dataContainer.geoCoordCenterAltitude);
        objectNodeRoot.put("centerGeographicCoord", centerGeoCoordNode);

        objectNodeRoot.put("width_km", this.dataContainer.width_km);
        objectNodeRoot.put("height_km", this.dataContainer.height_km);

        objectNodeRoot.put("timeInterval", this.dataContainer.timeInterval);
        objectNodeRoot.put("timeIntervalUnits", this.dataContainer.timeIntervalUnits);

//        //ArrayNode mosaicTexMetaDataFileNamesArrayNode = objectMapper.createArrayNode();
//        ArrayNode mosaicTexMetaDataJsonArrayNode = objectMapper.createArrayNode();
//        int mosaicTexMetaDataFileNamesCount = this.dataContainer.mosaicTexMetaDataFileNames.size();
//        for(int i=0; i<mosaicTexMetaDataFileNamesCount; i++)
//        {
//            String mosaicTexMetaDataFileName = this.dataContainer.mosaicTexMetaDataFileNames.get(i);
//            //mosaicTexMetaDataFileNamesArrayNode.add(mosaicTexMetaDataFileName);
//
//            // load the jsonFile.***
//            String mosaicTexMetaDataFilePath = outputFolderPath + "\\" + mosaicTexMetaDataFileName;
//            ObjectNode mosaicTexMetaDataObjectNode = null;
//            try {
//                mosaicTexMetaDataObjectNode = (ObjectNode) objectMapper.readTree(new File(mosaicTexMetaDataFilePath));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            mosaicTexMetaDataJsonArrayNode.add(mosaicTexMetaDataObjectNode);
//        }
//        //objectNodeRoot.put("mosaicTexMetaDataFileNames", mosaicTexMetaDataFileNamesArrayNode);
//        objectNodeRoot.put("mosaicTexMetaDataFileNamesCount", mosaicTexMetaDataFileNamesCount);
//        objectNodeRoot.put("mosaicTexMetaDataJsonArray", mosaicTexMetaDataJsonArrayNode);

        // save data slices of the layer.***************************************************************************
        // we use only the 1rst layer.***
        ArrayNode layersArrayNode = objectMapper.createArrayNode();
        DataLayer dataLayer = this.dataContainer.getDataLayer(0);
        ObjectNode layerObjectNode = objectMapper.createObjectNode();
        layerObjectNode.put("folderName", dataLayer.folderName);
        layerObjectNode.put("minAltitude", dataLayer.minAltitude);
        layerObjectNode.put("maxAltitude", dataLayer.maxAltitude);

        ArrayNode timeSlicesArrayNode = objectMapper.createArrayNode();
        int timeSlicesCount = vecDataSlices.size();
        double totalMinValue = 0.0;
        double totalMaxValue = 0.0;
        for(int timeSlice = 0; timeSlice < timeSlicesCount; timeSlice++)
        {
            DataSlice dataSlice = vecDataSlices.get(timeSlice);
            ObjectNode timeSliceObjectNode = objectMapper.createObjectNode();
            timeSliceObjectNode.put("minValue", dataSlice.minValue);
            timeSliceObjectNode.put("maxValue", dataSlice.maxValue);
            timeSliceObjectNode.put("width", dataSlice.columnsCount);
            timeSliceObjectNode.put("height", dataSlice.rowsCount);
            timeSliceObjectNode.put("minAltitude", dataSlice.minAltitude);
            timeSliceObjectNode.put("maxAltitude", dataSlice.maxAltitude);
            timeSliceObjectNode.put("fileName", dataSlice.fileName);
            timeSliceObjectNode.put("imagefileName", dataSlice.imagefileName);

            timeSlicesArrayNode.add(timeSliceObjectNode);

            if(timeSlice == 0)
            {
                totalMinValue = dataSlice.minValue;
                totalMaxValue = dataSlice.maxValue;
            }
            else {
                if (dataSlice.minValue < totalMinValue) {
                    totalMinValue = dataSlice.minValue;
                }
                if (dataSlice.maxValue > totalMaxValue) {
                    totalMaxValue = dataSlice.maxValue;
                }
            }
        }
        layerObjectNode.put("totalMinValue", totalMinValue);
        layerObjectNode.put("totalMaxValue", totalMaxValue);
        layerObjectNode.put("timeSlicesCount", timeSlicesCount);
        layerObjectNode.put("timeSlices", timeSlicesArrayNode);


        layersArrayNode.add(layerObjectNode);


        objectNodeRoot.put("layers", layersArrayNode);
        // end data slices.----------------------------------------------------------------------------------------

        // now write the pngsBinaryBlockData.***
        HashMap<String, Integer> pngsBinDataMap = new HashMap<>();
        ArrayNode pngsBinDataArrayNode = objectMapper.createArrayNode();
        int pngsBinDataArrayCount = pngsBinDataArray.size();
        for(int i=0; i<pngsBinDataArrayCount; i++)
        {
            PngsBinaryBlockData pngsBinData = pngsBinDataArray.get(i);
            ObjectNode pngsBinDataObjectNode = objectMapper.createObjectNode();
            pngsBinDataObjectNode.put("originalPngFileName", pngsBinData.originalPngFileName);
            pngsBinDataObjectNode.put("pngsBinaryBlockDataFileName", pngsBinData.pngsBinaryBlockDataFileName);
            pngsBinDataObjectNode.put("startByteIndex", pngsBinData.startByteIndex);
            pngsBinDataObjectNode.put("endByteIndex", pngsBinData.endByteIndex);
            pngsBinDataArrayNode.add(pngsBinDataObjectNode);

            pngsBinDataMap.put(pngsBinData.pngsBinaryBlockDataFileName, i);
        }
        objectNodeRoot.put("pngsBinDataArray", pngsBinDataArrayNode);

        // now save the pngsBinaryBlockDataFileNameMap.***
        ArrayNode pngsBinDataMapArrayNode = objectMapper.createArrayNode();
        Set<String> keys = pngsBinDataMap.keySet();
        for(String key : keys)
        {
            ObjectNode pngsBinDataMapObjectNode = objectMapper.createObjectNode();
            pngsBinDataMapObjectNode.put("fileName", key);
            pngsBinDataMapArrayNode.add(pngsBinDataMapObjectNode);
        }

        objectNodeRoot.put("pngsBinBlockFileNames", pngsBinDataMapArrayNode);

        // Save the json index file.***
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(objectNodeRoot.toString());
            objectMapper.writeValue(new File(outputFilePath), jsonNode);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
