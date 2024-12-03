package com.gaia3d.command;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Slf4j
public class TempTest {

    @Test
    void sampleKoreaTerrain() throws IOException {
        File inputPath = new File("E:\\(DEM) Sample", "korea");
        File outputPath = new File("D:\\data\\mago-server\\output", "south-korea-15");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "15",
                //"-d",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleKoreaCreateLayerJson() throws IOException {
        //File inputPath = new File("E:\\(DEM) Sample", "korea");
        File outputPath = new File("D:\\data\\mago-server\\output", "south-korea-15");

        String[] args = new String[]{
                "-input", outputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-json",
                "-calculateNormals",
                //"-d",
        };
        MagoTerrainerMain.main(args);
    }
}
