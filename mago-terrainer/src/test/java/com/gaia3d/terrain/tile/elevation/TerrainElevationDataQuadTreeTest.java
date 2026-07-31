package com.gaia3d.terrain.tile.elevation;

import com.gaia3d.terrain.structure.GeographicExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TerrainElevationDataQuadTreeTest {

    @Test
    @Tag("default")
    void retainsCrossBoundaryRasterAtParentWithoutDuplicates() {
        TerrainElevationDataQuadTree tree = new TerrainElevationDataQuadTree(null);
        TerrainElevationData west = elevationData(0.0, 0.0, 4.0, 4.0);
        TerrainElevationData east = elevationData(6.0, 0.0, 10.0, 4.0);
        TerrainElevationData crossBoundary = elevationData(4.0, 1.0, 6.0, 3.0);
        TerrainElevationData northeast = elevationData(6.0, 6.0, 10.0, 10.0);
        tree.addTerrainElevationData(west);
        tree.addTerrainElevationData(east);
        tree.addTerrainElevationData(crossBoundary);
        tree.addTerrainElevationData(northeast);

        tree.makeQuadTree(4);

        assertEquals(1, tree.getTerrainElevationDataList().size());
        assertSame(crossBoundary, tree.getTerrainElevationDataList().get(0));

        List<TerrainElevationData> result = new ArrayList<>();
        tree.getTerrainElevationDataArray(extension(3.0, 0.0, 7.0, 4.0), result, 1);
        assertEquals(3, result.size());
        assertEquals(3, result.stream().distinct().count());
    }

    @Test
    @Tag("default")
    void pointQueryTraversesOnlyContainingBranch() {
        TerrainElevationDataQuadTree tree = new TerrainElevationDataQuadTree(null);
        TerrainElevationData southwest = elevationData(0.0, 0.0, 4.0, 4.0);
        TerrainElevationData northeast = elevationData(6.0, 6.0, 10.0, 10.0);
        tree.addTerrainElevationData(southwest);
        tree.addTerrainElevationData(northeast);
        tree.makeQuadTree(4);

        List<TerrainElevationData> result = new ArrayList<>();
        tree.getTerrainElevationDataArray(2.0, 2.0, result);

        assertEquals(List.of(southwest), result);
    }

    private static TerrainElevationData elevationData(double minLon, double minLat, double maxLon, double maxLat) {
        TerrainElevationData data = new TerrainElevationData(null);
        data.setGeographicExtension(extension(minLon, minLat, maxLon, maxLat));
        return data;
    }

    private static GeographicExtension extension(double minLon, double minLat, double maxLon, double maxLat) {
        GeographicExtension extension = new GeographicExtension();
        extension.setDegrees(minLon, minLat, 0.0, maxLon, maxLat, 0.0);
        return extension;
    }
}
