package com.gaia3d.terrain.tile;


import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.terrain.types.TerrainHalfEdgeType;
import com.gaia3d.terrain.types.TerrainObjectStatus;
import com.gaia3d.command.GlobalOptions;
import com.gaia3d.io.BigEndianDataOutputStream;
import com.gaia3d.io.LittleEndianDataOutputStream;
import com.gaia3d.quantizedMesh.QuantizedMesh;
import com.gaia3d.quantizedMesh.QuantizedMeshManager;
import com.gaia3d.terrain.structure.*;
import com.gaia3d.terrain.util.TileWgs84Utils;
import com.gaia3d.util.FileUtils;
import com.gaia3d.util.GlobeUtils;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.opengis.referencing.operation.TransformException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.Math.abs;

@Slf4j
public class TileMatrix {
    private static final GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final double VERTEXT_COINCIDENT_ERROR = 0.0000000000001;
    private final TilesRange tilesRange;
    private final List<List<TileWgs84>> tilesMatrixRowCol = new ArrayList<>();
    public TileWgs84Manager manager = null;
    // the tilesMatrixRowCol is a matrix of tiles
    // all the arrays have the same length
    List<TerrainVertex> listVertices = new ArrayList<>();
    List<TerrainHalfEdge> listHalfEdges = new ArrayList<>();

    public TileMatrix(TilesRange tilesRange, TileWgs84Manager manager) {
        this.tilesRange = tilesRange;
        this.manager = manager;
    }

    public void deleteObjects() {
        for (List<TileWgs84> row : tilesMatrixRowCol) {
            for (TileWgs84 tile : row) {
                if (tile != null) tile.deleteObjects();
            }
        }

        listVertices.clear();
        listHalfEdges.clear();
    }

    private boolean setTwinHalfEdgeWithHalfEdgesList(TerrainHalfEdge halfEdge, List<TerrainHalfEdge> halfEdgesList, int axisToCheck) {
        // axisToCheck 0 = x axis, 1 = y axis, 2 = both axis
        for (TerrainHalfEdge halfEdge2 : halfEdgesList) {
            if (halfEdge2.getTwin() != null) {
                continue;
            }

            if (halfEdge.isHalfEdgePossibleTwin(halfEdge2, VERTEXT_COINCIDENT_ERROR, axisToCheck)) {
                // 1rst, must change the startVertex & endVertex of the halfEdge2
                TerrainVertex startVertex = halfEdge.getStartVertex();
                TerrainVertex endVertex = halfEdge.getEndVertex();

                TerrainVertex startVertex2 = halfEdge2.getStartVertex();
                TerrainVertex endVertex2 = halfEdge2.getEndVertex();

                List<TerrainHalfEdge> outingHalfEdges_strVertex2 = startVertex2.getAllOutingHalfEdges();
                List<TerrainHalfEdge> outingHalfEdges_endVertex2 = endVertex2.getAllOutingHalfEdges();

                for (TerrainHalfEdge outingHalfEdge : outingHalfEdges_strVertex2) {
                    // NOTE : for outingHEdges of startVertex2, must set "startVertex" the endVertex of halfEdge
                    outingHalfEdge.setStartVertex(endVertex);
                }

                for (TerrainHalfEdge outingHalfEdge : outingHalfEdges_endVertex2) {
                    // NOTE : for outingHEdges of endVertex2, must set "startVertex" the startVertex of halfEdge
                    outingHalfEdge.setStartVertex(startVertex);
                }

                // finally set twins
                halfEdge.setTwin(halfEdge2);

                // now, set as deleted the startVertex2 & endVertex2
                if (!startVertex2.equals(endVertex)) {
                    startVertex2.setObjectStatus(TerrainObjectStatus.DELETED);
                }

                if (!endVertex2.equals(startVertex)) {
                    endVertex2.setObjectStatus(TerrainObjectStatus.DELETED);
                }

                return true;
            }
        }
        return false;
    }

    private void setTwinsBetweenHalfEdges(List<TerrainHalfEdge> listHEdges_A, List<TerrainHalfEdge> listHEdges_B, int axisToCheck) {
        for (TerrainHalfEdge halfEdge : listHEdges_A) {
            if (halfEdge.getTwin() != null) {
                //log.info("HalfEdge has a twin.");
                continue;
            }
            if (!this.setTwinHalfEdgeWithHalfEdgesList(halfEdge, listHEdges_B, axisToCheck)) {
                log.error("No twin halfEdge found.");
            }
        }
    }

    public void makeMatrixMesh(boolean is1rstGeneration) throws TransformException, IOException {
        TileIndices tileIndices = new TileIndices();

        boolean originIsLeftUp = this.manager.isOriginIsLeftUp();

        // 1rst, load or create all the of the matrix
        // Must load from min tile-1 to max tile+1
        tilesMatrixRowCol.clear();
        int minTileX = tilesRange.getMinTileX() - 1;
        int maxTileX = tilesRange.getMaxTileX() + 1;
        int minTileY = tilesRange.getMinTileY() - 1;
        int maxTileY = tilesRange.getMaxTileY() + 1;
        // Note : the minTileX, minTileY, maxTileX, maxTileY are no necessary to verify if the values are out of the limits
        // It is verified in the TileWgs84Manager

        int totalTiles = (maxTileX - minTileX + 1) * (maxTileY - minTileY + 1);

        int counter = 0;
        int counterAux = 0;
        for (int Y = minTileY; Y <= maxTileY; Y++) {
            List<TileWgs84> tilesListRow = new ArrayList<>();
            for (int X = minTileX; X <= maxTileX; X++) {
                tileIndices.set(X, Y, tilesRange.getTileDepth());
                TileWgs84 tile = null;
                if (is1rstGeneration) {
                    tile = this.manager.loadOrCreateTileWgs84(tileIndices);
                } else {
                    tile = this.manager.loadTileWgs84(tileIndices);
                }
                if (counter >= 100) {
                    counter = 0;
                    log.debug("Loading Tile Level : {}, i : {}/{}", tileIndices.getL(), counterAux, totalTiles);
                }

                tilesListRow.add(tile);

                counter++;
                counterAux++;
            }
            tilesMatrixRowCol.add(tilesListRow);
        }


        int rowsCount = tilesMatrixRowCol.size();
        int colsCount = tilesMatrixRowCol.get(0).size();
        log.debug("Making TileMatrix columns : {}, rows : {} ", colsCount, rowsCount);

        List<TerrainMesh> rowMeshesList = new ArrayList<>();
        int axisToCheck = 1;
        for (int i = 0; i < rowsCount; i++) {
            List<TileWgs84> rowTilesArray = tilesMatrixRowCol.get(i);
            TerrainMesh rowMesh = null;

            for (int j = 0; j < colsCount; j++) {
                TileWgs84 tile = rowTilesArray.get(j);
                if (tile != null) {
                    TerrainMesh tileMesh = tile.getMesh();
                    if (rowMesh == null) {
                        rowMesh = tileMesh;
                    } else {
                        //  +----------+----------+
                        //  |          |          |
                        //  | RowMesh  | tileMesh |
                        //  |          |          |
                        //  +----------+----------+
                        // merge the tileMesh with the rowMesh
                        // set twins between the right HEdges of the rowMesh and the left HEdges of the tileMesh
                        List<TerrainHalfEdge> rowMeshRightHalfEdges = rowMesh.getHalfEdgesByType(TerrainHalfEdgeType.RIGHT);
                        List<TerrainHalfEdge> tileMeshLeftHalfEdges = tileMesh.getHalfEdgesByType(TerrainHalfEdgeType.LEFT);

                        // the c_tile can be null
                        if (!rowMeshRightHalfEdges.isEmpty()) {
                            // now, set twins of halfEdges
                            this.setTwinsBetweenHalfEdges(rowMeshRightHalfEdges, tileMeshLeftHalfEdges, axisToCheck);

                            // now, merge the left tile mesh to the result mesh.
                            rowMesh.removeDeletedObjects();
                            rowMesh.mergeMesh(tileMesh);
                        }
                    }
                }
            }
            rowMeshesList.add(rowMesh);
        }

        // now, join all the rowMeshes
        TerrainMesh resultMesh = null;
        axisToCheck = 0;
        for (TerrainMesh rowMesh : rowMeshesList) {
            if (rowMesh == null) {
                continue;
            }

            if (resultMesh == null) {
                resultMesh = rowMesh;
            } else {
                if (originIsLeftUp) {
                    //  +------------+
                    //  |            |
                    //  | resultMesh |
                    //  |            |
                    //  +------------+
                    //  |            |
                    //  | rowMesh    |
                    //  |            |
                    //  +------------+
                    // merge the rowMesh with the resultMesh
                    // set twins between the bottom HEdges of the resultMesh and the top HEdges of the rowMesh
                    List<TerrainHalfEdge> resultMeshDownHalfEdges = resultMesh.getHalfEdgesByType(TerrainHalfEdgeType.DOWN);
                    List<TerrainHalfEdge> rowMeshUpHalfEdges = rowMesh.getHalfEdgesByType(TerrainHalfEdgeType.UP);
                    // the c_tile can be null
                    if (!resultMeshDownHalfEdges.isEmpty()) {
                        // now, set twins of halfEdges
                        this.setTwinsBetweenHalfEdges(resultMeshDownHalfEdges, rowMeshUpHalfEdges, axisToCheck);

                        // now, merge the row mesh to the result mesh.
                        resultMesh.removeDeletedObjects();
                        resultMesh.mergeMesh(rowMesh);
                    }
                } else {
                    //  +------------+
                    //  |            |
                    //  |  rowMesh   |
                    //  |            |
                    //  +------------+
                    //  |            |
                    //  | resultMesh |
                    //  |            |
                    //  +------------+
                    // merge the rowMesh with the resultMesh
                    // set twins between the bottom HEdges of the resultMesh and the top HEdges of the rowMesh
                    List<TerrainHalfEdge> resultMesh_up_halfEdges = resultMesh.getHalfEdgesByType(TerrainHalfEdgeType.UP);
                    List<TerrainHalfEdge> rowMesh_down_halfEdges = rowMesh.getHalfEdgesByType(TerrainHalfEdgeType.DOWN);
                    // the c_tile can be null
                    if (!resultMesh_up_halfEdges.isEmpty()) {
                        // now, set twins of halfEdges
                        this.setTwinsBetweenHalfEdges(resultMesh_up_halfEdges, rowMesh_down_halfEdges, axisToCheck);

                        // now, merge the row mesh to the result mesh.
                        resultMesh.removeDeletedObjects();
                        resultMesh.mergeMesh(rowMesh);
                    }
                }
            }
        }

        log.debug("End making TileMatrix");

        if (resultMesh != null) {
            resultMesh.setObjectsIdInList();

            this.recalculateElevation(resultMesh, tilesRange);
            this.refineMesh(resultMesh, tilesRange);

            // check if you must calculate normals
            if (globalOptions.isCalculateNormals()) {
                this.listVertices.clear();
                this.listHalfEdges.clear();
                resultMesh.calculateNormals(this.listVertices, this.listHalfEdges);
            }

            // now save the 9 tiles
            List<TerrainMesh> separatedMeshes = new ArrayList<>();
            this.getSeparatedMeshes(resultMesh, separatedMeshes, originIsLeftUp);

            // save order :
            // 1- saveSeparatedTiles()
            // 2- saveQuantizedMeshes()
            // 3- saveSeparatedChildrenTiles()
            //------------------------------------------
            log.debug("Saving Separated Tiles...");
            saveSeparatedTiles(separatedMeshes);

            // now save quantizedMeshes
            log.debug("Saving Quantized Meshes...");
            saveQuantizedMeshes(separatedMeshes);

            // finally save the children tiles
            // note : the children tiles must be the last saved
            log.debug("Saving Separated children Tiles...");
            saveSeparatedChildrenTiles(separatedMeshes);
        } else {
            log.error("ResultMesh is null.");
        }
    }

    public void saveQuantizedMeshes(List<TerrainMesh> separatedMeshes) throws IOException {
        boolean originIsLeftUp = this.manager.isOriginIsLeftUp();
        boolean calculateNormals = globalOptions.isCalculateNormals();

        for (TerrainMesh mesh : separatedMeshes) {
            TerrainTriangle triangle = mesh.triangles.get(0); // take the first triangle
            TileIndices tileIndices = triangle.getOwnerTileIndices();

            TileWgs84 tile = new TileWgs84(null, this.manager);
            tile.setTileIndices(tileIndices);
            String imageryType = this.manager.getIMAGINARY_TYPE();
            tile.setGeographicExtension(TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, imageryType, originIsLeftUp));
            tile.setMesh(mesh);

            QuantizedMeshManager quantizedMeshManager = new QuantizedMeshManager();
            QuantizedMesh quantizedMesh = quantizedMeshManager.getQuantizedMeshFromTile(tile, calculateNormals);
            String tileFullPath = this.manager.getQuantizedMeshTilePath(tileIndices);
            String tileFolderPath = this.manager.getQuantizedMeshTileFolderPath(tileIndices);
            FileUtils.createAllFoldersIfNoExist(tileFolderPath);

            LittleEndianDataOutputStream dataOutputStream = new LittleEndianDataOutputStream(new BufferedOutputStream(new FileOutputStream(tileFullPath)));

            // save the tile
            quantizedMesh.saveDataOutputStream(dataOutputStream, calculateNormals);
            dataOutputStream.close();
        }
    }

    public void saveFile(TerrainMesh mesh, String filePath) throws IOException {
        String foldersPath = FileUtils.removeFileNameFromPath(filePath);
        FileUtils.createAllFoldersIfNoExist(foldersPath);

        File file = new File(filePath);
        Files.deleteIfExists(file.toPath());

        BigEndianDataOutputStream dataOutputStream = new BigEndianDataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        // save the tile
        mesh.saveDataOutputStream(dataOutputStream);

        dataOutputStream.close();
    }

    public boolean saveSeparatedTiles(List<TerrainMesh> separatedMeshes) {
        int meshesCount = separatedMeshes.size();
        int counter = 0;
        for (int i = 0; i < meshesCount; i++) {
            TerrainMesh mesh = separatedMeshes.get(i);

            TerrainTriangle triangle = mesh.triangles.get(0);
            TileIndices tileIndices = triangle.getOwnerTileIndices();
            String tileTempDirectory = globalOptions.getTileTempPath();
            String tileFilePath = TileWgs84Utils.getTileFilePath(tileIndices.getX(), tileIndices.getY(), tileIndices.getL());
            String tileFullPath = tileTempDirectory + File.separator + tileFilePath;

            if (counter >= 100) {
                counter = 0;
                log.debug("Saving separated tiles... L : " + tileIndices.getL() + " i : " + i + " / " + meshesCount);
            }

            try {
                saveFile(mesh, tileFullPath);
            } catch (IOException e) {
                log.error("Error:", e);
                return false;
            }
            counter++;
        }

        return true;
    }

    private boolean saveSeparatedChildrenTiles(List<TerrainMesh> separatedMeshes) {
        int meshesCount = separatedMeshes.size();
        int counter = 0;
        for (int i = 0; i < meshesCount; i++) {
            TerrainMesh mesh = separatedMeshes.get(i);

            TerrainTriangle triangle = mesh.triangles.get(0); // take the first triangle
            TileIndices tileIndices = triangle.getOwnerTileIndices();

            if (counter >= 100) {
                counter = 0;
                log.debug("Saving children tiles... L : " + tileIndices.getL() + " i : " + i + " / " + meshesCount);
            }

            // Save children if necessary************************************************************************
            int minTileDepth = globalOptions.getMinimumTileDepth();
            int maxTileDepth = globalOptions.getMaximumTileDepth();
            if (tileIndices.getL() < maxTileDepth) {
                // 1rst, mark triangles with the children tile indices
                boolean originIsLeftUp = this.manager.isOriginIsLeftUp();
                String imageryType = this.manager.getIMAGINARY_TYPE();

                // 2- make the 4 children
                TileIndices childLightUpTileIndices = tileIndices.getChildLeftUpTileIndices(originIsLeftUp);
                TileIndices childRightUpTileIndices = tileIndices.getChildRightUpTileIndices(originIsLeftUp);
                TileIndices childLeftDownTileIndices = tileIndices.getChildLeftDownTileIndices(originIsLeftUp);
                TileIndices childRightDownTileIndices = tileIndices.getChildRightDownTileIndices(originIsLeftUp);

                // 1rst, classify the triangles of the tile
                GeographicExtension geoExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, imageryType, originIsLeftUp);
                double midLonDeg = geoExtension.getMidLongitudeDeg();
                double midLatDeg = geoExtension.getMidLatitudeDeg();
                List<TerrainTriangle> triangles = mesh.triangles;
                for (TerrainTriangle gaiaTriangle : triangles) {
                    triangle = gaiaTriangle;

                    if (triangle.getObjectStatus() == TerrainObjectStatus.DELETED) {
                        continue;
                    }

                    this.listVertices.clear();
                    this.listHalfEdges.clear();
                    Vector3d barycenter = triangle.getBarycenter(this.listVertices, this.listHalfEdges);
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
                this.getSeparatedMeshes(mesh, childMeshes, this.manager.isOriginIsLeftUp());

                // 3- save the 4 children
                int childMeshesCount = childMeshes.size();
                for (TerrainMesh childMesh : childMeshes) {
                    triangle = childMesh.triangles.get(0); // take the first triangle
                    TileIndices childTileIndices = triangle.getOwnerTileIndices();
                    String tileTempDirectory = globalOptions.getTileTempPath();
                    String outputDirectory = globalOptions.getOutputPath();
                    String childTileFilePath = TileWgs84Utils.getTileFilePath(childTileIndices.getX(), childTileIndices.getY(), childTileIndices.getL());
                    String childTileFullPath = tileTempDirectory + File.separator + childTileFilePath;

                    try {
                        //log.debug("Saving children tiles... L : " + childTileIndices.getL() + " i : " + j + " / " + childMeshesCount);
                        saveFile(childMesh, childTileFullPath); // original
                    } catch (IOException e) {
                        log.error("Error:", e);
                        return false;
                    }
                }
            }
            counter++;
        }

        return true;
    }

    private List<TerrainHalfEdge> getHalfEdgesOfTriangles(List<TerrainTriangle> triangles, List<TerrainHalfEdge> resultHalfEdges, List<TerrainHalfEdge> listHalfEdges) {
        if (resultHalfEdges == null) {
            resultHalfEdges = new ArrayList<>();
        }
        //List<GaiaHalfEdge> halfEdgesLoop = new ArrayList<>();
        listHalfEdges.clear();
        for (TerrainTriangle triangle : triangles) {
            triangle.getHalfEdge().getHalfEdgesLoop(listHalfEdges);
            resultHalfEdges.addAll(listHalfEdges);
            listHalfEdges.clear();
        }
        return resultHalfEdges;
    }

    private List<TerrainVertex> getVerticesOfTriangles(List<TerrainTriangle> triangles) {
        List<TerrainVertex> resultVertices = new ArrayList<>();
        HashMap<TerrainVertex, Integer> map_vertices = new HashMap<>();
        for (TerrainTriangle triangle : triangles) {
            this.listVertices.clear();
            this.listHalfEdges.clear();
            this.listVertices = triangle.getVertices(this.listVertices, this.listHalfEdges);
            for (TerrainVertex vertex : this.listVertices) {
                if (!map_vertices.containsKey(vertex)) {
                    map_vertices.put(vertex, 1);
                    resultVertices.add(vertex);
                }
            }
        }
        return resultVertices;
    }

    public void getSeparatedMeshes(TerrainMesh bigMesh, List<TerrainMesh> resultSeparatedMeshes, boolean originIsLeftUp) {
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
            this.listHalfEdges.clear();
            List<TerrainHalfEdge> halfEdges = new ArrayList<>();
            halfEdges = this.getHalfEdgesOfTriangles(trianglesList, halfEdges, this.listHalfEdges); // note : "halfEdges" if different to "this.listHalfEdges"
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
            separatedMesh.vertices = this.getVerticesOfTriangles(trianglesList);

            resultSeparatedMeshes.add(separatedMesh);
        }

    }

    public void recalculateElevation(TerrainMesh terrainMesh, TilesRange tilesRange) throws TransformException, IOException {
        List<TerrainTriangle> triangles = new ArrayList<>();
        terrainMesh.getTrianglesByTilesRange(tilesRange, triangles, null);

        HashMap<TerrainVertex, TerrainVertex> mapVertices = new HashMap<>();
        for (TerrainTriangle triangle : triangles) {
            this.listVertices.clear();
            this.listHalfEdges.clear();
            this.listVertices = triangle.getVertices(this.listVertices, this.listHalfEdges);
            for (TerrainVertex vertex : this.listVertices) {
                mapVertices.put(vertex, vertex);
            }
        }

        // now make vertices from the hashMap
        List<TerrainVertex> verticesOfCurrentTile = new ArrayList<>(mapVertices.values());
        TerrainElevationDataManager terrainElevationDataManager = this.manager.getTerrainElevationDataManager();

        int verticesCount = verticesOfCurrentTile.size();
        log.debug("recalculating elevations... vertices count : " + verticesCount);
        TileIndices tileIndicesAux = new TileIndices();
        boolean originIsLeftUp = this.manager.isOriginIsLeftUp();
        int currDepth = tilesRange.getTileDepth();
        for (int i = 0; i < verticesCount; i++) {
            TerrainVertex vertex = verticesOfCurrentTile.get(i);
            TileWgs84Utils.selectTileIndices(currDepth, vertex.getPosition().x, vertex.getPosition().y, tileIndicesAux, originIsLeftUp);
            vertex.getPosition().z = terrainElevationDataManager.getElevationBilinearRasterTile(tileIndicesAux, this.manager, vertex.getPosition().x, vertex.getPosition().y);
            //vertex.getPosition().z = terrainElevationDataManager.getElevation(vertex.getPosition().x, vertex.getPosition().y, this.manager.getTerrainElevDataList());
        }
    }

    public boolean mustRefineTriangle(TerrainTriangle triangle) throws TransformException, IOException {
        if (triangle.isRefineChecked()) {
            return false;
        }

        TerrainElevationDataManager terrainElevationDataManager = this.manager.getTerrainElevationDataManager();
        TileIndices tileIndices = triangle.getOwnerTileIndices();

        // check if the triangle must be refined
        this.listVertices.clear();
        this.listHalfEdges.clear();
        GaiaBoundingBox bboxTriangle = triangle.getBoundingBox(this.listVertices, this.listHalfEdges);
        double bboxMaxLength = bboxTriangle.getLongestDistanceXY();
        double equatorialRadius = GlobeUtils.EQUATORIAL_RADIUS;
        double bboxMaxLengthInMeters = Math.toRadians(bboxMaxLength) * equatorialRadius;

        int currL = triangle.getOwnerTileIndices().getL();

        double tileSize = TileWgs84Utils.getTileSizeInMetersByDepth(currL);
        double scale = bboxMaxLengthInMeters / tileSize;

        // Y = 0.8X + 0.2.
        scale = 0.8 * scale + 0.2;

        double maxDiff = this.manager.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(triangle.getOwnerTileIndices().getL());
        maxDiff *= scale; // scale the maxDiff

        TileWgs84Raster tileRaster = terrainElevationDataManager.getTileWgs84Raster(tileIndices, this.manager);


        // if the triangle size is very small, then do not refine**********************
        // Calculate the maxLength of the triangle in meters
        this.listVertices.clear();
        this.listHalfEdges.clear();
        double triangleMaxLengthMeters = triangle.getTriangleMaxSizeInMeters(this.listVertices, this.listHalfEdges);
        double minTriangleSizeForDepth = this.manager.getMinTriangleSizeForTileDepth(triangle.getOwnerTileIndices().getL());


        if (triangleMaxLengthMeters < minTriangleSizeForDepth) {
            triangle.setRefineChecked(true);
            log.debug("Filtered by Min Triangle Size : L : " + tileIndices.getL() + " # triangleMaxLengthMeters : " + triangleMaxLengthMeters + " # minTriangleSizeForDepth : " + minTriangleSizeForDepth);
            return false;
        }


        double maxTriangleSizeForDepth = this.manager.getMaxTriangleSizeForTileDepth(triangle.getOwnerTileIndices().getL());
        if (triangleMaxLengthMeters > maxTriangleSizeForDepth) {
            log.debug("Filtered by Max Triangle Size : L : " + tileIndices.getL() + " # triangleMaxLengthMeters : " + triangleMaxLengthMeters + " # maxTriangleSizeForDepth : " + maxTriangleSizeForDepth);
            return true;
        }

        // check if the triangle intersects the terrainData
        GeographicExtension rootGeographicExtension = terrainElevationDataManager.getRootGeographicExtension();
        if (!rootGeographicExtension.intersectsBox(bboxTriangle.getMinX(), bboxTriangle.getMinY(), bboxTriangle.getMaxX(), bboxTriangle.getMaxY())) {
            // Need check only the 3 vertex of the triangle
            this.listVertices.clear();
            this.listHalfEdges.clear();
            this.listVertices = triangle.getVertices(this.listVertices, this.listHalfEdges);
            int verticesCount = this.listVertices.size();
            for (TerrainVertex vertex : this.listVertices) {
                if (vertex.getPosition().z > maxDiff) {
                    return true;
                }
            }

            return false;
        }

        // check with tileRaster
        if (tileRaster == null) {
            return false;
        }

        // calculate the angle between triangleNormalWC with the normal at cartesian of the center of the tile
        float cosAng = 1.0f;
        if (tileIndices.getL() > 10) {

            Vector3f triangleNormalWC = triangle.getNormal(); // this is normalWC
            Vector3d triangleNornalDouble = new Vector3d(triangleNormalWC.x, triangleNormalWC.y, triangleNormalWC.z);
            GeographicExtension geographicExtension = tileRaster.getGeographicExtension();
            Vector3d centerGeoCoord = geographicExtension.getMidPoint();
            double[] centerCartesian = GlobeUtils.geographicToCartesianWgs84(centerGeoCoord.x, centerGeoCoord.y, centerGeoCoord.z);
            Vector3d normalAtCartesian = GlobeUtils.normalAtCartesianPointWgs84(centerCartesian[0], centerCartesian[1], centerCartesian[2]);

            double angRad = triangleNornalDouble.angle(normalAtCartesian);
            cosAng = (float) Math.cos(angRad);
        }

        // check the barycenter of the triangle
        this.listVertices.clear();
        this.listHalfEdges.clear();
        TerrainPlane plane = triangle.getPlane(this.listVertices, this.listHalfEdges);
        this.listVertices.clear();
        this.listHalfEdges.clear();
        Vector3d barycenter = triangle.getBarycenter(this.listVertices, this.listHalfEdges);
        int colIdx = tileRaster.getColumn(barycenter.x);
        int rowIdx = tileRaster.getRow(barycenter.y);
        double baricenterLonDeg = tileRaster.getLonDeg(colIdx);
        double baricenterLatDeg = tileRaster.getLatDeg(rowIdx);

        double elevation = tileRaster.getElevation(colIdx, rowIdx);
        double planeElevation = plane.getValueZ(baricenterLonDeg, baricenterLatDeg);

        double distToPlane = abs(elevation - planeElevation) * cosAng;

        if (distToPlane > maxDiff) {
            // is it Barycenter?
            log.debug("Filtered by Baricenter : L : " + tileIndices.getL() + " # col : " + colIdx + " # row : " + rowIdx + " # distToPlane : " + distToPlane + " # maxDiff : " + maxDiff);
            return true;
        }

        int startCol = tileRaster.getColumn(bboxTriangle.getMinX());
        int startRow = tileRaster.getRow(bboxTriangle.getMinY());
        int endCol = tileRaster.getColumn(bboxTriangle.getMaxX());
        int endRow = tileRaster.getRow(bboxTriangle.getMaxY());

        int colsCount = endCol - startCol + 1;
        int rowsCount = endRow - startRow + 1;

        if (colsCount < 6 || rowsCount < 6) {
            triangle.setRefineChecked(true);
            return false;
        }

        double startLonDeg = tileRaster.getLonDeg(startCol); // here contains the semiDeltaLonDeg, for the pixel center
        double startLatDeg = tileRaster.getLatDeg(startRow); // here contains the semiDeltaLatDeg, for the pixel center

        double deltaLonDeg = tileRaster.getDeltaLonDeg();
        double deltaLatDeg = tileRaster.getDeltaLatDeg();

        double pos_x;
        double pos_y;

        int colAux = 0;
        int rowAux = 0;

        boolean intersects = false;
        List<TerrainHalfEdge> halfEdges = new ArrayList<>();
        TerrainLine2D line2d = new TerrainLine2D();

        for (int col = startCol; col <= endCol; col++) {
            rowAux = 0;
            pos_x = startLonDeg + colAux * deltaLonDeg;
            for (int row = startRow; row <= endRow; row++) {

                // skip the 4 corners of the triangle's bounding rectangle
                if (col == startCol && row == startRow) {
                    rowAux++;
                    continue;
                } else if (col == startCol && row == endRow) {
                    rowAux++;
                    continue;
                } else if (col == endCol && row == startRow) {
                    rowAux++;
                    continue;
                } else if (col == endCol && row == endRow) {
                    rowAux++;
                    continue;
                }
                pos_y = startLatDeg + rowAux * deltaLatDeg;

                float elevationFloat = tileRaster.getElevation(col, row);

                planeElevation = plane.getValueZ(pos_x, pos_y);

                distToPlane = abs(elevationFloat - planeElevation) * cosAng;
                if (distToPlane > maxDiff) {
                    this.listVertices.clear();
                    intersects = triangle.intersectsPointXY(pos_x, pos_y, halfEdges, this.listVertices, line2d);

                    if (!intersects) {
                        continue;
                    }
                    log.debug("Filtered by RasterTile : L : " + tileIndices.getL() + " # col : " + col + " / " + colsCount + " # row : " + row + " / " + rowsCount + " # cosAng : " + cosAng + " # distToPlane : " + distToPlane + " # maxDiff : " + maxDiff);
                    halfEdges.clear();
                    return true;
                }
                rowAux++;
            }
            colAux++;
        }
        halfEdges.clear();

        triangle.setRefineChecked(true);

        log.debug("Filtered by RasterTile : L : " + tileIndices.getL() + " # col : " + colAux + " / " + colsCount + " # row : " + rowAux + " / " + rowsCount + " # cosAng : " + cosAng + " # distToPlane : " + distToPlane + " # maxDiff : " + maxDiff);
        return false;
    }


    private boolean refineMeshOneIteration(TerrainMesh mesh, TilesRange tilesRange) throws TransformException, IOException {
        // Inside the mesh, there are triangles of 9 different tiles
        // Here refine only the triangles of the current tile

        // refine the mesh
        boolean refined = false;
        int splitCount = 0;
        int trianglesCount = mesh.triangles.size();
        log.debug("[RefineMesh] Triangles count : {}", trianglesCount);
        for (int i = 0; i < trianglesCount; i++) {
            TerrainTriangle triangle = mesh.triangles.get(i);

            if (triangle.getObjectStatus() == TerrainObjectStatus.DELETED) {
                continue;
            }

            if (!tilesRange.intersects(triangle.getOwnerTileIndices())) {
                continue;
            }

            if (mustRefineTriangle(triangle)) {
                this.manager.getTriangleList().clear();
                this.listHalfEdges.clear();
                mesh.splitTriangle(triangle, this.manager.getTerrainElevationDataManager(), this.manager.getTriangleList(), this.listHalfEdges);
                this.listHalfEdges.clear();

                if (!this.manager.getTriangleList().isEmpty()) {
                    splitCount++;
                    refined = true;
                }
                this.manager.getTriangleList().clear();
            }
        }

        if (refined) {
            log.debug("Removing deleted Meshes : Splited count : {}", splitCount);
            mesh.removeDeletedObjects();
            mesh.setObjectsIdInList();
        }


        return refined;
    }

    public void refineMesh(TerrainMesh mesh, TilesRange tilesRange) throws TransformException, IOException {
        // Inside the mesh, there are triangles of n different tiles
        // Here refine only the triangles of the tiles of TilesRange

        double maxDiff = this.manager.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(tilesRange.getTileDepth());
        log.debug("[RefineMesh] Tile Level : {} # MaxDiff(m) : {}", tilesRange.getTileDepth(), maxDiff);

        // refine the mesh
        boolean finished = false;
        int splitCount = 0;
        int maxIterations = this.manager.getTriangleRefinementMaxIterations();
        while (!finished) {
            if (!this.refineMeshOneIteration(mesh, tilesRange)) {
                finished = true;
            }

            splitCount++;

            if (splitCount >= maxIterations) {
                finished = true;
            }
        }

        this.manager.getTerrainElevationDataManager().deleteTileRasters();
    }
}
