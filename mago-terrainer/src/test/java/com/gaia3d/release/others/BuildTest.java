package com.gaia3d.release.others;

import com.gaia3d.command.Mago3DTerrainerMain;
import com.gaia3d.release.env.MagoTestConfig;
import org.junit.jupiter.api.Test;

import java.io.File;

public class BuildTest {

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
                "-min", "0",
                "-max", "14",
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
                "-max", "4",
                "-calculateNormals",
                "-geoid", "EGM96",
        };
        Mago3DTerrainerMain.main(args);
    }

    @Test
    void resampleBigMoreFast() {
        String name = "global-copernicus-dem-90m";
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
