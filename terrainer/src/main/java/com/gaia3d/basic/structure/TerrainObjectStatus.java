package com.gaia3d.basic.structure;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TerrainObjectStatus {
    UNKNOWN(-1),
    ACTIVE(0),
    DELETED(1);

    private final int value;

    public static TerrainObjectStatus fromValue(int value) {
        for (TerrainObjectStatus type : TerrainObjectStatus.values()) {
            if (type.value == value) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
