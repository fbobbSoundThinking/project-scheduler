package com.example.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "assignments")
@Data
public class Assignment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignments_id")
    private Integer assignmentsId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projects_id")
    @JsonIgnoreProperties({"assignments", "hibernateLazyInitializer"})
    private Project project;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subitems_id")
    @JsonIgnoreProperties({"assignments", "hibernateLazyInitializer"})
    private Subitem subitem;
    
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
