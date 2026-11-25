package com.gaia3d.terrain.tile.generation;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@Tag("default")
class TempTilePackTest {
    @TempDir
    Path tempDir;

    @Test
    void writtenTilesAreReadBackAndTheLastWriteWins() throws IOException {
        TempTilePack pack = TempTilePack.open(tempDir.toString(), 3);
        pack.write(1L, new byte[]{1, 2, 3});
        pack.write(2L, new byte[]{4, 5});
        pack.write(1L, new byte[]{9, 9, 9, 9});
        assertArrayEquals(new byte[]{9, 9, 9, 9}, pack.read(1L));
        assertArrayEquals(new byte[]{4, 5}, pack.read(2L));
        assertNull(pack.read(3L));

        pack.remove(2L);
        assertFalse(pack.contains(2L));
        assertTrue(pack.contains(1L));
        pack.close();
    }

    @Test
    void theIndexIsReadByTheNextRun() throws IOException {
        TempTilePack pack = TempTilePack.open(tempDir.toString(), 5);
        pack.write(10L, new byte[]{7, 7});
        pack.write(11L, new byte[]{8});
        pack.writeIndex();
        pack.close();
        assertTrue(TempTilePack.existsOnDisk(tempDir.toString(), 5));

        TempTilePack reopened = TempTilePack.open(tempDir.toString(), 5);
        assertArrayEquals(new byte[]{7, 7}, reopened.read(10L));
        assertArrayEquals(new byte[]{8}, reopened.read(11L));
        // the records appended after the reopening do not overwrite the previous ones
        reopened.write(12L, new byte[]{1, 2, 3});
        assertArrayEquals(new byte[]{7, 7}, reopened.read(10L));
        assertArrayEquals(new byte[]{1, 2, 3}, reopened.read(12L));
        reopened.close();
    }

    @Test
    void aPackWithoutIndexIsDiscarded() throws IOException {
        TempTilePack pack = TempTilePack.open(tempDir.toString(), 7);
        pack.write(1L, new byte[]{1});
        pack.close();
        assertFalse(TempTilePack.existsOnDisk(tempDir.toString(), 7));

        // a pack whose index was never written (e.g. a crash) is not trusted
        TempTilePack reopened = TempTilePack.open(tempDir.toString(), 7);
        assertTrue(reopened.isEmpty());
        reopened.delete();
        assertFalse(Files.exists(TempTilePack.getPackPath(tempDir.toString(), 7)));
    }
}
