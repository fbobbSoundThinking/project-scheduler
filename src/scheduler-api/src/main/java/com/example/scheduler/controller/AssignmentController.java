package com.example.scheduler.controller;

import com.example.scheduler.model.Assignment;
import com.example.scheduler.model.Developer;
import com.example.scheduler.model.Project;
import com.example.scheduler.model.Subitem;
import com.example.scheduler.repository.AssignmentRepository;
import com.example.scheduler.repository.DeveloperRepository;
import com.example.scheduler.repository.ProjectRepository;
import com.example.scheduler.repository.SubitemRepository;
import com.example.scheduler.service.CapacityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/assignments")
@CrossOrigin(origins = "http://localhost:4200")
public class AssignmentController {
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private SubitemRepository subitemRepository;
    
    @Autowired
    private DeveloperRepository developerRepository;
    
    @Autowired
    private CapacityService capacityService;
    
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
    public ResponseEntity<?> createAssignment(@RequestBody Map<String, Object> payload) {
        try {
            // Parse IDs safely - they might come as String or Integer from JSON
            Integer projectId = parseInteger(payload.get("projectId"));
            Integer subitemId = parseInteger(payload.get("subitemId"));
            Integer developerId = parseInteger(payload.get("developerId"));
            Double ratioValue = payload.get("ratio") != null ? 
                ((Number) payload.get("ratio")).doubleValue() : 1.0;
            
            // Must have either projectId or subitemId, but not both
            if ((projectId == null && subitemId == null) || (projectId != null && subitemId != null)) {
                return ResponseEntity.badRequest().body("Must provide either projectId or subitemId (not both)");
            }
            
            // Validate project or subitem exists
            if (projectId != null) {
                Optional<Project> projectOpt = projectRepository.findById(projectId);
                if (!projectOpt.isPresent()) {
                    return ResponseEntity.badRequest().body("Project not found");
                }
            }
            
            if (subitemId != null) {
                Optional<Subitem> subitemOpt = subitemRepository.findById(subitemId);
                if (!subitemOpt.isPresent()) {
                    return ResponseEntity.badRequest().body("Subitem not found");
                }
            }
            
            // Validate developer exists
            Optional<Developer> developerOpt = developerRepository.findById(developerId);
            if (!developerOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Developer not found");
            }
            
            // Check if assignment already exists
            if (projectId != null) {
                Optional<Assignment> existingAssignment = assignmentRepository
                    .findByProjectIdAndDeveloperId(projectId, developerId);
                if (existingAssignment.isPresent()) {
                    return ResponseEntity.badRequest().body("Developer already assigned to this project");
                }
            }
            
            if (subitemId != null) {
                Optional<Assignment> existingAssignment = assignmentRepository
                    .findBySubitemSubitemsIdAndDeveloperDevelopersId(subitemId, developerId);
                if (existingAssignment.isPresent()) {
                    return ResponseEntity.badRequest().body("Developer already assigned to this subitem");
                }
            }
            
            // Create new assignment
            Assignment assignment = new Assignment();
            if (projectId != null) {
                Project project = new Project();
                project.setProjectsId(projectId);
                assignment.setProject(project);
            }
            if (subitemId != null) {
                Subitem subitem = new Subitem();
                subitem.setSubitemsId(subitemId);
                assignment.setSubitem(subitem);
            }
            assignment.setDeveloper(developerOpt.get());
            assignment.setRatio(java.math.BigDecimal.valueOf(ratioValue));
            
            // Set dates if provided
            if (payload.get("startDate") != null) {
                assignment.setStartDate(java.time.LocalDate.parse((String) payload.get("startDate")));
            }
            if (payload.get("endDate") != null) {
                assignment.setEndDate(java.time.LocalDate.parse((String) payload.get("endDate")));
            }
            
            Assignment savedAssignment = assignmentRepository.save(assignment);
            
            // Return assignment with details
            return ResponseEntity.ok(assignmentRepository.findById(savedAssignment.getAssignmentsId()).orElse(savedAssignment));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating assignment: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Assignment> updateAssignment(
            @PathVariable Integer id, 
            @RequestBody Assignment assignment) {
        return assignmentRepository.findById(id)
                .map(existingAssignment -> {
                    existingAssignment.setStartDate(assignment.getStartDate());
                    existingAssignment.setEndDate(assignment.getEndDate());
                    if (assignment.getRatio() != null) {
                        existingAssignment.setRatio(assignment.getRatio());
                    }
                    Assignment updated = assignmentRepository.save(existingAssignment);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
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
    
    @GetMapping("/conflict-check")
    public ResponseEntity<?> checkConflict(
            @RequestParam Integer developerId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam Double ratio,
            @RequestParam(required = false) Integer excludeAssignmentId) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            BigDecimal ratioDecimal = BigDecimal.valueOf(ratio);
            
            CapacityService.ConflictCheckResult result = capacityService.checkConflict(
                developerId, start, end, ratioDecimal, excludeAssignmentId
            );
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error checking conflict: " + e.getMessage());
        }
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
