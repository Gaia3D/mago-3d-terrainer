package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.*;
import com.gaia3d.basic.types.HalfEdgeType;
import com.gaia3d.reader.FileUtils;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.util.io.BigEndianDataInputStream;
import com.gaia3d.util.io.BigEndianDataOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.opengis.referencing.operation.TransformException;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.abs;

@Slf4j
public class TileWgs84 {
    public TileWgs84Manager manager = null;

    public TileWgs84 parentTile = null;
    // if parentTile == null, then this is the root tile.
    public TileIndices tileIndices = null;

    public GeographicExtension geographicExtension = null;

    public GaiaMesh mesh = null;

    // for current tile, create the 8 neighbor tiles.
    //  +----------+----------+----------+
    //  |          |          |          |
    //  | LU_Tile  |  U_Tile  | RU_Tile  |
    //  |          |          |          |
    //  +----------+----------+----------+
    //  |          |          |          |
    //  | L_Tile   |curr_Tile | R_Tile   |
    //  |          |          |          |
    //  +----------+----------+----------+
    //  |          |          |          |
    //  | LD_Tile  | D_Tile   | RD_Tile  |
    //  |          |          |          |
    //  +----------+----------+----------+

    public TileWgs84[] neighborTiles = new TileWgs84[8];
    // neighborTiles[0] = LD_Tile
    // neighborTiles[1] = D_Tile
    // neighborTiles[2] = RD_Tile
    // neighborTiles[3] = R_Tile
    // neighborTiles[4] = RU_Tile
    // neighborTiles[5] = U_Tile
    // neighborTiles[6] = LU_Tile
    // neighborTiles[7] = L_Tile


    public TileWgs84[] childTiles = new TileWgs84[4];

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

    public void saveFile(GaiaMesh mesh, String filePath) throws IOException {
        String foldersPath = FileUtils.removeFileNameFromPath(filePath);
        FileUtils.createAllFoldersIfNoExist(foldersPath);


        FileOutputStream fileOutputStream = new FileOutputStream(filePath);

        // Crear un BufferedInputStream para mejorar el rendimiento
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

        // Envolver el BufferedInputStream en un LittleEndianDataInputStream
        BigEndianDataOutputStream dataOutputStream = new BigEndianDataOutputStream(bufferedOutputStream);

        mesh.saveDataOutputStream(dataOutputStream);
        dataOutputStream.close();
        bufferedOutputStream.close();
        fileOutputStream.close();
    }

    /*public void saveFileBigMesh(String filePath, GaiaMesh bigMesh) throws IOException {
        // this is a temp function
        // delete after test
        String foldersPath = FileUtils.removeFileNameFromPath(filePath);
        FileUtils.createAllFoldersIfNoExist(foldersPath);

        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        BigEndianDataOutputStream dataOutputStream = new BigEndianDataOutputStream(new BufferedOutputStream(fileOutputStream));

        // delete the file if exists before save
        FileUtils.deleteFileIfExists(filePath);

        // save the tile
        bigMesh.saveDataOutputStream(dataOutputStream);

        fileOutputStream.close();
    }*/

    public void loadFile(String filePath) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(filePath);

        // Crear un BufferedInputStream para mejorar el rendimiento
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

        // Envolver el BufferedInputStream en un LittleEndianDataInputStream
        BigEndianDataInputStream dataInputStream = new BigEndianDataInputStream(bufferedInputStream);

        this.mesh = new GaiaMesh();
        this.mesh.loadDataInputStream(dataInputStream);
        dataInputStream.close();
        bufferedInputStream.close();
        fileInputStream.close();
    }

    public void createInitialMesh() throws TransformException, IOException {
        // The initial mesh consists in 4 vertex & 2 triangles
        this.mesh = new GaiaMesh();

        GaiaVertex vertexLD = this.mesh.newVertex();
        GaiaVertex vertexRD = this.mesh.newVertex();
        GaiaVertex vertexRU = this.mesh.newVertex();
        GaiaVertex vertexLU = this.mesh.newVertex();

        TerrainElevationDataManager terrainElevationDataManager = this.manager.getTerrainElevationDataManager();

        double minLonDeg = this.geographicExtension.getMinLongitudeDeg();
        double minLatDeg = this.geographicExtension.getMinLatitudeDeg();
        double maxLonDeg = this.geographicExtension.getMaxLongitudeDeg();
        double maxLatDeg = this.geographicExtension.getMaxLatitudeDeg();


        double elevMinLonMinLat = terrainElevationDataManager.getElevation(minLonDeg, minLatDeg, this.manager.getMemSaveTerrainElevDataArray());
        double elevMaxLonMinLat = terrainElevationDataManager.getElevation(maxLonDeg, minLatDeg, this.manager.getMemSaveTerrainElevDataArray());
        double elevMaxLonMaxLat = terrainElevationDataManager.getElevation(maxLonDeg, maxLatDeg, this.manager.getMemSaveTerrainElevDataArray());
        double elevMinLonMaxLat = terrainElevationDataManager.getElevation(minLonDeg, maxLatDeg, this.manager.getMemSaveTerrainElevDataArray());

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
        // so, the halfEdge_T1_3 is the twin of halfEdge_T2_1

        GaiaTriangle triangle1 = this.mesh.newTriangle();
        GaiaTriangle triangle2 = this.mesh.newTriangle();

        // Triangle 1
        GaiaHalfEdge halfEdge_T1_1 = this.mesh.newHalfEdge();
        GaiaHalfEdge halfEdge_T1_2 = this.mesh.newHalfEdge();
        GaiaHalfEdge halfEdge_T1_3 = this.mesh.newHalfEdge(); // twin of halfEdge_T2_1

        halfEdge_T1_1.setStartVertex(vertexLD);
        halfEdge_T1_1.setType(HalfEdgeType.DOWN);
        halfEdge_T1_2.setStartVertex(vertexRD);
        halfEdge_T1_2.setType(HalfEdgeType.RIGHT);
        halfEdge_T1_3.setStartVertex(vertexRU);
        halfEdge_T1_3.setType(HalfEdgeType.INTERIOR);

        List<GaiaHalfEdge> halfEdges_T1 = new ArrayList<GaiaHalfEdge>();
        halfEdges_T1.add(halfEdge_T1_1);
        halfEdges_T1.add(halfEdge_T1_2);
        halfEdges_T1.add(halfEdge_T1_3);

        GaiaHalfEdgeUtils.concatenateHalfEdgesLoop(halfEdges_T1);

        // Triangle 2
        GaiaHalfEdge halfEdge_T2_1 = this.mesh.newHalfEdge(); // twin of halfEdge_T1_3
        GaiaHalfEdge halfEdge_T2_2 = this.mesh.newHalfEdge();
        GaiaHalfEdge halfEdge_T2_3 = this.mesh.newHalfEdge();

        halfEdge_T2_1.setStartVertex(vertexLD);
        halfEdge_T2_1.setType(HalfEdgeType.INTERIOR);
        halfEdge_T2_2.setStartVertex(vertexRU);
        halfEdge_T2_2.setType(HalfEdgeType.UP);
        halfEdge_T2_3.setStartVertex(vertexLU);
        halfEdge_T2_3.setType(HalfEdgeType.LEFT);

        List<GaiaHalfEdge> halfEdges_T2 = new ArrayList<GaiaHalfEdge>();
        halfEdges_T2.add(halfEdge_T2_1);
        halfEdges_T2.add(halfEdge_T2_2);
        halfEdges_T2.add(halfEdge_T2_3);

        GaiaHalfEdgeUtils.concatenateHalfEdgesLoop(halfEdges_T2);

        // now set twins
        halfEdge_T1_3.setTwin(halfEdge_T2_1);

        // now set triangles
        triangle1.setHalfEdge(halfEdge_T1_1);
        triangle2.setHalfEdge(halfEdge_T2_1);

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

    public void createInitialMesh_test() throws TransformException, IOException {
        // The initial mesh consists in 4 vertex & 2 triangles
        this.mesh = new GaiaMesh();

        GaiaVertex vertexLD = this.mesh.newVertex();
        GaiaVertex vertexRD = this.mesh.newVertex();
        GaiaVertex vertexRU = this.mesh.newVertex();
        GaiaVertex vertexLU = this.mesh.newVertex();

        TerrainElevationDataManager terrainElevationDataManager = this.manager.getTerrainElevationDataManager();

        double minLonDeg = this.geographicExtension.getMinLongitudeDeg();
        double minLatDeg = this.geographicExtension.getMinLatitudeDeg();
        double maxLonDeg = this.geographicExtension.getMaxLongitudeDeg();
        double maxLatDeg = this.geographicExtension.getMaxLatitudeDeg();

        double elevMinLonMinLat = terrainElevationDataManager.getElevation(minLonDeg, minLatDeg, this.manager.getMemSaveTerrainElevDataArray());
        double elevMaxLonMinLat = terrainElevationDataManager.getElevation(maxLonDeg, minLatDeg, this.manager.getMemSaveTerrainElevDataArray());
        double elevMaxLonMaxLat = terrainElevationDataManager.getElevation(maxLonDeg, maxLatDeg, this.manager.getMemSaveTerrainElevDataArray());
        double elevMinLonMaxLat = terrainElevationDataManager.getElevation(minLonDeg, maxLatDeg, this.manager.getMemSaveTerrainElevDataArray());

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
        // so, the halfEdge_T1_3 is the twin of halfEdge_T2_1

        GaiaTriangle triangle1 = this.mesh.newTriangle();
        GaiaTriangle triangle2 = this.mesh.newTriangle();

        // Triangle 1
        GaiaHalfEdge halfEdge_T1_1 = this.mesh.newHalfEdge();
        GaiaHalfEdge halfEdge_T1_2 = this.mesh.newHalfEdge();
        GaiaHalfEdge halfEdge_T1_3 = this.mesh.newHalfEdge(); // twin of halfEdge_T2_1

        halfEdge_T1_1.setStartVertex(vertexLD);
        halfEdge_T1_1.setType(HalfEdgeType.DOWN);
        halfEdge_T1_2.setStartVertex(vertexRD);
        halfEdge_T1_2.setType(HalfEdgeType.RIGHT);
        halfEdge_T1_3.setStartVertex(vertexRU);
        halfEdge_T1_3.setType(HalfEdgeType.INTERIOR);

        List<GaiaHalfEdge> halfEdges_T1 = new ArrayList<GaiaHalfEdge>();
        halfEdges_T1.add(halfEdge_T1_1);
        halfEdges_T1.add(halfEdge_T1_2);
        halfEdges_T1.add(halfEdge_T1_3);

        GaiaHalfEdgeUtils.concatenateHalfEdgesLoop(halfEdges_T1);

        // Triangle 2
        GaiaHalfEdge halfEdge_T2_1 = this.mesh.newHalfEdge(); // twin of halfEdge_T1_3
        GaiaHalfEdge halfEdge_T2_2 = this.mesh.newHalfEdge();
        GaiaHalfEdge halfEdge_T2_3 = this.mesh.newHalfEdge();

        halfEdge_T2_1.setStartVertex(vertexLD);
        halfEdge_T2_1.setType(HalfEdgeType.INTERIOR);
        halfEdge_T2_2.setStartVertex(vertexRU);
        halfEdge_T2_2.setType(HalfEdgeType.UP);
        halfEdge_T2_3.setStartVertex(vertexLU);
        halfEdge_T2_3.setType(HalfEdgeType.LEFT);

        List<GaiaHalfEdge> halfEdges_T2 = new ArrayList<GaiaHalfEdge>();
        halfEdges_T2.add(halfEdge_T2_1);
        halfEdges_T2.add(halfEdge_T2_2);
        halfEdges_T2.add(halfEdge_T2_3);

        GaiaHalfEdgeUtils.concatenateHalfEdgesLoop(halfEdges_T2);

        // now set twins
        halfEdge_T1_3.setTwin(halfEdge_T2_1);

        // now set triangles
        triangle1.setHalfEdge(halfEdge_T1_1);
        triangle2.setHalfEdge(halfEdge_T2_1);

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


    public void makeBigMesh(boolean is1rstGeneration) throws IOException, TransformException {

        // make a bigMesh
        // bigMesh:
        //  +----------+----------+----------+
        //  |          |          |          |
        //  | LU_mesh  |  U_mesh  | RU_mesh  |
        //  |          |          |          |
        //  +----------+----------+----------+
        //  |          |          |          |
        //  | L_mesh   |curr_mesh | R_mesh   |
        //  |          |          |          |
        //  +----------+----------+----------+
        //  |          |          |          |
        //  | LD_mesh  | D_mesh   | RD_mesh  |
        //  |          |          |          |
        //  +----------+----------+----------+

        // Note : only in the 1rst generation the tiles must be created
        boolean originIsLeftUp = this.manager.isOriginIsLeftUp();

        TileIndices curr_TileIndices = this.tileIndices;
        TileWgs84 curr_tile = null;
        if (is1rstGeneration) {
            curr_tile = this.manager.loadOrCreateTileWgs84(curr_TileIndices);
        } else {
            curr_tile = this.manager.loadTileWgs84(curr_TileIndices);
        }


        TileIndices LD_TileIndices = this.tileIndices.get_LD_TileIndices(originIsLeftUp);
        TileWgs84 LD_tile = null;
        if (is1rstGeneration) {
            LD_tile = this.manager.loadOrCreateTileWgs84(LD_TileIndices);
        } else {
            LD_tile = this.manager.loadTileWgs84(LD_TileIndices);
        }

        TileIndices D_TileIndices = this.tileIndices.get_D_TileIndices(originIsLeftUp);
        TileWgs84 D_tile = null;
        if (is1rstGeneration) {
            D_tile = this.manager.loadOrCreateTileWgs84(D_TileIndices);
        } else {
            D_tile = this.manager.loadTileWgs84(D_TileIndices);
        }

        TileIndices RD_TileIndices = this.tileIndices.get_RD_TileIndices(originIsLeftUp);
        TileWgs84 RD_tile = null;
        if (is1rstGeneration) {
            RD_tile = this.manager.loadOrCreateTileWgs84(RD_TileIndices);
        } else {
            RD_tile = this.manager.loadTileWgs84(RD_TileIndices);
        }

        TileIndices L_TileIndices = this.tileIndices.get_L_TileIndices(originIsLeftUp);
        TileWgs84 L_tile = null;
        if (is1rstGeneration) {
            L_tile = this.manager.loadOrCreateTileWgs84(L_TileIndices);
        } else {
            L_tile = this.manager.loadTileWgs84(L_TileIndices);
        }

        TileIndices R_TileIndices = this.tileIndices.get_R_TileIndices(originIsLeftUp);
        TileWgs84 R_tile = null;
        if (is1rstGeneration) {
            R_tile = this.manager.loadOrCreateTileWgs84(R_TileIndices);
        } else {
            R_tile = this.manager.loadTileWgs84(R_TileIndices);
        }

        TileIndices LU_TileIndices = this.tileIndices.get_LU_TileIndices(originIsLeftUp);
        TileWgs84 LU_tile = null;
        if (is1rstGeneration) {
            LU_tile = this.manager.loadOrCreateTileWgs84(LU_TileIndices);
        } else {
            LU_tile = this.manager.loadTileWgs84(LU_TileIndices);
        }

        TileIndices U_TileIndices = this.tileIndices.get_U_TileIndices(originIsLeftUp);
        TileWgs84 U_tile = null;
        if (is1rstGeneration) {
            U_tile = this.manager.loadOrCreateTileWgs84(U_TileIndices);
        } else {
            U_tile = this.manager.loadTileWgs84(U_TileIndices);
        }

        TileIndices RU_TileIndices = this.tileIndices.get_RU_TileIndices(originIsLeftUp);
        TileWgs84 RU_tile = null;
        if (is1rstGeneration) {
            RU_tile = this.manager.loadOrCreateTileWgs84(RU_TileIndices);
        } else {
            RU_tile = this.manager.loadTileWgs84(RU_TileIndices);
        }

        // now make the bigMesh
        // public TileMerger3x3(TileWgs84 center_tile, TileWgs84 left_tile, TileWgs84 right_tile,
        //                         TileWgs84 up_tile, TileWgs84 down_tile, TileWgs84 left_up_tile,
        //                         TileWgs84 right_up_tile, TileWgs84 left_down_tile, TileWgs84 right_down_tile)
        TileMerger3x3 tileMerger3x3 = new TileMerger3x3(curr_tile, L_tile, R_tile, U_tile, D_tile, LU_tile, RU_tile, LD_tile, RD_tile);

        GaiaMesh bigMesh = tileMerger3x3.getMergedMesh();
        bigMesh.setObjectsIdInList();

        // 1rst update elevations with the current tile depth geoTiff
        recalculateElevation(bigMesh, curr_TileIndices);

        // Now refine the bigMesh
        refineMesh(bigMesh, curr_TileIndices);

        // now save the 9 tiles
        List<GaiaMesh> separatedMeshes = new ArrayList<GaiaMesh>();
        tileMerger3x3.getSeparatedMeshes(bigMesh, separatedMeshes, originIsLeftUp);
        saveSeparatedTiles(separatedMeshes);

        // provisionally save the bigMesh
        /*
        String tileTempDirectory = this.manager.tileTempDirectory;
        String outputDirectory = this.manager.outputDirectory;
        String bigMeshFilePath = TileWgs84Utils.getTileFileName(curr_TileIndices.X, curr_TileIndices.Y, curr_TileIndices.L) + "bigMesh.til";
        String bigMeshFullPath = tileTempDirectory + File.separator + bigMeshFilePath;
        this.saveFileBigMesh(bigMeshFullPath, bigMesh);
        */

        
    }

    public boolean saveSeparatedTiles(List<GaiaMesh> separatedMeshes) {
        // save the 9 tiles
        int meshesCount = separatedMeshes.size();
        for (int i = 0; i < meshesCount; i++) {
            GaiaMesh mesh = separatedMeshes.get(i);

            // Test check meshes
            if (!TileWgs84Utils.checkTile_test(mesh, this.manager.getVertexCoincidentError(), this.manager.isOriginIsLeftUp())) {
                log.info("Error: mesh is not valid");

            }

            GaiaTriangle triangle = mesh.triangles.get(0); // take the first triangle
            TileIndices tileIndices = triangle.getOwnerTileIndices();
            String tileTempDirectory = this.manager.getTileTempDirectory();
            String outputDirectory = this.manager.getOutputDirectory();
            String tileFilePath = TileWgs84Utils.getTileFilePath(tileIndices.X, tileIndices.Y, tileIndices.L);
            String tileFullPath = tileTempDirectory + File.separator + tileFilePath;

            try {
                saveFile(mesh, tileFullPath);

            } catch (IOException e) {
                log.error(e.getMessage());
                return false;
            }
        }

        return true;
    }

    private boolean triangleIntersectsLongitudeOrLatitude(GaiaTriangle triangle, double midLonDeg, double midLatDeg) {
        List<GaiaVertex> vertices = triangle.getVertices();
        int verticesCount = vertices.size();
        int vertexLeftSideCount = 0;
        int vertexRightSideCount = 0;
        int vertexUpsideCount = 0;
        int vertexDownsideCount = 0;
        int vertexCoincidentCount = 0;
        double vertexCoincidentError = this.manager.getVertexCoincidentError();

        for (int i = 0; i < verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            double diffX = Math.abs(vertex.getPosition().x - midLonDeg);
            double diffY = Math.abs(vertex.getPosition().y - midLatDeg);
            if (diffX < vertexCoincidentError || diffY < vertexCoincidentError) {
                // the vertex is coincident with midLon or midLat
                vertexCoincidentCount++;
                continue;
            }

            if (vertexCoincidentCount > 1) {
                return false;
            }

            if (vertex.getPosition().x < midLonDeg) {
                vertexLeftSideCount++;
            } else {
                vertexRightSideCount++;
            }

            if (vertex.getPosition().y < midLatDeg) {
                vertexDownsideCount++;
            } else {
                vertexUpsideCount++;
            }
        }

        if (vertexLeftSideCount > 0 && vertexRightSideCount > 0) {
            return true;
        }

        return vertexUpsideCount > 0 && vertexDownsideCount > 0;
    }

    public boolean mustRefineTriangle(GaiaTriangle triangle) throws TransformException, IOException {
        if (triangle.isRefineChecked()) {
            log.debug("SUPER-FAST-Check : false");
            return false;
        }

        TerrainElevationDataManager terrainElevationDataManager = this.manager.getTerrainElevationDataManager();


        // check if the triangle must be refined
        GaiaBoundingBox bboxTriangle = triangle.getBoundingBox();

        double widthDeg = bboxTriangle.getLengthX();
        double heightDeg = bboxTriangle.getLengthY();

        double maxDiff = this.manager.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(triangle.getOwnerTileIndices().L);
        log.debug("maxDiff : " + maxDiff + " , tileDepth : " + triangle.getOwnerTileIndices().L + " , tileX : " + triangle.getOwnerTileIndices().X + " , tileY : " + triangle.getOwnerTileIndices().Y);

        // fast check***************************************************************
        // check the barycenter of the triangle
        Vector3d barycenter = triangle.getBarycenter();
        double elevation = terrainElevationDataManager.getElevation(barycenter.x, barycenter.y, this.manager.getMemSaveTerrainElevDataArray());
        double planeElevation = barycenter.z;

        if (abs(elevation - planeElevation) > maxDiff) {
            log.debug("FAST-Check : true");
            return true;
        }

        // more fast check
        /*
        int numInterpolation = 5;
        List<Vector3d> perimeterPositions = triangle.getPerimeterPositions(numInterpolation);
        for(Vector3d perimeterPosition : perimeterPositions)
        {
            elevation = terrainElevationData.getElevation(perimeterPosition.x, perimeterPosition.y);
            planeElevation = perimeterPosition.z;

            if(abs(elevation - planeElevation) > maxDiff)
            {
                return true;
            }
        }*/
        // end check the barycenter of the triangle************************************


        // Another fast check**********************************************************
        //if(mustDivideTriangleByMidLongitudeAndMidLatitude(triangle, this.geographicExtension))
        //{
        //    log.info("ANOTHER-FAST-Check : true*******************************************");
        //    return true;
        //}
        // end another fast check******************************************************

        // if the triangle size is very small, then do not refine**********************
        // Calculate the maxLength of the triangle in meters
        double triangleMaxLegthDeg = Math.max(bboxTriangle.getLengthX(), bboxTriangle.getLengthY());
        double triangleMaxLegthRad = Math.toRadians(triangleMaxLegthDeg);
        double triangleMaxLengthMeters = triangleMaxLegthRad * GlobeUtils.getEquatorialRadius();
        double minTriangleSizeForDepth = this.manager.getMinTriangleSizeForTileDepth(triangle.getOwnerTileIndices().L);
        if (triangleMaxLengthMeters < minTriangleSizeForDepth) {
            triangle.setRefineChecked(true);
            log.debug("MIN-TRIANGLE-SIZE-Check : false &*###################-----------------#################");
            return false;
        }

        double maxTriangleSizeForDepth = this.manager.getMaxTriangleSizeForTileDepth(triangle.getOwnerTileIndices().L);
        if (triangleMaxLengthMeters > maxTriangleSizeForDepth) {
            log.debug("FAST-Check : TRIANGLE IS BIG FOR THE TILE DEPTH*** --- *** --- ***");
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
                    log.debug("SUPER-FAST-Check : true * true * true * true * true * true * true * ");
                    return true;
                }
            }

            log.debug("SUPER-FAST-Check : false *----------------------------------------");
            return false;
        }

        TerrainElevationData terrainElevationData = terrainElevationDataManager.rootTerrainElevationDataQuadTree.getTerrainElevationData(barycenter.x, barycenter.y);
        Vector2d pixelSizeDeg = this.manager.getMemSavePixelSizeDegrees();
        pixelSizeDeg.set(widthDeg / 256.0, heightDeg / 256.0);
        if (terrainElevationData != null) {
            terrainElevationData.getPixelSizeDegree(pixelSizeDeg);
        }

//        double pixelSizeX = Math.max(pixelSizeDeg.x, widthDeg / 256.0);
//        double pixelSizeY = Math.max(pixelSizeDeg.y, heightDeg / 256.0);

        double pixelSizeX = widthDeg / 32.0;
        double pixelSizeY = heightDeg / 32.0;

        GaiaPlane plane = triangle.getPlane();

        int columnsCount = (int) (widthDeg / pixelSizeX);
        int rowsCount = (int) (heightDeg / pixelSizeY);

        double bbox_minX = bboxTriangle.getMinX();
        double bbox_minY = bboxTriangle.getMinY();

        boolean intersects = false;
        int counter = 0;
        List<GaiaHalfEdge> memSave_hedges = new ArrayList<GaiaHalfEdge>();
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

                elevation = terrainElevationDataManager.getElevation(pos_x, pos_y, this.manager.getMemSaveTerrainElevDataArray());
                planeElevation = plane.getValueZ(pos_x, pos_y);

                if (elevation > planeElevation) {
                    if (abs(elevation - planeElevation) > maxDiff * 0.5) {
                        log.debug("SLOW-Check : true" + " , counter : " + counter);
                        memSave_hedges.clear();
                        memSave_line.deleteObjects();
                        return true;
                    }
                } else {
                    if (abs(elevation - planeElevation) > maxDiff) {
                        log.debug("SLOW-Check : true" + " , counter : " + counter);
                        memSave_hedges.clear();
                        memSave_line.deleteObjects();
                        return true;
                    }
                }


            }
        }

        memSave_hedges.clear();
        memSave_line.deleteObjects();

        log.debug("SLOW-Check : false");
        triangle.setRefineChecked(true);
        return false;
    }

    public boolean mustDivideTriangleByMidLongitudeAndMidLatitude(GaiaTriangle triangle, GeographicExtension geoExtent) {
        double lonDegRange = geoExtent.getLongitudeRangeDegree();
        double latDegRange = geoExtent.getLatitudeRangeDegree();

        GaiaBoundingBox bboxTriangle = triangle.getBoundingBox();

        double bboxLengthX = bboxTriangle.getLengthX();
        double bboxLengthY = bboxTriangle.getLengthY();
        double lonError = lonDegRange * 0.1;
        double latError = latDegRange * 0.1;

        double lonDiff = abs(lonDegRange - bboxLengthX);
        double latDiff = abs(latDegRange - bboxLengthY);

        boolean originIsLeftUp = this.manager.isOriginIsLeftUp();

        if (lonDiff < lonError || latDiff < latError) {
            GaiaHalfEdge longestHEdge = triangle.getLongestHalfEdge();
            if (longestHEdge.getTwin() == null) {
                // in this case must check if exist neiborghTile
                HalfEdgeType halfEdgeType = longestHEdge.getType();
                if (halfEdgeType == HalfEdgeType.LEFT) {
                    // check if exist left neigborTile
                    TileIndices leftTileIndices = this.tileIndices.get_L_TileIndices(originIsLeftUp);
                    return !this.manager.existTileFile(leftTileIndices);
                } else if (halfEdgeType == HalfEdgeType.RIGHT) {
                    // check if exist right neigborTile
                    TileIndices rightTileIndices = this.tileIndices.get_R_TileIndices(originIsLeftUp);
                    return !this.manager.existTileFile(rightTileIndices);
                } else if (halfEdgeType == HalfEdgeType.UP) {
                    // check if exist up neigborTile
                    TileIndices upTileIndices = this.tileIndices.get_U_TileIndices(originIsLeftUp);
                    return !this.manager.existTileFile(upTileIndices);
                } else if (halfEdgeType == HalfEdgeType.DOWN) {
                    // check if exist down neigborTile
                    TileIndices downTileIndices = this.tileIndices.get_D_TileIndices(originIsLeftUp);
                    return !this.manager.existTileFile(downTileIndices);
                } else {
                    // error
                    log.debug("ERROR: mustDivideTriangleByMidLongitudeAndMidLatitude");
                }

                return true;
            }

            double midLonDeg = geoExtent.getMidLongitudeDeg();
            double midLatDeg = geoExtent.getMidLatitudeDeg();
            return triangleIntersectsLongitudeOrLatitude(triangle, midLonDeg, midLatDeg);
        }

        return false;
    }

    private boolean refineMeshOneIteration(GaiaMesh mesh, TileIndices currTileIndices) throws TransformException, IOException {
        // Inside the mesh, there are triangles of 9 different tiles
        // Here refine only the triangles of the current tile

        // refine the mesh
        boolean refined = false;
        int splitCount = 0;
        int trianglesCount = mesh.triangles.size();
        log.debug("Triangles count : " + trianglesCount);
        splitCount = 0;
        for (int i = 0; i < trianglesCount; i++) {
            GaiaTriangle triangle = mesh.triangles.get(i);

            if (triangle.getObjectStatus() == GaiaObjectStatus.DELETED) {
                continue;
            }

            if (!triangle.getOwnerTileIndices().isCoincident(currTileIndices)) {
                continue;
            }

            /*
            // Test. Refine only the triangles that intersects the midLon or midLatdouble triangleMaxLegthDeg = Math.max(bboxTriangle.getLengthX(), bboxTriangle.getLengthY());
            GaiaBoundingBox bboxTriangle = triangle.getBoundingBox();
            double triangleMaxLegthDeg = Math.max(bboxTriangle.getLengthX(), bboxTriangle.getLengthY());
            double triangleMaxLegthRad = Math.toRadians(triangleMaxLegthDeg);
            double triangleMaxLengthMeters = triangleMaxLegthRad * GlobeUtils.getEquatorialRadius();
            double minTriangleSizeForDepth = TileWgs84Utils.getMinTriangleSizeForTileDepth(triangle.ownerTile_tileIndices.L);
            if(triangleMaxLengthMeters < minTriangleSizeForDepth)
            {
                triangle.refineChecked = true;
                log.info("MIN-TRIANGLE-SIZE-Check : false &*###################-----------------#################");
                continue;
            }

            if(triangleMaxLengthMeters > TileWgs84Utils.getMaxTriangleSizeForTileDepth(triangle.ownerTile_tileIndices.L))
            {
                log.info("FAST-Check : TRIANGLE IS BIG FOR THE TILE DEPTH*** --- *** --- ***");
                TerrainElevationData terrainElevationData = this.manager.terrainElevationData;
                List<GaiaTriangle> splitTriangles = mesh.splitTriangle(triangle, terrainElevationData);

                if (splitTriangles.size() > 0)
                {
                    splitCount++;
                    refined = true;
                }
            }
            // end test.----------------------------------------------------------------------------------------------------------------------------
            */

            log.debug("iteration :" + i);
            if (mustRefineTriangle(triangle)) // X
            {
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
            mesh.removeDeletedObjects();
            mesh.setObjectsIdInList();
        }


        return refined;
    }

    private boolean refineMeshOneIterationInitial(GaiaMesh mesh) throws TransformException, IOException {

        // refine big triangles of the mesh
        boolean refined = false;
        int splitCount = 0;

        splitCount = 0;

        int trianglesCount = mesh.triangles.size();
        log.debug("Triangles count : " + trianglesCount);
        for (int i = 0; i < trianglesCount; i++) {
            GaiaTriangle triangle = mesh.triangles.get(i);

            if (triangle.getObjectStatus() == GaiaObjectStatus.DELETED) {
                continue;
            }

            log.debug("FAST-Check : TRIANGLE IS BIG FOR THE TILE DEPTH*** --- *** --- ***");
            this.manager.getMemSaveTrianglesArray().clear();
            mesh.splitTriangle(triangle, this.manager.getTerrainElevationDataManager(), this.manager.getMemSaveTrianglesArray());

            if (!this.manager.getMemSaveTrianglesArray().isEmpty()) {
                splitCount++;
                refined = true;
            }
        }

        this.manager.getMemSaveTrianglesArray().clear();


        if (refined) {
            mesh.removeDeletedObjects();
            mesh.setObjectsIdInList();
        }


        return refined;
    }

    public void refineMeshInitial(GaiaMesh mesh) throws TransformException, IOException {
        boolean finished = false;
        int splitCount = 0;
        int maxIterations = 3;
        if (this.tileIndices.L < 10) {
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

    public void recalculateElevation(GaiaMesh mesh, TileIndices currTileIndices) throws TransformException, IOException {
        // Inside the mesh, there are triangles of 9 different tiles
        List<GaiaTriangle> triangles = new ArrayList<>();
        mesh.getTrianglesByTileIndices(currTileIndices, triangles);

        Map<GaiaVertex, GaiaVertex> mapVertices = new HashMap<>();
        for (GaiaTriangle triangle : triangles) {
            List<GaiaVertex> vertices = triangle.getVertices();
            for (GaiaVertex vertex : vertices) {
                mapVertices.put(vertex, vertex);
            }
        }

        // now make vertices from the hashMap
        List<GaiaVertex> verticesOfCurrentTile = new ArrayList<>(mapVertices.values());
        TerrainElevationDataManager terrainElevationDataManager = this.manager.getTerrainElevationDataManager();

        for (GaiaVertex vertex : verticesOfCurrentTile) {
            Vector3d position = vertex.getPosition();
            position.z = terrainElevationDataManager.getElevation(position.x, position.y, this.manager.getMemSaveTerrainElevDataArray());
        }
    }

    public void refineMesh(GaiaMesh mesh, TileIndices currTileIndices) throws TransformException, IOException {
        // Inside the mesh, there are triangles of 9 different tiles
        // Here refine only the triangles of the current tile

        // refine the mesh
        boolean finished = false;
        int splitCount = 0;
        int maxIterations = this.manager.getTriangleRefinementMaxIterations();
        while (!finished) {
            log.debug("iteration : " + splitCount + " : L : " + currTileIndices.L);

            if (!this.refineMeshOneIteration(mesh, currTileIndices)) {
                finished = true;
            }

            splitCount++;

            if (splitCount >= maxIterations) {
                finished = true;
            }

        }

        
    }

    public void loadTileAndSave4Children(TileIndices tileIndices) throws IOException, TransformException {
        //******************************************
        // Function used when save 4 child tiles
        //******************************************

        // 1- load the tile
        // 2- make the 4 children
        // 3- save the 4 children
        // 4- delete the tile

        String tilePath = this.manager.getTilePath(tileIndices);

        // 1- load the tile
        this.loadFile(tilePath);

        boolean originIsLeftUp = this.manager.isOriginIsLeftUp();

        // 2- make the 4 children
        TileIndices child_LU_TileIndices = tileIndices.getChild_LU_TileIndices(originIsLeftUp);
        TileIndices child_RU_TileIndices = tileIndices.getChild_RU_TileIndices(originIsLeftUp);
        TileIndices child_LD_TileIndices = tileIndices.getChild_LD_TileIndices(originIsLeftUp);
        TileIndices child_RD_TileIndices = tileIndices.getChild_RD_TileIndices(originIsLeftUp);

        // 1rst, classify the triangles of the tile
        double midLonDeg = this.geographicExtension.getMidLongitudeDeg();
        double midLatDeg = this.geographicExtension.getMidLatitudeDeg();
        List<GaiaTriangle> triangles = this.mesh.triangles;
        int trianglesCount = triangles.size();
        for (int i = 0; i < trianglesCount; i++) {
            GaiaTriangle triangle = triangles.get(i);

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

        TileMerger3x3 tileMerger3x3 = new TileMerger3x3();
        List<GaiaMesh> childMeshes = new ArrayList<>();
        tileMerger3x3.getSeparatedMeshes(this.mesh, childMeshes, this.manager.isOriginIsLeftUp());

        // 3- save the 4 children
        int childMeshesCount = childMeshes.size();
        for (int i = 0; i < childMeshesCount; i++) {
            GaiaMesh childMesh = childMeshes.get(i);
            GaiaTriangle triangle = childMesh.triangles.get(0); // take the first triangle
            TileIndices childTileIndices = triangle.getOwnerTileIndices();
            String tileTempDirectory = this.manager.getTileTempDirectory();
            String outputDirectory = this.manager.getOutputDirectory();
            String childTileFilePath = TileWgs84Utils.getTileFilePath(childTileIndices.X, childTileIndices.Y, childTileIndices.L);
            String childTileFullPath = tileTempDirectory + File.separator + childTileFilePath;

            try {
                saveFile(childMesh, childTileFullPath); // original

            } catch (IOException e) {
                log.error(e.getMessage());
                return;
            }
        }

        // 4- delete the tile
        //tile.deleteFile(tileIndices);
    }

    public void save4Children_test(TileIndices tileIndices) throws IOException, TransformException {
        //******************************************
        // Test function. Save 4 children tiles
        //******************************************

        // 3- save the 4 children
        // 4- delete the tile

        String tilePath = this.manager.getTilePath(tileIndices);

        // 1- load the tile
        //this.loadFile(tilePath);

        boolean originIsLeftUp = this.manager.isOriginIsLeftUp();

        // 2- make the 4 children
        TileIndices child_LU_TileIndices = tileIndices.getChild_LU_TileIndices(originIsLeftUp);
        TileIndices child_RU_TileIndices = tileIndices.getChild_RU_TileIndices(originIsLeftUp);
        TileIndices child_LD_TileIndices = tileIndices.getChild_LD_TileIndices(originIsLeftUp);
        TileIndices child_RD_TileIndices = tileIndices.getChild_RD_TileIndices(originIsLeftUp);

        List<TileIndices> tileIndicesArray = new ArrayList<>();
        tileIndicesArray.add(child_LU_TileIndices);
        tileIndicesArray.add(child_RU_TileIndices);
        tileIndicesArray.add(child_LD_TileIndices);
        tileIndicesArray.add(child_RD_TileIndices);

        for (int i = 0; i < 4; i++) {
            try {
                TileIndices childTileIndices = tileIndicesArray.get(i);
                String tileTempDirectory = this.manager.getTileTempDirectory();
                String outputDirectory = this.manager.getOutputDirectory();
                String childTileFilePath = TileWgs84Utils.getTileFilePath(childTileIndices.X, childTileIndices.Y, childTileIndices.L);
                String childTileFullPath = tileTempDirectory + File.separator + childTileFilePath;

                // Test. save a simple tile*************************************
                TileWgs84 simpleTile = new TileWgs84(null, this.manager);
                simpleTile.tileIndices = childTileIndices;
                simpleTile.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(childTileIndices.L, childTileIndices.X, childTileIndices.Y, null, this.manager.getImageryType(), this.manager.isOriginIsLeftUp());
                simpleTile.createInitialMesh_test();
                saveFile(simpleTile.mesh, childTileFullPath);

            } catch (IOException e) {
                log.error(e.getMessage());
                return;
            }
        }

    }


}
