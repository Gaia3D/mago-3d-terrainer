package com.gaia3d.quantizedMesh;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.terrain.structure.*;
import com.gaia3d.terrain.util.OctNormalFactory;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.terrain.tile.TileWgs84;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * QuantizedMeshManager
 * https://github.com/CesiumGS/cesium/blob/master/Source/Core/CesiumTerrainProvider.js#L327
 */
public class QuantizedMeshManager {

    private final List<TerrainHalfEdge> listHalfEdges = new ArrayList<>();
    private List<TerrainVertex> listVertices = new ArrayList<>();

    public QuantizedMesh getQuantizedMeshFromTile(TileWgs84 tile, boolean calculateNormals) {
        // First get the quantized mesh header
        QuantizedMeshHeader header = new QuantizedMeshHeader();
        TerrainMesh mesh = tile.getMesh();

        if (mesh == null) return null;

        List<TerrainVertex> vertices = mesh.vertices;
        int vertexCount = vertices.size();
        if (vertexCount == 0) return null;

        mesh.setObjectsIdInList();

        // Calculate the minimum and maximum heights & bbox
        GaiaBoundingBox bboxWC = new GaiaBoundingBox();
        double minimumHeight = Double.MAX_VALUE;
        double maximumHeight = -Double.MAX_VALUE;
        for (int i = 0; i < vertexCount; i++) {
            TerrainVertex vertex = vertices.get(i);
            double height = vertex.getPosition().z;
            if (height < minimumHeight) minimumHeight = height;
            if (height > maximumHeight) maximumHeight = height;

            // calculate the bbox in world coordinates
            double[] posWC = GlobeUtils.geographicToCartesianWgs84(vertex.getPosition().x, vertex.getPosition().y, height);
            bboxWC.addPoint(posWC[0], posWC[1], posWC[2]);
        }
        double midHeight = (minimumHeight + maximumHeight) / 2.0;

        // Calculate the center of the tile in Earth-centered Fixed coordinates
        GeographicExtension geographicExtension = tile.getGeographicExtension();
        double midLonDeg = geographicExtension.getMidLongitudeDeg();
        double midLatDeg = geographicExtension.getMidLatitudeDeg();

        double[] cartesianWC = GlobeUtils.geographicToCartesianWgs84(midLonDeg, midLatDeg, midHeight);

        header.setCenterX(cartesianWC[0]);
        header.setCenterY(cartesianWC[1]);
        header.setCenterZ(cartesianWC[2]);

        header.setMinimumHeight((float) minimumHeight);
        header.setMaximumHeight((float) maximumHeight);

        // Calculate the bounding sphere
        Vector3d centerWC = bboxWC.getCenter();
        double radius = bboxWC.getLongestDistance() / 2.0;

        header.setBoundingSphereCenterX(centerWC.x);
        header.setBoundingSphereCenterY(centerWC.y);
        header.setBoundingSphereCenterZ(centerWC.z);
        header.setBoundingSphereRadius(radius);

        // Calculate the horizon occlusion point
        // https://cesium.com/blog/2013/05/09/computing-the-horizon-occlusion-point/
        Vector3d horizonOccPoint = calculateHorizonOcclusionPoint(bboxWC);

        header.setHorizonOcclusionPointX(horizonOccPoint.x);
        header.setHorizonOcclusionPointY(horizonOccPoint.y);
        header.setHorizonOcclusionPointZ(horizonOccPoint.z);

        // Now, calculate the quantized mesh***************************************************************************
        QuantizedMesh quantizedMesh = new QuantizedMesh();
        quantizedMesh.setHeader(header);

        quantizedMesh.setVertexCount(vertexCount);
        quantizedMesh.setTriangleCount(mesh.triangles.size());

        quantizedMesh.setUBuffer(new short[vertexCount]);
        quantizedMesh.setVBuffer(new short[vertexCount]);
        quantizedMesh.setHeightBuffer(new short[vertexCount]);
        quantizedMesh.setTriangleIndices(new int[quantizedMesh.getTriangleCount() * 3]);

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
            TerrainVertex vertex = vertices.get(i);
            double lonDeg = vertex.getPosition().x;
            double latDeg = vertex.getPosition().y;
            double height = vertex.getPosition().z;

            short[] uBuffer = quantizedMesh.getUBuffer();
            short[] vBuffer = quantizedMesh.getVBuffer();
            short[] heightBuffer = quantizedMesh.getHeightBuffer();

            uBuffer[i] = (short) ((lonDeg - minLonDeg) / lonScale);
            vBuffer[i] = (short) ((latDeg - minLatDeg) / latScale);
            heightBuffer[i] = (short) ((height - minimumHeight) * heightScale);
        }

        for (int i = 0; i < quantizedMesh.getTriangleCount(); i++) {
            TerrainTriangle triangle = mesh.triangles.get(i);
            this.listVertices.clear();
            this.listHalfEdges.clear();
            this.listVertices = triangle.getVertices(this.listVertices, this.listHalfEdges);
            quantizedMesh.getTriangleIndices()[i * 3] = this.listVertices.get(0).getId();
            quantizedMesh.getTriangleIndices()[i * 3 + 1] = this.listVertices.get(1).getId();
            quantizedMesh.getTriangleIndices()[i * 3 + 2] = this.listVertices.get(2).getId();
        }

        // now, edgesIndices
        // west vertices
        List<TerrainVertex> westVertices = mesh.getLeftVerticesSortedUpToDown(); // original
        int westVerticesCount = westVertices.size();
        quantizedMesh.setWestIndices(new int[westVerticesCount]);
        quantizedMesh.setWestVertexCount(westVerticesCount);
        for (int i = 0; i < westVerticesCount; i++) {
            quantizedMesh.getWestIndices()[i] = westVertices.get(i).getId();
        }

        // south vertices
        List<TerrainVertex> southVertices = mesh.getDownVerticesSortedLeftToRight();
        int southVerticesCount = southVertices.size();
        quantizedMesh.setSouthIndices(new int[southVerticesCount]);
        quantizedMesh.setSouthVertexCount(southVerticesCount);
        for (int i = 0; i < southVerticesCount; i++) {
            quantizedMesh.getSouthIndices()[i] = southVertices.get(i).getId();
        }

        // east vertices
        List<TerrainVertex> eastVertices = mesh.getRightVerticesSortedDownToUp();
        int eastVerticesCount = eastVertices.size();
        quantizedMesh.setEastIndices(new int[eastVerticesCount]);
        quantizedMesh.setEastVertexCount(eastVerticesCount);
        for (int i = 0; i < eastVerticesCount; i++) {
            quantizedMesh.getEastIndices()[i] = eastVertices.get(i).getId();
        }

        // north vertices
        List<TerrainVertex> northVertices = mesh.getUpVerticesSortedRightToLeft();
        int northVerticesCount = northVertices.size();
        quantizedMesh.setNorthIndices(new int[northVerticesCount]);
        quantizedMesh.setNorthVertexCount(northVerticesCount);
        for (int i = 0; i < northVerticesCount; i++) {
            int[] northIndices = quantizedMesh.getNorthIndices();
            northIndices[i] = northVertices.get(i).getId();
        }

        // check if save normals
        if (calculateNormals) {

            // Calculate the normals
            quantizedMesh.setOctEncodedNormals(new byte[vertexCount * 2]);
            for (int i = 0; i < vertexCount; i++) {
                TerrainVertex vertex = vertices.get(i);
                Vector3f normal = vertex.getNormal();
                if (normal == null) {
                    normal = new Vector3f(0, 0, 1);
                }

                //Vector2f octNormal = encodeOctNormal(normal);
                byte[] octNormalBytes = OctNormalFactory.encodeOctNormalByte(normal);
                quantizedMesh.getOctEncodedNormals()[i * 2] = octNormalBytes[0];
                quantizedMesh.getOctEncodedNormals()[i * 2 + 1] = octNormalBytes[1];
            }

            // Terrain Lighting
            // Name: Oct-Encoded Per-Vertex Normals
            // extension Id: 1
            quantizedMesh.setExtensionId((byte) 1); // byte
            // the size in bytes of "quantizedMesh.octEncodedNormals" is vertexCount * 2
            quantizedMesh.setExtensionLength(vertexCount * 2); // int
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
        //double centerHeight = centerCartographic.z;

        double centerLonRad = centerLonDeg * GlobeUtils.DEGREE_TO_RADIAN_FACTOR;
        double centerLatRad = centerLatDeg * GlobeUtils.DEGREE_TO_RADIAN_FACTOR;

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
