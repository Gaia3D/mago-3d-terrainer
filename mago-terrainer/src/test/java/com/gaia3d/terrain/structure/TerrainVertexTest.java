package com.gaia3d.terrain.structure;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TerrainVertexTest {
    @Test
    @Tag("default")
    void outgoingHalfEdgeTraversalReusesAndClearsResultList() {
        TerrainVertex vertex = new TerrainVertex();
        TerrainHalfEdge outgoing = new TerrainHalfEdge();
        outgoing.setStartVertex(vertex);
        vertex.setOutingHEdge(outgoing);
        List<TerrainHalfEdge> result = new ArrayList<>();
        result.add(new TerrainHalfEdge());

        List<TerrainHalfEdge> returned = vertex.getAllOutingHalfEdges(result);

        assertSame(result, returned);
        assertEquals(List.of(outgoing), returned);
    }
}
