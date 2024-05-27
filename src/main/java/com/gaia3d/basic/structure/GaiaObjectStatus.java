package com.gaia3d.basic.structure;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum GaiaObjectStatus {
    UNKNOWN(-1), ACTIVE(0), DELETED(1);

    private final int value;

    public static GaiaObjectStatus fromValue(int value) {
        for (GaiaObjectStatus type : GaiaObjectStatus.values()) {
            if (type.value == value) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
