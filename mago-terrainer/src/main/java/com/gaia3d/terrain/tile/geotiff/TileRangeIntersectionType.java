package com.gaia3d.terrain.tile.geotiff;

public enum TileRangeIntersectionType {
    // there are 10 possible cases of intersection:
    //      A_CONTAINS_2_B_0                A_CONTAINS_2_B_1                    PARTIAL_OVERLAP                 A_CONTAINS_1_B_1
    //     +--------------+                 +--------------+                 +---------+----+------+            +--------------+
    //     |              |                 |              |                 |         |    |      |            |              |
    //     |         +----+----+            |         +----+----+            |         |    |      |            |         +----+----+
    //     |    A    |    | B  |            |    A    |    |    |            |    A    |    |  B   |            |    A    |    |    |
    //     |         +----+----+            |         |    |  B |            |         |    |      |            |         |    |    |
    //     |              |                 |         |    |    |            |         |    |      |            |         |    |    |
    //     +--------------+                 +---------+----+----+            +---------+----+------+            +---------+----+  B |
    //                                                                                                                    |         |
    //                                                                                                                    +----+----+
    //
    //     B_CONTAINS_2_A_0                 B_CONTAINS_2_A_1                   A_CONTAINS_B                   B_CONTAINS_A
    //     +--------------+                 +--------------+                 +------------------+            +------------------+
    //     |              |                 |              |                 |        A         |            |        B         |
    //     |         +----+----+            |         +----+----+            |    +--------+    |            |    +--------+    |
    //     |    B    |    | A  |            |    B    |    |    |            |    |   B    |    |            |    |   A    |    |
    //     |         +----+----+            |         |    |  A |            |    |        |    |            |    |        |    |
    //     |              |                 |         |    |    |            |    +--------+    |            |    +--------+    |
    //     +--------------+                 +---------+----+----+            +------------------+            +------------------+
    //
    //     HORIZONTAL_FULL_TOUCHING
    //     +------------++-------------+
    //     |            ||             |
    //     |     A      ||      B      |
    //     |            ||             |
    //     +------------++-------------+
    //
    //     VERTICAL_FULL_TOUCHING
    //     +-------------+
    //     |             |
    //     |      A      |
    //     |             |
    //     +-------------+
    //     +-------------+
    //     |             |
    //     |      B      |
    //     |             |
    //     +-------------+

    // A contains 2 vertexes of B and B no contains any vertex of A (and vice versa)
    // A contains 2 vertexes of B and B contains 1 vertex of A (and vice versa)
    // A contains 2 vertexes of B and B contains 2 vertexes of A (partial overlap)
    // A contains all 4 vertexes of B (and vice versa)
    // Both contains 1 vertex of each other

    UNKNOWN,
    NO_INTERSECTION,
    PARTIAL_OVERLAP,
    A_CONTAINS_B,
    B_CONTAINS_A,
    A_CONTAINS_2_B_0,
    A_CONTAINS_2_B_1,
    B_CONTAINS_2_A_0,
    B_CONTAINS_2_A_1,
    A_CONTAINS_1_B_1,
    HORIZONTAL_FULL_TOUCHING,
    VERTICAL_FULL_TOUCHING;

}
