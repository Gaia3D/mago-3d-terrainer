package com.gaia3d.geometry;

public class Point3D
{
    public double x = 0.0;
    public double y = 0.0;
    public double z = 0.0;

    public void set(double px, double py, double pz) {
        this.x = px;
        this.y = py;
        this.z = pz;
    }

    public Point3D getSubstracted(Point3D p) {
        Point3D result = new Point3D();
        result.x = this.x - p.x;
        result.y = this.y - p.y;
        result.z = this.z - p.z;

        return result;
    }

    public Point3D getNormalized() {
        Point3D result = new Point3D();
        double length = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        result.x = this.x / length;
        result.y = this.y / length;
        result.z = this.z / length;

        return result;
    }

    public double getSquaredDistanceToPoint(Point3D p) {
        double squaredDistance = 0.0;
        double dx = p.x - this.x;
        double dy = p.y - this.y;
        double dz = p.z - this.z;
        squaredDistance = dx * dx + dy * dy + dz * dz;
        return squaredDistance;
    }

    public double getDistanceToPoint(Point3D p) {
        double squaredDistance = this.getSquaredDistanceToPoint(p);
        double distance = Math.sqrt(squaredDistance);
        return distance;
    }
}
