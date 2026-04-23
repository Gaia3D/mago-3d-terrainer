package com.gaia3d.api.repository;

import com.gaia3d.api.entity.TerrainTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TerrainTaskRepository extends JpaRepository<TerrainTask, Long> {
}
