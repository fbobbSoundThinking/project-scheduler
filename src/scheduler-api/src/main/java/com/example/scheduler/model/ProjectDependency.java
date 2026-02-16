package com.example.scheduler.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "project_dependencies")
@Data
public class ProjectDependency {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_dependencies_id")
    private Integer projectDependenciesId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predecessor_id", nullable = false)
    @JsonIgnoreProperties({"assignments", "subitems", "hibernateLazyInitializer"})
    private Project predecessor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "successor_id", nullable = false)
    @JsonIgnoreProperties({"assignments", "subitems", "hibernateLazyInitializer"})
    private Project successor;
    
    @Column(name = "dependency_type", length = 20, nullable = false)
    private String dependencyType = "FINISH_TO_START";
}
