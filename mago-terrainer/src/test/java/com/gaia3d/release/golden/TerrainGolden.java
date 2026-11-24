package com.gaia3d.release.golden;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Compares a terrain output folder with a golden manifest (relative path -> SHA-256 of the tile).
 * The tiles are hashed in a canonical form: the order of the edge vertex indices is not meaningful, so the edge lists are sorted.
 * Run the tests with "-PupdateGolden" to (re)write the golden manifests.
 */
public class TerrainGolden {
    public static final File GOLDEN_DIR = new File("src/test/resources/golden");
    private static final boolean UPDATE = Boolean.parseBoolean(System.getProperty("mago.golden.update", "false"));
    private static final int MAX_REPORTED_DIFFS = 20;

    public static void assertMatches(String caseName, File outputDir) throws IOException {
        assertMatches(caseName, outputDir, true);
    }

    /**
     * @param updatable false if the output is checked against the golden of another case, so it must never rewrite that golden
     */
    public static void assertMatches(String caseName, File outputDir, boolean updatable) throws IOException {
        SortedMap<String, String> actual = buildManifest(outputDir);
        if (actual.isEmpty()) {
            fail("[" + caseName + "] No output tiles in " + outputDir);
        }

        File goldenFile = new File(GOLDEN_DIR, caseName + ".manifest");
        if (UPDATE && updatable) {
            writeManifest(goldenFile, actual);
            return;
        }
        if (!goldenFile.exists()) {
            fail("[" + caseName + "] No golden manifest: " + goldenFile + ". Run with -PupdateGolden to create it.");
        }

        SortedMap<String, String> expected = readManifest(goldenFile);
        List<String> missing = new ArrayList<>();
        List<String> different = new ArrayList<>();
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String actualHash = actual.get(entry.getKey());
            if (actualHash == null) {
                missing.add(entry.getKey());
            } else if (!actualHash.equals(entry.getValue())) {
                different.add(entry.getKey());
            }
        }
        List<String> extra = actual.keySet().stream().filter(path -> !expected.containsKey(path)).toList();

        if (!missing.isEmpty() || !different.isEmpty() || !extra.isEmpty()) {
            fail(String.format("[%s] Output does not match the golden manifest (expected %d files, actual %d files)%n" +
                            "  missing %d: %s%n  different %d: %s%n  extra %d: %s",
                    caseName, expected.size(), actual.size(),
                    missing.size(), head(missing), different.size(), head(different), extra.size(), head(extra)));
        }
    }

    /**
     * Checks that the output has the same files as the golden of the case, whatever their contents.
     */
    public static void assertSameFiles(String caseName, File outputDir) throws IOException {
        File goldenFile = new File(GOLDEN_DIR, caseName + ".manifest");
        Set<String> expected = readManifest(goldenFile).keySet();
        Set<String> actual = buildManifest(outputDir).keySet();
        List<String> missing = expected.stream().filter(path -> !actual.contains(path)).toList();
        List<String> extra = actual.stream().filter(path -> !expected.contains(path)).toList();
        if (!missing.isEmpty() || !extra.isEmpty()) {
            fail(String.format("[%s] Output files differ from the golden manifest%n  missing %d: %s%n  extra %d: %s",
                    caseName, missing.size(), head(missing), extra.size(), head(extra)));
        }
    }

    public static SortedMap<String, String> buildManifest(File outputDir) throws IOException {
        SortedMap<String, String> manifest = new TreeMap<>();
        Path root = outputDir.toPath();
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> files = paths.filter(Files::isRegularFile).toList();
            for (Path file : files) {
                String relativePath = root.relativize(file).toString().replace(File.separatorChar, '/');
                if (relativePath.endsWith(".terrain")) {
                    manifest.put(relativePath, sha256(canonicalizeTerrain(Files.readAllBytes(file))));
                } else if (relativePath.equals("layer.json")) {
                    manifest.put(relativePath, sha256(Files.readAllBytes(file)));
                }
            }
        }
        return manifest;
    }

    /**
     * Returns the quantized-mesh bytes with the 4 edge index lists sorted.
     */
    public static byte[] canonicalizeTerrain(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(88); // header
        int vertexCount = buffer.getInt();
        buffer.position(buffer.position() + vertexCount * 2 * 3); // u, v, height
        boolean is32Bit = vertexCount > 65536;
        int indexSize = is32Bit ? 4 : 2;
        int alignment = buffer.position() % indexSize;
        if (alignment != 0) {
            buffer.position(buffer.position() + indexSize - alignment);
        }
        int triangleCount = buffer.getInt();
        buffer.position(buffer.position() + triangleCount * 3 * indexSize);

        for (int edge = 0; edge < 4; edge++) {
            int count = buffer.getInt();
            int start = buffer.position();
            long[] indices = new long[count];
            for (int i = 0; i < count; i++) {
                indices[i] = is32Bit ? Integer.toUnsignedLong(buffer.getInt()) : Short.toUnsignedInt(buffer.getShort());
            }
            Arrays.sort(indices);
            buffer.position(start);
            for (long index : indices) {
                if (is32Bit) {
                    buffer.putInt((int) index);
                } else {
                    buffer.putShort((short) index);
                }
            }
        }
        return bytes;
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeManifest(File file, SortedMap<String, String> manifest) throws IOException {
        Files.createDirectories(file.getParentFile().toPath());
        StringBuilder builder = new StringBuilder();
        manifest.forEach((path, hash) -> builder.append(path).append(' ').append(hash).append('\n'));
        Files.writeString(file.toPath(), builder.toString(), StandardCharsets.UTF_8);
    }

    private static SortedMap<String, String> readManifest(File file) throws IOException {
        SortedMap<String, String> manifest = new TreeMap<>();
        for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            int separator = line.lastIndexOf(' ');
            manifest.put(line.substring(0, separator), line.substring(separator + 1));
        }
        return manifest;
    }

    private static String head(List<String> paths) {
        if (paths.size() <= MAX_REPORTED_DIFFS) {
            return paths.toString();
        }
        return paths.subList(0, MAX_REPORTED_DIFFS) + " ...";
    }
}
