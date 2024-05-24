package com.gaia3d.basic.structure;

import com.gaia3d.util.io.BigEndianDataInputStream;
import com.gaia3d.util.io.BigEndianDataOutputStream;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Slf4j
public class GaiaVertex {
    private GaiaHalfEdge outingHEdge = null;
    private Vector3d position = new Vector3d();
    private Vector3f normal = null;
    private int id = -1;
    private int outingHEdgeId = -1;
    private GaiaObjectStatus objectStatus = GaiaObjectStatus.ACTIVE;

    public void deleteObjects() {
        outingHEdge = null;
        position = null;
    }

    public boolean isCoincidentVertex(GaiaVertex vertex, double error) {
        if (vertex == null) {
            return false;
        }

        // Test debug:***
        /*double xDiff = Math.abs(this.getPosition().x - vertex.getPosition().x);
        double yDiff = Math.abs(this.getPosition().y - vertex.getPosition().y);
        double zDiff = Math.abs(this.getPosition().z - vertex.getPosition().z);*/
        // end test debug

        return Math.abs(this.getPosition().x - vertex.getPosition().x) < error && Math.abs(this.getPosition().y - vertex.getPosition().y) < error && Math.abs(this.getPosition().z - vertex.getPosition().z) < error;
    }

    public void avoidOutingHalfEdge(GaiaHalfEdge avoidOutingHalfEdge) {
        // if this outingHEdge is the avoidOutingHalfEdge, then must change it
        if (this.outingHEdge != avoidOutingHalfEdge) {
            return;
        }

        List<GaiaHalfEdge> allOutingHalfEdges = this.getAllOutingHalfEdges();
        for (GaiaHalfEdge outingHalfEdge : allOutingHalfEdges) {
            if (outingHalfEdge != avoidOutingHalfEdge) {
                this.outingHEdge = outingHalfEdge;
                break;
            }
        }
    }

    public void calculateNormal() {
        if (this.normal == null) {
            this.normal = new Vector3f();
        }

        this.normal.set(0, 0, 0);
        List<GaiaHalfEdge> outingHalfEdges = this.getAllOutingHalfEdges();
        for (GaiaHalfEdge outingHalfEdge : outingHalfEdges) {
            GaiaTriangle triangle = outingHalfEdge.getTriangle();
            if (triangle == null) {
                continue;
            }

            Vector3f normal = triangle.getNormal();
            if (normal == null) {
                continue;
            }

            this.normal.add(normal);
        }

        this.normal.normalize();
    }

    public List<GaiaHalfEdge> getAllOutingHalfEdges() {
        List<GaiaHalfEdge> outingHalfEdges = new ArrayList<>();

        // there are 2 cases: this vertex is interior vertex or boundary vertex, but we dont know
        // 1- interior vertex
        // 2- boundary vertex
        if (this.outingHEdge == null) {
            // error
            log.warn("This vertex has no outingHEdge. id : {}", this.id);
        }

        GaiaHalfEdge firstHalfEdge = this.outingHEdge;
        GaiaHalfEdge currHalfEdge = this.outingHEdge;
        outingHalfEdges.add(this.outingHEdge); // put the first halfEdge
        boolean finished = false;
        boolean isInteriorVertex = true;
        while (!finished) {
            GaiaHalfEdge twinHalfEdge = currHalfEdge.getTwin();
            if (twinHalfEdge == null) {
                finished = true;
                isInteriorVertex = false;
                break;
            }
            GaiaHalfEdge nextHalfEdge = twinHalfEdge.getNext();
            if (nextHalfEdge == null) {
                finished = true;
                isInteriorVertex = false;
                break;
            } else if (nextHalfEdge == firstHalfEdge) {
                finished = true;
                break;
            }

            outingHalfEdges.add(nextHalfEdge);
            currHalfEdge = nextHalfEdge;
        }

        // if this vertex is NO interior vertex, then must check if there are more outing halfEdges
        if (!isInteriorVertex) {
            // check if there are more outing halfEdges
            currHalfEdge = this.outingHEdge;
            finished = false;
            while (!finished) {
                GaiaHalfEdge prevHalfEdge = currHalfEdge.getPrev();
                GaiaHalfEdge twinHalfEdge = prevHalfEdge.getTwin();
                if (twinHalfEdge == null) {
                    finished = true;
                    break;
                }
                outingHalfEdges.add(twinHalfEdge);
                if (outingHalfEdges.size() > 4) {
                    break;
                }
                currHalfEdge = twinHalfEdge;
            }
        }
        return outingHalfEdges;
    }


    public void saveDataOutputStream(BigEndianDataOutputStream dataOutputStream) {
        try {
            // 1rst, save id
            dataOutputStream.writeInt(id);
            dataOutputStream.writeDouble(position.x);
            dataOutputStream.writeDouble(position.y);
            dataOutputStream.writeDouble(position.z);

            // 2nd, save outingHEdge
            if (outingHEdge != null) {
                dataOutputStream.writeInt(outingHEdge.getId());
            } else {
                dataOutputStream.writeInt(-1);
            }
        } catch (Exception e) {
            log.error("{}", e.getMessage());
        }
    }

    public void loadDataInputStream(BigEndianDataInputStream dataInputStream) throws IOException {
        this.id = dataInputStream.readInt();
        this.getPosition().x = dataInputStream.readDouble();
        this.getPosition().y = dataInputStream.readDouble();
        this.getPosition().z = dataInputStream.readDouble();

        this.outingHEdgeId = dataInputStream.readInt();
    }
}
