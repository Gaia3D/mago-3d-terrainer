package com.gaia3d.wgs84Tiles;

import com.gaia3d.util.io.BigEndianDataInputStream;
import com.gaia3d.util.io.BigEndianDataOutputStream;

import java.io.IOException;

public class TileIndices {

    // child tile indices.***
    //    +--------+--------+
    //    |        |        |
    //    |   LU   |   RU   |
    //    |        |        |
    //    +--------+--------+
    //    |        |        |
    //    |   LD   |   RD   |
    //    |        |        |
    //    +--------+--------+

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

    public boolean isCoincident(TileIndices tileIndices) {
        if (X == tileIndices.X && Y == tileIndices.Y && L == tileIndices.L) {
            return true;
        }
        return false;
    }

    public void saveDataOutputStream(BigEndianDataOutputStream dataOutputStream)
    {
        try {
            dataOutputStream.writeInt(X);
            dataOutputStream.writeInt(Y);
            dataOutputStream.writeInt(L);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadDataInputStream(BigEndianDataInputStream dataInputStream) throws IOException
    {
        X = dataInputStream.readInt();
        Y = dataInputStream.readInt();
        L = dataInputStream.readInt();
    }

    public boolean isValid()
    {
        // for each tile depth (L), there are minX & maxX, minY & maxY.***
        return TileWgs84Utils.isValidTileIndices(L, X, Y);
    }

    public TileIndices get_LD_TileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if(originIsLeftUp)
        {
            tileIndices.set(X - 1, Y + 1, L);
        }
        else {
            tileIndices.set(X - 1, Y - 1, L);
        }

        return tileIndices;
    }

    public TileIndices get_D_TileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if(originIsLeftUp)
        {
            tileIndices.set(X, Y + 1, L);
        }
        else {
            tileIndices.set(X, Y - 1, L);
        }


        return tileIndices;
    }

    public TileIndices get_RD_TileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if(originIsLeftUp)
        {
            tileIndices.set(X + 1, Y + 1, L);
        }
        else {
            tileIndices.set(X + 1, Y - 1, L);
        }

        return tileIndices;
    }

    public TileIndices get_R_TileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if(originIsLeftUp)
        {
            tileIndices.set(X + 1, Y, L);
        }
        else {
            tileIndices.set(X + 1, Y, L);
        }


        return tileIndices;
    }

    public TileIndices get_RU_TileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if(originIsLeftUp)
        {
            tileIndices.set(X + 1, Y - 1, L);
        }
        else {
            tileIndices.set(X + 1, Y + 1, L);
        }

        return tileIndices;
    }

    public TileIndices get_U_TileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if(originIsLeftUp)
        {
            tileIndices.set(X, Y - 1, L);
        }
        else {
            tileIndices.set(X, Y + 1, L);
        }

        return tileIndices;
    }

    public TileIndices get_LU_TileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if(originIsLeftUp)
        {
            tileIndices.set(X - 1, Y - 1, L);
        }
        else {
            tileIndices.set(X - 1, Y + 1, L);
        }

        return tileIndices;
    }

    public TileIndices get_L_TileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if(originIsLeftUp)
        {
            tileIndices.set(X - 1, Y, L);
        }
        else {
            tileIndices.set(X - 1, Y, L);
        }

        return tileIndices;
    }

    public TileIndices getChild_LU_TileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if(originIsLeftUp)
        {
            tileIndices.set(X * 2, Y * 2, L + 1);
        }
        else {
            tileIndices.set(X * 2, Y * 2 + 1, L + 1);
        }

        return tileIndices;
    }

    public TileIndices getChild_RU_TileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if(originIsLeftUp)
        {
            tileIndices.set(X * 2 + 1, Y * 2, L + 1);
        }
        else {
            tileIndices.set(X * 2 + 1, Y * 2 + 1, L + 1);
        }

        return tileIndices;
    }

    public TileIndices getChild_LD_TileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if(originIsLeftUp)
        {
            tileIndices.set(X * 2, Y * 2 + 1, L + 1);
        }
        else {
            tileIndices.set(X * 2, Y * 2, L + 1);
        }

        return tileIndices;
    }

    public TileIndices getChild_RD_TileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if(originIsLeftUp)
        {
            tileIndices.set(X * 2 + 1, Y * 2 + 1, L + 1);
        }
        else {
            tileIndices.set(X * 2 + 1, Y * 2, L + 1);
        }

        return tileIndices;
    }

    public String getString() {
        String result = "";
        result += "X : " + X + ", Y : " + Y + ", L : " + L;
        return result;
    }

}
