package com.gaia3d.basic.structure;

import java.util.List;

public class GaiaHalfEdgeUtils {

    public static void concatenateHalfEdgesLoop(List<GaiaHalfEdge> halfEdgesArray) {
        for (int i = 0; i < halfEdgesArray.size(); i++) {
            GaiaHalfEdge halfEdge = halfEdgesArray.get(i);
            GaiaHalfEdge nextHalfEdge = halfEdgesArray.get((i + 1) % halfEdgesArray.size());
            halfEdge.setNext(nextHalfEdge);
        }
    }

    public static void concatenate3HalfEdgesLoop(GaiaHalfEdge hedge1, GaiaHalfEdge hedge2, GaiaHalfEdge hedge3) {
        hedge1.setNext(hedge2);
        hedge2.setNext(hedge3);
        hedge3.setNext(hedge1);
    }
}
