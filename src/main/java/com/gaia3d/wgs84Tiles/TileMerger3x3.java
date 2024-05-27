package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.*;
import com.gaia3d.basic.types.HalfEdgeType;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//@NoArgsConstructor
//@AllArgsConstructor
@Builder
@Slf4j
public class TileMerger3x3 {

    private final static double VERTEX_COINCIDENT_ERROR = 0.0000000000001;

    //  +----------+----------+----------+
    //  |          |          |          |
    //  | LU_Tile  |  U_Tile  | RU_Tile  |
    //  |          |          |          |
    //  +----------+----------+----------+
    //  |          |          |          |
    //  | L_Tile   |cent_Tile | R_Tile   |
    //  |          |          |          |
    //  +----------+----------+----------+
    //  |          |          |          |
    //  | LD_Tile  | D_Tile   | RD_Tile  |
    //  |          |          |          |
    //  +----------+----------+----------+
    private TileWgs84 centerTile;
    private TileWgs84 leftTile;
    private TileWgs84 rightTile;
    private TileWgs84 upTile;
    private TileWgs84 downTile;

    private TileWgs84 leftUpTile;
    private TileWgs84 rightUpTile;
    private TileWgs84 leftDownTile;
    private TileWgs84 rightDownTile;

    /*public TileMerger3x3(TileWgs84 centerTile, TileWgs84 leftTile, TileWgs84 rightTile, TileWgs84 upTile, TileWgs84 downTile, TileWgs84 leftUpTile, TileWgs84 rightUpTile, TileWgs84 leftDownTile, TileWgs84 rightDownTile) {
        this.centerTile = centerTile;
        this.leftTile = leftTile;
        this.rightTile = rightTile;
        this.upTile = upTile;
        this.downTile = downTile;
        this.leftUpTile = leftUpTile;
        this.rightUpTile = rightUpTile;
        this.leftDownTile = leftDownTile;
        this.rightDownTile = rightDownTile;
    }*/

    private boolean setTwinHalfEdgeWithHalfEdgesList(GaiaHalfEdge halfEdge, List<GaiaHalfEdge> halfEdgesList) {
        for (GaiaHalfEdge halfEdge2 : halfEdgesList) {
            if (halfEdge2.getTwin() != null) {
                // this halfEdge2 has a twin
                continue;
            }

            if (halfEdge.isHalfEdgePossibleTwin(halfEdge2, VERTEX_COINCIDENT_ERROR)) {
                // 1rst, must change the startVertex & endVertex of the halfEdge2
                GaiaVertex startVertex = halfEdge.getStartVertex();
                GaiaVertex endVertex = halfEdge.getEndVertex();

                GaiaVertex startVertex2 = halfEdge2.getStartVertex();
                GaiaVertex endVertex2 = halfEdge2.getEndVertex();


                List<GaiaHalfEdge> outingHalfEdges_strVertex2 = startVertex2.getAllOutingHalfEdges();
                List<GaiaHalfEdge> outingHalfEdges_endVertex2 = endVertex2.getAllOutingHalfEdges();

                for (GaiaHalfEdge outingHalfEdge : outingHalfEdges_strVertex2) {
                    // NOTE : for outingHEdges of startVertex2, must set "startVertex" the endVertex of halfEdge
                    outingHalfEdge.setStartVertex(endVertex);
                }

                for (GaiaHalfEdge outingHalfEdge : outingHalfEdges_endVertex2) {
                    // NOTE : for outingHEdges of endVertex2, must set "startVertex" the startVertex of halfEdge
                    outingHalfEdge.setStartVertex(startVertex);
                }

                // finally set twins
                halfEdge.setTwin(halfEdge2);

                // now, set as deleted the startVertex2 & endVertex2
                if (!startVertex2.equals(endVertex)) {
                    startVertex2.setObjectStatus(GaiaObjectStatus.DELETED);
                }

                if (!endVertex2.equals(startVertex)) {
                    endVertex2.setObjectStatus(GaiaObjectStatus.DELETED);
                }

                return true;
            }
        }
        return false;
    }

    private void setTwinsBetweenHalfEdges(List<GaiaHalfEdge> listHEdges_A, List<GaiaHalfEdge> listHEdges_B) {
        for (GaiaHalfEdge halfEdge : listHEdges_A) {
            if (halfEdge.getTwin() != null) {
                // this halfEdge has a twin
                //log.warn("halfEdge has a twin.");
                continue;
            }
            if (!this.setTwinHalfEdgeWithHalfEdgesList(halfEdge, listHEdges_B)) {
                // error.!***
                log.error("HalfEdge has no twin.");
            }
        }
    }

    private GaiaMesh getMergedMesh_3x1(TileWgs84 L_Tile, TileWgs84 C_Tile, TileWgs84 R_Tile) {
        // given 3 tiles in row, merge them
        //  +----------+----------+----------+
        //  |          |          |          |
        //  | L_Tile   |  C_Tile  | R_Tile   |
        //  |          |          |          |
        //  +----------+----------+----------+

        GaiaMesh resultMergedMesh = null;
        if (C_Tile != null) {
            resultMergedMesh = C_Tile.getMesh();
        }

        if (resultMergedMesh == null) {
            resultMergedMesh = new GaiaMesh();
        }

        if (L_Tile != null) {
            //  +----------+----------+
            //  |          |          |
            //  | L_mesh   | result_m |
            //  |          |          |
            //  +----------+----------+

            // in this case, join halfEdges of the right side of the left tile with the left side of the result mesh.
            GaiaMesh L_mesh = L_Tile.getMesh();
            List<GaiaHalfEdge> leftMeshRightHalfEdges = L_mesh.getHalfEdgesByType(HalfEdgeType.RIGHT);
            List<GaiaHalfEdge> resultMeshLeftHalfEdges = resultMergedMesh.getHalfEdgesByType(HalfEdgeType.LEFT);

            // the c_tile can be null
            if (!resultMeshLeftHalfEdges.isEmpty()) {
                // now, set twins of halfEdges
                this.setTwinsBetweenHalfEdges(leftMeshRightHalfEdges, resultMeshLeftHalfEdges);

                // now, merge the left tile mesh to the result mesh.
                resultMergedMesh.removeDeletedObjects();
                resultMergedMesh.mergeMesh(L_mesh);
            }
        }

        // 3rd, merge the right tile mesh to the result mesh.
        if (R_Tile != null) {
            // now merge the right tile mesh to the result mesh.
            //  +---------------------+----------+
            //  |                     |          |
            //  | result_m            | R_mesh   |
            //  |                     |          |
            //  +---------------------+----------+

            // in this case, join halfEdges of the left side of the right tile with the right side of the result mesh.
            GaiaMesh R_mesh = R_Tile.getMesh();
            List<GaiaHalfEdge> rightMeshLeftHalfEdges = R_mesh.getHalfEdgesByType(HalfEdgeType.LEFT);
            List<GaiaHalfEdge> resultMeshRightHalfEdges = resultMergedMesh.getHalfEdgesByType(HalfEdgeType.RIGHT);

            // the c_tile & left_tile can be null
            if (!resultMeshRightHalfEdges.isEmpty()) {
                // now, set twins of halfEdges
                this.setTwinsBetweenHalfEdges(rightMeshLeftHalfEdges, resultMeshRightHalfEdges);

                // now, merge the right tile mesh to the result mesh.
                resultMergedMesh.removeDeletedObjects();
                resultMergedMesh.mergeMesh(R_mesh);
            }
        }

        return resultMergedMesh;
    }

    private GaiaMesh getMergedMesh_1x3(GaiaMesh U_mesh, GaiaMesh C_mesh, GaiaMesh D_mesh) {
        // given 3 tiles in column, merge them
        //  +----------+
        //  |          |
        //  | U_Tile   |
        //  |          |
        //  +----------+
        //  |          |
        //  | C_Tile   |
        //  |          |
        //  +----------+
        //  |          |
        //  | D_Tile   |
        //  |          |
        //  +----------+

        GaiaMesh resultMergedMesh = C_mesh;

        if (resultMergedMesh == null) {
            resultMergedMesh = new GaiaMesh();
        }

        if (U_mesh != null) {
            //  +----------+
            //  |          |
            //  | U_Tile   |
            //  |          |
            //  +----------+
            //  |          |
            //  | result_m |
            //  |          |
            //  +----------+

            // in this case, join halfEdges of the down side of the up tile with the up side of the result mesh.
            List<GaiaHalfEdge> upMeshDownHalfEdges = U_mesh.getHalfEdgesByType(HalfEdgeType.DOWN);
            List<GaiaHalfEdge> resultMeshUpHalfEdges = resultMergedMesh.getHalfEdgesByType(HalfEdgeType.UP);

            // now, set twins of halfEdges
            this.setTwinsBetweenHalfEdges(upMeshDownHalfEdges, resultMeshUpHalfEdges);

            // now, merge the up tile mesh to the result mesh.
            resultMergedMesh.removeDeletedObjects();
            resultMergedMesh.mergeMesh(U_mesh);
        }

        if (D_mesh != null) {
            //  +----------+
            //  |          |
            //  | result_m |
            //  |          |
            //  +----------+
            //  |          |
            //  | D_Tile   |
            //  |          |
            //  +----------+

            // in this case, join halfEdges of the up side of the down tile with the down side of the result mesh.
            List<GaiaHalfEdge> downMeshUpHalfEdges = D_mesh.getHalfEdgesByType(HalfEdgeType.UP);
            List<GaiaHalfEdge> resultMeshDownHalfEdges = resultMergedMesh.getHalfEdgesByType(HalfEdgeType.DOWN);

            // now, set twins of halfEdges
            this.setTwinsBetweenHalfEdges(downMeshUpHalfEdges, resultMeshDownHalfEdges);

            // now, merge the down tile mesh to the result mesh.
            resultMergedMesh.removeDeletedObjects();
            resultMergedMesh.mergeMesh(D_mesh);
        }


        return resultMergedMesh;
    }

    public GaiaMesh getMergedMesh() {
        // 1rst, copy the center tile mesh to the result mesh.
        GaiaMesh resultMergedMeshUp = this.getMergedMesh_3x1(leftUpTile, upTile, rightUpTile);
        GaiaMesh resultMergedMesh = this.getMergedMesh_3x1(leftTile, centerTile, rightTile);
        GaiaMesh resultMergedMeshDown = this.getMergedMesh_3x1(leftDownTile, downTile, rightDownTile);

        // 2nd, merge the up & down meshes to the result mesh.
        resultMergedMesh = this.getMergedMesh_1x3(resultMergedMeshUp, resultMergedMesh, resultMergedMeshDown);


        return resultMergedMesh;
    }

    private List<GaiaHalfEdge> getHalfEdgesOfTriangles(List<GaiaTriangle> triangles) {
        List<GaiaHalfEdge> resultHalfEdges = new ArrayList<>();
        List<GaiaHalfEdge> halfEdgesLoop = new ArrayList<>();
        for (GaiaTriangle triangle : triangles) {
            triangle.getHalfEdge().getHalfEdgesLoop(halfEdgesLoop);
            resultHalfEdges.addAll(halfEdgesLoop);
            halfEdgesLoop.clear();
        }
        return resultHalfEdges;
    }

    private GaiaBoundingBox getBBoxOfTriangles(List<GaiaTriangle> triangles) {
        GaiaBoundingBox resultBBox = new GaiaBoundingBox();
        for (GaiaTriangle triangle : triangles) {
            GaiaBoundingBox triangleBBox = triangle.getBoundingBox();
            resultBBox.addBoundingBox(triangleBBox);
        }
        return resultBBox;
    }

    private List<GaiaVertex> getVerticesOfTriangles(List<GaiaTriangle> triangles) {
        List<GaiaVertex> resultVertices = new ArrayList<>();
        HashMap<GaiaVertex, Integer> mapVertices = new HashMap<>();
        for (GaiaTriangle triangle : triangles) {
            List<GaiaVertex> vertices = triangle.getVertices();
            for (GaiaVertex vertex : vertices) {
                if (!mapVertices.containsKey(vertex)) {
                    mapVertices.put(vertex, 1);
                    resultVertices.add(vertex);
                }
            }
        }
        return resultVertices;
    }

    public void getSeparatedMeshes(GaiaMesh bigMesh, List<GaiaMesh> resultSeparatedMeshes, boolean originIsLeftUp) {
        // separate by ownerTile_tileIndices
        List<GaiaTriangle> triangles = bigMesh.triangles;
        HashMap<String, List<GaiaTriangle>> mapTriangles = new HashMap<>();
        for (GaiaTriangle triangle : triangles) {
            if (triangle.getOwnerTileIndices() != null) {
                TileIndices tileIndices = triangle.getOwnerTileIndices();
                String tileIndicesString = tileIndices.getString();
                List<GaiaTriangle> trianglesList = mapTriangles.get(tileIndicesString);
                if (trianglesList == null) {
                    trianglesList = new ArrayList<GaiaTriangle>();
                    mapTriangles.put(tileIndicesString, trianglesList);
                }
                trianglesList.add(triangle);
            } else {
                // error
                log.info("Error: triangle has not ownerTile_tileIndices.");
            }
        }

        // now, create separated meshes
        for (String tileIndicesString : mapTriangles.keySet()) {
            List<GaiaTriangle> trianglesList = mapTriangles.get(tileIndicesString);

            GaiaMesh separatedMesh = new GaiaMesh();
            separatedMesh.triangles = trianglesList;
            TileIndices tileIndices = trianglesList.get(0).getOwnerTileIndices();
            TileIndices leftTileIndices = tileIndices.getLeftTileIndices(originIsLeftUp);
            TileIndices rightTileIndices = tileIndices.getRightTileIndices(originIsLeftUp);
            TileIndices upTileIndices = tileIndices.getUpTileIndices(originIsLeftUp);
            TileIndices downTileIndices = tileIndices.getDownTileIndices(originIsLeftUp);

            //GaiaBoundingBox bbox = this.getBBoxOfTriangles(trianglesList);
            List<GaiaHalfEdge> halfEdges = this.getHalfEdgesOfTriangles(trianglesList);
            // for all HEdges, check the triangle of the twin
            // if the triangle of the twin has different ownerTile_tileIndices, then set the twin as null
            for (GaiaHalfEdge halfEdge : halfEdges) {
                GaiaHalfEdge twin = halfEdge.getTwin();
                if (twin != null) {
                    GaiaTriangle twinsTriangle = twin.getTriangle();
                    if (twinsTriangle != null) {
                        String twinsTriangleTileIndicesString = twinsTriangle.getOwnerTileIndices().getString();
                        if (!twinsTriangleTileIndicesString.equals(tileIndicesString)) {
                            // the twin triangle has different ownerTile_tileIndices
                            halfEdge.setTwin(null);

                            // now, for the hedges, must calculate the hedgeType
                            // must know the relative position of the twin triangle's tile
                            if (twinsTriangleTileIndicesString.equals(leftTileIndices.getString())) {
                                halfEdge.setType(HalfEdgeType.LEFT);
                                twin.setType(HalfEdgeType.RIGHT);
                            } else if (twinsTriangleTileIndicesString.equals(rightTileIndices.getString())) {
                                halfEdge.setType(HalfEdgeType.RIGHT);
                                twin.setType(HalfEdgeType.LEFT);
                            } else if (twinsTriangleTileIndicesString.equals(upTileIndices.getString())) {
                                halfEdge.setType(HalfEdgeType.UP);
                                twin.setType(HalfEdgeType.DOWN);
                            } else if (twinsTriangleTileIndicesString.equals(downTileIndices.getString())) {
                                halfEdge.setType(HalfEdgeType.DOWN);
                                twin.setType(HalfEdgeType.UP);
                            }
                        }
                    }
                }
            }

            separatedMesh.halfEdges = halfEdges;
            separatedMesh.vertices = this.getVerticesOfTriangles(trianglesList);

            resultSeparatedMeshes.add(separatedMesh);
        }
    }
}
