package com.gaia3d.release;

import com.gaia3d.command.MagoTerrainerMain;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Tag("Mandatory")
@Slf4j
public class DefaultTest {

    @Test
    void help() {
        String[] args = new String[]{"-h"};
        MagoTerrainerMain.main(args);
    }

    @Test
    void sampleBilinear() throws IOException {
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
        FileUtils.deleteDirectory(outputPath);
    }

    @Test
    void sampleBilinearMaxDepth12() throws IOException {
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
        FileUtils.deleteDirectory(outputPath);
    }

    @Test
    void sampleNearest() throws IOException {
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
        FileUtils.deleteDirectory(outputPath);
    }

    @Test
    void sampleNearestMaxDepth12() throws IOException {
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
        FileUtils.deleteDirectory(outputPath);
    }

    @Test
    void sampleAnotherCrsTerrain() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File samplePath = new File(Objects.requireNonNull(classLoader.getResource("another-crs-sample")).getFile());
        File inputPath = new File(samplePath, "input");
        File outputPath = new File(samplePath, "output");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "12",
                //"-d",
        };
        MagoTerrainerMain.main(args);
        FileUtils.deleteDirectory(outputPath);
    }

    @Test
    void sampleMultiTerrain() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File samplePath = new File(Objects.requireNonNull(classLoader.getResource("multi-sample")).getFile());
        File inputPath = new File(samplePath, "input");
        File outputPath = new File(samplePath, "output");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-min", "0",
                "-max", "12",
                //"-d",
        };
        MagoTerrainerMain.main(args);
        FileUtils.deleteDirectory(outputPath);
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
