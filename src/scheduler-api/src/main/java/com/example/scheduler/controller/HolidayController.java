package com.example.scheduler.controller;

import com.example.scheduler.model.CompanyHoliday;
import com.example.scheduler.repository.CompanyHolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/holidays")
@CrossOrigin(origins = "http://localhost:4200")
public class HolidayController {
    
    @Autowired
    private CompanyHolidayRepository holidayRepository;
    
    @GetMapping
    public List<CompanyHoliday> getAllHolidays(@RequestParam(required = false) Integer year) {
        if (year != null) {
            return holidayRepository.findByYear(year);
        }
        return holidayRepository.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CompanyHoliday> getHolidayById(@PathVariable Integer id) {
        return holidayRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public CompanyHoliday createHoliday(@RequestBody CompanyHoliday holiday) {
        return holidayRepository.save(holiday);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<CompanyHoliday> updateHoliday(
            @PathVariable Integer id, 
            @RequestBody CompanyHoliday holidayDetails) {
        
        return holidayRepository.findById(id)
                .map(holiday -> {
                    holiday.setHolidayDate(holidayDetails.getHolidayDate());
                    holiday.setName(holidayDetails.getName());
                    return ResponseEntity.ok(holidayRepository.save(holiday));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Integer id) {
        return holidayRepository.findById(id)
                .map(holiday -> {
                    holidayRepository.delete(holiday);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
