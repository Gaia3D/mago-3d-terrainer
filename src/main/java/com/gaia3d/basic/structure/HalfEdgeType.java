package com.gaia3d.basic.structure;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum HalfEdgeType {

    //             UP
    //        +----------+
    //        |          |
    // LEFT   |          | RIGHT
    //        |          |
    //        +----------+
    //             DOWN


    UNKNOWN(-1),
    LEFT(0),
    RIGHT(1),
    UP(2),
    DOWN(3),
    INTERIOR(4);

    private int value;

    public static HalfEdgeType fromValue(int value) {
        for (HalfEdgeType type : HalfEdgeType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
