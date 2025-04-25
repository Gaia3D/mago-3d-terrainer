package com.gaia3d.terrain.util;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.structure.GeographicExtension;
import com.gaia3d.terrain.structure.TerrainMesh;
import com.gaia3d.terrain.structure.TerrainVertex;
import com.gaia3d.terrain.tile.TileIndices;
import com.gaia3d.terrain.tile.TileRange;
import com.gaia3d.util.GlobeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TileWgs84Utils {
    private final static GlobalOptions globalOptions = GlobalOptions.getInstance();

    public static double getTileSizeInMetersByDepth(int depth) {
        double angDeg = TileWgs84Utils.selectTileAngleRangeByDepth(depth);
        double angRad = angDeg * Math.PI / 180.0;
        return angRad * GlobeUtils.EQUATORIAL_RADIUS;
    }

    public static void clampVerticesInToTile(TerrainMesh mesh, TileIndices tileIndices, String imaginaryType, boolean originIsLeftUp) {
        // clamp the vertices in to the tile
        // Obtain the geographicExtension by tileIndices
        GeographicExtension geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, imaginaryType, originIsLeftUp);
        if (geographicExtension == null) {
            return;
        }
        double minLonDeg = geographicExtension.getMinLongitudeDeg();
        double maxLonDeg = geographicExtension.getMaxLongitudeDeg();
        double minLatDeg = geographicExtension.getMinLatitudeDeg();
        double maxLatDeg = geographicExtension.getMaxLatitudeDeg();

        // south vertices.***
        List<TerrainVertex> downVertices = mesh.getDownVerticesSortedLeftToRight();
        for (TerrainVertex vertex : downVertices) {
            double latDeg = vertex.getPosition().y;
            if (latDeg != minLatDeg) {
                vertex.getPosition().y = minLatDeg;
            }
        }

        // north vertices.***
        List<TerrainVertex> upVertices = mesh.getUpVerticesSortedRightToLeft();
        for (TerrainVertex vertex : upVertices) {
            double latDeg = vertex.getPosition().y;
            if (latDeg != maxLatDeg) {
                vertex.getPosition().y = maxLatDeg;
            }
        }

        // west vertices.***
        List<TerrainVertex> leftVertices = mesh.getLeftVerticesSortedUpToDown();
        for (TerrainVertex vertex : leftVertices) {
            double lonDeg = vertex.getPosition().x;
            if (lonDeg != minLonDeg) {
                vertex.getPosition().x = minLonDeg;
            }
        }

        // east vertices.***
        List<TerrainVertex> rightVertices = mesh.getRightVerticesSortedDownToUp();
        for (TerrainVertex vertex : rightVertices) {
            double lonDeg = vertex.getPosition().x;
            if (lonDeg != maxLonDeg) {
                vertex.getPosition().x = maxLonDeg;
            }
        }
    }

    public static double getMaxDiffBetweenGeoTiffSampleAndTrianglePlane(int depth) {
        double tileSize = TileWgs84Utils.getTileSizeInMetersByDepth(depth);
        double result;
        if (depth < 5) {
            result = tileSize * 0.01;
        } else if (depth <= 8) {
            result = tileSize * 0.015;
        } else if (depth == 9) {
            result = tileSize * 0.02;
        } else if (depth == 10) {
            result = tileSize * 0.03;
        } else if (depth == 11) {
            result = tileSize * 0.04;
        } else if (depth == 12) {
            result = tileSize * 0.05;
        } else if (depth == 13) {
            result = tileSize * 0.06;
        } else if (depth == 14) {
            result = tileSize * 0.06;
        } else if (depth == 15) {
            result = tileSize * 0.06;
        } else if (depth == 16) {
            result = tileSize * 0.065;
        } else if (depth == 17) {
            result = tileSize * 0.07;
        } else if (depth == 18) {
            result = tileSize * 0.07;
        } else {
            result = tileSize * 0.07;
        }
        return result / globalOptions.getIntensity();
    }

    public static double selectTileAngleRangeByDepth(int depth) {
        // given tile depth, this function returns the latitude angle range of the tile
        if (depth < 0 || depth > 28) {
            return -1.0;
        }

        return 180.0 / Math.pow(2, depth);
    }

    public static int getRefinementIterations(int depth) {
        if (depth < 0 || depth > 28) {
            return 5;
        }

        if (depth < 6) {
            return 15;
        } else if (depth < 10) {
            return 15;
        } else if (depth < 20) {
            return 15;
        } else {
            return 15;
        }
    }


    public static TileIndices selectTileIndices(int depth, double longitude, double latitude, TileIndices resultTileIndices, boolean originIsLeftUp) {
        // Given a geographic point (longitude, latitude) & a depth, this function returns the tileIndices for the specific depth.**
        double angRange = TileWgs84Utils.selectTileAngleRangeByDepth(depth);

        if (resultTileIndices == null) {
            resultTileIndices = new TileIndices();
        }

        if (originIsLeftUp) {
            double xMin = -180.0;
            double yMin = 90.0;

            int xIndex = (int) Math.floor((longitude - xMin) / angRange);
            int yIndex = (int) Math.floor((yMin - latitude) / angRange);

            resultTileIndices.set(xIndex, yIndex, depth);
        } else {
            double xMin = -180.0;
            double yMin = -90.0;

            int xIndex = (int) Math.floor((longitude - xMin) / angRange);
            int yIndex = (int) Math.floor((latitude - yMin) / angRange);

            resultTileIndices.set(xIndex, yIndex, depth);
        }

        return resultTileIndices;
    }

    public static void selectTileIndicesArray(int depth, double minLon, double maxLon, double minLat, double maxLat, TileRange tilesRange, boolean originIsLeftUp) {
        // Given a geographic rectangle (minLon, minLat, maxLon, maxLat) & a depth, this function returns all
        // tilesIndices intersected by the rectangle for the specific depth.**
        TileIndices leftDownTileName = TileWgs84Utils.selectTileIndices(depth, minLon, minLat, null, originIsLeftUp);
        TileIndices rightDownTileName = TileWgs84Utils.selectTileIndices(depth, maxLon, minLat, null, originIsLeftUp);
        TileIndices rightUpTileName = TileWgs84Utils.selectTileIndices(depth, maxLon, maxLat, null, originIsLeftUp);

        int minX = leftDownTileName.getX();
        int maxX = rightDownTileName.getX();
        int maxY = leftDownTileName.getY(); // origin is left-up.
        int minY = rightUpTileName.getY();

        double xMin = -180.0;
        double yMin = -90.0;

        double angRange = TileWgs84Utils.selectTileAngleRangeByDepth(depth);
        int xIndexMax = (int) Math.round((maxLon - xMin) / angRange);
        int yIndexMax = (int) Math.round((maxLat - yMin) / angRange);

        int xIndexMin = (int) Math.round((minLon - xMin) / angRange);
        int yIndexMin = (int) Math.round((minLat - yMin) / angRange);

        if (!originIsLeftUp) {
            maxY = rightUpTileName.getY();
            minY = leftDownTileName.getY();
        }

        if (maxX < xIndexMax) {
            maxX = xIndexMax;
        }

        if (maxY < yIndexMax) {
            maxY = yIndexMax;
        }

        if (minX > xIndexMin) {
            minX = xIndexMin;
        }

        if (minY > yIndexMin) {
            minY = yIndexMin;
        }

        // the "tilesRange" is optional
        if (tilesRange != null) {
            tilesRange.setTileDepth(depth);
            tilesRange.setMinTileX(minX);
            tilesRange.setMaxTileX(maxX);
            tilesRange.setMinTileY(minY);
            tilesRange.setMaxTileY(maxY);
        }
    }

    public static String getTileFileName(int X, int Y, int L) {
        return "L" + L + "_X" + X + "_Y" + Y + ".til";
    }

    public static String getTileFolderNameL(int L) {
        return "L" + L;
    }

    public static String getTileFolderNameX(int X) {
        return "X" + X;
    }

    public static String getTileFilePath(int X, int Y, int L) {
        return getTileFolderNameL(L) + File.separator + getTileFolderNameX(X) + File.separator + getTileFileName(X, Y, L);
    }

    public static List<TileRange> subDivideTileRange(TileRange tilesRange, int maxCol, int maxRow, List<TileRange> resultSubDividedTilesRanges) {
        if (resultSubDividedTilesRanges == null) {
            resultSubDividedTilesRanges = new ArrayList<>();
        }

        int colsCount = tilesRange.getMaxTileX() - tilesRange.getMinTileX() + 1;
        int rowsCount = tilesRange.getMaxTileY() - tilesRange.getMinTileY() + 1;

        if (colsCount <= maxCol && rowsCount <= maxRow) {
            resultSubDividedTilesRanges.add(tilesRange);
            return resultSubDividedTilesRanges;
        }

        int colsSubDividedCount = 1;
        int rowsSubDividedCount = 1;

        if (colsCount > maxCol) {
            colsSubDividedCount = (int) Math.ceil((double) colsCount / (double) maxCol);
        }

        if (rowsCount > maxRow) {
            rowsSubDividedCount = (int) Math.ceil((double) rowsCount / (double) maxRow);
        }

        double colsSubDividedSize = (double) colsCount / (double) colsSubDividedCount;
        double rowsSubDividedSize = (double) rowsCount / (double) rowsSubDividedCount;

        for (int i = 0; i < colsSubDividedCount; i++) {
            for (int j = 0; j < rowsSubDividedCount; j++) {
                TileRange subDividedTilesRange = new TileRange();
                subDividedTilesRange.setTileDepth(tilesRange.getTileDepth());
                subDividedTilesRange.setMinTileX(tilesRange.getMinTileX() + (int) (i * colsSubDividedSize));
                subDividedTilesRange.setMaxTileX(tilesRange.getMinTileX() + (int) ((i + 1) * colsSubDividedSize) - 1);
                subDividedTilesRange.setMinTileY(tilesRange.getMinTileY() + (int) (j * rowsSubDividedSize));
                subDividedTilesRange.setMaxTileY(tilesRange.getMinTileY() + (int) ((j + 1) * rowsSubDividedSize) - 1);

                resultSubDividedTilesRanges.add(subDividedTilesRange);
            }
        }

        return resultSubDividedTilesRanges;
    }

    public static boolean isValidTileIndices(int L, int X, int Y) {
        // calculate the minX & minY, maxX & maxY for the tile depth(L)
        double angDeg = TileWgs84Utils.selectTileAngleRangeByDepth(L);

        // in longitude, the range is (-180, 180)
        int numTilesX = (int) (360.0 / angDeg);
        int numTilesY = (int) (180.0 / angDeg);

        if (X < 0 || X >= numTilesX) {
            return false;
        }

        return Y >= 0 && Y < numTilesY;
    }

    public static GeographicExtension getGeographicExtentOfTileLXY(int L, int X, int Y, GeographicExtension resultGeoExtend, String imageryType, boolean originIsLeftUp) {
        if (resultGeoExtend == null) {
            resultGeoExtend = new GeographicExtension();
        }

        if (imageryType.equals("CRS84")) {
            double angRange = TileWgs84Utils.selectTileAngleRangeByDepth(L);
            double minLon = angRange * (double) X - 180.0;
            double maxLon = angRange * ((double) X + 1.0) - 180.0;
            double minLat = 90.0 - angRange * ((double) Y + 1.0);
            double maxLat = 90.0 - angRange * ((double) Y);

            if (!originIsLeftUp) {
                minLat = -90.0 + angRange * ((double) Y);
                maxLat = -90.0 + angRange * ((double) Y + 1.0);
            }

            resultGeoExtend.setDegrees(minLon, minLat, 0, maxLon, maxLat, 0);
            return resultGeoExtend;
        } else if (imageryType.equals("WEB_MERCATOR")) {
            double webMercatorMaxLatRad = 1.4844222297453324; // = 2*Math.atan(Math.pow(Math.E, Math.PI)) - (Math.PI/2);

            // First, must know how many colums & rows there are in depth "L"
            double numCols = Math.pow(2, L);
            double numRows = numCols;

            // calculate the angles of the tiles.
            double lonAngDegRange = 360.0 / numCols; // the longitude are lineal

            // In depth L=0, the latitude range is (-webMercatorMaxLatRad, webMercatorMaxLatRad)
            double M_PI = Math.PI;
            double M_E = Math.E;
            double maxMercatorY = M_PI;
            double minMercatorY = -M_PI;
            double maxLadRad = webMercatorMaxLatRad;
            double minLadRad = -webMercatorMaxLatRad;
            double midLatRad;
            double midLatRadMercator;
            double y_ratio = (Y + 0.0005) / numRows;
            int currL = 0;
            boolean finished = false;
            while (!finished && currL <= 22) {
                if (currL == L) {
                    double minLongitude = lonAngDegRange * (double) X - 180.0;
                    double maxLongitude = minLongitude + lonAngDegRange;
                    double minLatitude = minLadRad * 180.0 / M_PI;
                    double maxLatitude = maxLadRad * 180.0 / M_PI;

                    resultGeoExtend.setDegrees(minLongitude, minLatitude, 0, maxLongitude, maxLatitude, 0);
                    finished = true;
                } else {
                    double midMercatorY = (maxMercatorY + minMercatorY) / 2.0;
                    midLatRad = 2.0 * Math.atan(Math.pow(M_E, midMercatorY)) - M_PI / 2.0;
                    double midLatRatio = (M_PI - midMercatorY) / (M_PI - (-M_PI));

                    // must choice : the up_side of midLatRadMercator, or the down_side
                    if (midLatRatio > y_ratio) {
                        // choice the up_side of midLatRadMercator
                        // maxLatRad no changes
                        minLadRad = midLatRad;
                        minMercatorY = midMercatorY;
                    } else {
                        // choice the down_side of midLatRadMercator
                        maxLadRad = midLatRad;
                        maxMercatorY = midMercatorY;
                        // minLadRad no changes
                    }
                }

                currL++;
            }
            return resultGeoExtend;
        }
        return null;
    }
}
