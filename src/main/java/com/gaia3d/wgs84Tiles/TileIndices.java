package com.gaia3d.wgs84Tiles;

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
