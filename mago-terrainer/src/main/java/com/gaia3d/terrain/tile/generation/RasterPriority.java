package com.gaia3d.terrain.tile.generation;

import com.gaia3d.command.GlobalOptions;
import com.gaia3d.terrain.tile.geotiff.RasterStandardizer;
import com.gaia3d.terrain.types.PriorityType;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * The priority rank of the input rasters (0 = highest). The overlapping rasters are sorted by (rank, pixel area).
 * <ul>
 *     <li>resolution: no rank, only the resolution is used.</li>
 *     <li>input: the order of the input rasters (the -input options, the files of a directory by path).</li>
 *     <li>file: the order of the lines of the priority file. A line is a raster file, a directory or a glob pattern
 *     (e.g. "D:/dem/survey-2025/*.tif"), relative to the priority file. Empty lines and lines starting with '#' are ignored.</li>
 * </ul>
 * The rasters without a rank come after the ranked rasters.
 * The standardized and resized temp rasters are stored in a folder named after their source raster
 * ({@link RasterStandardizer#sourceDirectoryName(String)}), which is used to find the rank of a temp raster.
 */
@Slf4j
public class RasterPriority {
    public static final int UNRANKED = Integer.MAX_VALUE;
    private static final RasterPriority NONE = new RasterPriority(Map.of());

    private final Map<String, Integer> rankBySourceDirectoryName;

    private RasterPriority(Map<String, Integer> rankBySourceDirectoryName) {
        this.rankBySourceDirectoryName = rankBySourceDirectoryName;
    }

    public static RasterPriority none() {
        return NONE;
    }

    public static RasterPriority create(List<String> sourceRasterPaths) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        PriorityType priorityType = globalOptions.getPriorityType();
        if (priorityType == null || priorityType == PriorityType.RESOLUTION) {
            return NONE;
        }

        Map<String, Integer> ranks = new HashMap<>();
        if (priorityType == PriorityType.INPUT) {
            List<String> orderedPaths = orderByInputOptions(sourceRasterPaths, globalOptions.getInputPaths());
            for (int i = 0; i < orderedPaths.size(); i++) {
                ranks.put(RasterStandardizer.sourceDirectoryName(orderedPaths.get(i)), i);
            }
        } else {
            List<PriorityEntry> entries = readPriorityFile(new File(globalOptions.getPriorityFilePath()));
            Set<Integer> usedEntries = new HashSet<>();
            for (String sourceRasterPath : sourceRasterPaths) {
                int rank = findRank(entries, sourceRasterPath);
                if (rank != UNRANKED) {
                    ranks.put(RasterStandardizer.sourceDirectoryName(sourceRasterPath), rank);
                    usedEntries.add(rank);
                }
            }
            for (int i = 0; i < entries.size(); i++) {
                if (!usedEntries.contains(i)) {
                    log.warn("[Pre][Priority] The priority file entry '{}' matches no input raster.", entries.get(i).text());
                }
            }
        }

        RasterPriority rasterPriority = new RasterPriority(ranks);
        log.info("[Pre][Priority] Raster priority: {}", priorityType.getArgumentName());
        for (String sourceRasterPath : sourceRasterPaths) {
            int rank = rasterPriority.getRankOfSource(sourceRasterPath);
            log.info("[Pre][Priority]   {} : {}", rank == UNRANKED ? "-" : String.valueOf(rank + 1), sourceRasterPath);
        }
        return rasterPriority;
    }

    /**
     * Returns the rank of a raster: a source raster, or a standardized/resized temp raster made from it.
     */
    public int getRank(String rasterPath) {
        if (rankBySourceDirectoryName.isEmpty() || rasterPath == null) {
            return UNRANKED;
        }
        Path path = Path.of(rasterPath);
        for (Path segment : path) {
            Integer rank = rankBySourceDirectoryName.get(segment.toString());
            if (rank != null) {
                return rank;
            }
        }
        return getRankOfSource(rasterPath);
    }

    private int getRankOfSource(String sourceRasterPath) {
        Integer rank = rankBySourceDirectoryName.get(RasterStandardizer.sourceDirectoryName(sourceRasterPath));
        return rank != null ? rank : UNRANKED;
    }

    /**
     * Orders the rasters by the input option that contains them, and by path inside a directory input
     * (the order of the directory listing depends on the file system).
     */
    static List<String> orderByInputOptions(List<String> sourceRasterPaths, List<String> inputPaths) {
        List<String> remaining = new ArrayList<>(sourceRasterPaths);
        List<String> ordered = new ArrayList<>();
        for (String inputPath : inputPaths) {
            String inputPrefix = normalize(new File(inputPath).getAbsolutePath());
            String directoryPrefix = inputPrefix.endsWith("/") ? inputPrefix : inputPrefix + "/";
            List<String> matched = new ArrayList<>();
            for (String sourceRasterPath : remaining) {
                String normalizedPath = normalize(new File(sourceRasterPath).getAbsolutePath());
                if (normalizedPath.equals(inputPrefix) || normalizedPath.startsWith(directoryPrefix)) {
                    matched.add(sourceRasterPath);
                }
            }
            matched.sort(Comparator.comparing(path -> normalize(new File(path).getAbsolutePath())));
            ordered.addAll(matched);
            remaining.removeAll(matched);
        }
        ordered.addAll(remaining);
        return ordered;
    }

    private static int findRank(List<PriorityEntry> entries, String sourceRasterPath) {
        String normalizedPath = normalize(new File(sourceRasterPath).getAbsolutePath());
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).matches(normalizedPath)) {
                return i;
            }
        }
        return UNRANKED;
    }

    static List<PriorityEntry> readPriorityFile(File priorityFile) {
        List<String> lines;
        try {
            lines = Files.readAllLines(priorityFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read the priority file: " + priorityFile.getAbsolutePath(), e);
        }
        File baseDirectory = priorityFile.getAbsoluteFile().getParentFile();
        List<PriorityEntry> entries = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            entries.add(PriorityEntry.of(trimmed, baseDirectory));
        }
        return entries;
    }

    private static String normalize(String path) {
        String normalized = path.replace('\\', '/');
        // the Windows paths are case-insensitive
        return File.separatorChar == '\\' ? normalized.toLowerCase(Locale.ROOT) : normalized;
    }

    /**
     * A line of the priority file: a glob pattern, a directory or a file.
     */
    record PriorityEntry(String text, Pattern globPattern, String directoryPrefix, String filePath) {
        static PriorityEntry of(String entry, File baseDirectory) {
            File file = new File(entry);
            if (!file.isAbsolute()) {
                file = new File(baseDirectory, entry);
            }
            String absolutePath = normalize(file.getAbsolutePath());
            if (entry.contains("*") || entry.contains("?")) {
                return new PriorityEntry(entry, Pattern.compile(globToRegex(absolutePath)), null, null);
            }
            if (file.isDirectory()) {
                return new PriorityEntry(entry, null, absolutePath.endsWith("/") ? absolutePath : absolutePath + "/", null);
            }
            return new PriorityEntry(entry, null, null, absolutePath);
        }

        boolean matches(String normalizedPath) {
            if (globPattern != null) {
                return globPattern.matcher(normalizedPath).matches();
            }
            if (directoryPrefix != null) {
                return normalizedPath.startsWith(directoryPrefix);
            }
            return normalizedPath.equals(filePath);
        }

        /**
         * "**" matches any characters, "*" any characters except '/', "?" one character except '/'.
         */
        private static String globToRegex(String glob) {
            StringBuilder regex = new StringBuilder();
            for (int i = 0; i < glob.length(); i++) {
                char c = glob.charAt(i);
                if (c == '*') {
                    if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                        regex.append(".*");
                        i++;
                    } else {
                        regex.append("[^/]*");
                    }
                } else if (c == '?') {
                    regex.append("[^/]");
                } else {
                    regex.append(Pattern.quote(String.valueOf(c)));
                }
            }
            return regex.toString();
        }
    }
}
