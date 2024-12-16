package com.gaia3d.release;

import com.gaia3d.command.MagoTerrainerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
public class MaxLevelTest {

    private final File INPUT_PATH = new File("G:\\(2024)\\(2024) 3차원 데이터 모음\\GeoTIFF");
    private final File OUTPUT_PATH = new File("D:\\data\\mago-server\\output");

    @Test
    void sampleSeoulTerrainLevel14() {
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-14");

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
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-14");

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
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-15");

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
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-16");

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
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-17");

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
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-18");

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
        File inputPath = new File(INPUT_PATH, "seoul");
        File outputPath = new File(OUTPUT_PATH, "seoul-10-normal");

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
    void sampleSouthKorea() {
        File inputPath = new File(INPUT_PATH, "korea");
        File outputPath = new File(OUTPUT_PATH, "korea-10");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "10",
                "-calculateNormals",
                //"-d",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleKoreaCreateLayerJson() {
        //File inputPath = new File("G:\E-Drive\\(DEM) Sample", "korea");
        File outputPath = new File(OUTPUT_PATH, "south-korea-15");

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
