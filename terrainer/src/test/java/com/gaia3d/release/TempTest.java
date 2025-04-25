package com.gaia3d.release;

import com.gaia3d.command.MagoTerrainerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
public class TempTest {

    private final File INPUT_PATH = new File("G:\\(archive)\\(archive) 3차원 데이터 모음\\GeoTIFF");
    private final File OUTPUT_PATH = new File("E:\\data\\mago-server\\output");

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
    void sejong14IntensityContinue() {
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-12-intensity-1");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-intensity", "4",
                "-calculateNormals",
                "-continue",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sample12Intensity1() {
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-12-intensity-1");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "12",
                "-intensity", "1.0",
                "-calculateNormals",
                "-leaveTemp",
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
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sample14Intensity2() {
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-15-intensity-2");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "15",
                "-intensity", "4.0",
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

    @Test
    void sampleNearest() {
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-nearest-16");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "15",
                "-interpolationType", "nearest",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleBilinear() {
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-bilinear-15");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "15",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }


    @Test
    void sampleDebug() {
        File inputPath = new File(INPUT_PATH, "debug");
        File outputPath = new File(OUTPUT_PATH, "debug-terrain-5186-16");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "16",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleDem05Parts() {
        File inputPath = new File("D:\\dem05-wgs84-parts");
        File outputPath = new File(OUTPUT_PATH, "dem05-wgs84-parts");

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
    void sampleDem5m4326() {
        File inputPath = new File("D:\\dem05-wgs84");
        File outputPath = new File(OUTPUT_PATH, "dem05-wgs84-korea-all");

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
    void sampleDem5m5186() {
        File inputPath = new File("D:\\dem05-wgs84");
        File outputPath = new File(OUTPUT_PATH, "dem05-wgs84-korea-all");

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
    void sampleDem5m5186Part() {
        File inputPath = new File("D:\\dem05-5186-part-real");
        File outputPath = new File(OUTPUT_PATH, "dem05-5186-part-real-new-14");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-leaveTemp",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleDem30m5186Part() {
        File inputPath = new File("D:\\dem30-5186");
        File outputPath = new File(OUTPUT_PATH, "dem30-5186");

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
    void sampleDemJSON() {
        File inputPath = new File("E:\\data\\mago-server\\output\\dem05-wgs84-korea-all-14");
        File outputPath = new File("E:\\data\\mago-server\\output\\dem05-wgs84-korea-all-14");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-json",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void dem5m4326() {
        File inputPath = new File("D:\\korea-5m-4326-bilinear");
        File outputPath = new File(OUTPUT_PATH, "korea-5m-4326-bilinear");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-leaveTemp",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void dem5m5186parts() {
        File inputPath = new File("D:\\dem05-5186-parts");
        File outputPath = new File(OUTPUT_PATH, "korea-5m-5186-parts");

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
    void ws2dem1m() {
        File inputPath = new File("G:\\(archive)\\(archive) 3차원 데이터 모음\\GeoTIFF\\wangsuk2_1m");
        File outputPath = new File(OUTPUT_PATH, "ws2-dem-1m");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "16",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sangjiUniversity() {
        File inputPath = new File("G:\\(archive)\\(archive) 3차원 데이터 모음\\GeoTIFF\\sangji-university");
        File outputPath = new File(OUTPUT_PATH, "sangji-university");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "16",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sangjiUniversity5186() {
        File inputPath = new File("G:\\(archive)\\(archive) 3차원 데이터 모음\\GeoTIFF\\sangji-university-5186");
        File outputPath = new File(OUTPUT_PATH, "sangji-university-5186");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "16",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }
}
