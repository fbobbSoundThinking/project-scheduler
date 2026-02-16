# Implementation Plan — Top 3 Scheduler Improvements

This document details the implementation plan for the three highest-priority improvements to the scheduler application:

1. **Project Dependencies & Critical Path**
2. **Progress Tracking (Actual vs. Planned)**
3. **Assignment Conflict Warnings**

---

## 1. Project Dependencies & Critical Path

**Goal:** Allow PMs to define predecessor/successor relationships between projects so that scheduling conflicts and critical paths are visible.

### 1.1 Database Changes

Create a new `project_dependencies` table. Schema is managed externally (Hibernate `ddl-auto=validate`), so a SQL script must be run manually.

**SQL Script** (`src/project_dependencies_schema.sql`):

```sql
CREATE TABLE project_dependencies (
    project_dependencies_id INT IDENTITY(1,1) PRIMARY KEY,
    predecessor_id          INT NOT NULL,
    successor_id            INT NOT NULL,
    dependency_type         VARCHAR(20) NOT NULL DEFAULT 'FINISH_TO_START',
    CONSTRAINT FK_dep_predecessor FOREIGN KEY (predecessor_id) REFERENCES projects(projects_id),
    CONSTRAINT FK_dep_successor   FOREIGN KEY (successor_id) REFERENCES projects(projects_id),
    CONSTRAINT UQ_dependency      UNIQUE (predecessor_id, successor_id),
    CONSTRAINT CHK_no_self_dep    CHECK (predecessor_id <> successor_id)
);
```

Dependency types: `FINISH_TO_START` (default — most common), `START_TO_START`, `FINISH_TO_FINISH`, `START_TO_FINISH`.

### 1.2 Backend — Entity, Repository, Controller

**New Entity** (`model/ProjectDependency.java`):

- Fields: `projectDependenciesId` (Integer, PK, Identity), `predecessor` (ManyToOne → Project), `successor` (ManyToOne → Project), `dependencyType` (String, default `FINISH_TO_START`)
- Follow existing patterns: `@Data`, `@Entity`, `@Table`

**New Repository** (`repository/ProjectDependencyRepository.java`):

- `findByPredecessorProjectsId(Integer predecessorId)` — get all successors of a project
- `findBySuccessorProjectsId(Integer successorId)` — get all predecessors of a project
- `findByPredecessorProjectsIdAndSuccessorProjectsId(Integer, Integer)` — duplicate check
- Custom `@Query` with `JOIN FETCH` for eager loading project details

**New Controller** (`controller/ProjectDependencyController.java` at `/api/dependencies`):

- `GET /project/{projectId}` — get all dependencies for a project (as predecessor and successor)
- `POST /` — create dependency (validate no circular refs, no duplicates)
- `DELETE /{id}` — remove dependency

**Circular Dependency Check:** On POST, traverse the dependency graph from successor to see if predecessor is reachable. If so, reject with 400 error.

### 1.3 Frontend — Dependency Management UI

**ProjectList Component Updates:**

- Add a "Dependencies" column or expandable section per project
- "Add Dependency" button opens a dropdown to select a predecessor project
- Display dependency badges/chips showing predecessor project names
- Delete dependency via (x) button on each chip

**New Service** (`dependency.service.ts`):

- `getDependencies(projectId)` → GET `/api/dependencies/project/{projectId}`
- `createDependency(predecessorId, successorId, type)` → POST `/api/dependencies`
- `deleteDependency(id)` → DELETE `/api/dependencies/{id}`

**GanttView Updates:**

- Draw dependency arrows between project bars (SVG lines from end of predecessor to start of successor)
- Highlight conflicts: if a successor's start date is before its predecessor's end date, show the arrow in red
- This is the highest-value visual payoff for this feature

### 1.4 Work Items

- [ ] Write and run `project_dependencies_schema.sql`
- [ ] Create `ProjectDependency` entity
- [ ] Create `ProjectDependencyRepository`
- [ ] Create `ProjectDependencyController` with circular dependency validation
- [ ] Create `dependency.service.ts` in frontend
- [ ] Add dependency management UI to ProjectList
- [ ] Draw dependency arrows on Gantt chart
- [ ] Highlight scheduling conflicts (successor starts before predecessor ends)
- [ ] Write backend unit tests for circular dependency detection
- [ ] Write backend unit tests for CRUD operations

---

## 2. Progress Tracking (Actual vs. Planned)

**Goal:** Track actual effort and completion percentage so PMs can detect slippage and calibrate future estimates.

### 2.1 Database Changes

Add columns to existing tables. Run manually.

**SQL Script** (`src/progress_tracking_schema.sql`):

```sql
-- Project-level progress
ALTER TABLE projects ADD actual_dev_hours   DECIMAL(10,2) NULL;
ALTER TABLE projects ADD actual_wf_hours    DECIMAL(10,2) NULL;
ALTER TABLE projects ADD actual_qa_hours    DECIMAL(10,2) NULL;
ALTER TABLE projects ADD percent_complete   TINYINT NULL DEFAULT 0;
ALTER TABLE projects ADD actual_start_date  DATE NULL;
ALTER TABLE projects ADD actual_end_date    DATE NULL;

-- Subitem-level progress
ALTER TABLE subitems ADD actual_days        DECIMAL(10,2) NULL;
ALTER TABLE subitems ADD percent_complete   TINYINT NULL DEFAULT 0;
```

### 2.2 Backend — Entity Updates

**Project Entity** — add fields:

- `actualDevHours` (BigDecimal)
- `actualWfHours` (BigDecimal)
- `actualQaHours` (BigDecimal)
- `percentComplete` (Integer, 0–100)
- `actualStartDate` (LocalDate)
- `actualEndDate` (LocalDate)

**Subitem Entity** — add fields:

- `actualDays` (BigDecimal)
- `percentComplete` (Integer, 0–100)

**ProjectController PUT** — update the existing `updateProject` method to handle new fields in the request body.

**New Analytics Endpoint** (`ProjectController` or new `ReportController`):

- `GET /api/projects/slippage` — return projects where `actualDevHours > devHours` or `actualEndDate > targetProdDate`, sorted by severity
- `GET /api/projects/velocity` — return estimated vs. actual hours ratios for completed projects (for calibration)

### 2.3 Frontend — Progress UI

**ProjectList Updates:**

- Add `% Complete` column with inline-editable progress bar or number input
- Add `Actual Hours` column (dev/wf/qa) with inline editing
- Color-code: green if on track, yellow if actual > 80% of estimate with < 80% complete, red if over estimate

**New Slippage Indicator:**

- Small icon/badge on each project row showing slippage status
- Tooltip with details: "Estimated 40h dev, actual 52h (130%)"

**Optional — Progress Summary Panel:**

- Show at top of ProjectList: total projects on track / at risk / over budget
- Filter to only at-risk projects with one click

### 2.4 Work Items

- [ ] Write and run `progress_tracking_schema.sql`
- [ ] Add new fields to `Project` entity
- [ ] Add new fields to `Subitem` entity
- [ ] Update `ProjectController.updateProject()` to handle new fields
- [ ] Update `SubitemController.updateSubitem()` to handle new fields
- [ ] Add slippage/velocity API endpoints
- [ ] Add `% Complete` column to ProjectList with inline editing
- [ ] Add `Actual Hours` columns to ProjectList with inline editing
- [ ] Add slippage indicator badges to project rows
- [ ] Write backend tests for new endpoints

---

## 3. Assignment Conflict Warnings

**Goal:** Warn the user at assignment creation/edit time when a developer would be over capacity, before the assignment is saved.

### 3.1 Backend — Conflict Check Endpoint

**New endpoint** in `AssignmentController` or as a standalone utility:

`GET /api/assignments/conflict-check?developerId={id}&startDate={date}&endDate={date}&ratio={ratio}&excludeAssignmentId={id}`

**Response:**

```json
{
  "hasConflict": true,
  "weeks": [
    {
      "weekStart": "2026-03-02",
      "currentHours": 24,
      "newHours": 16,
      "totalHours": 40,
      "capacityHours": 32,
      "overageHours": 8
    }
  ]
}
```

**Logic** (mirrors the frontend WorkloadHeatmap calculation, moved server-side):

1. Fetch all existing assignments for the developer (optionally excluding one being edited)
2. For each week in the proposed date range, sum `MAX_HOURS_PER_WEEK * ratio` across all assignments
3. Add the proposed assignment's contribution
4. Flag weeks where total > 32 hours

### 3.2 Frontend — Warning UI

**Assignment Creation Flow** (`confirmAddDeveloper()` in ProjectList):

Before calling `AssignmentService.createAssignment()`:

1. Call conflict-check endpoint with the selected developer, ratio, and date range
2. If `hasConflict: true`, show a warning dialog:
   - "This assignment would overload {Developer Name} in {N} weeks"
   - Show table of conflicting weeks with current/new/total hours
   - Buttons: "Assign Anyway" / "Cancel"
3. If no conflict, proceed as normal

**Assignment Edit Flow** (`saveAssignmentEdit()` in ProjectList):

Same check when changing dates or ratio on an existing assignment, passing `excludeAssignmentId` to avoid self-conflict.

**Visual Indicator:**

- In the "Add Developer" dropdown, show a small capacity indicator next to each developer name (e.g., colored dot: green = available, yellow = near capacity, red = overloaded) for the project's date range
- This requires a batch conflict check or a lightweight `/api/developers/availability` endpoint

### 3.3 Lightweight Availability Endpoint (Optional Enhancement)

`GET /api/developers/availability?startDate={date}&endDate={date}`

Returns all developers with their average utilization % across the date range. This powers the colored dots in the developer dropdown without N individual conflict-check calls.

### 3.4 Work Items

- [ ] Create conflict-check endpoint in backend (`GET /api/assignments/conflict-check`)
- [ ] Implement weekly capacity calculation logic in a service class
- [ ] Add conflict check call to `confirmAddDeveloper()` in ProjectList
- [ ] Add conflict check call to `saveAssignmentEdit()` in ProjectList
- [ ] Create warning dialog component for overload conflicts
- [ ] Add availability indicators to developer dropdown (optional)
- [ ] Create `/api/developers/availability` endpoint (optional)
- [ ] Write backend tests for conflict-check logic
- [ ] Write backend tests for edge cases (no dates, partial overlap, ratio changes)

---

## Implementation Order

The recommended implementation order balances value delivery with technical dependencies:

### Phase 1: Assignment Conflict Warnings (#3)

**Rationale:** Smallest scope, immediate user-facing value, no schema changes required for the basic version (computation only). Prevents new scheduling mistakes from day one.

### Phase 2: Progress Tracking (#2)

**Rationale:** Schema changes are additive (ALTER TABLE ADD), low risk. Enables the feedback loop that improves all future scheduling decisions.

### Phase 3: Project Dependencies (#1)

**Rationale:** Most complex (new table, graph traversal, Gantt visualization). Benefits from having progress tracking in place so dependencies can factor in actual completion state.

---

## Technical Notes

- **Schema management:** All DDL is manual (`ddl-auto=validate`). SQL scripts go in `src/` following the existing pattern (`subitems_schema.sql`, `subitems_add_dates.sql`).
- **Architecture pattern:** Controllers inject repositories directly (no service layer) except where business logic warrants it. The conflict-check and circular-dependency-check logic should use a service class.
- **Frontend pattern:** Standalone Angular components, services hardcode `localhost:8080`. New services follow existing patterns in `core/services/`.
- **Testing:** Backend uses JUnit 5 + MockMvc. New endpoints need corresponding tests.
- **Capacity constant:** 32 hours/week (8h/day × 5 days × 80% utilization), defined in WorkloadHeatmap and to be mirrored in backend service.
