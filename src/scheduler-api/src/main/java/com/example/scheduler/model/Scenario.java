package com.example.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "scenarios")
@Data
public class Scenario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_id")
    private Integer scenarioId;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "created_at", columnDefinition = "DATETIME2", insertable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", columnDefinition = "DATETIME2", insertable = false, updatable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "status", length = 20)
    private String status;  // DRAFT, APPLIED, ARCHIVED
}
