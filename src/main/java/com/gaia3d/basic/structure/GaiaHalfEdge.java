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

    public HalfEdgeType type = HalfEdgeType.UNKNOWN;

    public void setTwin(GaiaHalfEdge twin) {
        this.twin = twin;
        twin.twin = this;
    }

    public void setStartVertex(GaiaVertex startVertex) {
        this.startVertex = startVertex;
        startVertex.outingHEdge = this;
    }

    public GaiaVertex getStartVertex() {
        return startVertex;
    }

    public GaiaVertex getEndVertex()
    {
        if(next != null)
        {
            return next.getStartVertex();
        }
        else
        {
            // check if exist twin.***
            if(twin != null)
            {
                return twin.getStartVertex();
            }
            else
            {
                return null;
            }
        }
    }

    public GaiaHalfEdge getNext() {
        return next;
    }

    public GaiaHalfEdge getPrev() {
        GaiaHalfEdge thisHalfEdge = this;
        GaiaHalfEdge currHalfEdge = this;
        GaiaHalfEdge prevHalfEdge = null;
        boolean finished = false;
        while (!finished)
        {
            GaiaHalfEdge nextHalfEdge = currHalfEdge.next;
            if(nextHalfEdge == null)
            {
                return null;
            }

            if(nextHalfEdge == thisHalfEdge)
            {
                finished = true;
                prevHalfEdge = currHalfEdge;
            }
            else
            {
                currHalfEdge = nextHalfEdge;
            }
        }

        return prevHalfEdge;
    }

    public boolean isHalfEdgePossibleTwin(GaiaHalfEdge halfEdge)
    {
        // 2 halfEdges is possible to be twins if : startPoint_A is coincident with endPoint_B & startPoint_B is coincident with endPoint_A.***
        GaiaVertex startPoint_A = this.getStartVertex();
        GaiaVertex endPoint_A = this.getEndVertex();

        GaiaVertex startPoint_B = halfEdge.getStartVertex();
        GaiaVertex endPoint_B = halfEdge.getEndVertex();

        // 1rst compare objects as pointers.***
        if(startPoint_A.equals(endPoint_B) && startPoint_B.equals(endPoint_A))
        {
            return true;
        }

        // 2nd compare objects as values.***
        if(startPoint_A.isCoincidentVertex(endPoint_B, 0.0000001) && startPoint_B.isCoincidentVertex(endPoint_A, 0.0000001))
        {
            return true;
        }

        return false;
    }

    public void loadDataInputStream(LittleEndianDataInputStream dataInputStream) throws IOException
    {
        this.id = dataInputStream.readInt();

        this.startVertexId = dataInputStream.readInt();
        this.nextId = dataInputStream.readInt();
        this.twinId = dataInputStream.readInt();
        this.triangleId = dataInputStream.readInt();
        int typeValue= dataInputStream.readInt();
        this.type = HalfEdgeType.fromValue(typeValue);
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
            dataOutputStream.writeInt(type.getValue());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
