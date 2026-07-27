package com.gaia3d.terrain.tile.raster;

import lombok.NoArgsConstructor;

import java.nio.ByteOrder;

@NoArgsConstructor
public final class TerrainRasterFormat {
    public static final String EXTENSION = ".mtrf";
    public static final String CRS = "EPSG:4326";
    public static final byte[] MAGIC = new byte[]{'M', 'T', 'R', 'F'};
    public static final int VERSION = 1;
    public static final int HEADER_SIZE_BYTES = 64;
    public static final int FLOAT_BYTES = Float.BYTES;
    public static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

}
