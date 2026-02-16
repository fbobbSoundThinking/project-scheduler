package com.example.scheduler.repository;

import com.example.scheduler.model.ScenarioAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScenarioAssignmentRepository extends JpaRepository<ScenarioAssignment, Integer> {
    
    List<ScenarioAssignment> findByScenarioScenarioId(Integer scenarioId);
    
    Optional<ScenarioAssignment> findByScenarioScenarioIdAndOriginalAssignmentId(
        Integer scenarioId, Integer originalAssignmentId);
}
