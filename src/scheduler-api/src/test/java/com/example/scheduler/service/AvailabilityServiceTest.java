package com.example.scheduler.service;

import com.example.scheduler.model.CompanyHoliday;
import com.example.scheduler.model.Developer;
import com.example.scheduler.model.DeveloperTimeOff;
import com.example.scheduler.repository.CompanyHolidayRepository;
import com.example.scheduler.repository.DeveloperTimeOffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private DeveloperTimeOffRepository timeOffRepository;

    @Mock
    private CompanyHolidayRepository holidayRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    private Developer testDeveloper;

    @BeforeEach
    void setUp() {
        testDeveloper = new Developer();
        testDeveloper.setDevelopersId(1);
        testDeveloper.setFirstName("Test");
        testDeveloper.setLastName("Developer");
    }

    @Test
    void getBusinessDaysOff_NoTimeOff_ReturnsZero() {
        // Given
        Integer developerId = 1;
        LocalDate weekStart = LocalDate.of(2026, 3, 2); // Monday
        LocalDate weekEnd = LocalDate.of(2026, 3, 8);   // Sunday

        when(timeOffRepository.findByDeveloperIdAndDateRange(developerId, weekStart, weekEnd))
            .thenReturn(new ArrayList<>());
        when(holidayRepository.findByDateRange(weekStart, weekEnd))
            .thenReturn(new ArrayList<>());

        // When
        int result = availabilityService.getBusinessDaysOff(developerId, weekStart, weekEnd);

        // Then
        assertEquals(0, result);
    }

    @Test
    void getBusinessDaysOff_FullWeekOff_Returns5Days() {
        // Given
        Integer developerId = 1;
        LocalDate weekStart = LocalDate.of(2026, 3, 2); // Monday
        LocalDate weekEnd = LocalDate.of(2026, 3, 8);   // Sunday

        DeveloperTimeOff timeOff = new DeveloperTimeOff();
        timeOff.setDeveloper(testDeveloper);
        timeOff.setStartDate(weekStart);
        timeOff.setEndDate(weekEnd);
        timeOff.setType("VACATION");

        when(timeOffRepository.findByDeveloperIdAndDateRange(developerId, weekStart, weekEnd))
            .thenReturn(Arrays.asList(timeOff));
        when(holidayRepository.findByDateRange(weekStart, weekEnd))
            .thenReturn(new ArrayList<>());

        // When
        int result = availabilityService.getBusinessDaysOff(developerId, weekStart, weekEnd);

        // Then
        assertEquals(5, result); // Mon-Fri only (weekends excluded)
    }

    @Test
    void getBusinessDaysOff_OneDayOff_Returns1Day() {
        // Given
        Integer developerId = 1;
        LocalDate weekStart = LocalDate.of(2026, 3, 2); // Monday
        LocalDate weekEnd = LocalDate.of(2026, 3, 8);   // Sunday
        LocalDate wednesday = LocalDate.of(2026, 3, 4);

        DeveloperTimeOff timeOff = new DeveloperTimeOff();
        timeOff.setDeveloper(testDeveloper);
        timeOff.setStartDate(wednesday);
        timeOff.setEndDate(wednesday);
        timeOff.setType("SICK");

        when(timeOffRepository.findByDeveloperIdAndDateRange(developerId, weekStart, weekEnd))
            .thenReturn(Arrays.asList(timeOff));
        when(holidayRepository.findByDateRange(weekStart, weekEnd))
            .thenReturn(new ArrayList<>());

        // When
        int result = availabilityService.getBusinessDaysOff(developerId, weekStart, weekEnd);

        // Then
        assertEquals(1, result);
    }

    @Test
    void getBusinessDaysOff_WeekendOnly_Returns0Days() {
        // Given
        Integer developerId = 1;
        LocalDate weekStart = LocalDate.of(2026, 3, 2); // Monday
        LocalDate weekEnd = LocalDate.of(2026, 3, 8);   // Sunday
        LocalDate saturday = LocalDate.of(2026, 3, 7);
        LocalDate sunday = LocalDate.of(2026, 3, 8);

        DeveloperTimeOff timeOff = new DeveloperTimeOff();
        timeOff.setDeveloper(testDeveloper);
        timeOff.setStartDate(saturday);
        timeOff.setEndDate(sunday);
        timeOff.setType("PTO");

        when(timeOffRepository.findByDeveloperIdAndDateRange(developerId, weekStart, weekEnd))
            .thenReturn(Arrays.asList(timeOff));
        when(holidayRepository.findByDateRange(weekStart, weekEnd))
            .thenReturn(new ArrayList<>());

        // When
        int result = availabilityService.getBusinessDaysOff(developerId, weekStart, weekEnd);

        // Then
        assertEquals(0, result); // Weekends don't count
    }

    @Test
    void getBusinessDaysOff_CompanyHoliday_Returns1Day() {
        // Given
        Integer developerId = 1;
        LocalDate weekStart = LocalDate.of(2026, 3, 2); // Monday
        LocalDate weekEnd = LocalDate.of(2026, 3, 8);   // Sunday
        LocalDate friday = LocalDate.of(2026, 3, 6);

        CompanyHoliday holiday = new CompanyHoliday();
        holiday.setHolidayDate(friday);
        holiday.setName("Test Holiday");

        when(timeOffRepository.findByDeveloperIdAndDateRange(developerId, weekStart, weekEnd))
            .thenReturn(new ArrayList<>());
        when(holidayRepository.findByDateRange(weekStart, weekEnd))
            .thenReturn(Arrays.asList(holiday));

        // When
        int result = availabilityService.getBusinessDaysOff(developerId, weekStart, weekEnd);

        // Then
        assertEquals(1, result);
    }

    @Test
    void getBusinessDaysOff_OverlappingTimeOffAndHoliday_Deduplicated() {
        // Given
        Integer developerId = 1;
        LocalDate weekStart = LocalDate.of(2026, 3, 2); // Monday
        LocalDate weekEnd = LocalDate.of(2026, 3, 8);   // Sunday
        LocalDate wednesday = LocalDate.of(2026, 3, 4);

        // Developer has time off including Wednesday
        DeveloperTimeOff timeOff = new DeveloperTimeOff();
        timeOff.setDeveloper(testDeveloper);
        timeOff.setStartDate(LocalDate.of(2026, 3, 3)); // Tuesday
        timeOff.setEndDate(LocalDate.of(2026, 3, 5));   // Thursday
        timeOff.setType("VACATION");

        // Company holiday also on Wednesday
        CompanyHoliday holiday = new CompanyHoliday();
        holiday.setHolidayDate(wednesday);
        holiday.setName("Mid-Week Holiday");

        when(timeOffRepository.findByDeveloperIdAndDateRange(developerId, weekStart, weekEnd))
            .thenReturn(Arrays.asList(timeOff));
        when(holidayRepository.findByDateRange(weekStart, weekEnd))
            .thenReturn(Arrays.asList(holiday));

        // When
        int result = availabilityService.getBusinessDaysOff(developerId, weekStart, weekEnd);

        // Then
        assertEquals(3, result); // Tue, Wed, Thu (Wednesday counted only once)
    }

    @Test
    void getBusinessDaysOff_MultipleTimeOffEntries_Combined() {
        // Given
        Integer developerId = 1;
        LocalDate weekStart = LocalDate.of(2026, 3, 2); // Monday
        LocalDate weekEnd = LocalDate.of(2026, 3, 8);   // Sunday

        // First time-off: Monday-Tuesday
        DeveloperTimeOff timeOff1 = new DeveloperTimeOff();
        timeOff1.setDeveloper(testDeveloper);
        timeOff1.setStartDate(LocalDate.of(2026, 3, 2));
        timeOff1.setEndDate(LocalDate.of(2026, 3, 3));
        timeOff1.setType("SICK");

        // Second time-off: Thursday-Friday
        DeveloperTimeOff timeOff2 = new DeveloperTimeOff();
        timeOff2.setDeveloper(testDeveloper);
        timeOff2.setStartDate(LocalDate.of(2026, 3, 5));
        timeOff2.setEndDate(LocalDate.of(2026, 3, 6));
        timeOff2.setType("PTO");

        when(timeOffRepository.findByDeveloperIdAndDateRange(developerId, weekStart, weekEnd))
            .thenReturn(Arrays.asList(timeOff1, timeOff2));
        when(holidayRepository.findByDateRange(weekStart, weekEnd))
            .thenReturn(new ArrayList<>());

        // When
        int result = availabilityService.getBusinessDaysOff(developerId, weekStart, weekEnd);

        // Then
        assertEquals(4, result); // Mon, Tue, Thu, Fri
    }

    @Test
    void getAdjustedCapacity_NoTimeOff_Returns32Hours() {
        // Given
        Integer developerId = 1;
        LocalDate weekStart = LocalDate.of(2026, 3, 2); // Monday

        when(timeOffRepository.findByDeveloperIdAndDateRange(eq(developerId), any(), any()))
            .thenReturn(new ArrayList<>());
        when(holidayRepository.findByDateRange(any(), any()))
            .thenReturn(new ArrayList<>());

        // When
        BigDecimal result = availabilityService.getAdjustedCapacity(developerId, weekStart);

        // Then
        assertEquals(new BigDecimal("32"), result);
    }

    @Test
    void getAdjustedCapacity_OneDayOff_Returns24Hours() {
        // Given
        Integer developerId = 1;
        LocalDate weekStart = LocalDate.of(2026, 3, 2); // Monday
        LocalDate weekEnd = weekStart.plusDays(6);

        DeveloperTimeOff timeOff = new DeveloperTimeOff();
        timeOff.setDeveloper(testDeveloper);
        timeOff.setStartDate(LocalDate.of(2026, 3, 4)); // Wednesday
        timeOff.setEndDate(LocalDate.of(2026, 3, 4));
        timeOff.setType("SICK");

        when(timeOffRepository.findByDeveloperIdAndDateRange(developerId, weekStart, weekEnd))
            .thenReturn(Arrays.asList(timeOff));
        when(holidayRepository.findByDateRange(weekStart, weekEnd))
            .thenReturn(new ArrayList<>());

        // When
        BigDecimal result = availabilityService.getAdjustedCapacity(developerId, weekStart);

        // Then
        assertEquals(new BigDecimal("24"), result); // 32 - 8 = 24
    }

    @Test
    void getAdjustedCapacity_FullWeekOff_Returns0Hours() {
        // Given
        Integer developerId = 1;
        LocalDate weekStart = LocalDate.of(2026, 3, 2); // Monday
        LocalDate weekEnd = weekStart.plusDays(6);

        DeveloperTimeOff timeOff = new DeveloperTimeOff();
        timeOff.setDeveloper(testDeveloper);
        timeOff.setStartDate(weekStart);
        timeOff.setEndDate(weekEnd);
        timeOff.setType("VACATION");

        when(timeOffRepository.findByDeveloperIdAndDateRange(developerId, weekStart, weekEnd))
            .thenReturn(Arrays.asList(timeOff));
        when(holidayRepository.findByDateRange(weekStart, weekEnd))
            .thenReturn(new ArrayList<>());

        // When
        BigDecimal result = availabilityService.getAdjustedCapacity(developerId, weekStart);

        // Then
        assertEquals(new BigDecimal("0"), result); // 32 - 40 clamped to 0
    }

    @Test
    void getAdjustedCapacity_NullDeveloperId_Returns32Hours() {
        // Given
        LocalDate weekStart = LocalDate.of(2026, 3, 2);

        // When
        BigDecimal result = availabilityService.getAdjustedCapacity(null, weekStart);

        // Then
        assertEquals(new BigDecimal("32"), result);
    }

    @Test
    void getTeamAdjustedCapacity_ThreeDevelopersNoTimeOff_Returns96Hours() {
        // Given
        List<Integer> developerIds = Arrays.asList(1, 2, 3);
        LocalDate weekStart = LocalDate.of(2026, 3, 2);

        when(timeOffRepository.findByDeveloperIdAndDateRange(any(), any(), any()))
            .thenReturn(new ArrayList<>());
        when(holidayRepository.findByDateRange(any(), any()))
            .thenReturn(new ArrayList<>());

        // When
        BigDecimal result = availabilityService.getTeamAdjustedCapacity(developerIds, weekStart);

        // Then
        assertEquals(new BigDecimal("96"), result); // 32 * 3
    }

    @Test
    void getTeamAdjustedCapacity_MixedTimeOff_CorrectTotal() {
        // Given
        List<Integer> developerIds = Arrays.asList(1, 2);
        LocalDate weekStart = LocalDate.of(2026, 3, 2);
        LocalDate weekEnd = weekStart.plusDays(6);

        // Developer 1: 1 day off (24h capacity)
        DeveloperTimeOff timeOff1 = new DeveloperTimeOff();
        Developer dev1 = new Developer();
        dev1.setDevelopersId(1);
        timeOff1.setDeveloper(dev1);
        timeOff1.setStartDate(LocalDate.of(2026, 3, 4));
        timeOff1.setEndDate(LocalDate.of(2026, 3, 4));

        // Developer 2: no time off (32h capacity)
        when(timeOffRepository.findByDeveloperIdAndDateRange(eq(1), any(), any()))
            .thenReturn(Arrays.asList(timeOff1));
        when(timeOffRepository.findByDeveloperIdAndDateRange(eq(2), any(), any()))
            .thenReturn(new ArrayList<>());
        when(holidayRepository.findByDateRange(any(), any()))
            .thenReturn(new ArrayList<>());

        // When
        BigDecimal result = availabilityService.getTeamAdjustedCapacity(developerIds, weekStart);

        // Then
        assertEquals(new BigDecimal("56"), result); // 24 + 32
    }
}
