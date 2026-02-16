package com.example.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "scenario_assignments")
@Data
public class ScenarioAssignment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_assignment_id")
    private Integer scenarioAssignmentId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private Scenario scenario;
    
    @Column(name = "change_type", nullable = false, length = 10)
    private String changeType;  // ADD, MODIFY, DELETE
    
    @Column(name = "original_assignment_id")
    private Integer originalAssignmentId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projects_id")
    @JsonIgnoreProperties({"assignments", "hibernateLazyInitializer"})
    private Project project;
    
    @Column(name = "subitems_id")
    private Integer subitemId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developers_id")
    @JsonIgnoreProperties({"assignments", "hibernateLazyInitializer"})
    private Developer developer;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "ratio", precision = 5, scale = 2)
    private BigDecimal ratio;
}
