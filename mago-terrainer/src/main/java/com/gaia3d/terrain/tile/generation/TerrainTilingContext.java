package com.gaia3d.terrain.tile.generation;

import com.gaia3d.command.GlobalOptions;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TerrainTilingContext {
    private final int rasterTileSize = 256;
    private final String imageryType = "CRS84";
    private boolean originIsLeftUp = false;

    public boolean originIsLeftUp() {
        return originIsLeftUp;
    }
}
