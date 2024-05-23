package com.gaia3d.wgs84Tiles;


import com.gaia3d.basic.structure.*;
import com.gaia3d.basic.types.HalfEdgeType;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.Math.abs;

@Slf4j
public class TileMatrix {
    private final TilesRange tilesRange;
    private final List<List<TileWgs84>> tilesMatrixRowCol = new ArrayList<>();
    public TileWgs84Manager manager = null;
    double vertexCoincidentError = 0.0000000000001;
    // the tilesMatrixRowCol is a matrix of tiles
    // all the arrays have the same length

    public TileMatrix(TilesRange tilesRange, TileWgs84Manager manager) {
        this.tilesRange = tilesRange;
        this.manager = manager;
    }

    private boolean setTwinHalfEdgeWithHalfEdgesList(GaiaHalfEdge halfEdge, List<GaiaHalfEdge> halfEdgesList, int axisToCheck) {
        // axisToCheck 0 = x axis, 1 = y axis, 2 = both axis
        int halfEdgesList_count = halfEdgesList.size();
        for (int i = 0; i < halfEdgesList_count; i++) {
            GaiaHalfEdge halfEdge2 = halfEdgesList.get(i);

            if (halfEdge2.getTwin() != null) {
                // this halfEdge2 has a twin
                continue;
            }

            if (halfEdge.isHalfEdgePossibleTwin(halfEdge2, vertexCoincidentError, axisToCheck)) {
                // 1rst, must change the startVertex & endVertex of the halfEdge2
                GaiaVertex startVertex = halfEdge.getStartVertex();
                GaiaVertex endVertex = halfEdge.getEndVertex();

                GaiaVertex startVertex2 = halfEdge2.getStartVertex();
                GaiaVertex endVertex2 = halfEdge2.getEndVertex();

                List<GaiaHalfEdge> outingHalfEdges_strVertex2 = startVertex2.getAllOutingHalfEdges();
                List<GaiaHalfEdge> outingHalfEdges_endVertex2 = endVertex2.getAllOutingHalfEdges();

                int outingHalfEdges_strVertex2_count = outingHalfEdges_strVertex2.size();
                for (int j = 0; j < outingHalfEdges_strVertex2_count; j++) {
                    // NOTE : for outingHEdges of startVertex2, must set "startVertex" the endVertex of halfEdge
                    GaiaHalfEdge outingHalfEdge = outingHalfEdges_strVertex2.get(j);
                    outingHalfEdge.setStartVertex(endVertex);
                }

                int outingHalfEdges_endVertex2_count = outingHalfEdges_endVertex2.size();
                for (int j = 0; j < outingHalfEdges_endVertex2_count; j++) {
                    // NOTE : for outingHEdges of endVertex2, must set "startVertex" the startVertex of halfEdge
                    GaiaHalfEdge outingHalfEdge = outingHalfEdges_endVertex2.get(j);
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
        int listHEdges_A_count = listHEdges_A.size();
        for (int i = 0; i < listHEdges_A_count; i++) {
            GaiaHalfEdge halfEdge = listHEdges_A.get(i);
            if (halfEdge.getTwin() != null) {
                // this halfEdge has a twin
                log.info("Error: halfEdge has a twin.");
                continue;
            }

            if (!this.setTwinHalfEdgeWithHalfEdgesList(halfEdge, listHEdges_B, axisToCheck)) {
                // error.!***
                log.info("Error: no twin halfEdge found.");
                

//                if(!this.setTwinHalfEdgeWithHalfEdgesList(halfEdge, listHEdges_B))
//                {
//                      log.info("Error: no twin halfEdge found.");
//                }
            }
        }
    }

    public void makeMatrixMesh(boolean is1rstGeneration) throws TransformException, IOException {
        TileIndices tileIndices = new TileIndices();

        boolean originIsLeftUp = this.manager.isOriginIsLeftUp();

        // 1rst, load or create all the of the matrix
        // Must load from mintile-1 to maxtile+1
        tilesMatrixRowCol.clear();
        int minTileX = tilesRange.minTileX - 1;
        int maxTileX = tilesRange.maxTileX + 1;
        int minTileY = tilesRange.minTileY - 1;
        int maxTileY = tilesRange.maxTileY + 1;
        // Note : the minTileX, minTileY, maxTileX, maxTileY are no necessary to verify if the values are out of the limits
        // It is verified in the TileWgs84Manager

        int totalTiles = (maxTileX - minTileX + 1) * (maxTileY - minTileY + 1);

        int counter = 0;
        int counterAux = 0;
        for (int Y = minTileY; Y <= maxTileY; Y++) {
            List<TileWgs84> tilesListRow = new ArrayList<>();
            for (int X = minTileX; X <= maxTileX; X++) {
                tileIndices.set(X, Y, tilesRange.tileDepth);
                TileWgs84 tile = null;
                if (is1rstGeneration) {
                    tile = this.manager.loadOrCreateTileWgs84(tileIndices);
                } else {
                    tile = this.manager.loadTileWgs84(tileIndices);
                }
                if (counter >= 100) {
                    counter = 0;
                    log.debug("Loading tile L : " + tileIndices.L + ", i : " + counterAux + " / " + totalTiles);
                }

                tilesListRow.add(tile);

                counter++;
                counterAux++;
            }
            tilesMatrixRowCol.add(tilesListRow);
        }


        int rowsCount = tilesMatrixRowCol.size();
        int colsCount = tilesMatrixRowCol.get(0).size();
        log.debug("making tile-matrix : columns : " + colsCount + " rows : " + rowsCount);

        List<GaiaMesh> rowMeshesList = new ArrayList<>(); // each mesh is a row
        int axisToCheck = 1;
        for (int i = 0; i < rowsCount; i++) {
            List<TileWgs84> rowTilesArray = tilesMatrixRowCol.get(i);
            GaiaMesh rowMesh = null;

            for (int j = 0; j < colsCount; j++) {
                TileWgs84 tile = rowTilesArray.get(j);
                if (tile != null) {
                    GaiaMesh tileMesh = tile.mesh;
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
                        List<GaiaHalfEdge> rowMesh_right_halfEdges = rowMesh.getHalfEdgesByType(HalfEdgeType.RIGHT);
                        List<GaiaHalfEdge> tileMesh_left_halfEdges = tileMesh.getHalfEdgesByType(HalfEdgeType.LEFT);

                        if (rowMesh_right_halfEdges.size() > 0)// the c_tile can be null
                        {
                            // now, set twins of halfEdges
                            this.setTwinsBetweenHalfEdges(rowMesh_right_halfEdges, tileMesh_left_halfEdges, axisToCheck);

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
        for (int i = 0; i < rowMeshesList.size(); i++) {
            GaiaMesh rowMesh = rowMeshesList.get(i);
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
                    List<GaiaHalfEdge> resultMesh_down_halfEdges = resultMesh.getHalfEdgesByType(HalfEdgeType.DOWN);
                    List<GaiaHalfEdge> rowMesh_UP_halfEdges = rowMesh.getHalfEdgesByType(HalfEdgeType.UP);
                    if (!resultMesh_down_halfEdges.isEmpty())// the c_tile can be null
                    {
                        // now, set twins of halfEdges
                        this.setTwinsBetweenHalfEdges(resultMesh_down_halfEdges, rowMesh_UP_halfEdges, axisToCheck);

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
                    if (!resultMesh_up_halfEdges.isEmpty())// the c_tile can be null
                    {
                        // now, set twins of halfEdges
                        this.setTwinsBetweenHalfEdges(resultMesh_up_halfEdges, rowMesh_down_halfEdges, axisToCheck);

                        // now, merge the row mesh to the result mesh.
                        resultMesh.removeDeletedObjects();
                        resultMesh.mergeMesh(rowMesh);
                    }
                }


            }
        }

        log.debug("end making tile-matrix");

        if (resultMesh != null) {
            resultMesh.setObjectsIdInList();

            this.recalculateElevation(resultMesh, tilesRange);

            if (tilesRange.tileDepth == 2) {
                
            }

            this.refineMesh(resultMesh, tilesRange);

            // check if you must calculate normals
            if (this.manager.isCalculateNormals()) {
                resultMesh.calculateNormals();
            }

            // now save the 9 tiles
            List<GaiaMesh> separatedMeshes = new ArrayList<GaiaMesh>();
            this.getSeparatedMeshes(resultMesh, separatedMeshes, originIsLeftUp);

            // save order :
            // 1- saveSeparatedTiles()
            // 2- saveQuantizedMeshes()
            // 3- saveSeparatedChildrenTiles()
            //------------------------------------------
            log.debug("Saving separated tiles...");
            saveSeparatedTiles(separatedMeshes);

            // now save quantizedMeshes
            log.debug("Saving quantized meshes...");
            saveQuantizedMeshes(separatedMeshes);

            // finally save the children tiles
            // note : the children tiles must be the last saved
            log.debug("Saving separated children tiles...");
            saveSeparatedChildrenTiles(separatedMeshes);

        } else {
            log.error("Error: resultMesh is null.");
        }
    }

    public void saveQuantizedMeshes(List<GaiaMesh> separatedMeshes) throws IOException {
        boolean originIsLeftUp = this.manager.isOriginIsLeftUp();
        boolean calculateNormals = this.manager.isCalculateNormals();

        int meshesCount = separatedMeshes.size();
        for (int i = 0; i < meshesCount; i++) {
            GaiaMesh mesh = separatedMeshes.get(i);

            GaiaTriangle triangle = mesh.triangles.get(0); // take the first triangle
            TileIndices tileIndices = triangle.getOwnerTileIndices();

            TileWgs84 tile = new TileWgs84(null, this.manager);
            tile.tileIndices = tileIndices;
            String imageryType = this.manager.getImageryType();
            tile.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, imageryType, originIsLeftUp);
            tile.mesh = mesh;

            QuantizedMeshManager quantizedMeshManager = new QuantizedMeshManager();
            QuantizedMesh quantizedMesh = quantizedMeshManager.getQuantizedMeshFromTile(tile, calculateNormals);
            String tileFullPath = this.manager.getQuantizedMeshTilePath(tileIndices);
            String tileFolderPath = this.manager.getQuantizedMeshTileFolderPath(tileIndices);
            FileUtils.createAllFoldersIfNoExist(tileFolderPath);

//            FileOutputStream fileOutputStream = new FileOutputStream(tileFullPath);
//            LittleEndianDataOutputStream dataOutputStream = new LittleEndianDataOutputStream(fileOutputStream);

            //******************************************************************************************************
            FileOutputStream fileOutputStream = new FileOutputStream(tileFullPath);

            // Crear un BufferedOutputStream para mejorar el rendimiento
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

            // Envolver el BufferedOutputStream en un LittleEndianDataOutputStream
            LittleEndianDataOutputStream dataOutputStream = new LittleEndianDataOutputStream(bufferedOutputStream);

            // save the tile
            quantizedMesh.saveDataOutputStream(dataOutputStream, calculateNormals);

            dataOutputStream.close();
            bufferedOutputStream.close();
            fileOutputStream.close();
        }
    }

    public void saveFile(GaiaMesh mesh, String filePath) throws IOException {
        String foldersPath = FileUtils.removeFileNameFromPath(filePath);
        FileUtils.createAllFoldersIfNoExist(foldersPath);

        File file = new File(filePath);
        FileOutputStream fileOutputStream = new FileOutputStream(file);

        // Crear un BufferedOutputStream para mejorar el rendimiento
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

        // Envolver el BufferedOutputStream en un LittleEndianDataOutputStream
        BigEndianDataOutputStream dataOutputStream = new BigEndianDataOutputStream(bufferedOutputStream);

        // delete the file if exists before save
        //FileUtils.deleteFileIfExists(filePath);

        // save the tile
        mesh.saveDataOutputStream(dataOutputStream);

        dataOutputStream.close();
        bufferedOutputStream.close();
        fileOutputStream.close();
    }

    public boolean saveSeparatedTiles(List<GaiaMesh> separatedMeshes) {
        int meshesCount = separatedMeshes.size();
        int counter = 0;
        for (int i = 0; i < meshesCount; i++) {

            GaiaMesh mesh = separatedMeshes.get(i);

//            // Test check meshes
//            if(!TileWgs84Utils.checkTile_test(mesh, this.manager.vertexCoincidentError, this.manager.originIsLeftUp))
//            {
//                log.info("Error: mesh is not valid");
//            }

            GaiaTriangle triangle = mesh.triangles.get(0); // take the first triangle
            TileIndices tileIndices = triangle.getOwnerTileIndices();
            String tileTempDirectory = this.manager.getTileTempDirectory();
            String outputDirectory = this.manager.getOutputDirectory();
            String tileFilePath = TileWgs84Utils.getTileFilePath(tileIndices.X, tileIndices.Y, tileIndices.L);
            String tileFullPath = tileTempDirectory + File.separator + tileFilePath;

            if (counter >= 100) {
                counter = 0;
                log.debug("Saving separated tiles... L : " + tileIndices.L + " i : " + i + " / " + meshesCount);
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
//            String tileTempDirectory = this.manager.tileTempDirectory;
//            String outputDirectory = this.manager.outputDirectory;
//            String tileFilePath = TileWgs84Utils.getTileFilePath(tileIndices.X, tileIndices.Y, tileIndices.L);
//            String tileFullPath = tileTempDirectory + File.separator + tileFilePath;

            if (counter >= 100) {
                counter = 0;
                log.debug("Saving children tiles... L : " + tileIndices.L + " i : " + i + " / " + meshesCount);
            }

            // Save children if necessary************************************************************************
            if (tileIndices.L < this.manager.getMaxTileDepth()) {
                // 1rst, mark triangles with the children tile indices
                boolean originIsLeftUp = this.manager.isOriginIsLeftUp();
                String imageryType = this.manager.getImageryType();

                // 2- make the 4 children
                TileIndices child_LU_TileIndices = tileIndices.getChild_LU_TileIndices(originIsLeftUp);
                TileIndices child_RU_TileIndices = tileIndices.getChild_RU_TileIndices(originIsLeftUp);
                TileIndices child_LD_TileIndices = tileIndices.getChild_LD_TileIndices(originIsLeftUp);
                TileIndices child_RD_TileIndices = tileIndices.getChild_RD_TileIndices(originIsLeftUp);

                // 1rst, classify the triangles of the tile
                GeographicExtension geoExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, imageryType, originIsLeftUp);
                double midLonDeg = geoExtension.getMidLongitudeDeg();
                double midLatDeg = geoExtension.getMidLatitudeDeg();
                List<GaiaTriangle> triangles = mesh.triangles;
                int trianglesCount = triangles.size();
                for (int j = 0; j < trianglesCount; j++) {
                    triangle = triangles.get(j);

                    if (triangle.getObjectStatus() == GaiaObjectStatus.DELETED) {
                        continue;
                    }

                    Vector3d barycenter = triangle.getBarycenter();
                    if (barycenter.x < midLonDeg) {
                        if (barycenter.y < midLatDeg) {
                            // LD_Tile
                            triangle.setOwnerTileIndices(child_LD_TileIndices);
                        } else {
                            // LU_Tile
                            triangle.setOwnerTileIndices(child_LU_TileIndices);
                        }
                    } else {
                        if (barycenter.y < midLatDeg) {
                            // RD_Tile
                            triangle.setOwnerTileIndices(child_RD_TileIndices);
                        } else {
                            // RU_Tile
                            triangle.setOwnerTileIndices(child_RU_TileIndices);
                        }
                    }
                }

                List<GaiaMesh> childMeshes = new ArrayList<>();
                this.getSeparatedMeshes(mesh, childMeshes, this.manager.isOriginIsLeftUp());

                // 3- save the 4 children
                int childMeshesCount = childMeshes.size();
                for (int j = 0; j < childMeshesCount; j++) {
                    GaiaMesh childMesh = childMeshes.get(j);
                    triangle = childMesh.triangles.get(0); // take the first triangle
                    TileIndices childTileIndices = triangle.getOwnerTileIndices();
                    String tileTempDirectory = this.manager.getTileTempDirectory();
                    String outputDirectory = this.manager.getOutputDirectory();
                    String childTileFilePath = TileWgs84Utils.getTileFilePath(childTileIndices.X, childTileIndices.Y, childTileIndices.L);
                    String childTileFullPath = tileTempDirectory + File.separator + childTileFilePath;

                    try {
                        //log.debug("Saving children tiles... L : " + childTileIndices.L + " i : " + j + " / " + childMeshesCount);
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

    private List<GaiaHalfEdge> getHalfEdgesOfTriangles(List<GaiaTriangle> triangles) {
        List<GaiaHalfEdge> resultHalfEdges = new ArrayList<>();
        List<GaiaHalfEdge> halfEdgesLoop = new ArrayList<>();
        int triangles_count = triangles.size();
        for (GaiaTriangle triangle : triangles) {
            triangle.getHalfEdge().getHalfEdgesLoop(halfEdgesLoop);
            resultHalfEdges.addAll(halfEdgesLoop);
            halfEdgesLoop.clear();
        }
        return resultHalfEdges;
    }

    private List<GaiaVertex> getVerticesOfTriangles(List<GaiaTriangle> triangles) {
        List<GaiaVertex> resultVertices = new ArrayList<>();
        HashMap<GaiaVertex, Integer> map_vertices = new HashMap<>();
        int triangles_count = triangles.size();
        for (GaiaTriangle triangle : triangles) {
            List<GaiaVertex> vertices = triangle.getVertices();
            int vertices_count = vertices.size();
            for (GaiaVertex vertex : vertices) {
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
        int triangles_count = triangles.size();
        for (int i = 0; i < triangles_count; i++) {
            GaiaTriangle triangle = triangles.get(i);
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
            TileIndices L_tileIndices = tileIndices.get_L_TileIndices(originIsLeftUp);
            TileIndices R_tileIndices = tileIndices.get_R_TileIndices(originIsLeftUp);
            TileIndices U_tileIndices = tileIndices.get_U_TileIndices(originIsLeftUp);
            TileIndices D_tileIndices = tileIndices.get_D_TileIndices(originIsLeftUp);

            //GaiaBoundingBox bbox = this.getBBoxOfTriangles(trianglesList);
            List<GaiaHalfEdge> halfEdges = this.getHalfEdgesOfTriangles(trianglesList);
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

        HashMap<GaiaVertex, GaiaVertex> mapVertices = new HashMap<GaiaVertex, GaiaVertex>();
        for (GaiaTriangle triangle : triangles) {
            List<GaiaVertex> vertices = triangle.getVertices();
            for (GaiaVertex vertex : vertices) {
                mapVertices.put(vertex, vertex);
            }
        }

        // now make vertices from the hashMap
        List<GaiaVertex> verticesOfCurrentTile = new ArrayList<>(mapVertices.values());
        TerrainElevationDataManager terrainElevationDataManager = this.manager.getTerrainElevationDataManager();

        int verticesCount = verticesOfCurrentTile.size();
        log.debug("recalculating elevations... vertices count : " + verticesCount);
        for (int i = 0; i < verticesCount; i++) {
            GaiaVertex vertex = verticesOfCurrentTile.get(i);
            double elevation = terrainElevationDataManager.getElevation(vertex.getPosition().x, vertex.getPosition().y, this.manager.getMemSaveTerrainElevDataArray());
            if (abs(elevation - vertex.getPosition().z) > 50.0) {
                
            }

            vertex.getPosition().z = elevation;
        }
    }

    public boolean mustRefineTriangle(GaiaTriangle triangle) throws TransformException, IOException {
        if (triangle.isRefineChecked()) {
            return false;
        }

        TerrainElevationDataManager terrainElevationDataManager = this.manager.getTerrainElevationDataManager();
        TileIndices tileIndices = triangle.getOwnerTileIndices();

        // check if the triangle must be refined
        GaiaBoundingBox bboxTriangle = triangle.getBoundingBox();
        double bboxMaxLength = bboxTriangle.getLongestDistanceXY();
        double equatorialRadius = GlobeUtils.getEquatorialRadius();
        double bboxMaxLengthInMeters = Math.toRadians(bboxMaxLength) * equatorialRadius;

        int currL = triangle.getOwnerTileIndices().L;

        double tileSize = TileWgs84Utils.getTileSizeInMetersByDepth(currL);
        double scale = bboxMaxLengthInMeters / tileSize;

        // Y = 0.8X + 0.2.
        scale = 0.8 * scale + 0.2;

        double maxDiff = this.manager.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(triangle.getOwnerTileIndices().L);
        maxDiff *= scale; // scale the maxDiff

        TileWgs84Raster tileRaster = terrainElevationDataManager.getTileWgs84Raster(tileIndices, this.manager);


        // if the triangle size is very small, then do not refine**********************
        // Calculate the maxLength of the triangle in meters
        double triangleMaxLengthMeters = triangle.getTriangleMaxSizeInMeters();
        double minTriangleSizeForDepth = this.manager.getMinTriangleSizeForTileDepth(triangle.getOwnerTileIndices().L);


        if (triangleMaxLengthMeters < minTriangleSizeForDepth) {
            triangle.setRefineChecked(true);
            return false;
        }


        double maxTriangleSizeForDepth = this.manager.getMaxTriangleSizeForTileDepth(triangle.getOwnerTileIndices().L);
        if (triangleMaxLengthMeters > maxTriangleSizeForDepth) {
            return true;
        }

        // check if the triangle intersects the terrainData
        GeographicExtension rootGeographicExtension = terrainElevationDataManager.getRootGeographicExtension();
        if (!rootGeographicExtension.intersectsBBox(bboxTriangle.getMinX(), bboxTriangle.getMinY(), bboxTriangle.getMaxX(), bboxTriangle.getMaxY())) {
            // Need check only the 3 vertex of the triangle
            List<GaiaVertex> vertices = triangle.getVertices();
            int verticesCount = vertices.size();
            for (GaiaVertex vertex : vertices) {
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
        if (tileIndices.L > 10) {

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
        GaiaPlane plane = triangle.getPlane();
        Vector3d barycenter = triangle.getBarycenter();
        int colIdx = tileRaster.getColumn(barycenter.x);
        int rowIdx = tileRaster.getRow(barycenter.y);
        double baricenterLonDeg = tileRaster.getLonDeg(colIdx);
        double baricenterLatDeg = tileRaster.getLatDeg(rowIdx);

        double elevation = tileRaster.getElevation(colIdx, rowIdx);
        double planeElevation = plane.getValueZ(baricenterLonDeg, baricenterLatDeg);

        double distToPlane = abs(elevation - planeElevation) * cosAng;

        if (distToPlane > maxDiff) {
            log.debug("Filtered by Baricenter : L : " + tileIndices.L + " # col : " + colIdx + " # row : " + rowIdx + " # distToPlane : " + distToPlane + " # maxDiff : " + maxDiff);
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
        List<GaiaHalfEdge> memSave_hedges = new ArrayList<>();
        GaiaLine2D memSave_line = new GaiaLine2D();

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
                if (distToPlane > maxDiff) // original
                {
                    intersects = triangle.intersectsPointXY(pos_x, pos_y, memSave_hedges, memSave_line);

                    if (!intersects) {
                        continue;
                    }

                    log.debug("Filtered by RasterTile : L : " + tileIndices.L + " # col : " + col + " / " + colsCount + " # row : " + row + " / " + rowsCount + " # cosAng : " + cosAng + " # distToPlane : " + distToPlane + " # maxDiff : " + maxDiff);


                    memSave_hedges.clear();
                    memSave_line.deleteObjects();
                    return true;
                }
                rowAux++;
            }
            colAux++;
        }

        memSave_hedges.clear();
        memSave_line.deleteObjects();

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
        log.debug("Refinement : Triangles count : " + trianglesCount);
        splitCount = 0;
        for (int i = 0; i < trianglesCount; i++) {
            GaiaTriangle triangle = mesh.triangles.get(i);

            if (triangle.getObjectStatus() == GaiaObjectStatus.DELETED) {
                continue;
            }

            if (!tilesRange.intersects(triangle.getOwnerTileIndices())) {
                continue;
            }

//            if(tilesRange.tileDepth >= 16) {
//
//                log.info("Refinement : L : " + tilesRange.tileDepth + " # i : " + i + " / " + trianglesCount);
//                if(i==22481)
//                {
//                    
//                }
//            }

            if (mustRefineTriangle(triangle)) {
                this.manager.getMemSaveTrianglesArray().clear();
                mesh.splitTriangle(triangle, this.manager.getTerrainElevationDataManager(), this.manager.getMemSaveTrianglesArray());

                if (!this.manager.getMemSaveTrianglesArray().isEmpty()) {
                    splitCount++;
                    refined = true;
                }

                this.manager.getMemSaveTrianglesArray().clear();
            }

        }

        if (refined) {
            log.debug("Removing deleted objects : splited count : " + splitCount);
            mesh.removeDeletedObjects();
            mesh.setObjectsIdInList();
        }


        return refined;
    }

    public void refineMesh(GaiaMesh mesh, TilesRange tilesRange) throws TransformException, IOException {
        // Inside the mesh, there are triangles of n different tiles
        // Here refine only the triangles of the tiles of TilesRange

        double maxDiff = this.manager.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(tilesRange.tileDepth);
        log.info("RefineMesh : L : " + tilesRange.tileDepth + " # maxDiff(m) : " + maxDiff);

        if (tilesRange.tileDepth >= 16) {
            
        }

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
