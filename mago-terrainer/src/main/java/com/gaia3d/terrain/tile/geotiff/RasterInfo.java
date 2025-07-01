package com.gaia3d.terrain.tile.geotiff;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.geotools.coverage.grid.GridCoverage2D;

@Getter
@Setter
@AllArgsConstructor
public class RasterInfo {
    private String name;
    private GridCoverage2D gridCoverage2D;
}
