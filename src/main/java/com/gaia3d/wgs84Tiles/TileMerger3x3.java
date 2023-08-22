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
                    // NOTE : for outingHEdges of startVertex2, must set "startVertex" the endVertex of halfEdge.***
                    GaiaHalfEdge outingHalfEdge = outingHalfEdges_strVertex2.get(j);
                    outingHalfEdge.setStartVertex(endVertex);
                }

                int outingHalfEdges_endVertex2_count = outingHalfEdges_endVertex2.size();
                for(int j=0; j<outingHalfEdges_endVertex2_count; j++)
                {
                    // NOTE : for outingHEdges of endVertex2, must set "startVertex" the startVertex of halfEdge.***
                    GaiaHalfEdge outingHalfEdge = outingHalfEdges_endVertex2.get(j);
                    outingHalfEdge.setStartVertex(startVertex);
                }

                // finally set twins.***
                halfEdge.setTwin(halfEdge2);

                // now, set as deleted the startVertex2 & endVertex2.***
                if(!startVertex2.equals(endVertex))
                {
                    startVertex2.objectStatus = GaiaObjectStatus.DELETED;
                }

                if(!endVertex2.equals(startVertex))
                {
                    endVertex2.objectStatus = GaiaObjectStatus.DELETED;
                }

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
            if(halfEdge.twin != null)
            {
                // this halfEdge has a twin.***
                continue;
            }

            if(!this.setTwinHalfEdgeWithHalfEdgesList(halfEdge, listHEdges_B))
            {
                // system.out.println("Error: no twin halfEdge found.");
                // error.!***
                int hola = 0;
            }
        }
    }

    public GaiaMesh getMergedMesh_3x1(TileWgs84 L_Tile, TileWgs84 C_Tile, TileWgs84 R_Tile)
    {
        // given 3 tiles in row, merge them.***
        //  +----------+----------+----------+
        //  |          |          |          |
        //  | L_Tile   |  C_Tile  | R_Tile   |
        //  |          |          |          |
        //  +----------+----------+----------+

        GaiaMesh resultMergedMesh = C_Tile.mesh;

        if(L_Tile != null)
        {
            //  +----------+----------+
            //  |          |          |
            //  | L_mesh   | result_m |
            //  |          |          |
            //  +----------+----------+

            // in this case, join halfEdges of the right side of the left tile with the left side of the result mesh.
            GaiaMesh L_mesh = L_Tile.mesh;
            ArrayList<GaiaHalfEdge> L_mesh_right_halfEdges = L_mesh.getHalfEdgesByType(HalfEdgeType.RIGHT);
            ArrayList<GaiaHalfEdge> result_mesh_left_halfEdges = resultMergedMesh.getHalfEdgesByType(HalfEdgeType.LEFT);

            // now, set twins of halfEdges.***
            this.setTwinsBetweenHalfEdges(L_mesh_right_halfEdges, result_mesh_left_halfEdges);

            // now, merge the left tile mesh to the result mesh.
            resultMergedMesh.removeDeletedObjects();
            resultMergedMesh.mergeMesh(L_mesh);
        }

        // 3rd, merge the right tile mesh to the result mesh.
        if(R_Tile != null)
        {
            // now merge the right tile mesh to the result mesh.
            //  +---------------------+----------+
            //  |                     |          |
            //  | result_m            | R_mesh   |
            //  |                     |          |
            //  +---------------------+----------+

            // in this case, join halfEdges of the left side of the right tile with the right side of the result mesh.
            GaiaMesh R_mesh = R_Tile.mesh;
            ArrayList<GaiaHalfEdge> R_mesh_left_halfEdges = R_mesh.getHalfEdgesByType(HalfEdgeType.LEFT);
            ArrayList<GaiaHalfEdge> result_mesh_right_halfEdges = resultMergedMesh.getHalfEdgesByType(HalfEdgeType.RIGHT);

            // now, set twins of halfEdges.***
            this.setTwinsBetweenHalfEdges(R_mesh_left_halfEdges, result_mesh_right_halfEdges);

            // now, merge the right tile mesh to the result mesh.
            resultMergedMesh.removeDeletedObjects();
            resultMergedMesh.mergeMesh(R_mesh);
        }

        return resultMergedMesh;
    }

    public GaiaMesh getMergedMesh_1x3(GaiaMesh U_mesh, GaiaMesh C_mesh, GaiaMesh D_mesh)
    {
        // given 3 tiles in column, merge them.***
        //  +----------+
        //  |          |
        //  | U_Tile   |
        //  |          |
        //  +----------+
        //  |          |
        //  | C_Tile   |
        //  |          |
        //  +----------+
        //  |          |
        //  | D_Tile   |
        //  |          |
        //  +----------+

        GaiaMesh resultMergedMesh = C_mesh;

        if(U_mesh != null)
        {
            //  +----------+
            //  |          |
            //  | U_Tile   |
            //  |          |
            //  +----------+
            //  |          |
            //  | result_m |
            //  |          |
            //  +----------+

            // in this case, join halfEdges of the down side of the up tile with the up side of the result mesh.
            ArrayList<GaiaHalfEdge> U_mesh_down_halfEdges = U_mesh.getHalfEdgesByType(HalfEdgeType.DOWN);
            ArrayList<GaiaHalfEdge> result_mesh_up_halfEdges = resultMergedMesh.getHalfEdgesByType(HalfEdgeType.UP);

            // now, set twins of halfEdges.***
            this.setTwinsBetweenHalfEdges(U_mesh_down_halfEdges, result_mesh_up_halfEdges);

            // now, merge the up tile mesh to the result mesh.
            resultMergedMesh.removeDeletedObjects();
            resultMergedMesh.mergeMesh(U_mesh);
        }

        if(D_mesh != null)
        {
            //  +----------+
            //  |          |
            //  | result_m |
            //  |          |
            //  +----------+
            //  |          |
            //  | D_Tile   |
            //  |          |
            //  +----------+

            // in this case, join halfEdges of the up side of the down tile with the down side of the result mesh.
            ArrayList<GaiaHalfEdge> D_mesh_up_halfEdges = D_mesh.getHalfEdgesByType(HalfEdgeType.UP);
            ArrayList<GaiaHalfEdge> result_mesh_down_halfEdges = resultMergedMesh.getHalfEdgesByType(HalfEdgeType.DOWN);

            // now, set twins of halfEdges.***
            this.setTwinsBetweenHalfEdges(D_mesh_up_halfEdges, result_mesh_down_halfEdges);

            // now, merge the down tile mesh to the result mesh.
            resultMergedMesh.removeDeletedObjects();
            resultMergedMesh.mergeMesh(D_mesh);
        }



        return resultMergedMesh;
    }

    public GaiaMesh getMergedMesh()
    {
        // 1rst, copy the center tile mesh to the result mesh.
        GaiaMesh resultMergedMesh_up = this.getMergedMesh_3x1(left_up_tile, up_tile, right_up_tile);
        GaiaMesh resultMergedMesh = this.getMergedMesh_3x1(left_tile, center_tile, right_tile);
        GaiaMesh resultMergedMesh_down = this.getMergedMesh_3x1(left_down_tile, down_tile, right_down_tile);

        // 2nd, merge the up & down meshes to the result mesh.
        resultMergedMesh = this.getMergedMesh_1x3(resultMergedMesh_up, resultMergedMesh, resultMergedMesh_down);


        return resultMergedMesh;
    }

}
