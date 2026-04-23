package com.gaia3d.api.controller;

import com.gaia3d.api.entity.TerrainTask;
import com.gaia3d.api.service.TerrainService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/terrain/data")
@RequiredArgsConstructor
// 移除这里的 @CrossOrigin，统一走全局配置
public class TerrainDataController {

    private final TerrainService terrainService;

    @Operation(summary = "通过任务ID访问切片文件")
    @GetMapping("/{taskId}/**")
    public ResponseEntity<Resource> serveTile(@PathVariable Long taskId, HttpServletRequest request) {
        Optional<TerrainTask> taskOpt = terrainService.getTask(taskId);
        if (taskOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String baseDir = taskOpt.get().getOutputPath();
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String relativePath = new org.springframework.util.AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path);

        File file = new File(baseDir, relativePath);
        
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Resource resource = new FileSystemResource(file);
        HttpHeaders headers = new HttpHeaders();
        
        try {
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".terrain")) {
                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
            } else if (fileName.endsWith(".json")) {
                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            } else {
                String contentType = Files.probeContentType(file.toPath());
                headers.add(HttpHeaders.CONTENT_TYPE, contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
            }
            
            // 移除 headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            // 全局 CorsConfig 已经处理了
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
