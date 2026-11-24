package com.gaia3d.release;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.command.Mago3DTerrainerMain;
import com.gaia3d.release.golden.TerrainGolden;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Release feature tests. Each test runs one feature on a small sample and compares the output with the golden manifest.
 * <pre>
 * ./gradlew :mago-terrainer:test -PincludeReleaseTests --tests "com.gaia3d.release.ReleaseFeatureTest"
 * ./gradlew :mago-terrainer:test -PincludeReleaseTests -PupdateGolden --tests "com.gaia3d.release.ReleaseFeatureTest"  // rewrite the golden manifests
 * </pre>
 * See src/test/resources/golden/README.md.
 */
@Tag("release")
@TestMethodOrder(MethodOrderer.DisplayName.class) // F01 first, the golden of F01 is shared
class ReleaseFeatureTest {
    private static final File RESOURCES = new File("src/test/resources");
    private static final File OUTPUT_ROOT = new File("build/release-test");

    private static final File SAMPLE = new File(RESOURCES, "sample/input");
    private static final File MULTI_SAMPLE = new File(RESOURCES, "multi-sample/input");
    private static final File MULTI_RESOLUTION_SAMPLE = new File(RESOURCES, "multi-resolution-sample/input");
    private static final File ANOTHER_CRS_SAMPLE = new File(RESOURCES, "another-crs-sample/input");
    private static final File COPERNICUS_SAMPLE = new File(RESOURCES, "copernicus-sample");
    private static final File GARISAN_1M = new File(RESOURCES, "precision-test-garisan/garisan-precision-1m.tif");
    private static final File KNGEOID18 = new File(RESOURCES, "custom-geoid-kngeoid18/kr_ngii_KNGeoid18_4326.tif");

    // --- basic options ---

    @Test
    @DisplayName("F01 basic: EPSG:5186 input, bilinear, normals")
    void basic() throws IOException {
        assertGolden("F01-basic", run("F01-basic", "-i", path(SAMPLE), "-max", "10"));
    }

    @Test
    @DisplayName("F02 nearest interpolation")
    void nearestInterpolation() throws IOException {
        assertGolden("F02-nearest", run("F02-nearest", "-i", path(SAMPLE), "-max", "10", "-it", "nearest"));
    }

    @Test
    @DisplayName("F03 no normals")
    void noCalculateNormals() throws IOException {
        assertGolden("F03-no-normals", run("F03-no-normals", "-i", path(SAMPLE), "-max", "10", "-ncn"));
    }

    @Test
    @DisplayName("F04 refinement intensity")
    void intensity() throws IOException {
        assertGolden("F04-intensity", run("F04-intensity", "-i", path(SAMPLE), "-max", "10", "-is", "8"));
    }

    @Test
    @DisplayName("F05 mosaic size and raster split size")
    void mosaicAndRasterMaxSize() throws IOException {
        assertGolden("F05-mosaic-raster-size", run("F05-mosaic-raster-size", "-i", path(SAMPLE), "-max", "10", "-ms", "4", "-mr", "1024"));
    }

    @Test
    @Disabled("Not implemented: -waterMask/-metadata only add the extension names to layer.json, the tiles have no extension data.")
    @DisplayName("F06 water mask and metadata extensions")
    void extensions() throws IOException {
        assertGolden("F06-extensions", run("F06-extensions", "-i", path(SAMPLE), "-max", "10", "-wm", "-md"));
    }

    // --- height reference ---

    @Test
    @DisplayName("F07 built-in geoid (EGM96)")
    void builtInGeoid() throws IOException {
        assertGolden("F07-geoid-egm96", run("F07-geoid-egm96", "-i", path(SAMPLE), "-max", "10", "-geoid", "EGM96"));
    }

    @Test
    @DisplayName("F08 external geoid file (KNGeoid18)")
    void externalGeoid() throws IOException {
        assertGolden("F08-geoid-external", run("F08-geoid-external", "-i", path(SAMPLE), "-max", "10", "-geoid", path(KNGEOID18)));
    }

    // --- input CRS / resolution ---

    @Test
    @DisplayName("F09 EPSG:5186 5m input")
    void anotherCrs() throws IOException {
        assertGolden("F09-crs-5186-5m", run("F09-crs-5186-5m", "-i", path(ANOTHER_CRS_SAMPLE), "-max", "12"));
    }

    @Test
    @DisplayName("F10 EPSG:4326 input, separated files")
    void geographicInput() throws IOException {
        assertGolden("F10-crs-4326", run("F10-crs-4326", "-i", path(COPERNICUS_SAMPLE), "-max", "9"));
    }

    @Test
    @DisplayName("F11 EPSG:5187 1m input, automatic max depth")
    void autoMaxDepth() throws IOException {
        assertGolden("F11-auto-max-depth", run("F11-auto-max-depth", "-i", path(GARISAN_1M)));
    }

    // --- multiple inputs ---

    @Test
    @DisplayName("F12 multiple adjacent files")
    void multipleFiles() throws IOException {
        assertGolden("F12-multi-file", run("F12-multi-file", "-i", path(MULTI_SAMPLE), "-max", "12"));
    }

    @Test
    @DisplayName("F13 multi-resolution overlap (500m/100m/30m), resolution priority")
    void multiResolutionOverlap() throws IOException {
        assertGolden("F13-multi-resolution", run("F13-multi-resolution", "-i", path(MULTI_RESOLUTION_SAMPLE), "-max", "12"));
    }

    @Test
    @Disabled("Not implemented: PriorityType.HIGHER_VALUE is commented out, so \"-pt higher\" falls back to resolution.")
    @DisplayName("F14 multi-resolution overlap, higher priority")
    void multiResolutionHigherPriority() throws IOException {
        assertGolden("F14-multi-resolution-higher", run("F14-multi-resolution-higher", "-i", path(MULTI_RESOLUTION_SAMPLE), "-max", "12", "-pt", "higher"));
    }

    @Test
    @DisplayName("F15 multiple -input options and NODATA value")
    void repeatedInputAndNodata() throws IOException {
        File output = run("F15-repeated-input-nodata",
                "-i", path(new File(MULTI_RESOLUTION_SAMPLE, "dem500.tif")),
                "-i", path(new File(MULTI_RESOLUTION_SAMPLE, "dem100.tif")),
                "-max", "11", "-nv", "0");
        assertGolden("F15-repeated-input-nodata", output);
    }

    // --- temp / layer.json ---

    @Test
    @DisplayName("F16 leave temp: same output as F01, temp files kept")
    void leaveTemp() throws IOException {
        File output = run("F16-leave-temp", "-i", path(SAMPLE), "-max", "10", "-lt");
        assertTrue(new File(output, "temp").isDirectory(), "temp folder must be kept");
        assertSameAsGolden("F01-basic", output);
    }

    @Test
    @DisplayName("F17 custom temp path: same output as F01")
    void customTempPath() throws IOException {
        File tempPath = new File(OUTPUT_ROOT, "F17-temp-path-temp");
        FileUtils.deleteDirectory(tempPath);
        assertSameAsGolden("F01-basic", run("F17-temp-path", "-i", path(SAMPLE), "-max", "10", "-t", path(tempPath)));
    }

    @Test
    @DisplayName("F18 layer.json only (-json) on an existing tileset")
    void layerJsonOnly() throws IOException {
        File output = run("F18-json", "-i", path(SAMPLE), "-max", "8");
        File layerJson = new File(output, "layer.json");
        assertTrue(layerJson.delete());
        execute("-i", path(output), "-o", path(output), "-json");
        assertTrue(layerJson.isFile(), "layer.json must be generated");
        assertGolden("F18-json", output);
    }

    // --- continue / recovery ---

    @Test
    @DisplayName("F19 continue a completed tileset to a deeper max depth")
    void continueToDeeperDepth() throws IOException {
        File output = run("F19-continue-deeper", "-i", path(SAMPLE), "-max", "8");
        execute("-i", path(SAMPLE), "-o", path(output), "-max", "10", "-continue");
        assertGolden("F19-continue-deeper", output);
    }

    @Test
    @DisplayName("F20 recovery: killed right after a depth ends, continue gives the same output as F01")
    void recoverAfterKillAtDepthBoundary() throws Exception {
        File output = runAndKill("F20-recovery-depth-boundary", "\\[Tile\\]\\[7/10\\] - End", "-i", path(SAMPLE), "-max", "10");
        execute("-i", path(SAMPLE), "-o", path(output), "-max", "10", "-continue");
        assertSameAsGolden("F01-basic", output);
    }

    @Test
    @DisplayName("F21 recovery: killed in the middle of a depth, continue generates the interrupted depth again")
    void recoverAfterKillInDepth() throws Exception {
        // the depth 11 of this sample has 2 subranges: the run is killed after the first one modified the temp tiles (#34)
        File output = runAndKill("F21-recovery-in-depth", "\\[Tile\\]\\[11/12\\]\\[2/", "-i", path(MULTI_RESOLUTION_SAMPLE), "-max", "12");
        execute("-i", path(MULTI_RESOLUTION_SAMPLE), "-o", path(output), "-max", "12", "-continue");
        // no missing tiles. The interrupted depth is generated from the quantized meshes, so the tiles differ from F13
        TerrainGolden.assertSameFiles("F13-multi-resolution", output);
        assertGolden("F21-recovery-in-depth", output);
    }

    // --- helpers ---

    private static String path(File file) {
        return file.getAbsolutePath();
    }

    private static void assertGolden(String caseName, File output) throws IOException {
        TerrainGolden.assertMatches(caseName, output);
    }

    /**
     * Compares the output with the golden of another case. It never rewrites that golden.
     */
    private static void assertSameAsGolden(String caseName, File output) throws IOException {
        TerrainGolden.assertMatches(caseName, output, false);
    }

    /**
     * Runs the terrainer into a clean output folder (build/release-test/{caseName}).
     */
    private static File run(String caseName, String... args) throws IOException {
        File output = new File(OUTPUT_ROOT, caseName);
        FileUtils.deleteDirectory(output);
        List<String> fullArgs = new ArrayList<>(Arrays.asList(args));
        fullArgs.addAll(List.of("-o", path(output), "-l", path(new File(OUTPUT_ROOT, caseName + ".log"))));
        execute(fullArgs.toArray(new String[0]));
        assertTrue(new File(output, "layer.json").isFile(), "layer.json must be generated");
        return output;
    }

    private static void execute(String... args) {
        GlobalOptions.recreateInstance();
        Mago3DTerrainerMain.main(args);
    }

    /**
     * Runs the terrainer in a child JVM and kills it (like a crash) when the log matches the pattern.
     */
    private static File runAndKill(String caseName, String killLogPattern, String... args) throws Exception {
        File output = new File(OUTPUT_ROOT, caseName);
        FileUtils.deleteDirectory(output);
        File logFile = new File(OUTPUT_ROOT, caseName + "-killed.log");
        Files.deleteIfExists(logFile.toPath());

        // the class path is passed with an argument file, because it can exceed the command line length on Windows
        File argFile = new File(OUTPUT_ROOT, caseName + ".args");
        List<String> javaArgs = new ArrayList<>();
        javaArgs.add("-cp");
        javaArgs.add("\"" + System.getProperty("java.class.path").replace("\\", "\\\\") + "\"");
        javaArgs.add(Mago3DTerrainerMain.class.getName());
        for (String arg : args) {
            javaArgs.add("\"" + arg.replace("\\", "\\\\") + "\"");
        }
        javaArgs.addAll(List.of("-o", "\"" + path(output).replace("\\", "\\\\") + "\"", "-l", "\"" + path(logFile).replace("\\", "\\\\") + "\""));
        Files.createDirectories(OUTPUT_ROOT.toPath());
        Files.write(argFile.toPath(), javaArgs, StandardCharsets.UTF_8);

        String javaExecutable = new File(System.getProperty("java.home"), "bin/java").getAbsolutePath();
        Process process = new ProcessBuilder(javaExecutable, "@" + path(argFile)).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        Pattern pattern = Pattern.compile(killLogPattern);
        long deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10);
        boolean matched = false;
        while (process.isAlive() && System.currentTimeMillis() < deadline) {
            if (logFile.exists() && pattern.matcher(Files.readString(logFile.toPath(), StandardCharsets.UTF_8)).find()) {
                matched = true;
                break;
            }
            Thread.sleep(50);
        }
        process.destroyForcibly().waitFor();
        assertTrue(matched, "the child process must reach the kill point: " + killLogPattern);
        // a completed run deletes its temp folder
        assertTrue(new File(output, "temp").isDirectory(), "the child process must be killed before it finishes");
        return output;
    }
}
