package com.gaia3d.geometry;

import org.joml.Vector3d;

public class BoundingBox
{
    public double minX, minY, minZ, maxX, maxY, maxZ;

    public Vector3d GetCenterPosition() {
        Vector3d centerPos = new Vector3d();

        centerPos.x = (minX + maxX) / 2.0;
        centerPos.y = (minY + maxY) / 2.0;
        centerPos.z = (minZ + maxZ) / 2.0;

        return centerPos;
    }

    public void init(double x, double y, double z) {
        minX = x;
        minY = y;
        minZ = z;
        maxX = x;
        maxY = y;
        maxZ = z;
    }

    public double getLengthX() {
        return maxX - minX;
    }

    public double getLengthY() {
        return maxY - minY;
    }

    public double getLengthZ() {
        return maxZ - minZ;
    }

    public void copyFrom(BoundingBox bbox) {
        minX = bbox.minX;
        minY = bbox.minY;
        minZ = bbox.minZ;
        maxX = bbox.maxX;
        maxY = bbox.maxY;
        maxZ = bbox.maxZ;
    }

    public void addBox(BoundingBox bbox) {
        if (bbox.minX < minX) {
            minX = bbox.minX;
        }
        if (bbox.minY < minY) {
            minY = bbox.minY;
        }
        if (bbox.minZ < minZ) {
            minZ = bbox.minZ;
        }
        if (bbox.maxX > maxX) {
            maxX = bbox.maxX;
        }
        if (bbox.maxY > maxY) {
            maxY = bbox.maxY;
        }
        if (bbox.maxZ > maxZ) {
            maxZ = bbox.maxZ;
        }
    }

    public void addPoint(double x, double y, double z) {
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
    }
}
