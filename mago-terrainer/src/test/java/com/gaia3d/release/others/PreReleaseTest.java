package com.gaia3d.release.others;

import com.gaia3d.command.Mago3DTerrainerMain;
import com.gaia3d.release.env.MagoTestConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
public class PreReleaseTest {

    @Test
    void multiBilinear() {
        String name = "multi-resolution";
        File inputPath = MagoTestConfig.getInputPath(name);
        File outputPath = MagoTestConfig.getOutputPath(name + "_bilinear");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-interpolation", "bilinear",
                "-calculateNormals",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void multiNearest() {
        String name = "multi-resolution";
        File inputPath = MagoTestConfig.getInputPath(name);
        File outputPath = MagoTestConfig.getOutputPath(name + "_nearest");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-interpolation", "nearest",
                "-calculateNormals",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void multiResolution() {
        String name = "multi-resolution";
        File inputPath = MagoTestConfig.getInputPath(name);
        File outputPath = MagoTestConfig.getOutputPath(name);

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-max", "10",
                "-interpolation", "nearest",
                "-calculateNormals",
        };
        Mago3DTerrainerMain.main(args);
    }


    @Test
    void multiResolutionWithGeoid() {
        String name = "multi-resolution";
        File inputPath = MagoTestConfig.getInputPath(name);
        File outputPath = MagoTestConfig.getOutputPath(name);

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath() + "_geoid",
                "-max", "10",
                "-interpolation", "nearest",
                "-calculateNormals",
                "-geoid", "EGM96",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void multiResolutionWithGeoidExternal() {
        String name = "multi-resolution";
        File inputPath = MagoTestConfig.getInputPath(name);
        File outputPath = MagoTestConfig.getOutputPath(name);
        File inputGeoidPath = MagoTestConfig.getInputPath("kr_ngii_KNGeoid18_4326.tif");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath() + "_kngeoid18",
                "-max", "10",
                "-interpolation", "nearest",
                "-calculateNormals",
                "-geoid", inputGeoidPath.getAbsolutePath(),
        };
        Mago3DTerrainerMain.main(args);
    }


    @Test
    void multiResolutionNodata() {
        String name = "multi-resolution-nodata";
        File inputPath = MagoTestConfig.getInputPath(name);
        File outputPath = MagoTestConfig.getOutputPath(name);

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-max", "13",
                "-interpolation", "nearest",
                "-calculateNormals",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void multiResolutionBig() {
        String name = "multi-resolution-big";
        File inputPath = MagoTestConfig.getInputPath(name);
        File outputPath = MagoTestConfig.getOutputPath(name);

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-max", "12",
                "-interpolation", "nearest",
                "-calculateNormals",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void multiResolutionBigWithGeoid() {
        String name = "multi-resolution-big";
        File inputPath = MagoTestConfig.getInputPath(name);
        File outputPath = MagoTestConfig.getOutputPath(name + "_geoid");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-max", "12",
                "-interpolation", "nearest",
                "-calculateNormals",
                "-geoid", "EGM96",
                "-nodataValue", "-8612",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void seoulTerrainQuadWithGeoid() {
        String name = "seoul-terrain-quad";
        File inputPath = MagoTestConfig.getInputPath(name);
        File outputPath = MagoTestConfig.getOutputPath(name + "_geoid");
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-max", "14",
                "-calculateNormals",
                "-geoid", "EGM96",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void koreaWithGeoid5m() {
        String name = "korea-terrain";
        File inputPath = new File("D:/data/mago-3d-tiler/terrain-sample/", "dem05-all-4326-cog.tif");
        File outputPath = MagoTestConfig.getOutputPath(name + "_nodata_geoid");
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-max", "14",
                "-calculateNormals",
                "-geoid", "EGM96",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void dalli() {
        String name = "dalli";
        File inputPath = MagoTestConfig.getInputPath(name);
        File outputPath = MagoTestConfig.getOutputPath(name);
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-calculateNormals",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void testChangwon() {
        String name = "changwon_4326_0501_nodata";
        File inputPath = MagoTestConfig.getInputPath(name);
        File outputPath = MagoTestConfig.getOutputPath(name);
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-max", "13",
                "-calculateNormals",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void nodata9999TestGeoid() {
        String name = "nodata-9999-test";
        File inputPath = MagoTestConfig.getInputPath(name);
        File outputPath = MagoTestConfig.getOutputPath(name);
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void crackTest() {
        String name = "crack_test";
        File inputPath = MagoTestConfig.getInputPath(name);
        File outputPath = MagoTestConfig.getOutputPath(name);
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-max", "14",
                "-leaveTemp",
                "-calculateNormals",
                "-geoid", "EGM96",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void resolutionTest5M() {
        String name = "test_jeju.tif";
        File inputPath = MagoTestConfig.getInputPath(name);
        File outputPath = MagoTestConfig.getOutputPath(name);
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-calculateNormals",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void resolutionTest1M() {
        String name = "test_jeju_1m.tif";
        File inputPath = MagoTestConfig.getInputPath(name);
        File outputPath = MagoTestConfig.getOutputPath(name);
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-calculateNormals",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void resolutionTest10CM() {
        String name = "test_jeju_10cm.tif";
        File inputPath = MagoTestConfig.getInputPath(name);
        File outputPath = MagoTestConfig.getOutputPath(name);
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-calculateNormals",
        };
        Mago3DTerrainerMain.main(args);
    }
}
