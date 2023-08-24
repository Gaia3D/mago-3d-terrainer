package com.gaia3d.basic.structure;

import org.joml.Vector3d;

public class GeographicExtension {
    Vector3d minGeographicCoordDeg = new Vector3d();
    Vector3d maxGeographicCoordDeg = new Vector3d();

    public void setDegrees(double minLonDeg, double minLatDeg, double minAlt, double maxLonDeg, double maxLatDeg, double maxAlt) {
        minGeographicCoordDeg.set(minLonDeg, minLatDeg, minAlt);
        maxGeographicCoordDeg.set(maxLonDeg, maxLatDeg, maxAlt);
    }

    public double getMaxLongitudeDeg()
    {
        return maxGeographicCoordDeg.x;
    }

    public double getMinLongitudeDeg()
    {
        return minGeographicCoordDeg.x;
    }

    public double getMaxLatitudeDeg()
    {
        return maxGeographicCoordDeg.y;
    }

    public double getMinLatitudeDeg()
    {
        return minGeographicCoordDeg.y;
    }

    public double getMaxAltitude()
    {
        return maxGeographicCoordDeg.z;
    }

    public double getMinAltitude()
    {
        return minGeographicCoordDeg.z;
    }

    public double getLongitudeRangeDegree()
    {
        return maxGeographicCoordDeg.x - minGeographicCoordDeg.x;
    }

    public double getLatitudeRangeDegree()
    {
        return maxGeographicCoordDeg.y - minGeographicCoordDeg.y;
    }

    public double getAltitudeRange()
    {
        return maxGeographicCoordDeg.z - minGeographicCoordDeg.z;
    }

    public boolean intersects(double lonDeg, double latDeg)
    {
        if(lonDeg >= minGeographicCoordDeg.x && lonDeg <= maxGeographicCoordDeg.x &&
                latDeg >= minGeographicCoordDeg.y && latDeg <= maxGeographicCoordDeg.y)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean intersectsLongitude(double lonDeg)
    {
        if(lonDeg >= minGeographicCoordDeg.x && lonDeg <= maxGeographicCoordDeg.x)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean intersectsLatitude(double latDeg)
    {
        if(latDeg >= minGeographicCoordDeg.y && latDeg <= maxGeographicCoordDeg.y)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
