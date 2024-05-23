package com.gaia3d.util;

import javax.media.jai.Interpolation;

public class CustomInterpolation extends Interpolation {
    public CustomInterpolation() {
        super();
    }

    @Override
    public int interpolateH(int[] samples, int xfrac) {
        return 0;
    }

    @Override
    public int interpolateV(int[] samples, int yfrac) {
        // Custom vertical interpolation logic here
        return 0;
    }

    @Override
    public float interpolateH(float[] floats, float v) {
        return 0;
    }

    @Override
    public double interpolateH(double[] doubles, float v) {
        return 0;
    }
}