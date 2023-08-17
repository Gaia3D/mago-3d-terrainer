package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.*;
import org.joml.Vector3d;

import java.util.ArrayList;

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
    //  |  LUTile  |   UTile  |  RUTile  |
    //  |          |          |          |
    //  +----------+----------+----------+
    //  |          |          |          |
    //  |  LTile   | currTile |  RTile   |
    //  |          |          |          |
    //  +----------+----------+----------+
    //  |          |          |          |
    //  |  LDTile  |  DTile   |  RDTile  |
    //  |          |          |          |
    //  +----------+----------+----------+

    public TileWgs84[] neighborTiles = new TileWgs84[8];
    // neighborTiles[0] = LDTile
    // neighborTiles[1] = DTile
    // neighborTiles[2] = RDTile
    // neighborTiles[3] = RTile
    // neighborTiles[4] = RUTile
    // neighborTiles[5] = UTile
    // neighborTiles[6] = LUTile
    // neighborTiles[7] = LTile


    public TileWgs84[] childTiles = new TileWgs84[4];

    public TileWgs84(TileWgs84 parentTile, TileWgs84Manager manager) {
        this.parentTile = parentTile;
        this.manager = manager;
    }

    public void createInitialMesh()
    {
        // The initial mesh consists in 4 vertex & 2 triangles.***
        this.mesh = new GaiaMesh();

        GaiaVertex vertexLD = this.mesh.newVertex();
        GaiaVertex vertexRD = this.mesh.newVertex();
        GaiaVertex vertexRU = this.mesh.newVertex();
        GaiaVertex vertexLU = this.mesh.newVertex();

        vertexLD.position = new Vector3d(this.geographicExtension.getMinLongitudeDeg(), this.geographicExtension.getMinLatitudeDeg(), 0.0);
        vertexRD.position = new Vector3d(this.geographicExtension.getMaxLongitudeDeg(), this.geographicExtension.getMinLatitudeDeg(), 0.0);
        vertexRU.position = new Vector3d(this.geographicExtension.getMaxLongitudeDeg(), this.geographicExtension.getMaxLatitudeDeg(), 0.0);
        vertexLU.position = new Vector3d(this.geographicExtension.getMinLongitudeDeg(), this.geographicExtension.getMaxLatitudeDeg(), 0.0);


        // create the 2 triangles.***
        // he = halfEdge.***
        //   +---------------------+
        //   |        he2        / |
        //   |                 /   |
        //   |      T2       /     |
        //   |             /       |
        //   | he3       /         |
        //   |     he1 /       he2 |
        //   |       /he3          |
        //   |     /     T1        |
        //   |   /                 |
        //   | /    he1            |
        //   +---------------------+

        GaiaTriangle triangle1 = this.mesh.newTriangle();
        GaiaTriangle triangle2 = this.mesh.newTriangle();

        // Triangle 1.***
        GaiaHalfEdge halfEdge_T1_1 = this.mesh.newHalfEdge();
        GaiaHalfEdge halfEdge_T1_2 = this.mesh.newHalfEdge();
        GaiaHalfEdge halfEdge_T1_3 = this.mesh.newHalfEdge(); // twin of halfEdge_T2_1.***

        halfEdge_T1_1.setStartVertex(vertexLD);
        halfEdge_T1_2.setStartVertex(vertexRD);
        halfEdge_T1_3.setStartVertex(vertexRU);

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
        halfEdge_T2_2.setStartVertex(vertexRU);
        halfEdge_T2_3.setStartVertex(vertexLU);

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

    }

    public void makeMesh()
    {
        // 1rst, check if exist neighbor tiles.
        int currDepth = this.tileIndices.L;
        int currX = this.tileIndices.X;
        int currY = this.tileIndices.Y;

        String tileTempDirectory = this.manager.tileTempDirectory;
        String outputDirectory = this.manager.outputDirectory;

        // check if exist LDTileFile.***
        TileIndices LD_TileIndices = this.tileIndices.get_LD_TileIndices();
        String LDTileFilePath = TileWgs84Utils.getTileFilePath(LD_TileIndices.X, LD_TileIndices.Y, LD_TileIndices.L);
        String LDTileFullPath = tileTempDirectory + "\\" + LDTileFilePath;



        int hola = 0;
    }

}
