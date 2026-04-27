package com.gaia3d.terrain.tile.writer;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class SqlitePakWriter implements TerrainWriter {
    private final String pakFilePath;
    private Connection connection;
    private final Set<String> createdTables = new HashSet<>();
    private int batchCount = 0;
    private static final int BATCH_SIZE = 200; 
    private static final int PARTITION_SIZE = 256; // 核心：调整为 256x256 规模，约 6.5万槽位，实际存储约 3万条

    public SqlitePakWriter(String pakFilePath) {
        this.pakFilePath = pakFilePath.toLowerCase().endsWith(".pak") ? pakFilePath : pakFilePath + ".pak";
    }

    @Override
    public void init() throws IOException {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + pakFilePath);
            connection.setAutoCommit(true);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode = DELETE");
                stmt.execute("PRAGMA synchronous = NORMAL");
            }
            connection.setAutoCommit(false);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS \"infos\" (" +
                        "  \"minx\" double, \"miny\" double, \"maxx\" double, \"maxy\" double, " +
                        "  \"minlevel\" int, \"maxlevel\" int, \"source\" VARCHAR(255), \"type\" VARCHAR(20), " +
                        "  \"tiletrans\" VARCHAR(20), \"zip\" int, \"cur_level\" int, \"cur_x\" int, \"cur_y\" int, " +
                        "  \"layerjson\" blob, \"contenttype\" VARCHAR(20))");
                ensureTable("blocks");
            }
            connection.commit(); 
        } catch (SQLException e) {
            safeCloseConnection();
            throw new IOException("SQLite Init Error: " + e.getMessage(), e);
        }
    }

    @Override
    public void writeTile(int z, long x, long y, byte[] data) throws IOException {
        String tableName = getTableName(z, x, y);
        try {
            ensureTable(tableName);
            String sql = "INSERT OR REPLACE INTO \"" + tableName + "\" (z, x, y, tile, hm) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, z);
                pstmt.setLong(2, x);
                pstmt.setLong(3, y);
                pstmt.setBytes(4, data);
                pstmt.setBytes(5, null);
                pstmt.executeUpdate();
            }
            if (++batchCount >= BATCH_SIZE) {
                connection.commit();
                batchCount = 0;
            }
        } catch (SQLException e) {
            throw new IOException("Failed to write tile data", e);
        }
    }

    private String getTableName(int z, long x, long y) {
        // 0-9 级归入 blocks 表 (z < 10)
        if (z < 10) return "blocks";
        
        // 10 级及以上按 PARTITION_SIZE x PARTITION_SIZE 分区，且按当前层级 z 分表
        long gridX = (x / PARTITION_SIZE) * PARTITION_SIZE;
        long gridY = (y / PARTITION_SIZE) * PARTITION_SIZE;
        return "blocks_" + z + "_" + gridX + "_" + gridY;
    }

    private void ensureTable(String tableName) throws SQLException {
        if (!createdTables.contains(tableName)) {
            log.info("正在物理创建表: {}", tableName);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS \"" + tableName + "\" (" +
                        "  \"z\" int, \"x\" long, \"y\" long, \"tile\" blob, \"hm\" blob)");
                String indexName = tableName.equals("blocks") ? "blocksindex" : tableName + "index";
                stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS \"" + indexName + "\" ON \"" + tableName + "\" (\"x\" ASC, \"y\" ASC, \"z\" ASC)");
                connection.commit();
            }
            createdTables.add(tableName);
        }
    }

    @Override
    public void writeMetadata(double minX, double minY, double maxX, double maxY, 
                               int minLevel, int maxLevel, String source, String layerJson) throws IOException {
        String sql = "INSERT OR REPLACE INTO \"infos\" (minx, miny, maxx, maxy, minlevel, maxlevel, source, type, tiletrans, zip, layerjson, contenttype) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDouble(1, minX);
            pstmt.setDouble(2, minY);
            pstmt.setDouble(3, maxX);
            pstmt.setDouble(4, maxY);
            pstmt.setInt(5, minLevel);
            pstmt.setInt(6, maxLevel);
            pstmt.setString(7, source);
            pstmt.setString(8, "terrain");
            pstmt.setString(9, "quantized-mesh");
            pstmt.setInt(10, 0);
            pstmt.setBytes(11, layerJson.getBytes(StandardCharsets.UTF_8));
            pstmt.setString(12, "application/json");
            pstmt.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            throw new IOException("Failed to write metadata", e);
        }
    }

    private void safeCloseConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
        connection = null;
    }

    @Override
    public void close() throws IOException {
        if (connection == null) return;
        try {
            log.info("物理合并事务并释放 PAK 锁...");
            connection.commit(); 
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA optimize");
            }
            connection.close();
            log.info("PAK 数据库已安全关闭。");
        } catch (SQLException e) {
            log.error("PAK 关闭失败", e);
        } finally {
            connection = null;
        }
    }
}
