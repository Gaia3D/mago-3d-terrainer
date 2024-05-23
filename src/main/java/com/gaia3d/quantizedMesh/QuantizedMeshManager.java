package com.gaia3d.quantizedMesh;

import com.gaia3d.basic.structure.*;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.wgs84Tiles.TileWgs84;
import org.joml.*;

import java.lang.Math;
import java.util.List;

public class QuantizedMeshManager {
    //https://github.com/CesiumGS/cesium/blob/master/Source/Core/CesiumTerrainProvider.js#L327

    private Vector2f signNotZero(Vector2f value) {
        return new Vector2f(value.x >= 0 ? 1 : -1, value.y >= 0 ? 1 : -1);
    }

    private Vector2f float32x3ToOct(Vector3f normal) {
        float x = normal.x;
        float y = normal.y;
        float z = normal.z;

        float den = Math.abs(x) + Math.abs(y) + Math.abs(z);
        float u = x / den;
        float v = y / den;
        Vector2f p = new Vector2f(u, v);

        // Reflect the folds of the lower hemisphere over the diagonals
        if (z <= 0) {
            u = (float) ((1.0 - Math.abs(p.y)) * signNotZero(p).x);
            v = (float) ((1.0 - Math.abs(p.x)) * signNotZero(p).y);
            p = new Vector2f(u, v);
        }

        return p;
    }

    public byte[] guardarOctEncodedNormal(Vector3f normal) {
        float x = normal.x;
        float y = normal.y;
        float z = normal.z;

        // Proyectar el vector tridimensional en coordenadas octogonales
        float pX = x * (1.0f / (Math.abs(x) + Math.abs(y) + Math.abs(z)));
        float pY = y * (1.0f / (Math.abs(x) + Math.abs(y) + Math.abs(z)));

        // Determinar si es necesario reflejar los pliegues de la mitad inferior
        boolean reflejar = z <= 0.0f;

        // Calcular los valores de los bytes
        int bX = Math.round(pX * 127) & 0xFF; // Convertir de [-1, 1] a [0, 254]
        int bY = Math.round(pY * 127) & 0xFF; // Convertir de [-1, 1] a [0, 254]

        if (reflejar) {
            bX = 254 - bX; // Reflejar sobre la diagonal
            bY = 254 - bY; // Reflejar sobre la diagonal
        }

        // Guardar los valores de los bytes en un arreglo
        return new byte[]{(byte) bX, (byte) bY};
    }

    private Vector3f octToFloat32x3(Vector2f e) {
        Vector3f v = new Vector3f(e.x, e.y, 1.0f - Math.abs(e.x) - Math.abs(e.y));
        if(v.z < 0) {
            Vector2f temp = new Vector2f(v.x, v.y);
            v.x = (1.0f - Math.abs(v.y)) * signNotZero(temp).x;
            v.y = (1.0f - Math.abs(temp.x)) * signNotZero(temp).y;
        }
        return v.normalize();
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float dot(Vector3f a, Vector3f b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    private Vector2f float32x3ToOctnPrecise(Vector3f normal, int n) {
        Vector2f s = float32x3ToOct(normal);// Remap to the square
        // Each snorm’s max value interpreted as an integer,
        // e.g., 127.0 for snorm8

        float M = (float) ((1 << (n/2 - 1)) - 1);
        // Remap components to snorm(n/2) precision...with floor instead
        // of round (see equation 1)
        s.x = (float) (Math.floor(clamp(s.x, -1.0f, 1.0f) * M) * (1.0f / M));
        s.y = (float) (Math.floor(clamp(s.y, -1.0f, 1.0f) * M) * (1.0f / M));

        Vector2f bestRepresentation = s;
        float highestCosine = dot(octToFloat32x3(s), normal);

        // Test all combinations of floor and ceil and keep the best.
        // Note that at +/- 1, this will exit the square... but that
        // will be a worse encoding and never win.
        for (int i = 0; i <= 1; ++i)
            for (int j = 0; j <= 1; ++j)
                // This branch will be evaluated at compile time
                if ((i != 0) || (j != 0)) {

                    Vector2f candidate = new Vector2f(i * (1.0f/M) + s.x, j * (1.0f/M) + s.y);
                    float cosine = dot(octToFloat32x3(candidate), normal);
                    if (cosine > highestCosine) {
                        highestCosine = cosine;
                        bestRepresentation = candidate;
                    }
                }

        return bestRepresentation;
    }

    public QuantizedMesh getQuantizedMeshFromTile(TileWgs84 tile, boolean calculateNormals) {
        // 1rst get the quantized mesh header
        QuantizedMeshHeader header = new QuantizedMeshHeader();
        GaiaMesh mesh = tile.mesh;

        if (mesh == null) return null;

        List<GaiaVertex> vertices = mesh.vertices;
        int vertexCount = vertices.size();
        if (vertexCount == 0) return null;

        mesh.setObjectsIdInList();

        // Calculate the minimum and maximum heights & bbox
        GaiaBoundingBox bboxWC = new GaiaBoundingBox();
        double minimumHeight = Double.MAX_VALUE;
        double maximumHeight = -Double.MAX_VALUE;
        for (int i = 0; i < vertexCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            double height = vertex.getPosition().z;
            if (height < minimumHeight) minimumHeight = height;
            if (height > maximumHeight) maximumHeight = height;

            // calculate the bbox in world coordinates
            double[] posWC = GlobeUtils.geographicToCartesianWgs84(vertex.getPosition().x, vertex.getPosition().y, height);
            bboxWC.addPoint(posWC[0], posWC[1], posWC[2]);
        }
        double midHeight = (minimumHeight + maximumHeight) / 2.0;

        // Calculate the center of the tile in Earth-centered Fixed coordinates
        GeographicExtension geographicExtension = tile.geographicExtension;
        double midLonDeg = geographicExtension.getMidLongitudeDeg();
        double midLatDeg = geographicExtension.getMidLatitudeDeg();

        double[] cartesianWC = GlobeUtils.geographicToCartesianWgs84(midLonDeg, midLatDeg, midHeight);

        header.CenterX = cartesianWC[0];
        header.CenterY = cartesianWC[1];
        header.CenterZ = cartesianWC[2];

        header.MinimumHeight = (float) minimumHeight;
        header.MaximumHeight = (float) maximumHeight;

        // Calculate the bounding sphere
        Vector3d centerWC = bboxWC.getCenter();
        double radius = bboxWC.getLongestDistance() / 2.0;

        header.BoundingSphereCenterX = centerWC.x;
        header.BoundingSphereCenterY = centerWC.y;
        header.BoundingSphereCenterZ = centerWC.z;
        header.BoundingSphereRadius = radius;

        // Calculate the horizon occlusion point
        // https://cesium.com/blog/2013/05/09/computing-the-horizon-occlusion-point/
        Vector3d horizonOccPoint = calculateHorizonOcclusionPoint(bboxWC);

        header.HorizonOcclusionPointX = horizonOccPoint.x;
        header.HorizonOcclusionPointY = horizonOccPoint.y;
        header.HorizonOcclusionPointZ = horizonOccPoint.z;

        // Now, calculate the quantized mesh***************************************************************************
        QuantizedMesh quantizedMesh = new QuantizedMesh();
        quantizedMesh.header = header;

        quantizedMesh.vertexCount = vertexCount;
        quantizedMesh.triangleCount = mesh.triangles.size();

        quantizedMesh.uBuffer = new short[vertexCount];
        quantizedMesh.vBuffer = new short[vertexCount];
        quantizedMesh.heightBuffer = new short[vertexCount];
        quantizedMesh.triangleIndices = new int[quantizedMesh.triangleCount * 3];

        double minLonDeg = geographicExtension.getMinLongitudeDeg();
        double maxLonDeg = geographicExtension.getMaxLongitudeDeg();
        double minLatDeg = geographicExtension.getMinLatitudeDeg();
        double maxLatDeg = geographicExtension.getMaxLatitudeDeg();

        double lonRange = maxLonDeg - minLonDeg;
        double latRange = maxLatDeg - minLatDeg;
        double heightRange = maximumHeight - minimumHeight;
        if (heightRange == 0.0) heightRange = 1.0;

        double lonScale = lonRange / 32767.0;
        double latScale = latRange / 32767.0;
        double heightScale = 32767.0 / heightRange;


        for (int i = 0; i < vertexCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            /*if (vertex.getObjectStatus() == GaiaObjectStatus.DELETED) {

            }*/
            double lonDeg = vertex.getPosition().x;
            double latDeg = vertex.getPosition().y;
            double height = vertex.getPosition().z;

            quantizedMesh.uBuffer[i] = (short) ((lonDeg - minLonDeg) / lonScale);
            quantizedMesh.vBuffer[i] = (short) ((latDeg - minLatDeg) / latScale);
            quantizedMesh.heightBuffer[i] = (short) ((height - minimumHeight) * heightScale);
        }

        for (int i = 0; i < quantizedMesh.triangleCount; i++) {
            GaiaTriangle triangle = mesh.triangles.get(i);
            List<GaiaVertex> triVertices = triangle.getVertices();
            quantizedMesh.triangleIndices[i * 3] = triVertices.get(0).getId();
            quantizedMesh.triangleIndices[i * 3 + 1] = triVertices.get(1).getId();
            quantizedMesh.triangleIndices[i * 3 + 2] = triVertices.get(2).getId();
        }

        // now, edgesIndices
        // west vertices
        List<GaiaVertex> westVertices = mesh.getLeftVerticesSortedUpToDown(); // original
        int westVerticesCount = westVertices.size();
        quantizedMesh.westIndices = new int[westVerticesCount];
        quantizedMesh.westVertexCount = westVerticesCount;
        for (int i = 0; i < westVerticesCount; i++) {
            quantizedMesh.westIndices[i] = westVertices.get(i).getId();
        }

        // south vertices
        List<GaiaVertex> southVertices = mesh.getDownVerticesSortedLeftToRight();
        int southVerticesCount = southVertices.size();
        quantizedMesh.southIndices = new int[southVerticesCount];
        quantizedMesh.southVertexCount = southVerticesCount;
        for (int i = 0; i < southVerticesCount; i++) {
            quantizedMesh.southIndices[i] = southVertices.get(i).getId();
        }

        // east vertices
        List<GaiaVertex> eastVertices = mesh.getRightVerticesSortedDownToUp();
        int eastVerticesCount = eastVertices.size();
        quantizedMesh.eastIndices = new int[eastVerticesCount];
        quantizedMesh.eastVertexCount = eastVerticesCount;
        for (int i = 0; i < eastVerticesCount; i++) {
            quantizedMesh.eastIndices[i] = eastVertices.get(i).getId();
        }

        // north vertices
        List<GaiaVertex> northVertices = mesh.getUpVerticesSortedRightToLeft();
        int northVerticesCount = northVertices.size();
        quantizedMesh.northIndices = new int[northVerticesCount];
        quantizedMesh.northVertexCount = northVerticesCount;
        for (int i = 0; i < northVerticesCount; i++) {
            quantizedMesh.northIndices[i] = northVertices.get(i).getId();
        }

        // check if save normals
        if(calculateNormals) {
            Matrix4d tMat = GlobeUtils.transformMatrixAtCartesianPointWgs84(cartesianWC[0], cartesianWC[1], cartesianWC[2]);
            Matrix3d rotMat = new Matrix3d();
            tMat.get3x3(rotMat);

            Matrix3d rotMatInv = new Matrix3d();
            rotMatInv.invert(rotMat);

            // Calculate the normals
            quantizedMesh.octEncodedNormals = new byte[vertexCount * 2];
            for (int i = 0; i < vertexCount; i++) {
                GaiaVertex vertex = vertices.get(i);
                Vector3f normal = vertex.getNormal();
                if(normal == null)
                {
                    normal = new Vector3f(0, 0, 1);
                }

                // rotate normal
                Vector3f normalRotated = new Vector3f(normal.x, normal.z, -normal.y); // best result (-90 in xAxis)
                rotMatInv.transform(normalRotated); // test

                Vector2f octNormal = float32x3ToOct(normalRotated);

                quantizedMesh.octEncodedNormals[i * 2] = (byte) (octNormal.x * 254);
                quantizedMesh.octEncodedNormals[i * 2 + 1] = (byte) (octNormal.y * 254);
            }

            // Terrain Lighting
            // Name: Oct-Encoded Per-Vertex Normals
            // extension Id: 1
            quantizedMesh.extensionId = 1; // byte
            // the size in bytes of "quantizedMesh.octEncodedNormals" is vertexCount * 2
            quantizedMesh.extensionLength = vertexCount * 2; // int

        }

        return quantizedMesh;
    }

    public Vector3d calculateHorizonOcclusionPoint(GaiaBoundingBox bboxWC) {
        Vector3d centerWC = bboxWC.getCenter();
        double radius = bboxWC.getLongestDistance() / 2.0;

        // Calculate the horizon occlusion point
        // https://cesium.com/blog/2013/05/09/computing-the-horizon-occlusion-point/
        double[] centerCartesian = new double[3];
        centerCartesian[0] = centerWC.x;
        centerCartesian[1] = centerWC.y;
        centerCartesian[2] = centerWC.z;

        Vector3d centerCartographic = GlobeUtils.cartesianToGeographicWgs84(centerCartesian[0], centerCartesian[1], centerCartesian[2]);
        double centerLonDeg = centerCartographic.x;
        double centerLatDeg = centerCartographic.y;
        double centerHeight = centerCartographic.z;

        double centerLonRad = centerLonDeg * GlobeUtils.getDegToRadFactor();
        double centerLatRad = centerLatDeg * GlobeUtils.getDegToRadFactor();

        double cosLat = Math.cos(centerLatRad);
        double sinLat = Math.sin(centerLatRad);
        double cosLon = Math.cos(centerLonRad);
        double sinLon = Math.sin(centerLonRad);

        double x = cosLat * cosLon;
        double y = cosLat * sinLon;
        double z = sinLat;

        double occlusionPointX = centerCartesian[0] + x * radius;
        double occlusionPointY = centerCartesian[1] + y * radius;
        double occlusionPointZ = centerCartesian[2] + z * radius;

        return new Vector3d(occlusionPointX, occlusionPointY, occlusionPointZ);
    }
}
