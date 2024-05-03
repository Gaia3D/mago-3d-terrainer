package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GeographicExtension;
import lombok.Getter;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;

public class TileWgs84Raster {
    public TileWgs84Manager manager = null;
    public TileIndices tileIndices = null;

    public GeographicExtension geographicExtension = null;

    public float[] elevations = null;
    public int rasterWidth = 0;
    public int rasterHeight = 0;

    @Getter
    private double deltaLonDeg = 0;
    @Getter
    private double deltaLatDeg = 0;

    public TileWgs84Raster(TileIndices tileIndices, TileWgs84Manager manager) {
        this.tileIndices = tileIndices;
        this.manager = manager;

        String imageryType = manager.imageryType;
        boolean originIsLeftUp = manager.originIsLeftUp;
        this.geographicExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.L, tileIndices.X, tileIndices.Y, null, imageryType, originIsLeftUp);
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

    public float getElevation(int col, int row) {
        if (col < 0 || col >= rasterWidth || row < 0 || row >= rasterHeight) {
            return Float.NaN;
        }

        int idx = row * rasterWidth + col;
        return elevations[idx];
    }

    public void deleteObjects() {
        this.geographicExtension = null;
        this.elevations = null;
    }

    public void makeElevations(TerrainElevationDataManager terrainElevationDataManager, int rasterWidth, int rasterHeight) throws TransformException, IOException {
        this.rasterWidth = rasterWidth;
        this.rasterHeight = rasterHeight;

        int elevationsCount = rasterWidth * rasterHeight;
        this.elevations = new float[elevationsCount];

        double minLonDeg = this.geographicExtension.getMinLongitudeDeg();
        double minLatDeg = this.geographicExtension.getMinLatitudeDeg();

        double maxLonDeg = this.geographicExtension.getMaxLongitudeDeg();
        double maxLatDeg = this.geographicExtension.getMaxLatitudeDeg();

        deltaLonDeg = (maxLonDeg - minLonDeg) / (rasterWidth);
        deltaLatDeg = (maxLatDeg - minLatDeg) / (rasterHeight);

        boolean[] intersects = new boolean[1];

        for (int col = 0; col < rasterWidth; col++) {
            double lonDeg = minLonDeg + col * deltaLonDeg;
            for (int row = 0; row < rasterHeight; row++) {
                double latDeg = minLatDeg + row * deltaLatDeg;
                int idx = row * rasterWidth + col;
                elevations[idx] = (float) terrainElevationDataManager.getElevation(lonDeg, latDeg, this.manager.memSave_terrainElevDatasArray);
            }
        }
    }
}
