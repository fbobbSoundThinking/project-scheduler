package com.example.scheduler.repository;

import com.example.scheduler.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Integer> {
    
    @Query("SELECT a FROM Assignment a JOIN FETCH a.project JOIN FETCH a.developer")
    List<Assignment> findAllWithDetails();
    
    @Query("SELECT a FROM Assignment a JOIN FETCH a.project JOIN FETCH a.developer WHERE a.developer.developersId = :developerId")
    List<Assignment> findByDeveloperIdWithDetails(Integer developerId);
    
    @Query("SELECT a FROM Assignment a JOIN FETCH a.project JOIN FETCH a.developer WHERE a.project.projectsId = :projectId")
    List<Assignment> findByProjectIdWithDetails(Integer projectId);
}
