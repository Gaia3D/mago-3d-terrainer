package com.gaia3d.terrain.tile.generation;

import com.gaia3d.terrain.structure.GeographicExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("default")
class AvailableTileLongitudeTest {

    private static GeographicExtension extension(double minLon, double maxLon) {
        GeographicExtension extension = new GeographicExtension();
        extension.setDegrees(minLon, -10.0, 0.0, maxLon, 10.0, 0.0);
        return extension;
    }

    private static void assertRange(GeographicExtension extension, double minLon, double maxLon) {
        assertEquals(minLon, extension.getMinLongitudeDeg(), 1.0e-9);
        assertEquals(maxLon, extension.getMaxLongitudeDeg(), 1.0e-9);
    }

    @Test
    void extensionInsideTheRangeIsKept() {
        List<GeographicExtension> result = AvailableTileAnalyzer.normalizeLongitudeRange(extension(-30.0, 40.0));
        assertEquals(1, result.size());
        assertRange(result.getFirst(), -30.0, 40.0);
    }

    @Test
    void extensionBeyond180IsShifted() {
        List<GeographicExtension> result = AvailableTileAnalyzer.normalizeLongitudeRange(extension(190.0, 250.0));
        assertEquals(1, result.size());
        assertRange(result.getFirst(), -170.0, -110.0);
    }

    @Test
    void globalExtensionIn0To360CoversTheWholeWorld() {
        List<GeographicExtension> result = AvailableTileAnalyzer.normalizeLongitudeRange(extension(0.0, 360.0));
        assertEquals(2, result.size());
        assertRange(result.get(0), 0.0, 180.0);
        assertRange(result.get(1), -180.0, 0.0);
    }

    @Test
    void extensionCrossingTheAntimeridianIsSplit() {
        List<GeographicExtension> result = AvailableTileAnalyzer.normalizeLongitudeRange(extension(170.0, 190.0));
        assertEquals(2, result.size());
        assertRange(result.get(0), 170.0, 180.0);
        assertRange(result.get(1), -180.0, -170.0);
    }
}
