package com.gaia3d.terrain.tile;


import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.command.GlobalOptions;
import com.gaia3d.io.LittleEndianDataOutputStream;
import com.gaia3d.quantized.mesh.QuantizedMesh;
import com.gaia3d.quantized.mesh.QuantizedMeshManager;
import com.gaia3d.terrain.structure.*;
import com.gaia3d.terrain.types.TerrainObjectStatus;
import com.gaia3d.terrain.util.MemoryMonitor;
import com.gaia3d.terrain.util.TerrainMeshUtils;
import com.gaia3d.terrain.util.TileWgs84Utils;
import com.gaia3d.util.CelestialBody;
import com.gaia3d.util.FileUtils;
import com.gaia3d.util.GeometryUtils;
import com.gaia3d.util.GlobeUtils;
import lombok.extern.slf4j.Slf4j;
import org.geotools.api.referencing.operation.TransformException;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.abs;

@Slf4j
public class TileMatrix {
    private static final double VERTEX_COINCIDENT_ERROR = 0.0000000000001; // 1e-13

    private static final GlobalOptions globalOptions = GlobalOptions.getInstance();
    private final TileRange tilesRange;
    private final List<List<TileWgs84>> tilesMatrixRowCol = new ArrayList<>();
    public TileWgs84Manager manager = null;
    // the tilesMatrixRowCol is a matrix of tiles
    // all the arrays have the same length
    List<TerrainVertex> listVertices = new ArrayList<>();
    List<TerrainHalfEdge> listHalfEdges = new ArrayList<>();

    public TileMatrix(TileRange tilesRange, TileWgs84Manager manager) {
        this.tilesRange = tilesRange;
        this.manager = manager;
    }

    public void deleteObjects() {
        for (List<TileWgs84> row : tilesMatrixRowCol) {
            for (TileWgs84 tile : row) {
                if (tile != null) {tile.deleteObjects();}
            }
        }

        listVertices.clear();
        listHalfEdges.clear();
    }

    private boolean refineAdjacentTilesVertically(TerrainMesh upMesh, TerrainMesh downMesh, boolean isModify) {
        if(upMesh == null || downMesh == null) {
            return false;
        }

        //  +------------+
        //  |            |
        //  |   tileUp   |
        //  |            |
        //  +------------+
        //  |            |
        //  |  tileDown  |
        //  |            |
        //  +------------+

        // The downSide of tileUp must have the same vertices count than the upSide of tileDown, and must be coincident.

        List<TerrainVertex> upTileDownVertices = upMesh.getDownVerticesSortedLeftToRight();
        List<TerrainVertex> downTileUpVertices = downMesh.getUpVerticesSortedRightToLeft();

        if(upTileDownVertices.size() != downTileUpVertices.size()) {
            // For each vertex of the upTile, check if is coincident with a vertex or hedge of the downTile.
            // If a vertex of upTile is coincident with a hedge of downTile, then must refine the triangle of the hedge of the downTile.
            int splitCountUp = 0;
            int splitCountDown = 0;

            splitCountUp = refineTileOneSide(upMesh, downTileUpVertices, CardinalDirection.SOUTH, isModify);
            splitCountDown = refineTileOneSide(downMesh, upTileDownVertices, CardinalDirection.NORTH, isModify);

            return true;
        }

        return false;
    }

    private boolean refineAdjacentTilesHorizontally(TerrainMesh leftMesh, TerrainMesh rightMesh, boolean isModify) {
        if (leftMesh == null || rightMesh == null) {
            return false;
        }

        //  +----------+----------+
        //  |          |          |
        //  | tileLeft | tileRight|
        //  |          |          |
        //  +----------+----------+

        // The rightSide of the leftTile must twineable with the leftSide of the rightTile,
        // so, must have same number of vertices and be coincident.

        List<TerrainVertex> leftTileRightVertices = leftMesh.getRightVerticesSortedDownToUp();
        List<TerrainVertex> rightTileLeftVertices = rightMesh.getLeftVerticesSortedUpToDown();

        if(leftTileRightVertices.size() != rightTileLeftVertices.size()) {
            // For each vertex of the leftTile, check if is coincident with a vertex or hedge of the rightTile.
            // If a vertex of leftTile is coincident with a hedge of rightTile, then must refine the triangle of the hedge of the rightTile.
            int splitCountLeft = 0;
            int splitCountRight = 0;
            splitCountLeft = refineTileOneSide(leftMesh, rightTileLeftVertices, CardinalDirection.EAST, isModify);
            splitCountRight = refineTileOneSide(rightMesh, leftTileRightVertices, CardinalDirection.WEST, isModify);
            return true;
        }

        return false;
    }

    private int refineTileOneSide(TerrainMesh meshToRefine, List<TerrainVertex> vertices,
                                      CardinalDirection tileSide, boolean isModify){
        double error = 1e-6;
        int verticesCount = vertices.size();
        boolean triangleSplit = false;
        boolean finished = false;
        int counter = 0;
        int splitCount = 0;
        List<TerrainHalfEdge> halfEdgesToRefine = null;
        while(!finished) {
            triangleSplit = false;
            for (int i = 0; i < verticesCount; i++) {
                TerrainVertex vertex = vertices.get(i);
                if(vertex.getObjectStatus() == TerrainObjectStatus.DELETED) {
                    continue;
                }
                if (tileSide == CardinalDirection.SOUTH) {
                    halfEdgesToRefine = meshToRefine.getDownHalfEdgesSortedLeftToRight();
                } else if (tileSide == CardinalDirection.NORTH) {
                    halfEdgesToRefine = meshToRefine.getUpHalfEdgesSortedRightToLeft();
                } else if (tileSide == CardinalDirection.EAST) {
                    halfEdgesToRefine = meshToRefine.getRightHalfEdgesSortedDownToUp();
                } else if (tileSide == CardinalDirection.WEST) {
                    halfEdgesToRefine = meshToRefine.getLeftHalfEdgesSortedUpToDown();
                }

                int hedgesCount = halfEdgesToRefine.size();
                for (int j = 0; j < hedgesCount; j++) {
                    TerrainHalfEdge hEdge = halfEdgesToRefine.get(j);
                    if(hEdge.getObjectStatus() == TerrainObjectStatus.DELETED) {
                        continue;
                    }
                    // 0 = NO_INTERSECTION, 1 = INTERSECTION, 2 = COINCIDENT_WITH_START_VERTEX, 3 = COINCIDENT_WITH_END_VERTEX
                    if (hEdge.intersectsPoint(vertex.getPosition(), error) == 1) {
                        // refine the triangle of the hedge of the rightTile
                        TerrainTriangle triangle = hEdge.getTriangle();
                        try {
                            this.listHalfEdges.clear();
                            meshToRefine.splitTriangle(triangle, this.manager.getTerrainElevationDataManager(),
                                    this.manager.getTriangleList(), this.listHalfEdges, isModify, null);
                            this.listHalfEdges.clear();
                            splitCount++;
                        } catch (Exception e) {
                            log.warn("Refine adjacent tiles horizontally, counter : {}", counter);
                        }

                        meshToRefine.removeDeletedObjects();
                        meshToRefine.setObjectsIdInList();

                        triangleSplit = true;
                        break; // break for this vertex.
                    }
                }
            }

            meshToRefine.removeDeletedObjects();
            meshToRefine.setObjectsIdInList();

            if(!triangleSplit){
                finished = true;
                break;
            }

            counter++;
            if(counter > 100) {
                log.warn("Refine adjacent tiles horizontally, counter : {}", counter);
                break;
            }
        }

        return splitCount;
    }

    private boolean setTwinsBetweenHalfEdgesInverseOrder(List<TerrainHalfEdge> listHEdges_A, List<TerrainHalfEdge> listHEdges_B) {
        if (listHEdges_A.size() != listHEdges_B.size()) {
            log.error("The size of the halfEdges lists are different." + "A_size: " + listHEdges_A.size() + ", B_size: " + listHEdges_B.size());
            return false;
        }

        int countA = listHEdges_A.size();
        int countB = listHEdges_B.size();
        int minCount = Math.min(countA, countB); // Only process edges that exist in both lists

        for (int i = 0; i < minCount; i++) {
            TerrainHalfEdge halfEdge = listHEdges_A.get(i);
            if (halfEdge.getTwin() != null) {
                continue;
            }

            int idxInverse = minCount - i - 1;
            TerrainHalfEdge halfEdge2 = listHEdges_B.get(idxInverse);
            if (halfEdge2.getTwin() != null) {
                continue;
            }

            // set twin
            // First, must change the startVertex & endVertex of the halfEdge2
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
            // Before marking vertices as deleted, ensure no active vertex
            // references half-edges from the deleted vertices
            if (!startVertex2.equals(endVertex)) {
                // Clear outingHEdge reference before marking as DELETED
                // This prevents vertices from having outingHEdge pointing to their own half-edges
                if (startVertex2.getOutingHEdge() != null) {
                    startVertex2.setOutingHEdge(null);
                }
                startVertex2.setObjectStatus(TerrainObjectStatus.DELETED);
            }

            if (!endVertex2.equals(startVertex)) {
                if (endVertex2.getOutingHEdge() != null) {
                    endVertex2.setOutingHEdge(null);
                }
                endVertex2.setObjectStatus(TerrainObjectStatus.DELETED);
            }

            // Ensure the consolidated vertices have valid outingHEdge references
            // After redirecting all half-edges from startVertex2/endVertex2 to startVertex/endVertex,
            // make sure startVertex and endVertex have valid outingHEdge pointers
            if (startVertex.getOutingHEdge() == null ||
                startVertex.getOutingHEdge().getObjectStatus() == TerrainObjectStatus.DELETED) {
                // Find a valid outingHEdge for startVertex from the consolidated edges
                for (TerrainHalfEdge outingHalfEdge : outingHalfEdges_endVertex2) {
                    if (outingHalfEdge.getObjectStatus() != TerrainObjectStatus.DELETED) {
                        startVertex.setOutingHEdge(outingHalfEdge);
                        break;
                    }
                }
            }

            if (endVertex.getOutingHEdge() == null ||
                endVertex.getOutingHEdge().getObjectStatus() == TerrainObjectStatus.DELETED) {
                for (TerrainHalfEdge outingHalfEdge : outingHalfEdges_strVertex2) {
                    if (outingHalfEdge.getObjectStatus() != TerrainObjectStatus.DELETED) {
                        endVertex.setOutingHEdge(outingHalfEdge);
                        break;
                    }
                }
            }

            // Ensure consolidated vertices have valid outingHEdge after fix-up
            // If fix-up failed, try harder to find a valid edge from listHEdges_A

            // Validate startVertex
            if (startVertex.getOutingHEdge() == null ||
                startVertex.getOutingHEdge().getObjectStatus() == TerrainObjectStatus.DELETED) {

                // Try to find a valid outingHEdge from listHEdges_A
                boolean foundValid = false;
                for (TerrainHalfEdge edge : listHEdges_A) {
                    if (edge.getStartVertex() == startVertex &&
                        edge.getObjectStatus() != TerrainObjectStatus.DELETED) {
                        startVertex.setOutingHEdge(edge);
                        foundValid = true;
                        break;
                    }
                }

                if (!foundValid) {
                    log.warn("Failed to find valid outingHEdge for vertex {} after consolidation. " +
                             "Marking as DELETED to prevent topology corruption.", startVertex.getId());
                    startVertex.setOutingHEdge(null);
                    startVertex.setObjectStatus(TerrainObjectStatus.DELETED);
                }
            }

            // Validate endVertex
            if (endVertex.getOutingHEdge() == null ||
                endVertex.getOutingHEdge().getObjectStatus() == TerrainObjectStatus.DELETED) {

                boolean foundValid = false;
                for (TerrainHalfEdge edge : listHEdges_A) {
                    if (edge.getStartVertex() == endVertex &&
                        edge.getObjectStatus() != TerrainObjectStatus.DELETED) {
                        endVertex.setOutingHEdge(edge);
                        foundValid = true;
                        break;
                    }
                }

                if (!foundValid) {
                    log.warn("Failed to find valid outingHEdge for vertex {} after consolidation. " +
                             "Marking as DELETED to prevent topology corruption.", endVertex.getId());
                    endVertex.setOutingHEdge(null);
                    endVertex.setObjectStatus(TerrainObjectStatus.DELETED);
                }
            }
        }

        return true;
    }

    public void makeMatrixMeshModifyMode(boolean isFirstGeneration) throws TransformException, IOException {
        TileIndices tileIndices = new TileIndices();

        boolean originIsLeftUp = this.manager.isOriginIsLeftUp();

        // First, load or create all the of the matrix
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

                // In MODIFY_MODE always load or create the tile file.
                TileWgs84 tile = this.manager.loadOrCreateTileWgs84(tileIndices);
//                if (isFirstGeneration) {
//                    tile = this.manager.loadOrCreateTileWgs84(tileIndices);
//                } else {
//                    tile = this.manager.loadTileWgs84(tileIndices);
//                    if(tile == null) {
//                        int hola = 0;
//                    }
//                }
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
                        List<TerrainHalfEdge> rowMeshRightHalfEdges = rowMesh.getRightHalfEdgesSortedDownToUp();
                        List<TerrainHalfEdge> tileMeshLeftHalfEdges = tileMesh.getLeftHalfEdgesSortedUpToDown();

                        // the c_tile can be null
                        if (!rowMeshRightHalfEdges.isEmpty()) {
                            if(rowMeshRightHalfEdges.size() != tileMeshLeftHalfEdges.size()) {
                                log.warn("The size of the halfEdges lists are different." + "rowMeshRightHalfEdges_size: " + rowMeshRightHalfEdges.size() + ", tileMeshLeftHalfEdges_size: " + tileMeshLeftHalfEdges.size());
                                refineAdjacentTilesHorizontally(rowMesh, tileMesh, globalOptions.isModify());
                            }
                            this.setTwinsBetweenHalfEdgesInverseOrder(rowMeshRightHalfEdges, tileMeshLeftHalfEdges);

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
                    List<TerrainHalfEdge> resultMeshDownHalfEdges = resultMesh.getDownHalfEdgesSortedLeftToRight();
                    List<TerrainHalfEdge> rowMeshUpHalfEdges = rowMesh.getUpHalfEdgesSortedRightToLeft();

                    // the c_tile can be null
                    if (!resultMeshDownHalfEdges.isEmpty()) {
                        // now, set twins of halfEdges
                        if(resultMeshDownHalfEdges.size() != rowMeshUpHalfEdges.size()) {
                            log.warn("The size of the halfEdges lists are different." + "resultMeshDownHalfEdges_size: " + resultMeshDownHalfEdges.size() + ", rowMeshUpHalfEdges_size: " + rowMeshUpHalfEdges.size());
                            refineAdjacentTilesVertically(resultMesh, rowMesh, globalOptions.isModify());
                        }
                        this.setTwinsBetweenHalfEdgesInverseOrder(resultMeshDownHalfEdges, rowMeshUpHalfEdges);
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
                    List<TerrainHalfEdge> resultMeshUpHalfEdges = resultMesh.getUpHalfEdgesSortedRightToLeft();
                    List<TerrainHalfEdge> rowMeshDownHalfEdges = rowMesh.getDownHalfEdgesSortedLeftToRight();

                    // the c_tile can be null
                    if (!resultMeshUpHalfEdges.isEmpty()) {
                        // now, set twins of halfEdges
                        this.setTwinsBetweenHalfEdgesInverseOrder(resultMeshUpHalfEdges, rowMeshDownHalfEdges);
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

            boolean skipNoDataType = true; // true only in MODIFY_MODE.
            // noDataType includes NO_INTERSECTION and INTERSECTION_BUT_NO_DATA.
            this.recalculateElevation(resultMesh, tilesRange, skipNoDataType);
            this.refineMesh(resultMesh, tilesRange);

            // check if you must calculate normals
            if (globalOptions.isCalculateNormalsExtension()) {
                this.listVertices.clear();
                this.listHalfEdges.clear();
                resultMesh.calculateNormals(this.listVertices, this.listHalfEdges);
            }

            // now save the 9 tiles
            List<TerrainMesh> separatedMeshes = new ArrayList<>();
            TerrainMeshUtils.getSeparatedMeshes(resultMesh, separatedMeshes, originIsLeftUp);

            // save order:
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
            if (tilesRange.getTileDepth() < globalOptions.getMaximumTileDepth()) {
                log.debug("Saving Separated children Tiles...");
                saveSeparatedChildrenTiles(separatedMeshes);
            }
        } else {
            log.error("ResultMesh is null.");
        }
    }

    public void makeMatrixMesh(boolean isFirstGeneration) throws TransformException, IOException {
        TileIndices tileIndices = new TileIndices();

        boolean originIsLeftUp = this.manager.isOriginIsLeftUp();

        // First, load or create all the of the matrix
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
                if (isFirstGeneration) {
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
                        List<TerrainHalfEdge> rowMeshRightHalfEdges = rowMesh.getRightHalfEdgesSortedDownToUp();
                        List<TerrainHalfEdge> tileMeshLeftHalfEdges = tileMesh.getLeftHalfEdgesSortedUpToDown();

                        // the c_tile can be null
                        if (!rowMeshRightHalfEdges.isEmpty()) {
//                            if(rowMeshRightHalfEdges.size() != tileMeshLeftHalfEdges.size()) {
//                                log.warn("The size of the halfEdges lists are different." + "rowMeshRightHalfEdges_size: " + rowMeshRightHalfEdges.size() + ", tileMeshLeftHalfEdges_size: " + tileMeshLeftHalfEdges.size());
//                                refineAdjacentTilesHorizontally(rowMesh, tileMesh, globalOptions.isModify());
//                            }
                            this.setTwinsBetweenHalfEdgesInverseOrder(rowMeshRightHalfEdges, tileMeshLeftHalfEdges);

                            // now, merge the left tile mesh to the result mesh.
                            rowMesh.removeDeletedObjects();
                            rowMesh.mergeMesh(tileMesh);

//                            // POST-MERGE VALIDATION: Check and repair topology after horizontal consolidation
//                            int[] healthMetrics = checkTopologyHealth(rowMesh);
//                            int totalVertices = healthMetrics[0];
//                            int corruptedVertices = healthMetrics[1];
//                            int maxEdgeCount = healthMetrics[2];
//
//                            if (corruptedVertices > 0) {
//                                double corruptionRate = (double) corruptedVertices / totalVertices * 100.0;
//
//                                // Check for SEVERE corruption before attempting repair
//                                if (corruptionRate > 20.0 || maxEdgeCount > 50) {
//                                    log.error("[TileConsolidation] SEVERE corruption detected: {}% vertices corrupted " +
//                                            "({}/{}), max edges={}. Consolidation quality too poor.",
//                                            String.format("%.1f", corruptionRate), corruptedVertices, totalVertices, maxEdgeCount);
//                                    // Continue anyway but log severe warning - mesh may still be usable
//                                }
//
//                                log.warn("[TileConsolidation] Horizontal merge: Detected {} corrupted vertices ({:.1f}%). Attempting repair...",
//                                        corruptedVertices, String.format("%.1f", corruptionRate));
//                                int repairedCount = rowMesh.repairMeshTopology();
//                                if (repairedCount < corruptedVertices) {
//                                    log.warn("[TileConsolidation] Horizontal merge: Only repaired {}/{} vertices.",
//                                            repairedCount, corruptedVertices);
//                                } else {
//                                    log.info("[TileConsolidation] Horizontal merge: Successfully repaired all {} corrupted vertices.",
//                                            repairedCount);
//                                }
//                            }
                        }
                    }
                }
            }
            rowMeshesList.add(rowMesh);
        }

        // now, join all the rowMeshes
        TerrainMesh resultMesh = null;
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
                    List<TerrainHalfEdge> resultMeshDownHalfEdges = resultMesh.getDownHalfEdgesSortedLeftToRight();
                    List<TerrainHalfEdge> rowMeshUpHalfEdges = rowMesh.getUpHalfEdgesSortedRightToLeft();

                    // the c_tile can be null
                    if (!resultMeshDownHalfEdges.isEmpty()) {
                        // now, set twins of halfEdges
//                        if(resultMeshDownHalfEdges.size() != rowMeshUpHalfEdges.size()) {
//                            log.warn("The size of the halfEdges lists are different." + "resultMeshDownHalfEdges_size: " + resultMeshDownHalfEdges.size() + ", rowMeshUpHalfEdges_size: " + rowMeshUpHalfEdges.size());
//                            refineAdjacentTilesVertically(resultMesh, rowMesh, globalOptions.isModify());
//                        }
                        this.setTwinsBetweenHalfEdgesInverseOrder(resultMeshDownHalfEdges, rowMeshUpHalfEdges);
                        // now, merge the row mesh to the result mesh.
                        resultMesh.removeDeletedObjects();
                        resultMesh.mergeMesh(rowMesh);

//                        // POST-MERGE VALIDATION: Check and repair topology after vertical consolidation
//                        int[] healthMetrics = checkTopologyHealth(resultMesh);
//                        int totalVertices = healthMetrics[0];
//                        int corruptedVertices = healthMetrics[1];
//                        int maxEdgeCount = healthMetrics[2];
//
//                        if (corruptedVertices > 0) {
//                            double corruptionRate = (double) corruptedVertices / totalVertices * 100.0;
//
//                            // Check for SEVERE corruption before attempting repair
//                            if (corruptionRate > 20.0 || maxEdgeCount > 50) {
//                                log.error("[TileConsolidation] SEVERE corruption detected: {}% vertices corrupted " +
//                                        "({}/{}), max edges={}. Consolidation quality too poor.",
//                                        String.format("%.1f", corruptionRate), corruptedVertices, totalVertices, maxEdgeCount);
//                                // Continue anyway but log severe warning - mesh may still be usable
//                            }
//
//                            log.warn("[TileConsolidation] Vertical merge (down): Detected {} corrupted vertices ({:.1f}%). Attempting repair...",
//                                    corruptedVertices, String.format("%.1f", corruptionRate));
//                            int repairedCount = resultMesh.repairMeshTopology();
//                            if (repairedCount < corruptedVertices) {
//                                log.warn("[TileConsolidation] Vertical merge (down): Only repaired {}/{} vertices.",
//                                        repairedCount, corruptedVertices);
//                            } else {
//                                log.info("[TileConsolidation] Vertical merge (down): Successfully repaired all {} corrupted vertices.",
//                                        repairedCount);
//                            }
//                        }
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
                    List<TerrainHalfEdge> resultMeshUpHalfEdges = resultMesh.getUpHalfEdgesSortedRightToLeft();
                    List<TerrainHalfEdge> rowMeshDownHalfEdges = rowMesh.getDownHalfEdgesSortedLeftToRight();

                    // the c_tile can be null
                    if (!resultMeshUpHalfEdges.isEmpty()) {
                        // now, set twins of halfEdges
                        this.setTwinsBetweenHalfEdgesInverseOrder(resultMeshUpHalfEdges, rowMeshDownHalfEdges);
                        // now, merge the row mesh to the result mesh.
                        resultMesh.removeDeletedObjects();
                        resultMesh.mergeMesh(rowMesh);

//                        // POST-MERGE VALIDATION: Check and repair topology after vertical consolidation
//                        int[] healthMetrics = checkTopologyHealth(resultMesh);
//                        int totalVertices = healthMetrics[0];
//                        int corruptedVertices = healthMetrics[1];
//                        int maxEdgeCount = healthMetrics[2];
//
//                        if (corruptedVertices > 0) {
//                            double corruptionRate = (double) corruptedVertices / totalVertices * 100.0;
//
//                            // Check for SEVERE corruption before attempting repair
//                            if (corruptionRate > 20.0 || maxEdgeCount > 50) {
//                                log.error("[TileConsolidation] SEVERE corruption detected: {}% vertices corrupted " +
//                                        "({}/{}), max edges={}. Consolidation quality too poor.",
//                                        String.format("%.1f", corruptionRate), corruptedVertices, totalVertices, maxEdgeCount);
//                                // Continue anyway but log severe warning - mesh may still be usable
//                            }
//
//                            log.warn("[TileConsolidation] Vertical merge (up): Detected {} corrupted vertices ({:.1f}%). Attempting repair...",
//                                    corruptedVertices, String.format("%.1f", corruptionRate));
//                            int repairedCount = resultMesh.repairMeshTopology();
//                            if (repairedCount < corruptedVertices) {
//                                log.warn("[TileConsolidation] Vertical merge (up): Only repaired {}/{} vertices.",
//                                        repairedCount, corruptedVertices);
//                            } else {
//                                log.info("[TileConsolidation] Vertical merge (up): Successfully repaired all {} corrupted vertices.",
//                                        repairedCount);
//                            }
//                        }
                    }
                }
            }
        }

        log.debug("End making TileMatrix");

        if (resultMesh != null) {
            resultMesh.setObjectsIdInList();

            boolean skipNoDataType = false; // only true in MODIFY_MODE, so, here is false.
            this.recalculateElevation(resultMesh, tilesRange, skipNoDataType);
            this.refineMesh(resultMesh, tilesRange);

            // check if you must calculate normals
            if (globalOptions.isCalculateNormalsExtension()) {
                this.listVertices.clear();
                this.listHalfEdges.clear();
                resultMesh.calculateNormals(this.listVertices, this.listHalfEdges);
            }

            // now save the 9 tiles
            List<TerrainMesh> separatedMeshes = new ArrayList<>();
            TerrainMeshUtils.getSeparatedMeshes(resultMesh, separatedMeshes, originIsLeftUp);

            // save order:
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
            if (tilesRange.getTileDepth() < globalOptions.getMaximumTileDepth()) {
                log.debug("Saving Separated children Tiles...");
                saveSeparatedChildrenTiles(separatedMeshes);
            }
        } else {
            log.error("ResultMesh is null.");
        }
    }

    public void saveQuantizedMeshes(List<TerrainMesh> separatedMeshes) throws IOException {
        boolean originIsLeftUp = this.manager.isOriginIsLeftUp();
        boolean calculateNormals = globalOptions.isCalculateNormalsExtension();

        for (TerrainMesh mesh : separatedMeshes) {
            TerrainTriangle triangle = mesh.triangles.get(0); // take the first triangle
            TileIndices tileIndices = triangle.getOwnerTileIndices();

            TileWgs84 tile = new TileWgs84(null, this.manager);
            tile.setTileIndices(tileIndices);
            String imageryType = this.manager.getImaginaryType();
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
                mesh.saveFile(tileFullPath);
            } catch (IOException e) {
                log.error("Error:", e);
                return false;
            }
            counter++;
        }

        return true;
    }

    private void saveSeparatedChildrenTiles(List<TerrainMesh> separatedMeshes) {
        for (TerrainMesh mesh : separatedMeshes) {
            TerrainMeshUtils.save4ChildrenMeshes(mesh, this.manager, globalOptions);
        }
    }

    public void recalculateElevation(TerrainMesh terrainMesh, TileRange tilesRange, boolean skipNoDataType) throws TransformException, IOException {
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
        byte[] intersectionType = {0}; // 0 = NO_INTERSECTION, 1 = INTERSECTION, 2 = INTERSECTION_BUT_NO_DATA
        // noDataType includes NO_INTERSECTION and INTERSECTION_BUT_NO_DATA.
        for (int i = 0; i < verticesCount; i++) {
            TerrainVertex vertex = verticesOfCurrentTile.get(i);
            Vector3d position = vertex.getPosition();
            //********************************************************************************
            // in ModifyMode, check if the position intersects with any of the geoTiff.
            // In the case of NO intersection, then do nothing.
            //********************************************************************************
            TileWgs84Utils.selectTileIndices(currDepth, position.x, position.y, tileIndicesAux, originIsLeftUp);
            double z = terrainElevationDataManager.getElevationBilinearRasterTile(tileIndicesAux, this.manager, position.x, position.y, intersectionType);
            if(skipNoDataType){
                //********************************************
                // skipNoDataType is true only in MODIFY_MODE.
                //********************************************
                if(intersectionType[0] == 1){
                    // only modify when there are pixel data.***
                    position.z = z;
                }
            } else {
                position.z = z;
            }
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
        double equatorialRadius = GlobalOptions.getInstance().getCelestialBody().getEquatorialRadius();
        double bboxMaxLengthInMeters = Math.toRadians(bboxMaxLength) * equatorialRadius;

        int currL = tileIndices.getL();

        double tileSize = TileWgs84Utils.getTileSizeInMetersByDepth(currL);
        double scale = bboxMaxLengthInMeters / tileSize;

        // Y = 0.8X + 0.2.
        scale = 0.8 * scale + 0.2;

        double maxDiff = this.manager.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(triangle.getOwnerTileIndices().getL());
        maxDiff *= scale; // scale the maxDiff

        TileWgs84Raster tileRaster = terrainElevationDataManager.getTileWgs84Raster(tileIndices, this.manager);

        // if the triangle size is very small, then do not refine**********************
        // Calculate the maxLength of the triangle in meters
        double triangleMaxLengthMeters = Math.toRadians(Math.max(bboxTriangle.getLengthX(), bboxTriangle.getLengthY())) * equatorialRadius;
        double minTriangleSizeForDepth = this.manager.getMinTriangleSizeForTileDepth(currL);

        if (triangleMaxLengthMeters < minTriangleSizeForDepth) {
            triangle.setRefineChecked(true);
            log.debug("Filtered by Min Triangle Size : L : " + tileIndices.getL() + " # triangleMaxLengthMeters : " + triangleMaxLengthMeters + " # minTriangleSizeForDepth : " + minTriangleSizeForDepth);
            return false;
        }

        double maxTriangleSizeForDepth = this.manager.getMaxTriangleSizeForTileDepth(currL);
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
            Vector3d triangleNormalDouble = new Vector3d(triangleNormalWC.x, triangleNormalWC.y, triangleNormalWC.z);
            GeographicExtension geographicExtension = tileRaster.getGeographicExtension();
            Vector3d centerGeoCoord = geographicExtension.getMidPoint();
            CelestialBody body = GlobalOptions.getInstance().getCelestialBody();
            double[] centerCartesian = GlobeUtils.geographicToCartesian(centerGeoCoord.x, centerGeoCoord.y, centerGeoCoord.z, body);
            Vector3d normalAtCartesian = GlobeUtils.normalAtCartesianPoint(centerCartesian[0], centerCartesian[1], centerCartesian[2], body);
            cosAng = (float) GeometryUtils.cosineBetweenUnitaryVectors(triangleNormalDouble.x, triangleNormalDouble.y, triangleNormalDouble.z, normalAtCartesian.x, normalAtCartesian.y, normalAtCartesian.z);
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
        double barycenterLonDeg = tileRaster.getLonDeg(colIdx);
        double barycenterLatDeg = tileRaster.getLatDeg(rowIdx);

        double elevation = tileRaster.getElevation(colIdx, rowIdx);
        double planeElevation = plane.getValueZ(barycenterLonDeg, barycenterLatDeg);

        double distToPlane = abs(elevation - planeElevation) * cosAng;

        if (distToPlane > maxDiff) {
            // is it Barycenter?
            log.debug("Filtered by Barycenter : L : " + tileIndices.getL() + " # col : " + colIdx + " # row : " + rowIdx + " # distToPlane : " + distToPlane + " # maxDiff : " + maxDiff);
            return true;
        }

        // bbox of the triangle in the raster
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

        RasterTriangle rasterTriangle = tileRaster.getRasterTriangle(triangle);
        if (rasterTriangle == null || rasterTriangle.getP1() == null ||
            rasterTriangle.getP2() == null || rasterTriangle.getP3() == null) {
            log.warn("Unable to get valid raster triangle for triangle {}. Skipping refinement check.",
                     triangle.getId());
            triangle.setRefineChecked(true);
            return false;
        }
        Vector2i rasterTriangleP1 = rasterTriangle.getP1();
        Vector2i rasterTriangleP2 = rasterTriangle.getP2();
        Vector2i rasterTriangleP3 = rasterTriangle.getP3();

        // parameters used for the barycentric coordinates
        int deltaYBC = rasterTriangleP2.y - rasterTriangleP3.y;
        int deltaYCA = rasterTriangleP3.y - rasterTriangleP1.y;
        int deltaYAC = rasterTriangleP1.y - rasterTriangleP2.y;
        int deltaXCB = rasterTriangleP3.x - rasterTriangleP2.x;
        int deltaXAC = rasterTriangleP1.x - rasterTriangleP3.x;

        double denominator = deltaYBC * deltaXAC + deltaXCB * deltaYAC;
        if (denominator == 0.0) {
            triangle.setRefineChecked(true);
            return false;
        }
        double inverseDenominator = 1.0 / denominator;

        double startLonDeg = tileRaster.getLonDeg(startCol); // here contains the semiDeltaLonDeg, for the pixel center
        double startLatDeg = tileRaster.getLatDeg(startRow); // here contains the semiDeltaLatDeg, for the pixel center

        double deltaLonDeg = tileRaster.getDeltaLonDeg();
        double deltaLatDeg = tileRaster.getDeltaLatDeg();

        double posX;
        double posY;

        int colAux = 0;
        int rowAux = 0;

        boolean intersects = false;
        for (int col = startCol; col <= endCol; col++) {
            rowAux = 0;
            posX = startLonDeg + colAux * deltaLonDeg;
            int colOffsetFromP3 = col - rasterTriangleP3.x;
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

                // check if the pixel (col, row) intersects the rasterTriangle
                intersects = false;
                int rowOffsetFromP3 = row - rasterTriangleP3.y;
                double alpha = (deltaYBC * colOffsetFromP3 + deltaXCB * rowOffsetFromP3) * inverseDenominator;
                if (alpha < 0 || alpha > 1) {
                    rowAux++;
                    continue;
                }
                double beta = (deltaYCA * colOffsetFromP3 + deltaXAC * rowOffsetFromP3) * inverseDenominator;
                if (beta < 0 || beta > 1) {
                    rowAux++;
                    continue;
                }
                double gamma = 1.0 - alpha - beta;
                if (gamma < 0 || gamma > 1) {
                    rowAux++;
                    continue;
                }

                if (alpha >= 0 && beta >= 0 && gamma >= 0) {
                    intersects = true;
                }

                if (!intersects) {
                    rowAux++;
                    continue;
                }

                posY = startLatDeg + rowAux * deltaLatDeg;

                float elevationFloat = tileRaster.getElevation(col, row);

                planeElevation = plane.getValueZ(posX, posY);

                distToPlane = abs(elevationFloat - planeElevation) * cosAng;
                if (distToPlane > maxDiff) {

//                    this.listVertices.clear();
                    //halfEdges.clear();
//                    intersects = triangle.intersectsPointXY(posX, posY, halfEdges, this.listVertices, line2d);

                    log.debug("Filtered by RasterTile : L : " + tileIndices.getL() + " # col : " + col + " / " + colsCount + " # row : " + row + " / " + rowsCount + " # cosAng : " + cosAng + " # distToPlane : " + distToPlane + " # maxDiff : " + maxDiff);
                    return true;
                }
                rowAux++;
            }
            colAux++;
        }
        triangle.setRefineChecked(true);

        log.debug("Filtered by RasterTile : L : " + tileIndices.getL() + " # col : " + colAux + " / " + colsCount + " # row : " + rowAux + " / " + rowsCount + " # cosAng : " + cosAng + " # distToPlane : " + distToPlane + " # maxDiff : " + maxDiff);
        return false;
    }


    private boolean refineMeshOneIteration(TerrainMesh mesh, TileRange tilesRange) throws TransformException, IOException {
        // Inside the mesh, there are triangles of 9 different tiles
        // Here refine only the triangles of the current tile

        boolean isModify = globalOptions.isModify();

        // refine the mesh
        AtomicBoolean refined = new AtomicBoolean(false);
        AtomicInteger splitCount = new AtomicInteger();
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
                mesh.splitTriangle(triangle, this.manager.getTerrainElevationDataManager(), this.manager.getTriangleList(), this.listHalfEdges, isModify, null);
                this.listHalfEdges.clear();

                if (!this.manager.getTriangleList().isEmpty()) {
                    splitCount.getAndIncrement();
                    refined.set(true);
                }
                this.manager.getTriangleList().clear();
            }
        }

        if (refined.get()) {
            log.debug("Removing deleted Meshes : Splited count : {}", splitCount);
            mesh.removeDeletedObjects();
            mesh.setObjectsIdInList();
        }

        return refined.get();
    }

    /**
     * Check the topology health of the mesh by examining vertex half-edge counts.
     * Detects corrupted vertices that have excessive outgoing half-edges (>10),
     * which indicates topology corruption from infinite splitting.
     *
     * @param mesh The mesh to validate
     * @return An array: [totalActiveVertices, verticesWithExcessiveEdges, maxEdgeCount]
     */
    private int[] checkTopologyHealth(TerrainMesh mesh) {
        int totalVertices = 0;
        int verticesWithExcessiveEdges = 0;
        int maxEdgeCount = 0;
        int criticalVertexId = -1;

        for (TerrainVertex vertex : mesh.vertices) {
            if (vertex.getObjectStatus() == TerrainObjectStatus.DELETED) {
                continue;
            }
            totalVertices++;

            List<TerrainHalfEdge> edges = vertex.getAllOutingHalfEdges();
            int edgeCount = edges.size();

            if (edgeCount > maxEdgeCount) {
                maxEdgeCount = edgeCount;
                criticalVertexId = vertex.getId();
            }

            if (edgeCount > 10) {
                verticesWithExcessiveEdges++;
                if (edgeCount > 15) {
                    log.warn("[TopologyHealth] Critical: Vertex {} has {} half-edges (threshold: 15)",
                            vertex.getId(), edgeCount);
                }
            }
        }

        if (verticesWithExcessiveEdges > 0) {
            double corruptionRate = (double) verticesWithExcessiveEdges / totalVertices * 100.0;
            log.warn("[TopologyHealth] Detected {} vertices with >10 edges out of {} total ({:.2f}%). " +
                    "Max edges: {} (vertex {})",
                    verticesWithExcessiveEdges, totalVertices,
                    String.format("%.2f", corruptionRate), maxEdgeCount, criticalVertexId);
        }

        return new int[]{totalVertices, verticesWithExcessiveEdges, maxEdgeCount};
    }

    public void refineMesh(TerrainMesh mesh, TileRange tilesRange) throws TransformException, IOException {
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
    }

    public void refineMesh_new(TerrainMesh mesh, TileRange tilesRange) throws TransformException, IOException {
        // Inside the mesh, there are triangles of n different tiles
        // Here refine only the triangles of the tiles of TilesRange

        double maxDiff = this.manager.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(tilesRange.getTileDepth());
        log.info("[RefineMesh] Starting refinement: Tile depth={}, Initial triangles={}, MaxDiff(m)={}, Max iterations={}",
                tilesRange.getTileDepth(), mesh.triangles.size(), maxDiff, this.manager.getTriangleRefinementMaxIterations());

        // refine the mesh with convergence detection
        boolean finished = false;
        int splitCount = 0;
        int maxIterations = this.manager.getTriangleRefinementMaxIterations();
        int consecutiveNoProgressCount = 0;
        int previousTriangleCount = mesh.triangles.size();
        final int CONVERGENCE_WINDOW = 7;  // Extended from 3 to 7 iterations

        // Memory monitoring for legitimate mesh growth
        long previousUsedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        final int MEMORY_CHECK_INTERVAL = 1; // Check every iteration due to rapid growth

        // Initial memory check before refinement starts
        MemoryMonitor.MemoryState initialMemState = MemoryMonitor.checkMemory("RefineMesh-Start");
        if (initialMemState.isCritical) {
            log.error("[RefineMesh] CRITICAL memory pressure BEFORE refinement starts: free={}%. " +
                    "Cannot proceed. Increase heap size (-Xmx).",
                    initialMemState.getFormattedPercent());
            return; // Abort refinement
        }

        while (!finished) {
            // MEMORY MONITORING: Check before each iteration to prevent OutOfMemoryError
            if (splitCount % MEMORY_CHECK_INTERVAL == 0) {
                MemoryMonitor.MemoryState memState = MemoryMonitor.checkMemory("RefineMesh-Iter" + splitCount);

                // CRITICAL MEMORY: Stop refinement to prevent crash
                if (memState.isCritical) {
                    int currentTriangleCount = mesh.triangles.size();
                    int currentVertexCount = mesh.vertices.size();

                    MemoryMonitor.MemoryMetrics metrics = new MemoryMonitor.MemoryMetrics(
                            memState.usedMemory, currentTriangleCount, currentVertexCount);

                    log.error("[RefineMesh] STOPPING at iteration {} due to CRITICAL memory pressure: " +
                            "free={}%, triangles={}, vertices={}, " +
                            "memory_per_triangle={}, memory_per_vertex={}. " +
                            "Increase heap size (-Xmx) or reduce refinement intensity to process deeper tiles.",
                            splitCount, memState.getFormattedPercent(),
                            currentTriangleCount, currentVertexCount,
                            metrics.formattedBytesPerTriangle, metrics.formattedBytesPerVertex);

                    finished = true;
                    continue;
                }

                // WARNING: Log trend information to help users understand memory consumption
                if (memState.isWarning && splitCount % 5 == 0) {
                    int currentTriangleCount = mesh.triangles.size();
                    MemoryMonitor.MemoryGrowth growth = MemoryMonitor.calculateGrowth(
                            previousUsedMemory, memState.usedMemory,
                            previousTriangleCount, currentTriangleCount);

                    log.warn("[RefineMesh] Memory warning at iteration {}: free={}%, " +
                            "triangles_added={}, memory_growth={}MB, " +
                            "triangles_total={}, vertices_total={}",
                            splitCount, memState.getFormattedPercent(),
                            growth.triangleDelta, String.format("%.2f", growth.growthRateMB),
                            currentTriangleCount, mesh.vertices.size());
                }

                previousUsedMemory = memState.usedMemory;
            }

            boolean refined = this.refineMeshOneIteration(mesh, tilesRange);

            if (!refined) {
                finished = true;
                log.info("[RefineMesh] Converged naturally after {} iterations", splitCount);
            } else {
                splitCount++;

                // CONVERGENCE DETECTION: Check if we're making progress
                int currentTriangleCount = mesh.triangles.size();
                int trianglesAdded = currentTriangleCount - previousTriangleCount;

                // TOPOLOGY HEALTH CHECK: Validate mesh topology every 3 iterations or when no progress
                if (splitCount % 3 == 0 || trianglesAdded <= 0) {
                    int[] health = checkTopologyHealth(mesh);
                    int totalVertices = health[0];
                    int verticesWithExcessiveEdges = health[1];
                    int maxEdgeCount = health[2];

                    if (totalVertices > 0) {
                        double corruptionRate = (double) verticesWithExcessiveEdges / totalVertices * 100.0;  // Percentage (0-100)

                        // SEVERE CORRUPTION: >5% vertices corrupted OR any vertex >20 edges
                        if (corruptionRate > 5.0 || maxEdgeCount > 20) {
                            log.error("[RefineMesh] SEVERE topology corruption detected at iteration {}: " +
                                    "corruption_rate={}%, max_edges={}. " +
                                    "Stopping refinement to prevent OutOfMemoryError.",
                                    splitCount, String.format("%.2f", corruptionRate), maxEdgeCount);
                            finished = true;
                            continue;
                        }

                        // MODERATE CORRUPTION: 2-5% vertices corrupted
                        if (corruptionRate > 2.0 && corruptionRate <= 5.0) {
                            log.warn("[RefineMesh] MODERATE topology corruption at iteration {}: " +
                                    "corruption_rate={}%, max_edges={}. Watching closely.",
                                    splitCount, String.format("%.2f", corruptionRate), maxEdgeCount);
                        }
                    }
                }

                if (trianglesAdded <= 0) {
                    // No new triangles added despite refinement returning true
                    // This indicates a problem - likely infinite splitting loop
                    consecutiveNoProgressCount++;
                    log.warn("[RefineMesh] Iteration {}/{} added no new triangles (current: {}, previous: {}). " +
                            "Consecutive no-progress count: {}/{}",
                            splitCount, maxIterations, currentTriangleCount, previousTriangleCount,
                            consecutiveNoProgressCount, CONVERGENCE_WINDOW);

                    if (consecutiveNoProgressCount >= CONVERGENCE_WINDOW) {
                        log.error("[RefineMesh] No progress for {} consecutive iterations. " +
                                 "Stopping refinement to prevent infinite loop. " +
                                 "This may indicate topology corruption or deadlock issues.",
                                 CONVERGENCE_WINDOW);
                        finished = true;
                    }
                } else {
                    consecutiveNoProgressCount = 0; // Reset counter on progress
                    log.info("[RefineMesh] Iteration {}/{}: added {} triangles, total {}, vertices {}",
                            splitCount, maxIterations, trianglesAdded, currentTriangleCount, mesh.vertices.size());
                }

                previousTriangleCount = currentTriangleCount;

                if (splitCount >= maxIterations) {
                    log.warn("[RefineMesh] Reached maximum iterations ({}) without full convergence. " +
                            "Final triangle count: {}", maxIterations, currentTriangleCount);
                    finished = true;
                }
            }
        }

        log.info("[RefineMesh] Refinement complete: {} iterations, final triangle count: {}",
                 splitCount, mesh.triangles.size());

        // Log final memory state for debugging and capacity planning
        MemoryMonitor.MemoryState finalMemState = MemoryMonitor.checkMemory("RefineMesh-Complete");
        MemoryMonitor.MemoryMetrics finalMetrics = new MemoryMonitor.MemoryMetrics(
                finalMemState.usedMemory, mesh.triangles.size(), mesh.vertices.size());

        log.info("[RefineMesh] Final memory state: used={}, free={}%, " +
                "avg_memory_per_triangle={}, avg_memory_per_vertex={}",
                finalMemState.getUsedMemoryDisplay(), finalMemState.getFormattedPercent(),
                finalMetrics.formattedBytesPerTriangle, finalMetrics.formattedBytesPerVertex);
    }
}
