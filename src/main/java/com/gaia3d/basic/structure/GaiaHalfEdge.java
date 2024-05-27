package com.gaia3d.basic.structure;


import com.gaia3d.basic.types.HalfEdgeType;
import com.gaia3d.util.io.BigEndianDataInputStream;
import com.gaia3d.util.io.BigEndianDataOutputStream;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Setter
public class GaiaHalfEdge {
    @Getter
    private GaiaVertex startVertex = null;
    @Getter
    private GaiaHalfEdge next = null;
    private GaiaHalfEdge twin = null;
    private GaiaTriangle triangle = null;

    private int id = -1;

    private int startVertexId = -1;
    private int nextId = -1;
    private int twinId = -1;
    private int triangleId = -1;

    private HalfEdgeType type = HalfEdgeType.UNKNOWN;
    private GaiaObjectStatus objectStatus = GaiaObjectStatus.ACTIVE;

    private GaiaLine2D line2D = null;

    private GaiaRectangle boundingRect = null;

    public void deleteObjects() {
        startVertex = null;
        next = null;
        twin = null;
        triangle = null;
        line2D = null;
    }

    public void setTwin(GaiaHalfEdge twin) {
        this.twin = twin;
        if (twin != null) {
            twin.twin = this;
        }
    }

    public void setTriangle(GaiaTriangle triangle) {
        this.triangle = triangle;
        //triangle.setHalfEdge(this);
        triangle.halfEdge = this;
    }

    public void setTriangleToHEdgesLoop(GaiaTriangle triangle) {
        List<GaiaHalfEdge> halfEdgesLoop = new ArrayList<>();
        this.getHalfEdgesLoop(halfEdgesLoop);
        for (GaiaHalfEdge halfEdge : halfEdgesLoop) {
            halfEdge.setTriangle(triangle);
        }
        halfEdgesLoop.clear();
    }

    public void setStartVertex(GaiaVertex startVertex) {
        this.startVertex = startVertex;
        startVertex.setOutingHEdge(this);
    }

    public GaiaVertex getEndVertex() {
        if (next != null) {
            return next.getStartVertex();
        } else {
            // check if exist twin.***
            if (twin != null) {
                return twin.getStartVertex();
            } else {
                return null;
            }
        }
    }

    public GaiaHalfEdge getPrev() {
        GaiaHalfEdge thisHalfEdge = this;
        GaiaHalfEdge currHalfEdge = this;
        GaiaHalfEdge prevHalfEdge = null;
        boolean finished = false;
        while (!finished) {
            GaiaHalfEdge nextHalfEdge = currHalfEdge.next;
            if (nextHalfEdge == null) {
                return null;
            }

            if (nextHalfEdge == thisHalfEdge) {
                finished = true;
                prevHalfEdge = currHalfEdge;
            } else {
                currHalfEdge = nextHalfEdge;
            }
        }

        return prevHalfEdge;
    }

    public GaiaLine2D getLine2DXY() {
        if (this.line2D == null) {
            this.line2D = new GaiaLine2D();
            Vector3d startPos = this.getStartVertex().getPosition();
            Vector3d endPos = this.getEndVertex().getPosition();
            this.line2D.setBy2Points(startPos.x, startPos.y, endPos.x, endPos.y);
        }

        return this.line2D;
    }

    public void getHalfEdgesLoop(List<GaiaHalfEdge> halfEdgesLoop) {
        GaiaHalfEdge currHalfEdge = this;
        GaiaHalfEdge firstHalfEdge = this;
        halfEdgesLoop.add(currHalfEdge);
        boolean finished = false;
        while (!finished) {
            GaiaHalfEdge nextHalfEdge = currHalfEdge.next;
            if (nextHalfEdge == null) {
                finished = true;
                break;
            } else {
                currHalfEdge = nextHalfEdge;
                if (currHalfEdge == firstHalfEdge) {
                    finished = true;
                    break;
                }
                halfEdgesLoop.add(currHalfEdge);
            }
        }
    }

    public double getSquaredLength() {
        Vector3d startPos = this.getStartVertex().getPosition();
        Vector3d endPos = this.getEndVertex().getPosition();
        return startPos.distanceSquared(endPos);
    }

    public double getSquaredLengthXY() {
        Vector3d startPos = this.getStartVertex().getPosition();
        Vector3d endPos = this.getEndVertex().getPosition();
        Vector2d startPosXY = new Vector2d(startPos.x, startPos.y);
        Vector2d endPosXY = new Vector2d(endPos.x, endPos.y);
        return startPosXY.distanceSquared(endPosXY);
    }

    public Vector3d getMidPosition() {
        Vector3d startPos = this.getStartVertex().getPosition();
        Vector3d endPos = this.getEndVertex().getPosition();
        Vector3d midPos = new Vector3d();
        midPos.add(startPos).add(endPos).mul(0.5);
        //midPos.set((startPos.x/2.0 + endPos.x/2.0), (startPos.y/2.0 + endPos.y/2.0), (startPos.z/2.0 + endPos.z/2.0));
        return midPos;
    }

    public Vector3d getDirection() {
        Vector3d startPos = this.getStartVertex().getPosition();
        Vector3d endPos = this.getEndVertex().getPosition();
        Vector3d dir = new Vector3d();
        dir.sub(endPos, startPos);
        dir.normalize();
        return dir;
    }

    public void getInterpolatedPositions(List<Vector3d> resultPositions, int numPositions) {
        // this function returns the interpolated positions of this halfEdge.***
        // resultPositions must be initialized.***
        resultPositions.clear();
        Vector3d startPos = this.getStartVertex().getPosition();
        Vector3d endPos = this.getEndVertex().getPosition();
        Vector3d dir = getDirection();
        double length = startPos.distance(endPos);
        double step = length / (numPositions - 1);
        for (int i = 0; i < numPositions; i++) {
            Vector3d pos = new Vector3d();
            pos.add(startPos).add(dir.mul(step * i));
            resultPositions.add(pos);
        }
    }

    public GaiaRectangle getBoundingRectangle() {
        if (this.boundingRect == null) {
            this.boundingRect = new GaiaRectangle();
            Vector3d startPos = this.getStartVertex().getPosition();
            Vector3d endPos = this.getEndVertex().getPosition();
            this.boundingRect.setInit(new Vector2d(startPos.x, startPos.y));
            this.boundingRect.addPoint(endPos.x, endPos.y);
        }

        return this.boundingRect;
    }

    public boolean isHalfEdgePossibleTwin(GaiaHalfEdge halfEdge, double error, int axisToCheck) {
        // 2 halfEdges is possible to be twins if : startPoint_A is coincident with endPoint_B & startPoint_B is coincident with endPoint_A.***
        GaiaVertex startPoint_A = this.getStartVertex();
        GaiaVertex endPoint_A = this.getEndVertex();

        GaiaVertex startPoint_B = halfEdge.getStartVertex();
        GaiaVertex endPoint_B = halfEdge.getEndVertex();

//        // 1rst do a bounding box check.***
        GaiaRectangle boundingRect_A = this.getBoundingRectangle();
        GaiaRectangle boundingRect_B = halfEdge.getBoundingRectangle();

        if (axisToCheck == 0) // x axis
        {
            if (!boundingRect_A.intersectsInXAxis(boundingRect_B)) {
                return false;
            }
        } else if (axisToCheck == 1) // y axis
        {
            if (!boundingRect_A.intersectsInYAxis(boundingRect_B)) {
                return false;
            }
        }

        // 2nd compare objects as values.***
        return startPoint_A.isCoincidentVertex(endPoint_B, error) && startPoint_B.isCoincidentVertex(endPoint_A, error);
    }

    public boolean isHalfEdgePossibleTwin(GaiaHalfEdge halfEdge, double error) {
        // 2 halfEdges is possible to be twins if : startPoint_A is coincident with endPoint_B & startPoint_B is coincident with endPoint_A.***
        GaiaVertex startPoint_A = this.getStartVertex();
        GaiaVertex endPoint_A = this.getEndVertex();

        GaiaVertex startPoint_B = halfEdge.getStartVertex();
        GaiaVertex endPoint_B = halfEdge.getEndVertex();

//        // 1rst do a bounding box check.***
        GaiaRectangle boundingRect_A = this.getBoundingRectangle();
        GaiaRectangle boundingRect_B = halfEdge.getBoundingRectangle();

        if (!boundingRect_A.intersectsInSomeAxis(boundingRect_B)) {
            return false;
        }

        // 2nd compare objects as values.***
        return startPoint_A.isCoincidentVertex(endPoint_B, error) && startPoint_B.isCoincidentVertex(endPoint_A, error);
    }

    public void loadDataInputStream(BigEndianDataInputStream dataInputStream) throws IOException {
        this.id = dataInputStream.readInt();

        this.startVertexId = dataInputStream.readInt();
        this.nextId = dataInputStream.readInt();
        this.twinId = dataInputStream.readInt();
        this.triangleId = dataInputStream.readInt();
        int typeValue = dataInputStream.readInt();
        this.type = HalfEdgeType.fromValue(typeValue);
    }

    public void saveDataOutputStream(BigEndianDataOutputStream dataOutputStream) {
        try {
            // 1rst, save id.***
            dataOutputStream.writeInt(id);

            // 2nd, save startVertex.***
            if (startVertex != null) {
                dataOutputStream.writeInt(startVertex.getId());
            } else {
                dataOutputStream.writeInt(-1);
            }

            // 3rd, save next.***
            if (next != null) {
                dataOutputStream.writeInt(next.id);
            } else {
                dataOutputStream.writeInt(-1);
            }

            // 4th, save twin.***
            if (twin != null) {
                dataOutputStream.writeInt(twin.id);
            } else {
                dataOutputStream.writeInt(-1);
            }

            // 5th, save triangle.***
            if (triangle != null) {
                dataOutputStream.writeInt(triangle.getId());
            } else {
                dataOutputStream.writeInt(-1);
            }
            int type_int = type.getValue();
            dataOutputStream.writeInt(type_int);
        } catch (Exception e) {
            log.error("{}", e.getMessage());
        }
    }

}
