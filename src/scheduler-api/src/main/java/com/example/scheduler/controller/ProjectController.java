package com.example.scheduler.controller;

import com.example.scheduler.model.Project;
import com.example.scheduler.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "http://localhost:4200")
public class ProjectController {
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @GetMapping
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }
    
    @GetMapping("/details")
    public List<Project> getAllProjectsWithDetails() {
        // Fetch all projects and manually initialize assignments and subitems
        List<Project> projects = projectRepository.findAll();
        projects.forEach(project -> {
            if (project.getAssignments() != null) {
                project.getAssignments().size(); // Force lazy loading
            }
            if (project.getSubitems() != null) {
                project.getSubitems().size(); // Force lazy loading of subitems
            }
        });
        return projects;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable Integer id) {
        return projectRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/status/{status}")
    public List<Project> getProjectsByStatus(@PathVariable String status) {
        return projectRepository.findByStatusOrderByPriorityDesc(status);
    }
    
     @GetMapping("/group/{group}")
    public List<Project> getProjectsByGroup(@PathVariable String group) {
        return projectRepository.findByGroupOrderByPriorityDesc(group);
    }
    
   @GetMapping("/search")
    public List<Project> searchProjects(@RequestParam String keyword) {
        return projectRepository.findByProjectNameContainingIgnoreCase(keyword);
    }
    
    @PostMapping
    public Project createProject(@RequestBody Project project) {
        return projectRepository.save(project);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable Integer id, @RequestBody Project projectDetails) {
        return projectRepository.findById(id)
                .map(project -> {
                    project.setProjectName(projectDetails.getProjectName());
                    project.setStatus(projectDetails.getStatus());
                    project.setGroup(projectDetails.getGroup());
                    project.setLevel(projectDetails.getLevel());
                    project.setPriority(projectDetails.getPriority());
                    project.setUrgency(projectDetails.getUrgency());
                    return ResponseEntity.ok(projectRepository.save(project));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Integer id) {
        return projectRepository.findById(id)
                .map(project -> {
                    projectRepository.delete(project);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
