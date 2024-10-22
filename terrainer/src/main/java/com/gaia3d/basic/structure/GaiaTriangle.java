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
    private GaiaBoundingBox myBoundingBox = null;
    private GaiaPlane myPlane = null;
    private int splitDepth = 0;
    private boolean refineChecked = false;

    public void deleteObjects() {
        halfEdge = null;
        ownerTileIndices = null;
        myBoundingBox = null;
        myPlane = null;
    }

    public void setHalfEdge(GaiaHalfEdge halfEdge) {
        this.halfEdge = halfEdge;
        halfEdge.setTriangleToHEdgesLoop(this);
    }

    public List<GaiaVertex> getVertices(List<GaiaVertex> resultVertices, List<GaiaHalfEdge> listHalfEdgesMemSave) {
        listHalfEdgesMemSave.clear();
        this.halfEdge.getHalfEdgesLoop(listHalfEdgesMemSave);
        if (resultVertices == null) resultVertices = new ArrayList<>();
        for (GaiaHalfEdge halfEdge : listHalfEdgesMemSave) {
            resultVertices.add(halfEdge.getStartVertex());
        }
        listHalfEdgesMemSave.clear();
        return resultVertices;
    }

    public List<Vector3d> getPositions(List<GaiaVertex> listVerticesMemSave, List<GaiaHalfEdge> listHalfEdgesMemSave) {
        listHalfEdgesMemSave.clear();
        listVerticesMemSave = this.getVertices(listVerticesMemSave, listHalfEdgesMemSave);
        List<Vector3d> positions = new ArrayList<>();
        for (GaiaVertex vertex : listVerticesMemSave) {
            positions.add(vertex.getPosition());
        }
        return positions;
    }

    public GaiaBoundingBox getBoundingBox(List<GaiaVertex> listVerticesMemSave, List<GaiaHalfEdge> listHalfEdgesMemSave) {
        listHalfEdgesMemSave.clear();
        if (this.myBoundingBox == null) {
            this.myBoundingBox = new GaiaBoundingBox();
            listVerticesMemSave.clear();
            listVerticesMemSave = this.getVertices(listVerticesMemSave, listHalfEdgesMemSave);
            for (GaiaVertex vertex : listVerticesMemSave) {
                this.myBoundingBox.addPoint(vertex.getPosition());
            }
        }

        return this.myBoundingBox;
    }

    public GaiaPlane getPlane(List<GaiaVertex> listVerticesMemSave, List<GaiaHalfEdge> listHalfEdgesMemSave) {
        if (this.myPlane == null) {
            this.myPlane = new GaiaPlane();
            listVerticesMemSave.clear();
            listHalfEdgesMemSave.clear();
            listVerticesMemSave = this.getVertices(listVerticesMemSave, listHalfEdgesMemSave);
            GaiaVertex vertex0 = listVerticesMemSave.get(0);
            GaiaVertex vertex1 = listVerticesMemSave.get(1);
            GaiaVertex vertex2 = listVerticesMemSave.get(2);
            this.myPlane.set3Points(vertex0.getPosition(), vertex1.getPosition(), vertex2.getPosition());
        }

        return this.myPlane;
    }

    public boolean intersectsPointXY(double pos_x, double pos_y, List<GaiaHalfEdge> memSaveHedges, List<GaiaVertex> listVerticesMemSave, GaiaLine2D memSaveline2D) {
        listVerticesMemSave.clear();
        memSaveHedges.clear();
        GaiaBoundingBox boundingBox = this.getBoundingBox(listVerticesMemSave, memSaveHedges);
        if (!boundingBox.intersectsPointXY(pos_x, pos_y)) {
            return false;
        }

        memSaveHedges.clear();
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

    public Vector3d getBarycenter(List<GaiaVertex> listVerticesMemSave, List<GaiaHalfEdge> listHalfEdgesMemSave) {
        listVerticesMemSave.clear();
        listHalfEdgesMemSave.clear();
        listVerticesMemSave = this.getVertices(listVerticesMemSave, listHalfEdgesMemSave);
        Vector3d barycenter = new Vector3d();
        for (GaiaVertex vertex : listVerticesMemSave) {
            barycenter.add(vertex.getPosition());
        }
        barycenter.mul(1.0 / 3.0);
        return barycenter;
    }

    public List<Vector3d> getSomePointsToCheckForTriangleRefinement(List<GaiaVertex> listVerticesMemSave, List<GaiaHalfEdge> listHalfEdgesMemSave) {
        List<Vector3d> somePoints = new ArrayList<>();

        listHalfEdgesMemSave.clear();
        Vector3d barycenter = this.getBarycenter(listVerticesMemSave, listHalfEdgesMemSave);
        somePoints.add(barycenter);

        listHalfEdgesMemSave.clear();
        listVerticesMemSave = this.getVertices(listVerticesMemSave, listHalfEdgesMemSave);
        for (GaiaVertex vertex : listVerticesMemSave) {
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

    public GaiaHalfEdge getLongestHalfEdge(List<GaiaHalfEdge> listHalfEdgesMemSave) {
        // Note : the length of the halfEdges meaning only the length of the XY plane
        listHalfEdgesMemSave.clear();
        this.halfEdge.getHalfEdgesLoop(listHalfEdgesMemSave);
        GaiaHalfEdge longestHalfEdge = null;
        double maxLength = 0.0;
        for (GaiaHalfEdge halfEdge : listHalfEdgesMemSave) {
            double length = halfEdge.getSquaredLengthXY();
            if (length > maxLength) {
                maxLength = length;
                longestHalfEdge = halfEdge;
            }
        }
        listHalfEdgesMemSave.clear();

        return longestHalfEdge;
    }

    public double getTriangleMaxSizeInMeters(List<GaiaVertex> listVerticesMemSave, List<GaiaHalfEdge> listHalfEdgesMemSave) {
        listHalfEdgesMemSave.clear();
        GaiaBoundingBox bboxTriangle = this.getBoundingBox(listVerticesMemSave, listHalfEdgesMemSave);
        double triangleMaxLengthDeg = Math.max(bboxTriangle.getLengthX(), bboxTriangle.getLengthY());
        double triangleMaxLengthRad = Math.toRadians(triangleMaxLengthDeg);
        return triangleMaxLengthRad * GlobeUtils.EQUATORIAL_RADIUS;
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

    public void calculateNormal(List<GaiaVertex> listVerticesMemSave, List<GaiaHalfEdge> listHalfEdgesMemSave) {
        listHalfEdgesMemSave.clear();
        calculateNormalWC(listVerticesMemSave, listHalfEdgesMemSave);
    }

    public Vector3f getNormal() {
        if (this.normal == null) {
            List<GaiaVertex> listVerticesMemSave = new ArrayList<>();
            List<GaiaHalfEdge> listHalfEdgesMemSave = new ArrayList<>();
            return this.getNormal(listVerticesMemSave, listHalfEdgesMemSave);
        }
        return this.normal;
    }

    public Vector3f getNormal(List<GaiaVertex> listVerticesMemSave, List<GaiaHalfEdge> listHalfEdgesMemSave) {
        if (this.normal == null) {
            listHalfEdgesMemSave.clear();
            calculateNormalWC(listVerticesMemSave, listHalfEdgesMemSave);
        }
        return this.normal;
    }

    public void calculateNormalWC(List<GaiaVertex> listVerticesMemSave, List<GaiaHalfEdge> listHalfEdgesMemSave) {
        if (this.normal == null) {
            listHalfEdgesMemSave.clear();
            listVerticesMemSave = this.getVertices(listVerticesMemSave, listHalfEdgesMemSave);
            Vector3d p0 = listVerticesMemSave.get(0).getPosition();
            Vector3d p1 = listVerticesMemSave.get(1).getPosition();
            Vector3d p2 = listVerticesMemSave.get(2).getPosition();

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
