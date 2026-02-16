# Developer Availability / Time-Off Tracking — Implementation Plan

## Problem

The system assumes every developer is available 32h/week, every week, with no way to account for vacations, PTO, or company holidays. This causes capacity views (WorkloadHeatmap, CapacityDashboard) to over-schedule developers during time-off periods and produce inaccurate utilization numbers.

## Proposed Approach

Add two new entities — **DeveloperTimeOff** (per-developer absences) and **CompanyHoliday** (org-wide days off that apply to all developers). Both are full-day only. The existing `CapacityService` will be modified to deduct time-off hours from each developer's weekly capacity before calculating utilization. A new Angular route (`/time-off`) provides CRUD management for both types.

### Data Model

**`developer_time_off` table:**
| Column | Type | Notes |
|---|---|---|
| `developer_time_off_id` | INT IDENTITY(1,1) | PK |
| `developers_id` | INT | FK → developers |
| `start_date` | DATE | First day of absence |
| `end_date` | DATE | Last day of absence |
| `type` | NVARCHAR(20) | PTO, VACATION, SICK, OTHER |
| `note` | NVARCHAR(200) | Optional description |

**`company_holidays` table:**
| Column | Type | Notes |
|---|---|---|
| `company_holiday_id` | INT IDENTITY(1,1) | PK |
| `holiday_date` | DATE | Single day |
| `name` | NVARCHAR(100) | e.g., "New Year's Day" |

### Capacity Deduction Logic

For each developer+week:
1. Count **business days off** = company holidays in that week + individual time-off days in that week (deduplicated if overlapping)
2. **Adjusted capacity** = `MAX_HOURS_PER_WEEK - (daysOff × 8)` (clamped to 0 minimum)
3. Use adjusted capacity instead of flat 32h in `CapacityService` methods: `getTeamCapacity()`, `getTeamDeveloperBreakdown()`, `checkConflict()`, and their scenario variants.

---

## Workplan

### Phase 1 — Database & Backend Model

- [ ] Create SQL schema script `src/sql/time_off_schema.sql` with both tables
- [ ] Create `DeveloperTimeOff` JPA entity in `model/`
- [ ] Create `CompanyHoliday` JPA entity in `model/`
- [ ] Create `DeveloperTimeOffRepository` in `repository/`
- [ ] Create `CompanyHolidayRepository` in `repository/`
- [ ] Verify `mvn clean compile` passes

### Phase 2 — REST API

- [ ] Create `TimeOffController` (`/api/time-off`) with CRUD endpoints:
  - `GET /api/time-off/developer/{developerId}` — list time-off for a developer
  - `GET /api/time-off?from=&to=` — list all time-off in a date range
  - `POST /api/time-off` — create time-off entry
  - `PUT /api/time-off/{id}` — update
  - `DELETE /api/time-off/{id}` — delete
- [ ] Create `HolidayController` (`/api/holidays`) with CRUD endpoints:
  - `GET /api/holidays?year=` — list holidays (optional year filter)
  - `POST /api/holidays` — create
  - `PUT /api/holidays/{id}` — update
  - `DELETE /api/holidays/{id}` — delete
- [ ] Verify `mvn clean compile` passes

### Phase 3 — Capacity Service Integration

- [ ] Add `AvailabilityService` (or extend `CapacityService`) with method:
  - `getBusinessDaysOff(developerId, weekStart, weekEnd)` → int count of days off that week
  - `getAdjustedCapacity(developerId, weekStart)` → BigDecimal (32 - daysOff × 8)
  - `getTeamAdjustedCapacity(teamId, weekStart)` → sum of adjusted capacities across team
- [ ] Modify `CapacityService.getTeamCapacity()` to use adjusted capacity per developer instead of flat `32 × developerCount`
- [ ] Modify `CapacityService.getTeamDeveloperBreakdown()` to use adjusted capacity per developer per week
- [ ] Modify `CapacityService.checkConflict()` to compare against adjusted capacity instead of flat 32h
- [ ] Update scenario variants of the above methods similarly
- [ ] Write unit tests for `AvailabilityService` — verify correct deduction for: overlapping time-off + holiday, multi-week spans, weekend exclusion
- [ ] Write unit tests for modified `CapacityService` methods with time-off applied
- [ ] Verify `mvn test` passes

### Phase 4 — Frontend Services

- [ ] Create `TimeOffService` in `core/services/` — CRUD calls to `/api/time-off`
- [ ] Create `HolidayService` in `core/services/` — CRUD calls to `/api/holidays`

### Phase 5 — Frontend Time-Off Management Page

- [ ] Create `TimeOffManagement` component in `features/time-off/time-off-management/`
  - Two sections: "Developer Time Off" and "Company Holidays"
  - Developer Time Off: table with developer name, dates, type, note, edit/delete actions; add form with developer dropdown, date range picker, type selector
  - Company Holidays: simple table with date, name, edit/delete; add form
- [ ] Add `/time-off` route to `app.routes.ts`
- [ ] Add "Time Off" link to the nav bar
- [ ] Verify `ng build` passes

### Phase 6 — Capacity Views Update

- [ ] Update `CapacityDashboard` to show adjusted capacity (no code change needed if backend returns adjusted numbers — verify this)
- [ ] Update `WorkloadHeatmap` to show adjusted capacity per developer per week (it currently calculates client-side, so either:
  - Option A: Refactor to use backend capacity API (preferred — single source of truth), OR
  - Option B: Fetch time-off data and deduct in the client-side calculation)
- [ ] Add visual indicator in capacity views for weeks with time-off (e.g., small icon or badge showing days off)
- [ ] Verify `ng build` passes

### Phase 7 — Verification & Cleanup

- [ ] `mvn clean install` succeeds
- [ ] `ng build` succeeds
- [ ] All existing tests pass
- [ ] Manual verification: capacity numbers change when time-off is added

---

## Notes

- **Weekend handling:** Time-off and holidays only count business days (Mon–Fri). A time-off entry spanning Sat–Sun should not deduct hours.
- **Overlap dedup:** If a company holiday falls within a developer's PTO range, it should only count as 1 day off (not 2).
- **Capacity floor:** Adjusted capacity cannot go below 0h/week.
- **No new dependencies:** Uses existing Spring Data JPA, Angular HttpClient, etc.
- **Schema management:** DDL is `validate` — schema must be applied manually via the SQL script before running the backend.
- **WorkloadHeatmap consideration:** This component currently does capacity math client-side. Phase 6 will need to decide whether to refactor it to use the backend API or to fetch time-off data and deduct locally. Backend API is preferred for consistency.
