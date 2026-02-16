package com.example.scheduler.controller;

import com.example.scheduler.model.Subitem;
import com.example.scheduler.repository.SubitemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subitems")
@CrossOrigin(origins = "http://localhost:4200")
public class SubitemController {
    
    @Autowired
    private SubitemRepository subitemRepository;
    
    @GetMapping
    public List<Subitem> getAllSubitems(@RequestParam(required = false) Integer projectId) {
        if (projectId != null) {
            return subitemRepository.findByProjectProjectsId(projectId);
        }
        return subitemRepository.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Subitem> getSubitemById(@PathVariable Integer id) {
        return subitemRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/status/{status}")
    public List<Subitem> getSubitemsByStatus(@PathVariable String status) {
        return subitemRepository.findByStatus(status);
    }
    
    @PostMapping
    public Subitem createSubitem(@RequestBody Subitem subitem) {
        return subitemRepository.save(subitem);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Subitem> updateSubitem(@PathVariable Integer id, @RequestBody Subitem subitemDetails) {
        return subitemRepository.findById(id)
                .map(subitem -> {
                    subitem.setSubitemName(subitemDetails.getSubitemName());
                    subitem.setStatus(subitemDetails.getStatus());
                    subitem.setGroup(subitemDetails.getGroup());
                    return ResponseEntity.ok(subitemRepository.save(subitem));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubitem(@PathVariable Integer id) {
        return subitemRepository.findById(id)
                .map(subitem -> {
                    subitemRepository.delete(subitem);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
