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
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.opengis.referencing.operation.TransformException;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.Math.abs;

@Slf4j
public class TileMatrix {
    public TileWgs84Manager manager = null;
    double vertexCoincidentError = 0.0000000000001;
    private final TilesRange tilesRange;
    private final List<List<TileWgs84>> tilesMatrixRowCol = new ArrayList<>();
    // the tilesMatrixRowCol is a matrix of tiles.***
    // all the arrays have the same length.***

    public TileMatrix(TilesRange tilesRange, TileWgs84Manager manager) {
        this.tilesRange = tilesRange;
        this.manager = manager;
    }

    private boolean setTwinHalfEdgeWithHalfEdgesList(GaiaHalfEdge halfEdge, ArrayList<GaiaHalfEdge> halfEdgesList, int axisToCheck) {
        // axisToCheck 0 = x axis, 1 = y axis, 2 = both axis.***
        int halfEdgesList_count = halfEdgesList.size();
        for (int i = 0; i < halfEdgesList_count; i++) {
            GaiaHalfEdge halfEdge2 = halfEdgesList.get(i);

            if (halfEdge2.twin != null) {
                // this halfEdge2 has a twin.***
                continue;
            }

            if (halfEdge.isHalfEdgePossibleTwin(halfEdge2, vertexCoincidentError, axisToCheck)) {
                // 1rst, must change the startVertex & endVertex of the halfEdge2.***
                GaiaVertex startVertex = halfEdge.getStartVertex();
                GaiaVertex endVertex = halfEdge.getEndVertex();

                GaiaVertex startVertex2 = halfEdge2.getStartVertex();
                GaiaVertex endVertex2 = halfEdge2.getEndVertex();

                ArrayList<GaiaHalfEdge> outingHalfEdges_strVertex2 = startVertex2.getAllOutingHalfEdges();
                ArrayList<GaiaHalfEdge> outingHalfEdges_endVertex2 = endVertex2.getAllOutingHalfEdges();

                int outingHalfEdges_strVertex2_count = outingHalfEdges_strVertex2.size();
                for (int j = 0; j < outingHalfEdges_strVertex2_count; j++) {
                    // NOTE : for outingHEdges of startVertex2, must set "startVertex" the endVertex of halfEdge.***
                    GaiaHalfEdge outingHalfEdge = outingHalfEdges_strVertex2.get(j);
                    outingHalfEdge.setStartVertex(endVertex);
                }

                int outingHalfEdges_endVertex2_count = outingHalfEdges_endVertex2.size();
                for (int j = 0; j < outingHalfEdges_endVertex2_count; j++) {
                    // NOTE : for outingHEdges of endVertex2, must set "startVertex" the startVertex of halfEdge.***
                    GaiaHalfEdge outingHalfEdge = outingHalfEdges_endVertex2.get(j);
                    outingHalfEdge.setStartVertex(startVertex);
                }

                // finally set twins.***
                halfEdge.setTwin(halfEdge2);

                // now, set as deleted the startVertex2 & endVertex2.***
                if (!startVertex2.equals(endVertex)) {
                    startVertex2.objectStatus = GaiaObjectStatus.DELETED;
                }

                if (!endVertex2.equals(startVertex)) {
                    endVertex2.objectStatus = GaiaObjectStatus.DELETED;
                }

                return true;
            }
        }
        return false;
    }

    private void setTwinsBetweenHalfEdges(ArrayList<GaiaHalfEdge> listHEdges_A, ArrayList<GaiaHalfEdge> listHEdges_B, int axisToCheck) {
        int listHEdges_A_count = listHEdges_A.size();
        for (int i = 0; i < listHEdges_A_count; i++) {
            GaiaHalfEdge halfEdge = listHEdges_A.get(i);
            if (halfEdge.twin != null) {
                // this halfEdge has a twin.***
                log.info("Error: halfEdge has a twin.");
                continue;
            }

            if (!this.setTwinHalfEdgeWithHalfEdgesList(halfEdge, listHEdges_B, axisToCheck)) {
                // error.!***
                log.info("Error: no twin halfEdge found.");
                int hola = 0;

//                if(!this.setTwinHalfEdgeWithHalfEdgesList(halfEdge, listHEdges_B))
//                {
//                      log.info("Error: no twin halfEdge found.");
//                }
            }
        }
    }

    public void makeMatrixMesh(boolean is1rstGeneration) throws TransformException, IOException {
        TileIndices tileIndices = new TileIndices();

        boolean originIsLeftUp = this.manager.originIsLeftUp;

        // 1rst, load or create all the of the matrix.***
        // Must load from mintile-1 to maxtile+1.***
        tilesMatrixRowCol.clear();
        int minTileX = tilesRange.minTileX - 1;
        int maxTileX = tilesRange.maxTileX + 1;
        int minTileY = tilesRange.minTileY - 1;
        int maxTileY = tilesRange.maxTileY + 1;
        // Note : the minTileX, minTileY, maxTileX, maxTileY are no necessary to verify if the values are out of the limits.***
        // It is verified in the TileWgs84Manager.***

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

        List<GaiaMesh> rowMeshesList = new ArrayList<>(); // each mesh is a row.***
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
                        // merge the tileMesh with the rowMesh.***
                        // set twins between the right HEdges of the rowMesh and the left HEdges of the tileMesh.***
                        ArrayList<GaiaHalfEdge> rowMesh_right_halfEdges = rowMesh.getHalfEdgesByType(HalfEdgeType.RIGHT);
                        ArrayList<GaiaHalfEdge> tileMesh_left_halfEdges = tileMesh.getHalfEdgesByType(HalfEdgeType.LEFT);

                        if (rowMesh_right_halfEdges.size() > 0)// the c_tile can be null.***
                        {
                            // now, set twins of halfEdges.***
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

        // now, join all the rowMeshes.***
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
                    // merge the rowMesh with the resultMesh.***
                    // set twins between the bottom HEdges of the resultMesh and the top HEdges of the rowMesh.***
                    ArrayList<GaiaHalfEdge> resultMesh_down_halfEdges = resultMesh.getHalfEdgesByType(HalfEdgeType.DOWN);
                    ArrayList<GaiaHalfEdge> rowMesh_UP_halfEdges = rowMesh.getHalfEdgesByType(HalfEdgeType.UP);
                    if (!resultMesh_down_halfEdges.isEmpty())// the c_tile can be null.***
                    {
                        // now, set twins of halfEdges.***
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
                    // merge the rowMesh with the resultMesh.***
                    // set twins between the bottom HEdges of the resultMesh and the top HEdges of the rowMesh.***
                    ArrayList<GaiaHalfEdge> resultMesh_up_halfEdges = resultMesh.getHalfEdgesByType(HalfEdgeType.UP);
                    ArrayList<GaiaHalfEdge> rowMesh_down_halfEdges = rowMesh.getHalfEdgesByType(HalfEdgeType.DOWN);
                    if (!resultMesh_up_halfEdges.isEmpty())// the c_tile can be null.***
                    {
                        // now, set twins of halfEdges.***
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

            this.refineMesh(resultMesh, tilesRange);

            // check if you must calculate normals.***
            if(this.manager.calculateNormals)
            {
                resultMesh.calculateNormals();
            }

            // now save the 9 tiles.***
            ArrayList<GaiaMesh> separatedMeshes = new ArrayList<GaiaMesh>();
            this.getSeparatedMeshes(resultMesh, separatedMeshes, originIsLeftUp);

            // save order :
            // 1- saveSeparatedTiles()
            // 2- saveQuantizedMeshes()
            // 3- saveSeparatedChildrenTiles()
            //------------------------------------------
            log.debug("Saving separated tiles...");
            saveSeparatedTiles(separatedMeshes);

            // now save quantizedMeshes.***
            log.debug("Saving quantized meshes...");
            saveQuantizedMeshes(separatedMeshes);

            // finally save the children tiles.***
            // note : the children tiles must be the last saved.***
            log.debug("Saving separated children tiles...");
            saveSeparatedChildrenTiles(separatedMeshes);

        } else {
            log.error("Error: resultMesh is null.");
        }
    }

    public void saveQuantizedMeshes(ArrayList<GaiaMesh> separatedMeshes) throws IOException {
        boolean originIsLeftUp = this.manager.originIsLeftUp;
        boolean calculateNormals = this.manager.calculateNormals;

        int meshesCount = separatedMeshes.size();
        for (int i = 0; i < meshesCount; i++) {
            GaiaMesh mesh = separatedMeshes.get(i);

            GaiaTriangle triangle = mesh.triangles.get(0); // take the first triangle.***
            TileIndices tileIndices = triangle.ownerTile_tileIndices;

            TileWgs84 tile = new TileWgs84(null, this.manager);
            tile.tileIndices = tileIndices;
            String imageryType = this.manager.imageryType;
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
            
            // save the tile.***
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

        // delete the file if exists before save.***
        //FileUtils.deleteFileIfExists(filePath);

        // save the tile.***
        mesh.saveDataOutputStream(dataOutputStream);

        dataOutputStream.close();
        bufferedOutputStream.close();
        fileOutputStream.close();
    }

    public boolean saveSeparatedTiles(ArrayList<GaiaMesh> separatedMeshes) {
        int meshesCount = separatedMeshes.size();
        int counter = 0;
        for (int i = 0; i < meshesCount; i++) {

            GaiaMesh mesh = separatedMeshes.get(i);

//            // Test check meshes.***
//            if(!TileWgs84Utils.checkTile_test(mesh, this.manager.vertexCoincidentError, this.manager.originIsLeftUp))
//            {
//                log.info("Error: mesh is not valid.***");
//            }

            GaiaTriangle triangle = mesh.triangles.get(0); // take the first triangle.***
            TileIndices tileIndices = triangle.ownerTile_tileIndices;
            String tileTempDirectory = this.manager.tileTempDirectory;
            String outputDirectory = this.manager.outputDirectory;
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

    private boolean saveSeparatedChildrenTiles(ArrayList<GaiaMesh> separatedMeshes) {
        int meshesCount = separatedMeshes.size();
        int counter = 0;
        for (int i = 0; i < meshesCount; i++) {

            GaiaMesh mesh = separatedMeshes.get(i);

            GaiaTriangle triangle = mesh.triangles.get(0); // take the first triangle.***
            TileIndices tileIndices = triangle.ownerTile_tileIndices;
//            String tileTempDirectory = this.manager.tileTempDirectory;
//            String outputDirectory = this.manager.outputDirectory;
//            String tileFilePath = TileWgs84Utils.getTileFilePath(tileIndices.X, tileIndices.Y, tileIndices.L);
//            String tileFullPath = tileTempDirectory + File.separator + tileFilePath;

            if (counter >= 100) {
                counter = 0;
                log.debug("Saving children tiles... L : " + tileIndices.L + " i : " + i + " / " + meshesCount);
            }

            // Save children if necessary.***************************************************************************
            if (tileIndices.L < this.manager.maxTileDepth) {
                // 1rst, mark triangles with the children tile indices.***
                boolean originIsLeftUp = this.manager.originIsLeftUp;
                String imageryType = this.manager.imageryType;

                // 2- make the 4 children.***
                TileIndices child_LU_TileIndices = tileIndices.getChild_LU_TileIndices(originIsLeftUp);
                TileIndices child_RU_TileIndices = tileIndices.getChild_RU_TileIndices(originIsLeftUp);
                TileIndices child_LD_TileIndices = tileIndices.getChild_LD_TileIndices(originIsLeftUp);
                TileIndices child_RD_TileIndices = tileIndices.getChild_RD_TileIndices(originIsLeftUp);

                // 1rst, classify the triangles of the tile.***
                GeographicExtension geoExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, imageryType, originIsLeftUp);
                double midLonDeg = geoExtension.getMidLongitudeDeg();
                double midLatDeg = geoExtension.getMidLatitudeDeg();
                ArrayList<GaiaTriangle> triangles = mesh.triangles;
                int trianglesCount = triangles.size();
                for (int j = 0; j < trianglesCount; j++) {
                    triangle = triangles.get(j);

                    if (triangle.objectStatus == GaiaObjectStatus.DELETED) {
                        continue;
                    }

                    Vector3d barycenter = triangle.getBarycenter();
                    if (barycenter.x < midLonDeg) {
                        if (barycenter.y < midLatDeg) {
                            // LD_Tile
                            triangle.ownerTile_tileIndices = child_LD_TileIndices;
                        } else {
                            // LU_Tile
                            triangle.ownerTile_tileIndices = child_LU_TileIndices;
                        }
                    } else {
                        if (barycenter.y < midLatDeg) {
                            // RD_Tile
                            triangle.ownerTile_tileIndices = child_RD_TileIndices;
                        } else {
                            // RU_Tile
                            triangle.ownerTile_tileIndices = child_RU_TileIndices;
                        }
                    }
                }

                ArrayList<GaiaMesh> childMeshes = new ArrayList<>();
                this.getSeparatedMeshes(mesh, childMeshes, this.manager.originIsLeftUp);

                // 3- save the 4 children.***
                int childMeshesCount = childMeshes.size();
                for (int j = 0; j < childMeshesCount; j++) {
                    GaiaMesh childMesh = childMeshes.get(j);
                    triangle = childMesh.triangles.get(0); // take the first triangle.***
                    TileIndices childTileIndices = triangle.ownerTile_tileIndices;
                    String tileTempDirectory = this.manager.tileTempDirectory;
                    String outputDirectory = this.manager.outputDirectory;
                    String childTileFilePath = TileWgs84Utils.getTileFilePath(childTileIndices.X, childTileIndices.Y, childTileIndices.L);
                    String childTileFullPath = tileTempDirectory + File.separator + childTileFilePath;

                    try {
                        log.debug("Saving children tiles... L : " + childTileIndices.L + " i : " + j + " / " + childMeshesCount);
                        saveFile(childMesh, childTileFullPath); // original.***
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

    private ArrayList<GaiaHalfEdge> getHalfEdgesOfTriangles(ArrayList<GaiaTriangle> triangles) {
        ArrayList<GaiaHalfEdge> resultHalfEdges = new ArrayList<GaiaHalfEdge>();
        ArrayList<GaiaHalfEdge> halfEdgesLoop = new ArrayList<>();
        int triangles_count = triangles.size();
        for (int i = 0; i < triangles_count; i++) {
            GaiaTriangle triangle = triangles.get(i);
            triangle.halfEdge.getHalfEdgesLoop(halfEdgesLoop);
            resultHalfEdges.addAll(halfEdgesLoop);
            halfEdgesLoop.clear();
        }
        return resultHalfEdges;
    }

    private ArrayList<GaiaVertex> getVerticesOfTriangles(ArrayList<GaiaTriangle> triangles) {
        ArrayList<GaiaVertex> resultVertices = new ArrayList<GaiaVertex>();
        HashMap<GaiaVertex, Integer> map_vertices = new HashMap<GaiaVertex, Integer>();
        int triangles_count = triangles.size();
        for (int i = 0; i < triangles_count; i++) {
            GaiaTriangle triangle = triangles.get(i);
            ArrayList<GaiaVertex> vertices = triangle.getVertices();
            int vertices_count = vertices.size();
            for (int j = 0; j < vertices_count; j++) {
                GaiaVertex vertex = vertices.get(j);
                if (!map_vertices.containsKey(vertex)) {
                    map_vertices.put(vertex, 1);
                    resultVertices.add(vertex);
                }
            }
        }
        return resultVertices;
    }

    public void getSeparatedMeshes(GaiaMesh bigMesh, ArrayList<GaiaMesh> resultSeparatedMeshes, boolean originIsLeftUp) {
        // separate by ownerTile_tileIndices.***
        ArrayList<GaiaTriangle> triangles = bigMesh.triangles;
        HashMap<String, ArrayList<GaiaTriangle>> map_triangles = new HashMap<String, ArrayList<GaiaTriangle>>();
        int triangles_count = triangles.size();
        for (int i = 0; i < triangles_count; i++) {
            GaiaTriangle triangle = triangles.get(i);
            if (triangle.ownerTile_tileIndices != null) {
                TileIndices tileIndices = triangle.ownerTile_tileIndices;
                String tileIndicesString = tileIndices.getString();
                ArrayList<GaiaTriangle> trianglesList = map_triangles.get(tileIndicesString);
                if (trianglesList == null) {
                    trianglesList = new ArrayList<GaiaTriangle>();
                    map_triangles.put(tileIndicesString, trianglesList);
                }
                trianglesList.add(triangle);
            } else {
                // error.***
                log.info("Error: triangle has not ownerTile_tileIndices.");
            }
        }

        // now, create separated meshes.***
        for (String tileIndicesString : map_triangles.keySet()) {
            ArrayList<GaiaTriangle> trianglesList = map_triangles.get(tileIndicesString);

            GaiaMesh separatedMesh = new GaiaMesh();
            separatedMesh.triangles = trianglesList;
            TileIndices tileIndices = trianglesList.get(0).ownerTile_tileIndices;
            TileIndices L_tileIndices = tileIndices.get_L_TileIndices(originIsLeftUp);
            TileIndices R_tileIndices = tileIndices.get_R_TileIndices(originIsLeftUp);
            TileIndices U_tileIndices = tileIndices.get_U_TileIndices(originIsLeftUp);
            TileIndices D_tileIndices = tileIndices.get_D_TileIndices(originIsLeftUp);

            //GaiaBoundingBox bbox = this.getBBoxOfTriangles(trianglesList);
            ArrayList<GaiaHalfEdge> halfEdges = this.getHalfEdgesOfTriangles(trianglesList);
            // for all HEdges, check the triangle of the twin.***
            // if the triangle of the twin has different ownerTile_tileIndices, then set the twin as null.***
            int halfEdges_count = halfEdges.size();
            for (int i = 0; i < halfEdges_count; i++) {
                GaiaHalfEdge halfEdge = halfEdges.get(i);
                GaiaHalfEdge twin = halfEdge.twin;
                if (twin != null) {
                    GaiaTriangle twins_triangle = twin.triangle;
                    if (twins_triangle != null) {
                        String twins_triangle_tileIndicesString = twins_triangle.ownerTile_tileIndices.getString();
                        if (!twins_triangle_tileIndicesString.equals(tileIndicesString)) {
                            // the twin triangle has different ownerTile_tileIndices.***
                            halfEdge.setTwin(null);
                            twin.setTwin(null);

                            // now, for the hedges, must calculate the hedgeType.***
                            // must know the relative position of the twin triangle's tile.***

                            if (twins_triangle_tileIndicesString.equals(L_tileIndices.getString())) {
                                halfEdge.type = HalfEdgeType.LEFT;
                                twin.type = HalfEdgeType.RIGHT;
                            } else if (twins_triangle_tileIndicesString.equals(R_tileIndices.getString())) {
                                halfEdge.type = HalfEdgeType.RIGHT;
                                twin.type = HalfEdgeType.LEFT;
                            } else if (twins_triangle_tileIndicesString.equals(U_tileIndices.getString())) {
                                halfEdge.type = HalfEdgeType.UP;
                                twin.type = HalfEdgeType.DOWN;
                            } else if (twins_triangle_tileIndicesString.equals(D_tileIndices.getString())) {
                                halfEdge.type = HalfEdgeType.DOWN;
                                twin.type = HalfEdgeType.UP;
                            } else {
                                // error.***
                                int hola = 0;
                            }
                        } else {
                            int hola = 0;
                        }
                    }
                }
            }

            separatedMesh.halfEdges = halfEdges;
            ArrayList<GaiaVertex> vertices = this.getVerticesOfTriangles(trianglesList);
            separatedMesh.vertices = vertices;

            resultSeparatedMeshes.add(separatedMesh);
        }

    }

    public void recalculateElevation(GaiaMesh gaiaMesh, TilesRange tilesRange) throws TransformException, IOException {
        ArrayList<GaiaTriangle> triangles = new ArrayList<>();
        gaiaMesh.getTrianglesByTilesRange(tilesRange, triangles, null);

        if (tilesRange.tileDepth >= 15) {
            int hola = 0;
        }

        HashMap<GaiaVertex, GaiaVertex> mapVertices = new HashMap<GaiaVertex, GaiaVertex>();
        int trianglesCount = triangles.size();
        for (int i = 0; i < trianglesCount; i++) {
            GaiaTriangle triangle = triangles.get(i);
            ArrayList<GaiaVertex> vertices = triangle.getVertices();
            int verticesCount = vertices.size();
            for (int j = 0; j < verticesCount; j++) {
                GaiaVertex vertex = vertices.get(j);
                mapVertices.put(vertex, vertex);
            }
        }

        // now make vertices from the hashMap.***
        ArrayList<GaiaVertex> verticesOfCurrentTile = new ArrayList<>();
        verticesOfCurrentTile.addAll(mapVertices.values());
        TerrainElevationDataManager terrainElevationDataManager = this.manager.terrainElevationDataManager;

        int verticesCount = verticesOfCurrentTile.size();
        log.debug("recalculating elevations... vertices count : " + verticesCount);
        for (int i = 0; i < verticesCount; i++) {
            GaiaVertex vertex = verticesOfCurrentTile.get(i);
            double elevation = terrainElevationDataManager.getElevation(vertex.position.x, vertex.position.y, this.manager.memSave_terrainElevDatasArray);
            if (abs(elevation - vertex.position.z) > 50.0) {
                int hola = 0;
            }

            vertex.position.z = elevation;
        }
    }

    public boolean mustRefineTriangle(GaiaTriangle triangle) throws TransformException, IOException {
        if (triangle.refineChecked) {
            return false;
        }

        TerrainElevationDataManager terrainElevationDataManager = this.manager.terrainElevationDataManager;
        TileIndices tileIndices = triangle.ownerTile_tileIndices;

        // check if the triangle must be refined.***
        GaiaBoundingBox bboxTriangle = triangle.getBoundingBox();
        double bboxMaxLength = bboxTriangle.getLongestDistanceXY();
        double equatorialRadius = GlobeUtils.getEquatorialRadius();
        double bboxMaxLengthInMeters = Math.toRadians(bboxMaxLength) * equatorialRadius;

        int currL = triangle.ownerTile_tileIndices.L;

        double tileSize = TileWgs84Utils.getTileSizeInMetersByDepth(currL);
        double scale = bboxMaxLengthInMeters / tileSize;

        // Y = 0.8X + 0.2.
        scale = 0.9 * scale + 0.1;

        double maxDiff = this.manager.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(triangle.ownerTile_tileIndices.L);
        maxDiff *= scale; // scale the maxDiff.***

        TileWgs84Raster tileRaster = terrainElevationDataManager.getTileWgs84Raster(tileIndices, this.manager);


        // if the triangle size is very small, then do not refine.*************************
        // Calculate the maxLength of the triangle in meters.***
        double triangleMaxLengthMeters = triangle.getTriangleMaxSizeInMeters();
        double minTriangleSizeForDepth = this.manager.getMinTriangleSizeForTileDepth(triangle.ownerTile_tileIndices.L);


        if (triangleMaxLengthMeters < minTriangleSizeForDepth) {
            triangle.refineChecked = true;
            return false;
        }


        double maxTriangleSizeForDepth = this.manager.getMaxTriangleSizeForTileDepth(triangle.ownerTile_tileIndices.L);
        if (triangleMaxLengthMeters > maxTriangleSizeForDepth) {
            return true;
        }

        // check if the triangle intersects the terrainData.***
        GeographicExtension rootGeographicExtension = terrainElevationDataManager.getRootGeographicExtension();
        if (rootGeographicExtension == null) {
            int hola = 0;
        }
        if (!rootGeographicExtension.intersectsBBox(bboxTriangle.getMinX(), bboxTriangle.getMinY(), bboxTriangle.getMaxX(), bboxTriangle.getMaxY())) {
            // Need check only the 3 vertex of the triangle.***
            ArrayList<GaiaVertex> vertices = triangle.getVertices();
            int verticesCount = vertices.size();
            for (int i = 0; i < verticesCount; i++) {
                GaiaVertex vertex = vertices.get(i);
                if (vertex.position.z > maxDiff) {
                    return true;
                }
            }

            return false;
        }

        // check the barycenter of the triangle.***
        Vector3d barycenter = triangle.getBarycenter();
        int colIdx = tileRaster.getColumn(barycenter.x);
        int rowIdx = tileRaster.getRow(barycenter.y);
        double elevation = tileRaster.getElevation(colIdx, rowIdx);
        double planeElevation = barycenter.z;

        if (abs(elevation - planeElevation) > maxDiff) {
            return true;
        }

        // check with tileRaster.***

        if (tileRaster == null) {
            return false;
        }

        int startCol = tileRaster.getColumn(bboxTriangle.getMinX());
        int startRow = tileRaster.getRow(bboxTriangle.getMinY());
        int endCol = tileRaster.getColumn(bboxTriangle.getMaxX());
        int endRow = tileRaster.getRow(bboxTriangle.getMaxY());

        int colsCount = endCol - startCol + 1;
        int rowsCount = endRow - startRow + 1;

        if (colsCount < 4 || rowsCount < 4) {
            triangle.refineChecked = true;
            return false;
        }

        double startLonDeg = bboxTriangle.getMinX();
        double startLatDeg = bboxTriangle.getMinY();
        double widthDeg = bboxTriangle.getLengthX();
        double heightDeg = bboxTriangle.getLengthY();

        double deltaLonDeg = tileRaster.getDeltaLonDeg();
        double deltaLatDeg = tileRaster.getDeltaLatDeg();

        double pos_x;
        double pos_y;
        GaiaPlane plane = triangle.getPlane();
        int colAux = 0;
        int rowAux = 0;
//        startLonDeg += deltaLonDeg * 0.5; // center of the pixel.***
//        startLatDeg += deltaLatDeg * 0.5; // center of the pixel.***
        boolean intersects = false;
        ArrayList<GaiaHalfEdge> memSave_hedges = new ArrayList<GaiaHalfEdge>();
        GaiaLine2D memSave_line = new GaiaLine2D();
        double elevationFromTiff;
        double scaleDiff = 1.0;
        for (int col = startCol; col <= endCol; col++) {
            rowAux = 0;
            pos_x = startLonDeg + colAux * deltaLonDeg;
            for (int row = startRow; row <= endRow; row++) {
                pos_y = startLatDeg + rowAux * deltaLatDeg;

                float elevationFloat = tileRaster.getElevation(col, row);
                //double distToPlane = plane.evaluatePoint(pos_x, pos_y, elevationFloat);

                planeElevation = plane.getValueZ(pos_x, pos_y);

//                if (elevationFloat > planeElevation) {
//                    scaleDiff = 1.0;
//                } else {
//                    scaleDiff = 1.0;
//                }

                if (abs(elevationFloat - planeElevation) > maxDiff) // original.***
                //if (abs(distToPlane) > maxDiff)
                {
                    intersects = triangle.intersectsPointXY(pos_x, pos_y, memSave_hedges, memSave_line);

                    if (!intersects) {
                        continue;
                    }

                    memSave_hedges.clear();
                    memSave_line.deleteObjects();
                    return true;
                }
                rowAux++;
            }
            colAux++;
        }

//        TerrainElevationData terrainElevationData = terrainElevationDataManager.rootTerrainElevationDataQuadTree.getTerrainElevationData(barycenter.x, barycenter.y);
//        Vector2d pixelSizeDeg = this.manager.memSave_pixelSizeDegrees;
//        pixelSizeDeg.set(widthDeg / 256.0, heightDeg / 256.0);
//        if(terrainElevationData != null)
//        {
//            terrainElevationData.getPixelSizeDegree(pixelSizeDeg);
//        }
//
//        double pixelSizeX = Math.max(pixelSizeDeg.x, widthDeg / 256.0);
//        double pixelSizeY = Math.max(pixelSizeDeg.y, heightDeg / 256.0);
//
//        GaiaPlane plane = triangle.getPlane();
//
//        int columnsCount = (int)(widthDeg / pixelSizeX);
//        int rowsCount = (int)(heightDeg / pixelSizeY);
//
//        double bbox_minX = bboxTriangle.getMinX();
//        double bbox_minY = bboxTriangle.getMinY();
//
//        boolean intersects = false;
//        int counter = 0;
//        ArrayList<GaiaHalfEdge> memSave_hedges = new ArrayList<GaiaHalfEdge>();
//        GaiaLine2D memSave_line = new GaiaLine2D();
//        for(int row = 0; row < rowsCount; row++)
//        {
//            double pos_y = bbox_minY + row * pixelSizeY;
//            for(int column = 0; column < columnsCount; column++)
//            {
//                double pos_x = bbox_minX + column * pixelSizeX;
//                intersects = triangle.intersectsPointXY(pos_x, pos_y, memSave_hedges, memSave_line);
//                counter++;
//
//                if(!intersects)
//                {
//                    continue;
//                }
//
//                elevation = terrainElevationDataManager.getElevation(pos_x, pos_y, this.manager.memSave_terrainElevDatasArray);
//                planeElevation = plane.getValueZ(pos_x, pos_y);
//
//                if(elevation > planeElevation)
//                {
//                    if(abs(elevation - planeElevation) > maxDiff)
//                    {
//                        memSave_hedges.clear();
//                        memSave_line.deleteObjects();
//                        return true;
//                    }
//                }
//                else {
//                    if(abs(elevation - planeElevation) > maxDiff)
//                    {
//                        memSave_hedges.clear();
//                        memSave_line.deleteObjects();
//                        return true;
//                    }
//                }
//            }
//        }
//
        memSave_hedges.clear();
        memSave_line.deleteObjects();

        triangle.refineChecked = true;
        return false;
    }

    public boolean mustRefineTriangle_original(GaiaTriangle triangle) throws TransformException, IOException {
        if (triangle.refineChecked) {
            return false;
        }

        TerrainElevationDataManager terrainElevationDataManager = this.manager.terrainElevationDataManager;
        TileIndices tileIndices = triangle.ownerTile_tileIndices;


        // check if the triangle must be refined.***
        GaiaBoundingBox bboxTriangle = triangle.getBoundingBox();

        double widthDeg = bboxTriangle.getLengthX();
        double heightDeg = bboxTriangle.getLengthY();

        double maxDiff = this.manager.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(triangle.ownerTile_tileIndices.L);

        //maxDiff *= 0.5; // TEST : reduce the maxDiff.***

        // fast check.******************************************************************
        // check the barycenter of the triangle.***
        Vector3d barycenter = triangle.getBarycenter();
        double elevation = terrainElevationDataManager.getElevation(barycenter.x, barycenter.y, this.manager.memSave_terrainElevDatasArray);
        double planeElevation = barycenter.z;

        if (abs(elevation - planeElevation) > maxDiff) {
            return true;
        }

        // more fast check.***
        /*
        int numInterpolation = 5;
        ArrayList<Vector3d> perimeterPositions = triangle.getPerimeterPositions(numInterpolation);
        for(Vector3d perimeterPosition : perimeterPositions)
        {
            elevation = terrainElevationData.getElevation(perimeterPosition.x, perimeterPosition.y);
            planeElevation = perimeterPosition.z;

            if(abs(elevation - planeElevation) > maxDiff)
            {
                return true;
            }
        }*/
        // end check the barycenter of the triangle.***************************************


        // if the triangle size is very small, then do not refine.*************************
        // Calculate the maxLength of the triangle in meters.***
        double triangleMaxLegthDeg = Math.max(bboxTriangle.getLengthX(), bboxTriangle.getLengthY());
        double triangleMaxLegthRad = Math.toRadians(triangleMaxLegthDeg);
        double triangleMaxLengthMeters = triangleMaxLegthRad * GlobeUtils.getEquatorialRadius();
        double minTriangleSizeForDepth = this.manager.getMinTriangleSizeForTileDepth(triangle.ownerTile_tileIndices.L);
        if (triangleMaxLengthMeters < minTriangleSizeForDepth) {
            triangle.refineChecked = true;
            return false;
        }

        double maxTriangleSizeForDepth = this.manager.getMaxTriangleSizeForTileDepth(triangle.ownerTile_tileIndices.L);
        if (triangleMaxLengthMeters > maxTriangleSizeForDepth) {
            return true;
        }

        // check if the triangle intersects the terrainData.***
        GeographicExtension rootGeographicExtension = terrainElevationDataManager.getRootGeographicExtension();
        if (rootGeographicExtension == null) {
            int hola = 0;
        }
        if (!rootGeographicExtension.intersectsBBox(bboxTriangle.getMinX(), bboxTriangle.getMinY(), bboxTriangle.getMaxX(), bboxTriangle.getMaxY())) {
            // Need check only the 3 vertex of the triangle.***
            ArrayList<GaiaVertex> vertices = triangle.getVertices();
            int verticesCount = vertices.size();
            for (int i = 0; i < verticesCount; i++) {
                GaiaVertex vertex = vertices.get(i);
                if (vertex.position.z > maxDiff) {
                    return true;
                }
            }

            return false;
        }

        TerrainElevationData terrainElevationData = terrainElevationDataManager.rootTerrainElevationDataQuadTree.getTerrainElevationData(barycenter.x, barycenter.y);
        Vector2d pixelSizeDeg = this.manager.memSave_pixelSizeDegrees;
        pixelSizeDeg.set(widthDeg / 256.0, heightDeg / 256.0);
        if (terrainElevationData != null) {
            terrainElevationData.getPixelSizeDegree(pixelSizeDeg);
        }

        double pixelSizeX = Math.max(pixelSizeDeg.x, widthDeg / 256.0);
        double pixelSizeY = Math.max(pixelSizeDeg.y, heightDeg / 256.0);

        GaiaPlane plane = triangle.getPlane();

        int columnsCount = (int) (widthDeg / pixelSizeX);
        int rowsCount = (int) (heightDeg / pixelSizeY);

        double bbox_minX = bboxTriangle.getMinX();
        double bbox_minY = bboxTriangle.getMinY();

        boolean intersects = false;
        int counter = 0;
        ArrayList<GaiaHalfEdge> memSave_hedges = new ArrayList<GaiaHalfEdge>();
        GaiaLine2D memSave_line = new GaiaLine2D();
        for (int row = 0; row < rowsCount; row++) {
            double pos_y = bbox_minY + row * pixelSizeY;
            for (int column = 0; column < columnsCount; column++) {
                double pos_x = bbox_minX + column * pixelSizeX;
                intersects = triangle.intersectsPointXY(pos_x, pos_y, memSave_hedges, memSave_line);
                counter++;

                if (!intersects) {
                    continue;
                }

                elevation = terrainElevationDataManager.getElevation(pos_x, pos_y, this.manager.memSave_terrainElevDatasArray);
                planeElevation = plane.getValueZ(pos_x, pos_y);

                if (elevation > planeElevation) {
                    if (abs(elevation - planeElevation) > maxDiff) {
                        memSave_hedges.clear();
                        memSave_line.deleteObjects();
                        return true;
                    }
                } else {
                    if (abs(elevation - planeElevation) > maxDiff) {
                        memSave_hedges.clear();
                        memSave_line.deleteObjects();
                        return true;
                    }
                }
            }
        }

        memSave_hedges.clear();
        memSave_line.deleteObjects();

        triangle.refineChecked = true;
        return false;
    }

    private boolean refineMeshOneIteration(GaiaMesh mesh, TilesRange tilesRange) throws TransformException, IOException {
        // Inside the mesh, there are triangles of 9 different tiles.***
        // Here refine only the triangles of the current tile.***

        // refine the mesh.***
        boolean refined = false;
        int splitCount = 0;
        int trianglesCount = mesh.triangles.size();
        log.debug("Refinement : Triangles count : " + trianglesCount);
        splitCount = 0;
        for (int i = 0; i < trianglesCount; i++) {
            GaiaTriangle triangle = mesh.triangles.get(i);

            if (triangle.objectStatus == GaiaObjectStatus.DELETED) {
                continue;
            }

            if (!tilesRange.intersects(triangle.ownerTile_tileIndices)) {
                continue;
            }

//            if(tilesRange.tileDepth >= 16) {
//
//                log.info("Refinement : L : " + tilesRange.tileDepth + " # i : " + i + " / " + trianglesCount);
//                if(i==22481)
//                {
//                    int hola = 0;
//                }
//            }

            if (mustRefineTriangle(triangle)) {
                this.manager.memSave_trianglesArray.clear();
                mesh.splitTriangle(triangle, this.manager.terrainElevationDataManager, this.manager.memSave_trianglesArray);

                if (this.manager.memSave_trianglesArray.size() > 0) {
                    splitCount++;
                    refined = true;
                }

                this.manager.memSave_trianglesArray.clear();
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
        // Inside the mesh, there are triangles of n different tiles.***
        // Here refine only the triangles of the tiles of TilesRange.***

        double maxDiff = this.manager.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(tilesRange.tileDepth);
        log.info("RefineMesh : L : " + tilesRange.tileDepth + " # maxDiff(m) : " + maxDiff);

        if (tilesRange.tileDepth >= 16) {
            int hola = 0;
        }

        // refine the mesh.***
        boolean finished = false;
        int splitCount = 0;
        int maxIterations = this.manager.triangleRefinementMaxIterations;
        while (!finished) {
            if (!this.refineMeshOneIteration(mesh, tilesRange)) {
                finished = true;
            }

            splitCount++;

            if (splitCount >= maxIterations) {
                finished = true;
            }

        }

        this.manager.terrainElevationDataManager.deleteTileRasters();

        int hola = 0;
    }
}
