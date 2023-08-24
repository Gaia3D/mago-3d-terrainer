package com.gaia3d.basic.structure;

import org.joml.Vector2d;

import static java.lang.Math.abs;

public class GaiaLine2D {
    Vector2d position = new Vector2d();
    Vector2d direction = new Vector2d();

    public GaiaLine2D() {
    }

    public void setBy2Points(Vector2d point0, Vector2d point1)
    {
        this.position.set(point0);
        this.direction.set(point1).sub(point0).normalize();
    }

    public RelativePosition2D_LinePoint relativePositionOfPoint(Vector2d p, double error)
    {
        // relative positions :
        // 0 : point is on the line.
        // 1 : point is on the left side of the line.
        // 2 : point is on the right side of the line.
        double x = p.x - this.position.x;
        double y = p.y - this.position.y;
        Vector2d v = new Vector2d(x, y).normalize();

        double cross = this.direction.x * v.y - this.direction.y * v.x;
        if(abs(cross) < error)
        {
            return RelativePosition2D_LinePoint.ON_THE_LINE;
        }
        else if(cross > 0)
        {
            return RelativePosition2D_LinePoint.LEFT_SIDE_OF_THE_LINE;
        }
        else
        {
            return RelativePosition2D_LinePoint.RIGHT_SIDE_OF_THE_LINE;
        }
    }
}
