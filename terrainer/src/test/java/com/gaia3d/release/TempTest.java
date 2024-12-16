package com.gaia3d.release;

import com.gaia3d.command.MagoTerrainerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
public class TempTest {

    private final File INPUT_PATH = new File("G:\\(2024)\\(2024) 3차원 데이터 모음\\GeoTIFF");
    private final File OUTPUT_PATH = new File("D:\\data\\mago-server\\output");

    @Test
    void sejong14Intensity1() {
        File inputPath = new File(INPUT_PATH, "sejong");
        File outputPath = new File(OUTPUT_PATH, "sejong-14-intensity-4");

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
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-13-intensity-1");

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
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-13-intensity-2");

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
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-13-intensity-4");

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
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-13-intensity-8");

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
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-13-intensity-16");
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
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-14-intensity-1");

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
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-14-intensity-2");

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
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-14-intensity-4");

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
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-14-intensity-8");

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
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-14-intensity-16");

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
