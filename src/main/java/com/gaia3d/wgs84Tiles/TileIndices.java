package com.gaia3d.wgs84Tiles;

import com.gaia3d.util.io.LittleEndianDataInputStream;
import com.gaia3d.util.io.LittleEndianDataOutputStream;

import java.io.IOException;

public class TileIndices {
    int X = 0;
    int Y = 0;
    int L = 0; // tile depth.

    public void set(int x, int y, int l) {
        X = x;
        Y = y;
        L = l;
    }

    //*************************************************
    // note : the origin of the tile system is left-up.
    //*************************************************

    public void copyFrom(TileIndices tileIndices) {
        X = tileIndices.X;
        Y = tileIndices.Y;
        L = tileIndices.L;
    }

    public void saveDataOutputStream(LittleEndianDataOutputStream dataOutputStream)
    {
        try {
            dataOutputStream.writeInt(X);
            dataOutputStream.writeInt(Y);
            dataOutputStream.writeInt(L);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadDataInputStream(LittleEndianDataInputStream dataInputStream) throws IOException
    {
        X = dataInputStream.readInt();
        Y = dataInputStream.readInt();
        L = dataInputStream.readInt();
    }

    public TileIndices get_LD_TileIndices() {
        TileIndices tileIndices = new TileIndices();
        tileIndices.set(X - 1, Y + 1, L);
        return tileIndices;
    }

    public TileIndices get_D_TileIndices() {
        TileIndices tileIndices = new TileIndices();
        tileIndices.set(X, Y + 1, L);
        return tileIndices;
    }

    public TileIndices get_RD_TileIndices() {
        TileIndices tileIndices = new TileIndices();
        tileIndices.set(X + 1, Y + 1, L);
        return tileIndices;
    }

    public TileIndices get_R_TileIndices() {
        TileIndices tileIndices = new TileIndices();
        tileIndices.set(X + 1, Y, L);
        return tileIndices;
    }

    public TileIndices get_RU_TileIndices() {
        TileIndices tileIndices = new TileIndices();
        tileIndices.set(X + 1, Y - 1, L);
        return tileIndices;
    }

    public TileIndices get_U_TileIndices() {
        TileIndices tileIndices = new TileIndices();
        tileIndices.set(X, Y - 1, L);
        return tileIndices;
    }

    public TileIndices get_LU_TileIndices() {
        TileIndices tileIndices = new TileIndices();
        tileIndices.set(X - 1, Y - 1, L);
        return tileIndices;
    }

    public TileIndices get_L_TileIndices() {
        TileIndices tileIndices = new TileIndices();
        tileIndices.set(X - 1, Y, L);
        return tileIndices;
    }


}
