package com.gaia3d.command;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Objects;

public class DefaultUnitTest {

    @Test
    void help() {
        String[] args = new String[]{"-h"};
        MagoMesherMain.main(args);
    }

    @Test
    void sample() {
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
    }
}
