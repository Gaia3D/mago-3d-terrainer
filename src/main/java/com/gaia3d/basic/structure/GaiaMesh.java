package com.gaia3d.basic.structure;

import com.gaia3d.util.io.LittleEndianDataInputStream;
import com.gaia3d.util.io.LittleEndianDataOutputStream;
import org.joml.Vector3d;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class GaiaMesh {
    public ArrayList<GaiaVertex> vertices = null;
    public ArrayList<GaiaTriangle> triangles = null;

    public ArrayList<GaiaHalfEdge> halfEdges = null;

    public int id = -1;

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

    public void setVertexIdInList()
    {
        int verticesCount = vertices.size();
        for(int i=0; i<verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            vertex.id = i;
        }
    }

    public void removeDeletedObjects()
    {
        // 1rst, check vertices.***
        int verticesCount = vertices.size();
        for(int i=0; i<verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            if(vertex.objectStatus == GaiaObjectStatus.DELETED) {
                vertices.remove(i);
                i--;
                verticesCount--;
            }
        }

        // 2nd, check triangles.***
        int trianglesCount = triangles.size();
        for(int i=0; i<trianglesCount; i++) {
            GaiaTriangle triangle = triangles.get(i);
            if(triangle.objectStatus == GaiaObjectStatus.DELETED) {
                triangles.remove(i);
                i--;
                trianglesCount--;
            }
        }

        // 3rd, check halfEdges.***
        int halfEdgesCount = halfEdges.size();
        for(int i=0; i<halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = halfEdges.get(i);
            if(halfEdge.objectStatus == GaiaObjectStatus.DELETED) {
                halfEdges.remove(i);
                i--;
                halfEdgesCount--;
            }
        }
    }

    public void mergeMesh(GaiaMesh mesh) {
        // 1rst, add vertices.***
        int verticesCount = mesh.vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            GaiaVertex vertex = mesh.vertices.get(i);
            if(vertex.objectStatus == GaiaObjectStatus.DELETED) {
                continue;
            }
            vertices.add(vertex);
        }

        // 2nd, add triangles.***
        int trianglesCount = mesh.triangles.size();
        for (int i = 0; i < trianglesCount; i++) {
            GaiaTriangle triangle = mesh.triangles.get(i);
            if(triangle.objectStatus == GaiaObjectStatus.DELETED) {
                continue;
            }
            triangles.add(triangle);
        }

        // 3rd, add halfEdges.***
        int halfEdgesCount = mesh.halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = mesh.halfEdges.get(i);
            if(halfEdge.objectStatus == GaiaObjectStatus.DELETED) {
                continue;
            }
            halfEdges.add(halfEdge);
        }
    }

    public ArrayList<GaiaHalfEdge> getHalfEdgesByType(HalfEdgeType type)
    {
        ArrayList<GaiaHalfEdge> halfEdges = new ArrayList<GaiaHalfEdge>();
        int halfEdgesCount = this.halfEdges.size();
        for(int i=0; i<halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = this.halfEdges.get(i);
            if(halfEdge.type == type) {
                halfEdges.add(halfEdge);
            }
        }
        return halfEdges;
    }


    public void setTriangleIdInList()
    {
        int trianglesCount = triangles.size();
        for(int i=0; i<trianglesCount; i++) {
            GaiaTriangle triangle = triangles.get(i);
            triangle.id = i;
        }
    }

    public void setHalfEdgeIdInList()
    {
        int halfEdgesCount = halfEdges.size();
        for(int i=0; i<halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = halfEdges.get(i);
            halfEdge.id = i;
        }
    }

    public void setObjectsIdInList()
    {
        setVertexIdInList();
        setTriangleIdInList();
        setHalfEdgeIdInList();
    }

    public HashMap<Integer, GaiaVertex> getVerticesMap()
    {
        HashMap<Integer, GaiaVertex> verticesMap = new HashMap<Integer, GaiaVertex>();
        int verticesCount = vertices.size();
        for(int i=0; i<verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            verticesMap.put(vertex.id, vertex);
        }
        return verticesMap;
    }

    public HashMap<Integer, GaiaTriangle> getTrianglesMap()
    {
        HashMap<Integer, GaiaTriangle> trianglesMap = new HashMap<Integer, GaiaTriangle>();
        int trianglesCount = triangles.size();
        for(int i=0; i<trianglesCount; i++) {
            GaiaTriangle triangle = triangles.get(i);
            trianglesMap.put(triangle.id, triangle);
        }
        return trianglesMap;
    }

    public HashMap<Integer, GaiaHalfEdge> getHalfEdgesMap()
    {
        HashMap<Integer, GaiaHalfEdge> halfEdgesMap = new HashMap<Integer, GaiaHalfEdge>();
        int halfEdgesCount = halfEdges.size();
        for(int i=0; i<halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = halfEdges.get(i);
            halfEdgesMap.put(halfEdge.id, halfEdge);
        }
        return halfEdgesMap;
    }

    private void disableHalfEdge(GaiaHalfEdge halfEdge)
    {
        halfEdge.objectStatus = GaiaObjectStatus.DELETED;

        // now, disable the twin.***
        //GaiaHalfEdge twin = halfEdge.twin;
        //twin.twin = null;
        halfEdge.twin = null;

        // now, disable the next.***
        halfEdge.next = null;

        // now, disable the triangle.***
        halfEdge.triangle.halfEdge = null;
        halfEdge.triangle = null;

        // now, disable the startVertex.***
        halfEdge.startVertex.avoidOutingHalfEdge(halfEdge);
        halfEdge.startVertex = null;
    }

    private void disableTriangle(GaiaTriangle triangle)
    {
        triangle.objectStatus = GaiaObjectStatus.DELETED;
        triangle.halfEdge = null;

        // now, disable the halfEdges.***
        GaiaHalfEdge halfEdge = triangle.halfEdge;
        if(halfEdge != null)
        {
            GaiaHalfEdge nextHalfEdge = halfEdge.next;
            GaiaHalfEdge nextNextHalfEdge = nextHalfEdge.next;

            disableHalfEdge(halfEdge);
            disableHalfEdge(nextHalfEdge);
            disableHalfEdge(nextNextHalfEdge);
        }

    }



    public ArrayList<GaiaTriangle> splitTriangle(GaiaTriangle triangle)
    {
        // A triangle is split by the longest edge.***
        // so, the longest edge of the triangle must be the longest edge of the adjacentTriangle.***
        // If the longest edge of the adjacentTriangle is not the longest edge of the triangle, then must split the adjacentTriangle first.***
        // If the adjacentTriangle is null, then the triangle is splittable.***
        ArrayList<GaiaTriangle> newTriangles = new ArrayList<GaiaTriangle>();

        GaiaTriangle adjacentTriangle = getSplittableAdjacentTriangle(triangle);
        if(adjacentTriangle == null)
        {
            // the triangle is border triangle, so is splittable.***
            GaiaHalfEdge longestHEdge = triangle.getLongestHalfEdge();
            GaiaHalfEdge prevHEdge = longestHEdge.getPrev();
            GaiaHalfEdge nextHEdge = longestHEdge.getNext();

            // keep the twin of the longestHEdge, prevHEdge and nextHEdge.***
            GaiaHalfEdge longestHEdge_twin = longestHEdge.twin;
            GaiaHalfEdge prevHEdge_twin = prevHEdge.twin;
            GaiaHalfEdge nextHEdge_twin = nextHEdge.twin;

            // in this case the twin is null.***
            Vector3d midPosition = longestHEdge.getMidPosition();
            GaiaVertex midVertex = newVertex();
            midVertex.position = midPosition;

            // find the opposite vertex of the longestHEdge.***
            // In a triangle, the opposite vertex of the longestHEdge is the startVertex of the prevHEdge of the longestHEdge.***

            GaiaVertex oppositeVertex = prevHEdge.getStartVertex();

            //                        oppositeVertex
            //                            / \
            //                         /       \
            //  longestEdge_prev--->/             \<-- longestHEdge_next
            //                   /        T          \
            //                /                         \
            //             /                               \
            //          +------------------+------------------+
            //                        midVertex    ^
            //                                     |
            //                                     |
            //                                     +-- longestHEdge

            // split the triangle.***
            // 1rst, create 2 new triangles.***

            //                      oppositeVertex
            //                            / \
            //                         /   |   \
            //                      /      |      \
            //                   /         |         \
            //                /    A       |     B      \
            //             /   halfEdge_A1 | halfEdge_B1   \
            //          +------------------+------------------+ <-- longestHEdge_endVertex
            //          ^            midVertex    ^
            //          |                         |
            //          |                         |
            //          |                         +-- longestHEdge
            //          |
            //          +-- longestHEdge_startVertex

            GaiaVertex longestHEdge_startVertex = longestHEdge.getStartVertex();
            GaiaVertex longestHEdge_endVertex = longestHEdge.getEndVertex();

            // Triangle_A.***
            GaiaHalfEdge halfEdge_A1 = newHalfEdge();
            GaiaHalfEdge halfEdge_A2 = newHalfEdge();
            GaiaHalfEdge halfEdge_A3 = newHalfEdge();

            // set vertex to the new halfEdges.***
            halfEdge_A1.setStartVertex(longestHEdge_startVertex);
            halfEdge_A2.setStartVertex(midVertex);
            halfEdge_A3.setStartVertex(oppositeVertex);

            GaiaHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdge_A1, halfEdge_A2, halfEdge_A3);
            GaiaTriangle triangleA = newTriangle();
            triangleA.setHalfEdge(halfEdge_A1);
            triangleA.ownerTile_tileIndices.copyFrom(triangle.ownerTile_tileIndices);

            // put the new triangle in the result list.***
            newTriangles.add(triangleA);

            // Triangle_B.***
            GaiaHalfEdge halfEdge_B1 = newHalfEdge();
            GaiaHalfEdge halfEdge_B2 = newHalfEdge();
            GaiaHalfEdge halfEdge_B3 = newHalfEdge();

            // set vertex to the new halfEdges.***
            halfEdge_B1.setStartVertex(midVertex);
            halfEdge_B2.setStartVertex(longestHEdge_endVertex);
            halfEdge_B3.setStartVertex(oppositeVertex);

            GaiaHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdge_B1, halfEdge_B2, halfEdge_B3);
            GaiaTriangle triangleB = newTriangle();
            triangleB.setHalfEdge(halfEdge_B1);
            triangleB.ownerTile_tileIndices.copyFrom(triangle.ownerTile_tileIndices);

            // put the new triangle in the result list.***
            newTriangles.add(triangleB);

            // now, set the twins.***
            // the halfEdge_A1 and halfEdge_B1 has no twins.***
            halfEdge_A2.setTwin(halfEdge_B3);
            halfEdge_A3.setTwin(prevHEdge_twin);
            halfEdge_B2.setTwin(nextHEdge_twin);

            // now set the triangles of halfEdges.***
            halfEdge_A1.setTriangle(triangleA);
            halfEdge_A2.setTriangle(triangleA);
            halfEdge_A3.setTriangle(triangleA);

            halfEdge_B1.setTriangle(triangleB);
            halfEdge_B2.setTriangle(triangleB);
            halfEdge_B3.setTriangle(triangleB);


            // now delete the triangle.***
            disableTriangle(triangle);
        }
        else
        {
            // split the 2 triangles.***
            //                                        oppVtx_T
            //                                          / \
            //                                       /       \
            //                                    /             \
            //             longestEdge_prev--->/         T         \<-- longestHEdge_next
            //                              /                         \
            //                           /         longestHEdge          \
            //  longestHEdge_strVtx--> +-------------------------------------+  <-- longestHEdge_endVertex
            //                           \       longestHEdgeAdjT         /
            //                              \                          /
            //                                 \       adjT         /<-- longestHEdgeAdjT_prev
            //            longestEdgeAdjT_next--->\              /
            //                                       \        /
            //                                          \  /
            //                                       oppVtx_AdjT

            GaiaHalfEdge longestHEdge = triangle.getLongestHalfEdge();
            GaiaHalfEdge prevHEdge = longestHEdge.getPrev();
            GaiaHalfEdge nextHEdge = longestHEdge.getNext();

            GaiaHalfEdge longestHEdgeAdjT = adjacentTriangle.getLongestHalfEdge();
            GaiaHalfEdge prevHEdgeAdjT = longestHEdgeAdjT.getPrev();
            GaiaHalfEdge nextHEdgeAdjT = longestHEdgeAdjT.getNext();

            // keep the twin of the longestHEdge, prevHEdge and nextHEdge.***
            GaiaHalfEdge longestHEdge_twin = longestHEdge.twin;
            GaiaHalfEdge prevHEdge_twin = prevHEdge.twin;
            GaiaHalfEdge nextHEdge_twin = nextHEdge.twin;

            // keep the twin of the longestHEdgeAdjT, prevHEdgeAdjT and nextHEdgeAdjT.***
            GaiaHalfEdge longestHEdgeAdjT_twin = longestHEdgeAdjT.twin;
            GaiaHalfEdge prevHEdgeAdjT_twin = prevHEdgeAdjT.twin;
            GaiaHalfEdge nextHEdgeAdjT_twin = nextHEdgeAdjT.twin;

            // need know the oppVtx_T and oppVtx_AdjT.***
            GaiaVertex oppVtx_T = prevHEdge.getStartVertex();
            GaiaVertex oppVtx_AdjT = prevHEdgeAdjT.getStartVertex();

            // need know the midVertex.***
            Vector3d midPosition = longestHEdge.getMidPosition();
            GaiaVertex midVertex = newVertex();
            midVertex.position = midPosition;

            // need longEdge_startVertex and longEdge_endVertex.***
            GaiaVertex longEdge_startVertex = longestHEdge.getStartVertex();
            GaiaVertex longEdge_endVertex = longestHEdge.getEndVertex();


            // A triangle is split by the longest edge.***
            //                                        oppVtx_T
            //                                           / \
            //                                        /   |   \
            //                longestEdge_prev---> /      |      \ <-- longestHEdge_next
            //                                  /         |         \
            //                               /    A       |     B      \
            //                            /   halfEdge_A1 | halfEdge_B1   \
            // longestHEdge_strVtx-->  +------------------+------------------+ <-- longestHEdge_endVertex
            //                            \   halfEdge_C1 | halfEdge_D1   /
            //                               \      C     |     D      /
            //                                  \         |         / <-- longestHEdgeAdjT_prev
            //            longestEdgeAdjT_next---> \      |      /
            //                                        \   |   /
            //                                           \ /
            //                                        oppVtx_AdjT

            // split the triangle.***
            // 1rst, create 4 new triangles.***
            // triangle_A.***
            GaiaHalfEdge halfEdge_A1 = newHalfEdge();
            GaiaHalfEdge halfEdge_A2 = newHalfEdge();
            GaiaHalfEdge halfEdge_A3 = newHalfEdge();

            // set vertex to the new halfEdges.***
            halfEdge_A1.setStartVertex(longEdge_startVertex);
            halfEdge_A2.setStartVertex(midVertex);
            halfEdge_A3.setStartVertex(oppVtx_T);

            GaiaHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdge_A1, halfEdge_A2, halfEdge_A3);
            GaiaTriangle triangleA = newTriangle();
            triangleA.setHalfEdge(halfEdge_A1);
            triangleA.ownerTile_tileIndices.copyFrom(triangle.ownerTile_tileIndices);

            // put the new triangle in the result list.***
            newTriangles.add(triangleA);

            // triangle_B.***
            GaiaHalfEdge halfEdge_B1 = newHalfEdge();
            GaiaHalfEdge halfEdge_B2 = newHalfEdge();
            GaiaHalfEdge halfEdge_B3 = newHalfEdge();

            // set vertex to the new halfEdges.***
            halfEdge_B1.setStartVertex(midVertex);
            halfEdge_B2.setStartVertex(longEdge_endVertex);
            halfEdge_B3.setStartVertex(oppVtx_T);

            GaiaHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdge_B1, halfEdge_B2, halfEdge_B3);
            GaiaTriangle triangleB = newTriangle();
            triangleB.setHalfEdge(halfEdge_B1);
            triangleB.ownerTile_tileIndices.copyFrom(triangle.ownerTile_tileIndices);

            // put the new triangle in the result list.***
            newTriangles.add(triangleB);

            // triangle_C.***
            GaiaHalfEdge halfEdge_C1 = newHalfEdge();
            GaiaHalfEdge halfEdge_C2 = newHalfEdge();
            GaiaHalfEdge halfEdge_C3 = newHalfEdge();

            // set vertex to the new halfEdges.***
            halfEdge_C1.setStartVertex(midVertex);
            halfEdge_C2.setStartVertex(longEdge_startVertex);
            halfEdge_C3.setStartVertex(oppVtx_AdjT);

            GaiaHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdge_C1, halfEdge_C2, halfEdge_C3);
            GaiaTriangle triangleC = newTriangle();
            triangleC.setHalfEdge(halfEdge_C1);
            triangleC.ownerTile_tileIndices.copyFrom(adjacentTriangle.ownerTile_tileIndices);

            // put the new triangle in the result list.***
            newTriangles.add(triangleC);

            // triangle_D.***
            GaiaHalfEdge halfEdge_D1 = newHalfEdge();
            GaiaHalfEdge halfEdge_D2 = newHalfEdge();
            GaiaHalfEdge halfEdge_D3 = newHalfEdge();

            // set vertex to the new halfEdges.***
            halfEdge_D1.setStartVertex(longEdge_endVertex);
            halfEdge_D2.setStartVertex(midVertex);
            halfEdge_D3.setStartVertex(oppVtx_AdjT);

            GaiaHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdge_D1, halfEdge_D2, halfEdge_D3);
            GaiaTriangle triangleD = newTriangle();
            triangleD.setHalfEdge(halfEdge_D1);
            triangleD.ownerTile_tileIndices.copyFrom(adjacentTriangle.ownerTile_tileIndices);

            // put the new triangle in the result list.***
            newTriangles.add(triangleD);

            // now, set the twins.***
            // here, all newHEdges has twins.***
            halfEdge_A1.setTwin(halfEdge_C1);
            halfEdge_A2.setTwin(halfEdge_B3);
            halfEdge_A3.setTwin(prevHEdge_twin);

            halfEdge_B1.setTwin(halfEdge_D1);
            halfEdge_B2.setTwin(nextHEdge_twin);
            //halfEdge_B3.setTwin(halfEdge_A2); // redundant.***

            //halfEdge_C1.setTwin(halfEdge_A1); // redundant.***
            halfEdge_C2.setTwin(nextHEdgeAdjT_twin);
            halfEdge_C3.setTwin(halfEdge_D2);

            //halfEdge_D1.setTwin(halfEdge_B1); // redundant.***
            //halfEdge_D2.setTwin(halfEdge_C3); // redundant.***
            halfEdge_D3.setTwin(prevHEdgeAdjT_twin);

            // now set the triangles of halfEdges.***
            halfEdge_A1.setTriangle(triangleA);
            halfEdge_A2.setTriangle(triangleA);
            halfEdge_A3.setTriangle(triangleA);

            halfEdge_B1.setTriangle(triangleB);
            halfEdge_B2.setTriangle(triangleB);
            halfEdge_B3.setTriangle(triangleB);

            halfEdge_C1.setTriangle(triangleC);
            halfEdge_C2.setTriangle(triangleC);
            halfEdge_C3.setTriangle(triangleC);

            halfEdge_D1.setTriangle(triangleD);
            halfEdge_D2.setTriangle(triangleD);
            halfEdge_D3.setTriangle(triangleD);

            // now delete the triangles.***
            disableTriangle(triangle);
            disableTriangle(adjacentTriangle);
        }

        return newTriangles;
    }

    public GaiaTriangle getSplittableAdjacentTriangle(GaiaTriangle targetTriangle)
    {
        // A triangle is split by the longest edge.***
        // so, the longest edge of the triangle must be the longest edge of the adjacentTriangle.***
        // If the longest edge of the adjacentTriangle is not the longest edge of the triangle, then must split the adjacentTriangle first.***
        // If the adjacentTriangle is null, then the triangle is splittable.***

        GaiaHalfEdge longestHEdge = targetTriangle.getLongestHalfEdge();
        GaiaHalfEdge twin = longestHEdge.twin;

        if(twin == null)
        {
            return null;
        }

        GaiaTriangle adjacentTriangle = twin.triangle;
        GaiaHalfEdge longestHEdgeOfAdjacentTriangle = adjacentTriangle.getLongestHalfEdge();

        if(longestHEdgeOfAdjacentTriangle.twin == longestHEdge)
        {
            return adjacentTriangle;
        }
        else
        {
            // first split the adjacentTriangle.***;
            ArrayList<GaiaTriangle> newTriangles = splitTriangle(adjacentTriangle);

            // now search the new adjacentTriangle for the targetTriangle.***
            double vertexCoincidentError = 0.0000000000001; // use the TileWgs84Manager.vertexCoincidentError.***
            int newTrianglesCount = newTriangles.size();
            for(int i=0; i<newTrianglesCount; i++)
            {
                GaiaTriangle newTriangle = newTriangles.get(i);
                GaiaHalfEdge longestHEdgeOfNewTriangle = newTriangle.getLongestHalfEdge();
                if(longestHEdgeOfNewTriangle.isHalfEdgePossibleTwin(longestHEdge, vertexCoincidentError))
                {
                    return newTriangle;
                }
            }

            // if not found, then is error.!!!
            int hola = 0;
        }


        GaiaTriangle splitableTriangle = null;


        return splitableTriangle;
    }

    public void addMesh(GaiaMesh mesh)
    {
        // 1rst, add vertices.***
        int verticesCount = mesh.vertices.size();
        for(int i=0; i<verticesCount; i++) {
            GaiaVertex vertex = mesh.vertices.get(i);
            vertices.add(vertex);
        }

        // 2nd, add triangles.***
        int trianglesCount = mesh.triangles.size();
        for(int i=0; i<trianglesCount; i++) {
            GaiaTriangle triangle = mesh.triangles.get(i);
            triangles.add(triangle);
        }

        // 3rd, add halfEdges.***
        int halfEdgesCount = mesh.halfEdges.size();
        for(int i=0; i<halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = mesh.halfEdges.get(i);
            halfEdges.add(halfEdge);
        }


    }

    public void saveDataOutputStream(LittleEndianDataOutputStream dataOutputStream) throws IOException {
        // save id.***
        dataOutputStream.writeInt(id);

        // save vertices.***
        int verticesCount = vertices.size();
        // save vertices count.***
        dataOutputStream.writeInt(verticesCount);

        for(int i=0; i<verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            vertex.saveDataOutputStream(dataOutputStream);
        }

        // save triangles.***
        int trianglesCount = triangles.size();
        // save triangles count.***
        dataOutputStream.writeInt(trianglesCount);

        for(int i=0; i<trianglesCount; i++) {
            GaiaTriangle triangle = triangles.get(i);
            triangle.saveDataOutputStream(dataOutputStream);
        }

        // save halfEdges.***
        int halfEdgesCount = halfEdges.size();
        // save halfEdges count.***
        dataOutputStream.writeInt(halfEdgesCount);

        for(int i=0; i<halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = halfEdges.get(i);
            halfEdge.saveDataOutputStream(dataOutputStream);
        }

    }

    public void loadDataInputStream(LittleEndianDataInputStream dataInputStream) throws IOException
    {
        this.id = dataInputStream.readInt();

        // load vertices.***
        int verticesCount = dataInputStream.readInt();
        for(int i=0; i<verticesCount; i++) {
            GaiaVertex vertex = newVertex();
            vertex.loadDataInputStream(dataInputStream);
        }

        // load triangles.***
        int trianglesCount = dataInputStream.readInt();
        for(int i=0; i<trianglesCount; i++) {
            GaiaTriangle triangle = newTriangle();
            triangle.loadDataInputStream(dataInputStream);
        }

        // load halfEdges.***
        int halfEdgesCount = dataInputStream.readInt();
        for(int i=0; i<halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = newHalfEdge();
            halfEdge.loadDataInputStream(dataInputStream);
        }

        // now, for each object, find pointing objects.***
        HashMap<Integer, GaiaVertex> verticesMap = getVerticesMap();
        HashMap<Integer, GaiaTriangle> trianglesMap = getTrianglesMap();
        HashMap<Integer, GaiaHalfEdge> halfEdgesMap = getHalfEdgesMap();

        // now, find pointing objects.***
        for(int i=0; i<verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);

            // vertex points outingHalfEdge.***
            int outingHalfEdgeId = vertex.outingHEdgeId;
            if(outingHalfEdgeId != -1) {
                GaiaHalfEdge outingHalfEdge = halfEdgesMap.get(outingHalfEdgeId);
                vertex.outingHEdge = outingHalfEdge;
            }
        }

        for(int i=0; i<trianglesCount; i++) {
            GaiaTriangle triangle = triangles.get(i);

            // triangle points halfEdge.***
            int halfEdgeId = triangle.halfEdgeId;
            if(halfEdgeId != -1) {
                GaiaHalfEdge halfEdge = halfEdgesMap.get(halfEdgeId);
                triangle.halfEdge = halfEdge;
            }
        }

        for(int i=0; i<halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = halfEdges.get(i);

            // halfEdge points vertex.***
            int vertexId = halfEdge.startVertexId;
            if(vertexId != -1) {
                GaiaVertex vertex = verticesMap.get(vertexId);
                halfEdge.startVertex = vertex;
            }

            // halfEdge points triangle.***
            int triangleId = halfEdge.triangleId;
            if(triangleId != -1) {
                GaiaTriangle triangle = trianglesMap.get(triangleId);
                halfEdge.triangle = triangle;
            }

            // halfEdge points next.***
            int nextId = halfEdge.nextId;
            if(nextId != -1) {
                GaiaHalfEdge next = halfEdgesMap.get(nextId);
                halfEdge.next = next;
            }

            // halfEdge points twin.***
            int twinId = halfEdge.twinId;
            if(twinId != -1) {
                GaiaHalfEdge twin = halfEdgesMap.get(twinId);
                halfEdge.twin = twin;
            }
        }

    }

}
