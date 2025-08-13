package com.gaia3d.chemicalAccidentDataConverter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gaia3d.basic.geometry.jgltf.GltfWriter;
import com.gaia3d.basic.geometry.voxel.VoxelCP;
import com.gaia3d.basic.geometry.voxel.VoxelCPGrid3D;
import com.gaia3d.basic.legend.LegendColors;
import com.gaia3d.basic.marchingcube.MarchingCube;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.geometry.BoundingBox;
import com.gaia3d.image.Texture2D;
import com.gaia3d.image.TextureUtils;
import com.gaia3d.utils.GeometryUtils;
import com.gaia3d.utils.StringModifier;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
public class ChemicalContaminationDataConverter
{
    // The input data file format is *.TXT.***
    public DataContainer dataContainer = null;

    public void MakeIndexFile(String outputFilePath,String outputFolderPath, ArrayList<PngsBinaryBlockData> pngsBinDataArray) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNodeRoot = objectMapper.createObjectNode();

        objectNodeRoot.put("year", this.dataContainer.year);
        objectNodeRoot.put("month", this.dataContainer.month);
        objectNodeRoot.put("day", this.dataContainer.day);
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

        // save totalMinValue & totalMaxValue.***
        objectNodeRoot.put("totalMinValue", this.dataContainer.totalMinValue);
        objectNodeRoot.put("totalMaxValue", this.dataContainer.totalMaxValue);

        //ArrayNode mosaicTexMetaDataFileNamesArrayNode = objectMapper.createArrayNode();
        ArrayNode mosaicTexMetaDataJsonArrayNode = objectMapper.createArrayNode();
        int mosaicTexMetaDataFileNamesCount = this.dataContainer.mosaicTexMetaDataFileNames.size();
        for(int i=0; i<mosaicTexMetaDataFileNamesCount; i++)
        {
            String mosaicTexMetaDataFileName = this.dataContainer.mosaicTexMetaDataFileNames.get(i);
            //mosaicTexMetaDataFileNamesArrayNode.add(mosaicTexMetaDataFileName);

            // load the jsonFile.***
            String mosaicTexMetaDataFilePath = outputFolderPath + "\\" + mosaicTexMetaDataFileName;
            ObjectNode mosaicTexMetaDataObjectNode = null;
            try {
                mosaicTexMetaDataObjectNode = (ObjectNode) objectMapper.readTree(new File(mosaicTexMetaDataFilePath));
            } catch (IOException e) {
                e.printStackTrace();
            }
            mosaicTexMetaDataJsonArrayNode.add(mosaicTexMetaDataObjectNode);
        }
        //objectNodeRoot.put("mosaicTexMetaDataFileNames", mosaicTexMetaDataFileNamesArrayNode);
        objectNodeRoot.put("mosaicTexMetaDataFileNamesCount", mosaicTexMetaDataFileNamesCount);
        objectNodeRoot.put("mosaicTexMetaDataJsonArray", mosaicTexMetaDataJsonArrayNode);

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

    public void ConvertDataByDataStructureFile(String inputDataStructurePath, String inputFolderPath, String outputFolderPath) throws IOException {
        /*{
        // inputDataStructure sample JSON file.***
	"year": 2024,
	"month": 2,
	"day": 6,
	"hour": 11,
	"minute": 0,
	"second": 0,
	"millisecond": 0,
	"centerGeographicCoord": {
		"longitude": 126.65403123232736,
		"latitude": 36.90329299539047,
		"altitude": 0.0
	},
	"width_km": 15.0,
	"height_km": 15.0,
	"timeInterval": 1,
	"timeIntervalUnits": "minute",
	"layersCount": 8,
	"layers": [
		{
			"minAltitude": 0.0,
			"maxAltitude": 10.0,
			"folderPath": "D:\\data\\simulation-data\\CHEMICAL_CONTAMINATION\\(20240806) 화학사고 Concentration\\1minute_interval\\Air1"
		},
		{
			"minAltitude": 10.0,
			"maxAltitude": 20.0,
			"folderPath": "D:\\data\\simulation-data\\CHEMICAL_CONTAMINATION\\(20240806) 화학사고 Concentration\\1minute_interval\\Air2"
		},
		{
			"minAltitude": 20.0,
			"maxAltitude": 40.0,
			"folderPath": "D:\\data\\simulation-data\\CHEMICAL_CONTAMINATION\\(20240806) 화학사고 Concentration\\1minute_interval\\Air3"
		},
		{
			"minAltitude": 40.0,
			"maxAltitude": 60.0,
			"folderPath": "D:\\data\\simulation-data\\CHEMICAL_CONTAMINATION\\(20240806) 화학사고 Concentration\\1minute_interval\\Air4"
		},
		{
			"minAltitude": 60.0,
			"maxAltitude": 80.0,
			"folderPath": "D:\\data\\simulation-data\\CHEMICAL_CONTAMINATION\\(20240806) 화학사고 Concentration\\1minute_interval\\Air5"
		},
		{
			"minAltitude": 80.0,
			"maxAltitude": 100.0,
			"folderPath": "D:\\data\\simulation-data\\CHEMICAL_CONTAMINATION\\(20240806) 화학사고 Concentration\\1minute_interval\\Air6"
		},
		{
			"minAltitude": 100.0,
			"maxAltitude": 1000.0,
			"folderPath": "D:\\data\\simulation-data\\CHEMICAL_CONTAMINATION\\(20240806) 화학사고 Concentration\\1minute_interval\\Air7"
		},
		{
			"minAltitude": 1000.0,
			"maxAltitude": 3000.0,
			"folderPath": "D:\\data\\simulation-data\\CHEMICAL_CONTAMINATION\\(20240806) 화학사고 Concentration\\1minute_interval\\Air8"
		}
	],
	"legend" : [
		{
			"index" : 0,
			"value" : 0.012,
			"rgba" : [0.0, 0.0, 0.0, 1.0],
		},
		{
			"index" : 1,
			"value" : 0.012,
			"rgba" : [0.0, 0.0, 0.0, 1.0],
		}
	]
}*/
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

        // check if it has "legend" node.***
        JsonNode legendNode = objectNodeRoot.get("legend");
        if(legendNode != null && legendNode.isArray()) {
            LegendColors legendColors = this.dataContainer.legendColors;
            ArrayNode legendArrayNode = (ArrayNode) legendNode;
            int legendCount = legendArrayNode.size();
            for(int i=0; i<legendCount; i++) {
                ObjectNode legendObjectNode = (ObjectNode) legendArrayNode.get(i);
                int index = legendObjectNode.get("index").asInt();
                double value = legendObjectNode.get("value").asDouble();
                ArrayNode rgbaArrayNode = (ArrayNode) legendObjectNode.get("rgba");
                double r, g, b, a;
                r = rgbaArrayNode.get(0).asDouble();
                g = rgbaArrayNode.get(1).asDouble();
                b = rgbaArrayNode.get(2).asDouble();
                a = rgbaArrayNode.get(3).asDouble();

                legendColors.setValueAndColor(value, r, g, b, a);
            }
        }

        ArrayList<String> vecExtensions = new ArrayList<>();
        vecExtensions.add("TXT");

        int layersCount = objectNodeRoot.get("layersCount").asInt();
        ArrayNode objectLayersArrayNode = (ArrayNode) objectNodeRoot.get("layers");
        int timeSlicesCount = 0;
        log.info("layersCount: {}", layersCount);
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

        // now, convert the data.***
        ArrayList<DataSlice> vecDataSlices = new ArrayList<>();
        double[] layerMinMaxValues = new double[2];
        double totalMinValue = 0;
        double totalMaxValue = 0;
        int[] mosaicColRow = new int[2];
        ArrayList<String> mosaicTexturesFilePaths = new ArrayList<>();
        log.info("timeSlicesCount: " + timeSlicesCount);
        for(int i=0; i<timeSlicesCount; i++)
        {
            log.info("timeSlice: " + i + " / " + timeSlicesCount);
            // load the same slice of all layers.***
            vecDataSlices.clear();
            for(int layer = 0; layer < layersCount; layer++)
            {
                DataLayer dataLayer = this.dataContainer.getDataLayer(layer);
                String layerFolderName = dataLayer.folderName;
                String timeSliceFileName = dataLayer.getTimeSliceFileName(i);

                // load the timeSlice.***
                String timeSliceFilePath = inputFolderPath + "\\" + layerFolderName + "\\" + timeSliceFileName;
                DataSlice dataSlice = new DataSlice();
                try {
                    loadDataSlice(timeSliceFilePath, dataSlice);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // set some layer data.***
                dataSlice.minAltitude = dataLayer.minAltitude;
                dataSlice.maxAltitude = dataLayer.maxAltitude;
                vecDataSlices.add(dataSlice);
                int hola = 0;
            }

            // now, make the mosaicTexture with vecDataSlices.***
            String rawFileName = StringModifier.getRawFileName(vecDataSlices.get(0).fileName);
            String mosaicTextureFileName = rawFileName + "_mosaicTexture.png";
            String mosaicTextureFilePath = outputFolderPath + "\\" + mosaicTextureFileName;
            Texture2D resultMosaicTexture = new Texture2D();
            makeMosaicTexture(vecDataSlices, resultMosaicTexture, layerMinMaxValues, mosaicColRow); // original.***

            if(i == 0)
            {
                totalMinValue = layerMinMaxValues[0];
                totalMaxValue = layerMinMaxValues[1];
            }
            else
            {
                if(totalMinValue > layerMinMaxValues[0])
                {
                    totalMinValue = layerMinMaxValues[0];
                }

                if(totalMaxValue < layerMinMaxValues[1])
                {
                    totalMaxValue = layerMinMaxValues[1];
                }
            }

            StringModifier.createFolderIfNoExists(Paths.get(outputFolderPath));
            resultMosaicTexture.saveAsPNG(mosaicTextureFilePath);
            mosaicTexturesFilePaths.add(mosaicTextureFilePath); // store the mosaicTextureFilePath.***

            // save png's metadata json file.***
            String mosaicTextureMetadataFileName = rawFileName + "_mosaicTextureMetadata.json";
            String mosaicTextureMetadataFilePath = outputFolderPath + "\\" + mosaicTextureMetadataFileName;
            ObjectNode mosaicTextureMetadataObjectNode = objectMapper.createObjectNode();
            mosaicTextureMetadataObjectNode.put("minValue", layerMinMaxValues[0]);
            mosaicTextureMetadataObjectNode.put("maxValue", layerMinMaxValues[1]);
            mosaicTextureMetadataObjectNode.put("width", resultMosaicTexture.width);
            mosaicTextureMetadataObjectNode.put("height", resultMosaicTexture.height);
            mosaicTextureMetadataObjectNode.put("mosaicTextureFileName", mosaicTextureFileName);
            mosaicTextureMetadataObjectNode.put("mosaicColumnsCount", mosaicColRow[0]);
            mosaicTextureMetadataObjectNode.put("mosaicRowsCount", mosaicColRow[1]);

            // Embed the mosaicTexture byteData.***
            boolean embedMosaicTextureByteData = false; // debug purposes.***
            if(embedMosaicTextureByteData) {
                ArrayNode mosaicTextureByteDataArrayNode = objectMapper.createArrayNode();
                int dataLength = resultMosaicTexture.data.length;
                for (int j = 0; j < dataLength; j++) {
                    mosaicTextureByteDataArrayNode.add(resultMosaicTexture.data[j]);
                }
                mosaicTextureMetadataObjectNode.put("byteData", mosaicTextureByteDataArrayNode);
            }

            ArrayNode mosaicTextureMetadataArrayNode = objectMapper.createArrayNode();
            int slicesCount = vecDataSlices.size();
            for(int slice = 0; slice < slicesCount; slice++)
            {
                DataSlice dataSlice = vecDataSlices.get(slice);
                ObjectNode dataSliceObjectNode = objectMapper.createObjectNode();
                dataSliceObjectNode.put("minValue", dataSlice.minValue);
                dataSliceObjectNode.put("maxValue", dataSlice.maxValue);
                dataSliceObjectNode.put("width", dataSlice.columnsCount);
                dataSliceObjectNode.put("height", dataSlice.rowsCount);
                dataSliceObjectNode.put("minAltitude", dataSlice.minAltitude);
                dataSliceObjectNode.put("maxAltitude", dataSlice.maxAltitude);
                dataSliceObjectNode.put("fileName", dataSlice.fileName);
                mosaicTextureMetadataArrayNode.add(dataSliceObjectNode);
            }

            mosaicTextureMetadataObjectNode.put("dataSlices", mosaicTextureMetadataArrayNode);

            try {
                JsonNode jsonNode = new ObjectMapper().readTree(mosaicTextureMetadataObjectNode.toString());
                objectMapper.writeValue(new File(mosaicTextureMetadataFilePath), jsonNode);
                this.dataContainer.addMosaicTexMetaDataFileName(mosaicTextureMetadataFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }

            int hola = 0;
        }

        this.dataContainer.totalMinValue = totalMinValue;
        this.dataContainer.totalMaxValue = totalMaxValue;

        // Marching cubes.*******************************************************************
        int isoValuesCount = 10;
        double[] isoValuesArray = new double[isoValuesCount];

        // make isoValuesArray.***
        double isoValuesIncrement = (totalMaxValue - totalMinValue) / (double) (isoValuesCount - 1);
        for(int i = 0; i < isoValuesCount; i++) {
            isoValuesArray[i] = totalMinValue + (double) (i) * isoValuesIncrement;
        }

        /*
        0 = 0.0
1 = 1.2444444444444444E7
2 = 2.4888888888888888E7
3 = 3.733333333333333E7
4 = 4.9777777777777776E7
5 = 6.2222222222222224E7
6 = 7.466666666666666E7
7 = 8.71111111111111E7
8 = 9.955555555555555E7
9 = 1.12E8
         */

        TreeMap<Double, DataSlice> treeMapVolumeData = new TreeMap<Double, DataSlice>(); // minAltitude is the key.***

        log.info("making marching cubes: " + timeSlicesCount);
        for(int i=0; i<timeSlicesCount; i++) {
            log.info("marching cube timeSlice: " + i + " / " + timeSlicesCount);
            // load the same slice of all layers.***
            for (int layer = 0; layer < layersCount; layer++) {
                DataLayer dataLayer = this.dataContainer.getDataLayer(layer);
                String layerFolderName = dataLayer.folderName;
                String timeSliceFileName = dataLayer.getTimeSliceFileName(i);

                // load the timeSlice.***
                String timeSliceFilePath = inputFolderPath + "\\" + layerFolderName + "\\" + timeSliceFileName;
                DataSlice dataSlice = new DataSlice();
                try {
                    loadDataSlice(timeSliceFilePath, dataSlice);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // set some layer data.***
                dataSlice.minAltitude = dataLayer.minAltitude;
                dataSlice.maxAltitude = dataLayer.maxAltitude;

                treeMapVolumeData.put(dataSlice.minAltitude, dataSlice); // store the dataSlice by minAltitude.***
                int hola = 0;
            }

            convertDataByDateMarchingCubes(treeMapVolumeData, outputFolderPath, isoValuesArray, i);
        }

        // now save indexJson file.
        log.info("Saving index.json file.");
        this.saveIndexJsonFileMC(outputFolderPath);
        // end Marching cubes.*******************************************************************

        // now, with the stored mosaicTexturesFilePaths, make the mosaicTexture's pngsBinaryBlock.***
        // The pngsBinaryBlock is a binary file that contains all mosaicTextures, limited to 60MB.***
        ArrayList<PngsBinaryBlockData> pngsBinDataArray = new ArrayList<>();
        makePngsBinaryBlocks(mosaicTexturesFilePaths, outputFolderPath, pngsBinDataArray);

        // finally, save the JSON index file from this.dataContainer.***
        String jsonIndexFilePath = outputFolderPath + "\\" + "JsonIndex.json";
        MakeIndexFile(jsonIndexFilePath, outputFolderPath, pngsBinDataArray);

        // delete the temp files.******************************************************************************
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

    private void convertDataByDateMarchingCubes(TreeMap<Double, DataSlice> treeMapVolumeData, String outputFolderPath, double[] isoValuesArray, int idx){
        boolean addLastTopSlice = true; // if true, add the last top slice with values zero at maxAltitude of the slice.***
        VoxelCPGrid3D voxelCPGrid3D = makeVoxelCPGrid3D(treeMapVolumeData, addLastTopSlice);

        if(voxelCPGrid3D == null){
            log.info("voxelCPGrid3D is null");
            return;
        }

        // calculate minMaxValues that is contained in the treeMapVolumeData.***
        GaiaScene gaiaSceneMaster;
        double[] minMaxValues = voxelCPGrid3D.getMinMaxValues();
        LegendColors legendColors = this.dataContainer.legendColors;
        if(legendColors.getColorMap().isEmpty()){
            // test mode.***
            gaiaSceneMaster = MarchingCube.makeGaiaSceneOnion(voxelCPGrid3D, isoValuesArray);
        }
        else{
            gaiaSceneMaster = MarchingCube.makeGaiaSceneOnion(voxelCPGrid3D, isoValuesArray, legendColors);
        }

        if(gaiaSceneMaster != null) {
            GltfWriter gltfWriter = new GltfWriter();
            String glbFileName = "chemicalAccident_" + idx + ".glb";

            // set the glbFileName to airPollutionVolume.
            //airPollutionVolume.setGlbFileName(glbFileName);

            // save the glb file.
            String glbFilePath = outputFolderPath + glbFileName;
            gltfWriter.writeGlb(gaiaSceneMaster, glbFilePath);

            // save the glbMetaData.
            String jsonFileName = "chemicalAccident_" + idx + ".json";
            String jsonFilePath = outputFolderPath + File.separator + jsonFileName;
            this.saveAsJsonMC(jsonFilePath, idx, glbFileName, minMaxValues);
            this.dataContainer.getGlbMetaDataFileNames().add(jsonFileName);
        }
    }

    public void saveAsJsonMC(String jsonFilePath, int idx, String glbFileName, double[] minMaxValues) {
        String date = this.dataContainer.getYYYYMMDDHHmmss(idx);

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode mosaicTextureMetadataObjectNode = objectMapper.createObjectNode();
        mosaicTextureMetadataObjectNode.put("date", date);
        mosaicTextureMetadataObjectNode.put("idx", idx);
        mosaicTextureMetadataObjectNode.put("minValue", minMaxValues[0]);
        mosaicTextureMetadataObjectNode.put("maxValue", minMaxValues[1]);
        mosaicTextureMetadataObjectNode.put("glbFileName", glbFileName);

        try {
            JsonNode jsonNode = new ObjectMapper().readTree(mosaicTextureMetadataObjectNode.toString());
            objectMapper.writeValue(new File(jsonFilePath), jsonNode);
        } catch (IOException e) {
            log.error("", e);
        }
    }

    private void saveIndexJsonFileMC(String outputFolderPath) {
        // save the index.json file.
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNodeRoot = objectMapper.createObjectNode();
        // centerGeographicCoords.
        ObjectNode objectCenterGeographicCoordsNode = objectMapper.createObjectNode();
        objectNodeRoot.set("centerGeographicCoord", objectCenterGeographicCoordsNode);
        objectCenterGeographicCoordsNode.put("longitude", this.dataContainer.geoCoordCenterLongitudeDeg);
        objectCenterGeographicCoordsNode.put("latitude", this.dataContainer.geoCoordCenterLatitudeDeg);
        objectCenterGeographicCoordsNode.put("altitude", this.dataContainer.geoCoordCenterAltitude);

        // date as YYYYMMDD + "T" + hhmmss.
        String dateString = String.format("%04d%02d%02dT%02d%02d%02d", this.dataContainer.year, this.dataContainer.month, this.dataContainer.day,
                this.dataContainer.hour, this.dataContainer.minute, this.dataContainer.second);

        objectNodeRoot.put("startDate", dateString);

        // height_km, width_km.
//        BoundingBox bbox = new BoundingBox();
//        this.dataContainer.getGeoCoordBoundingBox(bbox);
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

        // glbMetaData.
        List<String> glbMetaDataFileNames = (List<String>) this.dataContainer.getGlbMetaDataFileNames();
        ArrayNode glbMetaDataFileNamesArrayNode = objectMapper.createArrayNode();
        int glbMetaDataFileNamesCount = glbMetaDataFileNames.size();
        for (int i = 0; i < glbMetaDataFileNamesCount; i++) {
            String glbMetaDataFileName = glbMetaDataFileNames.get(i);

            // load the jsonFile.
            String glbMetaDataFilePath = outputFolderPath + File.separator + glbMetaDataFileName;
            ObjectNode glbMetaDataObjectNode = null;
            try {
                glbMetaDataObjectNode = (ObjectNode) objectMapper.readTree(new File(glbMetaDataFilePath));

                // now, delete the glbMetaDataFile.***
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

    private VoxelCPGrid3D makeVoxelCPGrid3D(TreeMap<Double, DataSlice> treeMapVolumeData, boolean addLastTopSlice){
        // This function makes a VoxelCPGrid3D from vecDataSlices.***
        int dataSlicesCount = treeMapVolumeData.size();
        if(dataSlicesCount == 0) {
            return null;
        }

        // calculate the minMaxValues from the treeMapVolumeData.***
        double[] minMaxValues = new double[2];
        for(Double key : treeMapVolumeData.keySet()) {
            DataSlice dataSlice = treeMapVolumeData.get(key);
            if (dataSlice.minValue < minMaxValues[0] || minMaxValues[0] == 0.0) {
                minMaxValues[0] = dataSlice.minValue;
            }
            if (dataSlice.maxValue > minMaxValues[1]) {
                minMaxValues[1] = dataSlice.maxValue;
            }
        }

        Set<Double> keys = treeMapVolumeData.keySet();

        int slicesCount = dataSlicesCount;
        DataSlice firstDataSlice = treeMapVolumeData.get(keys.iterator().next());
        int texHeight = firstDataSlice.rowsCount;
        int texWidth = firstDataSlice.columnsCount;

        // create a new VoxelCPGrid3D object with the specified dimensions.
        int addingSliceCount = addLastTopSlice ? 1 : 0;
        VoxelCPGrid3D resultVoxelCPGrid3D = new VoxelCPGrid3D(texWidth, texHeight, slicesCount + addingSliceCount);

//        double[] minMaxValues = new double[2];
//        minMaxValues[0] = this.dataContainer.totalMinValue;
//        minMaxValues[1] = this.dataContainer.totalMaxValue;

        resultVoxelCPGrid3D.setMinMaxValues(minMaxValues);

        double width_km = this.dataContainer.width_km;
        double height_km = this.dataContainer.height_km;
        double width = width_km * 1000.0; // convert to meters.
        double height = height_km * 1000.0; // convert to meters.

        double minX = -width / 2.0; // center at (0,0).
        double minY = -height / 2.0; // center at (0,0).
        double increX = width / (double) texWidth; // increment in x direction.
        double increY = height / (double) texHeight; // increment in y direction.

//        double totalMinValueOfVolume = minMaxValues[0];
//        double totalMaxValueOfVolume = minMaxValues[1];
        //double totalValuesRange = totalMaxValueOfVolume - totalMinValueOfVolume;


        List<Double> keysList = new ArrayList<>(keys);

        double minValue = 0.0;
        for (int i = 0; i < slicesCount; i++) {

            DataSlice sliceData = treeMapVolumeData.get(keysList.get(i));
            int sliceColCount = sliceData.columnsCount;
            int sliceRowCount = sliceData.rowsCount;
            //BoundingBox sliceBBox = this.getGeoCoordBBox();
            //BoundingBox sliceBBox = new BoundingBox(); // this is a placeholder, you should replace it with the actual bounding box of the slice.
//            double minLonDeg = sliceBBox.minX;
//            double minLatDeg = sliceBBox.minY;
//            double minAlt = sliceData.minAltitude;
            double maxAlt = sliceData.maxAltitude;
//            double maxLonDeg = sliceBBox.maxX;
//            double maxLatDeg = sliceBBox.maxY;

//            double lonDegRange = maxLonDeg - minLonDeg;
//            double latDegRange = maxLatDeg - minLatDeg;

//            double lonDegStep = lonDegRange / sliceColCount;
//            double latDegStep = latDegRange / sliceRowCount;

            // the altitude is constant for each slice.

            int sliceZ = i;
            for (int row = 0; row < sliceRowCount; row++) {
                for (int col = 0; col < sliceColCount; col++) {
                    double value = sliceData.getValue(col, row);
                    VoxelCP voxelCp = resultVoxelCPGrid3D.getVoxel(col, row, sliceZ);
                    voxelCp.setValue(value);
                    double localPosX = minX + col * increX;
                    double localPosY = minY + row * increY;
                    voxelCp.getPosition().set(localPosX, localPosY, maxAlt);
                    int hola = 0;
                }
            }

            if(addLastTopSlice) {
                if (i == slicesCount - 1) {
                    sliceZ = slicesCount;
                    // add the last top z slice with values zero at maxAltitude of the slice.
                    for (int row = 0; row < sliceRowCount; row++) {
                        for (int col = 0; col < sliceColCount; col++) {
                            double value = 0.0;
                            VoxelCP voxelCp = resultVoxelCPGrid3D.getVoxel(col, row, sliceZ);
                            voxelCp.setValue(value);
                            double localPosX = minX + col * increX;
                            double localPosY = minY + row * increY;
                            voxelCp.getPosition().set(localPosX, localPosY, maxAlt);
                            int hola = 0;
                        }
                    }
                }
            }
        }
        return resultVoxelCPGrid3D;
    }

    private void getPngsGroupLimitedByMaxByteSize(ArrayList<String> mosaicTexturesFilePaths, long maxByteSize, ArrayList<ArrayList<String>> resultPngsGroup)
    {
        int mosaicTexturesFilePathsCount = mosaicTexturesFilePaths.size();
        {
            ArrayList<String> pngsGroup = new ArrayList<>();
            long currentPngsGroupSize = 0;
            for (int i = 0; i < mosaicTexturesFilePathsCount; i++) {
                String mosaicTextureFilePath = mosaicTexturesFilePaths.get(i);
                File mosaicTextureFile = new File(mosaicTextureFilePath);
                long mosaicTextureFileSize = mosaicTextureFile.length();
                if (currentPngsGroupSize + mosaicTextureFileSize > maxByteSize) {
                    // save the current pngsGroup.***
                    resultPngsGroup.add(pngsGroup);
                    pngsGroup = new ArrayList<>();
                    currentPngsGroupSize = 0;
                }
                pngsGroup.add(mosaicTextureFilePath);
                currentPngsGroupSize += mosaicTextureFileSize;
            }
            if (pngsGroup.size() > 0) {
                resultPngsGroup.add(pngsGroup);
            }
        }
    }

    public void makePngsBinaryBlocks(ArrayList<String> mosaicTexturesFilePaths, String outputFolderPath, ArrayList<PngsBinaryBlockData> pngsBinDataArray)
    {
        // This function makes a pngsBinaryBlock.***
        // The pngsBinaryBlock is a binary file that contains all mosaicTextures, limited to 60MB.***
        double pngsBinaryBlockSizeLimit = 60.0; // MB.***
        long currentPngsBinaryBlockSize = 0;
        long pngsBinaryBlockSizeLimitBytes = (long) (pngsBinaryBlockSizeLimit * 1024.0 * 1024.0);
        ArrayList<ArrayList<String>> pngsGroups = new ArrayList<>();
        getPngsGroupLimitedByMaxByteSize(mosaicTexturesFilePaths, pngsBinaryBlockSizeLimitBytes, pngsGroups);
        int groupsCount = pngsGroups.size();
        log.info("making pngsBinaryBlocks...");
        log.info("Total groups: " + groupsCount + ", pngsGroups.size(): " + pngsGroups.size() + ", mosaicTexturesFilePaths.size(): " + mosaicTexturesFilePaths.size());
        for(int group = 0; group < groupsCount; group++)
        {
            String pngsBinaryBlockFileName = "pngsBinaryBlock_" + group + ".bin";
            currentPngsBinaryBlockSize = 0;
            ArrayList<String> mosaicTextureFilePathArray = pngsGroups.get(group);
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
    }

    public void writeExampleDataStructureJson(String outputFilePath)
    {
        // This function writes an example of dataStructure json file.***
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNodeRoot = objectMapper.createObjectNode();

        objectNodeRoot.put("year", 2023);
        objectNodeRoot.put("month", 04);
        objectNodeRoot.put("day", 10);
        objectNodeRoot.put("hour", 12);
        objectNodeRoot.put("minute", 0);
        objectNodeRoot.put("second", 0);
        objectNodeRoot.put("millisecond", 0);

        ObjectNode centerGeoCoordNode = objectMapper.createObjectNode();
        centerGeoCoordNode.put("longitude", 126.65403123232736);
        centerGeoCoordNode.put("latitude", 36.90329299539047);
        centerGeoCoordNode.put("altitude", 0.0);
        objectNodeRoot.put("centerGeographicCoord", centerGeoCoordNode);

        objectNodeRoot.put("width_km", 15.0);
        objectNodeRoot.put("height_km", 15.0);
        objectNodeRoot.put("timeInterval", 1);
        objectNodeRoot.put("timeIntervalUnits", "minute");

        int layersCount = 8;
        objectNodeRoot.put("layersCount", layersCount);

        ArrayNode layersArrayNode = objectMapper.createArrayNode();
        for(int layer = 0; layer < layersCount; layer++)
        {
            ObjectNode objectLayersNode = objectMapper.createObjectNode();
            double minAltitude = 0.0;
            double maxAltitude = 10.0;
            if(layer == 0)
            {
                minAltitude = 0.0;
                maxAltitude = 10.0;
            }
            else if(layer == 1)
            {
                minAltitude = 10.0;
                maxAltitude = 20.0;
            }
            else if(layer == 2)
            {
                minAltitude = 20.0;
                maxAltitude = 40.0;
            }
            else if(layer == 3)
            {
                minAltitude = 40.0;
                maxAltitude = 60.0;
            }
            else if(layer == 4)
            {
                minAltitude = 60.0;
                maxAltitude = 80.0;
            }
            else if(layer == 5)
            {
                minAltitude = 80.0;
                maxAltitude = 100.0;
            }
            else if(layer == 6)
            {
                minAltitude = 100.0;
                maxAltitude = 1000.0;
            }
            else if(layer == 7)
            {
                minAltitude = 1000.0;
                maxAltitude = 3000.0;
            }
            objectLayersNode.put("minAltitude", minAltitude);
            objectLayersNode.put("maxAltitude", maxAltitude);
            String folderName = "Air" + (layer + 1);
            objectLayersNode.put("folderPath", "D:\\data\\simulation-data\\CHEMICAL_CONTAMINATION\\Concentration(26H)\\1minute_interval\\" + folderName);
            layersArrayNode.add(objectLayersNode);
        }

        objectNodeRoot.put("layers", layersArrayNode);

        // Save the json index file.***
        try {
            JsonNode jsonNode = new ObjectMapper().readTree(objectNodeRoot.toString());
            objectMapper.writeValue(new File(outputFilePath), jsonNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void makeFAKEMosaicTexture_FORTEST(ArrayList<DataSlice> dataSlices, Texture2D resultMosaicTexture, double[] resultTotalMinMaxValues, int[] resultMosaicColRow)
    {
        // This function makes a hardCoding mosaic textures for to test.***
        int dataSlicesCount = dataSlices.size();
        int texWidth = dataSlices.get(0).columnsCount;
        int texHeight = dataSlices.get(0).rowsCount;
        TextureUtils.getMosaicColumnsAndRows(texWidth, texHeight, dataSlicesCount, resultMosaicColRow);

        int mosaicColumnsCount = resultMosaicColRow[0];
        int mosaicRowsCount = resultMosaicColRow[1];
        int mosaixTexWidth = mosaicColumnsCount * texWidth;
        int mosaicTexHeight = mosaicRowsCount * texHeight;

        // find the totalMinValue & totalMaxvalue.***
        double totalMinValue = 0.0;
        double totalMaxValue = 1.0;

        double totalValuesRange = totalMaxValue - totalMinValue;
        resultTotalMinMaxValues[0] = totalMinValue;
        resultTotalMinMaxValues[1] = totalMaxValue;

        // now, fill the mosaicTexture.***
        DataSlice fakeDataSlice = new DataSlice();
        fakeDataSlice.columnsCount = texWidth;
        fakeDataSlice.rowsCount = texHeight;
        fakeDataSlice.values = new double[texWidth * texHeight];


        double []mosaicData = new double[mosaixTexWidth * mosaicTexHeight];
        for(int i=0; i< dataSlicesCount; i++)
        {
            double factor = (double)i/2.0;
            double radiusFactor = (7.0 - factor) / 7.0;
            double valueHardCode = (1.0 - (double)i/6.0);
            fakeDataSlice.makeFAKEData_FORTEST(radiusFactor, valueHardCode);
            DataSlice dataSlice = fakeDataSlice;
            int dataSliceColCount = dataSlice.columnsCount;
            int dataSliceRowCount = dataSlice.rowsCount;
            int mosaicCol = i % mosaicColumnsCount;
            int mosaicRow = i / mosaicColumnsCount;
            for(int row=0; row < dataSliceRowCount; row++)
            {
                for(int col=0; col < dataSliceColCount; col++)
                {
                    double value = dataSlice.getValue(col, row);
                    if(value > 0.0)
                    {
                        int hola = 0;
                    }
                    int mosaicX = col + mosaicCol * dataSliceColCount;
                    int mosaicY = row + mosaicRow * dataSliceRowCount;
                    mosaicData[mosaicY * mosaixTexWidth + mosaicX] = value; // original.***
                }
            }
        }

        // make the mosaicTexture.***
        resultMosaicTexture.width = mosaixTexWidth;
        resultMosaicTexture.height = mosaicTexHeight;
        resultMosaicTexture.data = new byte[mosaixTexWidth * mosaicTexHeight * 4]; // original.***
        int totalValuesCount = mosaixTexWidth * mosaicTexHeight;
        byte []encoded = new byte[4];
        int []encodedInt = new int[4];
        for(int i=0; i<totalValuesCount; i++)
        {
            double value = mosaicData[i];
            double qValue = (value - totalMinValue) / totalValuesRange;
            if(qValue > 0.0)
            {
                int hola = 0;
            }



            GeometryUtils.encodeFloat((float)qValue, encoded);
//            GeometryUtils.encodeFloatToInt((float)qValue, encodedInt);
//            byte []encodedIntByte = new byte[4];
//            encodedIntByte[0] = (byte)encodedInt[0];
//            encodedIntByte[1] = (byte)encodedInt[1];
//            encodedIntByte[2] = (byte)encodedInt[2];
//            encodedIntByte[3] = (byte)encodedInt[3];

            if(encoded[0] < 0)
            {
                int hola = 0;
            }

            if(encoded[1] < 0)
            {
                int hola = 0;
            }

            if(encoded[2] < 0)
            {
                int hola = 0;
            }

            if(encoded[3] < 0)
            {
                int hola = 0;
            }

            resultMosaicTexture.data[i * 4] = encoded[0];
            resultMosaicTexture.data[i * 4 + 1] = encoded[1];
            resultMosaicTexture.data[i * 4 + 2] = encoded[2];
            resultMosaicTexture.data[i * 4 + 3] = encoded[3];

//            resultMosaicTexture.data[i * 4] = (byte)200;
//            resultMosaicTexture.data[i * 4 + 1] = (byte)200;
//            resultMosaicTexture.data[i * 4 + 2] = (byte)200;
//            resultMosaicTexture.data[i * 4 + 3] = (byte)200;

            int hola = 0;
        }



        int hola = 0;

    }

    public void makeFAKEMosaicTexture_FORTEST_2(ArrayList<DataSlice> dataSlices, Texture2D resultMosaicTexture, double[] resultTotalMinMaxValues, int[] resultMosaicColRow)
    {
        // This function makes a hardCoding mosaic textures for to test.***
        int dataSlicesCount = dataSlices.size();
        int texWidth = dataSlices.get(0).columnsCount;
        int texHeight = dataSlices.get(0).rowsCount;
        TextureUtils.getMosaicColumnsAndRows(texWidth, texHeight, dataSlicesCount, resultMosaicColRow);

        int mosaicColumnsCount = resultMosaicColRow[0];
        int mosaicRowsCount = resultMosaicColRow[1];
        int mosaixTexWidth = mosaicColumnsCount * texWidth;
        int mosaicTexHeight = mosaicRowsCount * texHeight;

        // find the totalMinValue & totalMaxvalue.***
        double totalMinValue = 0.0;
        double totalMaxValue = 1.0;

        double totalValuesRange = totalMaxValue - totalMinValue;
        resultTotalMinMaxValues[0] = totalMinValue;
        resultTotalMinMaxValues[1] = totalMaxValue;

        // now, fill the mosaicTexture.***
        DataSlice fakeDataSlice = new DataSlice();
        fakeDataSlice.columnsCount = texWidth;
        fakeDataSlice.rowsCount = texHeight;
        fakeDataSlice.values = new double[texWidth * texHeight];


        double []mosaicData = new double[mosaixTexWidth * mosaicTexHeight];
        for(int i=0; i< dataSlicesCount; i++)
        {
            double factor = (double)i/2.0;
            double radiusFactor = (7.0 - factor) / 7.0;
            double valueHardCode = (1.0 - (double)i/10.0);
            fakeDataSlice.makeFAKEData_FORTEST_2(radiusFactor, valueHardCode);
            DataSlice dataSlice = fakeDataSlice;
            int dataSliceColCount = dataSlice.columnsCount;
            int dataSliceRowCount = dataSlice.rowsCount;
            int mosaicCol = i % mosaicColumnsCount;
            int mosaicRow = i / mosaicColumnsCount;
            for(int row=0; row < dataSliceRowCount; row++)
            {
                for(int col=0; col < dataSliceColCount; col++)
                {
                    double value = dataSlice.getValue(col, row);
                    if(value > 0.0)
                    {
                        int hola = 0;
                    }
                    int mosaicX = col + mosaicCol * dataSliceColCount;
                    int mosaicY = row + mosaicRow * dataSliceRowCount;
                    mosaicData[mosaicY * mosaixTexWidth + mosaicX] = value; // original.***
                }
            }
        }

        // make the mosaicTexture.***
        resultMosaicTexture.width = mosaixTexWidth;
        resultMosaicTexture.height = mosaicTexHeight;
        resultMosaicTexture.data = new byte[mosaixTexWidth * mosaicTexHeight * 4]; // original.***
        int totalValuesCount = mosaixTexWidth * mosaicTexHeight;
        byte []encoded = new byte[4];
        int []encodedInt = new int[4];
        for(int i=0; i<totalValuesCount; i++)
        {
            double value = mosaicData[i];
            double qValue = (value - totalMinValue) / totalValuesRange;
            if(qValue > 0.0)
            {
                int hola = 0;
            }



            GeometryUtils.encodeFloat((float)qValue, encoded);
//            GeometryUtils.encodeFloatToInt((float)qValue, encodedInt);
//            byte []encodedIntByte = new byte[4];
//            encodedIntByte[0] = (byte)encodedInt[0];
//            encodedIntByte[1] = (byte)encodedInt[1];
//            encodedIntByte[2] = (byte)encodedInt[2];
//            encodedIntByte[3] = (byte)encodedInt[3];

            if(encoded[0] < 0)
            {
                int hola = 0;
            }

            if(encoded[1] < 0)
            {
                int hola = 0;
            }

            if(encoded[2] < 0)
            {
                int hola = 0;
            }

            if(encoded[3] < 0)
            {
                int hola = 0;
            }

            resultMosaicTexture.data[i * 4] = encoded[0];
            resultMosaicTexture.data[i * 4 + 1] = encoded[1];
            resultMosaicTexture.data[i * 4 + 2] = encoded[2];
            resultMosaicTexture.data[i * 4 + 3] = encoded[3];

//            resultMosaicTexture.data[i * 4] = (byte)200;
//            resultMosaicTexture.data[i * 4 + 1] = (byte)200;
//            resultMosaicTexture.data[i * 4 + 2] = (byte)200;
//            resultMosaicTexture.data[i * 4 + 3] = (byte)200;

            int hola = 0;
        }



        int hola = 0;

    }

    public void makeMosaicTexture(ArrayList<DataSlice> dataSlices, Texture2D resultMosaicTexture, double[] resultTotalMinMaxValues, int[] resultMosaicColRow)
    {
        int dataSlicesCount = dataSlices.size();
        int texWidth = dataSlices.get(0).columnsCount;
        int texHeight = dataSlices.get(0).rowsCount;
        TextureUtils.getMosaicColumnsAndRows(texWidth, texHeight, dataSlicesCount, resultMosaicColRow);

        int mosaicColumnsCount = resultMosaicColRow[0];
        int mosaicRowsCount = resultMosaicColRow[1];
        int mosaixTexWidth = mosaicColumnsCount * texWidth;
        int mosaicTexHeight = mosaicRowsCount * texHeight;

        // find the totalMinValue & totalMaxvalue.***
        double totalMinValue = 0.0;
        double totalMaxValue = 0.0;
        for(int i=0; i< dataSlicesCount; i++)
        {
            DataSlice dataSlice = dataSlices.get(i);
            if(i == 0)
            {
                totalMinValue = dataSlice.minValue;
                totalMaxValue = dataSlice.maxValue;
            }
            else
            {
                if(totalMinValue > dataSlice.minValue)
                {
                    totalMinValue = dataSlice.minValue;
                }
                if(totalMaxValue < dataSlice.maxValue)
                {
                    totalMaxValue = dataSlice.maxValue;
                }
            }
        }

        double totalValuesRange = totalMaxValue - totalMinValue;
        resultTotalMinMaxValues[0] = totalMinValue;
        resultTotalMinMaxValues[1] = totalMaxValue;

        // now, fill the mosaicTexture.***
        double []mosaicData = new double[mosaixTexWidth * mosaicTexHeight];

        for(int i=0; i< dataSlicesCount; i++)
        {
            DataSlice dataSlice = dataSlices.get(i);
            int dataSliceColCount = dataSlice.columnsCount;
            int dataSliceRowCount = dataSlice.rowsCount;
            int mosaicCol = i % mosaicColumnsCount;
            int mosaicRow = i / mosaicColumnsCount;
            for(int row=0; row<dataSliceRowCount; row++)
            {
                for(int col=0; col<dataSliceColCount; col++)
                {
                    double value = dataSlice.getValue(col, row);
                    int mosaicX = col + mosaicCol * dataSliceColCount;
                    int mosaicY = row + mosaicRow * dataSliceRowCount;
                    mosaicData[mosaicY * mosaixTexWidth + mosaicX] = value;
                }
            }
        }
        // make the mosaicTexture.***
        resultMosaicTexture.width = mosaixTexWidth;
        resultMosaicTexture.height = mosaicTexHeight;
        resultMosaicTexture.data = new byte[mosaixTexWidth * mosaicTexHeight * 4];
        int totalValuesCount = mosaixTexWidth * mosaicTexHeight;

        byte []encoded = new byte[4];
        for(int i=0; i<totalValuesCount; i++)
        {
            double value = mosaicData[i];
            double qValue = (value - totalMinValue) / totalValuesRange;
            if(qValue > 0.0)
            {
                int hola = 0;
            }
            GeometryUtils.encodeFloat((float)qValue, encoded);
            resultMosaicTexture.data[i * 4] = encoded[0];
            resultMosaicTexture.data[i * 4 + 1] = encoded[1];
            resultMosaicTexture.data[i * 4 + 2] = encoded[2];
            resultMosaicTexture.data[i * 4 + 3] = encoded[3];
        }

        int hola = 0;
    }

    public void ConvertMatrixAsciTxtToJsonByDataStructureFile(String inputDataStructurePath, String outputFolderPath)
    {
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
        this.dataContainer.date = objectNodeRoot.get("date").asText();
        ObjectNode centerGeoCoordNode = (ObjectNode) objectNodeRoot.get("centerGeographicCoord");
        this.dataContainer.geoCoordCenterLongitudeDeg = centerGeoCoordNode.get("longitude").asDouble();
        this.dataContainer.geoCoordCenterLatitudeDeg = centerGeoCoordNode.get("latitude").asDouble();
        this.dataContainer.geoCoordCenterAltitude = centerGeoCoordNode.get("altitude").asDouble();
        this.dataContainer.width_km = objectNodeRoot.get("width_km").asDouble();
        this.dataContainer.height_km = objectNodeRoot.get("height_km").asDouble();

        int layersCount = objectNodeRoot.get("layersCount").asInt();
        ArrayNode objectLayersArrayNode = (ArrayNode) objectNodeRoot.get("layers");
        for(int layer = 0; layer < layersCount; layer++)
        {
            ObjectNode objectLayersNode = (ObjectNode) objectLayersArrayNode.get(layer);
            String folderPath = objectLayersNode.get("folderPath").asText();
            String folderName = StringModifier.getLastNameFromPath(folderPath);
            String layerOutputFolderPath = outputFolderPath + "\\" + folderName;

            DataLayer dataLayer = new DataLayer();
            dataLayer.minAltitude = objectLayersNode.get("minAltitude").asDouble();
            dataLayer.maxAltitude = objectLayersNode.get("maxAltitude").asDouble();
            dataLayer.folderName = folderName;
            this.dataContainer.addDataLayer(dataLayer);

            ConvertMatrixAsciTxtToJsonInFolder(folderPath, layerOutputFolderPath);

            int hola = 0;
        }

        int hola = 0;
    }
    private void ConvertMatrixAsciTxtToJsonInFolder(String inputFolderPath, String outputFolderPath) {
        // 1rst, must know all fileNames in input folder.***
        ArrayList<String> vecExtensions = new ArrayList<>();
        vecExtensions.add("TXT");
        ArrayList<String> vecFileNamesInFolder = new ArrayList<>();
        DataLayer currentDataLayer = this.dataContainer.getLastDataLayer();

        StringModifier.getFileNamesInFolder(inputFolderPath, vecExtensions, vecFileNamesInFolder);
        int filesCount = vecFileNamesInFolder.size();
        for (int i = 0; i < filesCount; i++) {
            String fileName = vecFileNamesInFolder.get(i);
            String rawFileName = StringModifier.getRawFileName(fileName);
            currentDataLayer.addTimeSliceFileName(rawFileName);

            ConvertMatrixAsciTxtToJson(inputFolderPath, outputFolderPath, fileName);
        }
    }

    public void loadDataSlice(String dataSliceFilePath, DataSlice resultDataSlice) throws IOException {
        // read the dataSlice file.***
        // the dataSlice file is TXT format.***
        File file = new File(dataSliceFilePath);    //creates a new file instance
        FileReader fr = new FileReader(file);   //reads the file
        BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream

        String line;
        Boolean finished = false;
        Integer counter = 0;
        String delimiter = " ";
        Vector<Double> vecValues = new Vector<>();
        int columnsCount = 0;
        int rowsCount = 0;
        boolean bIsMatrix = true;

        Vector<String> resultSplittedStrings = new Vector<>();

        // read lines.***
        while (!finished) {
            line = br.readLine();
            if (line == null) {
                //finished = true;
                break;
            }

            counter += 1;

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

            // now, transform strings to double values.***
            for (int col = 0; col < columnsCount; col++) {
                String stringValue = resultSplittedStrings.get(col);
                Double doubleValue = Double.parseDouble(stringValue);
                if (doubleValue != null) {
                    vecValues.add(doubleValue);
                } else {
                    bIsMatrix = false;
                    break;
                }
            }
        }

        fr.close();
        br.close();

        rowsCount = counter;

        if (!bIsMatrix || columnsCount == 0 || rowsCount == 0) {
            return;
        }

        // now, find min max values needed for quantizing.***
        Vector2d minMaxValues = GeometryUtils.GetMinMaxValuesVectorDoubles(vecValues);

        String dataSliceFileName = StringModifier.getLastNameFromPath(dataSliceFilePath);
        resultDataSlice.fileName = dataSliceFileName;
        resultDataSlice.columnsCount = columnsCount;
        resultDataSlice.rowsCount = rowsCount;
        resultDataSlice.minValue = minMaxValues.x;
        resultDataSlice.maxValue = minMaxValues.y;
        resultDataSlice.values = new double[vecValues.size()];
        for (int i = 0; i < vecValues.size(); i++) {
            resultDataSlice.values[i] = vecValues.get(i);
        }

        // now calculate the quantized values array.***
        /*
        double minVal = minMaxValues.x;
        double maxVal = minMaxValues.y;
        double qRange = 1.0;
        if(minVal != maxVal) {
            qRange = maxVal - minVal;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNodeRoot = objectMapper.createObjectNode();
        ArrayNode quantizedValuesArrayNode = objectMapper.createArrayNode();

        int valuesCount = vecValues.size();
        for (int i = 0; i < valuesCount; i++) {
            Double value = vecValues.get(i);
            Double qValue = (value - minVal) / qRange;
            quantizedValuesArrayNode.add(qValue);
        }

         */
    }

    private void ConvertMatrixAsciTxtToJson(String inputFolderPath, String outputFolderPath, String inputFileName) {
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
            String delimiter = " ";
            Vector<Double> vecValues = new Vector<>();
            int columnsCount = 0;
            int rowsCount = 0;
            boolean bIsMatrix = true;

            Vector<String> resultSplittedStrings = new Vector<>();

            // read lines.***
            while (!finished) {
                line = br.readLine();
                if (line == null) {
                    //finished = true;
                    break;
                }

                counter += 1;

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

                // now, transform strings to double values.***
                for (int col = 0; col < columnsCount; col++) {
                    String stringValue = resultSplittedStrings.get(col);
                    Double doubleValue = Double.parseDouble(stringValue);
                    if (doubleValue != null) {
                        vecValues.add(doubleValue);
                    } else {
                        bIsMatrix = false;
                        break;
                    }
                }
            }

            fr.close();
            br.close();

            rowsCount = counter;

            if (!bIsMatrix || columnsCount == 0 || rowsCount == 0) {
                return;
            }

            // now, find min max values needed for quantizing.***
            Vector2d minMaxValues = GeometryUtils.GetMinMaxValuesVectorDoubles(vecValues);

            // now calculate the quantized values array.***
            double minVal = minMaxValues.x;
            double maxVal = minMaxValues.y;
            double qRange = 1.0;
            if(minVal != maxVal) {
                qRange = maxVal - minVal;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode objectNodeRoot = objectMapper.createObjectNode();
            ArrayNode quantizedValuesArrayNode = objectMapper.createArrayNode();

            int valuesCount = vecValues.size();
            for (int i = 0; i < valuesCount; i++) {
                Double value = vecValues.get(i);
                Double qValue = (value - minVal) / qRange;
                quantizedValuesArrayNode.add(qValue);
            }

            String rawFileName = StringModifier.getRawFileName(inputFileName);

            /*
            // save PNG file from quantizedValuesArrayNode.***
            String pngFileName = rawFileName + ".png";
            String outputPngFilePath = outputFolderPath + "\\" + pngFileName;
            int texWidth = columnsCount;
            int texHeight = rowsCount;
            Texture2D texture2D = new Texture2D(texWidth, texHeight);

            // now, fill the texture2D.***
            byte[] encoded = new byte[4];
            int valuesCount2 = quantizedValuesArrayNode.size();
            for (int i = 0; i < valuesCount2; i++) {
                Double value = quantizedValuesArrayNode.get(i).asDouble();
                GeometryUtils.encodeFloat(value.floatValue(), encoded);
                texture2D.data[i * 4] = encoded[0];
                texture2D.data[i * 4 + 1] = encoded[1];
                texture2D.data[i * 4 + 2] = encoded[2];
                texture2D.data[i * 4 + 3] = encoded[3];
            }
            texture2D.saveAsPNG(outputPngFilePath);
             */

            objectNodeRoot.put("columnsCount", columnsCount);
            objectNodeRoot.put("rowsCount", rowsCount);
            objectNodeRoot.put("fileName", inputFileName);
            //objectNodeRoot.put("pngFileName", pngFileName);
            objectNodeRoot.put("minValue", minVal);
            objectNodeRoot.put("maxValue", maxVal);
            objectNodeRoot.put("values", quantizedValuesArrayNode);

            // now save json file.***
            String outputFilePath = outputFolderPath + "\\" + rawFileName + ".json";
            JsonNode jsonNode = new ObjectMapper().readTree(objectNodeRoot.toString());
            File outputJSonFile = new File(outputFilePath);
            objectMapper.writeValue(outputJSonFile, jsonNode);

            int hola = 0;
        } catch (IOException e) {
            System.out.println("ERROR *** *** in ConvertMatrixAsciTxtToJson.***");
            e.printStackTrace();
        }
    }
}
