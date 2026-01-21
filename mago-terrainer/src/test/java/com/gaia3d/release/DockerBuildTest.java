package com.gaia3d.release;

import com.gaia3d.command.Configurator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

//@Tag("default")
@Slf4j
class DockerBuildTest {

    static {
        Configurator.initConsoleLogger();
    }

    @Test
    void pull() throws IOException {
        String dockerImage = "gaia3d/mago-3d-terrainer:latest";
        List<String> argList = new ArrayList<>();
        argList.add("docker");
        argList.add("image");
        argList.add("pull");
        argList.add(dockerImage);
        runCommand(argList);
    }

    @Test
    void inspect() throws IOException {
        String dockerImage = "gaia3d/mago-3d-terrainer:latest";
        List<String> argList = new ArrayList<>();
        argList.add("docker");
        argList.add("image");
        argList.add("inspect");
        argList.add(dockerImage);
        runCommand(argList);
    }

    @Test
    void runWithSimple() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();

        File input = new File(classLoader.getResource("./sample/input/sample.tif").getFile());
        File resource = new File(input.getParentFile().getParent());

        String dockerImage = "gaia3d/mago-3d-terrainer:latest";
        List<String> argList = new ArrayList<>();
        argList.add("docker");
        argList.add("run");
        argList.add("--rm");
        argList.add("-v");
        argList.add(resource.getAbsolutePath() + ":/workspace");
        argList.add(dockerImage);
        argList.add("--input");
        argList.add("/workspace/input");
        argList.add("--output");
        argList.add("/workspace/output");

        runCommand(argList);
    }

    @Test
    void runWithSimpleAndSave() throws IOException {
        String resourcePath = "multi-resolution";
        File inputPath = MagoTestConfig.getInputPath(resourcePath);
        File outputPath = MagoTestConfig.getOutputPath(resourcePath);

        File inputParentPath = inputPath.getParentFile();
        File outputParentPath = outputPath.getParentFile();

        String dockerImage = "gaia3d/mago-3d-terrainer:latest";
        List<String> argList = new ArrayList<>();
        argList.add("docker");
        argList.add("run");
        argList.add("--rm");
        argList.add("-v");
        argList.add(inputParentPath.getAbsolutePath() + ":/workspace/input");
        argList.add("-v");
        argList.add(outputParentPath.getAbsolutePath() + ":/workspace/output");
        argList.add(dockerImage);
        argList.add("--input");
        argList.add("/workspace/input/" + inputPath.getName());
        argList.add("--output");
        argList.add("/workspace/output/" + outputPath.getName());
        argList.add("--max");
        argList.add("12");
        argList.add("--geoid");
        argList.add("EGM96");

        runCommand(argList);
    }

    @Test
    void runWithSimpleAndSave2() throws IOException {
        String resourcePath = "multi-resolution-big";
        File inputPath = MagoTestConfig.getInputPath(resourcePath);
        File outputPath = MagoTestConfig.getOutputPath(resourcePath);

        File inputParentPath = inputPath.getParentFile();
        File outputParentPath = outputPath.getParentFile();

        String dockerImage = "gaia3d/mago-3d-terrainer:latest";
        List<String> argList = new ArrayList<>();
        argList.add("docker");
        argList.add("run");
        argList.add("--rm");
        argList.add("-v");
        argList.add(inputParentPath.getAbsolutePath() + ":/workspace/input");
        argList.add("-v");
        argList.add(outputParentPath.getAbsolutePath() + ":/workspace/output");
        argList.add(dockerImage);
        argList.add("--input");
        argList.add("/workspace/input/" + inputPath.getName());
        argList.add("--output");
        argList.add("/workspace/output/" + outputPath.getName());
        argList.add("--max");
        argList.add("12");
        argList.add("--geoid");
        argList.add("EGM96");

        runCommand(argList);
    }

    private void runCommand(List<String> argList) throws IOException {
        String[] args = argList.toArray(new String[0]);

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.redirectErrorStream(true);
        StringBuilder stringBuilder = new StringBuilder();
        for (String arg : args) {
            stringBuilder.append(arg).append(" ");
        }
        String command = stringBuilder.toString();
        Process process = processBuilder.start();
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        log.info("Executing command: {}", command);
        log.info("***Starting command execution***");
        for (String str; (str = inputReader.readLine()) != null; ) {
            log.info(str);
        }
        for (String str; (str = errorReader.readLine()) != null; ) {
            log.error(str);
        }
        log.info("***Command executed successfully***");
    }
}