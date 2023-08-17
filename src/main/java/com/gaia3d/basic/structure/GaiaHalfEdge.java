package com.gaia3d.basic.structure;

public class GaiaHalfEdge {
    public GaiaVertex startVertex = null;
    public GaiaHalfEdge next = null;
    public GaiaHalfEdge twin = null;
    public GaiaTriangle triangle = null;

    public void setTwin(GaiaHalfEdge twin) {
        this.twin = twin;
        twin.twin = this;
    }

    public void setStartVertex(GaiaVertex startVertex) {
        this.startVertex = startVertex;
        startVertex.outingHEdge = this;
    }
}
