package com.gaia3d.airPollutionDataConverter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gaia3d.basic.geometry.voxel.VoxelCP;
import com.gaia3d.basic.geometry.voxel.VoxelCPGrid3D;
import com.gaia3d.geometry.BoundingBox;
import com.gaia3d.image.Texture2D;
import com.gaia3d.image.TextureUtils;
import com.gaia3d.utils.GeometryUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@Getter
@Setter
public class AirPollutionVolume {
    public int mosaicColumnsCount = 0;
    public int mosaicRowsCount = 0;
    public int mosaicTextureWidth = 0;
    public int mosaicTextureHeight = 0;
    public double totalMinValueOfVolume = Double.MAX_VALUE;
    public double totalMaxValueOfVolume = Double.MIN_VALUE;
    public String mosaicPngFileName = "";
    // TreeMap <Z, AirPollutionSliceData>. Z = slice.
    public TreeMap<Double, AirPollutionSliceData> volumeData = new TreeMap<Double, AirPollutionSliceData>();
    public String date;
    private BoundingBox geoCoordBBox = new BoundingBox(); // minLongitude, minLatitude, minAltitude, maxLongitude, maxLatitude, maxAltitude
    private String glbFileName = "";
    private int idx = -1;

    public AirPollutionSliceData getOrNewAirPollutionSliceData(Double slice) {
        AirPollutionSliceData sliceData = volumeData.get(slice);
        if (sliceData == null) {
            sliceData = new AirPollutionSliceData();
            volumeData.put(slice, sliceData);
        }
        return sliceData;
    }

    public void getMinMaxValues(double[] resultMinMaxValues) {
        resultMinMaxValues[0] = Double.MAX_VALUE;
        resultMinMaxValues[1] = Double.MIN_VALUE;
        for (AirPollutionSliceData sliceData : volumeData.values()) {
            double[] minMaxValues = new double[2];
            sliceData.getMinMaxValues(minMaxValues);
            if (minMaxValues[0] < resultMinMaxValues[0]) {
                resultMinMaxValues[0] = minMaxValues[0];
            }
            if (minMaxValues[1] > resultMinMaxValues[1]) {
                resultMinMaxValues[1] = minMaxValues[1];
            }
        }

        this.totalMinValueOfVolume = resultMinMaxValues[0];
        this.totalMaxValueOfVolume = resultMinMaxValues[1];
    }

    public VoxelCPGrid3D makeVoxelCPGrid3D(boolean addLastTopSlice) {
        int slicesCount = volumeData.size();
        int texHeight = volumeData.get(volumeData.firstKey()).getRowsCount();
        int texWidth = volumeData.get(volumeData.firstKey()).getColumnsCount();

        // create a new VoxelCPGrid3D object with the specified dimensions.
        int addingSliceCount = addLastTopSlice ? 1 : 0;
        VoxelCPGrid3D resultVoxelCPGrid3D = new VoxelCPGrid3D(texWidth, texHeight, slicesCount + addingSliceCount);

        double[] minMaxValues = new double[2];
        getMinMaxValues(minMaxValues);

        this.totalMinValueOfVolume = minMaxValues[0];
        this.totalMaxValueOfVolume = minMaxValues[1];
        double totalValuesRange = totalMaxValueOfVolume - totalMinValueOfVolume;

        resultVoxelCPGrid3D.setMinMaxValues(minMaxValues);

        Set<Double> keys = volumeData.keySet();
        List<Double> keysList = new ArrayList<>(keys);

        double minValue = 0.0;
        for (int i = 0; i < slicesCount; i++) {

            AirPollutionSliceData sliceData = volumeData.get(keysList.get(i));
            int sliceColCount = sliceData.getColumnsCount();
            int sliceRowCount = sliceData.getRowsCount();
            BoundingBox sliceBBox = this.getGeoCoordBBox();
            double minLonDeg = sliceBBox.minX;
            double minLatDeg = sliceBBox.minY;
            double minAlt = sliceData.minAltitude;
            double maxAlt = sliceData.maxAltitude;
            double maxLonDeg = sliceBBox.maxX;
            double maxLatDeg = sliceBBox.maxY;

            double lonDegRange = maxLonDeg - minLonDeg;
            double latDegRange = maxLatDeg - minLatDeg;

            double lonDegStep = lonDegRange / sliceColCount;
            double latDegStep = latDegRange / sliceRowCount;

            // the altitude is constant for each slice.

            int sliceZ = i;
            for (int row = 0; row < sliceRowCount; row++) {
                for (int col = 0; col < sliceColCount; col++) {
                    double value = sliceData.getValue(col, row);
                    VoxelCP voxelCp = resultVoxelCPGrid3D.getVoxel(col, row, sliceZ);
                    voxelCp.setValue(value);
                    double lonDeg = minLonDeg + col * lonDegStep;
                    double latDeg = minLatDeg + row * latDegStep;
                    voxelCp.getPosition().set(lonDeg, latDeg, minAlt);
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
                            double lonDeg = minLonDeg + col * lonDegStep;
                            double latDeg = minLatDeg + row * latDegStep;
                            voxelCp.getPosition().set(lonDeg, latDeg, maxAlt);
                            int hola = 0;
                        }
                    }
                }
            }
        }

        return resultVoxelCPGrid3D;
    }

    public void makeMosaicTexture(Texture2D resultMosaicTexture) {
        int slicesCount = volumeData.size();
        int texHeight = volumeData.get(volumeData.firstKey()).getRowsCount();
        int texWidth = volumeData.get(volumeData.firstKey()).getColumnsCount();
        int[] mosaicColumnsAndRows = new int[2];
        TextureUtils.getMosaicColumnsAndRows(texWidth, texHeight, slicesCount, mosaicColumnsAndRows);

        int mosaicColumnsCount = mosaicColumnsAndRows[0];
        int mosaicRowsCount = mosaicColumnsAndRows[1];
        int mosaicTexWidth = mosaicColumnsAndRows[0] * texWidth;
        int mosaicTexHeight = mosaicColumnsAndRows[1] * texHeight;

        this.mosaicColumnsCount = mosaicColumnsCount;
        this.mosaicRowsCount = mosaicRowsCount;

        double[] minMaxValues = new double[2];
        getMinMaxValues(minMaxValues);

        this.totalMinValueOfVolume = minMaxValues[0];
        this.totalMaxValueOfVolume = minMaxValues[1];
        double totalValuesRange = totalMaxValueOfVolume - totalMinValueOfVolume;

        double[] mosaicData = new double[mosaicTexWidth * mosaicTexHeight];
        int i = 0;
        for (AirPollutionSliceData sliceData : volumeData.values()) {
            int sliceColCount = sliceData.getColumnsCount();
            int sliceRowCount = sliceData.getRowsCount();
            int mosaicCol = i % mosaicColumnsCount;
            int mosaicRow = i / mosaicColumnsCount;

            for (int row = 0; row < sliceRowCount; row++) {
                for (int col = 0; col < sliceColCount; col++) {
                    double value = sliceData.getValue(col, row);
                    int mosaicX = col + mosaicCol * sliceColCount;
                    int mosaicY = row + mosaicRow * sliceRowCount;
                    mosaicData[mosaicY * mosaicTexWidth + mosaicX] = value;
                }
            }
            i++;
        }

        // make the mosaicTexture.
        resultMosaicTexture.width = mosaicTexWidth;
        resultMosaicTexture.height = mosaicTexHeight;
        resultMosaicTexture.data = new byte[mosaicTexWidth * mosaicTexHeight * 4];

        int totalValuesCount = mosaicTexWidth * mosaicTexHeight;
        byte[] encoded = new byte[4];
        for (int j = 0; j < totalValuesCount; j++) {
            double value = mosaicData[j];
            double qValue = (value - totalMinValueOfVolume) / totalValuesRange;
            GeometryUtils.encodeFloat((float) qValue, encoded);
            resultMosaicTexture.data[j * 4] = encoded[0];
            resultMosaicTexture.data[j * 4 + 1] = encoded[1];
            resultMosaicTexture.data[j * 4 + 2] = encoded[2];
            resultMosaicTexture.data[j * 4 + 3] = encoded[3];
        }
    }

    public void saveAsJson(String jsonFilePath) {
        /*
        // example of json.
        {
			"minValue": 0.0,
			"maxValue": 0.0194,
			"width": 450,
			"height": 450,
			"mosaicTextureFileName": "Air   0min_mosaicTexture.png",
			"mosaicColumnsCount": 3,
			"mosaicRowsCount": 3,
			"dataSlices": [
				{
					"minValue": 0.0,
					"maxValue": 0.0194,
					"width": 150,
					"height": 150,
					"minAltitude": 0.0,
					"maxAltitude": 10.0,
					"fileName": "Air   0min.TXT"
				},
				{
					"minValue": 0.0,
					"maxValue": 0.000151,
					"width": 150,
					"height": 150,
					"minAltitude": 10.0,
					"maxAltitude": 20.0,
					"fileName": "Air   0min.TXT"
				}
				...
				...
				...
			]
		}*/

        AirPollutionSliceData anySliceData = volumeData.get(volumeData.firstKey());
        int mosaicTexWidth = this.mosaicColumnsCount * anySliceData.getColumnsCount();
        int mosaicTexHeight = this.mosaicRowsCount * anySliceData.getRowsCount();

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode mosaicTextureMetadataObjectNode = objectMapper.createObjectNode();
        mosaicTextureMetadataObjectNode.put("minValue", this.totalMinValueOfVolume);
        mosaicTextureMetadataObjectNode.put("maxValue", this.totalMaxValueOfVolume);
        mosaicTextureMetadataObjectNode.put("width", mosaicTexWidth);
        mosaicTextureMetadataObjectNode.put("height", mosaicTexHeight);
        mosaicTextureMetadataObjectNode.put("mosaicTextureFileName", this.mosaicPngFileName);
        mosaicTextureMetadataObjectNode.put("mosaicColumnsCount", this.mosaicColumnsCount);
        mosaicTextureMetadataObjectNode.put("mosaicRowsCount", this.mosaicRowsCount);

        ArrayNode mosaicTextureMetadataArrayNode = objectMapper.createArrayNode();
        // traverse "volumeData".
        for (AirPollutionSliceData sliceData : volumeData.values()) {
            ObjectNode sliceDataObjectNode = objectMapper.createObjectNode();
            double[] minMaxValues = new double[2];
            sliceData.getMinMaxValues(minMaxValues);
            sliceDataObjectNode.put("minValue", minMaxValues[0]);
            sliceDataObjectNode.put("maxValue", minMaxValues[1]);
            sliceDataObjectNode.put("width", sliceData.getColumnsCount());
            sliceDataObjectNode.put("height", sliceData.getRowsCount());
            sliceDataObjectNode.put("minAltitude", sliceData.minAltitude);
            sliceDataObjectNode.put("maxAltitude", sliceData.maxAltitude);
            sliceDataObjectNode.put("fileName", sliceData.fileName);
            mosaicTextureMetadataArrayNode.add(sliceDataObjectNode);
        }

        mosaicTextureMetadataObjectNode.put("dataSlices", mosaicTextureMetadataArrayNode);

        try {
            JsonNode jsonNode = new ObjectMapper().readTree(mosaicTextureMetadataObjectNode.toString());
            objectMapper.writeValue(new File(jsonFilePath), jsonNode);
            //this.dataContainer.addMosaicTexMetaDataFileName(mosaicTextureMetadataFileName);
        } catch (IOException e) {
            log.error("", e);
        }
    }

    public void saveAsJsonMC(String jsonFilePath) {
        /*
        // example of json.
        {
			"minValue": 0.0,
			"maxValue": 0.0194,
			"width": 450,
			"height": 450,
			"mosaicTextureFileName": "Air   0min_mosaicTexture.png",
			"mosaicColumnsCount": 3,
			"mosaicRowsCount": 3,
			"dataSlices": [
				{
					"minValue": 0.0,
					"maxValue": 0.0194,
					"width": 150,
					"height": 150,
					"minAltitude": 0.0,
					"maxAltitude": 10.0,
					"fileName": "Air   0min.TXT"
				},
				{
					"minValue": 0.0,
					"maxValue": 0.000151,
					"width": 150,
					"height": 150,
					"minAltitude": 10.0,
					"maxAltitude": 20.0,
					"fileName": "Air   0min.TXT"
				}
				...
				...
				...
			]
		}*/

        // date is YYMMDDhh. Change it to YYYYMMDD+"T"+hhmmss.
        String yy = date.substring(0, 2);
        String mm = date.substring(2, 4);
        String dd = date.substring(4, 6);
        String hh = date.substring(6, 8);
        String mi = "00";
        String ss = "00";
        String date = "20" + yy + mm + dd + "T" + hh + mi + ss;


        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode mosaicTextureMetadataObjectNode = objectMapper.createObjectNode();
        mosaicTextureMetadataObjectNode.put("date", date);
        mosaicTextureMetadataObjectNode.put("idx", this.idx);
        mosaicTextureMetadataObjectNode.put("minValue", this.totalMinValueOfVolume);
        mosaicTextureMetadataObjectNode.put("maxValue", this.totalMaxValueOfVolume);
        mosaicTextureMetadataObjectNode.put("glbFileName", this.glbFileName);

        try {
            JsonNode jsonNode = new ObjectMapper().readTree(mosaicTextureMetadataObjectNode.toString());
            objectMapper.writeValue(new File(jsonFilePath), jsonNode);
        } catch (IOException e) {
            log.error("", e);
        }
    }
}
