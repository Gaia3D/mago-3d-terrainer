package com.gaia3d.release;

import com.gaia3d.command.MagoTerrainerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.Objects;

@Tag("default")
@Slf4j
public class DefaultTest {

    @Test
    void noArgs() {
        String[] args = new String[]{};
        MagoTerrainerMain.main(args);
    }

    @Test
    void help() {
        String[] args = new String[]{"-h"};
        MagoTerrainerMain.main(args);
    }

    @Test
    void sample() {
        ClassLoader classLoader = getClass().getClassLoader();
        File samplePath = new File(Objects.requireNonNull(classLoader.getResource("sample")).getFile());
        File inputPath = new File(samplePath, "input");
        File outputPath = new File(samplePath, "output");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleBilinearWithGeoid() {
        ClassLoader classLoader = getClass().getClassLoader();
        File samplePath = new File(Objects.requireNonNull(classLoader.getResource("sample")).getFile());
        File inputPath = new File(samplePath, "input");
        File outputPath = new File(samplePath, "output");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-geoid", "EGM96",
                "-min", "0",
                "-max", "10",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleBilinear() {
        ClassLoader classLoader = getClass().getClassLoader();
        File samplePath = new File(Objects.requireNonNull(classLoader.getResource("sample")).getFile());
        File inputPath = new File(samplePath, "input");
        File outputPath = new File(samplePath, "output");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "10",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleBilinearMaxDepth12() {
        ClassLoader classLoader = getClass().getClassLoader();
        File samplePath = new File(Objects.requireNonNull(classLoader.getResource("sample")).getFile());
        File inputPath = new File(samplePath, "input");
        File outputPath = new File(samplePath, "output");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "12",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleNearest(){
        ClassLoader classLoader = getClass().getClassLoader();
        File samplePath = new File(Objects.requireNonNull(classLoader.getResource("sample")).getFile());
        File inputPath = new File(samplePath, "input");
        File outputPath = new File(samplePath, "output");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "10",
                "-interpolationType", "nearest",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleNearestMaxDepth12() {
        ClassLoader classLoader = getClass().getClassLoader();
        File samplePath = new File(Objects.requireNonNull(classLoader.getResource("sample")).getFile());
        File inputPath = new File(samplePath, "input");
        File outputPath = new File(samplePath, "output");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "12",
                "-interpolationType", "nearest",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleAnotherCrsTerrain() {
        ClassLoader classLoader = getClass().getClassLoader();
        File samplePath = new File(Objects.requireNonNull(classLoader.getResource("another-crs-sample")).getFile());
        File inputPath = new File(samplePath, "input");
        File outputPath = new File(samplePath, "output");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "12",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleMultiTerrain() {
        ClassLoader classLoader = getClass().getClassLoader();
        File samplePath = new File(Objects.requireNonNull(classLoader.getResource("multi-sample")).getFile());
        File inputPath = new File(samplePath, "input");
        File outputPath = new File(samplePath, "output");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "12",
        };
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleWrongCaseNoInput() {
        ClassLoader classLoader = getClass().getClassLoader();
        File samplePath = new File(Objects.requireNonNull(classLoader.getResource("sample")).getFile());
        File inputPath = new File(samplePath, "input");
        File outputPath = new File(samplePath, "output");

        String[] args = new String[]{
                "-output", outputPath.getAbsolutePath(),
                "-max", "12",
        };

        try {
            MagoTerrainerMain.main(args);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Test
    void sampleWrongCaseNoOutput() {
        ClassLoader classLoader = getClass().getClassLoader();
        File samplePath = new File(Objects.requireNonNull(classLoader.getResource("sample")).getFile());
        File inputPath = new File(samplePath, "input");
        File outputPath = new File(samplePath, "output");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-max", "12",
        };

        try {
            MagoTerrainerMain.main(args);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Test
    void sampleWrongCaseLowMaxDepthThanMinDepth() {
        ClassLoader classLoader = getClass().getClassLoader();
        File samplePath = new File(Objects.requireNonNull(classLoader.getResource("sample")).getFile());
        File inputPath = new File(samplePath, "input");
        File outputPath = new File(samplePath, "output");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "12",
                "-max", "5",
        };

        try {
            MagoTerrainerMain.main(args);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
