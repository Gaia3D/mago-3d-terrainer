package com.gaia3d.release.others;

import com.gaia3d.util.CelestialBody;
import com.gaia3d.util.GlobeUtils;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoonSupportTest {

    @Test
    void testMoonTileSizeSmaller() {
        // Moon tiles should be ~27% of Earth tiles at the same depth
        double earthAngle = 180.0 / Math.pow(2, 14);
        double earthTileSize = Math.toRadians(earthAngle) * CelestialBody.EARTH.getEquatorialRadius();
        double moonTileSize = Math.toRadians(earthAngle) * CelestialBody.MOON.getEquatorialRadius();

        assertTrue(moonTileSize < earthTileSize);
        double ratio = moonTileSize / earthTileSize;
        assertEquals(0.272, ratio, 0.01); // Moon is ~27.2% of Earth radius
    }

    @Test
    void testMoonGeographicToCartesianConsistency() {
        // Test multiple points on the Moon
        double[][] testPoints = {
            {0, 0, 0},
            {90, 0, 0},
            {0, 45, 0},
            {-120, -30, 1000},
            {180, 0, 0}
        };

        for (double[] point : testPoints) {
            double lon = point[0], lat = point[1], alt = point[2];
            double[] cart = GlobeUtils.geographicToCartesian(lon, lat, alt, CelestialBody.MOON);
            Vector3d geo = GlobeUtils.cartesianToGeographic(cart[0], cart[1], cart[2], CelestialBody.MOON);

            assertEquals(lon, geo.x, 1e-6, "Longitude mismatch for point (" + lon + ", " + lat + ", " + alt + ")");
            assertEquals(lat, geo.y, 1e-6, "Latitude mismatch for point (" + lon + ", " + lat + ", " + alt + ")");
            assertEquals(alt, geo.z, 0.1, "Altitude mismatch for point (" + lon + ", " + lat + ", " + alt + ")");
        }
    }

    @Test
    void testCelestialBodyFromStringCaseInsensitive() {
        assertEquals(CelestialBody.MOON, CelestialBody.fromString("moon"));
        assertEquals(CelestialBody.MOON, CelestialBody.fromString("Moon"));
        assertEquals(CelestialBody.MOON, CelestialBody.fromString("MOON"));
        assertEquals(CelestialBody.MOON, CelestialBody.fromString("  moon  "));
    }

    @Test
    void testMoonDistanceCalculation() {
        // On the Moon, distance between two latitudes should be smaller than on Earth
        double latRad1 = Math.toRadians(0);
        double latRad2 = Math.toRadians(10);

        double earthDist = GlobeUtils.distanceBetweenLatitudesRad(latRad1, latRad2, CelestialBody.EARTH);
        double moonDist = GlobeUtils.distanceBetweenLatitudesRad(latRad1, latRad2, CelestialBody.MOON);

        assertTrue(moonDist < earthDist);
        double ratio = moonDist / earthDist;
        assertEquals(0.272, ratio, 0.01);
    }
}
