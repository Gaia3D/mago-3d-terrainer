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
    void sampleKoreaTerrainLevel10() throws IOException {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "korea");
        File outputPath = new File("D:\\data\\mago-server\\output", "south-korea-10");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "10",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleKoreaTerrainLevel12() throws IOException {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "korea");
        File outputPath = new File("D:\\data\\mago-server\\output", "south-korea-12");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "12",
                //"-d",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleKoreaTerrainLevel14() throws IOException {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "korea");
        File outputPath = new File("D:\\data\\mago-server\\output", "south-korea-14");

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
    void sampleSeoulTerrainLevel15() throws IOException {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "seoul");
        File outputPath = new File("D:\\data\\mago-server\\output", "seoul-korea-15");

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
    void sampleSeoulTerrainLevel17() throws IOException {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "seoul");
        File outputPath = new File("D:\\data\\mago-server\\output", "seoul-korea-17");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "18",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleKoreaCreateLayerJson() throws IOException {
        //File inputPath = new File("G:\E-Drive\\(DEM) Sample", "korea");
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
