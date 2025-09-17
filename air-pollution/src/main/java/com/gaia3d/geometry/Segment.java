package com.gaia3d.geometry;

import com.gaia3d.utils.GeometryUtils;

public class Segment {
    public Vertex startVertex = null;
    public Vertex endVertex = null;

    public void set(Vertex startVertex, Vertex endVertex) {
        this.startVertex = startVertex;
        this.endVertex = endVertex;
    }

    public double getLength() {
        return startVertex.point3d.getDistanceToPoint(endVertex.point3d);
    }

    public boolean sharesVertexWith(Segment segment) {
        if (this.startVertex == segment.startVertex || this.startVertex == segment.endVertex) {
            return true;
        }
        return this.endVertex == segment.startVertex || this.endVertex == segment.endVertex;
    }

    public Line getLine() {
        Line line = new Line();
        line.point = this.startVertex.point3d;
        Point3D direction = this.endVertex.point3d.getSubstracted(this.startVertex.point3d); // end - start.
        line.direction = direction.getNormalized();
        return line;
    }

    public boolean intersectsWithPointXY(Point3D p) {
        // This function returns true if the input point intersects with this segment in XY plane.
        // 1rst check if vertex are collinear.
        // 2nd check if vertex is between the segment.
        double error = 0.0000001;
        if (!GeometryUtils.arePointsCollinealXY(this.startVertex.point3d, this.endVertex.point3d, p, error)) {
            return false;
        }

        double squaredDistance1 = this.startVertex.point3d.getSquaredDistanceToPoint(p);
        double squaredDistance2 = this.endVertex.point3d.getSquaredDistanceToPoint(p);
        double squaredDistanceSegment = this.startVertex.point3d.getSquaredDistanceToPoint(this.endVertex.point3d);
        return !(squaredDistance1 > squaredDistanceSegment) && !(squaredDistance2 > squaredDistanceSegment);
    }

    public BoundingRectangle getBoundingRectangleXY() {
        BoundingRectangle boundingRectangle = new BoundingRectangle();
        boundingRectangle.init(startVertex.point3d.x, startVertex.point3d.y);
        boundingRectangle.addPoint(endVertex.point3d.x, endVertex.point3d.y);
        return boundingRectangle;
    }

    public boolean intersectsWithSegmentXY(Segment segment, Point3D intersectionPoint) {
        // This function checks if the 2 segments intersect in XY plane.
        // 1rst check if segments are collinear.
        Line line1 = this.getLine();
        Line line2 = segment.getLine();
        double error = 0.0000001;
        intersectionPoint = line1.getIntersectedPointWithLineXY(line2, error);
        if (intersectionPoint == null) {
            return false;
        }

        // 2nd check if intersectionPoint is inside of 2 segments.
        if (!this.intersectsWithPointXY(intersectionPoint)) {
            return false;
        }
        return segment.intersectsWithPointXY(intersectionPoint);
    }
}
