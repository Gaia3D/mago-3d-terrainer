package com.gaia3d.api.controller;

import com.gaia3d.api.entity.TerrainTask;
import com.gaia3d.api.service.TerrainService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/terrain/data")
@RequiredArgsConstructor
public class TerrainDataController {

    private final TerrainService terrainService;
    private static final int PARTITION_SIZE = 256;
    private static final int ROOT_BLOCK_MAX_LEVEL = 10;

    @Operation(summary = "Get terrain data from Flat or Compact storage")
    @GetMapping("/{taskId}/**")
    public ResponseEntity<Resource> serveData(@PathVariable Long taskId, HttpServletRequest request) {
        Optional<TerrainTask> taskOpt = terrainService.getTask(taskId);
        if (taskOpt.isEmpty()) return ResponseEntity.notFound().build();
        
        TerrainTask task = taskOpt.get();
        String outputPath = task.getOutputPath();
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String relativePath = new org.springframework.util.AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path);

        if ("compact".equalsIgnoreCase(task.getOutputFormat())) {
            File pakFile = new File(outputPath.endsWith(".pak") ? outputPath : outputPath + ".pak");
            return serveFromPak(pakFile, relativePath);
        } else {
            return serveFromFileSystem(new File(outputPath), relativePath);
        }
    }

    private ResponseEntity<Resource> serveFromPak(File pakFile, String relativePath) {
        if (!pakFile.exists()) {
            log.error("PAK file not found: {}", pakFile.getAbsolutePath());
            return ResponseEntity.notFound().build();
        }

        String url = "jdbc:sqlite:" + pakFile.getAbsolutePath();
        org.sqlite.SQLiteConfig config = new org.sqlite.SQLiteConfig();
        config.setReadOnly(true);
        config.setLockingMode(org.sqlite.SQLiteConfig.LockingMode.NORMAL);
        
        // 自动管理连接关闭
        try (Connection conn = config.createConnection(url)) {
            if (relativePath.equalsIgnoreCase("layer.json")) {
                return getInfoBlob(conn);
            } else {
                String[] parts = relativePath.replace(".terrain", "").split("/");
                if (parts.length == 3) {
                    int z = Integer.parseInt(parts[0]);
                    long x = Long.parseLong(parts[1]);
                    long y = Long.parseLong(parts[2]);
                    return getTileBlob(conn, z, x, y);
                }
            }
        } catch (Exception e) {
            log.error("Error accessing PAK file (Task probably still running or locked): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity.notFound().build();
    }

    private ResponseEntity<Resource> getInfoBlob(Connection conn) throws SQLException {
        String sql = "SELECT layerjson FROM infos LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(sql); 
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                byte[] data = rs.getBytes("layerjson");
                return createResponse(data, MediaType.APPLICATION_JSON_VALUE);
            }
        }
        return ResponseEntity.notFound().build();
    }

    private ResponseEntity<Resource> getTileBlob(Connection conn, int z, long x, long y) throws SQLException {
        String tableName;
        if (z < ROOT_BLOCK_MAX_LEVEL) {
            tableName = "blocks";
        } else {
            long gridX = (x / PARTITION_SIZE) * PARTITION_SIZE;
            long gridY = (y / PARTITION_SIZE) * PARTITION_SIZE;
            tableName = "blocks_" + z + "_" + gridX + "_" + gridY;
        }

        // 检查表是否存在，确保 ResultSet 及时关闭
        DatabaseMetaData dbm = conn.getMetaData();
        try (ResultSet tables = dbm.getTables(null, null, tableName, null)) {
            if (!tables.next()) return ResponseEntity.notFound().build();
        }

        String sql = "SELECT tile FROM \"" + tableName + "\" WHERE z = ? AND x = ? AND y = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, z);
            pstmt.setLong(2, x);
            pstmt.setLong(3, y);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    byte[] data = rs.getBytes("tile");
                    return createResponse(data, "application/octet-stream");
                }
            }
        }
        return ResponseEntity.notFound().build();
    }

    private ResponseEntity<Resource> createResponse(byte[] data, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .contentLength(data.length)
                .body(new ByteArrayResource(data));
    }

    private ResponseEntity<Resource> serveFromFileSystem(File baseDir, String relativePath) {
        File file = new File(baseDir, relativePath);
        if (!file.exists() || !file.isFile()) return ResponseEntity.notFound().build();
        try {
            String contentType = Files.probeContentType(file.toPath());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                    .body(new FileSystemResource(file));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
