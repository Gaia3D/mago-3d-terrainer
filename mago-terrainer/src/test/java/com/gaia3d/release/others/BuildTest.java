package com.gaia3d.release.others;

import com.gaia3d.command.Mago3DTerrainerMain;
import com.gaia3d.release.env.MagoTestConfig;
import org.junit.jupiter.api.Test;

import java.io.File;

public class BuildTest {

    @Test
    void multipleInputTest() {
        String name = "korea-5m-terrain";
        File inputPath = new File("D:/data/mago-3d-tiler/terrain-sample", "korea-05-cog-dem-4326.tif");
        File inputPath2 = new File("D:/data/mago-3d-tiler/terrain-sample", "korea-compressed.tif");
        File outputPath = MagoTestConfig.getOutputPath(name + "_with_geoid");
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-input", inputPath2.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-calculateNormals",
                "-geoid", "EGM2008",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void koreaWithGeoid5m() {
        String name = "korea-5m-terrain";
        File inputPath = new File("D:/data/mago-3d-tiler/terrain-sample", "korea-05-cog-dem-4326.tif");
        File inputPath2 = new File("D:/data/mago-3d-tiler/terrain-sample", "korea-compressed.tif");
        File outputPath = MagoTestConfig.getOutputPath(name + "_with_geoid");
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-input", inputPath2.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-calculateNormals",
                "-geoid", "EGM2008",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void koreaWithoutGeoid5m() {
        String name = "korea-5m-terrain";
        File inputPath = new File("D:/data/mago-3d-tiler/terrain-sample", "korea-05-cog-dem-4326.tif");
        File inputPath2 = new File("D:/data/mago-3d-tiler/terrain-sample", "korea-compressed.tif");
        File outputPath = MagoTestConfig.getOutputPath(name + "_with_geoid");
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-input", inputPath2.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-calculateNormals",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void koreaWithGeoidOnlyJson() {
        String name = "korea-terrain";
        File outputPath = MagoTestConfig.getOutputPath(name);
        String[] args = new String[]{
                "-input", outputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-calculateNormals",
                "-json"
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void koreaWithGeoid100m() {
        String name = "korea-mini-terrain";
        File inputPath = new File("D:/data/mago-3d-tiler/terrain-sample/", "korea-compressed.tif");
        File outputPath = MagoTestConfig.getOutputPath(name + "_nodata_geoid");
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-calculateNormals",
                "-geoid", "EGM96",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void koreaWithGeoid100mTest() {
        String name = "korea-100m-dem";
        File inputPath = new File("D:/data/mago-3d-tiler/terrain-sample/", "korea-compressed.tif");
        File outputPath = MagoTestConfig.getOutputPath(name);

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-calculateNormals",
                "-geoid", "EGM96",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void globalCopernicusDem90m() {
        String name = "global-copernicus-dem-90m";
        File inputPath = new File("E:\\copernicus_dem_90m");
        File outputPath = MagoTestConfig.getOutputPath(name);
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                //"-max", "6",
                "-calculateNormals",
                "-geoid", "EGM96",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void changewon1mWithBase5M() {
        String name = "changewon1-m-with-base-5M";
        File inputPath = new File("D:\\data\\mago-3d-terrainer\\release-sample\\changwon_4326_0501_nodata");
        File outputPath = MagoTestConfig.getOutputPath(name);
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-calculateNormals",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void changewon1mWithBase5mMini() {
        String name = "changewon1-m-with-base-5M-mini";
        File inputPath = new File("D:\\data\\mago-3d-terrainer\\release-sample\\changwon_4326_mini");
        File outputPath = MagoTestConfig.getOutputPath(name);
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-calculateNormals",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void dsmSeattle2026() {
        String name = "dsm-tri-seattle-2026";
        File inputPath = new File("D:\\data\\mago-3d-terrainer\\release-sample\\dsm-tri-seattle-2026");
        File outputPath = MagoTestConfig.getOutputPath(name);
        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-calculateNormals",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void resampleBigMoreFast() {
        String name = "dem05-all-5186";
        File inputPath = new File("G:\\workspace\\dem05-all-5186.tif");
        File outputPath = MagoTestConfig.getOutputPath(name);

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "12",
                "-leaveTemp",
                "-calculateNormals",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void moon() {
        String name = "Lunar_LRO_LOLA";
        File inputPath = new File("D:\\user\\znkim\\Downloads\\Lunar_LRO_LOLA_Global_LDEM_118m_Mar2014-wrap.tif");
        File outputPath = MagoTestConfig.getOutputPath(name);

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "8",
                "-body", "moon",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void moonJson() {
        String name = "Lunar_LRO_LOLA";
        File outputPath = MagoTestConfig.getOutputPath(name);

        String[] args = new String[]{
                "-input", outputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "8",
                "-body", "moon",
                "-json",
        };
        Mago3DTerrainerMain.main(args);
    }
}
