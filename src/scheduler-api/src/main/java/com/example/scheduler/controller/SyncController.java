package com.example.scheduler.controller;

import com.example.scheduler.dto.SyncResponse;
import com.example.scheduler.service.ProjectSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sync")
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class SyncController {
    
    @Autowired
    private ProjectSyncService projectSyncService;
    
    @PostMapping("/monday")
    public ResponseEntity<SyncResponse> syncFromMonday() {
        log.info("Received request to sync from Monday.com");
        
        try {
            SyncResponse response = projectSyncService.syncProjectsFromMonday();
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(500).body(response);
            }
            
        } catch (Exception e) {
            log.error("Error during sync", e);
            SyncResponse errorResponse = new SyncResponse();
            errorResponse.setSuccess(false);
            errorResponse.setError(e.getMessage());
            errorResponse.setMessage("Sync failed: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
