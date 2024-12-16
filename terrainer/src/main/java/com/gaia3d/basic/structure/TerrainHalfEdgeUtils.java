package com.gaia3d.basic.structure;

import java.util.List;

public class TerrainHalfEdgeUtils {
    public static void concatenateHalfEdgesLoop(List<TerrainHalfEdge> halfEdgesArray) {
        for (int i = 0; i < halfEdgesArray.size(); i++) {
            TerrainHalfEdge halfEdge = halfEdgesArray.get(i);
            TerrainHalfEdge nextHalfEdge = halfEdgesArray.get((i + 1) % halfEdgesArray.size());
            halfEdge.setNext(nextHalfEdge);
        }
    }

    public static void concatenate3HalfEdgesLoop(TerrainHalfEdge hedge1, TerrainHalfEdge hedge2, TerrainHalfEdge hedge3) {
        hedge1.setNext(hedge2);
        hedge2.setNext(hedge3);
        hedge3.setNext(hedge1);
    }
}
