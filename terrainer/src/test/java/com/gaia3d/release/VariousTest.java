package com.gaia3d.release;

import com.gaia3d.command.MagoTerrainerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
public class VariousTest {

    private static final String MAX_LEVEL = "13";
    private final File INPUT_PATH = new File("G:\\(2024)\\(2024) 3차원 데이터 모음\\GeoTIFF");
    private final File OUTPUT_PATH = new File("E:\\data\\mago-server\\output");

    @Test
    void sriLanka() {
        File inputPath = new File(INPUT_PATH, "srilanka");
        File outputPath = new File(OUTPUT_PATH, "srilanka-" + MAX_LEVEL);
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
        File inputPath = new File(INPUT_PATH, "india");
        File outputPath = new File(OUTPUT_PATH, "india-" + MAX_LEVEL);
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
        File inputPath = new File(INPUT_PATH, "saudi");
        File outputPath = new File(OUTPUT_PATH, "saudi-" + MAX_LEVEL);
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
        File inputPath = new File(INPUT_PATH, "thailand");
        File outputPath = new File(OUTPUT_PATH, "thailand-" + MAX_LEVEL);
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
