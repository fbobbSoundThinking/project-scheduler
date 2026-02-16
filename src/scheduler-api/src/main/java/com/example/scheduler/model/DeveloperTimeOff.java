package com.example.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "developer_time_off")
@Data
public class DeveloperTimeOff {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "developer_time_off_id")
    private Integer developerTimeOffId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developers_id", nullable = false)
    @JsonIgnoreProperties({"assignments", "hibernateLazyInitializer"})
    private Developer developer;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    @Column(name = "type", length = 20, nullable = false)
    private String type;
    
    @Column(name = "note", length = 200)
    private String note;
}
