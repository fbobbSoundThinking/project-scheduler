package com.example.scheduler.service;

import com.example.scheduler.model.Project;
import com.example.scheduler.model.ProjectDependency;
import com.example.scheduler.repository.ProjectDependencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DependencyGraphServiceTest {

    @Mock(lenient = true)
    private ProjectDependencyRepository dependencyRepository;

    @InjectMocks
    private DependencyGraphService graphService;

    private Project project1;
    private Project project2;
    private Project project3;

    @BeforeEach
    void setUp() {
        project1 = new Project();
        project1.setProjectsId(1);
        project1.setProjectName("Project 1");

        project2 = new Project();
        project2.setProjectsId(2);
        project2.setProjectName("Project 2");

        project3 = new Project();
        project3.setProjectsId(3);
        project3.setProjectName("Project 3");
    }

    @Test
    void wouldCreateCircularDependency_SelfReference_ReturnsTrue() {
        // When
        boolean result = graphService.wouldCreateCircularDependency(1, 1);

        // Then
        assertTrue(result);
    }

    @Test
    void wouldCreateCircularDependency_NoDependencies_ReturnsFalse() {
        // Given
        when(dependencyRepository.findByPredecessorProjectsId(2))
            .thenReturn(new ArrayList<>());

        // When
        boolean result = graphService.wouldCreateCircularDependency(1, 2);

        // Then
        assertFalse(result);
    }

    @Test
    void wouldCreateCircularDependency_DirectCircle_ReturnsTrue() {
        // Given: Existing dependency Project 1 -> Project 2
        ProjectDependency dep1to2 = new ProjectDependency();
        dep1to2.setPredecessor(project1);
        dep1to2.setSuccessor(project2);

        List<ProjectDependency> depsFrom1 = new ArrayList<>();
        depsFrom1.add(dep1to2);

        when(dependencyRepository.findByPredecessorProjectsId(1))
            .thenReturn(depsFrom1);
        when(dependencyRepository.findByPredecessorProjectsId(2))
            .thenReturn(new ArrayList<>());

        // When: Trying to add Project 2 -> Project 1 (would create circle)
        // Algorithm: start at 1 (successor), traverse forward, find 2 (predecessor)
        boolean result = graphService.wouldCreateCircularDependency(2, 1);

        // Then
        assertTrue(result);
    }

    @Test
    void wouldCreateCircularDependency_IndirectCircle_ReturnsTrue() {
        // Given: Existing dependencies Project 1 -> Project 2 -> Project 3
        ProjectDependency dep1to2 = new ProjectDependency();
        dep1to2.setPredecessor(project1);
        dep1to2.setSuccessor(project2);

        ProjectDependency dep2to3 = new ProjectDependency();
        dep2to3.setPredecessor(project2);
        dep2to3.setSuccessor(project3);

        List<ProjectDependency> depsFrom1 = new ArrayList<>();
        depsFrom1.add(dep1to2);

        List<ProjectDependency> depsFrom2 = new ArrayList<>();
        depsFrom2.add(dep2to3);

        when(dependencyRepository.findByPredecessorProjectsId(1))
            .thenReturn(depsFrom1);
        when(dependencyRepository.findByPredecessorProjectsId(2))
            .thenReturn(depsFrom2);
        when(dependencyRepository.findByPredecessorProjectsId(3))
            .thenReturn(new ArrayList<>());

        // When: Trying to add Project 3 -> Project 1 (would create circle via 1->2->3)
        // Algorithm: start at 1 (successor), traverse forward through 2, find 3 (predecessor)
        boolean result = graphService.wouldCreateCircularDependency(3, 1);

        // Then
        assertTrue(result);
    }

    @Test
    void wouldCreateCircularDependency_ValidChain_ReturnsFalse() {
        // Given: Project 1 -> Project 2
        ProjectDependency dep1to2 = new ProjectDependency();
        dep1to2.setPredecessor(project1);
        dep1to2.setSuccessor(project2);

        List<ProjectDependency> depsFrom2 = new ArrayList<>();
        depsFrom2.add(dep1to2);

        when(dependencyRepository.findByPredecessorProjectsId(2))
            .thenReturn(depsFrom2);
        when(dependencyRepository.findByPredecessorProjectsId(1))
            .thenReturn(new ArrayList<>());

        // When: Trying to add Project 2 -> Project 3 (valid chain)
        boolean result = graphService.wouldCreateCircularDependency(2, 3);

        // Then
        assertFalse(result);
    }
}
