package com.gaia3d.command;

import com.gaia3d.terrain.tile.TileWgs84Manager;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    @Tag("default")
    void builtInGeoidModelsResolveToExtractedFiles() throws Exception {
        for (String geoidModel : new String[]{"EGM96", "EGM84", "EGM2008"}) {
            initGlobalOptions("--geoid", geoidModel);

            String geoidPath = GlobalOptions.getInstance().getGeoidPath();
            assertNotNull(geoidPath);
            assertTrue(Files.exists(Path.of(geoidPath)), geoidModel);
            assertTrue(geoidPath.endsWith(".tif"), geoidModel);
        }
    }

    @Test
    @Tag("default")
    void repeatedInputOptionsArePreservedAndResolved() throws Exception {
        GlobalOptions.recreateInstance();
        Path inputA = Files.createTempDirectory("terrainer-input-a");
        Path inputB = Files.createTempDirectory("terrainer-input-b");
        Path output = Files.createTempDirectory("terrainer-output");
        Path rasterA = Files.createFile(inputA.resolve("a.tif"));
        Path rasterB = Files.createFile(inputB.resolve("b.tiff"));

        String[] args = new String[]{
                "--input", inputA.toString(),
                "--input", inputB.toString(),
                "--output", output.toString()
        };

        CommandLineConfiguration configuration = GlobalOptions.getInstance().getCommandLineConfiguration();
        Options options = configuration.createOptions();
        CommandLine commandLine = configuration.createCommandLine(options, args);
        GlobalOptions.init(commandLine);

        assertEquals(List.of(inputA.toString(), inputB.toString()), GlobalOptions.getInstance().getInputPaths());
        assertEquals(inputA.toString(), GlobalOptions.getInstance().getInputPath());

        List<String> rasterFileNames = new TileWgs84Manager().resolveInputRasterFileNames();
        assertEquals(List.of(rasterA.toAbsolutePath().toString(), rasterB.toAbsolutePath().toString()), rasterFileNames);
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
