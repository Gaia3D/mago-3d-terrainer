package com.gaia3d.terrain.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * PriorityType
 * The order of the overlapping rasters. The first raster with data at a point gives its height.
 */
@Getter
@RequiredArgsConstructor
public enum PriorityType {
    RESOLUTION("resolution"), // the raster with the highest resolution first
    INPUT("input"), // the order of the input options, then the resolution
    FILE("file"); // the order of the priority file, then the resolution

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
