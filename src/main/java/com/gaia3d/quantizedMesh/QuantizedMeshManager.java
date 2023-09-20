package com.gaia3d.quantizedMesh;

import com.gaia3d.basic.structure.*;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.wgs84Tiles.TileWgs84;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Collections;

public class QuantizedMeshManager
{
    //https://github.com/CesiumGS/cesium/blob/master/Source/Core/CesiumTerrainProvider.js#L327

    public QuantizedMesh getQuantizedMeshFromTile(TileWgs84 tile)
    {
        // 1rst get the quantized mesh header.***
        QuantizedMeshHeader header = new QuantizedMeshHeader();
        GaiaMesh mesh = tile.mesh;

        if(mesh == null)
            return null;

        ArrayList<GaiaVertex> vertices = mesh.vertices;
        int vertexCount = vertices.size();
        if(vertexCount == 0)
            return null;

        mesh.setObjectsIdInList();

        // Calculate the minimum and maximum heights & bbox.***
        GaiaBoundingBox bboxWC = new GaiaBoundingBox();
        double minimumHeight = Double.MAX_VALUE;
        double maximumHeight = -Double.MAX_VALUE;
        for(int i = 0; i < vertexCount; i++)
        {
            GaiaVertex vertex = vertices.get(i);
            double height = vertex.position.z;
            if(height < minimumHeight)
                minimumHeight = height;
            if(height > maximumHeight)
                maximumHeight = height;

            // calculate the bbox in world coordinates.***
            double posWC[] = GlobeUtils.geographicToCartesianWgs84(vertex.position.x, vertex.position.y, height);
            bboxWC.addPoint(posWC[0], posWC[1], posWC[2]);
        }
        double midHeight = (minimumHeight + maximumHeight) / 2.0;

        // Calculate the center of the tile in Earth-centered Fixed coordinates.***
        GeographicExtension geographicExtension = tile.geographicExtension;
        double midLonDeg = geographicExtension.getMidLongitudeDeg();
        double midLatDeg = geographicExtension.getMidLatitudeDeg();

        double cartesianWC[] = GlobeUtils.geographicToCartesianWgs84(midLonDeg, midLatDeg, midHeight);

        header.CenterX = cartesianWC[0];
        header.CenterY = cartesianWC[1];
        header.CenterZ = cartesianWC[2];

        header.MinimumHeight = (float)minimumHeight;
        header.MaximumHeight = (float)maximumHeight;

        // Calculate the bounding sphere.***
        Vector3d centerWC = bboxWC.getCenter();
        double radius = bboxWC.getLongestDistance() / 2.0;

        header.BoundingSphereCenterX = centerWC.x;
        header.BoundingSphereCenterY = centerWC.y;
        header.BoundingSphereCenterZ = centerWC.z;
        header.BoundingSphereRadius = radius;

        // Calculate the horizon occlusion point.***
        // https://cesium.com/blog/2013/05/09/computing-the-horizon-occlusion-point/
        Vector3d horizonOccPoint = calculateHorizonOcclusionPoint(bboxWC);

        header.HorizonOcclusionPointX = horizonOccPoint.x;
        header.HorizonOcclusionPointY = horizonOccPoint.y;
        header.HorizonOcclusionPointZ = horizonOccPoint.z;

        // Now, calculate the quantized mesh.******************************************************************************
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
        if(heightRange == 0.0)
            heightRange = 1.0;

        double lonScale = lonRange / 32767.0;
        double latScale = latRange / 32767.0;
        double heightScale = 32767.0 / heightRange;


        for(int i = 0; i < vertexCount; i++)
        {
            GaiaVertex vertex = vertices.get(i);
            double lonDeg = vertex.position.x;
            double latDeg = vertex.position.y;
            double height = vertex.position.z;

            quantizedMesh.uBuffer[i] = (short)((lonDeg - minLonDeg) / lonScale);
            quantizedMesh.vBuffer[i] = (short)((latDeg - minLatDeg) / latScale);
            quantizedMesh.heightBuffer[i] = (short)((height - minimumHeight) * heightScale);
        }

        for(int i = 0; i < quantizedMesh.triangleCount; i++)
        {
            GaiaTriangle triangle = mesh.triangles.get(i);
            ArrayList<GaiaVertex> triVertices = triangle.getVertices();
            quantizedMesh.triangleIndices[i * 3] = triVertices.get(0).id;
            quantizedMesh.triangleIndices[i * 3 + 1] = triVertices.get(1).id;
            quantizedMesh.triangleIndices[i * 3 + 2] = triVertices.get(2).id;
        }

        // now, edgesIndices.***
        // west vertices.***
        ArrayList<GaiaVertex> westVertices = mesh.getLeftVerticesSortedUpToDown(); // original.***
        int westVerticesCount = westVertices.size();
        quantizedMesh.westIndices = new int[westVerticesCount];
        quantizedMesh.westVertexCount = westVerticesCount;
        for(int i = 0; i < westVerticesCount; i++)
        {
            quantizedMesh.westIndices[i] = westVertices.get(i).id;
        }

        // south vertices.***
        ArrayList<GaiaVertex> southVertices = mesh.getDownVerticesSortedLeftToRight();
        int southVerticesCount = southVertices.size();
        quantizedMesh.southIndices = new int[southVerticesCount];
        quantizedMesh.southVertexCount = southVerticesCount;
        for(int i = 0; i < southVerticesCount; i++)
        {
            quantizedMesh.southIndices[i] = southVertices.get(i).id;
        }

        // east vertices.***
        ArrayList<GaiaVertex> eastVertices = mesh.getRightVerticesSortedDownToUp();
        int eastVerticesCount = eastVertices.size();
        quantizedMesh.eastIndices = new int[eastVerticesCount];
        quantizedMesh.eastVertexCount = eastVerticesCount;
        for(int i = 0; i < eastVerticesCount; i++)
        {
            quantizedMesh.eastIndices[i] = eastVertices.get(i).id;
        }

        // north vertices.***
        ArrayList<GaiaVertex> northVertices = mesh.getUpVerticesSortedRightToLeft();
        int northVerticesCount = northVertices.size();
        quantizedMesh.northIndices = new int[northVerticesCount];
        quantizedMesh.northVertexCount = northVerticesCount;
        for(int i = 0; i < northVerticesCount; i++)
        {
            quantizedMesh.northIndices[i] = northVertices.get(i).id;
        }

        return quantizedMesh;
    }

    public Vector3d calculateHorizonOcclusionPoint(GaiaBoundingBox bboxWC)
    {
        Vector3d centerWC = bboxWC.getCenter();
        double radius = bboxWC.getLongestDistance() / 2.0;

        // Calculate the horizon occlusion point.***
        // https://cesium.com/blog/2013/05/09/computing-the-horizon-occlusion-point/
        double centerCartesian[] = new double[3];
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
