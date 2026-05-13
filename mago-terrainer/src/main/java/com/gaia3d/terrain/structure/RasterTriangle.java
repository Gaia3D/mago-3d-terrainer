package com.gaia3d.terrain.structure;

import lombok.Getter;
import org.joml.Vector2i;

@Getter
public class RasterTriangle {
    private final Vector2i p1; // column, row of the raster
    private final Vector2i p2; // column, row of the raster
    private final Vector2i p3; // column, row of the raster

    public RasterTriangle(Vector2i p1, Vector2i p2, Vector2i p3) {
        this.p1 = requireVertex(p1);
        this.p2 = requireVertex(p2);
        this.p3 = requireVertex(p3);
    }

    public RasterTriangle() {
        this(new Vector2i(), new Vector2i(), new Vector2i());
    }

    public void setVertices(Vector2i v0, Vector2i v1, Vector2i v2) {
        this.p1.set(v0);
        this.p2.set(v1);
        this.p3.set(v2);
    }

    public void setVertices(int col0, int row0, int col1, int row1, int col2, int row2) {
        this.p1.set(col0, row0);
        this.p2.set(col1, row1);
        this.p3.set(col2, row2);
    }

    private static Vector2i requireVertex(Vector2i value) {
        if (value == null) {
            throw new IllegalArgumentException("RasterTriangle vertices must not be null");
        }
        return value;
    }
}
