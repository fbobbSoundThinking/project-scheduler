package com.example.scheduler.controller;

import com.example.scheduler.dto.DeveloperCapacity;
import com.example.scheduler.dto.TeamCapacityResponse;
import com.example.scheduler.service.CapacityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/capacity")
@CrossOrigin(origins = "http://localhost:4200")
public class CapacityController {
    
    @Autowired
    private CapacityService capacityService;
    
    @GetMapping("/teams")
    public ResponseEntity<List<TeamCapacityResponse>> getAllTeamsCapacity(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer scenarioId) {
        
        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now();
        LocalDate toDate = to != null ? LocalDate.parse(to) : fromDate.plusWeeks(12);
        
        List<TeamCapacityResponse> capacity;
        if (scenarioId != null) {
            capacity = capacityService.getAllTeamsCapacityWithScenario(fromDate, toDate, scenarioId);
        } else {
            capacity = capacityService.getAllTeamsCapacity(fromDate, toDate);
        }
        
        return ResponseEntity.ok(capacity);
    }
    
    @GetMapping("/team/{teamId}")
    public ResponseEntity<TeamCapacityResponse> getTeamCapacity(
            @PathVariable Integer teamId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer scenarioId) {
        
        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now();
        LocalDate toDate = to != null ? LocalDate.parse(to) : fromDate.plusWeeks(12);
        
        TeamCapacityResponse capacity;
        if (scenarioId != null) {
            capacity = capacityService.getTeamCapacityWithScenario(teamId, fromDate, toDate, scenarioId);
        } else {
            capacity = capacityService.getTeamCapacity(teamId, fromDate, toDate);
        }
        
        if (capacity == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(capacity);
    }
    
    @GetMapping("/team/{teamId}/developers")
    public ResponseEntity<List<DeveloperCapacity>> getTeamDeveloperBreakdown(
            @PathVariable Integer teamId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer scenarioId) {
        
        LocalDate fromDate = from != null ? LocalDate.parse(from) : LocalDate.now();
        LocalDate toDate = to != null ? LocalDate.parse(to) : fromDate.plusWeeks(12);
        
        List<DeveloperCapacity> breakdown;
        if (scenarioId != null) {
            breakdown = capacityService.getTeamDeveloperBreakdownWithScenario(teamId, fromDate, toDate, scenarioId);
        } else {
            breakdown = capacityService.getTeamDeveloperBreakdown(teamId, fromDate, toDate);
        }
        
        return ResponseEntity.ok(breakdown);
    }
}
