package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.*;
import com.gaia3d.util.io.LittleEndianDataInputStream;
import com.gaia3d.util.io.LittleEndianDataOutputStream;
import com.gaia3d.reader.FileUtils;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.opengis.referencing.operation.TransformException;


import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

import static java.lang.Math.abs;


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

    public void saveFile(GaiaMesh mesh, String filePath) throws IOException {
        String foldersPath = FileUtils.removeFileNameFromPath(filePath);
        FileUtils.createAllFoldersIfNoExist(foldersPath);

        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        LittleEndianDataOutputStream dataOutputStream = new LittleEndianDataOutputStream(fileOutputStream);

        // delete the file if exists before save.***
        FileUtils.deleteFileIfExists(filePath);

        // save the tile.***
        mesh.saveDataOutputStream(dataOutputStream);
    }

    public void saveFileBigMesh(String filePath, GaiaMesh bigMesh) throws IOException {
        // this is a temp function.***
        // delete after test.***
        String foldersPath = FileUtils.removeFileNameFromPath(filePath);
        FileUtils.createAllFoldersIfNoExist(foldersPath);

        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        LittleEndianDataOutputStream dataOutputStream = new LittleEndianDataOutputStream(fileOutputStream);

        // delete the file if exists before save.***
        FileUtils.deleteFileIfExists(filePath);

        // save the tile.***
        bigMesh.saveDataOutputStream(dataOutputStream);
    }

    public void loadFile(String filePath) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(filePath);
        LittleEndianDataInputStream dataInputStream = new LittleEndianDataInputStream(fileInputStream);

        this.mesh = new GaiaMesh();
        this.mesh.loadDataInputStream(dataInputStream);
    }

    public void createInitialMesh() throws TransformException {
        // The initial mesh consists in 4 vertex & 2 triangles.***
        this.mesh = new GaiaMesh();

        GaiaVertex vertexLD = this.mesh.newVertex();
        GaiaVertex vertexRD = this.mesh.newVertex();
        GaiaVertex vertexRU = this.mesh.newVertex();
        GaiaVertex vertexLU = this.mesh.newVertex();

        TerrainElevationData terrainElevationData = this.manager.terrainElevationData;

        double minLonDeg = this.geographicExtension.getMinLongitudeDeg();
        double minLatDeg = this.geographicExtension.getMinLatitudeDeg();
        double maxLonDeg = this.geographicExtension.getMaxLongitudeDeg();
        double maxLatDeg = this.geographicExtension.getMaxLatitudeDeg();

        double elevMinLonMinLat = terrainElevationData.getElevation(minLonDeg, minLatDeg);
        double elevMaxLonMinLat = terrainElevationData.getElevation(maxLonDeg, minLatDeg);
        double elevMaxLonMaxLat = terrainElevationData.getElevation(maxLonDeg, maxLatDeg);
        double elevMinLonMaxLat = terrainElevationData.getElevation(minLonDeg, maxLatDeg);

        vertexLD.position = new Vector3d(minLonDeg, minLatDeg, elevMinLonMinLat);
        vertexRD.position = new Vector3d(maxLonDeg, minLatDeg, elevMaxLonMinLat);
        vertexRU.position = new Vector3d(maxLonDeg, maxLatDeg, elevMaxLonMaxLat);
        vertexLU.position = new Vector3d(minLonDeg, maxLatDeg, elevMinLonMaxLat);


        // create the 2 triangles.***
        // he = halfEdge.***
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
        // so, the halfEdge_T1_3 is the twin of halfEdge_T2_1.***

        GaiaTriangle triangle1 = this.mesh.newTriangle();
        GaiaTriangle triangle2 = this.mesh.newTriangle();

        // Triangle 1.***
        GaiaHalfEdge halfEdge_T1_1 = this.mesh.newHalfEdge();
        GaiaHalfEdge halfEdge_T1_2 = this.mesh.newHalfEdge();
        GaiaHalfEdge halfEdge_T1_3 = this.mesh.newHalfEdge(); // twin of halfEdge_T2_1.***

        halfEdge_T1_1.setStartVertex(vertexLD);
        halfEdge_T1_1.type = HalfEdgeType.DOWN;
        halfEdge_T1_2.setStartVertex(vertexRD);
        halfEdge_T1_2.type = HalfEdgeType.RIGHT;
        halfEdge_T1_3.setStartVertex(vertexRU);
        halfEdge_T1_3.type = HalfEdgeType.INTERIOR;

        ArrayList<GaiaHalfEdge> halfEdges_T1 = new ArrayList<GaiaHalfEdge>();
        halfEdges_T1.add(halfEdge_T1_1);
        halfEdges_T1.add(halfEdge_T1_2);
        halfEdges_T1.add(halfEdge_T1_3);

        GaiaHalfEdgeUtils.concatenateHalfEdgesLoop(halfEdges_T1);

        // Triangle 2.***
        GaiaHalfEdge halfEdge_T2_1 = this.mesh.newHalfEdge(); // twin of halfEdge_T1_3.***
        GaiaHalfEdge halfEdge_T2_2 = this.mesh.newHalfEdge();
        GaiaHalfEdge halfEdge_T2_3 = this.mesh.newHalfEdge();

        halfEdge_T2_1.setStartVertex(vertexLD);
        halfEdge_T2_1.type = HalfEdgeType.INTERIOR;
        halfEdge_T2_2.setStartVertex(vertexRU);
        halfEdge_T2_2.type = HalfEdgeType.UP;
        halfEdge_T2_3.setStartVertex(vertexLU);
        halfEdge_T2_3.type = HalfEdgeType.LEFT;

        ArrayList<GaiaHalfEdge> halfEdges_T2 = new ArrayList<GaiaHalfEdge>();
        halfEdges_T2.add(halfEdge_T2_1);
        halfEdges_T2.add(halfEdge_T2_2);
        halfEdges_T2.add(halfEdge_T2_3);

        GaiaHalfEdgeUtils.concatenateHalfEdgesLoop(halfEdges_T2);

        // now set twins.***
        halfEdge_T1_3.setTwin(halfEdge_T2_1);

        // now set triangles.***
        triangle1.setHalfEdge(halfEdge_T1_1);
        triangle2.setHalfEdge(halfEdge_T2_1);

        // the 2 triangles have the same ownerTile.***
        triangle1.ownerTile_tileIndices.copyFrom(this.tileIndices);
        triangle2.ownerTile_tileIndices.copyFrom(this.tileIndices);

        // now set objects id in list.***
        this.mesh.setObjectsIdInList();

    }



    public void makeBigMesh(boolean is1rstGeneration) throws IOException, TransformException {

        // make a bigMesh.***
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

        // Note : only in the 1rst generation the tiles must be created.***

        TileIndices curr_TileIndices = this.tileIndices;
        TileWgs84 curr_tile = null;
        if(is1rstGeneration)
        {
            curr_tile = this.manager.loadOrCreateTileWgs84(curr_TileIndices);
        }
        else {
            curr_tile = this.manager.loadTileWgs84(curr_TileIndices);
        }


        TileIndices LD_TileIndices = this.tileIndices.get_LD_TileIndices();
        TileWgs84 LD_tile = null;
        if(is1rstGeneration)
        {
            LD_tile = this.manager.loadOrCreateTileWgs84(LD_TileIndices);
        }
        else {
            LD_tile = this.manager.loadTileWgs84(LD_TileIndices);
        }

        TileIndices D_TileIndices = this.tileIndices.get_D_TileIndices();
        TileWgs84 D_tile = null;
        if(is1rstGeneration)
        {
            D_tile = this.manager.loadOrCreateTileWgs84(D_TileIndices);
        }
        else {
            D_tile = this.manager.loadTileWgs84(D_TileIndices);
        }

        TileIndices RD_TileIndices = this.tileIndices.get_RD_TileIndices();
        TileWgs84 RD_tile = null;
        if(is1rstGeneration)
        {
            RD_tile = this.manager.loadOrCreateTileWgs84(RD_TileIndices);
        }
        else {
            RD_tile = this.manager.loadTileWgs84(RD_TileIndices);
        }

        TileIndices L_TileIndices = this.tileIndices.get_L_TileIndices();
        TileWgs84 L_tile = null;
        if(is1rstGeneration)
        {
            L_tile = this.manager.loadOrCreateTileWgs84(L_TileIndices);
        }
        else {
            L_tile = this.manager.loadTileWgs84(L_TileIndices);
        }

        TileIndices R_TileIndices = this.tileIndices.get_R_TileIndices();
        TileWgs84 R_tile = null;
        if(is1rstGeneration)
        {
            R_tile = this.manager.loadOrCreateTileWgs84(R_TileIndices);
        }
        else {
            R_tile = this.manager.loadTileWgs84(R_TileIndices);
        }

        TileIndices LU_TileIndices = this.tileIndices.get_LU_TileIndices();
        TileWgs84 LU_tile = null;
        if(is1rstGeneration)
        {
            LU_tile = this.manager.loadOrCreateTileWgs84(LU_TileIndices);
        }
        else {
            LU_tile = this.manager.loadTileWgs84(LU_TileIndices);
        }

        TileIndices U_TileIndices = this.tileIndices.get_U_TileIndices();
        TileWgs84 U_tile = null;
        if(is1rstGeneration)
        {
            U_tile = this.manager.loadOrCreateTileWgs84(U_TileIndices);
        }
        else {
            U_tile = this.manager.loadTileWgs84(U_TileIndices);
        }

        TileIndices RU_TileIndices = this.tileIndices.get_RU_TileIndices();
        TileWgs84 RU_tile = null;
        if(is1rstGeneration)
        {
            RU_tile = this.manager.loadOrCreateTileWgs84(RU_TileIndices);
        }
        else {
            RU_tile = this.manager.loadTileWgs84(RU_TileIndices);
        }

        // now make the bigMesh.***
        // public TileMerger3x3(TileWgs84 center_tile, TileWgs84 left_tile, TileWgs84 right_tile,
        //                         TileWgs84 up_tile, TileWgs84 down_tile, TileWgs84 left_up_tile,
        //                         TileWgs84 right_up_tile, TileWgs84 left_down_tile, TileWgs84 right_down_tile)
        TileMerger3x3 tileMerger3x3 = new TileMerger3x3(curr_tile, L_tile, R_tile, U_tile, D_tile, LU_tile, RU_tile, LD_tile, RD_tile);

        GaiaMesh bigMesh = tileMerger3x3.getMergedMesh();
        bigMesh.setObjectsIdInList();

        // create the geographicExtension of the bigMesh.***
        // The geographicExtension of the bigMesh is the geographicExtension of the current tile.***
        //double midLonDeg = this.geographicExtension.getMidLongitudeDeg();
        //double midLatDeg = this.geographicExtension.getMidLatitudeDeg();

        refineMesh(bigMesh, curr_TileIndices);

        // now save the 9 tiles.***
        ArrayList<GaiaMesh> separatedMeshes = new ArrayList<GaiaMesh>();
        tileMerger3x3.getSeparatedMeshes(bigMesh, separatedMeshes);
        saveSeparatedTiles(separatedMeshes);

        // provisionally save the bigMesh.***
        String tileTempDirectory = this.manager.tileTempDirectory;
        String outputDirectory = this.manager.outputDirectory;
        String bigMeshFilePath = TileWgs84Utils.getTileFileName(curr_TileIndices.X, curr_TileIndices.X, curr_TileIndices.L) + "bigMesh.til";
        String bigMeshFullPath = tileTempDirectory + "\\" + bigMeshFilePath;
        this.saveFileBigMesh(bigMeshFullPath, bigMesh);
        int hola = 0;
    }

    public boolean saveSeparatedTiles(ArrayList<GaiaMesh> separatedMeshes)
    {
        // save the 9 tiles.***
        int meshesCount = separatedMeshes.size();
        for(int i = 0; i < meshesCount; i++)
        {
            GaiaMesh mesh = separatedMeshes.get(i);
            GaiaTriangle triangle = mesh.triangles.get(0); // take the first triangle.***
            TileIndices tileIndices = triangle.ownerTile_tileIndices;
            String tileTempDirectory = this.manager.tileTempDirectory;
            String outputDirectory = this.manager.outputDirectory;
            String tileFilePath = TileWgs84Utils.getTileFilePath(tileIndices.X, tileIndices.Y, tileIndices.L);
            String tileFullPath = tileTempDirectory + "\\" + tileFilePath;

            try {
                saveFile(mesh, tileFullPath);

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private boolean mustDivideTriangle(GaiaTriangle triangle, double midLonDeg, double midLatDeg)
    {
        ArrayList<GaiaVertex> vertices = triangle.getVertices();
        int verticesCount = vertices.size();
        int vertexLeftSideCount = 0;
        int vertexRightSideCount = 0;
        int vertexUpsideCount = 0;
        int vertexDownsideCount = 0;
        int vertexCoincidentCount = 0;
        double vertexCoincidentError = this.manager.vertexCoincidentError;

        for(int i = 0; i < verticesCount; i++)
        {
            GaiaVertex vertex = vertices.get(i);
            double diffX = Math.abs(vertex.position.x - midLonDeg);
            double diffY = Math.abs(vertex.position.y - midLatDeg);
            if(diffX < vertexCoincidentError || diffY < vertexCoincidentError)
            {
                // the vertex is coincident with midLon or midLat.***
                vertexCoincidentCount++;
                continue;
            }

            if(vertexCoincidentCount > 1)
            {
                return false;
            }

            if(vertex.position.x < midLonDeg)
            {
                vertexLeftSideCount++;
            }
            else
            {
                vertexRightSideCount++;
            }

            if(vertex.position.y < midLatDeg)
            {
                vertexDownsideCount++;
            }
            else
            {
                vertexUpsideCount++;
            }
        }

        if(vertexLeftSideCount > 0 && vertexRightSideCount > 0)
        {
            return true;
        }

        if(vertexUpsideCount > 0 && vertexDownsideCount > 0)
        {
            return true;
        }

        return false;
    }

    public boolean mustRefineTriangle(GaiaTriangle triangle) throws TransformException {
        if(triangle.refineChecked)
        {
            System.out.println("SUPER-FAST-Check : false");
            return false;
        }

        TerrainElevationData terrainElevationData = this.manager.terrainElevationData;
        Vector2d pixelSizeDeg = terrainElevationData.getPixelSizeDegree();

        // check if the triangle must be refined.***
        GaiaBoundingBox bboxTriangle = triangle.getBoundingBox();
        double widthDeg = bboxTriangle.getLengthX();
        double heightDeg = bboxTriangle.getLengthY();

        double maxDiff = TileWgs84Utils.getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(triangle.ownerTile_tileIndices.L);

        // fast check.******************************************************************
        // check the barycenter of the triangle.***
        Vector3d barycenter = triangle.getBarycenter();
        double elevation = terrainElevationData.getElevation(barycenter.x, barycenter.y);
        double planeElevation = barycenter.z;

        if(abs(elevation - planeElevation) > maxDiff)
        {
            System.out.println("FAST-Check : true");
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

        // Another fast check.*************************************************************
        if(mustDivideTriangleByMidLongitudeAndMidLatitude(triangle, this.geographicExtension))
        {
            System.out.println("ANOTHER-FAST-Check : true*******************************************");
            return true;
        }
        // end another fast check.*********************************************************



        double pixelSizeX = Math.max(pixelSizeDeg.x, widthDeg / 500.0);
        double pixelSizeY = Math.max(pixelSizeDeg.y, heightDeg / 500.0);

        GaiaPlane plane = triangle.getPlane();

        int columnsCount = (int)(widthDeg / pixelSizeX);
        int rowsCount = (int)(heightDeg / pixelSizeY);

        //System.out.println("Col count : " + columnsCount + " , Row count : " + rowsCount);


        double bbox_minX = bboxTriangle.getMinX();
        double bbox_minY = bboxTriangle.getMinY();

        boolean intersects = false;
        int counter = 0;
        for(int row = 0; row < rowsCount; row++)
        {
            double pos_y = bbox_minY + row * pixelSizeY;
            for(int column = 0; column < columnsCount; column++)
            {
                double pos_x = bbox_minX + column * pixelSizeX;
                intersects = triangle.intersectsPointXY(pos_x, pos_y);
                counter++;

                if(!intersects)
                {
                    continue;
                }

                elevation = terrainElevationData.getElevation(pos_x, pos_y);
                planeElevation = plane.getValueZ(pos_x, pos_y);

                if(abs(elevation - planeElevation) > maxDiff)
                {
                    System.out.println("SLOW-Check : true" + " , counter : " + counter);
                    return true;
                }
            }
        }

        System.out.println("SLOW-Check : false");
        triangle.refineChecked = true;
        return false;
    }

    public boolean mustDivideTriangleByMidLongitudeAndMidLatitude(GaiaTriangle triangle)
    {
        GeographicExtension geoExtent = this.geographicExtension;

        double lonDegRange = geoExtent.getLongitudeRangeDegree();
        double latDegRange = geoExtent.getLatitudeRangeDegree();

        GaiaBoundingBox bboxTriangle = triangle.getBoundingBox();

        double bboxLengthX = bboxTriangle.getLengthX();
        double bboxLengthY = bboxTriangle.getLengthY();
        double lonError = lonDegRange*0.1;
        double latError = latDegRange*0.1;

        double lonDiff = abs(lonDegRange*0.5 - bboxLengthX);
        double latDiff = abs(latDegRange*0.5 - bboxLengthY);

        if(abs(lonDiff) < lonError || abs(latDiff) < latError)
        {
            double midLonDeg = geoExtent.getMidLongitudeDeg();
            double midLatDeg = geoExtent.getMidLatitudeDeg();
            if(mustDivideTriangle(triangle, midLonDeg, midLatDeg))
            {
                return true;
            }
        }

        return false;
    }

    public boolean mustDivideTriangleByMidLongitudeAndMidLatitude(GaiaTriangle triangle, GeographicExtension geoExtent)
    {
        double lonDegRange = geoExtent.getLongitudeRangeDegree();
        double latDegRange = geoExtent.getLatitudeRangeDegree();

        GaiaBoundingBox bboxTriangle = triangle.getBoundingBox();

        double bboxLengthX = bboxTriangle.getLengthX();
        double bboxLengthY = bboxTriangle.getLengthY();
        double lonError = lonDegRange*0.1;
        double latError = latDegRange*0.1;

        double lonDiff = abs(lonDegRange - bboxLengthX);
        double latDiff = abs(latDegRange - bboxLengthY);

        if(lonDiff < lonError || latDiff < latError)
        {
            double midLonDeg = geoExtent.getMidLongitudeDeg();
            double midLatDeg = geoExtent.getMidLatitudeDeg();
            if(mustDivideTriangle(triangle, midLonDeg, midLatDeg))
            {
                return true;
            }
        }

        return false;
    }

    private boolean refineMeshOneIteration(GaiaMesh mesh, TileIndices currTileIndices) throws TransformException {
        // Inside the mesh, there are triangles of 9 different tiles.***
        // Here refine only the triangles of the current tile.***

        // refine the mesh.***
        boolean refined = false;
        int splitCount = 0;
        int trianglesCount = mesh.triangles.size();
        System.out.println("Triangles count : " + trianglesCount);
        splitCount = 0;
        for (int i = 0; i < trianglesCount; i++) {
            GaiaTriangle triangle = mesh.triangles.get(i);

            if (triangle.objectStatus == GaiaObjectStatus.DELETED) {
                continue;
            }

            if (!triangle.ownerTile_tileIndices.isCoincident(currTileIndices))
            {

                int L = triangle.ownerTile_tileIndices.L;
                int X = triangle.ownerTile_tileIndices.X;
                int Y = triangle.ownerTile_tileIndices.Y;
                GeographicExtension geoExtent = TileWgs84Utils.getGeographicExtentOfTileLXY(L, X, Y, null, this.manager.imageryType);
                if(mustDivideTriangleByMidLongitudeAndMidLatitude(triangle, geoExtent))
                {
                    TerrainElevationData terrainElevationData = this.manager.terrainElevationData;
                    ArrayList<GaiaTriangle> splitTriangles = mesh.splitTriangle(triangle, terrainElevationData);

                    if (splitTriangles.size() > 0)
                    {
                        splitCount++;
                        refined = true;
                    }
                }

                continue;
            }

            if (mustRefineTriangle(triangle))
            {
                TerrainElevationData terrainElevationData = this.manager.terrainElevationData;
                ArrayList<GaiaTriangle> splitTriangles = mesh.splitTriangle(triangle, terrainElevationData);

                if (splitTriangles.size() > 0)
                {
                    splitCount++;
                    refined = true;
                }
            }

        }

        if(refined)
        {
            mesh.removeDeletedObjects();
            mesh.setObjectsIdInList();
        }


        return refined;
    }

    public void refineMesh(GaiaMesh mesh, TileIndices currTileIndices) throws TransformException {
        // Inside the mesh, there are triangles of 9 different tiles.***
        // Here refine only the triangles of the current tile.***

        // refine the mesh.***
        boolean finished = false;
        int splitCount = 0;
        int maxIterations = this.manager.triangleRefinementMaxIterations;
        while(!finished) {
            System.out.println("iteration : " + splitCount + " : L : " + currTileIndices.L );
            if(!this.refineMeshOneIteration(mesh, currTileIndices))
            {
                finished = true;
            }
            else
            {
                splitCount++;
            }

            if(splitCount >= maxIterations)
            {
                finished = true;
            }
        }

        int hola = 0;
    }

    public void loadTileAndSave4Children(TileIndices tileIndices) throws IOException, TransformException {
        //******************************************
        // Function used when save 4 child tiles.***
        //******************************************

        // 1- load the tile.***
        // 2- make the 4 children.***
        // 3- save the 4 children.***
        // 4- delete the tile.***

        String tilePath = this.manager.getTilePath(tileIndices);

        // 1- load the tile.***
        this.loadFile(tilePath);

        // 2- make the 4 children.***
        TileIndices child_LU_TileIndices = tileIndices.getChild_LU_TileIndices();
        TileIndices child_RU_TileIndices = tileIndices.getChild_RU_TileIndices();
        TileIndices child_LD_TileIndices = tileIndices.getChild_LD_TileIndices();
        TileIndices child_RD_TileIndices = tileIndices.getChild_RD_TileIndices();

        // 1rst, classify the triangles of the tile.***
        double midLonDeg = this.geographicExtension.getMidLongitudeDeg();
        double midLatDeg = this.geographicExtension.getMidLatitudeDeg();
        ArrayList<GaiaTriangle> triangles = this.mesh.triangles;
        int trianglesCount = triangles.size();
        for(int i = 0; i < trianglesCount; i++)
        {
            GaiaTriangle triangle = triangles.get(i);

            if(triangle.objectStatus == GaiaObjectStatus.DELETED)
            {
                continue;
            }

            Vector3d barycenter = triangle.getBarycenter();
            if(barycenter.x < midLonDeg)
            {
                if(barycenter.y < midLatDeg)
                {
                    // LD_Tile
                    triangle.ownerTile_tileIndices = child_LD_TileIndices;
                }
                else
                {
                    // LU_Tile
                    triangle.ownerTile_tileIndices = child_LU_TileIndices;
                }
            }
            else
            {
                if(barycenter.y < midLatDeg)
                {
                    // RD_Tile
                    triangle.ownerTile_tileIndices = child_RD_TileIndices;
                }
                else
                {
                    // RU_Tile
                    triangle.ownerTile_tileIndices = child_RU_TileIndices;
                }
            }
        }

        TileMerger3x3 tileMerger3x3 = new TileMerger3x3();
        ArrayList<GaiaMesh>childMeshes = new ArrayList<>();
        tileMerger3x3.getSeparatedMeshes(this.mesh, childMeshes);

        // 3- save the 4 children.***
        int childMeshesCount = childMeshes.size();
        for(int i = 0; i < childMeshesCount; i++)
        {
            GaiaMesh childMesh = childMeshes.get(i);
            GaiaTriangle triangle = childMesh.triangles.get(0); // take the first triangle.***
            TileIndices childTileIndices = triangle.ownerTile_tileIndices;
            String tileTempDirectory = this.manager.tileTempDirectory;
            String outputDirectory = this.manager.outputDirectory;
            String childTileFilePath = TileWgs84Utils.getTileFilePath(childTileIndices.X, childTileIndices.Y, childTileIndices.L);
            String childTileFullPath = tileTempDirectory + "\\" + childTileFilePath;

            try {
                saveFile(childMesh, childTileFullPath);

            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        // 4- delete the tile.***
        //tile.deleteFile(tileIndices);
    }


}
