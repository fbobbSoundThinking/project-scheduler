package com.example.scheduler.repository;

import com.example.scheduler.model.DeveloperTimeOff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DeveloperTimeOffRepository extends JpaRepository<DeveloperTimeOff, Integer> {
    
    @Query("SELECT dto FROM DeveloperTimeOff dto WHERE dto.developer.developersId = :developerId")
    List<DeveloperTimeOff> findByDeveloperId(@Param("developerId") Integer developerId);
    
    @Query("SELECT dto FROM DeveloperTimeOff dto WHERE dto.developer.developersId = :developerId " +
           "AND dto.endDate >= :startDate AND dto.startDate <= :endDate")
    List<DeveloperTimeOff> findByDeveloperIdAndDateRange(
        @Param("developerId") Integer developerId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT dto FROM DeveloperTimeOff dto WHERE dto.endDate >= :startDate AND dto.startDate <= :endDate")
    List<DeveloperTimeOff> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
