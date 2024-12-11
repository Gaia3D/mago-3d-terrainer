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
    void sejong14Intensity1() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "sejong");
        File outputPath = new File("D:\\data\\mago-server\\output", "sejong-14-intensity-4");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-intensity", "4",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sample13Intensity1() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "seoul");
        File outputPath = new File("D:\\data\\mago-server\\output", "seoul-13-intensity-1");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "13",
                "-intensity", "1.0",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sample13Intensity2() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "seoul");
        File outputPath = new File("D:\\data\\mago-server\\output", "seoul-13-intensity-2");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "13",
                "-intensity", "2.0",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sample13Intensity4() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "seoul");
        File outputPath = new File("D:\\data\\mago-server\\output", "seoul-13-intensity-4");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "13",
                "-intensity", "4.0",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sample13Intensity8() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "seoul");
        File outputPath = new File("D:\\data\\mago-server\\output", "seoul-13-intensity-8");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "13",
                "-intensity", "8.0",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sample13Intensity16() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "seoul");
        File outputPath = new File("D:\\data\\mago-server\\output", "seoul-13-intensity-16");
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "13",
                "-intensity", "16.0",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sample14Intensity1() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "seoul");
        File outputPath = new File("D:\\data\\mago-server\\output", "seoul-14-intensity-1");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-intensity", "1.0",
                "-mosaicSize", "64",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sample14Intensity2() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "seoul");
        File outputPath = new File("D:\\data\\mago-server\\output", "seoul-14-intensity-2");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-intensity", "2.0",
                "-mosaicSize", "64",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sample14Intensity4() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "seoul");
        File outputPath = new File("D:\\data\\mago-server\\output", "seoul-14-intensity-4");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-intensity", "4.0",
                "-mosaicSize", "64",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sample14Intensity8() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "seoul");
        File outputPath = new File("D:\\data\\mago-server\\output", "seoul-14-intensity-8");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-intensity", "8.0",
                "-mosaicSize", "64",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sample14Intensity16() {
        File inputPath = new File("G:\\E-Drive\\(DEM) Sample", "seoul");
        File outputPath = new File("D:\\data\\mago-server\\output", "seoul-14-intensity-16");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-intensity", "16.0",
                "-mosaicSize", "64",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }
}
