package com.example.scheduler.controller;

import com.example.scheduler.model.Assignment;
import com.example.scheduler.repository.AssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@CrossOrigin(origins = "http://localhost:4200")
public class AssignmentController {
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @GetMapping
    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAllWithDetails();
    }
    
    @GetMapping("/developer/{developerId}")
    public List<Assignment> getAssignmentsByDeveloper(@PathVariable Integer developerId) {
        return assignmentRepository.findByDeveloperIdWithDetails(developerId);
    }
    
    @GetMapping("/project/{projectId}")
    public List<Assignment> getAssignmentsByProject(@PathVariable Integer projectId) {
        return assignmentRepository.findByProjectIdWithDetails(projectId);
    }
    
    @PostMapping
    public Assignment createAssignment(@RequestBody Assignment assignment) {
        return assignmentRepository.save(assignment);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Integer id) {
        return assignmentRepository.findById(id)
                .map(assignment -> {
                    assignmentRepository.delete(assignment);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
