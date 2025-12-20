package com.example.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "projects")
@Data
public class Project {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "projects_id")
    private Integer projectsId;
    
    @Column(name = "project_name", length = 256)
    private String projectName;
    
    @Column(name = "status", length = 45)
    private String status;
    
    @Column(name = "level", length = 45)
    private String level;
    
    @Column(name = "target_prod_date")
    private LocalDate targetProdDate;
    
    @Column(name = "primary_team_id")
    private Integer primaryTeamId;
    
    @Column(name = "web_focus")
    private Byte webFocus;
    
    @Column(name = "priority")
    private Byte priority;
    
    @Column(name = "urgency")
    private Byte urgency;
    
    @Column(name = "dev_hours")
    private Integer devHours;
    
    @Column(name = "wf_hours")
    private Integer wfHours;
    
    @Column(name = "qa_hours")
    private Integer qaHours;
    
    @Column(name = "primary_app_id")
    private Integer primaryAppId;
    
    @Column(name = "primary_app_name", length = 45)
    private String primaryAppName;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "qa_ready_date")
    private LocalDate qaReadyDate;
    
    @Column(name = "itb_number", length = 45)
    private String itbNumber;
    
    @Column(name = "prj_number", length = 45)
    private String prjNumber;
    
    @OneToMany(mappedBy = "project")
    private List<Assignment> assignments;
}
