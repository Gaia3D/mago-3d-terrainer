package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.*;
import com.gaia3d.util.io.LittleEndianDataInputStream;
import com.gaia3d.util.io.LittleEndianDataOutputStream;
import com.gaia3d.reader.FileUtils;
import org.joml.Vector3d;

import java.io.*;
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

    public void saveFile(String filePath) throws IOException {
        String foldersPath = FileUtils.removeFileNameFromPath(filePath);
        if(!FileUtils.createAllFoldersIfNoExist(foldersPath))
        {
            return;
        }

        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        LittleEndianDataOutputStream dataOutputStream = new LittleEndianDataOutputStream(fileOutputStream);

        // delete the file if exists before save.***
        FileUtils.deleteFileIfExists(filePath);

        // save the tile.***
        this.mesh.saveDataOutputStream(dataOutputStream);
    }

    public void loadFile(String filePath) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(filePath);
        LittleEndianDataInputStream dataInputStream = new LittleEndianDataInputStream(fileInputStream);

        this.mesh = new GaiaMesh();
        this.mesh.loadDataInputStream(dataInputStream);
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

        // the 2 triangles have the same ownerTile.***
        triangle1.ownerTile_tileIndices.copyFrom(this.tileIndices);
        triangle2.ownerTile_tileIndices.copyFrom(this.tileIndices);

        // now set objects id in list.***
        this.mesh.setObjectsIdInList();

    }



    public void makeBigMesh() throws IOException {

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

        GaiaMesh curr_Mesh = this.mesh;

        TileIndices LD_TileIndices = this.tileIndices.get_LD_TileIndices();
        TileWgs84 LD_tile = this.manager.loadOrCreateTileWgs84(LD_TileIndices);

        TileIndices D_TileIndices = this.tileIndices.get_D_TileIndices();
        TileWgs84 D_tile = this.manager.loadOrCreateTileWgs84(D_TileIndices);

        TileIndices RD_TileIndices = this.tileIndices.get_RD_TileIndices();
        TileWgs84 RD_tile = this.manager.loadOrCreateTileWgs84(RD_TileIndices);

        TileIndices L_TileIndices = this.tileIndices.get_L_TileIndices();
        TileWgs84 L_tile = this.manager.loadOrCreateTileWgs84(L_TileIndices);

        TileIndices R_TileIndices = this.tileIndices.get_R_TileIndices();
        TileWgs84 R_tile = this.manager.loadOrCreateTileWgs84(R_TileIndices);

        TileIndices LU_TileIndices = this.tileIndices.get_LU_TileIndices();
        TileWgs84 LU_tile = this.manager.loadOrCreateTileWgs84(LU_TileIndices);

        TileIndices U_TileIndices = this.tileIndices.get_U_TileIndices();
        TileWgs84 U_tile = this.manager.loadOrCreateTileWgs84(U_TileIndices);

        TileIndices RU_TileIndices = this.tileIndices.get_RU_TileIndices();
        TileWgs84 RU_tile = this.manager.loadOrCreateTileWgs84(RU_TileIndices);

        // now make the bigMesh.***
        TileWgs84 bigTile3x3 = new TileWgs84(null, this.manager);


        int hola = 0;
    }

}
