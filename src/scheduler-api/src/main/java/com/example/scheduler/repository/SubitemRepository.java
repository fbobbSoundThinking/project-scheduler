package com.example.scheduler.repository;

import com.example.scheduler.model.Subitem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubitemRepository extends JpaRepository<Subitem, Integer> {
    List<Subitem> findByProjectProjectsId(Integer projectsId);
    Optional<Subitem> findBySubitemId(String subitemId);
    List<Subitem> findByStatus(String status);
    List<Subitem> findBySubitemNameContainingIgnoreCase(String keyword);
}
