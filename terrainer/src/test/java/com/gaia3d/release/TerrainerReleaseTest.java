package com.gaia3d.release;

import com.gaia3d.command.MagoTerrainerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
public class TerrainerReleaseTest {

    private final File INPUT_PATH = new File("D:\\data\\mago-3d-terrainer\\release-sample");
    private final File OUTPUT_PATH = new File("D:\\data\\mago-server\\output");

    @Test
    void multiNearest() {
        String name = "multi-resolution";
        File inputPath = new File(INPUT_PATH, name);
        File outputPath = new File(OUTPUT_PATH, "TR_" + name + "_nearest");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "13",
                "-intensity", "4",
                "-interpolation", "nearest",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void multiResolution() {
        String name = "multi-resolution";
        File inputPath = new File(INPUT_PATH, name);
        File outputPath = new File(OUTPUT_PATH, "TR_" + name);

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-interpolation", "bilinear",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }
}
