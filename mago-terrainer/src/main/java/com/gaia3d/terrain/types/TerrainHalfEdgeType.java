package com.gaia3d.terrain.types;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TerrainHalfEdgeType {

    //           UP
    //      +----------+
    //      |          |
    // LEFT |          | RIGHT
    //      |          |
    //      +----------+
    //           DOWN

    UNKNOWN(-1),
    LEFT(0),
    RIGHT(1),
    UP(2),
    DOWN(3),
    INTERIOR(4);

    private final int value;

    public static TerrainHalfEdgeType fromValue(int value) {
        return switch (value) {
            case 0 -> LEFT;
            case 1 -> RIGHT;
            case 2 -> UP;
            case 3 -> DOWN;
            case 4 -> INTERIOR;
            default -> UNKNOWN;
        };
    }
}
