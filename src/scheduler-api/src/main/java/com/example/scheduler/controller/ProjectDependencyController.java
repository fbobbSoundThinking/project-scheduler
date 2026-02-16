package com.example.scheduler.controller;

import com.example.scheduler.model.Project;
import com.example.scheduler.model.ProjectDependency;
import com.example.scheduler.repository.ProjectDependencyRepository;
import com.example.scheduler.repository.ProjectRepository;
import com.example.scheduler.service.DependencyGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/dependencies")
@CrossOrigin(origins = "http://localhost:4200")
public class ProjectDependencyController {
    
    @Autowired
    private ProjectDependencyRepository dependencyRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private DependencyGraphService graphService;
    
    @GetMapping("/project/{projectId}")
    public List<ProjectDependency> getDependenciesForProject(@PathVariable Integer projectId) {
        return dependencyRepository.findAllByProjectId(projectId);
    }
    
    @PostMapping
    public ResponseEntity<?> createDependency(@RequestBody Map<String, Object> payload) {
        try {
            Integer predecessorId = parseInteger(payload.get("predecessorId"));
            Integer successorId = parseInteger(payload.get("successorId"));
            String dependencyType = (String) payload.getOrDefault("dependencyType", "FINISH_TO_START");
            
            if (predecessorId == null || successorId == null) {
                return ResponseEntity.badRequest().body("predecessorId and successorId are required");
            }
            
            // Validate projects exist
            Optional<Project> predecessorOpt = projectRepository.findById(predecessorId);
            Optional<Project> successorOpt = projectRepository.findById(successorId);
            
            if (!predecessorOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Predecessor project not found");
            }
            if (!successorOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Successor project not found");
            }
            
            // Check for self-dependency
            if (predecessorId.equals(successorId)) {
                return ResponseEntity.badRequest().body("A project cannot depend on itself");
            }
            
            // Check for duplicate
            Optional<ProjectDependency> existing = dependencyRepository
                .findByPredecessorProjectsIdAndSuccessorProjectsId(predecessorId, successorId);
            if (existing.isPresent()) {
                return ResponseEntity.badRequest().body("This dependency already exists");
            }
            
            // Check for circular dependency
            if (graphService.wouldCreateCircularDependency(predecessorId, successorId)) {
                return ResponseEntity.badRequest().body("This dependency would create a circular reference");
            }
            
            // Create dependency
            ProjectDependency dependency = new ProjectDependency();
            dependency.setPredecessor(predecessorOpt.get());
            dependency.setSuccessor(successorOpt.get());
            dependency.setDependencyType(dependencyType);
            
            ProjectDependency saved = dependencyRepository.save(dependency);
            
            return ResponseEntity.ok(saved);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating dependency: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDependency(@PathVariable Integer id) {
        return dependencyRepository.findById(id)
            .map(dependency -> {
                dependencyRepository.delete(dependency);
                return ResponseEntity.ok().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        throw new IllegalArgumentException("Cannot parse integer from: " + value);
    }
}
