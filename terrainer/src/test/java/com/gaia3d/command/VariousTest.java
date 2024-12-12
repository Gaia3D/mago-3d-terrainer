package com.gaia3d.command;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
public class VariousTest {

    private static final String MAX_LEVEL = "15";

    /*@Test
    void sriLanka10() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "srilanka");
        File outputPath = new File("D:\\data\\mago-server\\output", "srilanka-10");
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "10",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void india10() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "india");
        File outputPath = new File("D:\\data\\mago-server\\output", "india-10");
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "10",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args)
        ;
    }

    @Test
    void saudi10() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "saudi");
        File outputPath = new File("D:\\data\\mago-server\\output", "saudi-10");
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "10",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void thailand10() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "thailand");
        File outputPath = new File("D:\\data\\mago-server\\output", "thailand-10");
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "10",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }*/

    @Test
    void sriLanka() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "srilanka");
        File outputPath = new File("D:\\data\\mago-server\\output", "srilanka-" + MAX_LEVEL);
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", MAX_LEVEL,
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void india() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "india");
        File outputPath = new File("D:\\data\\mago-server\\output", "india-" + MAX_LEVEL);
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", MAX_LEVEL,
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void saudi() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "saudi");
        File outputPath = new File("D:\\data\\mago-server\\output", "saudi-" + MAX_LEVEL);
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", MAX_LEVEL,
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void thailand() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "thailand");
        File outputPath = new File("D:\\data\\mago-server\\output", "thailand-" + MAX_LEVEL);
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", MAX_LEVEL,
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }
}
