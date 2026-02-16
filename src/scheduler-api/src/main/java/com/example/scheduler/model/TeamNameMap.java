package com.example.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "team_name_map")
@Data
public class TeamNameMap {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @Column(name = "monday_team_name", length = 100)
    private String mondayTeamName;
    
    @Column(name = "teams_id")
    private Integer teamsId;
}
