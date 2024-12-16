package com.gaia3d.basic.structure;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.types.TerrainHalfEdgeType;
import com.gaia3d.basic.types.TerrainObjectStatus;
import com.gaia3d.io.BigEndianDataInputStream;
import com.gaia3d.io.BigEndianDataOutputStream;
import com.gaia3d.tile.TerrainElevationDataManager;
import com.gaia3d.tile.TileIndices;
import com.gaia3d.tile.TilesRange;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@NoArgsConstructor
public class TerrainMesh {
    public List<TerrainVertex> vertices = new ArrayList<>();
    public List<TerrainTriangle> triangles = new ArrayList<>();
    public List<TerrainHalfEdge> halfEdges = new ArrayList<>();

    public int id = -1;

    public void deleteObjects() {
        for (TerrainVertex vertex : vertices) {
            vertex.deleteObjects();
        }
        vertices.clear();
        vertices = null;

        for (TerrainTriangle triangle : triangles) {
            triangle.deleteObjects();
        }
        triangles.clear();
        triangles = null;

        for (TerrainHalfEdge halfEdge : halfEdges) {
            halfEdge.deleteObjects();
        }
        halfEdges.clear();
        halfEdges = null;
    }

    public TerrainVertex newVertex() {
        TerrainVertex vertex = new TerrainVertex();
        vertices.add(vertex);
        return vertex;
    }

    public TerrainTriangle newTriangle() {
        TerrainTriangle triangle = new TerrainTriangle();
        triangles.add(triangle);
        return triangle;
    }

    public TerrainHalfEdge newHalfEdge() {
        TerrainHalfEdge halfEdge = new TerrainHalfEdge();
        halfEdges.add(halfEdge);
        return halfEdge;
    }

    public void setVertexIdInList() {
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            TerrainVertex vertex = vertices.get(i);
            vertex.setId(i);
        }
    }

    public void removeDeletedObjects() {
        // 1rst, check vertices
        vertices.removeIf(vertex -> vertex.getObjectStatus() == TerrainObjectStatus.DELETED);

        // 2nd, check triangles
        triangles.removeIf(triangle -> triangle.getObjectStatus() == TerrainObjectStatus.DELETED);

        // 3rd, check halfEdges
        halfEdges.removeIf(halfEdge -> halfEdge.getObjectStatus() == TerrainObjectStatus.DELETED);
    }

    public void removeDeletedObjectsOriginal() {
        // 1rst, check vertices
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            TerrainVertex vertex = vertices.get(i);
            if (vertex.getObjectStatus() == TerrainObjectStatus.DELETED) {
                TerrainVertex removedVertex = vertices.remove(i);
                i--;
                verticesCount--;
            }
        }

        // 2nd, check triangles
        int trianglesCount = triangles.size();
        for (int i = 0; i < trianglesCount; i++) {
            TerrainTriangle triangle = triangles.get(i);
            if (triangle.getObjectStatus() == TerrainObjectStatus.DELETED) {
                i--;
                trianglesCount--;
            }
        }

        // 3rd, check halfEdges
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            TerrainHalfEdge halfEdge = halfEdges.get(i);
            if (halfEdge.getObjectStatus() == TerrainObjectStatus.DELETED) {
                TerrainHalfEdge removedHEdge = halfEdges.remove(i);
                i--;
                halfEdgesCount--;
            }
        }
    }

    public void mergeMesh(TerrainMesh mesh) {
        // 1rst, add vertices
        int verticesCount = mesh.vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            TerrainVertex vertex = mesh.vertices.get(i);
            if (vertex.getObjectStatus() == TerrainObjectStatus.DELETED) {
                continue;
            }
            vertices.add(vertex);
        }

        // 2nd, add triangles
        int trianglesCount = mesh.triangles.size();
        for (int i = 0; i < trianglesCount; i++) {
            TerrainTriangle triangle = mesh.triangles.get(i);
            if (triangle.getObjectStatus() == TerrainObjectStatus.DELETED) {
                continue;
            }
            triangles.add(triangle);
        }

        // 3rd, add halfEdges
        int halfEdgesCount = mesh.halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            TerrainHalfEdge halfEdge = mesh.halfEdges.get(i);
            if (halfEdge.getObjectStatus() == TerrainObjectStatus.DELETED) {
                continue;
            }
            halfEdges.add(halfEdge);
        }
    }

    public List<TerrainHalfEdge> getHalfEdgesByType(TerrainHalfEdgeType type) {
        // This function returns the halfEdges that have the type and the twin is null
        List<TerrainHalfEdge> halfEdges = new ArrayList<>();
        for (TerrainHalfEdge halfEdge : this.halfEdges) {
            if (halfEdge.getType() == type && halfEdge.getTwin() == null) {
                halfEdges.add(halfEdge);
            }
        }
        return halfEdges;
    }

    public GaiaBoundingBox getBoundingBox() {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        for (TerrainVertex vertex : vertices) {
            if (vertex.getObjectStatus() == TerrainObjectStatus.DELETED) {
                continue;
            }
            boundingBox.addPoint(vertex.getPosition());
        }
        return boundingBox;
    }

    public List<TerrainVertex> getLeftVerticesSortedUpToDown() {
        List<TerrainHalfEdge> leftHedges = getHalfEdgesByType(TerrainHalfEdgeType.LEFT);
        Map<TerrainVertex, TerrainVertex> mapVertices = new HashMap<>();
        for (TerrainHalfEdge halfEdge : leftHedges) {
            mapVertices.put(halfEdge.getStartVertex(), halfEdge.getStartVertex());
            mapVertices.put(halfEdge.getEndVertex(), halfEdge.getEndVertex());
        }
        List<TerrainVertex> vertices = new ArrayList<>(mapVertices.values());

        // sort the vertices
        vertices.sort((TerrainVertex v1, TerrainVertex v2) -> {
            return Double.compare(v2.getPosition().y, v1.getPosition().y);
        });

        return vertices;
    }


    public List<TerrainVertex> getDownVerticesSortedLeftToRight() {
        List<TerrainHalfEdge> downHedges = getHalfEdgesByType(TerrainHalfEdgeType.DOWN);
        Map<TerrainVertex, TerrainVertex> mapVertices = new HashMap<>();
        for (TerrainHalfEdge halfEdge : downHedges) {
            mapVertices.put(halfEdge.getStartVertex(), halfEdge.getStartVertex());
            mapVertices.put(halfEdge.getEndVertex(), halfEdge.getEndVertex());
        }

        List<TerrainVertex> vertices = new ArrayList<>(mapVertices.values());

        // sort the vertices
        vertices.sort((TerrainVertex v1, TerrainVertex v2) -> {
            return Double.compare(v1.getPosition().x, v2.getPosition().x);
        });

        return vertices;
    }

    public List<TerrainVertex> getRightVerticesSortedDownToUp() {
        List<TerrainHalfEdge> rightHedges = getHalfEdgesByType(TerrainHalfEdgeType.RIGHT);
        HashMap<TerrainVertex, TerrainVertex> mapVertices = new HashMap<>();
        for (TerrainHalfEdge halfEdge : rightHedges) {
            mapVertices.put(halfEdge.getStartVertex(), halfEdge.getStartVertex());
            mapVertices.put(halfEdge.getEndVertex(), halfEdge.getEndVertex());
        }
        List<TerrainVertex> vertices = new ArrayList<>(mapVertices.values());

        vertices.sort((TerrainVertex v1, TerrainVertex v2) -> {
            return Double.compare(v1.getPosition().y, v2.getPosition().y);
        });

        return vertices;
    }

    public List<TerrainVertex> getUpVerticesSortedRightToLeft() {
        List<TerrainHalfEdge> upHedges = getHalfEdgesByType(TerrainHalfEdgeType.UP);
        Map<TerrainVertex, TerrainVertex> mapVertices = new HashMap<>();
        int upHedgesCount = upHedges.size();
        for (TerrainHalfEdge halfEdge : upHedges) {
            mapVertices.put(halfEdge.getStartVertex(), halfEdge.getStartVertex());
            mapVertices.put(halfEdge.getEndVertex(), halfEdge.getEndVertex());
        }

        List<TerrainVertex> vertices = new ArrayList<>(mapVertices.values());

        // sort the vertices
        vertices.sort((TerrainVertex v1, TerrainVertex v2) -> {
            return Double.compare(v2.getPosition().x, v1.getPosition().x);
        });

        return vertices;
    }


    public void setTriangleIdInList() {
        int trianglesCount = triangles.size();
        for (int i = 0; i < trianglesCount; i++) {
            TerrainTriangle triangle = triangles.get(i);
            triangle.setId(i);
        }
    }

    public void setHalfEdgeIdInList() {
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            TerrainHalfEdge halfEdge = halfEdges.get(i);
            halfEdge.setId(i);
        }
    }

    public void setHalfEdgesStartVertexAsOutingHEdges() {
        // this function is used when the vertices belong to different tiles
        // call this function just before to save the mesh
        int halfEdgesCount = halfEdges.size();
        for (TerrainHalfEdge halfEdge : halfEdges) {
            halfEdge.getStartVertex().setOutingHEdge(halfEdge);
        }
    }

    public void setObjectsIdInList() {
        setVertexIdInList();
        setTriangleIdInList();
        setHalfEdgeIdInList();
    }

    public void getTrianglesByTileIndices(TileIndices tileIndices, List<TerrainTriangle> resultTriangles) {
        // This function returns the triangles that intersect the tile
        // 1rst, get the geographicExtension of the tile

        // 2nd, get the triangles that intersect the geographicExtension
        int trianglesCount = triangles.size();
        for (TerrainTriangle triangle : triangles) {
            if (triangle.getObjectStatus() == TerrainObjectStatus.DELETED) {
                continue;
            }

            if (triangle.getOwnerTileIndices().isCoincident(tileIndices)) {
                resultTriangles.add(triangle);
            }
        }
    }

    public void getVerticesByTriangles(List<TerrainVertex> resultVertices, List<TerrainVertex> listVertices, List<TerrainHalfEdge> listHalfEdges) {
        Map<TerrainVertex, TerrainVertex> mapVertices = new HashMap<>();
        for (TerrainTriangle triangle : triangles) {
            listHalfEdges.clear();
            listVertices.clear();
            List<TerrainVertex> vertices = triangle.getVertices(listVertices, listHalfEdges);
            for (TerrainVertex vertex : vertices) {
                mapVertices.put(vertex, vertex);
            }
        }

        // now make vertices from the hashMap
        resultVertices.addAll(mapVertices.values());
    }

    public Map<Integer, TerrainVertex> getVerticesMap() {
        Map<Integer, TerrainVertex> verticesMap = new HashMap<>();
        for (TerrainVertex vertex : vertices) {
            verticesMap.put(vertex.getId(), vertex);
        }
        return verticesMap;
    }

    public Map<Integer, TerrainTriangle> getTrianglesMap() {
        Map<Integer, TerrainTriangle> trianglesMap = new HashMap<>();
        for (TerrainTriangle triangle : triangles) {
            trianglesMap.put(triangle.getId(), triangle);
        }
        return trianglesMap;
    }

    public Map<Integer, TerrainHalfEdge> getHalfEdgesMap() {
        Map<Integer, TerrainHalfEdge> halfEdgesMap = new HashMap<>();
        for (TerrainHalfEdge halfEdge : halfEdges) {
            halfEdgesMap.put(halfEdge.getId(), halfEdge);
        }
        return halfEdgesMap;
    }

    private void disableTriangle(TerrainTriangle triangle) {
        triangle.setObjectStatus(TerrainObjectStatus.DELETED);
        triangle.halfEdge = null;
    }

    public boolean checkVerticesOutingHEdge() {
        int verticesCount = vertices.size();
        for (TerrainVertex vertex : vertices) {
            if (vertex.getObjectStatus() == TerrainObjectStatus.DELETED) {
                continue;
            }
            TerrainHalfEdge outingHEdge = vertex.getOutingHEdge();
            if (outingHEdge == null) {
                return false;
            }
            if (outingHEdge.getObjectStatus() == TerrainObjectStatus.DELETED) {
                return false;
            }
        }
        return true;
    }

    public boolean checkMesh(List<TerrainHalfEdge> listHalfEdges) {
        for (TerrainTriangle triangle : triangles) {
            if (triangle.getObjectStatus() == TerrainObjectStatus.DELETED) {
                continue;
            }
            listHalfEdges.clear();
            TerrainHalfEdge halfEdge = triangle.getLongestHalfEdge(listHalfEdges);
            if (halfEdge.getObjectStatus() == TerrainObjectStatus.DELETED) {
                return false;
            }
            TerrainHalfEdge twin = halfEdge.getTwin();
            if (twin != null) {
                if (twin.getObjectStatus() == TerrainObjectStatus.DELETED) {
                    return false;
                }
                TerrainTriangle adjacentTriangle = twin.getTriangle();
                if (adjacentTriangle.getObjectStatus() == TerrainObjectStatus.DELETED) {
                    return false;
                }
                if (adjacentTriangle.getSplitDepth() == triangle.getSplitDepth()) {
                    listHalfEdges.clear();
                    TerrainHalfEdge adjacentTriangleLongestHalfEdge = adjacentTriangle.getLongestHalfEdge(listHalfEdges);
                    listHalfEdges.clear();
                    if (adjacentTriangleLongestHalfEdge.getObjectStatus() == TerrainObjectStatus.DELETED) {
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

    public void calculateNormals(List<TerrainVertex> listVertices, List<TerrainHalfEdge> listHalfEdges) {
        // here we calculate the normals of the triangles and the vertices
        int trianglesCount = triangles.size();
        for (TerrainTriangle triangle : triangles) {
            if (triangle.getObjectStatus() == TerrainObjectStatus.DELETED) {
                continue;
            }
            listVertices.clear();
            listHalfEdges.clear();
            triangle.calculateNormal(listVertices, listHalfEdges);
        }

        // once all triangles have their normals calculated, we can calculate the normals of the vertices
        for (TerrainVertex vertex : vertices) {
            if (vertex.getObjectStatus() == TerrainObjectStatus.DELETED) {
                continue;
            }
            vertex.calculateNormal();
        }
    }


    public void splitTriangle(TerrainTriangle triangle, TerrainElevationDataManager terrainElevationDataManager, List<TerrainTriangle> resultNewTriangles, List<TerrainHalfEdge> listHalfEdges) throws TransformException, IOException {
        // A triangle is split by the longest edge
        // so, the longest edge of the triangle must be the longest edge of the adjacentTriangle
        // If the longest edge of the adjacentTriangle is not the longest edge of the triangle, then must split the adjacentTriangle first
        // If the adjacentTriangle is null, then the triangle is splittable

        listHalfEdges.clear();
        TerrainTriangle adjacentTriangle = getSplittableAdjacentTriangle(triangle, terrainElevationDataManager, listHalfEdges);
        if (adjacentTriangle == null) {
            // the triangle is border triangle, so is splittable
            listHalfEdges.clear();
            TerrainHalfEdge longestHEdge = triangle.getLongestHalfEdge(listHalfEdges);
            TerrainHalfEdge prevHEdge = longestHEdge.getPrev();
            TerrainHalfEdge nextHEdge = longestHEdge.getNext();

            // keep the twin of the longestHEdge, prevHEdge and nextHEdge
            TerrainHalfEdge longestHEdgeTwin = longestHEdge.getTwin();
            TerrainHalfEdge prevHEdgeTwin = prevHEdge.getTwin();
            TerrainHalfEdge nextHEdgeTwin = nextHEdge.getTwin();

            // in this case the twin is null
            Vector3d midPosition = longestHEdge.getMidPosition();

            // now determine the elevation of the midPoint
            TileIndices tileIndices = triangle.getOwnerTileIndices();

            midPosition.z = terrainElevationDataManager.getElevationBilinearRasterTile(tileIndices, terrainElevationDataManager.getTileWgs84Manager(), midPosition.x, midPosition.y);
            TerrainVertex midVertex = newVertex();
            midVertex.setPosition(midPosition);

            // find the opposite vertex of the longestHEdge
            // In a triangle, the opposite vertex of the longestHEdge is the startVertex of the prevHEdge of the longestHEdge

            TerrainVertex oppositeVertex = prevHEdge.getStartVertex();

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

            TerrainVertex longestHEdgeStartVertex = longestHEdge.getStartVertex();
            TerrainVertex longestHEdgeEndVertex = longestHEdge.getEndVertex();

            // TriangleA
            TerrainHalfEdge halfEdgeA1 = newHalfEdge();
            halfEdgeA1.setType(longestHEdge.getType());
            TerrainHalfEdge halfEdgeA2 = newHalfEdge();
            halfEdgeA2.setType(TerrainHalfEdgeType.INTERIOR);
            TerrainHalfEdge halfEdgeA3 = newHalfEdge();
            halfEdgeA3.setType(prevHEdge.getType());

            // set vertex to the new halfEdges
            halfEdgeA1.setStartVertex(longestHEdgeStartVertex);
            halfEdgeA2.setStartVertex(midVertex);
            halfEdgeA3.setStartVertex(oppositeVertex);

            TerrainHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdgeA1, halfEdgeA2, halfEdgeA3);
            TerrainTriangle triangleA = newTriangle();
            triangleA.setHalfEdge(halfEdgeA1);
            triangleA.getOwnerTileIndices().copyFrom(triangle.getOwnerTileIndices());
            triangleA.setSplitDepth(triangle.getSplitDepth() + 1);

            // put the new triangle in the result list
            resultNewTriangles.add(triangleA);

            // TriangleB
            TerrainHalfEdge halfEdgeB1 = newHalfEdge();
            halfEdgeB1.setType(longestHEdge.getType());
            TerrainHalfEdge halfEdgeB2 = newHalfEdge();
            halfEdgeB2.setType(nextHEdge.getType());
            TerrainHalfEdge halfEdgeB3 = newHalfEdge();
            halfEdgeB3.setType(TerrainHalfEdgeType.INTERIOR);

            // set vertex to the new halfEdges
            halfEdgeB1.setStartVertex(midVertex);
            halfEdgeB2.setStartVertex(longestHEdgeEndVertex);
            halfEdgeB3.setStartVertex(oppositeVertex);

            TerrainHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdgeB1, halfEdgeB2, halfEdgeB3);
            TerrainTriangle triangleB = newTriangle();
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

            longestHEdge.setObjectStatus(TerrainObjectStatus.DELETED);
            longestHEdge.deleteObjects();
            prevHEdge.setObjectStatus(TerrainObjectStatus.DELETED);
            prevHEdge.deleteObjects();
            nextHEdge.setObjectStatus(TerrainObjectStatus.DELETED);
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

            listHalfEdges.clear();
            TerrainHalfEdge longestHEdge = triangle.getLongestHalfEdge(listHalfEdges);
            TerrainHalfEdge prevHEdge = longestHEdge.getPrev();
            TerrainHalfEdge nextHEdge = longestHEdge.getNext();

            listHalfEdges.clear();
            TerrainHalfEdge longestHEdgeAdjT = adjacentTriangle.getLongestHalfEdge(listHalfEdges);
            TerrainHalfEdge prevHEdgeAdjT = longestHEdgeAdjT.getPrev();
            TerrainHalfEdge nextHEdgeAdjT = longestHEdgeAdjT.getNext();

            // keep the twin of the longestHEdge, prevHEdge and nextHEdge
            TerrainHalfEdge longestHEdge_twin = longestHEdge.getTwin();
            TerrainHalfEdge prevHEdge_twin = prevHEdge.getTwin();
            TerrainHalfEdge nextHEdge_twin = nextHEdge.getTwin();

            // keep the twin of the longestHEdgeAdjT, prevHEdgeAdjT and nextHEdgeAdjT
            TerrainHalfEdge longestHEdgeAdjT_twin = longestHEdgeAdjT.getTwin();
            TerrainHalfEdge prevHEdgeAdjT_twin = prevHEdgeAdjT.getTwin();
            TerrainHalfEdge nextHEdgeAdjT_twin = nextHEdgeAdjT.getTwin();

            // need know the oppVtx_T and oppVtx_AdjT
            TerrainVertex oppVtx_T = prevHEdge.getStartVertex();
            TerrainVertex oppVtx_AdjT = prevHEdgeAdjT.getStartVertex();

            // need know the midVertex
            Vector3d midPosition = longestHEdge.getMidPosition();

            TerrainVertex midVertex = newVertex();

            // now determine the elevation of the midPoint
            TileIndices tileIndices = triangle.getOwnerTileIndices();
            midPosition.z = terrainElevationDataManager.getElevationBilinearRasterTile(tileIndices, terrainElevationDataManager.getTileWgs84Manager(), midPosition.x, midPosition.y);

            midVertex.setPosition(midPosition);

            // need longEdge_startVertex and longEdge_endVertex
            TerrainVertex longEdge_startVertex = longestHEdge.getStartVertex();
            TerrainVertex longEdge_endVertex = longestHEdge.getEndVertex();


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
            TerrainHalfEdge halfEdgeA1 = newHalfEdge();
            halfEdgeA1.setType(longestHEdge.getType());
            TerrainHalfEdge halfEdgeA2 = newHalfEdge();
            halfEdgeA2.setType(TerrainHalfEdgeType.INTERIOR);
            TerrainHalfEdge halfEdgeA3 = newHalfEdge();
            halfEdgeA3.setType(prevHEdge.getType());

            // set vertex to the new halfEdges
            halfEdgeA1.setStartVertex(longEdge_startVertex);
            halfEdgeA2.setStartVertex(midVertex);
            halfEdgeA3.setStartVertex(oppVtx_T);

            TerrainHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdgeA1, halfEdgeA2, halfEdgeA3);
            TerrainTriangle triangleA = newTriangle();
            triangleA.setHalfEdge(halfEdgeA1);
            triangleA.getOwnerTileIndices().copyFrom(triangle.getOwnerTileIndices());
            triangleA.setSplitDepth(triangle.getSplitDepth() + 1);

            // put the new triangle in the result list
            resultNewTriangles.add(triangleA);

            // triangleB
            TerrainHalfEdge halfEdgeB1 = newHalfEdge();
            halfEdgeB1.setType(longestHEdge.getType());
            TerrainHalfEdge halfEdgeB2 = newHalfEdge();
            halfEdgeB2.setType(nextHEdge.getType());
            TerrainHalfEdge halfEdgeB3 = newHalfEdge();
            halfEdgeB3.setType(TerrainHalfEdgeType.INTERIOR);

            // set vertex to the new halfEdges
            halfEdgeB1.setStartVertex(midVertex);
            halfEdgeB2.setStartVertex(longEdge_endVertex);
            halfEdgeB3.setStartVertex(oppVtx_T);

            TerrainHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdgeB1, halfEdgeB2, halfEdgeB3);
            TerrainTriangle triangleB = newTriangle();
            triangleB.setHalfEdge(halfEdgeB1);
            triangleB.getOwnerTileIndices().copyFrom(triangle.getOwnerTileIndices());
            triangleB.setSplitDepth(triangle.getSplitDepth() + 1);

            // put the new triangle in the result list
            resultNewTriangles.add(triangleB);

            // triangle_C
            TerrainHalfEdge halfEdge_C1 = newHalfEdge();
            halfEdge_C1.setType(longestHEdgeAdjT.getType());
            TerrainHalfEdge halfEdge_C2 = newHalfEdge();
            halfEdge_C2.setType(nextHEdgeAdjT.getType());
            TerrainHalfEdge halfEdge_C3 = newHalfEdge();
            halfEdge_C3.setType(TerrainHalfEdgeType.INTERIOR);

            // set vertex to the new halfEdges
            halfEdge_C1.setStartVertex(midVertex);
            halfEdge_C2.setStartVertex(longEdge_startVertex);
            halfEdge_C3.setStartVertex(oppVtx_AdjT);

            TerrainHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdge_C1, halfEdge_C2, halfEdge_C3);
            TerrainTriangle triangleC = newTriangle();
            triangleC.setHalfEdge(halfEdge_C1);
            triangleC.getOwnerTileIndices().copyFrom(adjacentTriangle.getOwnerTileIndices());
            triangleC.setSplitDepth(adjacentTriangle.getSplitDepth() + 1);

            // put the new triangle in the result list
            resultNewTriangles.add(triangleC);

            // triangle_D
            TerrainHalfEdge halfEdge_D1 = newHalfEdge();
            halfEdge_D1.setType(longestHEdgeAdjT.getType());
            TerrainHalfEdge halfEdge_D2 = newHalfEdge();
            halfEdge_D2.setType(TerrainHalfEdgeType.INTERIOR);
            TerrainHalfEdge halfEdge_D3 = newHalfEdge();
            halfEdge_D3.setType(prevHEdgeAdjT.getType());

            // set vertex to the new halfEdges
            halfEdge_D1.setStartVertex(longEdge_endVertex);
            halfEdge_D2.setStartVertex(midVertex);
            halfEdge_D3.setStartVertex(oppVtx_AdjT);

            TerrainHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdge_D1, halfEdge_D2, halfEdge_D3);
            TerrainTriangle triangleD = newTriangle();
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
            longestHEdge.setObjectStatus(TerrainObjectStatus.DELETED);
            longestHEdge.deleteObjects();
            prevHEdge.setObjectStatus(TerrainObjectStatus.DELETED);
            prevHEdge.deleteObjects();
            nextHEdge.setObjectStatus(TerrainObjectStatus.DELETED);
            nextHEdge.deleteObjects();

            longestHEdgeAdjT.setObjectStatus(TerrainObjectStatus.DELETED);
            longestHEdgeAdjT.deleteObjects();
            prevHEdgeAdjT.setObjectStatus(TerrainObjectStatus.DELETED);
            prevHEdgeAdjT.deleteObjects();
            nextHEdgeAdjT.setObjectStatus(TerrainObjectStatus.DELETED);
            nextHEdgeAdjT.deleteObjects();
        }
    }

    public TerrainTriangle getSplittableAdjacentTriangle(TerrainTriangle targetTriangle, TerrainElevationDataManager terrainElevationDataManager, List<TerrainHalfEdge> listHalfEdges) throws TransformException, IOException {
        // A triangle is split by the longest edge
        // so, the longest edge of the triangle must be the longest edge of the adjacentTriangle
        // If the longest edge of the adjacentTriangle is not the longest edge of the triangle, then must split the adjacentTriangle first
        // If the adjacentTriangle is null, then the triangle is splittable

        listHalfEdges.clear();
        TerrainHalfEdge longestHEdge = targetTriangle.getLongestHalfEdge(listHalfEdges);
        TerrainHalfEdge twin = longestHEdge.getTwin();

        if (twin == null) {
            return null;
        }

        TerrainTriangle adjacentTriangle = twin.getTriangle();

        double vertexCoincidentError = 0.0000000000001; // use the TileWgs84Manager.vertexCoincidentError

        listHalfEdges.clear();
        TerrainHalfEdge longestHEdgeOfAdjacentTriangle = adjacentTriangle.getLongestHalfEdge(listHalfEdges);

        if (longestHEdgeOfAdjacentTriangle.getTwin() == longestHEdge) {
            return adjacentTriangle;
        } else if (longestHEdgeOfAdjacentTriangle.isHalfEdgePossibleTwin(longestHEdge, vertexCoincidentError)) {
            // here is error
        } else {
            // first split the adjacentTriangle;
            terrainElevationDataManager.getTrianglesArray().clear();
            listHalfEdges.clear();
            splitTriangle(adjacentTriangle, terrainElevationDataManager, terrainElevationDataManager.getTrianglesArray(), listHalfEdges);
            listHalfEdges.clear();

            // now search the new adjacentTriangle for the targetTriangle

            int newTrianglesCount = terrainElevationDataManager.getTrianglesArray().size();
            for (int i = 0; i < newTrianglesCount; i++) {
                TerrainTriangle newTriangle = terrainElevationDataManager.getTrianglesArray().get(i);
                listHalfEdges.clear();
                TerrainHalfEdge longestHEdgeOfNewTriangle = newTriangle.getLongestHalfEdge(listHalfEdges);
                if (longestHEdgeOfNewTriangle.isHalfEdgePossibleTwin(longestHEdge, vertexCoincidentError)) {
                    terrainElevationDataManager.getTrianglesArray().clear();
                    return newTriangle;
                }
            }
            terrainElevationDataManager.getTrianglesArray().clear();
            // if not found, then is error.!!!
        }

        listHalfEdges.clear();

        return null;
    }

    public void addMesh(TerrainMesh mesh) {
        // 1rst, add vertices
        int verticesCount = mesh.vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            TerrainVertex vertex = mesh.vertices.get(i);
            vertices.add(vertex);
        }

        // 2nd, add triangles
        int trianglesCount = mesh.triangles.size();
        for (int i = 0; i < trianglesCount; i++) {
            TerrainTriangle triangle = mesh.triangles.get(i);
            triangles.add(triangle);
        }

        // 3rd, add halfEdges
        int halfEdgesCount = mesh.halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            TerrainHalfEdge halfEdge = mesh.halfEdges.get(i);
            halfEdges.add(halfEdge);
        }

    }

    public void getTrianglesByTilesRange(TilesRange tilesRange, List<TerrainTriangle> resultTriangles, Map<String, List<TerrainTriangle>> mapTileIndicesTriangles) {
        int trianglesCount = triangles.size();
        for (TerrainTriangle triangle : triangles) {
            if (triangle.getObjectStatus() == TerrainObjectStatus.DELETED) {
                continue;
            }

            // check if exist triangle.ownerTile_tileIndices in the map
            if (tilesRange.intersects(triangle.getOwnerTileIndices())) {
                if (resultTriangles != null) {
                    resultTriangles.add(triangle);
                }

                if (mapTileIndicesTriangles != null) {
                    String tileIndicesKey = triangle.getOwnerTileIndices().getString();
                    List<TerrainTriangle> trianglesList = mapTileIndicesTriangles.computeIfAbsent(tileIndicesKey, k -> new ArrayList<>());
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
            TerrainVertex vertex = vertices.get(i);
            vertex.saveDataOutputStream(dataOutputStream);
        }

        // save triangles
        int trianglesCount = triangles.size();
        // save triangles count
        dataOutputStream.writeInt(trianglesCount);

        for (int i = 0; i < trianglesCount; i++) {
            TerrainTriangle triangle = triangles.get(i);
            triangle.saveDataOutputStream(dataOutputStream);
        }

        // save halfEdges
        int halfEdgesCount = halfEdges.size();
        // save halfEdges count
        dataOutputStream.writeInt(halfEdgesCount);

        for (int i = 0; i < halfEdgesCount; i++) {
            TerrainHalfEdge halfEdge = halfEdges.get(i);
            halfEdge.saveDataOutputStream(dataOutputStream);
        }
    }

    public boolean checkHalfEdges() {
        boolean isOk = true;
        int halfEdgesCount = halfEdges.size();
        for (TerrainHalfEdge halfEdge : halfEdges) {
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
            TerrainVertex vertex = newVertex();
            vertex.loadDataInputStream(dataInputStream);
        }

        // load triangles
        int trianglesCount = dataInputStream.readInt();
        for (int i = 0; i < trianglesCount; i++) {
            TerrainTriangle triangle = newTriangle();
            triangle.loadDataInputStream(dataInputStream);
        }

        // load halfEdges
        int halfEdgesCount = dataInputStream.readInt();
        for (int i = 0; i < halfEdgesCount; i++) {
            TerrainHalfEdge halfEdge = newHalfEdge();
            halfEdge.loadDataInputStream(dataInputStream);
        }

        // now, for each object, find pointing objects
        Map<Integer, TerrainVertex> verticesMap = getVerticesMap();
        Map<Integer, TerrainTriangle> trianglesMap = getTrianglesMap();
        Map<Integer, TerrainHalfEdge> halfEdgesMap = getHalfEdgesMap();

        // now, find pointing objects
        for (int i = 0; i < verticesCount; i++) {
            TerrainVertex vertex = vertices.get(i);
            int outingHalfEdgeId = vertex.getOutingHEdgeId();
            if (outingHalfEdgeId != -1) {
                TerrainHalfEdge outingHalfEdge = halfEdgesMap.get(outingHalfEdgeId);
                vertex.setOutingHEdge(outingHalfEdge);
            }
        }

        for (int i = 0; i < trianglesCount; i++) {
            TerrainTriangle triangle = triangles.get(i);

            int halfEdgeId = triangle.getHalfEdgeId();
            if (halfEdgeId != -1) {
                TerrainHalfEdge halfEdge = halfEdgesMap.get(halfEdgeId);
                triangle.setHalfEdge(halfEdge);
            }
        }

        for (int i = 0; i < halfEdgesCount; i++) {
            TerrainHalfEdge halfEdge = halfEdges.get(i);

            // halfEdge points vertex
            int vertexId = halfEdge.getStartVertexId();
            if (vertexId != -1) {
                TerrainVertex vertex = verticesMap.get(vertexId);
                halfEdge.setStartVertex(vertex);
            }

            // halfEdge points triangle
            int triangleId = halfEdge.getTriangleId();
            if (triangleId != -1) {
                TerrainTriangle triangle = trianglesMap.get(triangleId);
                halfEdge.setTriangle(triangle);
            }
            // halfEdge points next
            int nextId = halfEdge.getNextId();
            if (nextId != -1) {
                TerrainHalfEdge next = halfEdgesMap.get(nextId);
                halfEdge.setNext(next);
            }

            // halfEdge points twin
            // twin can be null
            int twinId = halfEdge.getTwinId();
            if (twinId != -1) {
                TerrainHalfEdge twin = halfEdgesMap.get(twinId);
                halfEdge.setTwin(twin);
            }
        }

    }

}
