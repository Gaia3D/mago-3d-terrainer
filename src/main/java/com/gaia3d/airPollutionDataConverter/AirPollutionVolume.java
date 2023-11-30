package com.gaia3d.airPollutionDataConverter;

import com.gaia3d.image.Texture2D;
import com.gaia3d.image.TextureUtils;
import com.gaia3d.utils.GeometryUtils;

import java.util.TreeMap;

public class AirPollutionVolume
{
     // TreeMap <Z, AirPollutionSliceData>. Z = slice.***
    TreeMap<Double, AirPollutionSliceData> volumeData = new TreeMap <Double, AirPollutionSliceData>();
    String date;

    public int mosaicColumnsCount = 0;
    public int mosaicRowsCount = 0;

    public double totalMinValue = Integer.MAX_VALUE;
    public double totalMaxValue = Integer.MIN_VALUE;

    public AirPollutionSliceData getOrNewAirPollutionSliceData(Double slice)
    {
        AirPollutionSliceData sliceData = volumeData.get(slice);
        if (sliceData == null)
        {
            sliceData = new AirPollutionSliceData();
            volumeData.put(slice, sliceData);
        }
        return sliceData;
    }

    public void getMinMaxValues(double[] resultMinMaxValues)
    {
        resultMinMaxValues[0] = Integer.MAX_VALUE;
        resultMinMaxValues[1] = Integer.MIN_VALUE;
        for (AirPollutionSliceData sliceData : volumeData.values())
        {
            double[] minMaxValues = new double[2];
            sliceData.getMinMaxValues(minMaxValues);
            if (minMaxValues[0] < resultMinMaxValues[0])
            {
                resultMinMaxValues[0] = minMaxValues[0];
            }
            if (minMaxValues[1] > resultMinMaxValues[1])
            {
                resultMinMaxValues[1] = minMaxValues[1];
            }
        }
    }

    public void makeMosaicTexture(Texture2D resultMosaicTexture)
    {
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

        this.totalMinValue = minMaxValues[0];
        this.totalMaxValue = minMaxValues[1];
        double totalValuesRange = totalMaxValue - totalMinValue;

        double[] mosaicData = new double[mosaicTexWidth * mosaicTexHeight];
        int i=0;
        for (AirPollutionSliceData sliceData : volumeData.values())
        {
            int sliceColCount = sliceData.getColumnsCount();
            int sliceRowCount = sliceData.getRowsCount();
            int mosaicCol = i % mosaicColumnsCount;
            int mosaicRow = i / mosaicColumnsCount;

            for(int row=0; row<sliceRowCount; row++)
            {
                for(int col=0; col<sliceColCount; col++)
                {
                    double value = sliceData.getValue(col, row);
                    int mosaicX = col + mosaicCol * sliceColCount;
                    int mosaicY = row + mosaicRow * sliceRowCount;
                    mosaicData[mosaicY * mosaicTexWidth + mosaicX] = value;
                }
            }
            i++;
        }

        // make the mosaicTexture.***
        resultMosaicTexture.width = mosaicTexWidth;
        resultMosaicTexture.height = mosaicTexHeight;
        resultMosaicTexture.data = new byte[mosaicTexWidth * mosaicTexHeight * 4];

        int totalValuesCount = mosaicTexWidth * mosaicTexHeight;
        byte []encoded = new byte[4];
        for(int j=0; j<totalValuesCount; j++)
        {
            double value = mosaicData[j];
            double qValue = (value - totalMinValue) / totalValuesRange;
            if(qValue > 0.0)
            {
                int hola = 0;
            }
            GeometryUtils.encodeFloat((float)qValue, encoded);
            resultMosaicTexture.data[j * 4] = encoded[0];
            resultMosaicTexture.data[j * 4 + 1] = encoded[1];
            resultMosaicTexture.data[j * 4 + 2] = encoded[2];
            resultMosaicTexture.data[j * 4 + 3] = encoded[3];
        }

        int hola = 0;
    }
}
