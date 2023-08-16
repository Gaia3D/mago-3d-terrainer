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
}
