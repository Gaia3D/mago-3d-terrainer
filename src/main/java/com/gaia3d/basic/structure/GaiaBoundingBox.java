package com.gaia3d.basic.structure;


import com.gaia3d.util.GlobeUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

/**
 * GaiaBoundingBox is a class to store the bounding box of a geometry.
 * It can be used to calculate the center and volume of the geometry.
 * It can also be used to convert the local bounding box to lonlat bounding box.
 * It can also be used to calculate the longest distance of the geometry.
 * @auther znkim
 * @since 1.0.0
 * @see GaiaRectangle
 */
@Slf4j
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaBoundingBox {
    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;
    private boolean isInit = false;

    public Vector3d getCenter() {
        return new Vector3d((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
    }

    public Vector3d getVolume() {
        return new Vector3d(maxX - minX, maxY - minY, maxZ - minZ);
    }

    public void addPoint(double x, double y, double z) {
        if (isInit) {
            if (x < minX) {
                minX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (z < minZ) {
                minZ = z;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y > maxY) {
                maxY = y;
            }
            if (z > maxZ) {
                maxZ = z;
            }
        } else {
            isInit = true;
            minX = x;
            minY = y;
            minZ = z;
            maxX = x;
            maxY = y;
            maxZ = z;
        }
    }
    public void addPoint(Vector3d vector3d) {
        if (isInit) {
            if (vector3d.x < minX) {
                minX = vector3d.x;
            }
            if (vector3d.y < minY) {
                minY = vector3d.y;
            }
            if (vector3d.z < minZ) {
                minZ = vector3d.z;
            }
            if (vector3d.x > maxX) {
                maxX = vector3d.x;
            }
            if (vector3d.y > maxY) {
                maxY = vector3d.y;
            }
            if (vector3d.z > maxZ) {
                maxZ = vector3d.z;
            }
        } else {
            isInit = true;
            minX = vector3d.x;
            minY = vector3d.y;
            minZ = vector3d.z;
            maxX = vector3d.x;
            maxY = vector3d.y;
            maxZ = vector3d.z;
        }
    }

    public boolean intersects(GaiaBoundingBox boundingBox, double errorX, double errorY, double errorZ)
    {
        if(maxX < boundingBox.minX - errorX || minX > boundingBox.maxX + errorX)
        {
            return false;
        }
        if(maxY < boundingBox.minY - errorY || minY > boundingBox.maxY + errorY)
        {
            return false;
        }
        if(maxZ < boundingBox.minZ - errorZ || minZ > boundingBox.maxZ + errorZ)
        {
            return false;
        }
        return true;
    }

    public boolean intersects(GaiaBoundingBox boundingBox, double error)
    {
        if(maxX < boundingBox.minX - error || minX > boundingBox.maxX + error)
        {
            return false;
        }
        if(maxY < boundingBox.minY - error || minY > boundingBox.maxY + error)
        {
            return false;
        }
        if(maxZ < boundingBox.minZ - error || minZ > boundingBox.maxZ + error)
        {
            return false;
        }
        return true;
    }

    public boolean intersectsPointXY(double pos_x, double pos_y)
    {
        // this function checks if a point2D is intersected by the boundingBox only meaning xAxis and yAxis.***
        if(pos_x < minX || pos_x > maxX || pos_y < minY || pos_y > maxY)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public boolean intersectsRectangleXY(double min_x, double min_y, double max_x, double max_y)
    {
        // this function checks if a rectangle2D is intersected by the boundingBox only meaning xAxis and yAxis.***
        if(max_x < minX || min_x > maxX || max_y < minY || min_y > maxY)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public boolean intersectsPointXY_xAxis(double pos_x)
    {
        // this function checks if a point2D is intersected by the boundingBox only meaning xAxis and yAxis.***
        if(pos_x < minX || pos_x > maxX)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public boolean intersectsPointXY_yAxis(double pos_y)
    {
        // this function checks if a point2D is intersected by the boundingBox only meaning xAxis and yAxis.***
        if(pos_y < minY || pos_y > maxY)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public double getLengthX()
    {
        return maxX - minX;
    }

    public double getLengthY()
    {
        return maxY - minY;
    }

    public double getLengthZ()
    {
        return maxZ - minZ;
    }

    public void addBoundingBox(GaiaBoundingBox boundingBox) {
        if (isInit) {
            if (boundingBox.minX < minX) {
                minX = boundingBox.minX;
            }
            if (boundingBox.minY < minY) {
                minY = boundingBox.minY;
            }
            if (boundingBox.minZ < minZ) {
                minZ = boundingBox.minZ;
            }
            if (boundingBox.maxX > maxX) {
                maxX = boundingBox.maxX;
            }
            if (boundingBox.maxY > maxY) {
                maxY = boundingBox.maxY;
            }
            if (boundingBox.maxZ > maxZ) {
                maxZ = boundingBox.maxZ;
            }
        } else {
            isInit = true;
            minX = boundingBox.getMinX();
            minY = boundingBox.getMinY();
            minZ = boundingBox.getMinZ();
            maxX = boundingBox.getMaxX();
            maxY = boundingBox.getMaxY();
            maxZ = boundingBox.getMaxZ();
        }
    }

    public GaiaBoundingBox convertLocalToLonlatBoundingBox(Vector3d center) {
        Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
        Matrix4d transformMatrix = GlobeUtils.normalAtCartesianPointWgs84(centerWorldCoordinate);

        Vector3d minLocalCoordinate = new Vector3d(minX, minY, minZ);
        Matrix4d minTransfromMatrix = transformMatrix.translate(minLocalCoordinate, new Matrix4d());
        Vector3d minWorldCoordinate = new Vector3d(minTransfromMatrix.m30(), minTransfromMatrix.m31(), minTransfromMatrix.m32());
        minWorldCoordinate = GlobeUtils.cartesianToGeographicWgs84(minWorldCoordinate);

        Vector3d maxLocalCoordinate = new Vector3d(maxX, maxY, maxZ);
        Matrix4d maxTransfromMatrix = transformMatrix.translate(maxLocalCoordinate, new Matrix4d());
        Vector3d maxWorldCoordinate = new Vector3d(maxTransfromMatrix.m30(), maxTransfromMatrix.m31(), maxTransfromMatrix.m32());
        maxWorldCoordinate = GlobeUtils.cartesianToGeographicWgs84(maxWorldCoordinate);

        GaiaBoundingBox result = new GaiaBoundingBox();
        result.addPoint(minWorldCoordinate);
        result.addPoint(maxWorldCoordinate);
        return result;
    }


    public double getLongestDistance() {
        Vector3d volume = getVolume();
        return Math.sqrt(volume.x * volume.x + volume.y * volume.y + volume.z * volume.z);
    }

    public double getLongestDistanceXY()
    {
        Vector3d volume = getVolume();
        return Math.sqrt(volume.x * volume.x + volume.y * volume.y);
    }
}
