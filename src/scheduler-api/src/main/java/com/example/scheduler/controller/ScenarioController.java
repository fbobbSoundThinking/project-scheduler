package com.example.scheduler.controller;

import com.example.scheduler.model.Scenario;
import com.example.scheduler.model.ScenarioAssignment;
import com.example.scheduler.service.ScenarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scenarios")
@CrossOrigin(origins = "http://localhost:4200")
public class ScenarioController {
    
    @Autowired
    private ScenarioService scenarioService;
    
    @GetMapping
    public List<Scenario> listScenarios() {
        return scenarioService.listScenarios();
    }
    
    @PostMapping
    public Scenario createScenario(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String description = body.getOrDefault("description", "");
        String createdBy = body.getOrDefault("createdBy", "");
        return scenarioService.createScenario(name, description, createdBy);
    }
    
    @GetMapping("/{id}")
    public Scenario getScenario(@PathVariable Integer id) {
        return scenarioService.getScenario(id);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteScenario(@PathVariable Integer id) {
        scenarioService.deleteScenario(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/changes")
    public ScenarioAssignment addChange(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> body) {
        
        String changeType = (String) body.get("changeType");
        Integer originalAssignmentId = (Integer) body.get("originalAssignmentId");
        Integer projectId = (Integer) body.get("projectId");
        Integer subitemId = (Integer) body.get("subitemId");
        Integer developerId = (Integer) body.get("developerId");
        String startDateStr = (String) body.get("startDate");
        String endDateStr = (String) body.get("endDate");
        BigDecimal ratio = body.get("ratio") != null 
            ? new BigDecimal(body.get("ratio").toString()) 
            : null;
        
        LocalDate startDate = startDateStr != null ? LocalDate.parse(startDateStr) : null;
        LocalDate endDate = endDateStr != null ? LocalDate.parse(endDateStr) : null;
        
        return scenarioService.addChange(
            id, changeType, originalAssignmentId,
            projectId, subitemId, developerId,
            startDate, endDate, ratio);
    }
    
    @DeleteMapping("/{id}/changes/{changeId}")
    public ResponseEntity<?> removeChange(
            @PathVariable Integer id,
            @PathVariable Integer changeId) {
        scenarioService.removeChange(changeId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/apply")
    public ResponseEntity<?> applyScenario(@PathVariable Integer id) {
        scenarioService.applyScenario(id);
        return ResponseEntity.ok().build();
    }
}
