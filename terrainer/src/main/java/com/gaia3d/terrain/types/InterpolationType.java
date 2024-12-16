package com.gaia3d.terrain.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.media.jai.Interpolation;

@Getter
@RequiredArgsConstructor
public enum InterpolationType {

    NEAREST(Interpolation.INTERP_NEAREST, "nearest"),
    BILINEAR(Interpolation.INTERP_BILINEAR, "bilinear");
    //BICUBIC(Interpolation.INTERP_BICUBIC, "bicubic");

    private final int interpolation;
    private final String interpolationArgument;

    public static InterpolationType fromString(String interpolationArgument) {
        for (InterpolationType interpolationType : values()) {
            if (interpolationType.interpolationArgument.equals(interpolationArgument)) {
                return interpolationType;
            }
        }
        throw new IllegalArgumentException("Unknown interpolation type: " + interpolationArgument);
    }
}
