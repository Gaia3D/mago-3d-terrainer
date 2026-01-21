package com.gaia3d.quantized.mesh;

import com.gaia3d.io.LittleEndianDataInputStream;
import com.gaia3d.io.LittleEndianDataOutputStream;
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

    // edge indices
    private int westVertexCount;
    private int southVertexCount;
    private int eastVertexCount;
    private int northVertexCount;

    private int[] westIndices = null;
    private int[] southIndices = null;
    private int[] eastIndices = null;
    private int[] northIndices = null;

    // normals data
    private byte extensionId = 0;
    private int extensionLength = 0;
    private byte[] octEncodedNormals = null; // 2 bytes per normal

    public short zigZagEncode(int n) {
        return (short) ((n << 1) ^ (n >> 31));
    }

    public int zigZagDecode(short n) {
        int unsigned = n & 0xFFFF;
        return (unsigned >> 1) ^ -(unsigned & 1);
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

    public void getDecodedIndices16(int[] indices, int count, int[] decodedIndices) {
        int highest = 0;
        for (int i = 0; i < count; i++) {
            int code = indices[i];
            decodedIndices[i] = (short) (highest - code);
            if (code == 0) highest += 1;
        }
    }

    public void getDecodedIndices16fromShort(short[] indices, int count, int[] decodedIndices) {
        int highest = 0;
        for (int i = 0; i < count; i++) {
            int code = indices[i];
            if (code < 0) code += 65536;
            int idx = highest - code;
            decodedIndices[i] = idx;

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

    public void loadDataInputStream(LittleEndianDataInputStream dataInputStream) throws IOException {
        // First load the header
        header.loadDataInputStream(dataInputStream);

        // 2nd load the vertexCount
        vertexCount = dataInputStream.readInt();

        // load uBuffer
        short uPrev = 0;
        uBuffer = new short[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            short uDiff = dataInputStream.readShort();
            short uCurr = (short) (uPrev + zigZagDecode(uDiff));
            uBuffer[i] = uCurr;
            uPrev = uCurr;
        }

        // load vBuffer
        short vPrev = 0;
        vBuffer = new short[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            short vDiff = dataInputStream.readShort();
            short vCurr = (short) (vPrev + zigZagDecode(vDiff));
            vBuffer[i] = vCurr;
            vPrev = vCurr;
        }

        // load heightBuffer
        short heightPrev = 0;
        heightBuffer = new short[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            short heightDiff = dataInputStream.readShort();
            short heightCurr = (short) (heightPrev + zigZagDecode(heightDiff));
            heightBuffer[i] = heightCurr;
            heightPrev = heightCurr;
        }

        // load triangleCount
        triangleCount = dataInputStream.readInt();

        int indicesCount = triangleCount * 3;
        // if vertexCount > 65536, then load the triangleIndices as int
        if (vertexCount > 65536) {
            // load IndexData32
            triangleIndices = new int[indicesCount];
            int[] encodedIndices = new int[indicesCount];
            for (int i = 0; i < indicesCount; i++) {
                encodedIndices[i] = dataInputStream.readInt();
                getDecodedIndices32(encodedIndices, indicesCount, triangleIndices);
            }
        } else {
            // load IndexData16
            triangleIndices = new int[indicesCount];
            short[] encodedIndices = new short[indicesCount];
            for (int i = 0; i < indicesCount; i++) {
                encodedIndices[i] = dataInputStream.readShort();
                getDecodedIndices16fromShort(encodedIndices, indicesCount, triangleIndices);
            }
        }

        // now load EdgeIndices
        if (vertexCount > 65536) {
            // load EdgeIndices32
            // westIndices
            westVertexCount = dataInputStream.readInt();
            westIndices = new int[westVertexCount];
            for (int i = 0; i < westVertexCount; i++) {
                westIndices[i] = dataInputStream.readInt();
            }

            // southIndices
            southVertexCount = dataInputStream.readInt();
            southIndices = new int[southVertexCount];
            for (int i = 0; i < southVertexCount; i++) {
                southIndices[i] = dataInputStream.readInt();
            }

            // eastIndices
            eastVertexCount = dataInputStream.readInt();
            eastIndices = new int[eastVertexCount];
            for (int i = 0; i < eastVertexCount; i++) {
                eastIndices[i] = dataInputStream.readInt();
            }

            // northIndices
            northVertexCount = dataInputStream.readInt();
            northIndices = new int[northVertexCount];
            for (int i = 0; i < northVertexCount; i++) {
                northIndices[i] = dataInputStream.readInt();
            }
        } else {
            // load EdgeIndices16
            // westIndices
            westVertexCount = dataInputStream.readInt();
            westIndices = new int[westVertexCount];
            for (int i = 0; i < westVertexCount; i++) {
                westIndices[i] = dataInputStream.readShort();
            }

            // southIndices
            southVertexCount = dataInputStream.readInt();
            southIndices = new int[southVertexCount];
            for (int i = 0; i < southVertexCount; i++) {
                southIndices[i] = dataInputStream.readShort();
            }

            // eastIndices
            eastVertexCount = dataInputStream.readInt();
            eastIndices = new int[eastVertexCount];
            for (int i = 0; i < eastVertexCount; i++) {
                eastIndices[i] = dataInputStream.readShort();
            }

            // northIndices
            northVertexCount = dataInputStream.readInt();
            northIndices = new int[northVertexCount];
            for (int i = 0; i < northVertexCount; i++) {
                northIndices[i] = dataInputStream.readShort();
            }
        }
    }

    public void saveDataOutputStream(LittleEndianDataOutputStream dataOutputStream, boolean saveNormals) throws IOException {
        // First save the header
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
