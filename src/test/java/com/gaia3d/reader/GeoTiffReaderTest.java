package com.gaia3d.reader;

import org.junit.jupiter.api.Test;

class GeoTiffReaderTest {
    @Test
    void read() {
        GaiaGeoTiffReader reader = new GaiaGeoTiffReader();
        String path = "D:\\QuantizedMesh_JavaProjects\\ws2_merged_dem.tif";
        reader.read(path);

        int hola = 0;
    }
}