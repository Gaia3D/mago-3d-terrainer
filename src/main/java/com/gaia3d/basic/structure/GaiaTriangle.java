package com.gaia3d.basic.structure;

import com.gaia3d.util.GlobeUtils;
import com.gaia3d.util.io.BigEndianDataInputStream;
import com.gaia3d.util.io.BigEndianDataOutputStream;
import com.gaia3d.wgs84Tiles.TileIndices;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Slf4j
public class GaiaTriangle {
    public GaiaHalfEdge halfEdge = null; // The half edge structure caused a stack overflow, so we applied a public access controller.

    private int id = -1;
    private int halfEdgeId = -1;
    private Vector3f normal = null;
    private TileIndices ownerTileIndices = new TileIndices(); // this triangle belongs to a tile
    private GaiaObjectStatus objectStatus = GaiaObjectStatus.ACTIVE;
    private GaiaBoundingBox boundingBox = null;
    private GaiaPlane plane = null;
    private int splitDepth = 0;
    private boolean refineChecked = false;

    public void deleteObjects() {
        halfEdge = null;
        ownerTileIndices = null;
        boundingBox = null;
        plane = null;
    }

    public void setHalfEdge(GaiaHalfEdge halfEdge) {
        this.halfEdge = halfEdge;
        halfEdge.setTriangleToHEdgesLoop(this);
    }

    public List<GaiaVertex> getVertices() {
        List<GaiaHalfEdge> halfEdges = new ArrayList<>();
        this.halfEdge.getHalfEdgesLoop(halfEdges);
        List<GaiaVertex> vertices = new ArrayList<>();
        for (GaiaHalfEdge halfEdge : halfEdges) {
            vertices.add(halfEdge.getStartVertex());
        }
        halfEdges.clear();
        return vertices;
    }

    public List<Vector3d> getPositions() {
        List<GaiaVertex> vertices = this.getVertices();
        List<Vector3d> positions = new ArrayList<>();
        for (GaiaVertex vertex : vertices) {
            positions.add(vertex.getPosition());
        }
        return positions;
    }

    public GaiaBoundingBox getBoundingBox() {
        if (this.boundingBox == null) {
            this.boundingBox = new GaiaBoundingBox();
            List<GaiaVertex> vertices = this.getVertices();
            for (GaiaVertex vertex : vertices) {
                this.boundingBox.addPoint(vertex.getPosition());
            }
        }

        return this.boundingBox;
    }

    public GaiaPlane getPlane() {
        if (this.plane == null) {
            this.plane = new GaiaPlane();
            List<GaiaVertex> vertices = this.getVertices();
            GaiaVertex vertex0 = vertices.get(0);
            GaiaVertex vertex1 = vertices.get(1);
            GaiaVertex vertex2 = vertices.get(2);
            this.plane.set3Points(vertex0.getPosition(), vertex1.getPosition(), vertex2.getPosition());
        }

        return this.plane;
    }

    public boolean intersectsPointXY(double pos_x, double pos_y, List<GaiaHalfEdge> memSaveHedges, GaiaLine2D memSaveline2D) {
        GaiaBoundingBox boundingBox = this.getBoundingBox();
        if (!boundingBox.intersectsPointXY(pos_x, pos_y)) {
            return false;
        }

        this.halfEdge.getHalfEdgesLoop(memSaveHedges);
        double error = 1e-8;
        int hedgesCount = memSaveHedges.size();
        GaiaLine2D line2dAux = null;
        for (int i = 0; i < hedgesCount; i++) {
            GaiaHalfEdge hedge = memSaveHedges.get(i);
            line2dAux = hedge.getLine2DXY();
            byte relativePosition2D_linePoint = line2dAux.relativePositionOfPoint(pos_x, pos_y, error);

            // relative positions :
            // 0 : point is on the line.
            // 1 : point is on the left side of the line.
            // 2 : point is on the right side of the line.
            if (relativePosition2D_linePoint == 2) {
                memSaveHedges.clear();
                return false;
            }
        }

        memSaveHedges.clear();
        return true;
    }

    public Vector3d getBarycenter() {
        List<GaiaVertex> vertices = this.getVertices();
        Vector3d barycenter = new Vector3d();
        for (GaiaVertex vertex : vertices) {
            barycenter.add(vertex.getPosition());
        }
        barycenter.mul(1.0 / 3.0);
        return barycenter;
    }

    public List<Vector3d> getSomePointsToCheckForTriangleRefinement() {
        List<Vector3d> somePoints = new ArrayList<>();

        Vector3d barycenter = this.getBarycenter();
        somePoints.add(barycenter);

        List<GaiaVertex> vertices = this.getVertices();
        for (GaiaVertex vertex : vertices) {
            Vector3d pos = vertex.getPosition().add(barycenter).mul(0.5);
            somePoints.add(pos);
        }

        return somePoints;
    }

    public List<Vector3d> getPerimeterPositions(int numInterpolation) {
        List<Vector3d> perimeterPositions = new ArrayList<>();
        List<GaiaHalfEdge> halfEdges = new ArrayList<>();
        this.halfEdge.getHalfEdgesLoop(halfEdges);
        for (GaiaHalfEdge halfEdge : halfEdges) {
            halfEdge.getInterpolatedPositions(perimeterPositions, numInterpolation);
        }
        halfEdges.clear();
        return perimeterPositions;
    }

    public GaiaHalfEdge getLongestHalfEdge() {
        // Note : the length of the halfEdges meaning only the length of the XY plane
        List<GaiaHalfEdge> halfEdges = new ArrayList<>();
        this.halfEdge.getHalfEdgesLoop(halfEdges);
        GaiaHalfEdge longestHalfEdge = null;
        double maxLength = 0.0;
        for (GaiaHalfEdge halfEdge : halfEdges) {
            double length = halfEdge.getSquaredLengthXY();
            if (length > maxLength) {
                maxLength = length;
                longestHalfEdge = halfEdge;
            }
        }
        halfEdges.clear();

        return longestHalfEdge;
    }

    public double getTriangleMaxSizeInMeters() {
        GaiaBoundingBox bboxTriangle = this.getBoundingBox();
        double triangleMaxLegthDeg = Math.max(bboxTriangle.getLengthX(), bboxTriangle.getLengthY());
        double triangleMaxLegthRad = Math.toRadians(triangleMaxLegthDeg);
        return triangleMaxLegthRad * GlobeUtils.EQUATORIAL_RADIUS;
    }

    public void saveDataOutputStream(BigEndianDataOutputStream dataOutputStream) {
        try {
            // 1rst, save id
            dataOutputStream.writeInt(id);

            // 2nd, save halfEdge
            if (halfEdge != null) {
                dataOutputStream.writeInt(halfEdge.getId());
            } else {
                dataOutputStream.writeInt(-1);
            }

            // 3rd, save ownerTile_tileIndices
            if (ownerTileIndices == null) {
                dataOutputStream.writeInt(-1);
                dataOutputStream.writeInt(-1);
                dataOutputStream.writeInt(-1);
            } else {
                ownerTileIndices.saveDataOutputStream(dataOutputStream);
            }

            // save splitDepth
            dataOutputStream.writeInt(splitDepth);

        } catch (Exception e) {
            log.error("{}", e.getMessage());
        }
    }

    public void loadDataInputStream(BigEndianDataInputStream dataInputStream) throws IOException {
        this.id = dataInputStream.readInt();
        this.halfEdgeId = dataInputStream.readInt();
        if (this.ownerTileIndices == null) {
            this.ownerTileIndices = new TileIndices();
        }
        this.ownerTileIndices.loadDataInputStream(dataInputStream);

        this.splitDepth = dataInputStream.readInt();
    }

    public void calculateNormal() {
        calculateNormalWC();
    }

    public Vector3f getNormal() {
        if (this.normal == null) {
            calculateNormalWC();
        }
        return this.normal;
    }

    public void calculateNormalWC() {
        if (this.normal == null) {
            List<GaiaVertex> vertices = this.getVertices();
            Vector3d p0 = vertices.get(0).getPosition();
            Vector3d p1 = vertices.get(1).getPosition();
            Vector3d p2 = vertices.get(2).getPosition();

            Vector3d p0WC = GlobeUtils.geographicToCartesianWgs84(p0);
            Vector3d p1WC = GlobeUtils.geographicToCartesianWgs84(p1);
            Vector3d p2WC = GlobeUtils.geographicToCartesianWgs84(p2);

            Vector3d v1 = new Vector3d(p1WC).sub(p0WC);
            Vector3d v2 = new Vector3d(p2WC).sub(p0WC);
            Vector3d normalized = new Vector3d(v1).cross(v2).normalize();
            this.normal = new Vector3f((float) normalized.x, (float) normalized.y, (float) normalized.z);
        }
    }
}
