package com.gaia3d.basic.structure;

import com.gaia3d.util.io.BigEndianDataInputStream;
import com.gaia3d.util.io.BigEndianDataOutputStream;
import com.gaia3d.wgs84Tiles.TileIndices;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.io.IOException;
import java.util.ArrayList;

public class GaiaTriangle {
    public GaiaHalfEdge halfEdge = null;

    public int id = -1;

    public int halfEdgeId = -1;

    // this triangle belongs to a tile.***
    public TileIndices ownerTile_tileIndices = new TileIndices();
    public GaiaObjectStatus objectStatus = GaiaObjectStatus.ACTIVE;

    public GaiaBoundingBox boundingBox = null;
    public GaiaPlane plane = null;

    public int splitDepth = 0;

    public boolean refineChecked = false;

    public void deleteObjects()
    {
        halfEdge = null;
        ownerTile_tileIndices = null;
        boundingBox = null;
        plane = null;
    }

    public void setHalfEdge(GaiaHalfEdge halfEdge) {
        this.halfEdge = halfEdge;
        halfEdge.setTriangleToHEdgesLoop(this);
    }

    public ArrayList<GaiaVertex> getVertices()
    {
        ArrayList<GaiaHalfEdge> halfEdges = new ArrayList<>();
        this.halfEdge.getHalfEdgesLoop(halfEdges);
        ArrayList<GaiaVertex> vertices = new ArrayList<GaiaVertex>();
        for(GaiaHalfEdge halfEdge : halfEdges)
        {
            vertices.add(halfEdge.getStartVertex());
        }
        halfEdges.clear();
        return vertices;
    }

    public GaiaBoundingBox getBoundingBox()
    {
        if(this.boundingBox == null)
        {
            this.boundingBox = new GaiaBoundingBox();
            ArrayList<GaiaVertex> vertices = this.getVertices();
            for(GaiaVertex vertex : vertices)
            {
                this.boundingBox.addPoint(vertex.position);
            }
        }

        return this.boundingBox;
    }

    public GaiaPlane getPlane()
    {
        if(this.plane == null)
        {
            this.plane = new GaiaPlane();
            ArrayList<GaiaVertex> vertices = this.getVertices();
            GaiaVertex vertex0 = vertices.get(0);
            GaiaVertex vertex1 = vertices.get(1);
            GaiaVertex vertex2 = vertices.get(2);
            this.plane.set3Points(vertex0.position, vertex1.position, vertex2.position);
        }

        return this.plane;
    }

    public boolean intersectsPointXY(double pos_x, double pos_y, ArrayList<GaiaHalfEdge> memSave_hedges, GaiaLine2D memSave_line2D)
    {
        this.halfEdge.getHalfEdgesLoop(memSave_hedges);
        double error = 1e-8;
        int hedgesCount = memSave_hedges.size();
        GaiaLine2D line2dAux = null;
        for(int i=0; i<hedgesCount; i++)
        {
            GaiaHalfEdge hedge = memSave_hedges.get(i);
            line2dAux = hedge.getLine2DXY();
            byte relativePosition2D_linePoint = line2dAux.relativePositionOfPoint(pos_x, pos_y, error);

            // relative positions :
            // 0 : point is on the line.
            // 1 : point is on the left side of the line.
            // 2 : point is on the right side of the line.
            if(relativePosition2D_linePoint == 2)
            {
                memSave_hedges.clear();
                return false;
            }
        }

        memSave_hedges.clear();
        return true;
    }

    public Vector3d getBarycenter()
    {
        ArrayList<GaiaVertex> vertices = this.getVertices();
        Vector3d barycenter = new Vector3d();
        for(GaiaVertex vertex : vertices)
        {
            barycenter.add(vertex.position);
        }
        barycenter.mul(1.0/3.0);
        return barycenter;
    }

    public ArrayList<Vector3d> getSomePointsToCheckForTriangleRefinement()
    {
        ArrayList<Vector3d> somePoints = new ArrayList<Vector3d>();

        Vector3d barycenter = this.getBarycenter();
        somePoints.add(barycenter);

        ArrayList<GaiaVertex> vertices = this.getVertices();
        for(GaiaVertex vertex : vertices)
        {
            Vector3d pos = vertex.position.add(barycenter).mul(0.5);
            somePoints.add(pos);
        }

        return somePoints;
    }

    public ArrayList<Vector3d> getPerimeterPositions(int numInterpolation)
    {
        ArrayList<Vector3d> perimeterPositions = new ArrayList<Vector3d>();
        ArrayList<GaiaHalfEdge> halfEdges = new ArrayList<>();
        this.halfEdge.getHalfEdgesLoop(halfEdges);
        for(GaiaHalfEdge halfEdge : halfEdges)
        {
            halfEdge.getInterpolatedPositions(perimeterPositions, numInterpolation);
        }
        halfEdges.clear();
        return perimeterPositions;
    }

    public GaiaHalfEdge getLongestHalfEdge()
    {
        // Note : the length of the halfEdges meaning only the length of the XY plane.***
        ArrayList<GaiaHalfEdge> halfEdges = new ArrayList<>();
        this.halfEdge.getHalfEdgesLoop(halfEdges);
        GaiaHalfEdge longestHalfEdge = null;
        double maxLength = 0.0;
        for(GaiaHalfEdge halfEdge : halfEdges)
        {
            double length = halfEdge.getSquaredLengthXY();
            if(length > maxLength)
            {
                maxLength = length;
                longestHalfEdge = halfEdge;
            }
        }
        halfEdges.clear();

        return longestHalfEdge;
    }

    public void saveDataOutputStream(BigEndianDataOutputStream dataOutputStream)
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

            // 3rd, save ownerTile_tileIndices.***
            if(ownerTile_tileIndices == null)
            {
                dataOutputStream.writeInt(-1);
                dataOutputStream.writeInt(-1);
                dataOutputStream.writeInt(-1);
            }
            else
            {
                ownerTile_tileIndices.saveDataOutputStream(dataOutputStream);
            }

            // save splitDepth.***
            dataOutputStream.writeInt(splitDepth);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadDataInputStream(BigEndianDataInputStream dataInputStream) throws IOException
    {
        this.id = dataInputStream.readInt();
        this.halfEdgeId = dataInputStream.readInt();
        if(this.ownerTile_tileIndices == null)
        {
            this.ownerTile_tileIndices = new TileIndices();
        }
        this.ownerTile_tileIndices.loadDataInputStream(dataInputStream);

        this.splitDepth = dataInputStream.readInt();
    }
}
