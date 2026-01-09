package com.gaia3d.terrain.tile;

import com.gaia3d.terrain.structure.*;
import com.gaia3d.terrain.util.TileWgs84Utils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2i;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Setter
public class TileWgs84Raster {
    private TileWgs84Manager manager = null;
    private TileIndices tileIndices = null;
    private GeographicExtension geographicExtension = null;
    private float[] elevations = null;
    private int rasterWidth = 0;
    private int rasterHeight = 0;
    private double deltaLonDeg = 0;
    private double deltaLatDeg = 0;

    public TileWgs84Raster(TileIndices tileIndices, TileWgs84Manager manager) {
        this.tileIndices = tileIndices;
        this.manager = manager;

        String imageryType = manager.getImaginaryType();
        boolean originIsLeftUp = manager.isOriginIsLeftUp();
        this.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, imageryType, originIsLeftUp);
    }

    public int getColumn(double lonDeg) {
        double minLonDeg = this.geographicExtension.getMinLongitudeDeg();
        double maxLonDeg = this.geographicExtension.getMaxLongitudeDeg();

        if (lonDeg < minLonDeg || lonDeg > maxLonDeg) {
            return -1;
        }

        return (int) ((lonDeg - minLonDeg) / deltaLonDeg);
    }

    public int getRow(double latDeg) {
        double minLatDeg = this.geographicExtension.getMinLatitudeDeg();
        double maxLatDeg = this.geographicExtension.getMaxLatitudeDeg();

        if (latDeg < minLatDeg || latDeg > maxLatDeg) {
            return -1;
        }

        return (int) ((latDeg - minLatDeg) / deltaLatDeg);
    }

    public double getLonDeg(int col) {
        double minLonDeg = this.geographicExtension.getMinLongitudeDeg();
        return minLonDeg + col * deltaLonDeg;
    }

    public double getLatDeg(int row) {
        double minLatDeg = this.geographicExtension.getMinLatitudeDeg();
        return minLatDeg + row * deltaLatDeg;
    }

    public float getElevation(int col, int row) {
        if (col < 0 || col >= rasterWidth || row < 0 || row >= rasterHeight) {
            return Float.NaN;
        }

        int idx = row * rasterWidth + col;
        return elevations[idx];
    }

    public float getElevationBilinear(double lonDeg, double latDeg) {
        int col = getColumn(lonDeg);
        int row = getRow(latDeg);

        if (col < 0 || col >= rasterWidth || row < 0 || row >= rasterHeight) {
            log.info("getElevationBilinear: col or row is out of range. col = {}, row = {}", col, row);
            return Float.NaN;
        }

        double lon0 = getLonDeg(col);
        double lat0 = getLatDeg(row);

        double lon1 = getLonDeg(col + 1);
        double lat1 = getLatDeg(row + 1);

        double dx = (lonDeg - lon0) / (lon1 - lon0);
        double dy = (latDeg - lat0) / (lat1 - lat0);

        float z00 = getElevation(col, row);
        float z01 = getElevation(col, row + 1);
        float z10 = getElevation(col + 1, row);
        float z11 = getElevation(col + 1, row + 1);

        float z0 = z00 + (z01 - z00) * (float) dy;
        float z1 = z10 + (z11 - z10) * (float) dy;

        return z0 + (z1 - z0) * (float) dx;
    }

    public void deleteObjects() {
        this.geographicExtension = null;
        this.elevations = null;
    }

    public void makeElevations(TerrainElevationDataManager terrainElevationDataManager, int rasterWidth, int rasterHeight) {
        this.rasterWidth = rasterWidth;
        this.rasterHeight = rasterHeight;

        int elevationsCount = rasterWidth * rasterHeight;
        this.elevations = new float[elevationsCount];

        double minLonDeg = this.geographicExtension.getMinLongitudeDeg();
        double minLatDeg = this.geographicExtension.getMinLatitudeDeg();

        double maxLonDeg = this.geographicExtension.getMaxLongitudeDeg();
        double maxLatDeg = this.geographicExtension.getMaxLatitudeDeg();

        // TODO : must check if the rasterWidth and rasterHeight are valid values. In low definition geoTiff files is possible that the
        // columns count and rows count are less than rasterWidth and rasterHeight.

        deltaLonDeg = (maxLonDeg - minLonDeg) / (rasterWidth - 1);
        deltaLatDeg = (maxLatDeg - minLatDeg) / (rasterHeight - 1);

        double semiDeltaLonDeg = deltaLonDeg * 0.5;
        double semiDeltaLatDeg = deltaLatDeg * 0.5;

        // make intersected terrainElevationDataList
        GeographicExtension geoExtension = this.getGeographicExtension();
        List<TerrainElevationData> resultTerrainElevDataArray = this.manager.getTerrainElevationDataList();
        resultTerrainElevDataArray.clear();
        // Debug : check if the terrainElevationDataList is intersected with the geoExtension
        Map<TerrainElevationData, TerrainElevationData> terrainElevDataMap = new HashMap<>();
        terrainElevationDataManager.getTerrainElevationDataArray(geoExtension, terrainElevDataMap);
        resultTerrainElevDataArray = new ArrayList<>(terrainElevDataMap.keySet());

        for (int col = 0; col < rasterWidth; col++) {
            double lonDeg = minLonDeg + semiDeltaLonDeg + col * deltaLonDeg;
            for (int row = 0; row < rasterHeight; row++) {
                double latDeg = minLatDeg + semiDeltaLatDeg + row * deltaLatDeg;
                int idx = row * rasterWidth + col;
                elevations[idx] = (float) terrainElevationDataManager.getElevation(lonDeg, latDeg, resultTerrainElevDataArray);
            }
        }
    }

    public RasterTriangle getRasterTriangle(TerrainTriangle triangle) {
        RasterTriangle rasterTriangle = new RasterTriangle();

        List<TerrainHalfEdge> listHalfEdges = new ArrayList<>(); // no used in this function, but needed to call the getVertices function.
        List<TerrainVertex> vertices = triangle.getVertices(null, listHalfEdges);
        Vector3d pos0 = vertices.get(0).getPosition();
        Vector3d pos1 = vertices.get(1).getPosition();
        Vector3d pos2 = vertices.get(2).getPosition();

        // the pos0, pos1 and pos2 are in geographic coordinates.
        int col0 = getColumn(pos0.x);
        int row0 = getRow(pos0.y);

        int col1 = getColumn(pos1.x);
        int row1 = getRow(pos1.y);

        int col2 = getColumn(pos2.x);
        int row2 = getRow(pos2.y);

        Vector2i v0 = new Vector2i(col0, row0);
        Vector2i v1 = new Vector2i(col1, row1);
        Vector2i v2 = new Vector2i(col2, row2);

        rasterTriangle.setVertices(v0, v1, v2);

        return rasterTriangle;
    }

}
