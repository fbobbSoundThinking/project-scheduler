package com.example.scheduler.repository;

import com.example.scheduler.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {
    List<Project> findByStatus(String status);
    List<Project> findByStatusOrderByPriorityDesc(String status);
    List<Project> findByProjectNameContainingIgnoreCase(String keyword);
}
