package com.gaia3d.command;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
public class VariousTest {

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
    void sriLanka14() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "srilanka");
        File outputPath = new File("D:\\data\\mago-server\\output", "srilanka-14");
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void india14() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "india");
        File outputPath = new File("D:\\data\\mago-server\\output", "india-14");
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void saudi14() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "saudi");
        File outputPath = new File("D:\\data\\mago-server\\output", "saudi-14");
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void thailand14() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "thailand");
        File outputPath = new File("D:\\data\\mago-server\\output", "thailand-14");
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }
}
