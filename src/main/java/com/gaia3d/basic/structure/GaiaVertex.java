package com.gaia3d.basic.structure;

import com.gaia3d.util.io.LittleEndianDataInputStream;
import com.gaia3d.util.io.LittleEndianDataOutputStream;
import org.joml.Vector3d;

import java.io.IOException;
import java.util.ArrayList;

public class GaiaVertex {
    public GaiaHalfEdge outingHEdge = null;
    public Vector3d position = new Vector3d();

    public int id = -1;

    public int outingHEdgeId = -1;

    public GaiaObjectStatus objectStatus = GaiaObjectStatus.ACTIVE;

    public void deleteObjects()
    {
        outingHEdge = null;
        position = null;
    }

    public boolean isCoincidentVertex(GaiaVertex vertex, double error)
    {
        if(vertex == null)
        {
            return false;
        }

        // Test debug:***
        double xDiff = Math.abs(this.position.x - vertex.position.x);
        double yDiff = Math.abs(this.position.y - vertex.position.y);
        double zDiff = Math.abs(this.position.z - vertex.position.z);
        // end test debug.***

        if(Math.abs(this.position.x - vertex.position.x) < error &&
                Math.abs(this.position.y - vertex.position.y) < error &&
                Math.abs(this.position.z - vertex.position.z) < error)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public void avoidOutingHalfEdge(GaiaHalfEdge avoidOutingHalfEdge)
    {
        // if this outingHEdge is the avoidOutingHalfEdge, then must change it.***
        if(this.outingHEdge != avoidOutingHalfEdge)
        {
            return;
        }

        ArrayList<GaiaHalfEdge> allOutingHalfEdges = this.getAllOutingHalfEdges();
        for(GaiaHalfEdge outingHalfEdge : allOutingHalfEdges)
        {
            if(outingHalfEdge != avoidOutingHalfEdge)
            {
                this.outingHEdge = outingHalfEdge;
                break;
            }
        }
    }

    public ArrayList<GaiaHalfEdge> getAllOutingHalfEdges()
    {
        ArrayList<GaiaHalfEdge> outingHalfEdges = new ArrayList<GaiaHalfEdge>();

        // there are 2 cases: this vertex is interior vertex or boundary vertex, but we dont know.***
        // 1- interior vertex.***
        // 2- boundary vertex.***
        if(this.outingHEdge == null)
        {
            // error.***
            System.out.println("Error: this.outingHEdge == null");
        }

        GaiaHalfEdge firstHalfEdge = this.outingHEdge;
        GaiaHalfEdge currHalfEdge = this.outingHEdge;
        outingHalfEdges.add(this.outingHEdge); // put the first halfEdge.***
        boolean finished = false;
        boolean isInteriorVertex = true;
        while(!finished)
        {
            GaiaHalfEdge twinHalfEdge = currHalfEdge.twin;
            if(twinHalfEdge == null)
            {
                finished = true;
                isInteriorVertex = false;
                break;
            }
            GaiaHalfEdge nextHalfEdge = twinHalfEdge.next;
            if(nextHalfEdge == null)
            {
                finished = true;
                isInteriorVertex = false;
                break;
            }
            else if(nextHalfEdge == firstHalfEdge)
            {
                finished = true;
                break;
            }

            outingHalfEdges.add(nextHalfEdge);
            currHalfEdge = nextHalfEdge;
        }

        // if this vertex is NO interior vertex, then must check if there are more outing halfEdges.***
        if(!isInteriorVertex)
        {
            // check if there are more outing halfEdges.***
            currHalfEdge = this.outingHEdge;
            finished = false;
            while(!finished)
            {
                GaiaHalfEdge prevHalfEdge = currHalfEdge.getPrev();
                GaiaHalfEdge twinHalfEdge = prevHalfEdge.twin;
                if(twinHalfEdge == null)
                {
                    finished = true;
                    break;
                }

                outingHalfEdges.add(twinHalfEdge);

                currHalfEdge = twinHalfEdge;
            }

        }

        return outingHalfEdges;
    }


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
