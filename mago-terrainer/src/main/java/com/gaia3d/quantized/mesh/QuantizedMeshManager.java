package com.gaia3d.quantized.mesh;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.terrain.structure.*;
import com.gaia3d.terrain.tile.TileIndices;
import com.gaia3d.terrain.tile.TileWgs84;
import com.gaia3d.terrain.tile.TileWgs84Manager;
import com.gaia3d.terrain.util.OctNormalFactory;
import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.util.TileWgs84Utils;
import com.gaia3d.util.CelestialBody;
import com.gaia3d.util.GlobeUtils;
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

    public QuantizedMesh createQuantizedMesh(TileIndices indices, float[] elevations, int width, int height, boolean calculateNormals) {
        QuantizedMesh qm = new QuantizedMesh();
        QuantizedMeshHeader header = new QuantizedMeshHeader();
        GlobalOptions options = GlobalOptions.getInstance();
        CelestialBody body = options.getCelestialBody();
        
        // 1. Calculate height range and bounding box in WC
        float minHeight = Float.MAX_VALUE;
        float maxHeight = -Float.MAX_VALUE;
        for (float h : elevations) {
            if (Float.isNaN(h)) continue;
            if (h < minHeight) minHeight = h;
            if (h > maxHeight) maxHeight = h;
        }
        if (minHeight == Float.MAX_VALUE) {
            minHeight = 0;
            maxHeight = 1;
        }
        
        header.setMinimumHeight(minHeight);
        header.setMaximumHeight(maxHeight);

        // 2. Calculate geographic extent
        GeographicExtension geoExtent = TileWgs84Utils.getGeographicExtentOfTileLXY(indices.getL(), indices.getX(), indices.getY(), null, "CRS84", false);
        double midLon = geoExtent.getMidLongitudeDeg();
        double midLat = geoExtent.getMidLatitudeDeg();
        double midH = (minHeight + maxHeight) / 2.0;

        // 3. Set Center (ECEF)
        double[] cartesian = GlobeUtils.geographicToCartesian(midLon, midLat, midH, body);
        header.setCenterX(cartesian[0]);
        header.setCenterY(cartesian[1]);
        header.setCenterZ(cartesian[2]);

        // 4. Calculate Bounding Sphere and Occlusion Point
        // We use corners for bounding calculation
        GaiaBoundingBox bboxWC = new GaiaBoundingBox();
        double[][] corners = {
            {geoExtent.getMinLongitudeDeg(), geoExtent.getMinLatitudeDeg()},
            {geoExtent.getMaxLongitudeDeg(), geoExtent.getMinLatitudeDeg()},
            {geoExtent.getMinLongitudeDeg(), geoExtent.getMaxLatitudeDeg()},
            {geoExtent.getMaxLongitudeDeg(), geoExtent.getMaxLatitudeDeg()}
        };
        for(double[] c : corners) {
            double[] pMin = GlobeUtils.geographicToCartesian(c[0], c[1], minHeight, body);
            double[] pMax = GlobeUtils.geographicToCartesian(c[0], c[1], maxHeight, body);
            bboxWC.addPoint(pMin[0], pMin[1], pMin[2]);
            bboxWC.addPoint(pMax[0], pMax[1], pMax[2]);
        }

        org.joml.Vector3d centerWC = bboxWC.getCenter();
        header.setBoundingSphereCenterX(centerWC.x);
        header.setBoundingSphereCenterY(centerWC.y);
        header.setBoundingSphereCenterZ(centerWC.z);
        header.setBoundingSphereRadius(bboxWC.getLongestDistance() / 2.0);

        org.joml.Vector3d horizonPoint = calculateHorizonOcclusionPoint(bboxWC);
        header.setHorizonOcclusionPointX(horizonPoint.x);
        header.setHorizonOcclusionPointY(horizonPoint.y);
        header.setHorizonOcclusionPointZ(horizonPoint.z);
        
        qm.setHeader(header);
        
        // 5. Quantize coordinates.
        // Use triangle soup rather than a shared row-major grid. The project encoder writes
        // quantized-mesh indices with high-water-mark encoding, which assumes vertices are
        // introduced sequentially by the index stream. A shared grid with row-major ids breaks
        // that assumption and produces unreadable .terrain files in Cesium.
        int triangleCount = (width - 1) * (height - 1) * 2;
        int vertexCount = triangleCount * 3;
        short[] uBuffer = new short[vertexCount];
        short[] vBuffer = new short[vertexCount];
        short[] hBuffer = new short[vertexCount];
        int[] indices0 = new int[vertexCount];
        byte[] octNormals = calculateNormals ? new byte[vertexCount * 2] : null;
        int[] westSamples = new int[height];
        int[] eastSamples = new int[height];
        int[] southSamples = new int[width];
        int[] northSamples = new int[width];
        java.util.Arrays.fill(westSamples, -1);
        java.util.Arrays.fill(eastSamples, -1);
        java.util.Arrays.fill(southSamples, -1);
        java.util.Arrays.fill(northSamples, -1);

        float hRange = maxHeight - minHeight;
        if (hRange == 0) {
            hRange = 1.0f;
        }

        GeographicExtension tileExtent =
                TileWgs84Utils.getGeographicExtentOfTileLXY(indices.getL(), indices.getX(), indices.getY(), null, "CRS84", false);
        int cursor = 0;
        for (int j = 0; j < height - 1; j++) {
            for (int i = 0; i < width - 1; i++) {
                int n0 = j * width + i;
                int n1 = j * width + i + 1;
                int n2 = (j + 1) * width + i;
                int n3 = (j + 1) * width + i + 1;

                cursor = writeSoupVertex(qm, body, tileExtent, elevations, width, height, minHeight, hRange, i, j, n0,
                        uBuffer, vBuffer, hBuffer, indices0, octNormals, cursor);
                if (i == 0 && westSamples[j] < 0) {
                    westSamples[j] = cursor - 1;
                }
                if (j == 0 && southSamples[i] < 0) {
                    southSamples[i] = cursor - 1;
                }

                cursor = writeSoupVertex(qm, body, tileExtent, elevations, width, height, minHeight, hRange, i + 1, j, n1,
                        uBuffer, vBuffer, hBuffer, indices0, octNormals, cursor);
                if (j == 0 && southSamples[i + 1] < 0) {
                    southSamples[i + 1] = cursor - 1;
                }
                if (i == width - 2 && eastSamples[j] < 0) {
                    eastSamples[j] = cursor - 1;
                }

                cursor = writeSoupVertex(qm, body, tileExtent, elevations, width, height, minHeight, hRange, i + 1, j + 1, n3,
                        uBuffer, vBuffer, hBuffer, indices0, octNormals, cursor);
                if (i == width - 2 && eastSamples[j + 1] < 0) {
                    eastSamples[j + 1] = cursor - 1;
                }
                if (j == height - 2 && northSamples[i + 1] < 0) {
                    northSamples[i + 1] = cursor - 1;
                }

                cursor = writeSoupVertex(qm, body, tileExtent, elevations, width, height, minHeight, hRange, i, j, n0,
                        uBuffer, vBuffer, hBuffer, indices0, octNormals, cursor);
                if (i == 0 && westSamples[j] < 0) {
                    westSamples[j] = cursor - 1;
                }
                if (j == 0 && southSamples[i] < 0) {
                    southSamples[i] = cursor - 1;
                }

                cursor = writeSoupVertex(qm, body, tileExtent, elevations, width, height, minHeight, hRange, i + 1, j + 1, n3,
                        uBuffer, vBuffer, hBuffer, indices0, octNormals, cursor);
                if (i == width - 2 && eastSamples[j + 1] < 0) {
                    eastSamples[j + 1] = cursor - 1;
                }
                if (j == height - 2 && northSamples[i + 1] < 0) {
                    northSamples[i + 1] = cursor - 1;
                }

                cursor = writeSoupVertex(qm, body, tileExtent, elevations, width, height, minHeight, hRange, i, j + 1, n2,
                        uBuffer, vBuffer, hBuffer, indices0, octNormals, cursor);
                if (i == 0 && westSamples[j + 1] < 0) {
                    westSamples[j + 1] = cursor - 1;
                }
                if (j == height - 2 && northSamples[i] < 0) {
                    northSamples[i] = cursor - 1;
                }
            }
        }

        qm.setUBuffer(uBuffer);
        qm.setVBuffer(vBuffer);
        qm.setHeightBuffer(hBuffer);
        qm.setTriangleIndices(indices0);
        qm.setVertexCount(vertexCount);
        qm.setTriangleCount(triangleCount);

        int[] west = new int[height];
        for (int j = 0; j < height; j++) {
            west[j] = westSamples[height - 1 - j];
        }
        qm.setWestIndices(west);
        qm.setWestVertexCount(height);

        int[] east = new int[height];
        for (int j = 0; j < height; j++) {
            east[j] = eastSamples[j];
        }
        qm.setEastIndices(east);
        qm.setEastVertexCount(height);

        qm.setSouthIndices(southSamples);
        qm.setSouthVertexCount(width);

        int[] north = new int[width];
        for (int i = 0; i < width; i++) {
            north[i] = northSamples[width - 1 - i];
        }
        qm.setNorthIndices(north);
        qm.setNorthVertexCount(width);

        if (calculateNormals) {
            qm.setOctEncodedNormals(octNormals);
            qm.setExtensionId((byte) 1);
            qm.setExtensionLength(vertexCount * 2);
        }

        return qm;
    }

    private int writeSoupVertex(
            QuantizedMesh qm,
            CelestialBody body,
            GeographicExtension tileExtent,
            float[] elevations,
            int width,
            int height,
            float minHeight,
            float hRange,
            int gridX,
            int gridY,
            int elevationIndex,
            short[] uBuffer,
            short[] vBuffer,
            short[] hBuffer,
            int[] triangleIndices,
            byte[] octNormals,
            int cursor) {
        uBuffer[cursor] = (short) (gridX * 32767 / (width - 1));
        vBuffer[cursor] = (short) (gridY * 32767 / (height - 1));
        float h = Float.isNaN(elevations[elevationIndex]) ? minHeight : elevations[elevationIndex];
        hBuffer[cursor] = (short) ((h - minHeight) * 32767 / hRange);
        triangleIndices[cursor] = cursor;

        if (octNormals != null) {
            double lon = tileExtent.getMinLongitudeDeg() +
                    (tileExtent.getLongitudeRangeDegree() * gridX / (double) (width - 1));
            double lat = tileExtent.getMinLatitudeDeg() +
                    (tileExtent.getLatitudeRangeDegree() * gridY / (double) (height - 1));
            double[] cartesian = GlobeUtils.geographicToCartesian(lon, lat, 0.0, body);
            Vector3f normal = new Vector3f((float) cartesian[0], (float) cartesian[1], (float) cartesian[2]).normalize();
            byte[] octNormalBytes = OctNormalFactory.encodeOctNormalByte(normal);
            octNormals[cursor * 2] = octNormalBytes[0];
            octNormals[cursor * 2 + 1] = octNormalBytes[1];
        }

        return cursor + 1;
    }

    public TileWgs84 getTileWgs84FromQuantizedMesh(QuantizedMesh quantizedMesh, TileIndices tileIndices, TileWgs84Manager tileManager) {
        // First get the quantized mesh header
        QuantizedMeshHeader header = quantizedMesh.getHeader();
        if (header == null) {return null;}

        // calculate the geographic extension by tileIndices
        String imaginaryType = tileManager.getImaginaryType();
        GeographicExtension geoExtension = TileWgs84Utils.getGeographicExtentOfTileLXY(tileIndices.getL(), tileIndices.getX(), tileIndices.getY(), null, imaginaryType, tileManager.originIsLeftUp());
        if (geoExtension == null) {
            return null;
        }

        TileWgs84 resultTile = new TileWgs84(null, tileManager);
        TerrainMesh mesh = new TerrainMesh();
        resultTile.setMesh(mesh);
        List<TerrainVertex> vertices = mesh.getVertices();
        List<TerrainHalfEdge> halfEdges = mesh.getHalfEdges();
        List<TerrainTriangle> triangles = mesh.getTriangles();

        // 1rst, make the TerrainVertices
        // TerrainVertex has the lonDeg, latDeg, height
        short[] uBuffer = quantizedMesh.getUBuffer();
        short[] vBuffer = quantizedMesh.getVBuffer();
        short[] heightBuffer = quantizedMesh.getHeightBuffer();
        double minHeight = header.getMinimumHeight();
        double maxHeight = header.getMaximumHeight();
        double heightRange = maxHeight - minHeight;
        if (heightRange == 0.0) {heightRange = 1.0;}
        double minLonDeg = geoExtension.getMinLongitudeDeg();
        double maxLonDeg = geoExtension.getMaxLongitudeDeg();
        double minLatDeg = geoExtension.getMinLatitudeDeg();
        double maxLatDeg = geoExtension.getMaxLatitudeDeg();
        double lonDegRange = geoExtension.getLongitudeRangeDegree();
        double latDegRange = geoExtension.getLatitudeRangeDegree();
        double lonScale = lonDegRange / 32767.0;
        double latScale = latDegRange / 32767.0;
        double heightScale = heightRange / 32767.0;

        int vertexCount = quantizedMesh.getVertexCount();
        for (int i = 0; i < vertexCount; i++) {
            int u = uBuffer[i] & 0xFFFF;
            int v = vBuffer[i] & 0xFFFF;
            int h = heightBuffer[i] & 0xFFFF;

            double lonDeg = u * lonScale + minLonDeg;
            double latDeg = v * latScale + minLatDeg;
            double height = h * heightScale + minHeight;

            // clamp vertices in to the tile's geographic extent
            if (lonDeg < minLonDeg) {
                lonDeg = minLonDeg;
            } else if (lonDeg > maxLonDeg) {
                lonDeg = maxLonDeg;
            }

            if (latDeg < minLatDeg) {
                latDeg = minLatDeg;
            } else if (latDeg > maxLatDeg) {
                latDeg = maxLatDeg;
            }

            TerrainVertex vertex = new TerrainVertex();
            vertex.setPosition(new Vector3d(lonDeg, latDeg, height));
            vertex.setId(i);
            vertices.add(vertex);
        }

        // for edge vertices, clamp to the tile's geographic extent
        int downVerticesCount = quantizedMesh.getSouthVertexCount();
        for (int i = 0; i < downVerticesCount; i++) {
            int index = quantizedMesh.getSouthIndices()[i];
            TerrainVertex vertex = vertices.get(index);
            double latDeg = vertex.getPosition().y;
            if (latDeg != minLatDeg) {
                latDeg = minLatDeg;
                vertex.getPosition().y = latDeg;
            }
        }

        int upVerticesCount = quantizedMesh.getNorthVertexCount();
        for (int i = 0; i < upVerticesCount; i++) {
            int index = quantizedMesh.getNorthIndices()[i];
            TerrainVertex vertex = vertices.get(index);
            double latDeg = vertex.getPosition().y;
            if (latDeg != maxLatDeg) {
                latDeg = maxLatDeg;
                vertex.getPosition().y = latDeg;
            }
        }

        int leftVerticesCount = quantizedMesh.getWestVertexCount();
        for (int i = 0; i < leftVerticesCount; i++) {
            int index = quantizedMesh.getWestIndices()[i];
            TerrainVertex vertex = vertices.get(index);
            double lonDeg = vertex.getPosition().x;
            if (lonDeg != minLonDeg) {
                lonDeg = minLonDeg;
                vertex.getPosition().x = lonDeg;
            }
        }

        int rightVerticesCount = quantizedMesh.getEastVertexCount();
        for (int i = 0; i < rightVerticesCount; i++) {
            int index = quantizedMesh.getEastIndices()[i];
            TerrainVertex vertex = vertices.get(index);
            double lonDeg = vertex.getPosition().x;
            if (lonDeg != maxLonDeg) {
                lonDeg = maxLonDeg;
                vertex.getPosition().x = lonDeg;
            }
        }

        int[] triangleIndices = quantizedMesh.getTriangleIndices();
        int triangleCount = quantizedMesh.getTriangleCount();
        for (int i = 0; i < triangleCount; i++) {
            int index1 = triangleIndices[i * 3];
            int index2 = triangleIndices[i * 3 + 1];
            int index3 = triangleIndices[i * 3 + 2];

            TerrainVertex vertex1 = vertices.get(index1);
            TerrainVertex vertex2 = vertices.get(index2);
            TerrainVertex vertex3 = vertices.get(index3);

            // for each triangle, create 3 half edges
            TerrainHalfEdge halfEdge1 = new TerrainHalfEdge();
            TerrainHalfEdge halfEdge2 = new TerrainHalfEdge();
            TerrainHalfEdge halfEdge3 = new TerrainHalfEdge();

            halfEdges.add(halfEdge1);
            halfEdges.add(halfEdge2);
            halfEdges.add(halfEdge3);

            halfEdge1.setStartVertex(vertex1);
            halfEdge2.setStartVertex(vertex2);
            halfEdge3.setStartVertex(vertex3);

            vertex1.setOutingHEdge(halfEdge1);
            vertex2.setOutingHEdge(halfEdge2);
            vertex3.setOutingHEdge(halfEdge3);

            TerrainHalfEdgeUtils.concatenate3HalfEdgesLoop(halfEdge1, halfEdge2, halfEdge3);

            TerrainTriangle triangle = new TerrainTriangle();
            triangle.setHalfEdge(halfEdge1);
            triangle.setOwnerTileIndices(tileIndices);

            halfEdge1.setTriangle(triangle);
            halfEdge2.setTriangle(triangle);
            halfEdge3.setTriangle(triangle);

            triangles.add(triangle);
        }

        mesh.setTwins();
        mesh.setStartVertexAllHEdges();
        mesh.determineHalfEdgesType();

        return resultTile;
    }

    public QuantizedMesh getQuantizedMeshFromTile(TileWgs84 tile, boolean calculateNormals) {
        // First get the quantized mesh header
        QuantizedMeshHeader header = new QuantizedMeshHeader();
        TerrainMesh mesh = tile.getMesh();

        if (mesh == null) {return null;}

        List<TerrainVertex> vertices = mesh.vertices;
        int vertexCount = vertices.size();
        if (vertexCount == 0) {return null;}

        mesh.setObjectsIdInList();

        // Calculate the minimum and maximum heights & bbox
        CelestialBody body = GlobalOptions.getInstance().getCelestialBody();
        GaiaBoundingBox bboxWC = new GaiaBoundingBox();
        double minimumHeight = Double.MAX_VALUE;
        double maximumHeight = -Double.MAX_VALUE;
        for (int i = 0; i < vertexCount; i++) {
            TerrainVertex vertex = vertices.get(i);
            double height = vertex.getPosition().z;
            if (height < minimumHeight) {minimumHeight = height;}
            if (height > maximumHeight) {maximumHeight = height;}

            // calculate the bbox in world coordinates
            double[] posWC = GlobeUtils.geographicToCartesian(vertex.getPosition().x, vertex.getPosition().y, height, body);
            bboxWC.addPoint(posWC[0], posWC[1], posWC[2]);
        }
        double midHeight = (minimumHeight + maximumHeight) / 2.0;

        // Calculate the center of the tile in Earth-centered Fixed coordinates
        GeographicExtension geographicExtension = tile.getGeographicExtension();
        double midLonDeg = geographicExtension.getMidLongitudeDeg();
        double midLatDeg = geographicExtension.getMidLatitudeDeg();

        double[] cartesianWC = GlobeUtils.geographicToCartesian(midLonDeg, midLatDeg, midHeight, body);

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
        if (heightRange == 0.0) {heightRange = 1.0;}

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

        CelestialBody body = GlobalOptions.getInstance().getCelestialBody();
        Vector3d centerCartographic = GlobeUtils.cartesianToGeographic(centerCartesian[0], centerCartesian[1], centerCartesian[2], body);
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
