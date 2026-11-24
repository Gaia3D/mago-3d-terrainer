package com.gaia3d.terrain.tile.generation;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.tile.geotiff.RasterStandardizer;
import com.gaia3d.terrain.types.PriorityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("default")
class RasterPriorityTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        GlobalOptions.recreateInstance();
    }

    private String createRaster(String relativePath) throws IOException {
        Path raster = tempDir.resolve(relativePath);
        Files.createDirectories(raster.getParent());
        Files.write(raster, new byte[0]);
        return raster.toFile().getAbsolutePath();
    }

    @Test
    void resolutionPriorityHasNoRank() throws IOException {
        GlobalOptions.getInstance().setPriorityType(PriorityType.RESOLUTION);
        String raster = createRaster("a.tif");
        RasterPriority priority = RasterPriority.create(List.of(raster));
        assertEquals(RasterPriority.UNRANKED, priority.getRank(raster));
    }

    @Test
    void inputPriorityFollowsTheInputOptionsAndThePathsInADirectory() throws IOException {
        String newSurvey = createRaster("new/survey.tif");
        String oldB = createRaster("old/b.tif");
        String oldA = createRaster("old/a.tif");
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setPriorityType(PriorityType.INPUT);
        globalOptions.setInputPaths(List.of(newSurvey, tempDir.resolve("old").toString()));

        // the directory listing order (b before a) does not matter
        RasterPriority priority = RasterPriority.create(List.of(oldB, newSurvey, oldA));
        assertEquals(0, priority.getRank(newSurvey));
        assertEquals(1, priority.getRank(oldA));
        assertEquals(2, priority.getRank(oldB));
    }

    @Test
    void filePriorityMatchesFilesDirectoriesAndGlobs() throws IOException {
        String survey = createRaster("dem/survey-2025/tile1.tif");
        String base5m = createRaster("dem/base-5m/korea.tif");
        String other = createRaster("dem/other/x.tif");
        String srtm = createRaster("global/srtm_01.tif");
        Path priorityFile = tempDir.resolve("priority.txt");
        Files.writeString(priorityFile, String.join("\n",
                "# the first line first",
                "dem/survey-*/*.tif",
                "",
                tempDir.resolve("dem/base-5m").toString(),
                "global/srtm_01.tif"), StandardCharsets.UTF_8);
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setPriorityType(PriorityType.FILE);
        globalOptions.setPriorityFilePath(priorityFile.toString());

        RasterPriority priority = RasterPriority.create(List.of(other, srtm, base5m, survey));
        assertEquals(0, priority.getRank(survey));
        assertEquals(1, priority.getRank(base5m));
        assertEquals(2, priority.getRank(srtm));
        assertEquals(RasterPriority.UNRANKED, priority.getRank(other));
    }

    @Test
    void tempRastersHaveTheRankOfTheirSource() throws IOException {
        String source = createRaster("a.tif");
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setPriorityType(PriorityType.INPUT);
        globalOptions.setInputPaths(List.of(source));
        RasterPriority priority = RasterPriority.create(List.of(source));

        // temp/resized/{depth}/{source directory}/batch/{raster}
        String sourceDirectory = RasterStandardizer.sourceDirectoryName(source);
        String resized = tempDir.resolve("temp/resized/12/" + sourceDirectory + "/batch-00000-09999/a-0-0.raster").toString();
        assertEquals(0, priority.getRank(resized));
        assertEquals(RasterPriority.UNRANKED, priority.getRank(tempDir.resolve("temp/resized/12/other-1234/a.raster").toString()));
    }

    @Test
    void globSyntax() {
        File base = new File("C:/data");
        RasterPriority.PriorityEntry star = RasterPriority.PriorityEntry.of("dem/*.tif", base);
        RasterPriority.PriorityEntry deep = RasterPriority.PriorityEntry.of("dem/**/*.tif", base);
        String direct = normalized("C:/data/dem/a.tif");
        String nested = normalized("C:/data/dem/x/a.tif");
        assertEquals(true, star.matches(direct));
        assertEquals(false, star.matches(nested));
        assertEquals(true, deep.matches(nested));
    }

    private static String normalized(String path) {
        String normalized = new File(path).getAbsolutePath().replace('\\', '/');
        return File.separatorChar == '\\' ? normalized.toLowerCase() : normalized;
    }
}
