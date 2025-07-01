package com.gaia3d.terrain.util;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.structure.*;
import com.gaia3d.terrain.tile.TileIndices;
import com.gaia3d.terrain.tile.TileWgs84Manager;
import com.gaia3d.terrain.types.TerrainHalfEdgeType;
import com.gaia3d.terrain.types.TerrainObjectStatus;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j

public class TerrainMeshUtils {
    public static List<TerrainHalfEdge> getHalfEdgesOfTriangles(List<TerrainTriangle> triangles, List<TerrainHalfEdge> resultHalfEdges, List<TerrainHalfEdge> listHalfEdges) {
        if (resultHalfEdges == null) {
            resultHalfEdges = new ArrayList<>();
        }
        listHalfEdges.clear();
        for (TerrainTriangle triangle : triangles) {
            triangle.getHalfEdge().getHalfEdgesLoop(listHalfEdges);
            resultHalfEdges.addAll(listHalfEdges);
            listHalfEdges.clear();
        }
        return resultHalfEdges;
    }

    public static List<TerrainVertex> getVerticesOfTriangles(List<TerrainTriangle> triangles) {
        List<TerrainVertex> resultVertices = new ArrayList<>();
        HashMap<TerrainVertex, Integer> map_vertices = new HashMap<>();
        List<TerrainVertex> listVertices = new ArrayList<>();
        List<TerrainHalfEdge> listHalfEdges = new ArrayList<>();
        for (TerrainTriangle triangle : triangles) {
            listVertices.clear();
            listHalfEdges.clear();
            listVertices = triangle.getVertices(listVertices, listHalfEdges);
            for (TerrainVertex vertex : listVertices) {
                if (!map_vertices.containsKey(vertex)) {
                    map_vertices.put(vertex, 1);
                    resultVertices.add(vertex);
                }
            }
        }
        return resultVertices;
    }

    public static void getSeparatedMeshes(TerrainMesh bigMesh, List<TerrainMesh> resultSeparatedMeshes, boolean originIsLeftUp) {
        // separate by ownerTile_tileIndices
        List<TerrainTriangle> triangles = bigMesh.triangles;
        HashMap<String, List<TerrainTriangle>> map_triangles = new HashMap<>();
        for (TerrainTriangle triangle : triangles) {
            if (triangle.getOwnerTileIndices() != null) {
                TileIndices tileIndices = triangle.getOwnerTileIndices();
                String tileIndicesString = tileIndices.getString();
                List<TerrainTriangle> trianglesList = map_triangles.get(tileIndicesString);
                if (trianglesList == null) {
                    trianglesList = new ArrayList<>();
                    map_triangles.put(tileIndicesString, trianglesList);
                }
                trianglesList.add(triangle);
            } else {
                // error
                log.info("Error: triangle has not ownerTile_tileIndices.");
            }
        }

        // now, create separated meshes
        for (String tileIndicesString : map_triangles.keySet()) {
            List<TerrainTriangle> trianglesList = map_triangles.get(tileIndicesString);

            TerrainMesh separatedMesh = new TerrainMesh();
            separatedMesh.triangles = trianglesList;
            TileIndices tileIndices = trianglesList.get(0).getOwnerTileIndices();
            TileIndices L_tileIndices = tileIndices.getLeftTileIndices(originIsLeftUp);
            TileIndices R_tileIndices = tileIndices.getRightTileIndices(originIsLeftUp);
            TileIndices U_tileIndices = tileIndices.getUpTileIndices(originIsLeftUp);
            TileIndices D_tileIndices = tileIndices.getDownTileIndices(originIsLeftUp);

            //GaiaBoundingBox bbox = this.getBBoxOfTriangles(trianglesList);
            List<TerrainHalfEdge> listHalfEdges = new ArrayList<>();
            List<TerrainHalfEdge> halfEdges = new ArrayList<>();
            halfEdges = TerrainMeshUtils.getHalfEdgesOfTriangles(trianglesList, halfEdges, listHalfEdges); // note : "halfEdges" if different to "this.listHalfEdges"
            // for all HEdges, check the triangle of the twin
            // if the triangle of the twin has different ownerTile_tileIndices, then set the twin as null
            int halfEdges_count = halfEdges.size();
            for (int i = 0; i < halfEdges_count; i++) {
                TerrainHalfEdge halfEdge = halfEdges.get(i);
                TerrainHalfEdge twin = halfEdge.getTwin();
                if (twin != null) {
                    TerrainTriangle twins_triangle = twin.getTriangle();
                    if (twins_triangle != null) {
                        String twins_triangle_tileIndicesString = twins_triangle.getOwnerTileIndices().getString();
                        if (!twins_triangle_tileIndicesString.equals(tileIndicesString)) {
                            // the twin triangle has different ownerTile_tileIndices
                            halfEdge.setTwin(null);
                            twin.setTwin(null);

                            // now, for the hedges, must calculate the hedgeType
                            // must know the relative position of the twin triangle's tile
                            if (twins_triangle_tileIndicesString.equals(L_tileIndices.getString())) {
                                halfEdge.setType(TerrainHalfEdgeType.LEFT);
                                twin.setType(TerrainHalfEdgeType.RIGHT);
                            } else if (twins_triangle_tileIndicesString.equals(R_tileIndices.getString())) {
                                halfEdge.setType(TerrainHalfEdgeType.RIGHT);
                                twin.setType(TerrainHalfEdgeType.LEFT);
                            } else if (twins_triangle_tileIndicesString.equals(U_tileIndices.getString())) {
                                halfEdge.setType(TerrainHalfEdgeType.UP);
                                twin.setType(TerrainHalfEdgeType.DOWN);
                            } else if (twins_triangle_tileIndicesString.equals(D_tileIndices.getString())) {
                                halfEdge.setType(TerrainHalfEdgeType.DOWN);
                                twin.setType(TerrainHalfEdgeType.UP);
                            }
                        }
                    }
                }
            }

            separatedMesh.halfEdges = halfEdges;
            separatedMesh.vertices = TerrainMeshUtils.getVerticesOfTriangles(trianglesList);
            resultSeparatedMeshes.add(separatedMesh);
        }

    }

    public static void save4ChildrenMeshes(TerrainMesh mesh, TileWgs84Manager manager, GlobalOptions globalOptions) {
        TerrainTriangle triangle = mesh.triangles.get(0); // take the first triangle
        TileIndices tileIndices = triangle.getOwnerTileIndices();

        // First, mark triangles with the children tile indices
        boolean originIsLeftUp = manager.isOriginIsLeftUp();
        String imageryType = manager.getImaginaryType();

        // 2- make the 4 children
        TileIndices childLightUpTileIndices = tileIndices.getChildLeftUpTileIndices(originIsLeftUp);
        TileIndices childRightUpTileIndices = tileIndices.getChildRightUpTileIndices(originIsLeftUp);
        TileIndices childLeftDownTileIndices = tileIndices.getChildLeftDownTileIndices(originIsLeftUp);
        TileIndices childRightDownTileIndices = tileIndices.getChildRightDownTileIndices(originIsLeftUp);

        // Classify the triangles of the tile
        GeographicExtension geoExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, imageryType, originIsLeftUp);
        double midLonDeg = geoExtension.getMidLongitudeDeg();
        double midLatDeg = geoExtension.getMidLatitudeDeg();
        List<TerrainTriangle> triangles = mesh.triangles;

        List<TerrainVertex> listVertices = new ArrayList<>();
        List<TerrainHalfEdge> listHalfEdges = new ArrayList<>();

        for (TerrainTriangle gaiaTriangle : triangles) {
            triangle = gaiaTriangle;

            if (triangle.getObjectStatus() == TerrainObjectStatus.DELETED) {
                continue;
            }

            listVertices.clear();
            listHalfEdges.clear();
            Vector3d barycenter = triangle.getBarycenter(listVertices, listHalfEdges);
            if (barycenter.x < midLonDeg) {
                if (barycenter.y < midLatDeg) {
                    // LD_Tile
                    triangle.setOwnerTileIndices(childLeftDownTileIndices);
                } else {
                    // LU_Tile
                    triangle.setOwnerTileIndices(childLightUpTileIndices);
                }
            } else {
                if (barycenter.y < midLatDeg) {
                    // RD_Tile
                    triangle.setOwnerTileIndices(childRightDownTileIndices);
                } else {
                    // RU_Tile
                    triangle.setOwnerTileIndices(childRightUpTileIndices);
                }
            }
        }

        List<TerrainMesh> childMeshes = new ArrayList<>();
        TerrainMeshUtils.getSeparatedMeshes(mesh, childMeshes, manager.isOriginIsLeftUp());

        if (childMeshes.size() != 4) {
            log.info("Info: childMeshes.size() != 4 : tile indices : " + tileIndices.getString());
        }

        // 3- save the 4 children
        for (TerrainMesh childMesh : childMeshes) {
            triangle = childMesh.triangles.get(0); // take the first triangle
            TileIndices childTileIndices = triangle.getOwnerTileIndices();

            // Now, clamp the vertices in to the tile.***
            TileWgs84Utils.clampVerticesInToTile(childMesh, childTileIndices, manager.getImaginaryType(), manager.originIsLeftUp());

            // Now, save the mesh.***
            String tileTempDirectory = globalOptions.getTileTempPath();
            String childTileFilePath = TileWgs84Utils.getTileFilePath(childTileIndices.getX(), childTileIndices.getY(), childTileIndices.getL());
            String childTileFullPath = tileTempDirectory + File.separator + childTileFilePath;

            try {
                //log.debug("Saving children tiles... L : " + childTileIndices.getL() + " i : " + j + " / " + childMeshesCount);
                childMesh.saveFile(childTileFullPath);
            } catch (IOException e) {
                log.error("Error:", e);
                return;
            }
        }
    }
}
