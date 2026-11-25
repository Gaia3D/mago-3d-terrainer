package com.gaia3d.terrain.tile.generation;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The temp tiles of one depth in a single file: "L{depth}.pack" holds the serialized tiles appended one after the other,
 * and "L{depth}.idx" the index (tile key, offset, length), written by {@link #writeIndex()}.
 * A tile written again is appended, its previous record stays in the pack as garbage until the depth is deleted.
 * It replaces one file per tile, which is slow to create and delete on NTFS when there are hundreds of thousands of tiles.
 */
@Slf4j
public class TempTilePack {
    private static final int INDEX_MAGIC = 0x4D545450; // "MTTP"
    private static final int INDEX_VERSION = 1;

    private final Path packPath;
    private final Path indexPath;
    private final Map<Long, Location> index = new HashMap<>();
    private FileChannel channel = null;
    private long size = 0;

    private record Location(long offset, int length) {
    }

    private TempTilePack(Path packPath, Path indexPath) {
        this.packPath = packPath;
        this.indexPath = indexPath;
    }

    public static Path getPackPath(String tempPath, int depth) {
        return Path.of(tempPath, "L" + depth + ".pack");
    }

    public static Path getIndexPath(String tempPath, int depth) {
        return Path.of(tempPath, "L" + depth + ".idx");
    }

    /**
     * Returns true if a previous run left the pack of the depth with its index (see {@link #writeIndex()}).
     */
    public static boolean existsOnDisk(String tempPath, int depth) {
        return Files.isRegularFile(getIndexPath(tempPath, depth)) && Files.isRegularFile(getPackPath(tempPath, depth));
    }

    /**
     * Opens the pack of the depth. The index of a previous run is loaded when it exists, otherwise the pack starts empty.
     */
    public static TempTilePack open(String tempPath, int depth) throws IOException {
        TempTilePack pack = new TempTilePack(getPackPath(tempPath, depth), getIndexPath(tempPath, depth));
        if (existsOnDisk(tempPath, depth)) {
            pack.readIndex();
        } else {
            Files.deleteIfExists(pack.packPath);
        }
        return pack;
    }

    private FileChannel channel() throws IOException {
        if (channel == null) {
            Files.createDirectories(packPath.getParent());
            channel = FileChannel.open(packPath, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
            size = Math.max(size, channel.size());
        }
        return channel;
    }

    public boolean contains(long key) {
        return index.containsKey(key);
    }

    public Set<Long> keys() {
        return index.keySet();
    }

    public boolean isEmpty() {
        return index.isEmpty();
    }

    public void write(long key, byte[] bytes) throws IOException {
        FileChannel fileChannel = channel();
        long offset = size;
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long position = offset;
        while (buffer.hasRemaining()) {
            position += fileChannel.write(buffer, position);
        }
        size = position;
        index.put(key, new Location(offset, bytes.length));
    }

    public byte[] read(long key) throws IOException {
        Location location = index.get(key);
        if (location == null) {
            return null;
        }
        FileChannel fileChannel = channel();
        ByteBuffer buffer = ByteBuffer.allocate(location.length());
        long position = location.offset();
        while (buffer.hasRemaining()) {
            int read = fileChannel.read(buffer, position);
            if (read < 0) {
                throw new EOFException("Truncated temp tile pack: " + packPath);
            }
            position += read;
        }
        return buffer.array();
    }

    public void remove(long key) {
        index.remove(key);
    }

    /**
     * Writes the index next to the pack, so a next run ("-continue") can read the tiles.
     */
    public void writeIndex() throws IOException {
        if (channel != null) {
            channel.force(false);
        }
        Files.createDirectories(indexPath.getParent());
        Path writingPath = indexPath.resolveSibling(indexPath.getFileName() + ".tmp");
        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(writingPath)))) {
            output.writeInt(INDEX_MAGIC);
            output.writeInt(INDEX_VERSION);
            output.writeInt(index.size());
            for (Map.Entry<Long, Location> entry : index.entrySet()) {
                output.writeLong(entry.getKey());
                output.writeLong(entry.getValue().offset());
                output.writeInt(entry.getValue().length());
            }
        }
        Files.move(writingPath, indexPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private void readIndex() throws IOException {
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(indexPath)))) {
            if (input.readInt() != INDEX_MAGIC || input.readInt() != INDEX_VERSION) {
                throw new IOException("Unknown temp tile index: " + indexPath);
            }
            int count = input.readInt();
            for (int i = 0; i < count; i++) {
                long key = input.readLong();
                long offset = input.readLong();
                int length = input.readInt();
                index.put(key, new Location(offset, length));
                size = Math.max(size, offset + length);
            }
        }
    }

    public void close() {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                log.warn("Failed to close the temp tile pack: {}", packPath, e);
            }
            channel = null;
        }
    }

    public void delete() {
        close();
        index.clear();
        try {
            Files.deleteIfExists(packPath);
            Files.deleteIfExists(indexPath);
        } catch (IOException e) {
            log.warn("Failed to delete the temp tile pack: {}", packPath, e);
        }
    }
}
