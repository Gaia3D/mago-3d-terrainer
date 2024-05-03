package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GaiaBoundingBox;
import com.gaia3d.basic.structure.GaiaMesh;
import com.gaia3d.basic.structure.GaiaVertex;
import com.gaia3d.basic.structure.GeographicExtension;
import com.gaia3d.reader.FileUtils;
import com.gaia3d.util.GlobeUtils;
import org.joml.Vector2d;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;

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
        if(depth < 5)
        {
            return tileSize * 0.01;
        }
        else if(depth < 8)
        {
            return tileSize * 0.05;
        }
        else if(depth <= 12)
        {
            return tileSize * 0.1;
        }
        else if(depth == 13)
        {
            return tileSize * 0.12;
        }
        else if(depth == 14)
        {
            return tileSize * 0.15;
        }
        else if(depth == 15)
        {
            return tileSize * 0.18;
        }
        else if(depth == 16)
        {
            return tileSize * 0.21;
        }
        else if(depth == 17)
        {
            return tileSize * 0.26;
        }
        else if(depth == 18)
        {
            return tileSize * 0.29;
        }
        else
        {
            return tileSize * 0.3;
        }
    }

    static public double selectTileAngleRangeByDepth(int depth)
    {
        // given tile depth, this function returns the latitude angle range of the tile
        if (depth < 0 || depth > 28)
        { return -1.0; }

        return 180.0 / Math.pow(2, depth);
    }



    static public int getRefinementIterations(int depth)
    {
        if (depth < 0 || depth > 28)
        { return 5; }

//        if(depth >= 0 && depth < 6)
//        {
//            return 4;
//        }
//        else if(depth >= 6 && depth < 10)
//        {
//            return 3;
//        }
//        else if(depth >= 10 && depth < 20)
//        {
//            return 2;
//        }
//        else
//        {
//            return 2;
//        }

        if(depth >= 0 && depth < 6)
        {
            return 15;
        }
        else if(depth >= 6 && depth < 10)
        {
            return 15;
        }
        else if(depth >= 10 && depth < 20)
        {
            return 15;
        }
        else
        {
            return 15;
        }

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

        double xMin = -180.0;
        double yMin = -90.0;

        double angRange = TileWgs84Utils.selectTileAngleRangeByDepth(depth);
        int xIndexMax = (int) Math.round((maxLon - xMin)/angRange);
        int yIndexMax = (int) Math.round((maxLat - yMin)/angRange);

        int xIndexMin = (int) Math.round((minLon - xMin)/angRange);
        int yIndexMin = (int) Math.round((minLat - yMin)/angRange);



        if(!originIsLeftUp)
        {
            maxY = rightUpTileName.Y;
            minY = leftDownTileName.Y;
        }

        if(maxX < xIndexMax)
        {
            maxX = xIndexMax;
            int hola = 0;
        }

        if(maxY < yIndexMax)
        {
            maxY = yIndexMax;
            int hola = 0;
        }

        if(minX > xIndexMin)
        {
            minX = xIndexMin;
            int hola = 0;
        }

        if(minY > yIndexMin)
        {
            minY = yIndexMin;
            int hola = 0;
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

    static public int getTileIndiceMaxX(int depth)
    {
        double angDeg = TileWgs84Utils.selectTileAngleRangeByDepth(depth);
        return (int) (360.0 / angDeg);
    }

    static public int getTileIndiceMaxY(int depth)
    {
        double angDeg = TileWgs84Utils.selectTileAngleRangeByDepth(depth);
        return (int) (180.0 / angDeg);
    }

    static List<TilesRange> subDivideTileRange(TilesRange tilesRange, int maxCol, int maxRow, List<TilesRange> resultSubDividedTilesRanges)
    {
        if(resultSubDividedTilesRanges == null)
        {
            resultSubDividedTilesRanges = new ArrayList<TilesRange>();
        }

        int colsCount = tilesRange.maxTileX - tilesRange.minTileX + 1;
        int rowsCount = tilesRange.maxTileY - tilesRange.minTileY + 1;

        if(colsCount <= maxCol && rowsCount <= maxRow)
        {
            resultSubDividedTilesRanges.add(tilesRange);
            return resultSubDividedTilesRanges;
        }

        int colsSubDividedCount = 1;
        int rowsSubDividedCount = 1;

        if(colsCount > maxCol)
        {
            colsSubDividedCount = (int) Math.ceil((double)colsCount / (double)maxCol);
        }

        if(rowsCount > maxRow)
        {
            rowsSubDividedCount = (int) Math.ceil((double)rowsCount / (double)maxRow);
        }

        double colsSubDividedSize = (double)colsCount / (double)colsSubDividedCount;
        double rowsSubDividedSize = (double)rowsCount / (double)rowsSubDividedCount;

        for(int i = 0; i < colsSubDividedCount; i++)
        {
            for(int j = 0; j < rowsSubDividedCount; j++)
            {
                TilesRange subDividedTilesRange = new TilesRange();
                subDividedTilesRange.tileDepth = tilesRange.tileDepth;
                subDividedTilesRange.minTileX = tilesRange.minTileX + (int)(i * colsSubDividedSize);
                subDividedTilesRange.maxTileX = tilesRange.minTileX + (int)((i + 1) * colsSubDividedSize) - 1;
                subDividedTilesRange.minTileY = tilesRange.minTileY + (int)(j * rowsSubDividedSize);
                subDividedTilesRange.maxTileY = tilesRange.minTileY + (int)((j + 1) * rowsSubDividedSize) - 1;

                resultSubDividedTilesRanges.add(subDividedTilesRange);
            }
        }

        return resultSubDividedTilesRanges;
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

    static public boolean checkTile_test(GaiaMesh mesh, double error, boolean originIsLeftUp)
    {
        ArrayList< GaiaVertex >resultVertices = new ArrayList< GaiaVertex >();
        mesh.getVerticesByTriangles(resultVertices);

        if(resultVertices.size()!= mesh.vertices.size())
        {
            return false;
        }

        // check the boundingBox of the tile.***
        GaiaBoundingBox bbox = mesh.getBoundingBox();
        TileIndices tileIndices = mesh.triangles.get(0).ownerTile_tileIndices;
        GeographicExtension geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, "CRS84", originIsLeftUp);
        double minX = bbox.getMinX();
        double minY = bbox.getMinY();
        double maxX = bbox.getMaxX();
        double maxY = bbox.getMaxY();

        if(abs(minX - geographicExtension.getMinLongitudeDeg()) > error)
        {
            return false;
        }

        if(abs(minY - geographicExtension.getMinLatitudeDeg()) > error)
        {
            return false;
        }

        if(abs(maxX - geographicExtension.getMaxLongitudeDeg()) > error)
        {
            return false;
        }

        if(abs(maxY - geographicExtension.getMaxLatitudeDeg()) > error)
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
