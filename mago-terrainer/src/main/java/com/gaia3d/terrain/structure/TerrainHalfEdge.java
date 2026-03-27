package com.gaia3d.terrain.structure;

import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.geometry.entities.GaiaLine;
import com.gaia3d.io.BigEndianDataInputStream;
import com.gaia3d.io.BigEndianDataOutputStream;
import com.gaia3d.terrain.types.TerrainHalfEdgeType;
import com.gaia3d.terrain.types.TerrainObjectStatus;
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
public class TerrainHalfEdge {
    private int id = -1;

    private TerrainVertex startVertex = null;
    private TerrainHalfEdge next = null;
    private TerrainHalfEdge twin = null;
    private TerrainTriangle triangle = null;

    private int startVertexId = -1;
    private int nextId = -1;
    private int twinId = -1;
    private int triangleId = -1;

    private TerrainHalfEdgeType type = TerrainHalfEdgeType.UNKNOWN;
    private TerrainObjectStatus objectStatus = TerrainObjectStatus.ACTIVE;
    private TerrainLine2D line2D = null;
    private GaiaRectangle boundingRect = null;

    public void deleteObjects() {
        startVertex = null;
        next = null;
        twin = null;
        triangle = null;
        line2D = null;
    }

    public void setTwin(TerrainHalfEdge twin) {
        this.twin = twin;
        if (twin != null) {
            twin.twin = this;
        }
    }

    public void setTriangle(TerrainTriangle triangle) {
        this.triangle = triangle;
        //triangle.setHalfEdge(this);
        triangle.halfEdge = this;
    }

    public void setTriangleToHEdgesLoop(TerrainTriangle triangle) {
        List<TerrainHalfEdge> halfEdgesLoop = new ArrayList<>();
        this.getHalfEdgesLoop(halfEdgesLoop);
        for (TerrainHalfEdge halfEdge : halfEdgesLoop) {
            halfEdge.setTriangle(triangle);
        }
        halfEdgesLoop.clear();
    }

    public void setStartVertex(TerrainVertex startVertex) {
        this.startVertex = startVertex;
        startVertex.setOutingHEdge(this);
    }

    public TerrainVertex getEndVertex() {
        if (next != null) {
            return next.getStartVertex();
        } else {
            // check if exist twin
            if (twin != null) {
                return twin.getStartVertex();
            } else {
                return null;
            }
        }
    }

    public TerrainHalfEdge getPrev() {
        TerrainHalfEdge thisHalfEdge = this;
        TerrainHalfEdge currHalfEdge = this;
        TerrainHalfEdge prevHalfEdge = null;
        boolean finished = false;
        while (!finished) {
            TerrainHalfEdge nextHalfEdge = currHalfEdge.next;
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

    public TerrainLine2D getLine2DXY() {
        if (this.line2D == null) {
            this.line2D = new TerrainLine2D();
            Vector3d startPos = this.getStartVertex().getPosition();
            Vector3d endPos = this.getEndVertex().getPosition();
            this.line2D.setBy2Points(startPos.x, startPos.y, endPos.x, endPos.y);
        }

        return this.line2D;
    }

    public void getHalfEdgesLoop(List<TerrainHalfEdge> halfEdgesLoop) {
        TerrainHalfEdge currHalfEdge = this;
        TerrainHalfEdge firstHalfEdge = this;
        halfEdgesLoop.add(currHalfEdge);
        boolean finished = false;
        while (!finished) {
            TerrainHalfEdge nextHalfEdge = currHalfEdge.next;
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
        endPos.sub(startPos, dir);
        dir.normalize();
        return dir;
    }

    public void getInterpolatedPositions(List<Vector3d> resultPositions, int numPositions) {
        // this function returns the interpolated positions of this halfEdge
        // resultPositions must be initialized
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

    public boolean isHalfEdgePossibleTwin(TerrainHalfEdge halfEdge, double error) {
        // 2 halfEdges is possible to be twins if : startPoint_A is coincident with endPoint_B & startPoint_B is coincident with endPoint_A
        TerrainVertex startPoint_A = this.getStartVertex();
        TerrainVertex endPoint_A = this.getEndVertex();

        TerrainVertex startPoint_B = halfEdge.getStartVertex();
        TerrainVertex endPoint_B = halfEdge.getEndVertex();

        // First do a bounding box check
        GaiaRectangle boundingRect_A = this.getBoundingRectangle();
        GaiaRectangle boundingRect_B = halfEdge.getBoundingRectangle();

        if (!boundingRect_A.intersectsInSomeAxis(boundingRect_B)) {
            return false;
        }

        // 2nd compare objects as values
        return startPoint_A.isCoincidentVertexXY(endPoint_B, error) && startPoint_B.isCoincidentVertexXY(endPoint_A, error);
    }

    public boolean isTwineableByPointers(TerrainHalfEdge twin) {
        TerrainVertex thisStartVertex = this.getStartVertex();
        TerrainVertex thisEndVertex = this.getEndVertex();
        TerrainVertex twinStartVertex = twin.getStartVertex();
        TerrainVertex twinEndVertex = twin.getEndVertex();

        return thisStartVertex == twinEndVertex && thisEndVertex == twinStartVertex;
    }

    public boolean hasTwin() {
        if (this.twin == null) {
            return false;
        } else {
            if (this.twin.getObjectStatus() == TerrainObjectStatus.DELETED) {
                this.twin.setTwin(null);
                this.twin = null;
                return false;
            }
        }
        return true;
    }

    public void loadDataInputStream(BigEndianDataInputStream dataInputStream) throws IOException {
        this.id = dataInputStream.readInt();

        this.startVertexId = dataInputStream.readInt();
        this.nextId = dataInputStream.readInt();
        this.twinId = dataInputStream.readInt();
        this.triangleId = dataInputStream.readInt();
        int typeValue = dataInputStream.readInt();
        this.type = TerrainHalfEdgeType.fromValue(typeValue);
    }

    public void saveDataOutputStream(BigEndianDataOutputStream dataOutputStream) {
        try {
            // First, save id
            dataOutputStream.writeInt(id);

            // 2nd, save startVertex
            if (startVertex != null) {
                dataOutputStream.writeInt(startVertex.getId());
            } else {
                dataOutputStream.writeInt(-1);
            }

            // 3rd, save next
            if (next != null) {
                dataOutputStream.writeInt(next.id);
            } else {
                dataOutputStream.writeInt(-1);
            }

            // 4th, save twin
            if (twin != null) {
                dataOutputStream.writeInt(twin.id);
            } else {
                dataOutputStream.writeInt(-1);
            }

            // 5th, save triangle
            if (triangle != null) {
                dataOutputStream.writeInt(triangle.getId());
            } else {
                dataOutputStream.writeInt(-1);
            }
            int type_int = type.getValue();
            dataOutputStream.writeInt(type_int);
        } catch (Exception e) {
            log.error("Error:", e);
        }
    }

    public GaiaLine getLine(){
        Vector3d startPos = this.getStartVertex().getPosition();
        Vector3d direction = getDirection();
        return new GaiaLine(startPos, direction);
    }

    public byte intersectsPoint(Vector3d point, double error) {
        // 0 = NO_INTERSECTION, 1 = INTERSECTION, 2 = COINCIDENT_WITH_START_VERTEX, 3 = COINCIDENT_WITH_END_VERTEX
        byte intersectionType = 0;

        // 1rst, check if intersects with the line.
        GaiaLine line = this.getLine();
        if(!line.intersectsPoint(point, error)) {
            return 0;
        }

        // check if the point is coincident with start point or end point.
        Vector3d startPoint = this.getStartVertex().getPosition();
        double dist = point.distance(startPoint);
        if(dist < error) {
            return 2;
        }

        Vector3d endPoint = this.getEndVertex().getPosition();
        double dist2 = endPoint.distance(startPoint);
        if(dist2 < error) {
            return 3;
        }

        // finally check if the point is inside the terrainHalfEdge.
        double abx = endPoint.x - startPoint.x;
        double aby = endPoint.y - startPoint.y;
        double abz = endPoint.z - startPoint.z;

        double apx = point.x - startPoint.x;
        double apy = point.y - startPoint.y;
        double apz = point.z - startPoint.z;

        double abLenSq = abx * abx + aby * aby + abz * abz;

        // Caso degenerado: A == B
        if (abLenSq == 0.0) {
            double dx = point.x - startPoint.x;
            double dy = point.y - startPoint.y;
            double dz = point.z - startPoint.z;
            if((dx*dx + dy*dy + dz*dz) < error * error) {
                return 2;
            }
        }

        // t = proyección
        double t = (apx * abx + apy * aby + apz * abz) / abLenSq;

        // fuera del segmento
        if (t < 0.0 || t > 1.0) {
            return 0;
        }

        // punto proyectado en la recta
        double projx = startPoint.x + t * abx;
        double projy = startPoint.y + t * aby;
        double projz = startPoint.z + t * abz;

        // distancia al segmento
        double dx = point.x - projx;
        double dy = point.y - projy;
        double dz = point.z - projz;

        double distSq = dx * dx + dy * dy + dz * dz;
        if(distSq < error * error) {
            intersectionType = 1;
        }

        return intersectionType;
    }

}
