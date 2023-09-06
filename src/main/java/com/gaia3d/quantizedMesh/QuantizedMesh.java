package com.gaia3d.quantizedMesh;

import com.gaia3d.util.io.LittleEndianDataOutputStream;

import java.io.IOException;

public class QuantizedMesh
{
    QuantizedMeshHeader header = new QuantizedMeshHeader();

    int vertexCount = 0;
    int triangleCount = 0;
    short[] uBuffer = null;
    short[] vBuffer = null;
    short[] heightBuffer = null;
    int[] triangleIndices = null;

    // edgesData.***
    int westVertexCount;
    int[] westIndices = null;

    int southVertexCount;
    int[] southIndices = null;

    int eastVertexCount;
    int[] eastIndices = null;

    int northVertexCount;
    int[] northIndices = null;

    public short zigZagEncode(int n)
    {
        return (short)((n << 1) ^ (n >> 31));
    }

    public void getEncodedIndices32(int[] indices, int count, int[] encodedIndices)
    {
        int highest = 0;
        int code;
        for (int i = 0; i < count; i++)
        {
            int idx = indices[i];
            code = highest - idx;
            encodedIndices[i] = code;
            if (code == 0)
                highest += 1;
        }
    }

    public void getEncodedIndices16(int[] indices, int count, short[] encodedIndices)
    {
        int highest = 0;
        int code;
        for (int i = 0; i < count; i++)
        {
            int idx = indices[i];
            code = highest - idx;
            encodedIndices[i] = (short)code;
            if (code == 0)
                highest += 1;
        }
    }

    public void saveDataOutputStream(LittleEndianDataOutputStream dataOutputStream) throws IOException
    {
        // 1rst save the header.***
        header.saveDataOutputStream(dataOutputStream);

        // 2nd save the vertexCount.***
        dataOutputStream.writeInt(vertexCount);

        /*
        // now, zigZagEncode u, v, height.***
        for(int i = 0; i < vertexCount-1; i++)
        {
            short uNext = quantizedMesh.uBuffer[i+1];
            short vNext = quantizedMesh.vBuffer[i+1];
            short heightNext = quantizedMesh.heightBuffer[i+1];

            short uDiff = (short)(uNext - quantizedMesh.uBuffer[i]);
            quantizedMesh.uBuffer[i] = zigZagEncode(uDiff);

            short vDiff = (short)(vNext - quantizedMesh.vBuffer[i]);
            quantizedMesh.vBuffer[i] = zigZagEncode(vDiff);

            short heightDiff = (short)(heightNext - quantizedMesh.heightBuffer[i]);
            quantizedMesh.heightBuffer[i] = zigZagEncode(heightDiff);
        }
         */

        // save uBuffer.***
        dataOutputStream.writeShort(uBuffer[0]);
        for(int i = 0; i < vertexCount - 1; i++)
        {
            short uNext = uBuffer[i+1];
            short uDiff = (short)(uNext - uBuffer[i]);
            dataOutputStream.writeShort(zigZagEncode(uDiff));
        }

        // save vBuffer.***
        dataOutputStream.writeShort(vBuffer[0]);
        for(int i = 0; i < vertexCount - 1; i++)
        {
            short vNext = vBuffer[i+1];
            short vDiff = (short)(vNext - vBuffer[i]);
            dataOutputStream.writeShort(zigZagEncode(vDiff));
        }

        // save heightBuffer.***
        dataOutputStream.writeShort(heightBuffer[0]);
        for(int i = 0; i < vertexCount - 1; i++)
        {
            short heightNext = heightBuffer[i+1];
            short heightDiff = (short)(heightNext - heightBuffer[i]);
            dataOutputStream.writeShort(zigZagEncode(heightDiff));
        }

        // save triangleCount.***
        dataOutputStream.writeInt(triangleCount);


        int indicesCount = triangleCount * 3;
        // if vertexCount > 65536, then save the triangleIndices as int.***
        if(vertexCount > 65536)
        {
            // save IndexData32.***
            int[] encodedIndices = new int[indicesCount];
            getEncodedIndices32(triangleIndices, indicesCount, encodedIndices);
            for(int i = 0; i < indicesCount; i++)
            {
                dataOutputStream.writeInt(encodedIndices[i]);
            }
        }
        else
        {
            // save IndexData16.***
            short[] encodedIndices = new short[indicesCount];
            getEncodedIndices16(triangleIndices, indicesCount, encodedIndices);
            for(int i = 0; i < indicesCount; i++)
            {
                dataOutputStream.writeShort(encodedIndices[i]);
            }
        }

        // now save EdgeIndices.***
        if(vertexCount > 65536)
        {
            // save EdgeIndices32.***
            // westIndices.***
            dataOutputStream.writeInt(westVertexCount);
            for(int i = 0; i < westVertexCount; i++)
            {
                dataOutputStream.writeInt(westIndices[i]);
            }

            // southIndices.***
            dataOutputStream.writeInt(southVertexCount);
            for(int i = 0; i < southVertexCount; i++)
            {
                dataOutputStream.writeInt(southIndices[i]);
            }

            // eastIndices.***
            dataOutputStream.writeInt(eastVertexCount);
            for(int i = 0; i < eastVertexCount; i++)
            {
                dataOutputStream.writeInt(eastIndices[i]);
            }

            // northIndices.***
            dataOutputStream.writeInt(northVertexCount);
            for(int i = 0; i < northVertexCount; i++)
            {
                dataOutputStream.writeInt(northIndices[i]);
            }
        }
        else
        {
            // save EdgeIndices16.***
            // westIndices.***
            dataOutputStream.writeInt(westVertexCount);
            for(int i = 0; i < westVertexCount; i++)
            {
                dataOutputStream.writeShort(westIndices[i]);
            }

            // southIndices.***
            dataOutputStream.writeInt(southVertexCount);
            for(int i = 0; i < southVertexCount; i++)
            {
                dataOutputStream.writeShort(southIndices[i]);
            }

            // eastIndices.***
            dataOutputStream.writeInt(eastVertexCount);
            for(int i = 0; i < eastVertexCount; i++)
            {
                dataOutputStream.writeShort(eastIndices[i]);
            }

            // northIndices.***
            dataOutputStream.writeInt(northVertexCount);
            for(int i = 0; i < northVertexCount; i++)
            {
                dataOutputStream.writeShort(northIndices[i]);
            }
        }

        int hola = 0;
    }
}
