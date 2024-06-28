package com.gaia3d.basic.structure;

import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

@Getter
@Setter
public class GeographicExtension {
    private Vector3d minGeographicCoordDeg = new Vector3d();
    private Vector3d maxGeographicCoordDeg = new Vector3d();

    public void setDegrees(double minLonDeg, double minLatDeg, double minAlt, double maxLonDeg, double maxLatDeg, double maxAlt) {
        minGeographicCoordDeg.set(minLonDeg, minLatDeg, minAlt);
        maxGeographicCoordDeg.set(maxLonDeg, maxLatDeg, maxAlt);
    }

    public void copyFrom(GeographicExtension geographicExtension) {
        minGeographicCoordDeg.set(geographicExtension.minGeographicCoordDeg);
        maxGeographicCoordDeg.set(geographicExtension.maxGeographicCoordDeg);
    }

    public void deleteObjects() {
        minGeographicCoordDeg = null;
        maxGeographicCoordDeg = null;
    }

    public void union(GeographicExtension geographicExtension) {
        if (geographicExtension.minGeographicCoordDeg.x < minGeographicCoordDeg.x) {
            minGeographicCoordDeg.x = geographicExtension.minGeographicCoordDeg.x;
        }

        if (geographicExtension.minGeographicCoordDeg.y < minGeographicCoordDeg.y) {
            minGeographicCoordDeg.y = geographicExtension.minGeographicCoordDeg.y;
        }

        if (geographicExtension.minGeographicCoordDeg.z < minGeographicCoordDeg.z) {
            minGeographicCoordDeg.z = geographicExtension.minGeographicCoordDeg.z;
        }

        if (geographicExtension.maxGeographicCoordDeg.x > maxGeographicCoordDeg.x) {
            maxGeographicCoordDeg.x = geographicExtension.maxGeographicCoordDeg.x;
        }

        if (geographicExtension.maxGeographicCoordDeg.y > maxGeographicCoordDeg.y) {
            maxGeographicCoordDeg.y = geographicExtension.maxGeographicCoordDeg.y;
        }

        if (geographicExtension.maxGeographicCoordDeg.z > maxGeographicCoordDeg.z) {
            maxGeographicCoordDeg.z = geographicExtension.maxGeographicCoordDeg.z;
        }
    }

    public double getMaxLongitudeDeg() {
        return maxGeographicCoordDeg.x;
    }

    public double getMinLongitudeDeg() {
        return minGeographicCoordDeg.x;
    }

    public double getMaxLatitudeDeg() {
        return maxGeographicCoordDeg.y;
    }

    public double getMinLatitudeDeg() {
        return minGeographicCoordDeg.y;
    }

    public double getMidLongitudeDeg() {
        return (maxGeographicCoordDeg.x + minGeographicCoordDeg.x) / 2.0;
    }

    public double getMidLatitudeDeg() {
        return (maxGeographicCoordDeg.y + minGeographicCoordDeg.y) / 2.0;
    }

    public double getMidAltitude() {
        return (maxGeographicCoordDeg.z + minGeographicCoordDeg.z) / 2.0;
    }

    public double getMaxAltitude() {
        return maxGeographicCoordDeg.z;
    }

    public double getMinAltitude() {
        return minGeographicCoordDeg.z;
    }

    public double getLongitudeRangeDegree() {
        return maxGeographicCoordDeg.x - minGeographicCoordDeg.x;
    }

    public double getLatitudeRangeDegree() {
        return maxGeographicCoordDeg.y - minGeographicCoordDeg.y;
    }

    public double getAltitudeRange() {
        return maxGeographicCoordDeg.z - minGeographicCoordDeg.z;
    }

    public Vector3d getMidPoint() {
        return new Vector3d((maxGeographicCoordDeg.x + minGeographicCoordDeg.x) / 2.0, (maxGeographicCoordDeg.y + minGeographicCoordDeg.y) / 2.0, (maxGeographicCoordDeg.z + minGeographicCoordDeg.z) / 2.0);
    }

    public boolean intersects(double lonDeg, double latDeg) {
        return lonDeg >= minGeographicCoordDeg.x && lonDeg <= maxGeographicCoordDeg.x && latDeg >= minGeographicCoordDeg.y && latDeg <= maxGeographicCoordDeg.y;
    }

    public boolean intersects(GeographicExtension geographicExtension) {
        if (geographicExtension.minGeographicCoordDeg.x > this.maxGeographicCoordDeg.x) {
            return false;
        } else if (geographicExtension.maxGeographicCoordDeg.x < this.minGeographicCoordDeg.x) {
            return false;
        } else if (geographicExtension.minGeographicCoordDeg.y > this.maxGeographicCoordDeg.y) {
            return false;
        } else return !(geographicExtension.maxGeographicCoordDeg.y < this.minGeographicCoordDeg.y);

    }

    public boolean intersectsBox(double minLonDeg, double minLatDeg, double maxLonDeg, double maxLatDeg) {
        return minLonDeg >= minGeographicCoordDeg.x && minLonDeg <= maxGeographicCoordDeg.x && minLatDeg >= minGeographicCoordDeg.y && minLatDeg <= maxGeographicCoordDeg.y && maxLonDeg >= minGeographicCoordDeg.x && maxLonDeg <= maxGeographicCoordDeg.x && maxLatDeg >= minGeographicCoordDeg.y && maxLatDeg <= maxGeographicCoordDeg.y;
    }

    public boolean intersectsLongitude(double lonDeg) {
        return lonDeg >= minGeographicCoordDeg.x && lonDeg <= maxGeographicCoordDeg.x;
    }

    public boolean intersectsLatitude(double latDeg) {
        return latDeg >= minGeographicCoordDeg.y && latDeg <= maxGeographicCoordDeg.y;
    }
}
