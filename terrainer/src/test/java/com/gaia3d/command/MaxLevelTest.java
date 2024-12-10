package com.gaia3d.command;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
public class MaxLevelTest {

    @Test
    void sampleSeoulTerrainLevel14() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "seoul");
        File outputPath = new File("D:\\data\\mago-server\\output", "seoul-korea-14");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-calculateNormals",
                //"-d",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleSeoulTerrainLevelRS14() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "seoul");
        File outputPath = new File("D:\\data\\mago-server\\output", "seoul-korea-14");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-intensity", "8.0",
                "-calculateNormals",
                //"-d",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleSeoulTerrainLevel15() {
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
    void sampleSeoulTerrainLevel16() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "seoul");
        File outputPath = new File("D:\\data\\mago-server\\output", "seoul-korea-16");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "16",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleSeoulTerrainLevel17() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "seoul");
        File outputPath = new File("D:\\data\\mago-server\\output", "seoul-korea-17");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "17",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleSeoulTerrainLevel18() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "seoul");
        File outputPath = new File("D:\\data\\mago-server\\output", "seoul-korea-18");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "18",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleSeoulCalcNormal() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "seoul");
        File outputPath = new File("D:\\data\\mago-server\\output", "seoul-korea-normal");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "11",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleKoreaCreateLayerJson() {
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
