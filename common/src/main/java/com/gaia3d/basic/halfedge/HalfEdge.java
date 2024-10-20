package com.gaia3d.basic.halfedge;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class HalfEdge implements Serializable {
    public String note = null;
    private HalfEdge twin = null;
    private HalfEdge next = null;
    private HalfEdgeVertex startVertex = null;
    private HalfEdgeFace face = null;
    private ObjectStatus status = ObjectStatus.ACTIVE;
    private int id = -1;
    private int twinId = -1;
    private int nextId = -1;
    private int startVertexId = -1;
    private int faceId = -1;

    public void setStartVertex(HalfEdgeVertex startVertex) {
        this.startVertex = startVertex;
        if (startVertex != null) {
            startVertex.setOutingHalfEdge(this);
        }
    }

    public boolean setTwin(HalfEdge twin) {
        if (twin == null) {
            this.twin = null;
            return true;
        }

        if (this.isTwineableByPointers(twin)) {
            this.twin = twin;
            twin.twin = this;
            return true;
        }
        return false;
    }

    public void untwin() {
        if (twin != null) {
            twin.twin = null;
            twin = null;
        }
    }

    public boolean hasTwin() {
        if (this.twin == null) {
            return false;
        } else {
            if (this.twin.getStatus() == ObjectStatus.DELETED) {
                this.twin.setTwin(null);
                this.twin = null;
                return false;
            }
        }
        return true;
    }

    public HalfEdgeVertex getEndVertex() {
        if (next == null) {
            return null;
        }
        return next.getStartVertex();
    }

    public boolean isTwineableByPointers(HalfEdge twin) {
        HalfEdgeVertex thisStartVertex = this.getStartVertex();
        HalfEdgeVertex thisEndVertex = this.getEndVertex();
        HalfEdgeVertex twinStartVertex = twin.getStartVertex();
        HalfEdgeVertex twinEndVertex = twin.getEndVertex();

        return thisStartVertex == twinEndVertex && thisEndVertex == twinStartVertex;
    }

    public boolean isTwin(HalfEdge halfEdge) {
        if (halfEdge == null || halfEdge.twin == null) {
            return false;
        }

        if (this.twin == null) {
            return false;
        }

        return halfEdge.twin == this && this.twin == halfEdge;
    }

    public double getSquaredLength() {
        if (startVertex == null || next == null) {
            return -1;
        }
        return startVertex.getPosition().distanceSquared(next.getStartVertex().getPosition());
    }

    public double getLength() {
        return Math.sqrt(getSquaredLength());
    }

    public List<HalfEdge> getLoop(List<HalfEdge> resultHalfEdgesLoop) {
        if (resultHalfEdgesLoop == null) {
            resultHalfEdgesLoop = new ArrayList<>();
        }
        resultHalfEdgesLoop.add(this);
        HalfEdge nextHalfEdge = this.next;
        while (nextHalfEdge != null && nextHalfEdge != this) {
            resultHalfEdgesLoop.add(nextHalfEdge);
            nextHalfEdge = nextHalfEdge.next;
        }
        return resultHalfEdgesLoop;
    }

    public HalfEdge getPrev() {
        HalfEdge prev = this;
        while (prev.next != this) {
            prev = prev.next;
            if (prev == null) {
                return null;
            }
        }
        return prev;
    }

    public boolean isDegenerated() {
        HalfEdgeVertex startVertex = this.getStartVertex();
        HalfEdgeVertex endVertex = this.getEndVertex();

        return startVertex == endVertex;
    }

    public void breakRelations() {
        if (this.startVertex != null) {
            if (this.startVertex.getOutingHalfEdge() == this) {
                this.startVertex.setOutingHalfEdge(null);
            }
            this.startVertex = null;
        }

        if (this.face != null) {
            if (this.face.getHalfEdge() == this) {
                this.face.setHalfEdge(null);
            }
            this.face = null;
        }

        if (this.next != null) {
            this.next = null;
        }

        if (this.twin != null) {
            this.twin.twin = null;
            this.twin = null;
        }
    }

    public void setItselfAsOutingHalfEdgeToTheStartVertex() {
        if (this.startVertex != null) {
            this.startVertex.setOutingHalfEdge(this);
        }
    }

    public boolean isApplauseEdge() {
        if (this.twin == null) {
            return false;
        }

        HalfEdgeFace face1 = this.face;
        HalfEdgeFace face2 = this.twin.face;

        if (face1 == null || face2 == null) {
            return false;
        }

        return face1.isApplauseFace(face2);
    }

    public boolean getIntersectionByPlane(PlaneType planeType, Vector3d planePosition, HalfEdgeVertex resultIntesectionVertex, double error) {
        //**************************************************************
        // Note : for OBLIQUE planes, override this method.
        //**************************************************************
        boolean intersection = false;

        if (planeType == PlaneType.XY) {
            // TODO: Implement this method.
        } else if (planeType == PlaneType.YZ) {
            intersection = getIntersectionByPlaneYZ(planePosition, resultIntesectionVertex, error);
        } else if (planeType == PlaneType.XZ) {
            intersection = getIntersectionByPlaneXZ(planePosition, resultIntesectionVertex, error);
        }

        return intersection;
    }

    private boolean getIntersectionByPlaneXY(Vector3d planePosition, HalfEdgeVertex resultIntesectionVertex, double error) {
        // TODO: Implement this method.

        return true;
    }

    private boolean getIntersectionByPlaneYZ(Vector3d planePosition, HalfEdgeVertex resultIntesectionVertex, double error) {
        // check if the startPoint or endPoint touches the plane
        HalfEdgeVertex startVertex = this.startVertex;
        HalfEdgeVertex endVertex = this.getEndVertex();
        Vector3d startVertexPosition = startVertex.getPosition();
        Vector3d endVertexPosition = endVertex.getPosition();
        Vector3d resultIntersectionPoint = new Vector3d();

        if (Math.abs(startVertexPosition.x - planePosition.x) < error) {
            return false;
        } else if (Math.abs(endVertexPosition.x - planePosition.x) < error) {
            return false;
        }

        // check if the startPoint and the endPoint are on the same side of the plane
        if ((startVertexPosition.x - planePosition.x) * (endVertexPosition.x - planePosition.x) > 0) {
            return false;
        }

        // calculate the intersection point
        double t = (planePosition.x - startVertexPosition.x) / (endVertexPosition.x - startVertexPosition.x);
        resultIntersectionPoint.set(planePosition.x,
                startVertexPosition.y + t * (endVertexPosition.y - startVertexPosition.y),
                startVertexPosition.z + t * (endVertexPosition.z - startVertexPosition.z));

        resultIntesectionVertex.setPosition(resultIntersectionPoint);

        // calculate the intersection normal
        if (startVertex.getNormal() != null && endVertex.getNormal() != null) {
            Vector3d resultIntersectionNormal = new Vector3d();
            Vector3d startVertexNormal = startVertex.getNormal();
            Vector3d endVertexNormal = endVertex.getNormal();
            resultIntersectionNormal.set(startVertexNormal.x + t * (endVertexNormal.x - startVertexNormal.x),
                    startVertexNormal.y + t * (endVertexNormal.y - startVertexNormal.y),
                    startVertexNormal.z + t * (endVertexNormal.z - startVertexNormal.z));

            resultIntesectionVertex.setNormal(resultIntersectionNormal);
        }

        // calculate the intersection texCoord
        if (startVertex.getTexcoords() != null && endVertex.getTexcoords() != null) {
            Vector2d resultIntersectionTexCoord = new Vector2d();
            Vector2d startVertexTexCoord = startVertex.getTexcoords();
            Vector2d endVertexTexCoord = endVertex.getTexcoords();
            resultIntersectionTexCoord.set(startVertexTexCoord.x + t * (endVertexTexCoord.x - startVertexTexCoord.x),
                    startVertexTexCoord.y + t * (endVertexTexCoord.y - startVertexTexCoord.y));

            resultIntesectionVertex.setTexcoords(resultIntersectionTexCoord);
        }

        // calculate the intersection color
        if (startVertex.getColor() != null && endVertex.getColor() != null) {
            byte[] startVertexColor = startVertex.getColor();
            byte[] endVertexColor = endVertex.getColor();
            byte[] resultIntersectionColor = new byte[4];
            for (int i = 0; i < 4; i++) {
                resultIntersectionColor[i] = (byte) (startVertexColor[i] + t * (endVertexColor[i] - startVertexColor[i]));
            }

            resultIntesectionVertex.setColor(resultIntersectionColor);
        }

        return true;
    }

    private boolean getIntersectionByPlaneXZ(Vector3d planePosition, HalfEdgeVertex resultIntesectionVertex, double error) {
        // check if the startPoint or endPoint touches the plane
        HalfEdgeVertex startVertex = this.startVertex;
        HalfEdgeVertex endVertex = this.getEndVertex();
        Vector3d startVertexPosition = startVertex.getPosition();
        Vector3d endVertexPosition = endVertex.getPosition();
        Vector3d resultIntersectionPoint = new Vector3d();

        if (Math.abs(startVertexPosition.y - planePosition.y) < error) {
            return false;
        } else if (Math.abs(endVertexPosition.y - planePosition.y) < error) {
            return false;
        }

        // check if the startPoint and the endPoint are on the same side of the plane
        if ((startVertexPosition.y - planePosition.y) * (endVertexPosition.y - planePosition.y) > 0) {
            return false;
        }

        // calculate the intersection point
        double t = (planePosition.y - startVertexPosition.y) / (endVertexPosition.y - startVertexPosition.y);
        resultIntersectionPoint.set(startVertexPosition.x + t * (endVertexPosition.x - startVertexPosition.x),
                planePosition.y,
                startVertexPosition.z + t * (endVertexPosition.z - startVertexPosition.z));

        resultIntesectionVertex.setPosition(resultIntersectionPoint);

        // calculate the intersection normal
        if (startVertex.getNormal() != null && endVertex.getNormal() != null) {
            Vector3d resultIntersectionNormal = new Vector3d();
            Vector3d startVertexNormal = startVertex.getNormal();
            Vector3d endVertexNormal = endVertex.getNormal();
            resultIntersectionNormal.set(startVertexNormal.x + t * (endVertexNormal.x - startVertexNormal.x),
                    startVertexNormal.y + t * (endVertexNormal.y - startVertexNormal.y),
                    startVertexNormal.z + t * (endVertexNormal.z - startVertexNormal.z));

            resultIntesectionVertex.setNormal(resultIntersectionNormal);
        }

        // calculate the intersection texCoord
        if (startVertex.getTexcoords() != null && endVertex.getTexcoords() != null) {
            Vector2d resultIntersectionTexCoord = new Vector2d();
            Vector2d startVertexTexCoord = startVertex.getTexcoords();
            Vector2d endVertexTexCoord = endVertex.getTexcoords();
            resultIntersectionTexCoord.set(startVertexTexCoord.x + t * (endVertexTexCoord.x - startVertexTexCoord.x),
                    startVertexTexCoord.y + t * (endVertexTexCoord.y - startVertexTexCoord.y));

            resultIntesectionVertex.setTexcoords(resultIntersectionTexCoord);
        }

        // calculate the intersection color
        if (startVertex.getColor() != null && endVertex.getColor() != null) {
            byte[] startVertexColor = startVertex.getColor();
            byte[] endVertexColor = endVertex.getColor();
            byte[] resultIntersectionColor = new byte[4];
            for (int i = 0; i < 4; i++) {
                resultIntersectionColor[i] = (byte) (startVertexColor[i] + t * (endVertexColor[i] - startVertexColor[i]));
            }

            resultIntesectionVertex.setColor(resultIntersectionColor);
        }

        return true;
    }

    public void writeFile(ObjectOutputStream outputStream) {
        /*
        public String note = null;
        private HalfEdge twin = null;
        private HalfEdge next = null;
        private HalfEdgeVertex startVertex = null;
        private HalfEdgeFace face = null;
        private ObjectStatus status = ObjectStatus.ACTIVE;
        private int id = -1;
        private int twinId = -1;
        private int nextId = -1;
        private int startVertexId = -1;
        private int faceId = -1;
         */

        try {
            // twinId
            int twinId = twin == null ? -1 : twin.id;
            outputStream.writeInt(twinId);
            // nextId
            int nextId = next == null ? -1 : next.id;
            outputStream.writeInt(nextId);
            // startVertexId
            int startVertexId = startVertex == null ? -1 : startVertex.getId();
            outputStream.writeInt(startVertexId);
            // faceId
            int faceId = face == null ? -1 : face.getId();
            outputStream.writeInt(faceId);
            // status
            outputStream.writeObject(status);
            // id
            outputStream.writeInt(id);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void readFile(ObjectInputStream inputStream) {
        try {
            // twinId
            twinId = inputStream.readInt();
            // nextId
            nextId = inputStream.readInt();
            // startVertexId
            startVertexId = inputStream.readInt();
            // faceId
            faceId = inputStream.readInt();
            // status
            status = (ObjectStatus) inputStream.readObject();
            // id
            id = inputStream.readInt();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}