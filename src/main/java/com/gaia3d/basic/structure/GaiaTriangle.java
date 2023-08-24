package com.gaia3d.basic.structure;

import com.gaia3d.util.io.LittleEndianDataInputStream;
import com.gaia3d.util.io.LittleEndianDataOutputStream;
import com.gaia3d.wgs84Tiles.TileIndices;
import org.joml.Vector2d;

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

    public void setHalfEdge(GaiaHalfEdge halfEdge) {
        this.halfEdge = halfEdge;
    }

    public ArrayList<GaiaVertex> getVertices()
    {
        ArrayList<GaiaHalfEdge> halfEdges = this.halfEdge.getHalfEdgesLoop();
        ArrayList<GaiaVertex> vertices = new ArrayList<GaiaVertex>();
        for(GaiaHalfEdge halfEdge : halfEdges)
        {
            vertices.add(halfEdge.getStartVertex());
        }
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

    public boolean intersectsPointXY(double pos_x, double pos_y)
    {
        ArrayList<GaiaHalfEdge>hedges = this.halfEdge.getHalfEdgesLoop();
        double error = 1e-8;
        for(GaiaHalfEdge hedge : hedges)
        {
            GaiaLine2D line2D = hedge.getLine2DXY();
            RelativePosition2D_LinePoint relativePosition2D_linePoint = line2D.relativePositionOfPoint(new Vector2d(pos_x, pos_y), error);
            if(relativePosition2D_linePoint == RelativePosition2D_LinePoint.RIGHT_SIDE_OF_THE_LINE)
            {
                return false;
            }
        }
        return true;
    }

    public boolean intersectsPointXY_v2(double pos_x, double pos_y)
    {
        // this function checks if a point2D is intersected by the triangle only meaning xAxis and yAxis.***
        // 1rst, get the boundingBox.***
        GaiaBoundingBox boundingBox = this.getBoundingBox();
        if(boundingBox.intersectsPointXY(pos_x, pos_y))
        {
            // 2nd, check if the point is inside the triangle.***
            ArrayList<GaiaVertex> vertices = this.getVertices();
            GaiaVertex vertex0 = vertices.get(0);
            GaiaVertex vertex1 = vertices.get(1);
            GaiaVertex vertex2 = vertices.get(2);

            double x0 = vertex0.position.x;
            double y0 = vertex0.position.y;
            double x1 = vertex1.position.x;
            double y1 = vertex1.position.y;
            double x2 = vertex2.position.x;
            double y2 = vertex2.position.y;

            double x = pos_x;
            double y = pos_y;

            double dX = x - x0;
            double dY = y - y0;
            double dX21 = x2 - x1;
            double dY12 = y1 - y2;
            double D = dY12 * (x0 - x2) + dX21 * (y0 - y2);
            double s = dY12 * dX + dX21 * dY;
            double t = (y2 - y0) * dX + (x0 - x2) * dY;

            if (D < 0) return s <= 0 && t <= 0 && s + t >= D;
            return s >= 0 && t >= 0 && s + t <= D;
        }
        else
        {
            return false;
        }
    }

    public void saveDataOutputStream(LittleEndianDataOutputStream dataOutputStream)
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadDataInputStream(LittleEndianDataInputStream dataInputStream) throws IOException
    {
        this.id = dataInputStream.readInt();
        this.halfEdgeId = dataInputStream.readInt();
        if(this.ownerTile_tileIndices == null)
        {
            this.ownerTile_tileIndices = new TileIndices();
        }
        this.ownerTile_tileIndices.loadDataInputStream(dataInputStream);
    }
}
