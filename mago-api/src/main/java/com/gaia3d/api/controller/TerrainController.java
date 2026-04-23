package com.gaia3d.api.controller;

import com.gaia3d.api.dto.TerrainRequestDTO;
import com.gaia3d.api.entity.TerrainTask;
import com.gaia3d.api.service.TerrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Terrain API", description = "Quantized-mesh terrain generation operations")
@RestController
@RequestMapping("/api/v1/terrain")
@RequiredArgsConstructor
@Slf4j
public class TerrainController {

    private final TerrainService terrainService;

    @Operation(summary = "Start terrain process", description = "Asynchronously executes the terrain tiling process.")
    @PostMapping("/process")
    public ResponseEntity<TerrainTask> startProcess(@RequestBody TerrainRequestDTO request) {
        TerrainTask task = terrainService.createTask(request);
        terrainService.executeTask(task.getId(), request);
        return ResponseEntity.accepted().body(task);
    }

    @Operation(summary = "List all tasks")
    @GetMapping("/tasks")
    public ResponseEntity<List<TerrainTask>> listTasks() {
        return ResponseEntity.ok(terrainService.getAllTasks());
    }

    @Operation(summary = "Get task status")
    @GetMapping("/tasks/{id}")
    public ResponseEntity<TerrainTask> getTask(@PathVariable Long id) {
        return terrainService.getTask(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
