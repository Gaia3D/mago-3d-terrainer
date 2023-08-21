package com.gaia3d.basic.structure;

import com.gaia3d.util.io.LittleEndianDataInputStream;
import com.gaia3d.util.io.LittleEndianDataOutputStream;
import org.joml.Vector3d;

import java.io.IOException;

public class GaiaVertex {
    public GaiaHalfEdge outingHEdge = null;
    public Vector3d position = new Vector3d();

    public int id = -1;

    public int outingHEdgeId = -1;

    public void saveDataOutputStream(LittleEndianDataOutputStream dataOutputStream)
    {
        try {
            // 1rst, save id.***
            dataOutputStream.writeInt(id);
            dataOutputStream.writeDouble(position.x);
            dataOutputStream.writeDouble(position.y);
            dataOutputStream.writeDouble(position.z);

            // 2nd, save outingHEdge.***
            if(outingHEdge != null)
            {
                dataOutputStream.writeInt(outingHEdge.id);
            }
            else
            {
                dataOutputStream.writeInt(-1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadDataInputStream(LittleEndianDataInputStream dataInputStream) throws IOException {
        this.id = dataInputStream.readInt();
        this.position.x = dataInputStream.readDouble();
        this.position.y = dataInputStream.readDouble();
        this.position.z = dataInputStream.readDouble();

        this.outingHEdgeId = dataInputStream.readInt();
    }
}
