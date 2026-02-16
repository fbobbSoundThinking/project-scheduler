package com.example.scheduler.repository;

import com.example.scheduler.model.TeamNameMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamNameMapRepository extends JpaRepository<TeamNameMap, Integer> {
    Optional<TeamNameMap> findByMondayTeamName(String mondayTeamName);
}
