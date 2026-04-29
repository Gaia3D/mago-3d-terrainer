package com.gaia3d.terrain.tile.writer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TerrainWriteRequest {
    private final int z;
    private final long x;
    private final long y;
    private final byte[] data;
}
