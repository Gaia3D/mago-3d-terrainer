package com.gaia3d.terrain.structure;

import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2i;

@Getter
@Setter
public class RasterTriangle {
    Vector2i p1; // column, row of the raster
    Vector2i p2; // column, row of the raster
    Vector2i p3; // column, row of the raster

    public RasterTriangle(Vector2i p1, Vector2i p2, Vector2i p3) {
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
    }

    public RasterTriangle() {

    }

    public void setVertices(Vector2i v0, Vector2i v1, Vector2i v2) {
        this.p1 = v0;
        this.p2 = v1;
        this.p3 = v2;
    }
}
