package com.gaia3d.quantized.mesh;

import com.gaia3d.terrain.tile.core.*;
import com.gaia3d.terrain.tile.elevation.*;
import com.gaia3d.terrain.tile.generation.*;
import com.gaia3d.terrain.tile.layer.*;
import com.gaia3d.terrain.tile.mesh.*;

import com.gaia3d.terrain.util.OctNormalCalculator;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
class OctNormalCalculatorTest {

    @Test
    void toShortNormal() {
        Vector2f caseA = OctNormalCalculator.toShortNormal(new Vector2f(0.0f, 0.0f));
        assertEquals(128.0f, caseA.x);
        assertEquals(128.0f, caseA.y);

        Vector2f caseB = OctNormalCalculator.toShortNormal(new Vector2f(0.5f, 0.5f));
        assertEquals(191.0f, caseB.x);
        assertEquals(191.0f, caseB.y);

        Vector2f caseC = OctNormalCalculator.toShortNormal(new Vector2f(-0.5f, -0.5f));
        assertEquals(64.0f, caseC.x);
        assertEquals(64.0f, caseC.y);

        Vector2f caseD = OctNormalCalculator.toShortNormal(new Vector2f(1.0f, 1.0f));
        assertEquals(255.0f, caseD.x);
        assertEquals(255.0f, caseD.y);

        Vector2f caseE = OctNormalCalculator.toShortNormal(new Vector2f(-1.0f, -1.0f));
        assertEquals(0.0f, caseE.x);
        assertEquals(0.0f, caseE.y);

        Vector2f caseF = OctNormalCalculator.toShortNormal(new Vector2f(1.2f, 1.2f));
        assertEquals(255.0f, caseF.x);
        assertEquals(255.0f, caseF.y);
    }

    @Test
    void signNotZero() {
        Vector2f caseA = OctNormalCalculator.signNotZero(new Vector2f(0.0f, 0.0f));
        assertEquals(1.0f, caseA.x);
        assertEquals(1.0f, caseA.y);

        Vector2f caseB = OctNormalCalculator.signNotZero(new Vector2f(0.0f, 0.03f));
        assertEquals(1.0f, caseB.x);
        assertEquals(1.0f, caseB.y);

        Vector2f caseC = OctNormalCalculator.signNotZero(new Vector2f(11.0f, 0.0f));
        assertEquals(1.0f, caseC.x);
        assertEquals(1.0f, caseC.y);

        Vector2f caseD = OctNormalCalculator.signNotZero(new Vector2f(-13.0f, -16.0f));
        assertEquals(-1.0f, caseD.x);
        assertEquals(-1.0f, caseD.y);

        Vector2f caseE = OctNormalCalculator.signNotZero(new Vector2f(-0.01f, -0.01f));
        assertEquals(-1.0f, caseE.x);
        assertEquals(-1.0f, caseE.y);
    }

    @Test
    void clamp() {
        float caseA = OctNormalCalculator.clamp(0.0f, -1.0f, 1.0f);
        assertEquals(0.0f, caseA);

        float caseB = OctNormalCalculator.clamp(-2.0f, -1.0f, 1.0f);
        assertEquals(-1.0f, caseB);

        float caseC = OctNormalCalculator.clamp(2.0f, -1.0f, 1.0f);
        assertEquals(1.0f, caseC);
    }

    @Test
    void encodeOctNormalByte() {
        byte[] caseA = OctNormalCalculator.encodeOctNormalByte(new Vector3f(0.0f, 0.0f, -1.0f));
        assertEquals(-1, caseA[0]);
        assertEquals(-1, caseA[1]);

        byte[] caseB = OctNormalCalculator.encodeOctNormalByte(new Vector3f(0.0f, 0.0f, 1.0f));
        assertEquals(-128, caseB[0]);
        assertEquals(-128, caseB[1]);
    }

    @Test
    void encodeOctNormal() {
        Vector2f caseA = OctNormalCalculator.encodeOctNormal(new Vector3f(0.0f, 0.0f, -1.0f));
        assertEquals(255, caseA.x);
        assertEquals(255, caseA.y);

        Vector2f caseB = OctNormalCalculator.encodeOctNormal(new Vector3f(0.0f, 0.0f, 1.0f));
        assertEquals(128, caseB.x);
        assertEquals(128, caseB.y);
    }
}