package com.gaia3d.wgs84Tiles;

import com.gaia3d.basic.structure.GeographicExtension;
import lombok.Getter;
import lombok.Setter;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;

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

        String imageryType = manager.getImageryType();
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
        return minLonDeg + col * deltaLonDeg + deltaLonDeg * 0.5;
    }

    public double getLatDeg(int row) {
        double minLatDeg = this.geographicExtension.getMinLatitudeDeg();
        return minLatDeg + row * deltaLatDeg + deltaLatDeg * 0.5;
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

        double semiDeltaLonDeg = deltaLonDeg * 0.5;
        double semiDeltaLatDeg = deltaLatDeg * 0.5;

        boolean[] intersects = new boolean[1];

        for (int col = 0; col < rasterWidth; col++) {
            double lonDeg = minLonDeg + semiDeltaLonDeg + col * deltaLonDeg;
            for (int row = 0; row < rasterHeight; row++) {
                double latDeg = minLatDeg + semiDeltaLatDeg + row * deltaLatDeg;
                int idx = row * rasterWidth + col;
                elevations[idx] = (float) terrainElevationDataManager.getElevation(lonDeg, latDeg, this.manager.getMemSaveTerrainElevDataArray());
            }
        }
    }

}
