package com.gaia3d.geometry;

import org.joml.Vector2d;

public class BoundingRectangle
{
    public double minX, minY, maxX, maxY;

    public Vector2d GetCenterPosition() {
        Vector2d centerPos = new Vector2d();

        centerPos.x = (minX + maxX) / 2.0;
        centerPos.y = (minY + maxY) / 2.0;

        return centerPos;
    }

    public void init(double x, double y) {
        minX = x;
        minY = y;
        maxX = x;
        maxY = y;
    }

    public void addPoint(double x, double y) {
        if (x < minX) {
            minX = x;
        }
        if (y < minY) {
            minY = y;
        }
        if (x > maxX) {
            maxX = x;
        }
        if (y > maxY) {
            maxY = y;
        }
    }

    public boolean intersectsPoint(double x, double y) {
        // This function returns true if the input point intersects with this bounding rectangle.
        if (x < minX) {
            return false;
        }
        if (y < minY) {
            return false;
        }
        if (x > maxX) {
            return false;
        }
        return !(y > maxY);
    }


    public boolean intersects(BoundingRectangle boundingRectangle) {
        // This function returns true if this bounding rectangle intersects with the input bounding rectangle.
        if (this.maxX < boundingRectangle.minX) {
            return false;
        }
        if (this.maxY < boundingRectangle.minY) {
            return false;
        }
        if (this.minX > boundingRectangle.maxX) {
            return false;
        }
        return !(this.minY > boundingRectangle.maxY);
    }
}
