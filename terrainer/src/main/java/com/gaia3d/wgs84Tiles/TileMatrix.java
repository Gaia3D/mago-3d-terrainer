package com.gaia3d.wgs84Tiles;


import com.gaia3d.basic.structure.*;
import com.gaia3d.basic.types.HalfEdgeType;
import com.gaia3d.command.GlobalOptions;
import com.gaia3d.quantizedMesh.QuantizedMesh;
import com.gaia3d.quantizedMesh.QuantizedMeshManager;
import com.gaia3d.reader.FileUtils;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.util.io.BigEndianDataOutputStream;
import com.gaia3d.util.io.LittleEndianDataOutputStream;
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
    private final double VERTEXT_COINCIDENT_ERROR = 0.0000000000001;
    private static final GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final TilesRange tilesRange;
    private final List<List<TileWgs84>> tilesMatrixRowCol = new ArrayList<>();
    public TileWgs84Manager manager = null;
    // the tilesMatrixRowCol is a matrix of tiles
    // all the arrays have the same length

    GaiaBoundingBox bboxMemSave = new GaiaBoundingBox();
    List<GaiaVertex> listVerticesMemSave = new ArrayList<>();
    List<GaiaHalfEdge> listHalfEdgesMemSave = new ArrayList<>();

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

        listVerticesMemSave.clear();
        listHalfEdgesMemSave.clear();
    }

    private boolean setTwinHalfEdgeWithHalfEdgesList(GaiaHalfEdge halfEdge, List<GaiaHalfEdge> halfEdgesList, int axisToCheck) {
        // axisToCheck 0 = x axis, 1 = y axis, 2 = both axis
        for (GaiaHalfEdge halfEdge2 : halfEdgesList) {
            if (halfEdge2.getTwin() != null) {
                continue;
            }

            if (halfEdge.isHalfEdgePossibleTwin(halfEdge2, VERTEXT_COINCIDENT_ERROR, axisToCheck)) {
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

    private void setTwinsBetweenHalfEdges(List<GaiaHalfEdge> listHEdges_A, List<GaiaHalfEdge> listHEdges_B, int axisToCheck) {
        for (GaiaHalfEdge halfEdge : listHEdges_A) {
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

        List<GaiaMesh> rowMeshesList = new ArrayList<>();
        int axisToCheck = 1;
        for (int i = 0; i < rowsCount; i++) {
            List<TileWgs84> rowTilesArray = tilesMatrixRowCol.get(i);
            GaiaMesh rowMesh = null;

            for (int j = 0; j < colsCount; j++) {
                TileWgs84 tile = rowTilesArray.get(j);
                if (tile != null) {
                    GaiaMesh tileMesh = tile.getMesh();
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
                        List<GaiaHalfEdge> rowMeshRightHalfEdges = rowMesh.getHalfEdgesByType(HalfEdgeType.RIGHT);
                        List<GaiaHalfEdge> tileMeshLeftHalfEdges = tileMesh.getHalfEdgesByType(HalfEdgeType.LEFT);

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
        GaiaMesh resultMesh = null;
        axisToCheck = 0;
        for (GaiaMesh rowMesh : rowMeshesList) {
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
                    List<GaiaHalfEdge> resultMeshDownHalfEdges = resultMesh.getHalfEdgesByType(HalfEdgeType.DOWN);
                    List<GaiaHalfEdge> rowMeshUpHalfEdges = rowMesh.getHalfEdgesByType(HalfEdgeType.UP);
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
                    List<GaiaHalfEdge> resultMesh_up_halfEdges = resultMesh.getHalfEdgesByType(HalfEdgeType.UP);
                    List<GaiaHalfEdge> rowMesh_down_halfEdges = rowMesh.getHalfEdgesByType(HalfEdgeType.DOWN);
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
            if (this.manager.isCalculateNormals()) {
                this.listVerticesMemSave.clear();
                this.listHalfEdgesMemSave.clear();
                resultMesh.calculateNormals(this.listVerticesMemSave, this.listHalfEdgesMemSave);
            }

            // now save the 9 tiles
            List<GaiaMesh> separatedMeshes = new ArrayList<>();
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

    public void saveQuantizedMeshes(List<GaiaMesh> separatedMeshes) throws IOException {
        boolean originIsLeftUp = this.manager.isOriginIsLeftUp();
        boolean calculateNormals = this.manager.isCalculateNormals();

        for (GaiaMesh mesh : separatedMeshes) {
            GaiaTriangle triangle = mesh.triangles.get(0); // take the first triangle
            TileIndices tileIndices = triangle.getOwnerTileIndices();

            TileWgs84 tile = new TileWgs84(null, this.manager);
            tile.setTileIndices(tileIndices);
            String imageryType = this.manager.getImageryType();
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

    public void saveFile(GaiaMesh mesh, String filePath) throws IOException {
        String foldersPath = FileUtils.removeFileNameFromPath(filePath);
        FileUtils.createAllFoldersIfNoExist(foldersPath);

        File file = new File(filePath);
        Files.deleteIfExists(file.toPath());

        BigEndianDataOutputStream dataOutputStream = new BigEndianDataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        // save the tile
        mesh.saveDataOutputStream(dataOutputStream);

        dataOutputStream.close();
    }

    public boolean saveSeparatedTiles(List<GaiaMesh> separatedMeshes) {
        int meshesCount = separatedMeshes.size();
        int counter = 0;
        for (int i = 0; i < meshesCount; i++) {
            GaiaMesh mesh = separatedMeshes.get(i);

            GaiaTriangle triangle = mesh.triangles.get(0);
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
                log.error(e.getMessage());
                return false;
            }
            counter++;
        }

        return true;
    }

    private boolean saveSeparatedChildrenTiles(List<GaiaMesh> separatedMeshes) {
        int meshesCount = separatedMeshes.size();
        int counter = 0;
        for (int i = 0; i < meshesCount; i++) {
            GaiaMesh mesh = separatedMeshes.get(i);

            GaiaTriangle triangle = mesh.triangles.get(0); // take the first triangle
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
                String imageryType = this.manager.getImageryType();

                // 2- make the 4 children
                TileIndices childLightUpTileIndices = tileIndices.getChildLeftUpTileIndices(originIsLeftUp);
                TileIndices childRightUpTileIndices = tileIndices.getChildRightUpTileIndices(originIsLeftUp);
                TileIndices childLeftDownTileIndices = tileIndices.getChildLeftDownTileIndices(originIsLeftUp);
                TileIndices childRightDownTileIndices = tileIndices.getChildRightDownTileIndices(originIsLeftUp);

                // 1rst, classify the triangles of the tile
                GeographicExtension geoExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, imageryType, originIsLeftUp);
                double midLonDeg = geoExtension.getMidLongitudeDeg();
                double midLatDeg = geoExtension.getMidLatitudeDeg();
                List<GaiaTriangle> triangles = mesh.triangles;
                for (GaiaTriangle gaiaTriangle : triangles) {
                    triangle = gaiaTriangle;

                    if (triangle.getObjectStatus() == GaiaObjectStatus.DELETED) {
                        continue;
                    }

                    this.listVerticesMemSave.clear();
                    this.listHalfEdgesMemSave.clear();
                    Vector3d barycenter = triangle.getBarycenter(this.listVerticesMemSave, this.listHalfEdgesMemSave);
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

                List<GaiaMesh> childMeshes = new ArrayList<>();
                this.getSeparatedMeshes(mesh, childMeshes, this.manager.isOriginIsLeftUp());

                // 3- save the 4 children
                int childMeshesCount = childMeshes.size();
                for (GaiaMesh childMesh : childMeshes) {
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
                        log.error(e.getMessage());
                        return false;
                    }
                }
            }
            counter++;
        }

        return true;
    }

    private List<GaiaHalfEdge> getHalfEdgesOfTriangles(List<GaiaTriangle> triangles, List<GaiaHalfEdge> resultHalfEdges, List<GaiaHalfEdge> listHalfEdgesMemSave) {
        if (resultHalfEdges == null) {
            resultHalfEdges = new ArrayList<>();
        }
        //List<GaiaHalfEdge> halfEdgesLoop = new ArrayList<>();
        listHalfEdgesMemSave.clear();
        for (GaiaTriangle triangle : triangles) {
            triangle.getHalfEdge().getHalfEdgesLoop(listHalfEdgesMemSave);
            resultHalfEdges.addAll(listHalfEdgesMemSave);
            listHalfEdgesMemSave.clear();
        }
        return resultHalfEdges;
    }

    private List<GaiaVertex> getVerticesOfTriangles(List<GaiaTriangle> triangles) {
        List<GaiaVertex> resultVertices = new ArrayList<>();
        HashMap<GaiaVertex, Integer> map_vertices = new HashMap<>();
        for (GaiaTriangle triangle : triangles) {
            this.listVerticesMemSave.clear();
            this.listHalfEdgesMemSave.clear();
            this.listVerticesMemSave = triangle.getVertices(this.listVerticesMemSave, this.listHalfEdgesMemSave);
            for (GaiaVertex vertex : this.listVerticesMemSave) {
                if (!map_vertices.containsKey(vertex)) {
                    map_vertices.put(vertex, 1);
                    resultVertices.add(vertex);
                }
            }
        }
        return resultVertices;
    }

    public void getSeparatedMeshes(GaiaMesh bigMesh, List<GaiaMesh> resultSeparatedMeshes, boolean originIsLeftUp) {
        // separate by ownerTile_tileIndices
        List<GaiaTriangle> triangles = bigMesh.triangles;
        HashMap<String, List<GaiaTriangle>> map_triangles = new HashMap<>();
        for (GaiaTriangle triangle : triangles) {
            if (triangle.getOwnerTileIndices() != null) {
                TileIndices tileIndices = triangle.getOwnerTileIndices();
                String tileIndicesString = tileIndices.getString();
                List<GaiaTriangle> trianglesList = map_triangles.get(tileIndicesString);
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
            List<GaiaTriangle> trianglesList = map_triangles.get(tileIndicesString);

            GaiaMesh separatedMesh = new GaiaMesh();
            separatedMesh.triangles = trianglesList;
            TileIndices tileIndices = trianglesList.get(0).getOwnerTileIndices();
            TileIndices L_tileIndices = tileIndices.getLeftTileIndices(originIsLeftUp);
            TileIndices R_tileIndices = tileIndices.getRightTileIndices(originIsLeftUp);
            TileIndices U_tileIndices = tileIndices.getUpTileIndices(originIsLeftUp);
            TileIndices D_tileIndices = tileIndices.getDownTileIndices(originIsLeftUp);

            //GaiaBoundingBox bbox = this.getBBoxOfTriangles(trianglesList);
            this.listHalfEdgesMemSave.clear();
            List<GaiaHalfEdge> halfEdges = new ArrayList<>();
            halfEdges = this.getHalfEdgesOfTriangles(trianglesList, halfEdges, this.listHalfEdgesMemSave); // note : "halfEdges" if different to "this.listHalfEdgesMemSave"
            // for all HEdges, check the triangle of the twin
            // if the triangle of the twin has different ownerTile_tileIndices, then set the twin as null
            int halfEdges_count = halfEdges.size();
            for (int i = 0; i < halfEdges_count; i++) {
                GaiaHalfEdge halfEdge = halfEdges.get(i);
                GaiaHalfEdge twin = halfEdge.getTwin();
                if (twin != null) {
                    GaiaTriangle twins_triangle = twin.getTriangle();
                    if (twins_triangle != null) {
                        String twins_triangle_tileIndicesString = twins_triangle.getOwnerTileIndices().getString();
                        if (!twins_triangle_tileIndicesString.equals(tileIndicesString)) {
                            // the twin triangle has different ownerTile_tileIndices
                            halfEdge.setTwin(null);
                            twin.setTwin(null);

                            // now, for the hedges, must calculate the hedgeType
                            // must know the relative position of the twin triangle's tile
                            if (twins_triangle_tileIndicesString.equals(L_tileIndices.getString())) {
                                halfEdge.setType(HalfEdgeType.LEFT);
                                twin.setType(HalfEdgeType.RIGHT);
                            } else if (twins_triangle_tileIndicesString.equals(R_tileIndices.getString())) {
                                halfEdge.setType(HalfEdgeType.RIGHT);
                                twin.setType(HalfEdgeType.LEFT);
                            } else if (twins_triangle_tileIndicesString.equals(U_tileIndices.getString())) {
                                halfEdge.setType(HalfEdgeType.UP);
                                twin.setType(HalfEdgeType.DOWN);
                            } else if (twins_triangle_tileIndicesString.equals(D_tileIndices.getString())) {
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

    public void recalculateElevation(GaiaMesh gaiaMesh, TilesRange tilesRange) throws TransformException, IOException {
        List<GaiaTriangle> triangles = new ArrayList<>();
        gaiaMesh.getTrianglesByTilesRange(tilesRange, triangles, null);

        HashMap<GaiaVertex, GaiaVertex> mapVertices = new HashMap<>();
        for (GaiaTriangle triangle : triangles) {
            this.listVerticesMemSave.clear();
            this.listHalfEdgesMemSave.clear();
            this.listVerticesMemSave = triangle.getVertices(this.listVerticesMemSave, this.listHalfEdgesMemSave);
            for (GaiaVertex vertex : this.listVerticesMemSave) {
                mapVertices.put(vertex, vertex);
            }
        }

        // now make vertices from the hashMap
        List<GaiaVertex> verticesOfCurrentTile = new ArrayList<>(mapVertices.values());
        TerrainElevationDataManager terrainElevationDataManager = this.manager.getTerrainElevationDataManager();

        int verticesCount = verticesOfCurrentTile.size();
        log.debug("recalculating elevations... vertices count : " + verticesCount);
        TileIndices tileIndicesAux = new TileIndices();
        boolean originIsLeftUp = this.manager.isOriginIsLeftUp();
        int currDepth = tilesRange.getTileDepth();
        for (int i = 0; i < verticesCount; i++) {
            GaiaVertex vertex = verticesOfCurrentTile.get(i);
            TileWgs84Utils.selectTileIndices(currDepth, vertex.getPosition().x, vertex.getPosition().y, tileIndicesAux, originIsLeftUp);
            vertex.getPosition().z = terrainElevationDataManager.getElevationBilinearRasterTile(tileIndicesAux, this.manager, vertex.getPosition().x, vertex.getPosition().y);
            //vertex.getPosition().z = terrainElevationDataManager.getElevation(vertex.getPosition().x, vertex.getPosition().y, this.manager.getMemSaveTerrainElevDataList());
        }
    }

    public boolean mustRefineTriangle(GaiaTriangle triangle) throws TransformException, IOException {
        if (triangle.isRefineChecked()) {
            return false;
        }

        TerrainElevationDataManager terrainElevationDataManager = this.manager.getTerrainElevationDataManager();
        TileIndices tileIndices = triangle.getOwnerTileIndices();

        // check if the triangle must be refined
        this.listVerticesMemSave.clear();
        this.listHalfEdgesMemSave.clear();
        GaiaBoundingBox bboxTriangle = triangle.getBoundingBox(this.listVerticesMemSave, this.listHalfEdgesMemSave);
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
        this.listVerticesMemSave.clear();
        this.listHalfEdgesMemSave.clear();
        double triangleMaxLengthMeters = triangle.getTriangleMaxSizeInMeters(this.listVerticesMemSave, this.listHalfEdgesMemSave);
        double minTriangleSizeForDepth = this.manager.getMinTriangleSizeForTileDepth(triangle.getOwnerTileIndices().getL());


        if (triangleMaxLengthMeters < minTriangleSizeForDepth) {
            triangle.setRefineChecked(true);
            return false;
        }


        double maxTriangleSizeForDepth = this.manager.getMaxTriangleSizeForTileDepth(triangle.getOwnerTileIndices().getL());
        if (triangleMaxLengthMeters > maxTriangleSizeForDepth) {
            return true;
        }

        // check if the triangle intersects the terrainData
        GeographicExtension rootGeographicExtension = terrainElevationDataManager.getRootGeographicExtension();
        if (!rootGeographicExtension.intersectsBox(bboxTriangle.getMinX(), bboxTriangle.getMinY(), bboxTriangle.getMaxX(), bboxTriangle.getMaxY())) {
            // Need check only the 3 vertex of the triangle
            this.listVerticesMemSave.clear();
            this.listHalfEdgesMemSave.clear();
            this.listVerticesMemSave = triangle.getVertices(this.listVerticesMemSave, this.listHalfEdgesMemSave);
            int verticesCount = this.listVerticesMemSave.size();
            for (GaiaVertex vertex : this.listVerticesMemSave) {
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
        this.listVerticesMemSave.clear();
        this.listHalfEdgesMemSave.clear();
        GaiaPlane plane = triangle.getPlane(this.listVerticesMemSave, this.listHalfEdgesMemSave);
        this.listVerticesMemSave.clear();
        this.listHalfEdgesMemSave.clear();
        Vector3d barycenter = triangle.getBarycenter(this.listVerticesMemSave, this.listHalfEdgesMemSave);
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
        List<GaiaHalfEdge> memSavehedges = new ArrayList<>();
        GaiaLine2D memSaveline = new GaiaLine2D();

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
                    this.listVerticesMemSave.clear();
                    intersects = triangle.intersectsPointXY(pos_x, pos_y, memSavehedges, this.listVerticesMemSave, memSaveline);

                    if (!intersects) {
                        continue;
                    }
                    log.debug("Filtered by RasterTile : L : " + tileIndices.getL() + " # col : " + col + " / " + colsCount + " # row : " + row + " / " + rowsCount + " # cosAng : " + cosAng + " # distToPlane : " + distToPlane + " # maxDiff : " + maxDiff);
                    memSavehedges.clear();
                    return true;
                }
                rowAux++;
            }
            colAux++;
        }
        memSavehedges.clear();

        triangle.setRefineChecked(true);
        return false;
    }


    private boolean refineMeshOneIteration(GaiaMesh mesh, TilesRange tilesRange) throws TransformException, IOException {
        // Inside the mesh, there are triangles of 9 different tiles
        // Here refine only the triangles of the current tile

        // refine the mesh
        boolean refined = false;
        int splitCount = 0;
        int trianglesCount = mesh.triangles.size();
        log.debug("[RefineMesh] Triangles count : {}", trianglesCount);
        for (int i = 0; i < trianglesCount; i++) {
            GaiaTriangle triangle = mesh.triangles.get(i);

            if (triangle.getObjectStatus() == GaiaObjectStatus.DELETED) {
                continue;
            }

            if (!tilesRange.intersects(triangle.getOwnerTileIndices())) {
                continue;
            }

            if (mustRefineTriangle(triangle)) {
                this.manager.getMemSaveTrianglesList().clear();
                this.listHalfEdgesMemSave.clear();
                mesh.splitTriangle(triangle, this.manager.getTerrainElevationDataManager(), this.manager.getMemSaveTrianglesList(), this.listHalfEdgesMemSave);
                this.listHalfEdgesMemSave.clear();

                if (!this.manager.getMemSaveTrianglesList().isEmpty()) {
                    splitCount++;
                    refined = true;
                }
                this.manager.getMemSaveTrianglesList().clear();
            }
        }

        if (refined) {
            log.debug("Removing deleted Meshes : Splited count : {}", splitCount);
            mesh.removeDeletedObjects();
            mesh.setObjectsIdInList();
        }


        return refined;
    }

    public void refineMesh(GaiaMesh mesh, TilesRange tilesRange) throws TransformException, IOException {
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
