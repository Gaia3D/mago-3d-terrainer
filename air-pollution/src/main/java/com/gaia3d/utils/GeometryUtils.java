package com.gaia3d.utils;

import com.gaia3d.geometry.BoundingBox;
import com.gaia3d.geometry.BoundingRectangle;
import com.gaia3d.geometry.Point3D;
import com.gaia3d.geometry.Vertex;
import org.joml.Vector2d;

import java.util.List;

public class GeometryUtils {
    public static Boolean AproxEqual(double valor, double valor_a_comparar, double error) {
        return valor < (valor_a_comparar + error) && valor > (valor_a_comparar - error);
    }

    public static float mod(float a, float b) {
        return (float) (a - b * Math.floor(a / b));
    }

    public static void encodeFloat(float value, byte[] result) {
        float[] bit_shift = {16777216.0f, 65536.0f, 256.0f, 1.0f};
        float[] bit_mask = {0.0f, 0.00390625f, 0.00390625f, 0.00390625f};

        float[] value_A = {value * bit_shift[0] * 255.0f, value * bit_shift[1] * 255.0f, value * bit_shift[2] * 255.0f, value * bit_shift[3] * 255.0f};
        float[] value_B = {256.0f, 256.0f, 256.0f, 256.0f};

        float[] resAux = {(mod(value_A[0], value_B[0])) / 255.0f, (mod(value_A[1], value_B[1])) / 255.0f, (mod(value_A[2], value_B[2])) / 255.0f, (mod(value_A[3], value_B[3])) / 255.0f};

        float[] resBitMasked = {resAux[0] * bit_mask[0], resAux[0] * bit_mask[1], resAux[1] * bit_mask[2], resAux[2] * bit_mask[3]};

        float[] res = {resAux[0] - resBitMasked[0], resAux[1] - resBitMasked[1], resAux[2] - resBitMasked[2], resAux[3] - resBitMasked[3]};

        // reverse the result.
        float[] reversedResult = {res[3], res[2], res[1], res[0]};

        result[0] = (byte) (reversedResult[0] * 255.0f);
        result[1] = (byte) (reversedResult[1] * 255.0f);
        result[2] = (byte) (reversedResult[2] * 255.0f);
        result[3] = (byte) (reversedResult[3] * 255.0f);

    }

    public static void encodeFloatToInt(float value, int[] result) {

        float[] bit_shift = {16777216.0f, 65536.0f, 256.0f, 1.0f};
        float[] bit_mask = {0.0f, 0.00390625f, 0.00390625f, 0.00390625f};

        float[] value_A = {value * bit_shift[0] * 255.0f, value * bit_shift[1] * 255.0f, value * bit_shift[2] * 255.0f, value * bit_shift[3] * 255.0f};
        float[] value_B = {256.0f, 256.0f, 256.0f, 256.0f};

        float[] resAux = {(mod(value_A[0], value_B[0])) / 255.0f, (mod(value_A[1], value_B[1])) / 255.0f, (mod(value_A[2], value_B[2])) / 255.0f, (mod(value_A[3], value_B[3])) / 255.0f};
        float[] resBitMasked = {resAux[0] * bit_mask[0], resAux[0] * bit_mask[1], resAux[1] * bit_mask[2], resAux[2] * bit_mask[3]};
        float[] res = {resAux[0] - resBitMasked[0], resAux[1] - resBitMasked[1], resAux[2] - resBitMasked[2], resAux[3] - resBitMasked[3]};

        float[] reversedResult = {res[3], res[2], res[1], res[0]};

        result[0] = (int) (reversedResult[0] * 255.0f);
        result[1] = (int) (reversedResult[1] * 255.0f);
        result[2] = (int) (reversedResult[2] * 255.0f);
        result[3] = (int) (reversedResult[3] * 255.0f);

    }

    public static double crossProduct2D(Point3D v1, Point3D v2) {
        return v1.x * v2.y - v1.y * v2.x;
    }

    public static double crossProduct2D(Point3D p1, Point3D p2, Point3D p3) {
        Point3D v1 = new Point3D();
        Point3D v2 = new Point3D();
        v1.x = p2.x - p1.x;
        v1.y = p2.y - p1.y;
        v2.x = p3.x - p2.x;
        v2.y = p3.y - p2.y;
        return v1.x * v2.y - v1.y * v2.x;
    }

    public static boolean arePointsCollinealXY(Point3D point1, Point3D point2, Point3D point3, double error) {
        // This function checks if the 3 vertexes are collinear.
        double crossProduct = GeometryUtils.crossProduct2D(point1, point2, point3);

        return crossProduct < error && crossProduct > -error;
    }

    public static boolean areVertexesCollinealXY(Vertex vertex0, Vertex vertex1, Vertex vertex2, double error) {
        // This function checks if the 3 vertexes are collinear.
        return GeometryUtils.arePointsCollinealXY(vertex0.point3d, vertex1.point3d, vertex2.point3d, error);
    }

    public static void translateVertexes(List<Vertex> vecVertex, double tx, double ty, double tz) {
        int vertexCount = vecVertex.size();
        for (int i = 0; i < vertexCount; i++) {
            Vertex vertex = vecVertex.get(i);
            vertex.point3d.x += tx;
            vertex.point3d.y += ty;
            vertex.point3d.z += tz;
        }
    }

    public static void setIdxInList(List<Vertex> vecVertex) {
        int vertexCount = vecVertex.size();
        for (int i = 0; i < vertexCount; i++) {
            Vertex vertex = vecVertex.get(i);
            vertex.idxInList = i;
        }
    }

    public static BoundingBox calculateBoundingBox(List<Vertex> vecVertex) {
        BoundingBox resultBoundingBox = new BoundingBox();
        int vertexCount = vecVertex.size();
        for (int i = 0; i < vertexCount; i++) {
            Vertex vertex = vecVertex.get(i);
            if (i == 0) {
                resultBoundingBox.init(vertex.point3d.x, vertex.point3d.y, vertex.point3d.z);
            } else {
                resultBoundingBox.addPoint(vertex.point3d.x, vertex.point3d.y, vertex.point3d.z);
            }
        }
        return resultBoundingBox;
    }

    public static Vector2d GetMinMaxValuesVectorDoubles(List<Double> vec_doubles) {
        Vector2d minMax = new Vector2d();

        int valuesCount = vec_doubles.size();
        for (int i = 0; i < valuesCount; i++) {
            double val = vec_doubles.get(i)
                    .doubleValue();
            if (i == 0) {
                minMax.set(val, val);
            } else {
                if (val < minMax.x) {
                    minMax.x = val;
                } else if (val > minMax.y) {
                    minMax.y = val;
                }
            }
        }

        return minMax;
    }

    public static BoundingRectangle GetBoundingRectangleOfDouble2Positions(List<Double> vec_double2Positions) {
        BoundingRectangle bRect = new BoundingRectangle();

        int positionsCount = vec_double2Positions.size() / 2;
        for (int i = 0; i < positionsCount; i++) {
            Double x = vec_double2Positions.get(i * 2);
            Double y = vec_double2Positions.get(i * 2 + 1);

            if (i == 0) {
                bRect.minX = x; // bbox min x.
                bRect.minY = y; // bbox min y.
                bRect.maxX = x; // bbox max x.
                bRect.maxY = y; // bbox max y.
            } else {
                // x.
                if (x < bRect.minX) {
                    bRect.minX = x;
                } else if (x > bRect.maxX) {
                    bRect.maxX = x;
                }

                // y.
                if (y < bRect.minY) {
                    bRect.minY = y;
                } else if (y > bRect.maxY) {
                    bRect.maxY = y;
                }

            }
        }

        return bRect;
    }

    public static BoundingBox GetBoundingBoxOfDouble3Positions(List<Double> vec_double3Positions) {
        BoundingBox bbox = new BoundingBox();
        int positionsCount = vec_double3Positions.size() / 3;
        for (int i = 0; i < positionsCount; i++) {
            Double x = vec_double3Positions.get(i * 3);
            Double y = vec_double3Positions.get(i * 3 + 1);
            Double z = vec_double3Positions.get(i * 3 + 2);

            if (i == 0) {
                bbox.minX = x; // bbox min x.
                bbox.minY = y; // bbox min y.
                bbox.minZ = z; // bbox min z.
                bbox.maxX = x; // bbox max x.
                bbox.maxY = y; // bbox max y.
                bbox.maxZ = z; // bbox max z.
            } else {
                // x.
                if (x < bbox.minX) {
                    bbox.minX = x;
                } else if (x > bbox.maxX) {
                    bbox.maxX = x;
                }

                // y.
                if (y < bbox.minY) {
                    bbox.minY = y;
                } else if (y > bbox.maxY) {
                    bbox.maxY = y;
                }

                // z.
                if (z < bbox.minZ) {
                    bbox.minZ = z;
                } else if (z > bbox.maxZ) {
                    bbox.maxZ = z;
                }
            }
        }
        return bbox;
    }
}
