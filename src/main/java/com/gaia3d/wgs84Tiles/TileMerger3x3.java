package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GaiaHalfEdge;
import com.gaia3d.basic.structure.GaiaMesh;
import com.gaia3d.basic.structure.HalfEdgeType;

import java.util.ArrayList;

public class TileMerger3x3 {

    //  +----------+----------+----------+
    //  |          |          |          |
    //  | LU_Tile  |  U_Tile  | RU_Tile  |
    //  |          |          |          |
    //  +----------+----------+----------+
    //  |          |          |          |
    //  | L_Tile   |cent_Tile | R_Tile   |
    //  |          |          |          |
    //  +----------+----------+----------+
    //  |          |          |          |
    //  | LD_Tile  | D_Tile   | RD_Tile  |
    //  |          |          |          |
    //  +----------+----------+----------+
    TileWgs84 center_tile = null;
    TileWgs84 left_tile = null;
    TileWgs84 right_tile = null;
    TileWgs84 up_tile = null;
    TileWgs84 down_tile = null;

    TileWgs84 left_up_tile = null;
    TileWgs84 right_up_tile = null;
    TileWgs84 left_down_tile = null;
    TileWgs84 right_down_tile = null;

    public TileMerger3x3(TileWgs84 center_tile, TileWgs84 left_tile, TileWgs84 right_tile,
                         TileWgs84 up_tile, TileWgs84 down_tile, TileWgs84 left_up_tile,
                         TileWgs84 right_up_tile, TileWgs84 left_down_tile, TileWgs84 right_down_tile) {
        this.center_tile = center_tile;
        this.left_tile = left_tile;
        this.right_tile = right_tile;
        this.up_tile = up_tile;
        this.down_tile = down_tile;
        this.left_up_tile = left_up_tile;
        this.right_up_tile = right_up_tile;
        this.left_down_tile = left_down_tile;
        this.right_down_tile = right_down_tile;
    }

    private void setTwinsBetweenHalfEdges(ArrayList<GaiaHalfEdge> listHEdges_A, ArrayList<GaiaHalfEdge> listHEdges_B)
    {
        int listHEdges_A_count = listHEdges_A.size();
        int listHEdges_B_count = listHEdges_B.size();
        for(int i=0; i<listHEdges_A_count; i++)
        {
            GaiaHalfEdge halfEdge_A = listHEdges_A.get(i);
            for(int j=0; j<listHEdges_B_count; j++)
            {
                GaiaHalfEdge halfEdge_B = listHEdges_B.get(j);
                if(halfEdge_A.isHalfEdgePossibleTwin(halfEdge_B))
                {
                    halfEdge_A.setTwin(halfEdge_B);
                    break;
                }
            }
        }
    }

    public GaiaMesh getMergedMesh()
    {
        // 1rst, copy the center tile mesh to the result mesh.
        GaiaMesh resultMergedMesh = center_tile.mesh;

        // 2nd, merge the left tile mesh to the result mesh.
        if(left_tile != null)
        {
            //  +----------+----------+
            //  |          |          |
            //  | L_mesh   | result_m |
            //  |          |          |
            //  +----------+----------+

            // in this case, join halfEdges of the right side of the left tile with the left side of the result mesh.
            GaiaMesh L_mesh = left_tile.mesh;
            HalfEdgeType L_mesh_right_halfEdge = HalfEdgeType.RIGHT;
            HalfEdgeType result_mesh_left_halfEdge = HalfEdgeType.LEFT;
            ArrayList<GaiaHalfEdge> L_mesh_right_halfEdges = L_mesh.getHalfEdgesByType(L_mesh_right_halfEdge);
            ArrayList<GaiaHalfEdge> result_mesh_left_halfEdges = resultMergedMesh.getHalfEdgesByType(result_mesh_left_halfEdge);

            // now, set twins of halfEdges.***
            this.setTwinsBetweenHalfEdges(L_mesh_right_halfEdges, result_mesh_left_halfEdges);

            //resultMergedMesh = resultMergedMesh.merge(left_tile.mesh);

        }

        return resultMergedMesh;
    }

}
