# Implementation Plan — Capacity Planning Dashboard

## Problem

The WorkloadHeatmap shows **current** per-developer utilization but offers no **forward-looking team-level capacity view**. PMs cannot quickly answer "Can Team X absorb a 200-hour project starting in March?" without mentally aggregating individual developer rows. With 5 teams and 18+ developers across 220+ projects, this is a frequent and manual process.

## Proposed Approach

Add a new `/capacity` route with a team-oriented dashboard that shows available hours per team per week, looking 12 weeks forward. The backend provides the aggregation; the frontend renders a compact summary with drill-down to individual developers.

The existing `CapacityService` (Phase 1) already calculates per-developer weekly hours. This plan extends it to aggregate at the team level and exposes new API endpoints.

---

## 1. Backend — Capacity Aggregation Endpoints

### 1.1 Extend `CapacityService`

Add two methods to the existing `CapacityService`:

**`getTeamCapacity(Integer teamId, LocalDate from, LocalDate to)`**

Returns a list of weekly capacity summaries for the team:

```java
public List<WeekCapacity> getTeamCapacity(Integer teamId, LocalDate from, LocalDate to)
```

Logic:
1. Fetch all developers for the team via `DeveloperRepository.findByTeamsId(teamId)`
2. For each developer, fetch their assignments via `AssignmentRepository.findByDeveloperIdWithDetails(developerId)`
3. For each week in the range, sum `ratio × 32` across all assignments for all team developers
4. Return per-week: `totalCapacityHours` (team size × 32), `assignedHours`, `availableHours`, `utilizationPercent`

**`getAllTeamsCapacity(LocalDate from, LocalDate to)`**

Calls `getTeamCapacity` for each team. Returns a map of team ID → weekly capacity list.

### 1.2 New DTOs

**`WeekCapacity`** (inner class or separate DTO):
- `weekStart` (LocalDate)
- `totalCapacityHours` (BigDecimal) — team members × 32
- `assignedHours` (BigDecimal) — sum of all assignment hours
- `availableHours` (BigDecimal) — capacity minus assigned
- `utilizationPercent` (BigDecimal) — assigned / capacity × 100
- `developerCount` (int) — number of developers on team

**`TeamCapacityResponse`**:
- `teamId` (Integer)
- `teamName` (String)
- `weeks` (List<WeekCapacity>)
- `averageUtilization` (BigDecimal) — across the period
- `developerCount` (int)

### 1.3 New Controller: `CapacityController`

At `/api/capacity`:

| Method | Path | Description |
|--------|------|-------------|
| GET | `/teams?from={date}&to={date}` | All teams' weekly capacity for the date range |
| GET | `/team/{teamId}?from={date}&to={date}` | Single team's weekly capacity |
| GET | `/team/{teamId}/developers?from={date}&to={date}` | Per-developer breakdown within a team |

Default date range: today → 12 weeks forward (if `from`/`to` omitted).

### 1.4 Add Repository Method

Add to `DeveloperRepository`:

```java
List<Developer> findByTeamsId(Integer teamsId);
```

(This may already work via Spring Data naming convention; verify and add `@Query` with `JOIN FETCH` if needed for eager loading.)

### 1.5 Work Items — Backend

- [ ] Add `findByTeamsId` to `DeveloperRepository`
- [ ] Add `WeekCapacity` and `TeamCapacityResponse` DTOs
- [ ] Add `getTeamCapacity()` and `getAllTeamsCapacity()` to `CapacityService`
- [ ] Add per-developer breakdown method to `CapacityService`
- [ ] Create `CapacityController` with 3 endpoints
- [ ] Write unit tests for team capacity aggregation
- [ ] Write unit tests for edge cases (team with no developers, developer with no assignments)

---

## 2. Frontend — Capacity Planning View

### 2.1 New Route

Add `/capacity` route in `app.routes.ts` pointing to a new `CapacityDashboard` component.

### 2.2 New Service: `capacity.service.ts`

In `core/services/`:

```typescript
getTeamsCapacity(from: string, to: string): Observable<TeamCapacityResponse[]>
getTeamCapacity(teamId: number, from: string, to: string): Observable<TeamCapacityResponse>
getTeamDeveloperBreakdown(teamId: number, from: string, to: string): Observable<DeveloperCapacity[]>
```

With corresponding interfaces: `TeamCapacityResponse`, `WeekCapacity`, `DeveloperCapacity`.

### 2.3 New Component: `CapacityDashboard`

Location: `features/capacity/capacity-dashboard/`

**Layout — Three Sections:**

**Section A — Team Summary Cards (top row)**
- One card per team (5 teams)
- Shows: team name, developer count, average utilization % for the selected period
- Color-coded border: green (<70%), yellow (70–90%), red (>90%)
- Clicking a card scrolls to / highlights that team in the grid below

**Section B — Team Capacity Grid (main area)**
- Rows: one per team (collapsed by default)
- Columns: weeks (12 weeks forward from selected start date)
- Cell content: available hours (e.g., "64h avail" or "128h / 192h")
- Cell color: same thresholds as WorkloadHeatmap but inverted (green = lots of availability, red = near capacity)
- Expandable rows: click a team row to show per-developer breakdown beneath it
- Current week highlighted with distinct border

**Section C — Controls (sticky header)**
- Date range selector: start date + number of weeks (default: today + 12 weeks)
- Team filter: checkboxes to show/hide specific teams
- View toggle: "Available Hours" vs "Utilization %" display mode

### 2.4 Expandable Developer Rows

When a team row is expanded:
- Show one sub-row per developer on that team
- Each cell shows the developer's assigned hours / 32h capacity
- Color coding matches WorkloadHeatmap thresholds
- Developer name links to the WorkloadHeatmap filtered to that developer (future enhancement)

### 2.5 "Can We Take This?" Quick Check (Optional Enhancement)

A small form at the top:
- Input: hours needed, start date, duration (weeks)
- Select: preferred team
- Output: highlights which weeks the team has enough available capacity, suggests feasible start dates if the requested dates don't work

### 2.6 Integration with FilterStateService

- The capacity dashboard does NOT share filter state with the project list/Gantt (different concerns)
- Uses its own local state for date range and team selection
- If a user navigates to `/workload` from the dashboard, no state collision

### 2.7 Work Items — Frontend

- [ ] Create `capacity.service.ts` with interfaces and API methods
- [ ] Add `/capacity` route to `app.routes.ts`
- [ ] Create `CapacityDashboard` component (standalone)
- [ ] Implement team summary cards (Section A)
- [ ] Implement team capacity grid with week columns (Section B)
- [ ] Implement expandable developer sub-rows
- [ ] Implement date range and team filter controls (Section C)
- [ ] Add navigation link to the app header/nav bar
- [ ] Style with SCSS following existing patterns (WorkloadHeatmap/Gantt)

---

## 3. Implementation Order

### Phase 1: Backend API

Build and test the backend endpoints first. This allows verification with `curl` before building the UI.

1. Add `findByTeamsId` to `DeveloperRepository`
2. Create DTOs (`WeekCapacity`, `TeamCapacityResponse`)
3. Implement `getTeamCapacity()` in `CapacityService`
4. Create `CapacityController`
5. Write unit tests

### Phase 2: Frontend — Core View

Build the capacity grid — this is the highest-value deliverable.

1. Create `capacity.service.ts`
2. Add route
3. Build `CapacityDashboard` component with team capacity grid
4. Add date range controls
5. Add nav link

### Phase 3: Frontend — Polish

Add the team summary cards, expandable developer rows, and team filters.

1. Team summary cards
2. Expandable developer sub-rows
3. Team filter checkboxes
4. View toggle (hours vs %)

### Phase 4: Quick Check (Optional)

The "Can We Take This?" form. Only implement if Phases 1–3 are stable.

---

## Technical Notes

- **Capacity constant:** 32 hours/week, already defined in `CapacityService` and `WorkloadHeatmap`. The new code reuses the existing constant from `CapacityService`.
- **Architecture pattern:** The new `CapacityController` follows the existing controller pattern (inject service directly, `@CrossOrigin` for localhost:4200). The aggregation logic belongs in `CapacityService` since it's non-trivial business logic.
- **Frontend pattern:** Standalone Angular component (no NgModule). SCSS styling should reuse color variables from WorkloadHeatmap where possible. The component manages its own state (no FilterStateService dependency).
- **Performance:** The `getAllTeamsCapacity` endpoint will query all developers and their assignments. For 18 developers with ~100 assignments each, this is manageable. If it becomes slow, add caching or a materialized view.
- **Testing:** Backend uses JUnit 5 + Mockito (same pattern as `CapacityServiceTest` and `DependencyGraphServiceTest`). Frontend uses Vitest.
- **No schema changes required.** This feature is read-only — it aggregates existing assignment data. No new tables or columns needed.
