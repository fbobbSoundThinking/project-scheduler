package com.example.scheduler.repository;

import com.example.scheduler.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {
    List<Project> findByStatus(String status);
    List<Project> findByStatusOrderByPriorityDesc(String status);
    List<Project> findByGroup(String group);
    List<Project> findByGroupOrderByPriorityDesc(String group);
    List<Project> findByProjectNameContainingIgnoreCase(String keyword);
    Optional<Project> findByItemId(String itemId);
}
