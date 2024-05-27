package com.gaia3d.basic.structure;

import com.gaia3d.basic.types.HalfEdgeType;
import com.gaia3d.util.io.BigEndianDataInputStream;
import com.gaia3d.util.io.BigEndianDataOutputStream;
import com.gaia3d.wgs84Tiles.TerrainElevationDataManager;
import com.gaia3d.wgs84Tiles.TileIndices;
import com.gaia3d.wgs84Tiles.TilesRange;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@Slf4j
public class GaiaMesh {
    public List<GaiaVertex> vertices = new ArrayList<>();
    public List<GaiaTriangle> triangles = new ArrayList<>();
    public List<GaiaHalfEdge> halfEdges = new ArrayList<>();

    public int id = -1;

    public void deleteObjects() {
        for (GaiaVertex vertex : vertices) {
            vertex.deleteObjects();
        }
        vertices.clear();
        vertices = null;

        for (GaiaTriangle triangle : triangles) {
            triangle.deleteObjects();
        }
        triangles.clear();
        triangles = null;

        for (GaiaHalfEdge halfEdge : halfEdges) {
            halfEdge.deleteObjects();
        }
        halfEdges.clear();
        halfEdges = null;
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

    public void setVertexIdInList() {
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            vertex.setId(i);
        }
    }

    public void removeDeletedObjects() {
        // 1rst, check vertices
        vertices.removeIf(vertex -> vertex.getObjectStatus() == GaiaObjectStatus.DELETED);

        // 2nd, check triangles
        triangles.removeIf(triangle -> triangle.getObjectStatus() == GaiaObjectStatus.DELETED);

        // 3rd, check halfEdges
        halfEdges.removeIf(halfEdge -> halfEdge.getObjectStatus() == GaiaObjectStatus.DELETED);
    }

    public void removeDeletedObjectsOriginal() {
        // 1rst, check vertices
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            if (vertex.getObjectStatus() == GaiaObjectStatus.DELETED) {
                GaiaVertex removedVertex = vertices.remove(i);
                i--;
                verticesCount--;
            }
        }

        // 2nd, check triangles
        int trianglesCount = triangles.size();
        for (int i = 0; i < trianglesCount; i++) {
            GaiaTriangle triangle = triangles.get(i);
            if (triangle.getObjectStatus() == GaiaObjectStatus.DELETED) {
                i--;
                trianglesCount--;
            }
        }

        // 3rd, check halfEdges
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = halfEdges.get(i);
            if (halfEdge.getObjectStatus() == GaiaObjectStatus.DELETED) {
                GaiaHalfEdge removedHEdge = halfEdges.remove(i);
                i--;
                halfEdgesCount--;
            }
        }
    }

    public void mergeMesh(GaiaMesh mesh) {
        // 1rst, add vertices
        int verticesCount = mesh.vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            GaiaVertex vertex = mesh.vertices.get(i);
            if (vertex.getObjectStatus() == GaiaObjectStatus.DELETED) {
                continue;
            }
            vertices.add(vertex);
        }

        // 2nd, add triangles
        int trianglesCount = mesh.triangles.size();
        for (int i = 0; i < trianglesCount; i++) {
            GaiaTriangle triangle = mesh.triangles.get(i);
            if (triangle.getObjectStatus() == GaiaObjectStatus.DELETED) {
                continue;
            }
            triangles.add(triangle);
        }

        // 3rd, add halfEdges
        int halfEdgesCount = mesh.halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = mesh.halfEdges.get(i);
            if (halfEdge.getObjectStatus() == GaiaObjectStatus.DELETED) {
                continue;
            }
            halfEdges.add(halfEdge);
        }
    }

    public List<GaiaHalfEdge> getHalfEdgesByType(HalfEdgeType type) {
        // This function returns the halfEdges that have the type and the twin is null
        List<GaiaHalfEdge> halfEdges = new ArrayList<>();
        for (GaiaHalfEdge halfEdge : this.halfEdges) {
            if (halfEdge.getType() == type && halfEdge.getTwin() == null) {
                halfEdges.add(halfEdge);
            }
        }
        return halfEdges;
    }

    public GaiaBoundingBox getBoundingBox() {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        for (GaiaVertex vertex : vertices) {
            if (vertex.getObjectStatus() == GaiaObjectStatus.DELETED) {
                continue;
            }
            boundingBox.addPoint(vertex.getPosition());
        }
        return boundingBox;
    }

    public List<GaiaVertex> getLeftVerticesSortedUpToDown() {
        List<GaiaHalfEdge> leftHedges = getHalfEdgesByType(HalfEdgeType.LEFT);
        Map<GaiaVertex, GaiaVertex> mapVertices = new HashMap<>();
        for (GaiaHalfEdge halfEdge : leftHedges) {
            mapVertices.put(halfEdge.getStartVertex(), halfEdge.getStartVertex());
            mapVertices.put(halfEdge.getEndVertex(), halfEdge.getEndVertex());
        }
        List<GaiaVertex> vertices = new ArrayList<>(mapVertices.values());

        // sort the vertices
        vertices.sort((GaiaVertex v1, GaiaVertex v2) -> {
            return Double.compare(v2.getPosition().y, v1.getPosition().y);
        });

        return vertices;
    }


    public List<GaiaVertex> getDownVerticesSortedLeftToRight() {
        List<GaiaHalfEdge> downHedges = getHalfEdgesByType(HalfEdgeType.DOWN);
        Map<GaiaVertex, GaiaVertex> mapVertices = new HashMap<>();
        for (GaiaHalfEdge halfEdge : downHedges) {
            mapVertices.put(halfEdge.getStartVertex(), halfEdge.getStartVertex());
            mapVertices.put(halfEdge.getEndVertex(), halfEdge.getEndVertex());
        }

        List<GaiaVertex> vertices = new ArrayList<>(mapVertices.values());

        // sort the vertices
        vertices.sort((GaiaVertex v1, GaiaVertex v2) -> {
            return Double.compare(v1.getPosition().x, v2.getPosition().x);
        });

        return vertices;
    }

    public List<GaiaVertex> getRightVerticesSortedDownToUp() {
        List<GaiaHalfEdge> rightHedges = getHalfEdgesByType(HalfEdgeType.RIGHT);
        HashMap<GaiaVertex, GaiaVertex> mapVertices = new HashMap<>();
        for (GaiaHalfEdge halfEdge : rightHedges) {
            mapVertices.put(halfEdge.getStartVertex(), halfEdge.getStartVertex());
            mapVertices.put(halfEdge.getEndVertex(), halfEdge.getEndVertex());
        }
        List<GaiaVertex> vertices = new ArrayList<>(mapVertices.values());

        vertices.sort((GaiaVertex v1, GaiaVertex v2) -> {
            return Double.compare(v1.getPosition().y, v2.getPosition().y);
        });

        return vertices;
    }

    public List<GaiaVertex> getUpVerticesSortedRightToLeft() {
        List<GaiaHalfEdge> upHedges = getHalfEdgesByType(HalfEdgeType.UP);
        Map<GaiaVertex, GaiaVertex> mapVertices = new HashMap<>();
        int upHedgesCount = upHedges.size();
        for (GaiaHalfEdge halfEdge : upHedges) {
            mapVertices.put(halfEdge.getStartVertex(), halfEdge.getStartVertex());
            mapVertices.put(halfEdge.getEndVertex(), halfEdge.getEndVertex());
        }

        List<GaiaVertex> vertices = new ArrayList<>(mapVertices.values());

        // sort the vertices
        vertices.sort((GaiaVertex v1, GaiaVertex v2) -> {
            return Double.compare(v2.getPosition().x, v1.getPosition().x);
        });

        return vertices;
    }


    public void setTriangleIdInList() {
        int trianglesCount = triangles.size();
        for (int i = 0; i < trianglesCount; i++) {
            GaiaTriangle triangle = triangles.get(i);
            triangle.setId(i);
        }
    }

    public void setHalfEdgeIdInList() {
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = halfEdges.get(i);
            halfEdge.setId(i);
        }
    }

    public void setHalfEdgesStartVertexAsOutingHEdges() {
        // this function is used when the vertices belong to different tiles
        // call this function just before to save the mesh
        int halfEdgesCount = halfEdges.size();
        for (GaiaHalfEdge halfEdge : halfEdges) {
            halfEdge.getStartVertex().setOutingHEdge(halfEdge);
        }
    }

    public void setObjectsIdInList() {
        setVertexIdInList();
        setTriangleIdInList();
        setHalfEdgeIdInList();
    }

    public void getTrianglesByTileIndices(TileIndices tileIndices, List<GaiaTriangle> resultTriangles) {
        // This function returns the triangles that intersect the tile
        // 1rst, get the geographicExtension of the tile

        // 2nd, get the triangles that intersect the geographicExtension
        int trianglesCount = triangles.size();
        for (GaiaTriangle triangle : triangles) {
            if (triangle.getObjectStatus() == GaiaObjectStatus.DELETED) {
                continue;
            }

            if (triangle.getOwnerTileIndices().isCoincident(tileIndices)) {
                resultTriangles.add(triangle);
            }
        }
    }

    public void getVerticesByTriangles(List<GaiaVertex> resultVertices) {
        Map<GaiaVertex, GaiaVertex> mapVertices = new HashMap<>();
        for (GaiaTriangle triangle : triangles) {
            List<GaiaVertex> vertices = triangle.getVertices();
            for (GaiaVertex vertex : vertices) {
                mapVertices.put(vertex, vertex);
            }
        }

        // now make vertices from the hashMap
        resultVertices.addAll(mapVertices.values());
    }

    public Map<Integer, GaiaVertex> getVerticesMap() {
        Map<Integer, GaiaVertex> verticesMap = new HashMap<>();
        for (GaiaVertex vertex : vertices) {
            verticesMap.put(vertex.getId(), vertex);
        }
        return verticesMap;
    }

    public Map<Integer, GaiaTriangle> getTrianglesMap() {
        Map<Integer, GaiaTriangle> trianglesMap = new HashMap<>();
        for (GaiaTriangle triangle : triangles) {
            trianglesMap.put(triangle.getId(), triangle);
        }
        return trianglesMap;
    }

    public Map<Integer, GaiaHalfEdge> getHalfEdgesMap() {
        Map<Integer, GaiaHalfEdge> halfEdgesMap = new HashMap<>();
        for (GaiaHalfEdge halfEdge : halfEdges) {
            halfEdgesMap.put(halfEdge.getId(), halfEdge);
        }
        return halfEdgesMap;
    }

    private void disableTriangle(GaiaTriangle triangle) {
        triangle.setObjectStatus(GaiaObjectStatus.DELETED);
        triangle.halfEdge = null;
    }

    public boolean checkVerticesOutingHEdge() {
        int verticesCount = vertices.size();
        for (GaiaVertex vertex : vertices) {
            if (vertex.getObjectStatus() == GaiaObjectStatus.DELETED) {
                continue;
            }
            GaiaHalfEdge outingHEdge = vertex.getOutingHEdge();
            if (outingHEdge == null) {
                return false;
            }
            if (outingHEdge.getObjectStatus() == GaiaObjectStatus.DELETED) {
                return false;
            }
        }
        return true;
    }

    public boolean checkMesh() {
        for (GaiaTriangle triangle : triangles) {
            if (triangle.getObjectStatus() == GaiaObjectStatus.DELETED) {
                continue;
            }
            GaiaHalfEdge halfEdge = triangle.getLongestHalfEdge();
            if (halfEdge.getObjectStatus() == GaiaObjectStatus.DELETED) {
                return false;
            }
            GaiaHalfEdge twin = halfEdge.getTwin();
            if (twin != null) {
                if (twin.getObjectStatus() == GaiaObjectStatus.DELETED) {
                    return false;
                }
                GaiaTriangle adjacentTriangle = twin.getTriangle();
                if (adjacentTriangle.getObjectStatus() == GaiaObjectStatus.DELETED) {
                    return false;
                }
                if (adjacentTriangle.getSplitDepth() == triangle.getSplitDepth()) {
                    GaiaHalfEdge adjacentTriangleLongestHalfEdge = adjacentTriangle.getLongestHalfEdge();
                    if (adjacentTriangleLongestHalfEdge.getObjectStatus() == GaiaObjectStatus.DELETED) {
                        return false;
                    }
                    if (adjacentTriangleLongestHalfEdge.getTwin() != halfEdge) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void calculateNormals()
    {
        // here we calculate the normals of the triangles and the vertices
        int trianglesCount = triangles.size();
        for (GaiaTriangle triangle : triangles) {
            if (triangle.getObjectStatus() == GaiaObjectStatus.DELETED) {
                continue;
            }
            triangle.calculateNormal();
        }

        // once all triangles have their normals calculated, we can calculate the normals of the vertices
        for (GaiaVertex vertex : vertices) {
            if (vertex.getObjectStatus() == GaiaObjectStatus.DELETED) {
                continue;
            }
            vertex.calculateNormal();
        }
    }


    public void splitTriangle(GaiaTriangle triangle, TerrainElevationDataManager terrainElevationDataManager, List<GaiaTriangle> resultNewTriangles) throws TransformException, IOException {
        // A triangle is split by the longest edge
        // so, the longest edge of the triangle must be the longest edge of the adjacentTriangle
        // If the longest edge of the adjacentTriangle is not the longest edge of the triangle, then must split the adjacentTriangle first
        // If the adjacentTriangle is null, then the triangle is splittable

        GaiaTriangle adjacentTriangle = getSplittableAdjacentTriangle(triangle, terrainElevationDataManager);
        if (adjacentTriangle == null) {
            // the triangle is border triangle, so is splittable
            GaiaHalfEdge longestHEdge = triangle.getLongestHalfEdge();
            GaiaHalfEdge prevHEdge = longestHEdge.getPrev();
            GaiaHalfEdge nextHEdge = longestHEdge.getNext();

            // keep the twin of the longestHEdge, prevHEdge and nextHEdge
            GaiaHalfEdge longestHEdgeTwin = longestHEdge.getTwin();
            GaiaHalfEdge prevHEdgeTwin = prevHEdge.getTwin();
            GaiaHalfEdge nextHEdgeTwin = nextHEdge.getTwin();

            // in this case the twin is null
            Vector3d midPosition = longestHEdge.getMidPosition();

            // now determine the elevation of the midPoint
            double elevation = terrainElevationDataManager.getElevation(midPosition.x, midPosition.y, terrainElevationDataManager.getMemSaveTerrainElevDatasArray());

            midPosition.z = elevation;
            GaiaVertex midVertex = newVertex();
            midVertex.setPosition(midPosition);

            // find the opposite vertex of the longestHEdge
            // In a triangle, the opposite vertex of the longestHEdge is the startVertex of the prevHEdge of the longestHEdge

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

            // split the triangle
            // 1rst, create 2 new triangles

            //                      oppositeVertex
            //                            / \
            //                         /   |   \
            //                      /      |      \
            //                   /         |         \
            //                /    A       |     B      \
            //             /   halfEdgeA1 | halfEdgeB1   \
            //          +------------------+------------------+ <-- longestHEdgeEndVertex
            //          ^            midVertex    ^
            //          |                         |
            //          |                         |
            //          |                         +-- longestHEdge
            //          |
            //          +-- longestHEdgeStartVertex

            GaiaVertex longestHEdgeStartVertex = longestHEdge.getStartVertex();
            GaiaVertex longestHEdgeEndVertex = longestHEdge.getEndVertex();

            // TriangleA
            GaiaHalfEdge halfEdgeA1 = newHalfEdge();
            halfEdgeA1.setType(longestHEdge.getType());
            GaiaHalfEdge halfEdgeA2 = newHalfEdge();
            halfEdgeA2.setType(HalfEdgeType.INTERIOR);
            GaiaHalfEdge halfEdgeA3 = newHalfEdge();
            halfEdgeA3.setType(prevHEdge.getType());

            // set vertex to the new halfEdges
            halfEdgeA1.setStartVertex(longestHEdgeStartVertex);
            halfEdgeA2.setStartVertex(midVertex);
            halfEdgeA3.setStartVertex(oppositeVertex);

            GaiaHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdgeA1, halfEdgeA2, halfEdgeA3);
            GaiaTriangle triangleA = newTriangle();
            triangleA.setHalfEdge(halfEdgeA1);
            triangleA.getOwnerTileIndices().copyFrom(triangle.getOwnerTileIndices());
            triangleA.setSplitDepth(triangle.getSplitDepth() + 1);

            // put the new triangle in the result list
            resultNewTriangles.add(triangleA);

            // TriangleB
            GaiaHalfEdge halfEdgeB1 = newHalfEdge();
            halfEdgeB1.setType(longestHEdge.getType());
            GaiaHalfEdge halfEdgeB2 = newHalfEdge();
            halfEdgeB2.setType(nextHEdge.getType());
            GaiaHalfEdge halfEdgeB3 = newHalfEdge();
            halfEdgeB3.setType(HalfEdgeType.INTERIOR);

            // set vertex to the new halfEdges
            halfEdgeB1.setStartVertex(midVertex);
            halfEdgeB2.setStartVertex(longestHEdgeEndVertex);
            halfEdgeB3.setStartVertex(oppositeVertex);

            GaiaHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdgeB1, halfEdgeB2, halfEdgeB3);
            GaiaTriangle triangleB = newTriangle();
            triangleB.setHalfEdge(halfEdgeB1);
            triangleB.getOwnerTileIndices().copyFrom(triangle.getOwnerTileIndices());
            triangleB.setSplitDepth(triangle.getSplitDepth() + 1);

            // put the new triangle in the result list
            resultNewTriangles.add(triangleB);

            // now, set the twins
            // the halfEdgeA1 and halfEdgeB1 has no twins
            halfEdgeA2.setTwin(halfEdgeB3);
            halfEdgeA3.setTwin(prevHEdgeTwin);
            halfEdgeB2.setTwin(nextHEdgeTwin);

            // now set the triangles of halfEdges
            halfEdgeA1.setTriangle(triangleA);
            halfEdgeA2.setTriangle(triangleA);
            halfEdgeA3.setTriangle(triangleA);

            halfEdgeB1.setTriangle(triangleB);
            halfEdgeB2.setTriangle(triangleB);
            halfEdgeB3.setTriangle(triangleB);


            // now delete the triangle
            disableTriangle(triangle);

            longestHEdge.setObjectStatus(GaiaObjectStatus.DELETED);
            longestHEdge.deleteObjects();
            prevHEdge.setObjectStatus(GaiaObjectStatus.DELETED);
            prevHEdge.deleteObjects();
            nextHEdge.setObjectStatus(GaiaObjectStatus.DELETED);
            nextHEdge.deleteObjects();
        } else {
            // split the 2 triangles
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

            // keep the twin of the longestHEdge, prevHEdge and nextHEdge
            GaiaHalfEdge longestHEdge_twin = longestHEdge.getTwin();
            GaiaHalfEdge prevHEdge_twin = prevHEdge.getTwin();
            GaiaHalfEdge nextHEdge_twin = nextHEdge.getTwin();

            // keep the twin of the longestHEdgeAdjT, prevHEdgeAdjT and nextHEdgeAdjT
            GaiaHalfEdge longestHEdgeAdjT_twin = longestHEdgeAdjT.getTwin();
            GaiaHalfEdge prevHEdgeAdjT_twin = prevHEdgeAdjT.getTwin();
            GaiaHalfEdge nextHEdgeAdjT_twin = nextHEdgeAdjT.getTwin();

            // need know the oppVtx_T and oppVtx_AdjT
            GaiaVertex oppVtx_T = prevHEdge.getStartVertex();
            GaiaVertex oppVtx_AdjT = prevHEdgeAdjT.getStartVertex();

            // need know the midVertex
            Vector3d midPosition = longestHEdge.getMidPosition();

            GaiaVertex midVertex = newVertex();

            // now determine the elevation of the midPoint
            double elevation = terrainElevationDataManager.getElevation(midPosition.x, midPosition.y, terrainElevationDataManager.getMemSaveTerrainElevDatasArray());
            midPosition.z = elevation;

            midVertex.setPosition(midPosition);

            // need longEdge_startVertex and longEdge_endVertex
            GaiaVertex longEdge_startVertex = longestHEdge.getStartVertex();
            GaiaVertex longEdge_endVertex = longestHEdge.getEndVertex();


            // A triangle is split by the longest edge
            //                                        oppVtx_T
            //                                           / \
            //                                        /   |   \
            //                longestEdge_prev---> /      |      \ <-- longestHEdge_next
            //                                  /         |         \
            //                               /    A       |     B      \
            //                            /   halfEdgeA1 | halfEdgeB1   \
            // longestHEdge_strVtx-->  +------------------+------------------+ <-- longestHEdge_endVertex
            //                            \   halfEdge_C1 | halfEdge_D1   /
            //                               \      C     |     D      /
            //                                  \         |         / <-- longestHEdgeAdjT_prev
            //            longestEdgeAdjT_next---> \      |      /
            //                                        \   |   /
            //                                           \ /
            //                                        oppVtx_AdjT

            // split the triangle
            // 1rst, create 4 new triangles
            // triangleA
            GaiaHalfEdge halfEdgeA1 = newHalfEdge();
            halfEdgeA1.setType(longestHEdge.getType());
            GaiaHalfEdge halfEdgeA2 = newHalfEdge();
            halfEdgeA2.setType(HalfEdgeType.INTERIOR);
            GaiaHalfEdge halfEdgeA3 = newHalfEdge();
            halfEdgeA3.setType(prevHEdge.getType());

            // set vertex to the new halfEdges
            halfEdgeA1.setStartVertex(longEdge_startVertex);
            halfEdgeA2.setStartVertex(midVertex);
            halfEdgeA3.setStartVertex(oppVtx_T);

            GaiaHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdgeA1, halfEdgeA2, halfEdgeA3);
            GaiaTriangle triangleA = newTriangle();
            triangleA.setHalfEdge(halfEdgeA1);
            triangleA.getOwnerTileIndices().copyFrom(triangle.getOwnerTileIndices());
            triangleA.setSplitDepth(triangle.getSplitDepth() + 1);

            // put the new triangle in the result list
            resultNewTriangles.add(triangleA);

            // triangleB
            GaiaHalfEdge halfEdgeB1 = newHalfEdge();
            halfEdgeB1.setType(longestHEdge.getType());
            GaiaHalfEdge halfEdgeB2 = newHalfEdge();
            halfEdgeB2.setType(nextHEdge.getType());
            GaiaHalfEdge halfEdgeB3 = newHalfEdge();
            halfEdgeB3.setType(HalfEdgeType.INTERIOR);

            // set vertex to the new halfEdges
            halfEdgeB1.setStartVertex(midVertex);
            halfEdgeB2.setStartVertex(longEdge_endVertex);
            halfEdgeB3.setStartVertex(oppVtx_T);

            GaiaHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdgeB1, halfEdgeB2, halfEdgeB3);
            GaiaTriangle triangleB = newTriangle();
            triangleB.setHalfEdge(halfEdgeB1);
            triangleB.getOwnerTileIndices().copyFrom(triangle.getOwnerTileIndices());
            triangleB.setSplitDepth(triangle.getSplitDepth() + 1);

            // put the new triangle in the result list
            resultNewTriangles.add(triangleB);

            // triangle_C
            GaiaHalfEdge halfEdge_C1 = newHalfEdge();
            halfEdge_C1.setType(longestHEdgeAdjT.getType());
            GaiaHalfEdge halfEdge_C2 = newHalfEdge();
            halfEdge_C2.setType(nextHEdgeAdjT.getType());
            GaiaHalfEdge halfEdge_C3 = newHalfEdge();
            halfEdge_C3.setType(HalfEdgeType.INTERIOR);

            // set vertex to the new halfEdges
            halfEdge_C1.setStartVertex(midVertex);
            halfEdge_C2.setStartVertex(longEdge_startVertex);
            halfEdge_C3.setStartVertex(oppVtx_AdjT);

            GaiaHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdge_C1, halfEdge_C2, halfEdge_C3);
            GaiaTriangle triangleC = newTriangle();
            triangleC.setHalfEdge(halfEdge_C1);
            triangleC.getOwnerTileIndices().copyFrom(adjacentTriangle.getOwnerTileIndices());
            triangleC.setSplitDepth(adjacentTriangle.getSplitDepth() + 1);

            // put the new triangle in the result list
            resultNewTriangles.add(triangleC);

            // triangle_D
            GaiaHalfEdge halfEdge_D1 = newHalfEdge();
            halfEdge_D1.setType(longestHEdgeAdjT.getType());
            GaiaHalfEdge halfEdge_D2 = newHalfEdge();
            halfEdge_D2.setType(HalfEdgeType.INTERIOR);
            GaiaHalfEdge halfEdge_D3 = newHalfEdge();
            halfEdge_D3.setType(prevHEdgeAdjT.getType());

            // set vertex to the new halfEdges
            halfEdge_D1.setStartVertex(longEdge_endVertex);
            halfEdge_D2.setStartVertex(midVertex);
            halfEdge_D3.setStartVertex(oppVtx_AdjT);

            GaiaHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdge_D1, halfEdge_D2, halfEdge_D3);
            GaiaTriangle triangleD = newTriangle();
            triangleD.setHalfEdge(halfEdge_D1);
            triangleD.getOwnerTileIndices().copyFrom(adjacentTriangle.getOwnerTileIndices());
            triangleD.setSplitDepth(adjacentTriangle.getSplitDepth() + 1);

            // put the new triangle in the result list
            resultNewTriangles.add(triangleD);

            // now, set the twins
            // here, all newHEdges has twins
            halfEdgeA1.setTwin(halfEdge_C1);
            halfEdgeA2.setTwin(halfEdgeB3);
            halfEdgeA3.setTwin(prevHEdge_twin);

            halfEdgeB1.setTwin(halfEdge_D1);
            halfEdgeB2.setTwin(nextHEdge_twin);
            //halfEdgeB3.setTwin(halfEdgeA2); // redundant

            //halfEdge_C1.setTwin(halfEdgeA1); // redundant
            halfEdge_C2.setTwin(nextHEdgeAdjT_twin);
            halfEdge_C3.setTwin(halfEdge_D2);

            //halfEdge_D1.setTwin(halfEdgeB1); // redundant
            //halfEdge_D2.setTwin(halfEdge_C3); // redundant
            halfEdge_D3.setTwin(prevHEdgeAdjT_twin);

            // now set the triangles of halfEdges
            halfEdgeA1.setTriangle(triangleA);
            halfEdgeA2.setTriangle(triangleA);
            halfEdgeA3.setTriangle(triangleA);

            halfEdgeB1.setTriangle(triangleB);
            halfEdgeB2.setTriangle(triangleB);
            halfEdgeB3.setTriangle(triangleB);

            halfEdge_C1.setTriangle(triangleC);
            halfEdge_C2.setTriangle(triangleC);
            halfEdge_C3.setTriangle(triangleC);

            halfEdge_D1.setTriangle(triangleD);
            halfEdge_D2.setTriangle(triangleD);
            halfEdge_D3.setTriangle(triangleD);

            // now delete the triangles
            disableTriangle(triangle);
            disableTriangle(adjacentTriangle);

            // disable hedges
            longestHEdge.setObjectStatus(GaiaObjectStatus.DELETED);
            longestHEdge.deleteObjects();
            prevHEdge.setObjectStatus(GaiaObjectStatus.DELETED);
            prevHEdge.deleteObjects();
            nextHEdge.setObjectStatus(GaiaObjectStatus.DELETED);
            nextHEdge.deleteObjects();

            longestHEdgeAdjT.setObjectStatus(GaiaObjectStatus.DELETED);
            longestHEdgeAdjT.deleteObjects();
            prevHEdgeAdjT.setObjectStatus(GaiaObjectStatus.DELETED);
            prevHEdgeAdjT.deleteObjects();
            nextHEdgeAdjT.setObjectStatus(GaiaObjectStatus.DELETED);
            nextHEdgeAdjT.deleteObjects();
        }
    }

    public GaiaTriangle getSplittableAdjacentTriangle(GaiaTriangle targetTriangle, TerrainElevationDataManager terrainElevationDataManager) throws TransformException, IOException {
        // A triangle is split by the longest edge
        // so, the longest edge of the triangle must be the longest edge of the adjacentTriangle
        // If the longest edge of the adjacentTriangle is not the longest edge of the triangle, then must split the adjacentTriangle first
        // If the adjacentTriangle is null, then the triangle is splittable

        GaiaHalfEdge longestHEdge = targetTriangle.getLongestHalfEdge();
        GaiaHalfEdge twin = longestHEdge.getTwin();

        if (twin == null) {
            return null;
        }

        GaiaTriangle adjacentTriangle = twin.getTriangle();

        double vertexCoincidentError = 0.0000000000001; // use the TileWgs84Manager.vertexCoincidentError

        GaiaHalfEdge longestHEdgeOfAdjacentTriangle = adjacentTriangle.getLongestHalfEdge();

        if (longestHEdgeOfAdjacentTriangle.getTwin() == longestHEdge) {
            return adjacentTriangle;
        } else if (longestHEdgeOfAdjacentTriangle.isHalfEdgePossibleTwin(longestHEdge, vertexCoincidentError)) {
            // here is error
        } else {
            // first split the adjacentTriangle;
            terrainElevationDataManager.getMemSaveTrianglesArray().clear();
            splitTriangle(adjacentTriangle, terrainElevationDataManager, terrainElevationDataManager.getMemSaveTrianglesArray());

            // now search the new adjacentTriangle for the targetTriangle

            int newTrianglesCount = terrainElevationDataManager.getMemSaveTrianglesArray().size();
            for (int i = 0; i < newTrianglesCount; i++) {
                GaiaTriangle newTriangle = terrainElevationDataManager.getMemSaveTrianglesArray().get(i);
                GaiaHalfEdge longestHEdgeOfNewTriangle = newTriangle.getLongestHalfEdge();
                if (longestHEdgeOfNewTriangle.isHalfEdgePossibleTwin(longestHEdge, vertexCoincidentError)) {
                    terrainElevationDataManager.getMemSaveTrianglesArray().clear();
                    return newTriangle;
                }
            }
            terrainElevationDataManager.getMemSaveTrianglesArray().clear();
            // if not found, then is error.!!!
        }

        return null;
    }

    public void addMesh(GaiaMesh mesh) {
        // 1rst, add vertices
        int verticesCount = mesh.vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            GaiaVertex vertex = mesh.vertices.get(i);
            vertices.add(vertex);
        }

        // 2nd, add triangles
        int trianglesCount = mesh.triangles.size();
        for (int i = 0; i < trianglesCount; i++) {
            GaiaTriangle triangle = mesh.triangles.get(i);
            triangles.add(triangle);
        }

        // 3rd, add halfEdges
        int halfEdgesCount = mesh.halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = mesh.halfEdges.get(i);
            halfEdges.add(halfEdge);
        }

    }

    public void getTrianglesByTilesRange(TilesRange tilesRange, List<GaiaTriangle> resultTriangles, Map<String, List<GaiaTriangle>> mapTileIndicesTriangles) {
        int trianglesCount = triangles.size();
        for (GaiaTriangle triangle : triangles) {
            if (triangle.getObjectStatus() == GaiaObjectStatus.DELETED) {
                continue;
            }

            // check if exist triangle.ownerTile_tileIndices in the map
            if (tilesRange.intersects(triangle.getOwnerTileIndices())) {
                if (resultTriangles != null) {
                    resultTriangles.add(triangle);
                }

                if (mapTileIndicesTriangles != null) {
                    String tileIndicesKey = triangle.getOwnerTileIndices().getString();
                    List<GaiaTriangle> trianglesList = mapTileIndicesTriangles.computeIfAbsent(tileIndicesKey, k -> new ArrayList<>());
                    trianglesList.add(triangle);

                }
            }
        }
    }

    public void saveDataOutputStream(BigEndianDataOutputStream dataOutputStream) throws IOException {
        this.setObjectsIdInList();
        this.setHalfEdgesStartVertexAsOutingHEdges();// this function is used when the vertices belong to different tiles

        // save id
        dataOutputStream.writeInt(id);

        // save vertices
        int verticesCount = vertices.size();
        // save vertices count
        dataOutputStream.writeInt(verticesCount);
        for (int i = 0; i < verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            vertex.saveDataOutputStream(dataOutputStream);
        }

        // save triangles
        int trianglesCount = triangles.size();
        // save triangles count
        dataOutputStream.writeInt(trianglesCount);

        for (int i = 0; i < trianglesCount; i++) {
            GaiaTriangle triangle = triangles.get(i);
            triangle.saveDataOutputStream(dataOutputStream);
        }

        // save halfEdges
        int halfEdgesCount = halfEdges.size();
        // save halfEdges count
        dataOutputStream.writeInt(halfEdgesCount);

        for (int i = 0; i < halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = halfEdges.get(i);
            halfEdge.saveDataOutputStream(dataOutputStream);
        }
    }

    public boolean checkHalfEdges() {
        boolean isOk = true;
        int halfEdgesCount = halfEdges.size();
        for (GaiaHalfEdge halfEdge : halfEdges) {
            if (halfEdge.getTriangle() == null) {
                isOk = false;
                break;
            }
        }
        return isOk;
    }

    public void loadDataInputStream(BigEndianDataInputStream dataInputStream) throws IOException {
        this.id = dataInputStream.readInt();

        // load vertices
        int verticesCount = dataInputStream.readInt();
        for (int i = 0; i < verticesCount; i++) {
            GaiaVertex vertex = newVertex();
            vertex.loadDataInputStream(dataInputStream);
        }

        // load triangles
        int trianglesCount = dataInputStream.readInt();
        for (int i = 0; i < trianglesCount; i++) {
            GaiaTriangle triangle = newTriangle();
            triangle.loadDataInputStream(dataInputStream);
        }

        // load halfEdges
        int halfEdgesCount = dataInputStream.readInt();
        for (int i = 0; i < halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = newHalfEdge();
            halfEdge.loadDataInputStream(dataInputStream);
        }

        // now, for each object, find pointing objects
        Map<Integer, GaiaVertex> verticesMap = getVerticesMap();
        Map<Integer, GaiaTriangle> trianglesMap = getTrianglesMap();
        Map<Integer, GaiaHalfEdge> halfEdgesMap = getHalfEdgesMap();

        // now, find pointing objects
        for (int i = 0; i < verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            int outingHalfEdgeId = vertex.getOutingHEdgeId();
            if (outingHalfEdgeId != -1) {
                GaiaHalfEdge outingHalfEdge = halfEdgesMap.get(outingHalfEdgeId);
                vertex.setOutingHEdge(outingHalfEdge);
            }
        }

        for (int i = 0; i < trianglesCount; i++) {
            GaiaTriangle triangle = triangles.get(i);

            int halfEdgeId = triangle.getHalfEdgeId();
            if (halfEdgeId != -1) {
                GaiaHalfEdge halfEdge = halfEdgesMap.get(halfEdgeId);
                triangle.setHalfEdge(halfEdge);
            }
        }

        for (int i = 0; i < halfEdgesCount; i++) {
            GaiaHalfEdge halfEdge = halfEdges.get(i);

            // halfEdge points vertex
            int vertexId = halfEdge.getStartVertexId();
            if (vertexId != -1) {
                GaiaVertex vertex = verticesMap.get(vertexId);
                halfEdge.setStartVertex(vertex);
            }

            // halfEdge points triangle
            int triangleId = halfEdge.getTriangleId();
            if (triangleId != -1) {
                GaiaTriangle triangle = trianglesMap.get(triangleId);
                halfEdge.setTriangle(triangle);
            }
            // halfEdge points next
            int nextId = halfEdge.getNextId();
            if (nextId != -1) {
                GaiaHalfEdge next = halfEdgesMap.get(nextId);
                halfEdge.setNext(next);
            }

            // halfEdge points twin
            // twin can be null
            int twinId = halfEdge.getTwinId();
            if (twinId != -1) {
                GaiaHalfEdge twin = halfEdgesMap.get(twinId);
                halfEdge.setTwin(twin);
            }
        }

    }

}
