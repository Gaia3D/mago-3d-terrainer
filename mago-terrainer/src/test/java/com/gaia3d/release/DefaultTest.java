package com.gaia3d.release;

import com.gaia3d.release.env.MagoTestConfig;
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
        MagoTestConfig.execute(args);
    }

    @Test
    void help() {
        String[] args = new String[]{"--help"};
        MagoTestConfig.execute(args);
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
        MagoTestConfig.execute(args);
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
                "-max", "10",
        };
        MagoTestConfig.execute(args);
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
                "-max", "10",
        };
        MagoTestConfig.execute(args);
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
                "-max", "12",
        };
        MagoTestConfig.execute(args);
    }

    @Test
    void sampleNearest() {
        ClassLoader classLoader = getClass().getClassLoader();
        File samplePath = new File(Objects.requireNonNull(classLoader.getResource("sample")).getFile());
        File inputPath = new File(samplePath, "input");
        File outputPath = new File(samplePath, "output");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-max", "10",
                "-interpolationType", "nearest",
        };
        MagoTestConfig.execute(args);
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
                "-max", "12",
                "-interpolationType", "nearest",
        };
        MagoTestConfig.execute(args);
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
                "-max", "12",
        };
        MagoTestConfig.execute(args);
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
                "-max", "12",
        };
        MagoTestConfig.execute(args);
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
            MagoTestConfig.execute(args);
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
            MagoTestConfig.execute(args);
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
            MagoTestConfig.execute(args);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
