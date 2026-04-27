package com.gaia3d.api.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "terrain_tasks")
public class TerrainTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 基本路径
    private String inputPath;
    private String outputPath;
    
    // 参数控制
    private Integer minDepth;
    private Integer maxDepth;
    private Double intensity;
    private String body;
    private String interpolationType;
    private String outputFormat; // flat or compact
    
    // 布尔选项 (存储为字符或整型，SQLite 支持)
    private Boolean calculateNormals;
    private Boolean generateJson;
    
    // 任务状态
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private String message;
    private Integer progress = 0; // 0-100
    
    // 时间戳
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationSeconds;
    
    // 日志
    @Column(columnDefinition = "TEXT")
    private String logs;
}
