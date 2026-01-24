package com.gaia3d.terrain.structure;

import com.gaia3d.io.BigEndianDataInputStream;
import com.gaia3d.io.BigEndianDataOutputStream;
import com.gaia3d.terrain.types.TerrainObjectStatus;
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

    public boolean isCoincidentVertexXY(TerrainVertex vertex, double error) {
        if (vertex == null) {
            return false;
        }

        return Math.abs(this.getPosition().x - vertex.getPosition().x) < error && Math.abs(this.getPosition().y - vertex.getPosition().y) < error;
    }

    public void avoidOutingHalfEdge(TerrainHalfEdge avoidOutingHalfEdge) {
        // Early exit if outingHEdge is null
        if (this.outingHEdge == null) {
            log.debug("Cannot avoid outingHalfEdge for vertex {} - no outingHEdge exists", this.id);
            return;
        }

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

        // Early exit if outingHEdge is null with safe default
        if (this.outingHEdge == null) {
            log.warn("Cannot calculate normal for vertex {} - no outingHEdge. Using default normal.", this.id);
            this.normal.set(0, 0, 1);
            return;
        }

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

        // if this vertex has no normal, then set default normal
        if (this.normal.equals(0, 0, 0)) {
            log.warn("This vertex has no normal. id : {}", this.id);
            this.normal.set(0, 0, 1);
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
            return outingHalfEdges; // Early return with empty list to prevent NPE
        }

        if (this.outingHEdge.getObjectStatus() == TerrainObjectStatus.DELETED) {
            log.warn("This outingHEdge is DELETED. id : {}", this.id);
            return outingHalfEdges;
        }

        TerrainHalfEdge firstHalfEdge = this.outingHEdge;
        TerrainHalfEdge currHalfEdge = this.outingHEdge;
        outingHalfEdges.add(this.outingHEdge); // put the first halfEdge
        boolean finished = false;
        boolean isInteriorVertex = true;
        int counter = 0;
        int maxIterations = 100; // Safety limit to prevent infinite loops
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

            counter++;
            if (counter > 10) {
                log.info("Info : This vertex has more than 10 outing halfEdges. id : {}", this.id);
            }

            // Safety check: prevent infinite loops in corrupted mesh topology
            if (counter >= maxIterations) {
                log.warn("Vertex {} has exceeded maximum iteration limit ({}) for outgoing half-edges. " +
                         "Mesh topology may be corrupted. Breaking loop.",
                         this.id, maxIterations);
                finished = true;
                break;
            }
        }

        // if this vertex is NO interior vertex, then must check if there are more outing halfEdges
        if (!isInteriorVertex) {
            // check if there are more outing halfEdges
            currHalfEdge = this.outingHEdge;
            finished = false;
            while (!finished) {
                TerrainHalfEdge prevHalfEdge = currHalfEdge.getPrev();
                if (prevHalfEdge == null || prevHalfEdge.getObjectStatus() == TerrainObjectStatus.DELETED) {
                    finished = true;
                    break;
                }
                TerrainHalfEdge twinHalfEdge = prevHalfEdge.getTwin();
                if (twinHalfEdge == null || twinHalfEdge.getObjectStatus() == TerrainObjectStatus.DELETED) {
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

    /**
     * Result class for topology validation
     */
    public static class TopologyValidationResult {
        public final boolean isValid;
        public final int edgeCount;
        public final boolean hasMultipleLoops;
        public final LoopClosureType loopClosureType;
        public final boolean hitIterationLimit;

        public TopologyValidationResult(boolean isValid, int edgeCount, boolean hasMultipleLoops,
                                       LoopClosureType loopClosureType, boolean hitIterationLimit) {
            this.isValid = isValid;
            this.edgeCount = edgeCount;
            this.hasMultipleLoops = hasMultipleLoops;
            this.loopClosureType = loopClosureType;
            this.hitIterationLimit = hitIterationLimit;
        }

        public enum LoopClosureType {
            INTERIOR,    // Closed loop (interior vertex)
            BOUNDARY,    // Open edges (boundary vertex)
            CORRUPTED    // Hit iteration limit or other anomaly
        }
    }

    /**
     * Validates that this vertex has proper topology (single continuous edge loop for manifold mesh)
     *
     * @param maxExpectedEdges Maximum expected edges (typically 6-10 for manifold, max 15 for tile boundaries)
     * @return TopologyValidationResult with validation metrics
     */
    public TopologyValidationResult validateTopology(int maxExpectedEdges) {
        if (this.outingHEdge == null) {
            log.warn("Vertex {} has no outingHEdge during validation", this.id);
            return new TopologyValidationResult(false, 0, false,
                    TopologyValidationResult.LoopClosureType.CORRUPTED, false);
        }

        if (this.outingHEdge.getObjectStatus() == TerrainObjectStatus.DELETED) {
            log.warn("Vertex {} has DELETED outingHEdge during validation", this.id);
            return new TopologyValidationResult(false, 0, false,
                    TopologyValidationResult.LoopClosureType.CORRUPTED, false);
        }

        // Traverse the half-edge loop to count edges and detect topology issues
        TerrainHalfEdge firstHalfEdge = this.outingHEdge;
        TerrainHalfEdge currHalfEdge = this.outingHEdge;
        int edgeCount = 1; // Start with first edge
        boolean finished = false;
        boolean isInteriorVertex = true;
        boolean hitIterationLimit = false;
        int maxIterations = 100; // Same as getAllOutingHalfEdges()

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
                // Loop closed properly
                finished = true;
                break;
            }

            edgeCount++;
            currHalfEdge = nextHalfEdge;

            // Safety check: detect infinite loops or corrupted topology
            if (edgeCount >= maxIterations) {
                log.warn("Vertex {} exceeded max iterations ({}) during validation - likely corrupted topology",
                        this.id, maxIterations);
                hitIterationLimit = true;
                finished = true;
                break;
            }
        }

        // Determine loop closure type
        TopologyValidationResult.LoopClosureType closureType;
        if (hitIterationLimit) {
            closureType = TopologyValidationResult.LoopClosureType.CORRUPTED;
        } else if (isInteriorVertex) {
            closureType = TopologyValidationResult.LoopClosureType.INTERIOR;
        } else {
            closureType = TopologyValidationResult.LoopClosureType.BOUNDARY;
        }

        // Determine if topology is valid
        boolean hasMultipleLoops = hitIterationLimit; // If we hit limit, assume multiple disconnected loops
        boolean isValid = !hitIterationLimit && edgeCount <= maxExpectedEdges;

        return new TopologyValidationResult(isValid, edgeCount, hasMultipleLoops,
                closureType, hitIterationLimit);
    }


    public void saveDataOutputStream(BigEndianDataOutputStream dataOutputStream) {
        try {
            // First, save id
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
