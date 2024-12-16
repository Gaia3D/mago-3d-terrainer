package com.gaia3d.basic.structure;

import com.gaia3d.basic.types.TerrainObjectStatus;
import com.gaia3d.io.BigEndianDataInputStream;
import com.gaia3d.io.BigEndianDataOutputStream;
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
public class TerrainVertex {
    private TerrainHalfEdge outingHEdge = null;
    private Vector3d position = new Vector3d();
    private Vector3f normal = null;
    private int id = -1;
    private int outingHEdgeId = -1;
    private TerrainObjectStatus objectStatus = TerrainObjectStatus.ACTIVE;

    public void deleteObjects() {
        outingHEdge = null;
        position = null;
    }

    public boolean isCoincidentVertex(TerrainVertex vertex, double error) {
        if (vertex == null) {
            return false;
        }
        return Math.abs(this.getPosition().x - vertex.getPosition().x) < error && Math.abs(this.getPosition().y - vertex.getPosition().y) < error && Math.abs(this.getPosition().z - vertex.getPosition().z) < error;
    }

    public void avoidOutingHalfEdge(TerrainHalfEdge avoidOutingHalfEdge) {
        // if this outingHEdge is the avoidOutingHalfEdge, then must change it
        if (this.outingHEdge != avoidOutingHalfEdge) {
            return;
        }

        List<TerrainHalfEdge> allOutingHalfEdges = this.getAllOutingHalfEdges();
        for (TerrainHalfEdge outingHalfEdge : allOutingHalfEdges) {
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
        List<TerrainHalfEdge> outingHalfEdges = this.getAllOutingHalfEdges();
        for (TerrainHalfEdge outingHalfEdge : outingHalfEdges) {
            TerrainTriangle triangle = outingHalfEdge.getTriangle();
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

    public List<TerrainHalfEdge> getAllOutingHalfEdges() {
        List<TerrainHalfEdge> outingHalfEdges = new ArrayList<>();

        // there are 2 cases: this vertex is interior vertex or boundary vertex, but we dont know
        // 1- interior vertex
        // 2- boundary vertex
        if (this.outingHEdge == null) {
            // error
            log.warn("This vertex has no outingHEdge. id : {}", this.id);
        }

        TerrainHalfEdge firstHalfEdge = this.outingHEdge;
        TerrainHalfEdge currHalfEdge = this.outingHEdge;
        outingHalfEdges.add(this.outingHEdge); // put the first halfEdge
        boolean finished = false;
        boolean isInteriorVertex = true;
        while (!finished) {
            TerrainHalfEdge twinHalfEdge = currHalfEdge.getTwin();
            if (twinHalfEdge == null) {
                finished = true;
                isInteriorVertex = false;
                break;
            }
            TerrainHalfEdge nextHalfEdge = twinHalfEdge.getNext();
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
                TerrainHalfEdge prevHalfEdge = currHalfEdge.getPrev();
                TerrainHalfEdge twinHalfEdge = prevHalfEdge.getTwin();
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
            log.error("Error:", e);
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
