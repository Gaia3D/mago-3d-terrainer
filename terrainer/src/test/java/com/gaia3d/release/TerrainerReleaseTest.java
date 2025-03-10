package com.gaia3d.release;

import com.gaia3d.command.MagoTerrainerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
public class TerrainerReleaseTest {

    private final File INPUT_PATH = new File("D:\\data\\mago-3d-terrainer\\release-sample");
    private final File OUTPUT_PATH = new File("E:\\data\\mago-server\\output");

    @Test
    void testChangwon() {
        String name = "changwon_4326_0501_nodata";
        File inputPath = new File(INPUT_PATH, name);
        File outputPath = new File(OUTPUT_PATH, name);

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
    void multiNearest() {
        String name = "multi-resolution";
        File inputPath = new File(INPUT_PATH, name);
        File outputPath = new File(OUTPUT_PATH, "TR_" + name + "_nearest");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "12",
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
                "-leaveTemp",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void multiResolutionNodata() {
        String name = "multi-resolution-nodata";
        File inputPath = new File(INPUT_PATH, name);
        File outputPath = new File(OUTPUT_PATH, "TR_" + name);

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "15",
                "-interpolation", "nearest",
                "-calculateNormals",
                "-leaveTemp"
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void multiResolutionBig() {
        String name = "multi-resolution-big";
        File inputPath = new File(INPUT_PATH, name);
        File outputPath = new File(OUTPUT_PATH, "TR_" + name);

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-interpolation", "bilinear",
                "-calculateNormals",
                "-leaveTemp"
        };
        MagoTerrainerMain.main(args);
    }
}
