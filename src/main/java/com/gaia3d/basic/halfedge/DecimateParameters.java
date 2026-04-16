package com.gaia3d.basic.halfedge;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class DecimateParameters {
    private double maxDiffAngDegrees = 15.0;
    private double hedgeMinLength = 0.5;
    private double frontierMaxDiffAngDeg = 4.0;
    private double maxAspectRatio = 6.0;
    private int maxCollapsesCount = 1000000;
    private int iterationsCount = 1;
    private double smallHedgeSize = 1.0;
    private int lod = -1;
    private WeldingParameters weldingParameters = new WeldingParameters();

    // small triangle parameters
    private double smallTriangleMinArea = 1.0;
    private double smallTrianglesMinSize = 0.5;

    public void setBasicValues(double maxDiffAngDegrees, double hedgeMinLength, double frontierMaxDiffAngDeg, double maxAspectRatio, int maxCollapsesCount, int iterationsCount, double smallHedgeSize) {
        this.maxDiffAngDegrees = maxDiffAngDegrees;
        this.hedgeMinLength = hedgeMinLength;
        this.frontierMaxDiffAngDeg = frontierMaxDiffAngDeg;
        this.maxAspectRatio = maxAspectRatio;
        this.maxCollapsesCount = maxCollapsesCount;
        this.iterationsCount = iterationsCount;
        this.smallHedgeSize = smallHedgeSize;
    }

    public DecimateParameters clone() {
        DecimateParameters clone = new DecimateParameters();
        clone.setMaxDiffAngDegrees(this.maxDiffAngDegrees);
        clone.setHedgeMinLength(this.hedgeMinLength);
        clone.setFrontierMaxDiffAngDeg(this.frontierMaxDiffAngDeg);
        clone.setMaxAspectRatio(this.maxAspectRatio);
        clone.setMaxCollapsesCount(this.maxCollapsesCount);
        clone.setIterationsCount(this.iterationsCount);
        clone.setSmallHedgeSize(this.smallHedgeSize);
        clone.setLod(this.lod);

        if (this.weldingParameters != null) {
            clone.setWeldingParameters(this.weldingParameters.clone());
        }
        return clone;
    }


}
