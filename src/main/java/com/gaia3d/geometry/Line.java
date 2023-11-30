package com.gaia3d.geometry;

import com.gaia3d.utils.GeometryUtils;

public class Line
{
    public Point3D point = new Point3D();
    public Point3D direction = new Point3D();

    public boolean isParallelXYtoLine(Line line, double error) {
        // This function checks if the line is parallel to the input line in XY plane.***
        double crossProduct = GeometryUtils.crossProduct2D(this.direction, line.direction);
        if (crossProduct < error && crossProduct > -error) {
            return true;
        }

        return true;
    }

    public Point3D getIntersectedPointWithLineXY(Line line, double error) {
        // this function returns the intersected point of this line with line1 in XY plane.
        if (this.isParallelXYtoLine(line, 0.0000001)) {
            return null;
        }

        Point3D intersectedPoint = new Point3D();
        // y1 = A1x+B1.***
        // y2 = A2x+B2.***
        // A1x+B1 = A2x+B2.***
        // x(A1-A2) = B2-B1.***
        // x = (B2-B1)/(A1-A2).***
        double A1 = this.direction.y / this.direction.x;
        double B1 = this.point.y - A1 * this.point.x;
        double A2 = line.direction.y / line.direction.x;
        double B2 = line.point.y - A2 * line.point.x;
        intersectedPoint.x = (B2 - B1) / (A1 - A2);
        intersectedPoint.y = A1 * intersectedPoint.x + B1;
        intersectedPoint.z = 0.0;

        return intersectedPoint;
    }
}
