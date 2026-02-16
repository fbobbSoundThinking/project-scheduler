package com.example.scheduler.service;

import com.example.scheduler.model.*;
import com.example.scheduler.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ScenarioService {
    
    @Autowired
    private ScenarioRepository scenarioRepository;
    
    @Autowired
    private ScenarioAssignmentRepository scenarioAssignmentRepository;
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    public Scenario createScenario(String name, String description, String createdBy) {
        Scenario scenario = new Scenario();
        scenario.setName(name);
        scenario.setDescription(description);
        scenario.setCreatedBy(createdBy);
        scenario.setStatus("DRAFT");
        return scenarioRepository.save(scenario);
    }
    
    public Scenario getScenario(Integer scenarioId) {
        return scenarioRepository.findById(scenarioId).orElse(null);
    }
    
    public List<Scenario> listScenarios() {
        return scenarioRepository.findAll();
    }
    
    public void deleteScenario(Integer scenarioId) {
        scenarioRepository.deleteById(scenarioId);
    }
    
    public ScenarioAssignment addChange(
            Integer scenarioId,
            String changeType,
            Integer originalAssignmentId,
            Integer projectId,
            Integer subitemId,
            Integer developerId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            BigDecimal ratio) {
        
        Scenario scenario = scenarioRepository.findById(scenarioId)
            .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + scenarioId));
        
        // For MODIFY/DELETE, check if a change already exists for this assignment
        if ((changeType.equals("MODIFY") || changeType.equals("DELETE")) && originalAssignmentId != null) {
            Optional<ScenarioAssignment> existing = 
                scenarioAssignmentRepository.findByScenarioScenarioIdAndOriginalAssignmentId(scenarioId, originalAssignmentId);
            if (existing.isPresent()) {
                scenarioAssignmentRepository.delete(existing.get());
            }
        }
        
        ScenarioAssignment change = new ScenarioAssignment();
        change.setScenario(scenario);
        change.setChangeType(changeType);
        change.setOriginalAssignmentId(originalAssignmentId);
        
        if (projectId != null) {
            Project project = new Project();
            project.setProjectsId(projectId);
            change.setProject(project);
        }
        
        if (subitemId != null) {
            change.setSubitemId(subitemId);
        }
        
        if (developerId != null) {
            Developer developer = new Developer();
            developer.setDevelopersId(developerId);
            change.setDeveloper(developer);
        }
        
        change.setStartDate(startDate);
        change.setEndDate(endDate);
        change.setRatio(ratio);
        
        return scenarioAssignmentRepository.save(change);
    }
    
    public void removeChange(Integer scenarioAssignmentId) {
        scenarioAssignmentRepository.deleteById(scenarioAssignmentId);
    }
    
    public void applyScenario(Integer scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
            .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + scenarioId));
        
        List<ScenarioAssignment> changes = scenarioAssignmentRepository.findByScenarioScenarioId(scenarioId);
        
        for (ScenarioAssignment change : changes) {
            if ("ADD".equals(change.getChangeType())) {
                // Create a new live assignment
                Assignment newAssignment = new Assignment();
                newAssignment.setProject(change.getProject());
                newAssignment.setDeveloper(change.getDeveloper());
                newAssignment.setStartDate(change.getStartDate());
                newAssignment.setEndDate(change.getEndDate());
                newAssignment.setRatio(change.getRatio());
                assignmentRepository.save(newAssignment);
                
            } else if ("MODIFY".equals(change.getChangeType())) {
                // Update the original live assignment
                Optional<Assignment> original = assignmentRepository.findById(change.getOriginalAssignmentId());
                if (original.isPresent()) {
                    Assignment assignment = original.get();
                    if (change.getProject() != null) assignment.setProject(change.getProject());
                    if (change.getStartDate() != null) assignment.setStartDate(change.getStartDate());
                    if (change.getEndDate() != null) assignment.setEndDate(change.getEndDate());
                    if (change.getRatio() != null) assignment.setRatio(change.getRatio());
                    if (change.getDeveloper() != null) assignment.setDeveloper(change.getDeveloper());
                    assignmentRepository.save(assignment);
                }
                
            } else if ("DELETE".equals(change.getChangeType())) {
                // Delete the original live assignment
                if (change.getOriginalAssignmentId() != null) {
                    assignmentRepository.deleteById(change.getOriginalAssignmentId());
                }
            }
        }
        
        // Mark scenario as APPLIED
        scenario.setStatus("APPLIED");
        scenarioRepository.save(scenario);
    }
}
