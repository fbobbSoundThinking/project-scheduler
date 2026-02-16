package com.example.scheduler.controller;

import com.example.scheduler.model.Developer;
import com.example.scheduler.repository.DeveloperRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/developers")
@CrossOrigin(origins = "http://localhost:4200")
public class DeveloperController {
    
    @Autowired
    private DeveloperRepository developerRepository;
    
    @GetMapping
    public List<Developer> getAllDevelopers() {
        return developerRepository.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Developer> getDeveloperById(@PathVariable Integer id) {
        return developerRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/team/{teamId}")
    public List<Developer> getDevelopersByTeam(@PathVariable Integer teamId) {
        return developerRepository.findByTeamsId(teamId);
    }
}
