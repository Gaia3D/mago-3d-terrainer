package com.gaia3d.release.others;

import com.gaia3d.command.MagoTerrainerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
public class TerrainerReleaseTest {

    private final File INPUT_PATH = new File("D:\\data\\mago-3d-terrainer\\release-sample");
    private final File OUTPUT_PATH = new File("H:\\workspace\\mago-server\\output");

    @Test
    void resampleBigMoreFast() {
        File inputPath = new File("G:\\workspace\\dem05-all-5186.tif");
        File outputPath = new File(OUTPUT_PATH, "more-fast");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "12",
                "-leaveTemp",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void dalli() {
        File inputPath = new File(INPUT_PATH, "dalli");
        File outputPath = new File(OUTPUT_PATH, "dalli-test");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "17",
                //"-interpolation", "nearest",
                "-leaveTemp",
                "-calculateNormals",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void testChangwon() {
        String name = "changwon_4326_0501_nodata";
        File inputPath = new File(INPUT_PATH, name);
        File outputPath = new File(OUTPUT_PATH, name);

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "13",
                "-calculateNormals",
                "-leaveTemp",
                "-log", new File(outputPath, "log.txt").getAbsolutePath()
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void multiBilinear() {
        String name = "multi-resolution";
        File inputPath = new File(INPUT_PATH, name);
        File outputPath = new File(OUTPUT_PATH, "TR_" + name + "_bilinear");

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
                "-max", "10",
                "-interpolation", "nearest",
                "-calculateNormals",
                //"-leaveTemp"
        };
        MagoTerrainerMain.main(args);
    }


    @Test
    void multiResolutionWithGeoid() {
        String name = "multi-resolution";
        File inputPath = new File(INPUT_PATH, name);
        File outputPath = new File(OUTPUT_PATH, "TR_" + name);

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath() + "_geoid",
                "-min", "0",
                "-max", "10",
                "-interpolation", "nearest",
                "-calculateNormals",
                "-geoid", "EGM96",
                //"-leaveTemp"
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void multiResolutionWithGeoidExternal() {
        String name = "multi-resolution";
        File inputPath = new File(INPUT_PATH, name);
        File outputPath = new File(OUTPUT_PATH, "TR_" + name);
        File inputGeoidPath = new File(INPUT_PATH, "kr_ngii_KNGeoid18_4326.tif");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath() + "_kngeoid18",
                "-min", "0",
                "-max", "10",
                "-interpolation", "nearest",
                "-calculateNormals",
                "-geoid", inputGeoidPath.getAbsolutePath(),
                //"-leaveTemp"
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
                "-max", "12",
                "-interpolation", "nearest",
                "-calculateNormals",
                //"-leaveTemp"
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
                "-max", "12",
                "-interpolation", "nearest",
                "-calculateNormals",
                "-leaveTemp"
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void multiResolutionBigWithGeoid() {
        String name = "multi-resolution-big";
        File inputPath = new File(INPUT_PATH, name);
        File outputPath = new File(OUTPUT_PATH, "TR_" + name + "_geoid");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "12",
                "-interpolation", "nearest",
                "-nodataValue", "-8612",
                "-calculateNormals",
                "-geoid", "EGM96",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void seoulTerrainQuadWithGeoid() {
        String name = "seoul-terrain-quad";
        File inputPath = new File(INPUT_PATH, name);
        File outputPath = new File(OUTPUT_PATH, "TR_" + name + "_geoid");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-calculateNormals",
                "-geoid", "EGM96",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void koreaWithGeoid() {
        String name = "korea-terrain";
        File inputPath = new File("D:/data/mago-3d-tiler/terrain-sample", "dem05-all-5186-nodata.tif");
        File outputPath = new File(OUTPUT_PATH, "TR_" + name + "_nodata_geoid");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-temp", "C:/temp/",
                "-calculateNormals",
                "-geoid", "EGM96",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void nodata9999TestGeoid() {
        String name = "nodata-9999-test";
        File inputPath = new File(INPUT_PATH, name);
        File outputPath = new File(OUTPUT_PATH, "TR_" + name + "_geoid");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "14",
                "-calculateNormals",
                "-geoid", "EGM96",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void koreaWithGeoidOnlyJson() {
        String name = "korea-terrain";
        File outputPath = new File(OUTPUT_PATH, "TR_" + name + "_geoid");

        String[] args = new String[]{
                "-input", outputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-calculateNormals",
                "-json"
        };
        MagoTerrainerMain.main(args);
    }
}
