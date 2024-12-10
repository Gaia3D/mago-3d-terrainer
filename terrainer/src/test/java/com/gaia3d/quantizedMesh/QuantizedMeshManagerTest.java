package com.gaia3d.quantizedMesh;

import com.gaia3d.command.Configurator;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class QuantizedMeshManagerTest {

    @Test
    void decodeOctNormal() {
        Configurator.initConsoleLogger();
        QuantizedMeshManager manager = new QuantizedMeshManager();
        Vector3f normal = new Vector3f(0.0f, 0.0f, -1.0f);
        Vector2f octEncode = manager.float32x3ToOct(normal);

        log.info("octEncode : {}", octEncode);
        assertEquals(255, octEncode.x);
        assertEquals(255, octEncode.y);

        normal = new Vector3f(0.0f, 0.0f, 1.0f);
        octEncode = manager.float32x3ToOct(normal);

        log.info("octEncode : {}", octEncode);
        assertEquals(128, octEncode.x);
        assertEquals(128, octEncode.y);
    }
}