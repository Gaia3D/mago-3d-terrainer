package com.gaia3d.command;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalOptionsNormalsTest {

    @Test
    @Tag("default")
    void calculateNormalsIsEnabledByDefault() throws Exception {
        initGlobalOptions();

        assertTrue(GlobalOptions.getInstance().isCalculateNormalsExtension());
    }

    @Test
    @Tag("default")
    void legacyCalculateNormalsOptionKeepsNormalsEnabled() throws Exception {
        initGlobalOptions("--calculateNormals");

        assertTrue(GlobalOptions.getInstance().isCalculateNormalsExtension());
    }

    @Test
    @Tag("default")
    void noCalculateNormalsDisablesNormals() throws Exception {
        initGlobalOptions("--noCalculateNormals");

        assertFalse(GlobalOptions.getInstance().isCalculateNormalsExtension());
    }

    @Test
    @Tag("default")
    void shortNoCalculateNormalsDisablesNormals() throws Exception {
        initGlobalOptions("-ncn");

        assertFalse(GlobalOptions.getInstance().isCalculateNormalsExtension());
    }

    @Test
    @Tag("default")
    void noCalculateNormalsWinsWhenBothOptionsArePresent() throws Exception {
        initGlobalOptions("--calculateNormals", "--noCalculateNormals");

        assertFalse(GlobalOptions.getInstance().isCalculateNormalsExtension());
    }

    private void initGlobalOptions(String... extraArgs) throws Exception {
        GlobalOptions.recreateInstance();
        Path input = Files.createTempDirectory("terrainer-input");
        Path output = Files.createTempDirectory("terrainer-output");

        String[] args = new String[4 + extraArgs.length];
        args[0] = "--input";
        args[1] = input.toString();
        args[2] = "--output";
        args[3] = output.toString();
        System.arraycopy(extraArgs, 0, args, 4, extraArgs.length);

        CommandLineConfiguration configuration = GlobalOptions.getInstance().getCommandLineConfiguration();
        Options options = configuration.createOptions();
        CommandLine commandLine = configuration.createCommandLine(options, args);
        GlobalOptions.init(commandLine);
    }
}
