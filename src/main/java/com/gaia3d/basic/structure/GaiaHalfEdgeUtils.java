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

    public static void concatenate3HalfEdgesLoop(GaiaHalfEdge hedge1, GaiaHalfEdge hedge2, GaiaHalfEdge hedge3) {
        hedge1.next = hedge2;
        hedge2.next = hedge3;
        hedge3.next = hedge1;
    }
}
