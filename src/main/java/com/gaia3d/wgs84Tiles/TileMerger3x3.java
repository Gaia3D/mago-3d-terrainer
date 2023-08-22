package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.*;

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

    double vertexCoincidentError = 0.0000000000001;

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

    private boolean setTwinHalfEdgeWithHalfEdgesList(GaiaHalfEdge halfEdge, ArrayList<GaiaHalfEdge> halfEdgesList)
    {
        int halfEdgesList_count = halfEdgesList.size();
        for(int i=0; i<halfEdgesList_count; i++)
        {
            GaiaHalfEdge halfEdge2 = halfEdgesList.get(i);

            if(halfEdge2.twin != null)
            {
                // this halfEdge2 has a twin.***
                continue;
            }

            if(halfEdge.isHalfEdgePossibleTwin(halfEdge2, vertexCoincidentError))
            {
                // 1rst, must change the startVertex & endVertex of the halfEdge2.***
                GaiaVertex startVertex = halfEdge.getStartVertex();
                GaiaVertex endVertex = halfEdge.getEndVertex();

                GaiaVertex startVertex2 = halfEdge2.getStartVertex();
                GaiaVertex endVertex2 = halfEdge2.getEndVertex();

                ArrayList<GaiaHalfEdge> outingHalfEdges_strVertex2 = startVertex2.getAllOutingHalfEdges();
                ArrayList<GaiaHalfEdge> outingHalfEdges_endVertex2 = endVertex2.getAllOutingHalfEdges();

                int outingHalfEdges_strVertex2_count = outingHalfEdges_strVertex2.size();
                for(int j=0; j<outingHalfEdges_strVertex2_count; j++)
                {
                    GaiaHalfEdge outingHalfEdge = outingHalfEdges_strVertex2.get(j);
                    outingHalfEdge.setStartVertex(endVertex);
                }

                int outingHalfEdges_endVertex2_count = outingHalfEdges_endVertex2.size();
                for(int j=0; j<outingHalfEdges_endVertex2_count; j++)
                {
                    GaiaHalfEdge outingHalfEdge = outingHalfEdges_endVertex2.get(j);
                    outingHalfEdge.setStartVertex(startVertex);
                }

                // finally set twins.***
                halfEdge.setTwin(halfEdge2);

                // now, set as deleted the startVertex2 & endVertex2.***
                startVertex2.objectStatus = GaiaObjectStatus.DELETED;
                endVertex2.objectStatus = GaiaObjectStatus.DELETED;

                return true;
            }
        }
        return false;
    }

    private void setTwinsBetweenHalfEdges(ArrayList<GaiaHalfEdge> listHEdges_A, ArrayList<GaiaHalfEdge> listHEdges_B)
    {
        int listHEdges_A_count = listHEdges_A.size();
        for(int i=0; i<listHEdges_A_count; i++)
        {
            GaiaHalfEdge halfEdge = listHEdges_A.get(i);
            if(!this.setTwinHalfEdgeWithHalfEdgesList(halfEdge, listHEdges_B))
            {
                // system.out.println("Error: no twin halfEdge found.");
                // error.!***
                int hola = 0;
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

            // now, merge the left tile mesh to the result mesh.
            resultMergedMesh.removeDeletedObjects();
            resultMergedMesh.mergeMesh(L_mesh);

            //resultMergedMesh = resultMergedMesh.merge(left_tile.mesh);
            int hola = 0;
        }

        return resultMergedMesh;
    }

}
