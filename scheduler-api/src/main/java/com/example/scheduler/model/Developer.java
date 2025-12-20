package com.example.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "developers")
@Data
public class Developer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "developers_id")
    private Integer developersId;
    
    @Column(name = "first_name", length = 45)
    private String firstName;
    
    @Column(name = "last_name", length = 45)
    private String lastName;
    
    @Column(name = "teams_id")
    private Integer teamsId;
    
    @Column(name = "position", length = 45)
    private String position;
    
    @OneToMany(mappedBy = "developer")
    private List<Assignment> assignments;
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
