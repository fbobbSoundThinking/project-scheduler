package com.example.scheduler.repository;

import com.example.scheduler.model.CompanyHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CompanyHolidayRepository extends JpaRepository<CompanyHoliday, Integer> {
    
    @Query("SELECT ch FROM CompanyHoliday ch WHERE ch.holidayDate BETWEEN :startDate AND :endDate")
    List<CompanyHoliday> findByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT ch FROM CompanyHoliday ch WHERE YEAR(ch.holidayDate) = :year")
    List<CompanyHoliday> findByYear(@Param("year") Integer year);
}
