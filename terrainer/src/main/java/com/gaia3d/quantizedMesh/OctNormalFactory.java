package com.gaia3d.quantizedMesh;

import org.joml.Vector2f;
import org.joml.Vector3f;

public class OctNormalFactory {

    /**
     * Encode a normal vector into 2 bytes using oct encoding
     */
    public static Vector2f toShortNormal(Vector2f value) {
        float x = (float) Math.round((clamp(value.x, -1.0f, 1.0f) * 0.5f + 0.5f) * 255.0f);
        float y = (float) Math.round((clamp(value.y, -1.0f, 1.0f) * 0.5f + 0.5f) * 255.0f);
        return new Vector2f(x, y);
    }

    /**
     * Sign function that returns 1 for positive numbers and -1 for negative numbers
     * Only returns (1 for -1)
     */
    public static Vector2f signNotZero(Vector2f value) {
        return new Vector2f(value.x >= 0 ? 1 : -1, value.y >= 0 ? 1 : -1);
    }

    /**
     * Clamp value between min and max
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Encode a normal vector into 2 bytes using oct encoding
     * @return Vector2f
     */
    public static Vector2f encodeOctNormal(Vector3f normal) {
        float x = normal.x;
        float y = normal.y;
        float z = normal.z;

        float den = Math.abs(x) + Math.abs(y) + Math.abs(z);
        float u = x / den;
        float v = y / den;
        Vector2f p = new Vector2f(u, v);

        // Reflect the folds of the lower hemisphere over the diagonals
        if (z <= 0) {
            u = (float) ((1.0 - Math.abs(p.y)) * signNotZero(p).x);
            v = (float) ((1.0 - Math.abs(p.x)) * signNotZero(p).y);
            p = new Vector2f(u, v);
        }
        p = toShortNormal(p);
        return p;
    }

    /**
     * Encode a normal vector into 2 bytes using oct encoding
     * @return 2 bytes
     */
    public static byte[] encodeOctNormalByte(Vector3f normal) {
        Vector2f octNormal = encodeOctNormal(normal);
        return new byte[]{(byte) octNormal.x, (byte) octNormal.y};
    }
}
