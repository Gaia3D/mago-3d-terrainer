package com.gaia3d.reader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoTiffReaderTest {
    @Test
    void read() {
        GeoTiffReader reader = new GeoTiffReader();
        String path = "D:\\QuantizedMesh_JavaProjects\\ws2_merged_dem.tif";
        reader.read(path);

        int hola = 0;
    }
}