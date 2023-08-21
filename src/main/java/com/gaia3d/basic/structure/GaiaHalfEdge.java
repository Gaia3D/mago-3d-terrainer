package com.gaia3d.basic.structure;



import com.gaia3d.util.io.LittleEndianDataInputStream;
import com.gaia3d.util.io.LittleEndianDataOutputStream;

import java.io.IOException;

public class GaiaHalfEdge {
    public GaiaVertex startVertex = null;
    public GaiaHalfEdge next = null;
    public GaiaHalfEdge twin = null;
    public GaiaTriangle triangle = null;

    public int id = -1;

    public int startVertexId = -1;
    public int nextId = -1;
    public int twinId = -1;
    public int triangleId = -1;

    public void setTwin(GaiaHalfEdge twin) {
        this.twin = twin;
        twin.twin = this;
    }

    public void setStartVertex(GaiaVertex startVertex) {
        this.startVertex = startVertex;
        startVertex.outingHEdge = this;
    }

    public void loadDataInputStream(LittleEndianDataInputStream dataInputStream) throws IOException
    {
        this.id = dataInputStream.readInt();

        this.startVertexId = dataInputStream.readInt();
        this.nextId = dataInputStream.readInt();
        this.twinId = dataInputStream.readInt();
        this.triangleId = dataInputStream.readInt();

    }

    public void saveDataOutputStream(LittleEndianDataOutputStream dataOutputStream)
    {
        try {
            // 1rst, save id.***
            dataOutputStream.writeInt(id);

            // 2nd, save startVertex.***
            if(startVertex != null)
            {
                dataOutputStream.writeInt(startVertex.id);
            }
            else
            {
                dataOutputStream.writeInt(-1);
            }

            // 3rd, save next.***
            if(next != null)
            {
                dataOutputStream.writeInt(next.id);
            }
            else
            {
                dataOutputStream.writeInt(-1);
            }

            // 4th, save twin.***
            if(twin != null)
            {
                dataOutputStream.writeInt(twin.id);
            }
            else
            {
                dataOutputStream.writeInt(-1);
            }

            // 5th, save triangle.***
            if(triangle != null)
            {
                dataOutputStream.writeInt(triangle.id);
            }
            else
            {
                dataOutputStream.writeInt(-1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
