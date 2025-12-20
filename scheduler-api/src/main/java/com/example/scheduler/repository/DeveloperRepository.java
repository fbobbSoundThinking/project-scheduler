package com.example.scheduler.repository;

import com.example.scheduler.model.Developer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeveloperRepository extends JpaRepository<Developer, Integer> {
    List<Developer> findByTeamsId(Integer teamsId);
    List<Developer> findByLastNameContainingIgnoreCase(String lastName);
}
