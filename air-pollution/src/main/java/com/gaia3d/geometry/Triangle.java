package com.gaia3d.geometry;

import com.gaia3d.utils.GeometryUtils;

import java.util.ArrayList;
import java.util.List;

public class Triangle {
    public Vertex vertex_0;
    public Vertex vertex_1;
    public Vertex vertex_2;

    public void setVertices(Vertex vertex0, Vertex vertex1, Vertex vertex2) {
        this.vertex_0 = vertex0;
        this.vertex_1 = vertex1;
        this.vertex_2 = vertex2;
    }

    public boolean isCCW_XYPlane() {
        // This function returns true if the triangle vertices are in counterclockwise order
        // in the XY plane.
        // Note: The triangle vertices must be in the XY plane.
        double crossProduct = GeometryUtils.crossProduct2D(vertex_0.point3d, vertex_1.point3d, vertex_2.point3d);
        return !(crossProduct < 0);
    }

    public void reverseSense() {
        // This function reverses the sense of the triangle.
        Vertex vertexAux = vertex_0;
        vertex_0 = vertex_2;
        vertex_2 = vertexAux;
    }

    public void getSegments(List<Segment> resultVecSegments) {
        // This function returns the segments of the triangle.
        Segment segment = new Segment();
        segment.set(vertex_0, vertex_1);
        resultVecSegments.add(segment);
        segment = new Segment();
        segment.set(vertex_1, vertex_2);
        resultVecSegments.add(segment);
        segment = new Segment();
        segment.set(vertex_2, vertex_0);
        resultVecSegments.add(segment);
    }

    public BoundingRectangle getBoundingRectangleXY() {
        // This function returns the bounding rectangle of the triangle in XY plane.
        BoundingRectangle boundingRectangle = new BoundingRectangle();
        boundingRectangle.init(vertex_0.point3d.x, vertex_0.point3d.y);
        boundingRectangle.addPoint(vertex_1.point3d.x, vertex_1.point3d.y);
        boundingRectangle.addPoint(vertex_2.point3d.x, vertex_2.point3d.y);
        return boundingRectangle;
    }

    public boolean hasVertex(Vertex vertex) {
        return vertex == vertex_0 || vertex == vertex_1 || vertex == vertex_2;
    }

    public boolean hasCommonVertex(Triangle triangle) {
        // This function returns true if the triangles have a common vertex.
        return hasVertex(triangle.vertex_0) || hasVertex(triangle.vertex_1) || hasVertex(triangle.vertex_2);
    }

    public boolean hasCommonEdge(Triangle triangle) {
        // This function returns true if the triangles have a common edge.
        List<Segment> vecSegments = new ArrayList<>();
        getSegments(vecSegments);
        int segmentsCount = vecSegments.size();
        for (int i = 0; i < segmentsCount; i++) {
            Segment segment = vecSegments.get(i);
            if (triangle.hasSegment(segment)) {
                return true;
            }
        }
        return false;
    }


    public boolean hasSegment(Segment segment) {
        // This function returns true if the triangle has the segment.
        List<Segment> vecSegments = new ArrayList<>();
        return hasVertex(segment.startVertex) && hasVertex(segment.endVertex);
    }

    public boolean intersectsXY(Segment segment) {
        // This function returns true if the segment intersects the triangle in XY plane.
        // 1rst check if intersects the bounding rectangle.
        // check if the "segment" belongs to the triangle.
        if (this.hasSegment(segment)) {
            // the "segment" belongs to the triangle, so return false.
            return false;
        }

        BoundingRectangle boundingRectangle = getBoundingRectangleXY();
        BoundingRectangle segmentBoundingRectangle = segment.getBoundingRectangleXY();
        if (!boundingRectangle.intersects(segmentBoundingRectangle)) {
            return false;
        }

        // 2nd check if intersects the segments.
        List<Segment> vecSegments = new ArrayList<>();
        getSegments(vecSegments);
        int segmentsCount = vecSegments.size();
        for (int i = 0; i < segmentsCount; i++) {
            Segment segmentAux = vecSegments.get(i);

            if (segmentAux.intersectsWithSegmentXY(segment, null)) {
                return true;
            }
        }

        return false;
    }

    public boolean intersectsXY(Triangle triangle) {
        // This function returns true if the triangles intersects in the XY plane.
        // 1rst check if intersects the bounding rectangles.
        BoundingRectangle boundingRectangle1 = getBoundingRectangleXY();
        BoundingRectangle boundingRectangle2 = triangle.getBoundingRectangleXY();
        if (!boundingRectangle1.intersects(boundingRectangle2)) {
            return false;
        }

        // 2nd check if intersects the segments.
        List<Segment> vecSegments1 = new ArrayList<>();
        getSegments(vecSegments1);
        int segmentsCount1 = vecSegments1.size();
        for (int i = 0; i < segmentsCount1; i++) {
            Segment segment1 = vecSegments1.get(i);

            if (triangle.intersectsXY(segment1)) {
                return true;
            }
        }

        return false;
    }


    public boolean intersectsWithTrianglesArrayXY(List<Triangle> vecTriangles) {
        // This function returns true if the triangle intersects with any triangle in the array.
        int trianglesCount = vecTriangles.size();
        for (int i = 0; i < trianglesCount; i++) {
            Triangle triangleAux = vecTriangles.get(i);
            if (this.intersectsXY(triangleAux)) {
                return true;
            }
        }
        return false;
    }

    public boolean isPointInsideCircumcircleXY(Vertex vertex) {
        // This function returns true if the given point (p) lies inside the circumcircle
        // made up by points (p0, p1, p2) in XY plane
        // The circumcircle centre is returned in (out_pCC) and the radius r
        // NOTE: A point on the edge is inside the circumcircle
        // if its distance to the centre is less than or equal to the radius.
        Point3D A = vertex_0.point3d;
        Point3D B = vertex_1.point3d;
        Point3D C = vertex_2.point3d;
        Point3D D = vertex.point3d;

        // https://es.wikipedia.org/wiki/Triangulaci%C3%B3n_de_Delaunay
        // now, calculate the determinant of the matrix.
        //        | Ax Ay (Ax^2 + Ay^2) 1 |
        //        | Bx By (Bx^2 + By^2) 1 |
        //        | Cx Cy (Cx^2 + Cy^2) 1 |
        //        | Dx Dy (Dx^2 + Dy^2) 1 |
        // and the matrix 3x3 is:
        //        | Ax-Dx Ay-Dy (Ax - Dx)^2 + (Ay - Dy)^2 |
        //        | Bx-Dx By-Dy (Bx - Dx)^2 + (By - Dy)^2 |
        //        | Cx-Dx Cy-Dy (Cx - Dx)^2 + (Cy - Dy)^2 |
        // and the determinant is:
        //        | Ax-Dx Ay-Dy (Ax - Dx)^2 + (Ay - Dy)^2 |
        //        | Bx-Dx By-Dy (Bx - Dx)^2 + (By - Dy)^2 |
        //        | Cx-Dx Cy-Dy (Cx - Dx)^2 + (Cy - Dy)^2 |
        // det = (Ax-Dx)(By-Dy)(Cx-Dx)(Ay-Dy)(Bx-Dx)(Cy-Dy) - (Ax-Dx)(Cy-Dy)(Cx-Dx)(Ay-Dy)(Bx-Dx)(By-Dy)

        double a11 = A.x - D.x;
        double a12 = A.y - D.y;
        double a13 = (a11 * a11) + (a12 * a12);
        double a21 = B.x - D.x;
        double a22 = B.y - D.y;
        double a23 = (a21 * a21) + (a22 * a22);
        double a31 = C.x - D.x;
        double a32 = C.y - D.y;
        double a33 = (a31 * a31) + (a32 * a32);

        double det = (a11 * a22 * a33) + (a12 * a23 * a31) + (a13 * a21 * a32) - (a13 * a22 * a31) - (a12 * a21 * a33) - (a11 * a23 * a32);

        if (det > 0.0) {
            return true;
        } else if (det < 0.0) {
            return false;
        } else {
            // the determinant is zero.
            // then the point is on the circumcircle.
            return false;
        }

    }
}
