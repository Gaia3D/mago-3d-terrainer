package com.gaia3d.command;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TilingSchema {
    // TMS
    GEODETIC("geodetic"),
    // Mercator
    MERCATOR("mercator");

    private final String argumentName;

    public static TilingSchema fromString(String text) {
        for (TilingSchema schema : TilingSchema.values()) {
            if (schema.argumentName.equalsIgnoreCase(text)) {
                return schema;
            }
        }
        return GEODETIC;
    }
}
