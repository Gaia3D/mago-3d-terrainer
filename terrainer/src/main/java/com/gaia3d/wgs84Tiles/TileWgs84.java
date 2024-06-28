package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.*;
import com.gaia3d.basic.types.HalfEdgeType;
import com.gaia3d.command.GlobalOptions;
import com.gaia3d.reader.FileUtils;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.util.io.BigEndianDataInputStream;
import com.gaia3d.util.io.BigEndianDataOutputStream;
import lombok.Getter;
import lombok.Setter;
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

@Getter
@Setter
@Slf4j
public class TileWgs84 {
    private static final GlobalOptions globalOptions = GlobalOptions.getInstance();

    private TileWgs84Manager manager = null;

    private TileWgs84 parentTile = null;
    // if parentTile == null, then this is the root tile.
    private TileIndices tileIndices = null;

    private GeographicExtension geographicExtension = null;

    private GaiaMesh mesh = null;

    // for current tile, create the 8 neighbor tiles.
    //  +----------+----------+----------+
    //  |          |          |          |
    //  | leftUp   |  upTile  | RupTile  |
    //  |  Tile    |          |          |
    //  +----------+----------+----------+
    //  |          |          |          |
    //  | leftTile |curr_Tile| rightTile |
    //  |          |          |          |
    //  +----------+----------+----------+
    //  |          |          |          |
    //  | leftDown | downTile | right    |
    //  |   Tile   |          | DownTile |
    //  +----------+----------+----------+

    private TileWgs84[] neighborTiles = new TileWgs84[8];
    private TileWgs84[] childTiles = new TileWgs84[4];

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
        BigEndianDataOutputStream dataOutputStream = new BigEndianDataOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)));

        mesh.saveDataOutputStream(dataOutputStream);
        dataOutputStream.close();
    }

    public void loadFile(String filePath) throws IOException {
        BigEndianDataInputStream dataInputStream = new BigEndianDataInputStream(new BufferedInputStream(new FileInputStream(filePath)));

        this.mesh = new GaiaMesh();
        this.mesh.loadDataInputStream(dataInputStream);
        dataInputStream.close();
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

        double elevMinLonMinLat = terrainElevationDataManager.getElevation(minLonDeg, minLatDeg, this.manager.getMemSaveTerrainElevDataList());
        double elevMaxLonMinLat = terrainElevationDataManager.getElevation(maxLonDeg, minLatDeg, this.manager.getMemSaveTerrainElevDataList());
        double elevMaxLonMaxLat = terrainElevationDataManager.getElevation(maxLonDeg, maxLatDeg, this.manager.getMemSaveTerrainElevDataList());
        double elevMinLonMaxLat = terrainElevationDataManager.getElevation(minLonDeg, maxLatDeg, this.manager.getMemSaveTerrainElevDataList());

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

        GaiaTriangle triangle1 = this.mesh.newTriangle();
        GaiaTriangle triangle2 = this.mesh.newTriangle();

        // Triangle 1
        GaiaHalfEdge halfEdgeT1V1 = this.mesh.newHalfEdge();
        GaiaHalfEdge halfEdgeT1V2 = this.mesh.newHalfEdge();
        GaiaHalfEdge halfEdgeT1V3 = this.mesh.newHalfEdge(); // twin of halfEdgeT2V1

        halfEdgeT1V1.setStartVertex(vertexLD);
        halfEdgeT1V1.setType(HalfEdgeType.DOWN);
        halfEdgeT1V2.setStartVertex(vertexRD);
        halfEdgeT1V2.setType(HalfEdgeType.RIGHT);
        halfEdgeT1V3.setStartVertex(vertexRU);
        halfEdgeT1V3.setType(HalfEdgeType.INTERIOR);

        List<GaiaHalfEdge> halfEdges_T1 = new ArrayList<>();
        halfEdges_T1.add(halfEdgeT1V1);
        halfEdges_T1.add(halfEdgeT1V2);
        halfEdges_T1.add(halfEdgeT1V3);

        GaiaHalfEdgeUtils.concatenateHalfEdgesLoop(halfEdges_T1);

        // Triangle 2
        GaiaHalfEdge halfEdgeT2V1 = this.mesh.newHalfEdge(); // twin of halfEdgeT1V3
        GaiaHalfEdge halfEdgeT2V2 = this.mesh.newHalfEdge();
        GaiaHalfEdge halfEdgeT2V3 = this.mesh.newHalfEdge();

        halfEdgeT2V1.setStartVertex(vertexLD);
        halfEdgeT2V1.setType(HalfEdgeType.INTERIOR);
        halfEdgeT2V2.setStartVertex(vertexRU);
        halfEdgeT2V2.setType(HalfEdgeType.UP);
        halfEdgeT2V3.setStartVertex(vertexLU);
        halfEdgeT2V3.setType(HalfEdgeType.LEFT);

        List<GaiaHalfEdge> halfEdgesT2 = new ArrayList<>();
        halfEdgesT2.add(halfEdgeT2V1);
        halfEdgesT2.add(halfEdgeT2V2);
        halfEdgesT2.add(halfEdgeT2V3);

        GaiaHalfEdgeUtils.concatenateHalfEdgesLoop(halfEdgesT2);

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

        double elevMinLonMinLat = terrainElevationDataManager.getElevation(minLonDeg, minLatDeg, this.manager.getMemSaveTerrainElevDataList());
        double elevMaxLonMinLat = terrainElevationDataManager.getElevation(maxLonDeg, minLatDeg, this.manager.getMemSaveTerrainElevDataList());
        double elevMaxLonMaxLat = terrainElevationDataManager.getElevation(maxLonDeg, maxLatDeg, this.manager.getMemSaveTerrainElevDataList());
        double elevMinLonMaxLat = terrainElevationDataManager.getElevation(minLonDeg, maxLatDeg, this.manager.getMemSaveTerrainElevDataList());

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

        GaiaTriangle triangle1 = this.mesh.newTriangle();
        GaiaTriangle triangle2 = this.mesh.newTriangle();

        // Triangle 1
        GaiaHalfEdge halfEdgeT1V1 = this.mesh.newHalfEdge();
        GaiaHalfEdge halfEdgeT1V2 = this.mesh.newHalfEdge();
        GaiaHalfEdge halfEdgeT1V3 = this.mesh.newHalfEdge(); // twin of halfEdgeT2V1

        halfEdgeT1V1.setStartVertex(vertexLD);
        halfEdgeT1V1.setType(HalfEdgeType.DOWN);
        halfEdgeT1V2.setStartVertex(vertexRD);
        halfEdgeT1V2.setType(HalfEdgeType.RIGHT);
        halfEdgeT1V3.setStartVertex(vertexRU);
        halfEdgeT1V3.setType(HalfEdgeType.INTERIOR);

        List<GaiaHalfEdge> halfEdgesT1 = new ArrayList<>();
        halfEdgesT1.add(halfEdgeT1V1);
        halfEdgesT1.add(halfEdgeT1V2);
        halfEdgesT1.add(halfEdgeT1V3);

        GaiaHalfEdgeUtils.concatenateHalfEdgesLoop(halfEdgesT1);

        // Triangle 2
        GaiaHalfEdge halfEdgeT2V1 = this.mesh.newHalfEdge(); // twin of halfEdgeT1V3
        GaiaHalfEdge halfEdgeT2V2 = this.mesh.newHalfEdge();
        GaiaHalfEdge halfEdgeT2V3 = this.mesh.newHalfEdge();

        halfEdgeT2V1.setStartVertex(vertexLD);
        halfEdgeT2V1.setType(HalfEdgeType.INTERIOR);
        halfEdgeT2V2.setStartVertex(vertexRU);
        halfEdgeT2V2.setType(HalfEdgeType.UP);
        halfEdgeT2V3.setStartVertex(vertexLU);
        halfEdgeT2V3.setType(HalfEdgeType.LEFT);

        List<GaiaHalfEdge> halfEdgesT2 = new ArrayList<>();
        halfEdgesT2.add(halfEdgeT2V1);
        halfEdgesT2.add(halfEdgeT2V2);
        halfEdgesT2.add(halfEdgeT2V3);

        GaiaHalfEdgeUtils.concatenateHalfEdgesLoop(halfEdgesT2);

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

    public void makeBigMesh(boolean is1rstGeneration) throws IOException, TransformException {

        // make a bigMesh
        // bigMesh:
        //  +----------+----------+----------+
        //  |          |          |          |
        //  | leftUpmesh  |  upmesh  | Rupmesh  |
        //  |          |          |          |
        //  +----------+----------+----------+
        //  |          |          |          |
        //  | leftmesh   |curr_mesh | rightmesh   |
        //  |          |          |          |
        //  +----------+----------+----------+
        //  |          |          |          |
        //  | leftDownmesh  | downmesh   | rightDownmesh  |
        //  |          |          |          |
        //  +----------+----------+----------+

        // Note : only in the 1rst generation the tiles must be created
        boolean originIsLeftUp = this.manager.isOriginIsLeftUp();

        TileIndices currTileIndices = this.tileIndices;
        TileWgs84 currTile = null;
        if (is1rstGeneration) {
            currTile = this.manager.loadOrCreateTileWgs84(currTileIndices);
        } else {
            currTile = this.manager.loadTileWgs84(currTileIndices);
        }


        TileIndices leftDownTileIndices = this.tileIndices.getLeftDownTileIndices(originIsLeftUp);
        TileWgs84 leftDownTile = null;
        if (is1rstGeneration) {
            leftDownTile = this.manager.loadOrCreateTileWgs84(leftDownTileIndices);
        } else {
            leftDownTile = this.manager.loadTileWgs84(leftDownTileIndices);
        }

        TileIndices downTileIndices = this.tileIndices.getDownTileIndices(originIsLeftUp);
        TileWgs84 downTile = null;
        if (is1rstGeneration) {
            downTile = this.manager.loadOrCreateTileWgs84(downTileIndices);
        } else {
            downTile = this.manager.loadTileWgs84(downTileIndices);
        }

        TileIndices rightDownTileIndices = this.tileIndices.getRightDownTileIndices(originIsLeftUp);
        TileWgs84 rightDownTile = null;
        if (is1rstGeneration) {
            rightDownTile = this.manager.loadOrCreateTileWgs84(rightDownTileIndices);
        } else {
            rightDownTile = this.manager.loadTileWgs84(rightDownTileIndices);
        }

        TileIndices leftTileIndices = this.tileIndices.getLeftTileIndices(originIsLeftUp);
        TileWgs84 leftTile = null;
        if (is1rstGeneration) {
            leftTile = this.manager.loadOrCreateTileWgs84(leftTileIndices);
        } else {
            leftTile = this.manager.loadTileWgs84(leftTileIndices);
        }

        TileIndices rightTileIndices = this.tileIndices.getRightTileIndices(originIsLeftUp);
        TileWgs84 rightTile = null;
        if (is1rstGeneration) {
            rightTile = this.manager.loadOrCreateTileWgs84(rightTileIndices);
        } else {
            rightTile = this.manager.loadTileWgs84(rightTileIndices);
        }

        TileIndices leftUpTileIndices = this.tileIndices.getLeftUpTileIndices(originIsLeftUp);
        TileWgs84 leftUpTile = null;
        if (is1rstGeneration) {
            leftUpTile = this.manager.loadOrCreateTileWgs84(leftUpTileIndices);
        } else {
            leftUpTile = this.manager.loadTileWgs84(leftUpTileIndices);
        }

        TileIndices upTileIndices = this.tileIndices.getUpTileIndices(originIsLeftUp);
        TileWgs84 upTile = null;
        if (is1rstGeneration) {
            upTile = this.manager.loadOrCreateTileWgs84(upTileIndices);
        } else {
            upTile = this.manager.loadTileWgs84(upTileIndices);
        }

        TileIndices rightUpTileIndices = this.tileIndices.getRightUpTileIndices(originIsLeftUp);
        TileWgs84 rightUpTile = null;
        if (is1rstGeneration) {
            rightUpTile = this.manager.loadOrCreateTileWgs84(rightUpTileIndices);
        } else {
            rightUpTile = this.manager.loadTileWgs84(rightUpTileIndices);
        }

        // now make the bigMesh
        // public TileMerger3x3(TileWgs84 center_Tile, TileWgs84 left_Tile, TileWgs84 right_Tile,
        //                         TileWgs84 up_Tile, TileWgs84 down_Tile, TileWgs84 left_up_Tile,
        //                         TileWgs84 right_up_Tile, TileWgs84 left_down_Tile, TileWgs84 right_down_Tile)
        TileMerger3x3 tileMerger3x3 = TileMerger3x3.builder()
                .centerTile(currTile)
                .leftTile(leftTile)
                .rightTile(rightTile)
                .upTile(upTile)
                .downTile(downTile)
                .leftUpTile(leftUpTile)
                .rightUpTile(rightUpTile)
                .leftDownTile(leftDownTile)
                .rightDownTile(rightDownTile)
                .build();

        GaiaMesh bigMesh = tileMerger3x3.getMergedMesh();
        bigMesh.setObjectsIdInList();

        // 1rst update elevations with the current tile depth geoTiff
        recalculateElevation(bigMesh, currTileIndices);

        // Now refine the bigMesh
        refineMesh(bigMesh, currTileIndices);

        // now save the 9 tiles
        List<GaiaMesh> separatedMeshes = new ArrayList<>();
        tileMerger3x3.getSeparatedMeshes(bigMesh, separatedMeshes, originIsLeftUp);
        saveSeparatedTiles(separatedMeshes);
    }

    public boolean saveSeparatedTiles(List<GaiaMesh> separatedMeshes) {
        // save the 9 tiles
        for (GaiaMesh mesh : separatedMeshes) {
            if (!TileWgs84Utils.checkTileTest(mesh, this.manager.getVertexCoincidentError(), this.manager.isOriginIsLeftUp())) {
                log.error("Mesh is invalid.");
            }

            GaiaTriangle triangle = mesh.triangles.get(0); // take the first triangle
            TileIndices tileIndices = triangle.getOwnerTileIndices();
            String tileTempDirectory = globalOptions.getTileTempPath();
            String outputDirectory = globalOptions.getOutputPath();
            String tileFilePath = TileWgs84Utils.getTileFilePath(tileIndices.getX(), tileIndices.getY(), tileIndices.getL());
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
        int vertexLeftSideCount = 0;
        int vertexRightSideCount = 0;
        int vertexUpsideCount = 0;
        int vertexDownsideCount = 0;
        int vertexCoincidentCount = 0;
        double vertexCoincidentError = this.manager.getVertexCoincidentError();

        for (GaiaVertex vertex : vertices) {
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

        double maxDiff = this.manager.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(triangle.getOwnerTileIndices().getL());
        log.debug("maxDiff : " + maxDiff + " , tileDepth : " + triangle.getOwnerTileIndices().getL() + " , tileX : " + triangle.getOwnerTileIndices().getX() + " , tileY : " + triangle.getOwnerTileIndices().getY());

        // fast check***************************************************************
        // check the barycenter of the triangle
        Vector3d barycenter = triangle.getBarycenter();
        double elevation = terrainElevationDataManager.getElevation(barycenter.x, barycenter.y, this.manager.getMemSaveTerrainElevDataList());
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
        double triangleMaxLengthDeg = Math.max(bboxTriangle.getLengthX(), bboxTriangle.getLengthY());
        double triangleMaxLengthRad = Math.toRadians(triangleMaxLengthDeg);
        double triangleMaxLengthMeters = triangleMaxLengthRad * GlobeUtils.EQUATORIAL_RADIUS;
        double minTriangleSizeForDepth = this.manager.getMinTriangleSizeForTileDepth(triangle.getOwnerTileIndices().getL());
        if (triangleMaxLengthMeters < minTriangleSizeForDepth) {
            triangle.setRefineChecked(true);
            log.debug("MIN-TRIANGLE-SIZE-Check : false &*###################-----------------#################");
            return false;
        }

        double maxTriangleSizeForDepth = this.manager.getMaxTriangleSizeForTileDepth(triangle.getOwnerTileIndices().getL());
        if (triangleMaxLengthMeters > maxTriangleSizeForDepth) {
            log.debug("FAST-Check : TRIANGLE IS BIG FOR THE TILE DEPTH*** --- *** --- ***");
            return true;
        }

        // check if the triangle intersects the terrainData
        GeographicExtension rootGeographicExtension = terrainElevationDataManager.getRootGeographicExtension();
        if (!rootGeographicExtension.intersectsBox(bboxTriangle.getMinX(), bboxTriangle.getMinY(), bboxTriangle.getMaxX(), bboxTriangle.getMaxY())) {
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

        TerrainElevationData terrainElevationData = terrainElevationDataManager.getRootTerrainElevationDataQuadTree().getTerrainElevationData(barycenter.x, barycenter.y);
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

        double bboxMinX = bboxTriangle.getMinX();
        double bboxMinY = bboxTriangle.getMinY();

        int counter = 0;
        List<GaiaHalfEdge> memSaveHedges = new ArrayList<>();
        GaiaLine2D memSaveline = new GaiaLine2D();
        for (int row = 0; row < rowsCount; row++) {
            double posY = bboxMinY + row * pixelSizeY;
            for (int column = 0; column < columnsCount; column++) {
                double posX = bboxMinX + column * pixelSizeX;
                boolean intersects = triangle.intersectsPointXY(posX, posY, memSaveHedges, memSaveline);
                counter++;

                if (!intersects) {
                    continue;
                }

                elevation = terrainElevationDataManager.getElevation(posX, posY, this.manager.getMemSaveTerrainElevDataList());
                planeElevation = plane.getValueZ(posX, posY);

                if (elevation > planeElevation) {
                    if (abs(elevation - planeElevation) > maxDiff * 0.5) {
                        log.debug("SLOW-Check : true" + " , counter : " + counter);
                        memSaveHedges.clear();
                        return true;
                    }
                } else {
                    if (abs(elevation - planeElevation) > maxDiff) {
                        log.debug("SLOW-Check : true" + " , counter : " + counter);
                        memSaveHedges.clear();
                        return true;
                    }
                }
            }
        }

        memSaveHedges.clear();

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
                // in this case must check if exist neighborTile
                HalfEdgeType halfEdgeType = longestHEdge.getType();
                if (halfEdgeType == HalfEdgeType.LEFT) {
                    // check if exist left neighborTile
                    TileIndices leftTileIndices = this.tileIndices.getLeftTileIndices(originIsLeftUp);
                    return this.manager.isNotExistsTileFile(leftTileIndices);
                } else if (halfEdgeType == HalfEdgeType.RIGHT) {
                    // check if exist right neighborTile
                    TileIndices rightTileIndices = this.tileIndices.getRightTileIndices(originIsLeftUp);
                    return this.manager.isNotExistsTileFile(rightTileIndices);
                } else if (halfEdgeType == HalfEdgeType.UP) {
                    // check if exist up neighborTile
                    TileIndices upTileIndices = this.tileIndices.getUpTileIndices(originIsLeftUp);
                    return this.manager.isNotExistsTileFile(upTileIndices);
                } else if (halfEdgeType == HalfEdgeType.DOWN) {
                    // check if exist down neighborTile
                    TileIndices downTileIndices = this.tileIndices.getDownTileIndices(originIsLeftUp);
                    return this.manager.isNotExistsTileFile(downTileIndices);
                } else {
                    log.warn("HalfEdgeType is not valid.");
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
        int trianglesCount = mesh.triangles.size();
        log.debug("[RefineMesh] Triangles Count : {}", trianglesCount);
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
            double minTriangleSizeForDepth = TileWgs84Utils.getMinTriangleSizeForTileDepth(triangle.ownerTile_TileIndices.L);
            if(triangleMaxLengthMeters < minTriangleSizeForDepth)
            {
                triangle.refineChecked = true;
                log.info("MIN-TRIANGLE-SIZE-Check : false &*###################-----------------#################");
                continue;
            }

            if(triangleMaxLengthMeters > TileWgs84Utils.getMaxTriangleSizeForTileDepth(triangle.ownerTile_TileIndices.L))
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

            log.debug("[RefineMesh] iteration :" + i);
            if (mustRefineTriangle(triangle)) // X
            {
                this.manager.getMemSaveTrianglesList().clear();
                mesh.splitTriangle(triangle, this.manager.getTerrainElevationDataManager(), this.manager.getMemSaveTrianglesList());

                if (!this.manager.getMemSaveTrianglesList().isEmpty()) {
                    refined = true;
                }

                this.manager.getMemSaveTrianglesList().clear();
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
        int trianglesCount = mesh.triangles.size();
        log.debug("[RefineMesh] Triangles Count : {}", trianglesCount);
        for (int i = 0; i < trianglesCount; i++) {
            GaiaTriangle triangle = mesh.triangles.get(i);

            if (triangle.getObjectStatus() == GaiaObjectStatus.DELETED) {
                continue;
            }

            log.debug("[RefineMesh] FAST-Check : TRIANGLE IS BIG FOR THE TILE DEPTH");
            this.manager.getMemSaveTrianglesList().clear();
            mesh.splitTriangle(triangle, this.manager.getTerrainElevationDataManager(), this.manager.getMemSaveTrianglesList());

            if (!this.manager.getMemSaveTrianglesList().isEmpty()) {
                refined = true;
            }
        }

        this.manager.getMemSaveTrianglesList().clear();
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
            position.z = terrainElevationDataManager.getElevation(position.x, position.y, this.manager.getMemSaveTerrainElevDataList());
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
            log.debug("[RefineMesh] iteration : {} : L : {} ", splitCount, currTileIndices.getL());
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
        TileIndices child_leftUpTileIndices = tileIndices.getChildLeftUpTileIndices(originIsLeftUp);
        TileIndices child_RupTileIndices = tileIndices.getChildRightUpTileIndices(originIsLeftUp);
        TileIndices child_leftDownTileIndices = tileIndices.getChildLeftDownTileIndices(originIsLeftUp);
        TileIndices child_rightDownTileIndices = tileIndices.getChildRightDownTileIndices(originIsLeftUp);

        // 1rst, classify the triangles of the tile
        double midLonDeg = this.geographicExtension.getMidLongitudeDeg();
        double midLatDeg = this.geographicExtension.getMidLatitudeDeg();
        List<GaiaTriangle> triangles = this.mesh.triangles;
        for (GaiaTriangle triangle : triangles) {
            if (triangle.getObjectStatus() == GaiaObjectStatus.DELETED) {
                continue;
            }

            Vector3d barycenter = triangle.getBarycenter();
            if (barycenter.x < midLonDeg) {
                if (barycenter.y < midLatDeg) {
                    // leftDownTile
                    triangle.setOwnerTileIndices(child_leftDownTileIndices);
                } else {
                    // leftUpTile
                    triangle.setOwnerTileIndices(child_leftUpTileIndices);
                }
            } else {
                if (barycenter.y < midLatDeg) {
                    // rightDownTile
                    triangle.setOwnerTileIndices(child_rightDownTileIndices);
                } else {
                    // RupTile
                    triangle.setOwnerTileIndices(child_RupTileIndices);
                }
            }
        }

        TileMerger3x3 tileMerger3x3 = TileMerger3x3.builder().build();
        List<GaiaMesh> childMeshes = new ArrayList<>();
        tileMerger3x3.getSeparatedMeshes(this.mesh, childMeshes, this.manager.isOriginIsLeftUp());

        // 3- save the 4 children
        int childMeshesCount = childMeshes.size();
        for (GaiaMesh childMesh : childMeshes) {
            GaiaTriangle triangle = childMesh.triangles.get(0); // take the first triangle
            TileIndices childTileIndices = triangle.getOwnerTileIndices();
            String tileTempDirectory = globalOptions.getTileTempPath();
            String childTileFilePath = TileWgs84Utils.getTileFilePath(childTileIndices.getX(), childTileIndices.getY(), childTileIndices.getL());
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

    /*public void save4Children_test(TileIndices tileIndices) throws IOException, TransformException {
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
        TileIndices child_leftUpTileIndices = tileIndices.getChildLeftUpTileIndices(originIsLeftUp);
        TileIndices child_RupTileIndices = tileIndices.getChildRightUpTileIndices(originIsLeftUp);
        TileIndices child_leftDownTileIndices = tileIndices.getChildLeftDownTileIndices(originIsLeftUp);
        TileIndices child_rightDownTileIndices = tileIndices.getChildRightDownTileIndices(originIsLeftUp);

        List<TileIndices> tileIndicesArray = new ArrayList<>();
        tileIndicesArray.add(child_leftUpTileIndices);
        tileIndicesArray.add(child_RupTileIndices);
        tileIndicesArray.add(child_leftDownTileIndices);
        tileIndicesArray.add(child_rightDownTileIndices);

        for (int i = 0; i < 4; i++) {
            try {
                TileIndices childTileIndices = tileIndicesArray.get(i);
                String tileTempDirectory = globalOptions.getTileTempPath();
                String outputDirectory = globalOptions.getOutputPath();
                String childTileFilePath = TileWgs84Utils.getTileFilePath(childTileIndices.getX(), childTileIndices.getY(), childTileIndices.getL());
                String childTileFullPath = tileTempDirectory + File.separator + childTileFilePath;

                // Test. save a simple tile*************************************
                TileWgs84 simpleTile = new TileWgs84(null, this.manager);
                simpleTile.tileIndices = childTileIndices;
                simpleTile.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(childTileIndices.getL(), childTileIndices.getX(), childTileIndices.getY(), null, this.manager.getImageryType(), this.manager.isOriginIsLeftUp());
                simpleTile.createInitialMesh_test();
                saveFile(simpleTile.mesh, childTileFullPath);

            } catch (IOException e) {
                log.error(e.getMessage());
                return;
            }
        }

    }*/
}
