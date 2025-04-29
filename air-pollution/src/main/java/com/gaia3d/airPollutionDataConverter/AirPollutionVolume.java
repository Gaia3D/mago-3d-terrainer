package com.gaia3d.airPollutionDataConverter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gaia3d.image.Texture2D;
import com.gaia3d.image.TextureUtils;
import com.gaia3d.utils.GeometryUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

@Slf4j
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
}
