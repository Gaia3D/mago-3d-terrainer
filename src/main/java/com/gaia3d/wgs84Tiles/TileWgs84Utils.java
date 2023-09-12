package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GaiaMesh;
import com.gaia3d.basic.structure.GeographicExtension;
import com.gaia3d.reader.FileUtils;
import com.gaia3d.util.GlobeUtils;
import org.joml.Vector2d;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TileWgs84Utils {

    static public double getTileSizeInMetersByDepth(int depth)
    {
        double angDeg = TileWgs84Utils.selectTileAngleRangeByDepth(depth);
        double angRad = angDeg * Math.PI / 180.0;
        double equatorialRadius = GlobeUtils.getEquatorialRadius();
        double tileSizeInMeters = angRad * equatorialRadius;
        return tileSizeInMeters;
    }
    static public double getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(int depth)
    {
        double tileSize = TileWgs84Utils.getTileSizeInMetersByDepth(depth);
        return tileSize / 60.0;
    }

    static public double getMinTriangleSizeForTileDepth(int depth)
    {
        double tileSize = TileWgs84Utils.getTileSizeInMetersByDepth(depth);
        return tileSize / 60.0;
    }

    static public double getMaxTriangleSizeForTileDepth(int depth)
    {
        double tileSize = TileWgs84Utils.getTileSizeInMetersByDepth(depth);
        return tileSize / 10.0;
    }
    static public double selectTileAngleRangeByDepth(int depth)
    {
        // given tile depth, this function returns the latitude angle range of the tile
        if (depth < 0 || depth > 28)
        { return -1.0; }

        if (depth == 0)
        { return 180; }
        if (depth == 1)
        { return 90; }
        if (depth == 2)
        { return 45; }
        if (depth == 3)
        { return 22.5; }
        if (depth == 4)
        { return 11.25; }
        if (depth == 5)
        { return 5.625; }
        if (depth == 6)
        { return 2.8125; }
        if (depth == 7)
        { return 1.40625; }
        if (depth == 8)
        { return 0.703125; }
        if (depth == 9)
        { return 0.3515625; }
        if (depth == 10)
        { return 0.17578125; }
        if (depth == 11)
        { return 0.087890625; }
        if (depth == 12)
        { return 0.043945313; }
        if (depth == 13)
        { return 0.021972656; }
        if (depth == 14)
        { return 0.010986328; }
        if (depth == 15)
        { return 0.010986328/2.0; }
        if (depth == 16)
        { return 0.010986328 / 4.0; }
        if (depth == 17)
        { return 0.010986328 / 8.0; }
        if (depth == 18)
        { return 0.010986328 / 16.0; }
        if (depth == 19)
        { return 0.010986328 / 32.0; }
        if (depth == 20)
        { return 0.010986328 / 64.0; }
        if (depth == 21)
        { return 0.010986328 / 128.0; }
        if (depth == 22)
        { return 0.010986328 / 256.0; }
        if (depth == 23)
        { return 0.010986328 / 512.0; }
        if (depth == 24) // tile aprox 1m edge.***
        { return 0.010986328 / 1024.0; }
        if (depth == 25)
        { return 0.010986328 / 2048.0; }
        if (depth == 26)
        { return 0.010986328 / (2048.0 * 2.0); }
        if (depth == 27)
        { return 0.010986328 / (2048.0 * 4.0); }
        if (depth == 28)
        { return 0.010986328 / (2048.0 * 8.0); }

        return -1.0;
    }



    static public int getRefinementIterations(int depth)
    {
        if (depth < 0 || depth > 28)
        { return 3; }

        if(depth >= 0 && depth < 6)
        {
            return 4;
        }
        else if(depth >= 6 && depth < 20)
        {
            return 4;
        }
        else if(depth >= 20 && depth < 28)
        {
            return 5;
        }
        else if(depth == 28)
        {
            return 6;
        }


        return 3;
    }



    static public TileIndices selectTileIndices(int depth, double longitude, double latitude, TileIndices resultTileIndices, boolean originIsLeftUp)
    {
        // Given a geographic point (longitude, latitude) & a depth, this function returns the tileIndices for the specific depth.**
        double angRange = TileWgs84Utils.selectTileAngleRangeByDepth(depth);

        if (resultTileIndices == null)
        {
            resultTileIndices = new TileIndices();
        }

        if (originIsLeftUp)
        {
            double xMin = -180.0;
            double yMin = 90.0;

            int xIndex = (int) Math.floor((longitude - xMin)/angRange);
            int yIndex = (int) Math.floor((yMin - latitude)/angRange);

            resultTileIndices.set( xIndex, yIndex, depth);
        }
        else
        {
            double xMin = -180.0;
            double yMin = -90.0;

            int xIndex = (int) Math.floor((longitude - xMin)/angRange);
            int yIndex = (int) Math.floor((latitude - yMin)/angRange);

            resultTileIndices.set( xIndex, yIndex, depth);
        }

        return resultTileIndices;
    }

    static public ArrayList<TileIndices> selectTileIndicesArray(int depth, double minLon, double maxLon, double minLat, double maxLat, ArrayList<TileIndices> resultTileIndicesArray, TilesRange tilesRange, boolean originIsLeftUp)
    {
        // Given a geographic rectangle (minLon, minLat, maxLon, maxLat) & a depth, this function returns all
        // tilesIndices intersected by the rectangle for the specific depth.**
        TileIndices leftDownTileName = TileWgs84Utils.selectTileIndices(depth, minLon, minLat, null, originIsLeftUp);
        TileIndices rightDownTileName = TileWgs84Utils.selectTileIndices(depth, maxLon, minLat, null, originIsLeftUp);
        TileIndices rightUpTileName = TileWgs84Utils.selectTileIndices(depth, maxLon, maxLat, null, originIsLeftUp);

        int minX = leftDownTileName.X;
        int maxX = rightDownTileName.X;
        int maxY = leftDownTileName.Y; // origin is left-up.
        int minY = rightUpTileName.Y;

        if(!originIsLeftUp)
        {
            maxY = rightUpTileName.Y;
            minY = leftDownTileName.Y;
        }


        // the "tilesRange" is optional.***
        if(tilesRange != null)
        {
            tilesRange.tileDepth = depth;
            tilesRange.minTileX = minX;
            tilesRange.maxTileX = maxX;
            tilesRange.minTileY = minY;
            tilesRange.maxTileY = maxY;
        }

        if (resultTileIndicesArray == null)
        {
            resultTileIndicesArray = new ArrayList<TileIndices>();
        }

        for (var x = minX; x <= maxX; x++)
        {
            for (var y = minY; y <= maxY; y++)
            {
                TileIndices tileIndices = new TileIndices();
                tileIndices.set(x, y, depth);
                resultTileIndicesArray.add(tileIndices);
            }
        }

        return resultTileIndicesArray;
    }

    static public String getTileFileName(int X, int Y, int L)
    {
        return "L" + L + "_X" + X + "_Y" + Y + ".til";
    }

    static public String getTileFolderName_L(int L)
    {
        return "L" + String.valueOf(L);
    }

    static public String getTileFolderName_X(int X)
    {
        return "X" + String.valueOf(X);
    }

    static public String getTileFilePath(int X, int Y, int L)
    {
        return getTileFolderName_L(L) + "\\" + getTileFolderName_X(X) + "\\" + getTileFileName(X, Y, L);
    }

    static boolean isValidTileIndices(int L, int X, int Y)
    {
        // calculate the minX & minY, maxX & maxY for the tile depth(L).***
        double angDeg = TileWgs84Utils.selectTileAngleRangeByDepth(L);

        // in longitude, the range is (-180, 180).***
        int numTilesX = (int) (360.0 / angDeg);
        int numTilesY = (int) (180.0 / angDeg);

        if( X < 0 || X >= numTilesX )
        {
            return false;
        }

        if( Y < 0 || Y >= numTilesY )
        {
            return false;
        }

        return true;
    }

    static public GeographicExtension getGeographicExtentOfTileLXY (int L, int X, int Y, GeographicExtension resultGeoExtend, String imageryType, boolean originIsLeftUp)
    {
        if (resultGeoExtend == null)
        { resultGeoExtend = new GeographicExtension(); }

        if (imageryType == "CRS84")
        {
            double angRange = TileWgs84Utils.selectTileAngleRangeByDepth(L);
            double minLon = angRange*(double)X - 180.0;
            double maxLon = angRange*((double)X+1.0) - 180.0;
            double minLat = 90.0 - angRange*((double)Y+1.0);
            double maxLat = 90.0 - angRange*((double)Y);

            if(!originIsLeftUp)
            {
                minLat = -90.0 + angRange*((double)Y);
                maxLat = -90.0 + angRange*((double)Y+1.0);
            }

            resultGeoExtend.setDegrees(minLon, minLat, 0, maxLon, maxLat, 0);
            return resultGeoExtend;
        }
        else if (imageryType == "WEB_MERCATOR")
        {
            double webMercatorMaxLatRad = 1.4844222297453324; // = 2*Math.atan(Math.pow(Math.E, Math.PI)) - (Math.PI/2);

            // 1rst, must know how many colums & rows there are in depth "L".***
            double numCols = Math.pow(2, L);
            double numRows = numCols;

            // calculate the angles of the tiles.
            double lonAngDegRange = 360.0 / numCols; // the longitude are lineal.***

            // In depth L=0, the latitude range is (-webMercatorMaxLatRad, webMercatorMaxLatRad).***
            double M_PI = Math.PI;
            double M_E = Math.E;
            double maxMercatorY = M_PI;
            double minMercatorY = -M_PI;
            double maxLadRad = webMercatorMaxLatRad;
            double minLadRad = -webMercatorMaxLatRad;
            double midLatRad;
            double midLatRadMercator;
            double y_ratio = ( Y + 0.0005 ) / numRows;
            int currL = 0;
            boolean finished = false;
            while (!finished && currL <= 22)
            {
                if (currL == L)
                {
                    double min_longitude = lonAngDegRange * (double)X - 180.0;
                    double max_longitude = min_longitude + lonAngDegRange;
                    double min_latitude = minLadRad * 180.0 / M_PI;
                    double max_latitude = maxLadRad * 180.0 / M_PI;

                    resultGeoExtend.setDegrees(min_longitude, min_latitude, 0, max_longitude, max_latitude, 0);
                    finished = true;
                }
                else
                {
                    double midMercatorY = (maxMercatorY + minMercatorY) / 2.0;
                    midLatRad = 2.0 * Math.atan(Math.pow(M_E, midMercatorY)) - M_PI / 2.0;
                    double midLatRatio = (M_PI - midMercatorY) / (M_PI - (-M_PI));

                    // must choice : the up_side of midLatRadMercator, or the down_side.***
                    if (midLatRatio > y_ratio)
                    {
                        // choice the up_side of midLatRadMercator.***
                        // maxLatRad no changes.***
                        minLadRad = midLatRad;
                        minMercatorY = midMercatorY;
                    }
                    else
                    {
                        // choice the down_side of midLatRadMercator.***
                        maxLadRad = midLatRad;
                        maxMercatorY = midMercatorY;
                        // minLadRad no changes.***
                    }
                }

                // Code to debug.************************************
                //double min_longitude = lonAngDegRange * X - 180.0;
                //double max_longitude = min_longitude + lonAngDegRange;

                //double min_latitude = minLadRad * 180.0 / M_PI;
                //double max_latitude = maxLadRad * 180.0 / M_PI;
                //---------------------------------------------------

                currL++;
            }

            return resultGeoExtend;
        }

        return null;
    }


}
