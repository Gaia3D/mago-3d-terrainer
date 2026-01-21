package com.gaia3d.terrain.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * PriorityType
 * for tile priority
 */
@Getter
@RequiredArgsConstructor
public enum PriorityType {
    RESOLUTION("resolution"); // high resolution raster
    //HIGHER_VALUE("higher");

    private final String argumentName;

    public static PriorityType fromString(String text) {
        for (PriorityType type : PriorityType.values()) {
            if (type.argumentName.equalsIgnoreCase(text)) {
                return type;
            }
        }
        return RESOLUTION;
    }
}
