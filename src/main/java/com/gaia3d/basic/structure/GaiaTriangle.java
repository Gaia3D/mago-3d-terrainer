package com.gaia3d.basic.structure;

import com.gaia3d.util.io.LittleEndianDataInputStream;
import com.gaia3d.util.io.LittleEndianDataOutputStream;
import com.gaia3d.wgs84Tiles.TileIndices;

import java.io.IOException;

public class GaiaTriangle {
    public GaiaHalfEdge halfEdge = null;

    public int id = -1;

    public int halfEdgeId = -1;

    // this triangle belongs to a tile.***
    public TileIndices ownerTile_tileIndices = new TileIndices();
    public GaiaObjectStatus objectStatus = GaiaObjectStatus.ACTIVE;

    public void setHalfEdge(GaiaHalfEdge halfEdge) {
        this.halfEdge = halfEdge;
    }

    public void saveDataOutputStream(LittleEndianDataOutputStream dataOutputStream)
    {
        try {
            // 1rst, save id.***
            dataOutputStream.writeInt(id);

            // 2nd, save halfEdge.***
            if(halfEdge != null)
            {
                dataOutputStream.writeInt(halfEdge.id);
            }
            else
            {
                dataOutputStream.writeInt(-1);
            }

            // 3rd, save ownerTile_tileIndices.***
            if(ownerTile_tileIndices == null)
            {
                dataOutputStream.writeInt(-1);
                dataOutputStream.writeInt(-1);
                dataOutputStream.writeInt(-1);
            }
            else
            {
                ownerTile_tileIndices.saveDataOutputStream(dataOutputStream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadDataInputStream(LittleEndianDataInputStream dataInputStream) throws IOException
    {
        this.id = dataInputStream.readInt();
        this.halfEdgeId = dataInputStream.readInt();
        if(this.ownerTile_tileIndices == null)
        {
            this.ownerTile_tileIndices = new TileIndices();
        }
        this.ownerTile_tileIndices.loadDataInputStream(dataInputStream);
    }
}
