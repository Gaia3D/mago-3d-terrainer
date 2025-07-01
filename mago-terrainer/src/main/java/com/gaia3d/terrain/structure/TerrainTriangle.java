package com.gaia3d.terrain.structure;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.io.BigEndianDataInputStream;
import com.gaia3d.io.BigEndianDataOutputStream;
import com.gaia3d.terrain.tile.TileIndices;
import com.gaia3d.terrain.types.TerrainObjectStatus;
import com.gaia3d.util.GlobeUtils;
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
public class TerrainTriangle {

    private int id = -1;
    private int halfEdgeId = -1;
    private Vector3f normal = null;
    private TileIndices ownerTileIndices = new TileIndices(); // this triangle belongs to a tile
    private TerrainObjectStatus objectStatus = TerrainObjectStatus.ACTIVE;
    private GaiaBoundingBox myBoundingBox = null;
    private TerrainPlane myPlane = null;
    public TerrainHalfEdge halfEdge = null; // The half edge structure caused a stack overflow, so we applied a public access controller.
    private int splitDepth = 0;
    private boolean refineChecked = false;

    public void deleteObjects() {
        halfEdge = null;
        ownerTileIndices = null;
        myBoundingBox = null;
        myPlane = null;
    }

    public void setHalfEdge(TerrainHalfEdge halfEdge) {
        this.halfEdge = halfEdge;
        halfEdge.setTriangleToHEdgesLoop(this);
    }

    public List<TerrainVertex> getVertices(List<TerrainVertex> resultVertices, List<TerrainHalfEdge> listHalfEdges) {
        listHalfEdges.clear();
        this.halfEdge.getHalfEdgesLoop(listHalfEdges);
        if (resultVertices == null) resultVertices = new ArrayList<>();
        for (TerrainHalfEdge halfEdge : listHalfEdges) {
            resultVertices.add(halfEdge.getStartVertex());
        }
        listHalfEdges.clear();
        return resultVertices;
    }

    public List<Vector3d> getPositions(List<TerrainVertex> listVertices, List<TerrainHalfEdge> listHalfEdges) {
        listHalfEdges.clear();
        listVertices = this.getVertices(listVertices, listHalfEdges);
        List<Vector3d> positions = new ArrayList<>();
        for (TerrainVertex vertex : listVertices) {
            positions.add(vertex.getPosition());
        }
        return positions;
    }

    public GaiaBoundingBox getBoundingBox(List<TerrainVertex> listVertices, List<TerrainHalfEdge> listHalfEdges) {
        listHalfEdges.clear();
        if (this.myBoundingBox == null) {
            this.myBoundingBox = new GaiaBoundingBox();
            listVertices.clear();
            listVertices = this.getVertices(listVertices, listHalfEdges);
            for (TerrainVertex vertex : listVertices) {
                this.myBoundingBox.addPoint(vertex.getPosition());
            }
        }

        return this.myBoundingBox;
    }

    public TerrainPlane getPlane(List<TerrainVertex> listVertices, List<TerrainHalfEdge> listHalfEdges) {
        if (this.myPlane == null) {
            this.myPlane = new TerrainPlane();
            listVertices.clear();
            listHalfEdges.clear();
            listVertices = this.getVertices(listVertices, listHalfEdges);
            TerrainVertex vertex0 = listVertices.get(0);
            TerrainVertex vertex1 = listVertices.get(1);
            TerrainVertex vertex2 = listVertices.get(2);
            this.myPlane.set3Points(vertex0.getPosition(), vertex1.getPosition(), vertex2.getPosition());
        }

        return this.myPlane;
    }

    public boolean intersectsPointXY(double posX, double posY, List<TerrainHalfEdge> halfEdges, List<TerrainVertex> listVertices, TerrainLine2D line2D) {
        listVertices.clear();
        halfEdges.clear();
        GaiaBoundingBox boundingBox = this.getBoundingBox(listVertices, halfEdges);
        if (!boundingBox.intersectsPointXY(posX, posY)) {
            return false;
        }

        halfEdges.clear();
        this.halfEdge.getHalfEdgesLoop(halfEdges);
        double error = 1e-8;
        int hedgesCount = halfEdges.size();
        TerrainLine2D line2dAux = null;
        for (int i = 0; i < hedgesCount; i++) {
            TerrainHalfEdge hedge = halfEdges.get(i);
            line2dAux = hedge.getLine2DXY();
            byte relativePosition2D_linePoint = line2dAux.relativePositionOfPoint(posX, posY, error);

            // relative positions :
            // 0 : point is on the line.
            // 1 : point is on the left side of the line.
            // 2 : point is on the right side of the line.
            if (relativePosition2D_linePoint == 2) {
                halfEdges.clear();
                return false;
            }
        }

        halfEdges.clear();
        return true;
    }

    public Vector3d getBarycenter(List<TerrainVertex> listVertices, List<TerrainHalfEdge> listHalfEdges) {
        listVertices.clear();
        listHalfEdges.clear();
        listVertices = this.getVertices(listVertices, listHalfEdges);
        Vector3d barycenter = new Vector3d();
        for (TerrainVertex vertex : listVertices) {
            barycenter.add(vertex.getPosition());
        }
        barycenter.mul(1.0 / 3.0);
        return barycenter;
    }

    public List<Vector3d> getSomePointsToCheckForTriangleRefinement(List<TerrainVertex> listVertices, List<TerrainHalfEdge> listHalfEdges) {
        List<Vector3d> somePoints = new ArrayList<>();

        listHalfEdges.clear();
        Vector3d barycenter = this.getBarycenter(listVertices, listHalfEdges);
        somePoints.add(barycenter);

        listHalfEdges.clear();
        listVertices = this.getVertices(listVertices, listHalfEdges);
        for (TerrainVertex vertex : listVertices) {
            Vector3d pos = vertex.getPosition().add(barycenter).mul(0.5);
            somePoints.add(pos);
        }

        return somePoints;
    }

    public List<Vector3d> getPerimeterPositions(int numInterpolation) {
        List<Vector3d> perimeterPositions = new ArrayList<>();
        List<TerrainHalfEdge> halfEdges = new ArrayList<>();
        this.halfEdge.getHalfEdgesLoop(halfEdges);
        for (TerrainHalfEdge halfEdge : halfEdges) {
            halfEdge.getInterpolatedPositions(perimeterPositions, numInterpolation);
        }
        halfEdges.clear();
        return perimeterPositions;
    }

    public TerrainHalfEdge getLongestHalfEdge(List<TerrainHalfEdge> listHalfEdges) {
        // Note : the length of the halfEdges meaning only the length of the XY plane
        listHalfEdges.clear();
        this.halfEdge.getHalfEdgesLoop(listHalfEdges);
        TerrainHalfEdge longestHalfEdge = null;
        double maxLength = 0.0;
        for (TerrainHalfEdge halfEdge : listHalfEdges) {
            double length = halfEdge.getSquaredLengthXY();
            if (length > maxLength) {
                maxLength = length;
                longestHalfEdge = halfEdge;
            }
        }
        listHalfEdges.clear();

        return longestHalfEdge;
    }

    public double getTriangleMaxSizeInMeters(List<TerrainVertex> listVertices, List<TerrainHalfEdge> listHalfEdges) {
        listHalfEdges.clear();
        GaiaBoundingBox bboxTriangle = this.getBoundingBox(listVertices, listHalfEdges);
        double triangleMaxLengthDeg = Math.max(bboxTriangle.getLengthX(), bboxTriangle.getLengthY());
        double triangleMaxLengthRad = Math.toRadians(triangleMaxLengthDeg);
        return triangleMaxLengthRad * GlobeUtils.EQUATORIAL_RADIUS;
    }

    public void saveDataOutputStream(BigEndianDataOutputStream dataOutputStream) {
        try {
            // First, save id
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
            log.error("Error:", e);
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

    public void calculateNormal(List<TerrainVertex> listVertices, List<TerrainHalfEdge> listHalfEdges) {
        listHalfEdges.clear();
        calculateNormalWC(listVertices, listHalfEdges);
    }

    public Vector3f getNormal() {
        if (this.normal == null) {
            List<TerrainVertex> listVertices = new ArrayList<>();
            List<TerrainHalfEdge> listHalfEdges = new ArrayList<>();
            return this.getNormal(listVertices, listHalfEdges);
        }
        return this.normal;
    }

    public Vector3f getNormal(List<TerrainVertex> listVertices, List<TerrainHalfEdge> listHalfEdges) {
        if (this.normal == null) {
            listHalfEdges.clear();
            calculateNormalWC(listVertices, listHalfEdges);
        }
        return this.normal;
    }

    public void calculateNormalWC(List<TerrainVertex> listVertices, List<TerrainHalfEdge> listHalfEdges) {
        if (this.normal == null) {
            listHalfEdges.clear();
            listVertices = this.getVertices(listVertices, listHalfEdges);
            Vector3d p0 = listVertices.get(0).getPosition();
            Vector3d p1 = listVertices.get(1).getPosition();
            Vector3d p2 = listVertices.get(2).getPosition();

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
