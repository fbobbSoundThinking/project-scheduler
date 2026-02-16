package com.example.scheduler.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "subitems")
@Data
public class Subitem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subitems_id")
    private Integer subitemsId;
    
    @Column(name = "subitem_id", length = 50, unique = true, nullable = false)
    private String subitemId;  // Monday.com subitem ID
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projects_id", nullable = false)
    @JsonIgnoreProperties({"assignments", "subitems", "hibernateLazyInitializer"})
    private Project project;
    
    @Column(name = "subitem_name", length = 256)
    private String subitemName;
    
    @Column(name = "status", length = 50)
    private String status;
    
    @Column(name = "[group]", length = 50)
    private String group;  // Inherited from parent project
    
    @Column(name = "estimated_days")
    private Integer estimatedDays;
    
    @Column(name = "dev_start_date")
    private LocalDate devStartDate;
    
    @Column(name = "dev_end_date")
    private LocalDate devEndDate;
    
    @Column(name = "qa_start_date")
    private LocalDate qaStartDate;
    
    @Column(name = "qa_end_date")
    private LocalDate qaEndDate;
    
    @Column(name = "target_deployment_date")
    private LocalDate targetDeploymentDate;
    
    @Column(name = "actual_days", precision = 10, scale = 2)
    private BigDecimal actualDays;
    
    @Column(name = "percent_complete")
    private Byte percentComplete;
    
    @OneToMany(mappedBy = "subitem", cascade = CascadeType.ALL)
    private List<Assignment> assignments;
}
