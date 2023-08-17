package com.gaia3d.basic.structure;

import java.util.ArrayList;

public class GaiaMesh {
    public ArrayList<GaiaVertex> vertices = null;
    public ArrayList<GaiaTriangle> triangles = null;

    public ArrayList<GaiaHalfEdge> halfEdges = null;

    public GaiaMesh() {
        vertices = new ArrayList<GaiaVertex>();
        triangles = new ArrayList<GaiaTriangle>();
        halfEdges = new ArrayList<GaiaHalfEdge>();
    }

    public GaiaVertex newVertex() {
        GaiaVertex vertex = new GaiaVertex();
        vertices.add(vertex);
        return vertex;
    }

    public GaiaTriangle newTriangle() {
        GaiaTriangle triangle = new GaiaTriangle();
        triangles.add(triangle);
        return triangle;
    }

    public GaiaHalfEdge newHalfEdge() {
        GaiaHalfEdge halfEdge = new GaiaHalfEdge();
        halfEdges.add(halfEdge);
        return halfEdge;
    }

}
