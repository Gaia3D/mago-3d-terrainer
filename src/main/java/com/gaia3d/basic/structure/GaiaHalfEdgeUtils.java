package com.gaia3d.basic.structure;

import java.util.ArrayList;

public class GaiaHalfEdgeUtils {

    public static void concatenateHalfEdgesLoop(ArrayList<GaiaHalfEdge> halfEdgesArray) {
        for (int i = 0; i < halfEdgesArray.size(); i++) {
            GaiaHalfEdge halfEdge = halfEdgesArray.get(i);
            GaiaHalfEdge nextHalfEdge = halfEdgesArray.get((i + 1) % halfEdgesArray.size());
            halfEdge.next = nextHalfEdge;
        }
    }
}
