package com.gaia3d.geometry;

public class Vertex {
    public Point3D point3d = new Point3D();
    public double pollutionValue = 0.0;
    public double angle = 0.0;

    public int idxInList = -1;

    public int status = 0; // 0 = no visited, 1 = visited, 2 = visited and added to convex hull.

    public void set(double px, double py, double pz) {
        this.point3d.x = px;
        this.point3d.y = py;
        this.point3d.z = pz;
    }


    public double getAngleWithVertexXY(Vertex vertex) {
        // This function returns the angle between this vertex and the input vertex.
        double angle = Math.atan2(vertex.point3d.y - this.point3d.y, vertex.point3d.x - this.point3d.x);
        if (angle < 0) {
            angle += 2.0 * Math.PI;
        }
        if (angle >= 2.0 * Math.PI) {
            angle -= 2.0 * Math.PI;
        }
        return angle;
    }
}
