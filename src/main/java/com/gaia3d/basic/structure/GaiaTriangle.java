package com.gaia3d.basic.structure;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GaiaTriangle {
    public GaiaHalfEdge halfEdge = null;

    public int id = -1;

    public int halfEdgeId = -1;

    public void setHalfEdge(GaiaHalfEdge halfEdge) {
        this.halfEdge = halfEdge;
    }

    public void saveDataOutputStream(DataOutputStream dataOutputStream)
    {
        try {
            // 1rst, save id.***
            dataOutputStream.writeInt(id);

            // 2nd, save halfEdge.***
            if(halfEdge != null)
            {
                dataOutputStream.writeInt(halfEdge.id);
            }
            else
            {
                dataOutputStream.writeInt(-1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadDataInputStream(DataInputStream dataInputStream) throws IOException
    {
        this.id = dataInputStream.readInt();

        this.halfEdgeId = dataInputStream.readInt();
    }
}
