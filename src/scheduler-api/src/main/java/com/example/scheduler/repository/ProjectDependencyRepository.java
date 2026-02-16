package com.example.scheduler.repository;

import com.example.scheduler.model.ProjectDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectDependencyRepository extends JpaRepository<ProjectDependency, Integer> {
    
    @Query("SELECT pd FROM ProjectDependency pd JOIN FETCH pd.predecessor JOIN FETCH pd.successor WHERE pd.predecessor.projectsId = :predecessorId")
    List<ProjectDependency> findByPredecessorProjectsId(Integer predecessorId);
    
    @Query("SELECT pd FROM ProjectDependency pd JOIN FETCH pd.predecessor JOIN FETCH pd.successor WHERE pd.successor.projectsId = :successorId")
    List<ProjectDependency> findBySuccessorProjectsId(Integer successorId);
    
    @Query("SELECT pd FROM ProjectDependency pd WHERE pd.predecessor.projectsId = :predecessorId AND pd.successor.projectsId = :successorId")
    Optional<ProjectDependency> findByPredecessorProjectsIdAndSuccessorProjectsId(Integer predecessorId, Integer successorId);
    
    @Query("SELECT pd FROM ProjectDependency pd JOIN FETCH pd.predecessor JOIN FETCH pd.successor WHERE pd.predecessor.projectsId = :projectId OR pd.successor.projectsId = :projectId")
    List<ProjectDependency> findAllByProjectId(Integer projectId);
}
