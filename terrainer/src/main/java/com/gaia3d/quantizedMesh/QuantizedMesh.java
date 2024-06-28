package com.gaia3d.quantizedMesh;

import com.gaia3d.util.io.LittleEndianDataOutputStream;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Setter
@Getter
@Slf4j
public class QuantizedMesh {
    private QuantizedMeshHeader header = new QuantizedMeshHeader();

    private int vertexCount = 0;
    private int triangleCount = 0;
    private short[] uBuffer = null;
    private short[] vBuffer = null;
    private short[] heightBuffer = null;
    private int[] triangleIndices = null;

    // edgesData
    private int westVertexCount;
    private int[] westIndices = null;

    private int southVertexCount;
    private int[] southIndices = null;

    private int eastVertexCount;
    private int[] eastIndices = null;

    private int northVertexCount;
    private int[] northIndices = null;

    // normals data
    private byte extensionId = 0;
    private  int extensionLength = 0;
    private byte[] octEncodedNormals = null; // 2 bytes per normal

    public short zigZagEncode(int n) {
        return (short) ((n << 1) ^ (n >> 31));
    }

    public void getDecodedIndices32(int[] indices, int count, int[] decodedIndices) {
        int highest = 0;
        int code;
        for (int i = 0; i < count; i++) {
            code = indices[i];
            decodedIndices[i] = highest - code;
            if (code == 0) highest += 1;
        }


    }

    public void getEncodedIndices32(int[] indices, int count, int[] encodedIndices) {
        int highest = 0;
        int code;
        for (int i = 0; i < count; i++) {
            int idx = indices[i];
            code = highest - idx;
            encodedIndices[i] = code;
            if (code == 0) highest += 1;
        }
    }

    public void getDecodedIndices16(int[] indices, int count, short[] decodedIndices) {
        int highest = 0;
        for (int i = 0; i < count; i++) {
            int code = indices[i];
            decodedIndices[i] = (short) (highest - code);
            if (code == 0) highest += 1;
        }
    }

    public void getDecodedIndices16fromShort(short[] indices, int count, short[] decodedIndices) {
        int highest = 0;
        for (int i = 0; i < count; i++) {
            short code = indices[i];
            decodedIndices[i] = (short) (highest - code);
            if (code == 0) highest += 1;
        }
    }

    public void getEncodedIndices16(int[] indices, int count, short[] encodedIndices) {
        int highest = 0;
        int code;
        for (int i = 0; i < count; i++) {
            int idx = indices[i];
            code = highest - idx;
            encodedIndices[i] = (short) code;
            if (code == 0) highest += 1;
        }
    }

    private void saveIndices32(LittleEndianDataOutputStream dataOutputStream, int[] indices, int count) throws IOException {
        int highest = 0;
        int code;
        for (int i = 0; i < count; i++) {
            int idx = indices[i];
            code = highest - idx;
            dataOutputStream.writeInt(code);
            if (code == 0) highest += 1;
        }
    }

    private void saveIndices16(LittleEndianDataOutputStream dataOutputStream, int[] indices, int count) throws IOException {
        int highest = 0;
        int code;
        for (int i = 0; i < count; i++) {
            int idx = indices[i];
            code = highest - idx;
            dataOutputStream.writeShort((short) code);
            if (code == 0) highest += 1;
        }
    }

    public void saveDataOutputStream(LittleEndianDataOutputStream dataOutputStream, boolean saveNormals) throws IOException {
        // 1rst save the header
        header.saveDataOutputStream(dataOutputStream);

        // 2nd save the vertexCount
        dataOutputStream.writeInt(vertexCount);

        // save uBuffer
        short uPrev = 0;
        for (int i = 0; i < vertexCount; i++) {
            short uCurr = uBuffer[i];
            short uDiff = (short) (uCurr - uPrev);
            dataOutputStream.writeShort(zigZagEncode(uDiff));

            uPrev = uCurr;
        }

        // save vBuffer
        short vPrev = 0;
        for (int i = 0; i < vertexCount; i++) {
            short vCurr = vBuffer[i];
            short vDiff = (short) (vCurr - vPrev);
            dataOutputStream.writeShort(zigZagEncode(vDiff));

            vPrev = vCurr;
        }

        // save heightBuffer
        short heightPrev = 0;
        for (int i = 0; i < vertexCount; i++) {
            short heightCurr = heightBuffer[i];
            short heightDiff = (short) (heightCurr - heightPrev);
            dataOutputStream.writeShort(zigZagEncode(heightDiff));

            heightPrev = heightCurr;
        }

        // save triangleCount
        dataOutputStream.writeInt(triangleCount);


        int indicesCount = triangleCount * 3;
        // if vertexCount > 65536, then save the triangleIndices as int
        if (vertexCount > 65536) {
            // save IndexData32
            int[] encodedIndices = new int[indicesCount];
            getEncodedIndices32(triangleIndices, indicesCount, encodedIndices);
            for (int i = 0; i < indicesCount; i++) {
                dataOutputStream.writeInt(encodedIndices[i]);
            }
        } else {
            // save IndexData16
            short[] encodedIndices = new short[indicesCount];
            getEncodedIndices16(triangleIndices, indicesCount, encodedIndices);

            for (int i = 0; i < indicesCount; i++) {
                dataOutputStream.writeShort(encodedIndices[i]);
            }
        }

        // now save EdgeIndices
        if (vertexCount > 65536) {
            // save EdgeIndices32
            // westIndices
            dataOutputStream.writeInt(westVertexCount);
            for (int i = 0; i < westVertexCount; i++) {
                dataOutputStream.writeInt(westIndices[i]);
            }

            // southIndices
            dataOutputStream.writeInt(southVertexCount);
            for (int i = 0; i < southVertexCount; i++) {
                dataOutputStream.writeInt(southIndices[i]);
            }

            // eastIndices
            dataOutputStream.writeInt(eastVertexCount);
            for (int i = 0; i < eastVertexCount; i++) {
                dataOutputStream.writeInt(eastIndices[i]);
            }

            // northIndices
            dataOutputStream.writeInt(northVertexCount);
            for (int i = 0; i < northVertexCount; i++) {
                dataOutputStream.writeInt(northIndices[i]);
            }
        } else {
            // save EdgeIndices16
            // westIndices
            dataOutputStream.writeInt(westVertexCount);
            for (int i = 0; i < westVertexCount; i++) {
                dataOutputStream.writeShort(westIndices[i]);
            }

            // southIndices
            dataOutputStream.writeInt(southVertexCount);
            for (int i = 0; i < southVertexCount; i++) {
                dataOutputStream.writeShort(southIndices[i]);
            }

            // eastIndices
            dataOutputStream.writeInt(eastVertexCount);
            for (int i = 0; i < eastVertexCount; i++) {
                dataOutputStream.writeShort(eastIndices[i]);
            }

            // northIndices
            dataOutputStream.writeInt(northVertexCount);
            for (int i = 0; i < northVertexCount; i++) {
                dataOutputStream.writeShort(northIndices[i]);
            }
        }

        // check if save normals
        if (saveNormals) {
            // save normals
            dataOutputStream.writeByte(extensionId);
            dataOutputStream.writeInt(extensionLength);
            dataOutputStream.write(octEncodedNormals);
        }

        
    }

}
