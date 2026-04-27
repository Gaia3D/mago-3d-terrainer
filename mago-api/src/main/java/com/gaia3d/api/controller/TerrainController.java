package com.gaia3d.api.controller;

import com.gaia3d.api.dto.TerrainRequestDTO;
import com.gaia3d.api.entity.TerrainTask;
import com.gaia3d.api.service.TerrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    @Operation(summary = "Start terrain process")
    @PostMapping("/process")
    public ResponseEntity<TerrainTask> startProcess(@RequestBody TerrainRequestDTO request) {
        TerrainTask task = terrainService.createTask(request);
        terrainService.executeTask(task.getId(), request);
        return ResponseEntity.accepted().body(task);
    }

    @Operation(summary = "List tasks with pagination")
    @GetMapping("/tasks")
    public ResponseEntity<Page<TerrainTask>> listTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(terrainService.getTasksPage(pageable));
    }

    @Operation(summary = "Get task status")
    @GetMapping("/tasks/{id}")
    public ResponseEntity<TerrainTask> getTask(@PathVariable Long id) {
        return terrainService.getTask(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a task")
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        terrainService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
