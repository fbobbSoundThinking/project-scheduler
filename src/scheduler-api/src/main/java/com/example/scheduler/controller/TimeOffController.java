package com.example.scheduler.controller;

import com.example.scheduler.model.DeveloperTimeOff;
import com.example.scheduler.repository.DeveloperTimeOffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/time-off")
@CrossOrigin(origins = "http://localhost:4200")
public class TimeOffController {
    
    @Autowired
    private DeveloperTimeOffRepository timeOffRepository;
    
    @GetMapping
    public List<DeveloperTimeOff> getAllTimeOff(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        if (from != null && to != null) {
            return timeOffRepository.findByDateRange(from, to);
        }
        return timeOffRepository.findAll();
    }
    
    @GetMapping("/developer/{developerId}")
    public List<DeveloperTimeOff> getTimeOffByDeveloper(@PathVariable Integer developerId) {
        return timeOffRepository.findByDeveloperId(developerId);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<DeveloperTimeOff> getTimeOffById(@PathVariable Integer id) {
        return timeOffRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public DeveloperTimeOff createTimeOff(@RequestBody DeveloperTimeOff timeOff) {
        return timeOffRepository.save(timeOff);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<DeveloperTimeOff> updateTimeOff(
            @PathVariable Integer id, 
            @RequestBody DeveloperTimeOff timeOffDetails) {
        
        return timeOffRepository.findById(id)
                .map(timeOff -> {
                    timeOff.setDeveloper(timeOffDetails.getDeveloper());
                    timeOff.setStartDate(timeOffDetails.getStartDate());
                    timeOff.setEndDate(timeOffDetails.getEndDate());
                    timeOff.setType(timeOffDetails.getType());
                    timeOff.setNote(timeOffDetails.getNote());
                    return ResponseEntity.ok(timeOffRepository.save(timeOff));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTimeOff(@PathVariable Integer id) {
        return timeOffRepository.findById(id)
                .map(timeOff -> {
                    timeOffRepository.delete(timeOff);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
