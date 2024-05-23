package com.gaia3d.command;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class DefaultUnitTest {

    @Test
    void help() {
        String[] args = new String[]{"-h"};
        MagoMesherMain.main(args);
    }

    @Test
    void sample() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File samplePath = new File(Objects.requireNonNull(classLoader.getResource("sample")).getFile());
        File inputPath = new File(samplePath, "input");
        File outputPath = new File(samplePath, "output");
        File logPath = new File(outputPath, "log.txt");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-log", logPath.getAbsolutePath(),
                "-min", "0",
                "-max", "10",
                "-d",
        };
        MagoMesherMain.main(args);
        FileUtils.deleteDirectory(outputPath);
    }

    @Test
    void sampleMaxDepth12() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File samplePath = new File(Objects.requireNonNull(classLoader.getResource("sample")).getFile());
        File inputPath = new File(samplePath, "input");
        File outputPath = new File(samplePath, "output");
        File logPath = new File(outputPath, "log.txt");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-log", logPath.getAbsolutePath(),
                "-min", "0",
                "-max", "12",
                //"-d",
        };
        MagoMesherMain.main(args);
        FileUtils.deleteDirectory(outputPath);
    }

    @Test
    void sampleNearest() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File samplePath = new File(Objects.requireNonNull(classLoader.getResource("sample")).getFile());
        File inputPath = new File(samplePath, "input");
        File outputPath = new File(samplePath, "output");
        File logPath = new File(outputPath, "log.txt");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-log", logPath.getAbsolutePath(),
                "-min", "0",
                "-max", "10",
                "-interpolationType", "nearest",
                //"-d",
        };
        MagoMesherMain.main(args);
        FileUtils.deleteDirectory(outputPath);
    }

    @Test
    void sampleNearestMaxDepth12() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File samplePath = new File(Objects.requireNonNull(classLoader.getResource("sample")).getFile());
        File inputPath = new File(samplePath, "input");
        File outputPath = new File(samplePath, "output");
        File logPath = new File(outputPath, "log.txt");

        String[] args = new String[]{
                "-input", inputPath.getAbsolutePath(),
                "-output", outputPath.getAbsolutePath(),
                "-log", logPath.getAbsolutePath(),
                "-min", "0",
                "-max", "12",
                "-interpolationType", "nearest",
                //"-d",
        };
        MagoMesherMain.main(args);
        FileUtils.deleteDirectory(outputPath);
    }
}
