package com.gaia3d.terrain.tile;

import com.gaia3d.io.BigEndianDataInputStream;
import com.gaia3d.io.BigEndianDataOutputStream;
import com.gaia3d.terrain.structure.*;
import com.gaia3d.terrain.types.TerrainHalfEdgeType;
import com.gaia3d.terrain.types.TerrainObjectStatus;
import com.gaia3d.util.FileUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.opengis.referencing.operation.TransformException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Slf4j
public class TileWgs84 {
    private TileWgs84Manager manager;
    private TileWgs84 parentTile; // if parentTile == null, then this is the root tile.
    private TileIndices tileIndices = null;
    private GeographicExtension geographicExtension = null;
    private TerrainMesh mesh = null;

    // for current tile, create the 8 neighbor tiles.
    //  +----------+----------+----------+
    //  |          |          |          |
    //  | leftUp   |  upTile  | RupTile  |
    //  |  Tile    |          |          |
    //  +----------+----------+----------+
    //  |          |          |          |
    //  | leftTile |curr_Tile |rightTile |
    //  |          |          |          |
    //  +----------+----------+----------+
    //  |          |          |          |
    //  | leftDown | downTile | right    |
    //  |   Tile   |          | DownTile |
    //  +----------+----------+----------+
    private TileWgs84[] neighborTiles = new TileWgs84[8];
    private TileWgs84[] childTiles = new TileWgs84[4];

    private List<TerrainVertex> listVertices = new ArrayList<>();
    private List<TerrainHalfEdge> listHalfEdges = new ArrayList<>();

    public TileWgs84(TileWgs84 parentTile, TileWgs84Manager manager) {
        this.parentTile = parentTile;
        this.manager = manager;
    }

    public void deleteObjects() {
        this.parentTile = null;
        this.tileIndices = null;
        if (this.geographicExtension != null) {
            this.geographicExtension.deleteObjects();
            this.geographicExtension = null;
        }
        if (this.mesh != null) {
            this.mesh.deleteObjects();
            this.mesh = null;
        }
        this.manager = null;
        this.neighborTiles = null;
        this.childTiles = null;
    }

    public void saveFile(TerrainMesh mesh, String filePath) throws IOException {
        String foldersPath = FileUtils.removeFileNameFromPath(filePath);
        FileUtils.createAllFoldersIfNoExist(foldersPath);
        BigEndianDataOutputStream dataOutputStream = new BigEndianDataOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)));

        mesh.saveDataOutputStream(dataOutputStream);
        dataOutputStream.close();
    }

    public void loadFile(String filePath) throws IOException {
        BigEndianDataInputStream dataInputStream = new BigEndianDataInputStream(new BufferedInputStream(new FileInputStream(filePath)));

        this.mesh = new TerrainMesh();
        this.mesh.loadDataInputStream(dataInputStream);
        dataInputStream.close();
    }

    public void createInitialMesh() throws TransformException, IOException {
        // The initial mesh consists in 4 vertex & 2 triangles
        this.mesh = new TerrainMesh();

        TerrainVertex vertexLD = this.mesh.newVertex();
        TerrainVertex vertexRD = this.mesh.newVertex();
        TerrainVertex vertexRU = this.mesh.newVertex();
        TerrainVertex vertexLU = this.mesh.newVertex();

        TerrainElevationDataManager terrainElevationDataManager = this.manager.getTerrainElevationDataManager();

        double minLonDeg = this.geographicExtension.getMinLongitudeDeg();
        double minLatDeg = this.geographicExtension.getMinLatitudeDeg();
        double maxLonDeg = this.geographicExtension.getMaxLongitudeDeg();
        double maxLatDeg = this.geographicExtension.getMaxLatitudeDeg();

        double elevMinLonMinLat = terrainElevationDataManager.getElevationBilinearRasterTile(this.tileIndices, this.manager, minLonDeg, minLatDeg);
        double elevMaxLonMinLat = terrainElevationDataManager.getElevationBilinearRasterTile(this.tileIndices, this.manager, maxLonDeg, minLatDeg);
        double elevMaxLonMaxLat = terrainElevationDataManager.getElevationBilinearRasterTile(this.tileIndices, this.manager, maxLonDeg, maxLatDeg);
        double elevMinLonMaxLat = terrainElevationDataManager.getElevationBilinearRasterTile(this.tileIndices, this.manager, minLonDeg, maxLatDeg);

//        double elevMinLonMinLat = terrainElevationDataManager.getElevation(minLonDeg, minLatDeg, this.manager.getTerrainElevDataList());
//        double elevMaxLonMinLat = terrainElevationDataManager.getElevation(maxLonDeg, minLatDeg, this.manager.getTerrainElevDataList());
//        double elevMaxLonMaxLat = terrainElevationDataManager.getElevation(maxLonDeg, maxLatDeg, this.manager.getTerrainElevDataList());
//        double elevMinLonMaxLat = terrainElevationDataManager.getElevation(minLonDeg, maxLatDeg, this.manager.getTerrainElevDataList());

        vertexLD.setPosition(new Vector3d(minLonDeg, minLatDeg, elevMinLonMinLat));
        vertexRD.setPosition(new Vector3d(maxLonDeg, minLatDeg, elevMaxLonMinLat));
        vertexRU.setPosition(new Vector3d(maxLonDeg, maxLatDeg, elevMaxLonMaxLat));
        vertexLU.setPosition(new Vector3d(minLonDeg, maxLatDeg, elevMinLonMaxLat));

        // create the 2 triangles
        // he = halfEdge
        //   +---------------------+
        //   |        he2        / |
        //   |                 /   |
        //   |     (T2)      /     |
        //   |             /       |
        //   | he3       /         |
        //   |     he1 /       he2 |
        //   |       /he3          |
        //   |     /     (T1)      |
        //   |   /                 |
        //   | /    he1            |
        //   +---------------------+
        // so, the halfEdgeT1V3 is the twin of halfEdgeT2V1

        TerrainTriangle triangle1 = this.mesh.newTriangle();
        TerrainTriangle triangle2 = this.mesh.newTriangle();

        // Triangle 1
        TerrainHalfEdge halfEdgeT1V1 = this.mesh.newHalfEdge();
        TerrainHalfEdge halfEdgeT1V2 = this.mesh.newHalfEdge();
        TerrainHalfEdge halfEdgeT1V3 = this.mesh.newHalfEdge(); // twin of halfEdgeT2V1

        halfEdgeT1V1.setStartVertex(vertexLD);
        halfEdgeT1V1.setType(TerrainHalfEdgeType.DOWN);
        halfEdgeT1V2.setStartVertex(vertexRD);
        halfEdgeT1V2.setType(TerrainHalfEdgeType.RIGHT);
        halfEdgeT1V3.setStartVertex(vertexRU);
        halfEdgeT1V3.setType(TerrainHalfEdgeType.INTERIOR);

        List<TerrainHalfEdge> halfEdges_T1 = new ArrayList<>();
        halfEdges_T1.add(halfEdgeT1V1);
        halfEdges_T1.add(halfEdgeT1V2);
        halfEdges_T1.add(halfEdgeT1V3);

        TerrainHalfEdgeUtils.concatenateHalfEdgesLoop(halfEdges_T1);

        // Triangle 2
        TerrainHalfEdge halfEdgeT2V1 = this.mesh.newHalfEdge(); // twin of halfEdgeT1V3
        TerrainHalfEdge halfEdgeT2V2 = this.mesh.newHalfEdge();
        TerrainHalfEdge halfEdgeT2V3 = this.mesh.newHalfEdge();

        halfEdgeT2V1.setStartVertex(vertexLD);
        halfEdgeT2V1.setType(TerrainHalfEdgeType.INTERIOR);
        halfEdgeT2V2.setStartVertex(vertexRU);
        halfEdgeT2V2.setType(TerrainHalfEdgeType.UP);
        halfEdgeT2V3.setStartVertex(vertexLU);
        halfEdgeT2V3.setType(TerrainHalfEdgeType.LEFT);

        List<TerrainHalfEdge> halfEdgesT2 = new ArrayList<>();
        halfEdgesT2.add(halfEdgeT2V1);
        halfEdgesT2.add(halfEdgeT2V2);
        halfEdgesT2.add(halfEdgeT2V3);

        TerrainHalfEdgeUtils.concatenateHalfEdgesLoop(halfEdgesT2);

        // now set twins
        halfEdgeT1V3.setTwin(halfEdgeT2V1);

        // now set triangles
        triangle1.setHalfEdge(halfEdgeT1V1);
        triangle2.setHalfEdge(halfEdgeT2V1);

        // the 2 triangles have the same ownerTile
        triangle1.getOwnerTileIndices().copyFrom(this.tileIndices);
        triangle2.getOwnerTileIndices().copyFrom(this.tileIndices);

        // now split triangles
        //    +----------+----------+
        //    |  \       |        / |
        //    |    \     |      /   |
        //    |      \   |    /     |
        //    |        \ |  /       |
        //    +----------X----------+
        //    |        / |  \       |
        //    |      /   |    \     |
        //    |    /     |      \   |
        //    |  /       |        \ |
        //    +----------+----------+
        this.refineMeshInitial(this.mesh);

        // now set objects id in list
        this.mesh.setObjectsIdInList();

    }


    private boolean refineMeshOneIterationInitial(TerrainMesh mesh) throws TransformException, IOException {

        // refine big triangles of the mesh
        boolean refined = false;
        int trianglesCount = mesh.triangles.size();
        log.debug("[RefineMesh] Triangles Count : {}", trianglesCount);
        for (int i = 0; i < trianglesCount; i++) {
            TerrainTriangle triangle = mesh.triangles.get(i);

            if (triangle.getObjectStatus() == TerrainObjectStatus.DELETED) {
                continue;
            }

            log.debug("[RefineMesh] FAST-Check : TRIANGLE IS BIG FOR THE TILE DEPTH");
            this.manager.getTriangleList().clear();
            this.listHalfEdges.clear();
            mesh.splitTriangle(triangle, this.manager.getTerrainElevationDataManager(), this.manager.getTriangleList(), this.listHalfEdges);
            this.listHalfEdges.clear();

            if (!this.manager.getTriangleList().isEmpty()) {
                refined = true;
            }
        }

        this.manager.getTriangleList().clear();
        if (refined) {
            mesh.removeDeletedObjects();
            mesh.setObjectsIdInList();
        }

        return refined;
    }

    public void refineMeshInitial(TerrainMesh mesh) throws TransformException, IOException {
        boolean finished = false;
        int splitCount = 0;
        int maxIterations = 3;
        if (this.tileIndices.getL() < 10) {
            maxIterations = 4;
        }

        while (!finished) {
            //log.info("iteration : " + splitCount);
            if (!this.refineMeshOneIterationInitial(mesh)) {
                finished = true;
            }

            splitCount++;

            if (splitCount >= maxIterations) {
                finished = true;
            }
        }
    }

}
