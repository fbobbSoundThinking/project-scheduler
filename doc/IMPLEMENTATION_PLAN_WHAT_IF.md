# Implementation Plan — What-If / Scenario Planning

## Problem

There is no way to test schedule changes without committing them. PMs managing 220+ projects across 5 teams need to evaluate questions like "What happens if we move Project X from Team A to Team B?" or "What if we push this project's start date back two weeks?" without modifying live data. Currently, any change to assignments is immediately persisted to the database and reflected across all views.

## Proposed Approach

Add a **scenario-based sandbox** that lets PMs create draft assignment changes (add, modify, delete) and preview the resulting impact on team capacity and developer workload before committing. Scenarios are stored in a new database table and applied as overlays on top of live data when viewing capacity projections.

This avoids duplicating the entire assignment table. Instead, a lightweight `scenario_assignments` table holds only the **deltas** (additions, modifications, deletions) from the current state. The capacity calculation engine accepts an optional scenario ID to layer these deltas onto live data.

### Why Deltas Instead of Full Clones

- The assignment table has ~2,000+ rows across 220 projects. Cloning all rows per scenario would create storage and sync issues.
- A delta approach means the scenario always reflects the latest live data plus the PM's proposed changes.
- Scenarios stay lightweight — most will have 5–15 draft changes.

---

## 1. Database — Scenario Tables

### 1.1 Schema

Two new tables:

```sql
-- Scenario metadata
CREATE TABLE scenarios (
    scenario_id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(500),
    created_by NVARCHAR(100),
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    status NVARCHAR(20) DEFAULT 'DRAFT'  -- DRAFT, APPLIED, ARCHIVED
);

-- Scenario assignment changes (deltas from live data)
CREATE TABLE scenario_assignments (
    scenario_assignment_id INT IDENTITY(1,1) PRIMARY KEY,
    scenario_id INT NOT NULL FOREIGN KEY REFERENCES scenarios(scenario_id) ON DELETE CASCADE,
    change_type NVARCHAR(10) NOT NULL,  -- ADD, MODIFY, DELETE
    -- For MODIFY/DELETE: references the live assignment being changed
    original_assignment_id INT NULL FOREIGN KEY REFERENCES assignments(assignments_id),
    -- Assignment fields (used by ADD and MODIFY)
    projects_id INT NULL FOREIGN KEY REFERENCES projects(projects_id),
    subitems_id INT NULL,
    developers_id INT NULL FOREIGN KEY REFERENCES developers(developers_id),
    start_date DATE NULL,
    end_date DATE NULL,
    ratio DECIMAL(5,2) NULL
);
```

### 1.2 How Change Types Work

| `change_type` | Meaning | Required Fields |
|---|---|---|
| `ADD` | New assignment that doesn't exist in live data | `projects_id` or `subitems_id`, `developers_id`, `start_date`, `end_date`, `ratio` |
| `MODIFY` | Change an existing live assignment | `original_assignment_id` + any fields being changed (nulls = keep original value) |
| `DELETE` | Remove an existing live assignment from the scenario | `original_assignment_id` only |

### 1.3 Work Items — Database

- [ ] Create `scenarios` table
- [ ] Create `scenario_assignments` table with foreign keys
- [ ] Create the SQL script at `src/scenario_planning_schema.sql`

---

## 2. Backend — Scenario API

### 2.1 New Entity: `Scenario`

```java
@Entity @Table(name = "scenarios")
public class Scenario {
    Integer scenarioId;
    String name;
    String description;
    String createdBy;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String status;  // DRAFT, APPLIED, ARCHIVED
}
```

### 2.2 New Entity: `ScenarioAssignment`

```java
@Entity @Table(name = "scenario_assignments")
public class ScenarioAssignment {
    Integer scenarioAssignmentId;
    Scenario scenario;              // ManyToOne
    String changeType;              // ADD, MODIFY, DELETE
    Integer originalAssignmentId;   // nullable FK
    Project project;                // nullable ManyToOne
    Subitem subitem;                // nullable ManyToOne
    Developer developer;            // nullable ManyToOne
    LocalDate startDate;
    LocalDate endDate;
    BigDecimal ratio;
}
```

### 2.3 New Repository: `ScenarioRepository`

Standard Spring Data JPA.

### 2.4 New Repository: `ScenarioAssignmentRepository`

```java
List<ScenarioAssignment> findByScenarioScenarioId(Integer scenarioId);
Optional<ScenarioAssignment> findByScenarioScenarioIdAndOriginalAssignmentId(
    Integer scenarioId, Integer originalAssignmentId);
```

### 2.5 New Service: `ScenarioService`

Core methods:

| Method | Description |
|---|---|
| `createScenario(name, description)` | Creates a new DRAFT scenario |
| `getScenario(id)` | Returns scenario with its changes |
| `listScenarios()` | Returns all scenarios ordered by updated_at desc |
| `deleteScenario(id)` | Deletes scenario and its changes (CASCADE) |
| `addChange(scenarioId, changeType, ...)` | Adds/replaces a change in the scenario |
| `removeChange(scenarioAssignmentId)` | Removes a single change from the scenario |
| `applyScenario(scenarioId)` | Commits all changes to live data, marks APPLIED |

### 2.6 Extend `CapacityService` — Scenario-Aware Capacity

Add a new method:

```java
public List<TeamCapacityResponse> getAllTeamsCapacityWithScenario(
    LocalDate from, LocalDate to, Integer scenarioId)
```

Logic:
1. Fetch all live assignments as normal
2. Fetch all `ScenarioAssignment` records for the given `scenarioId`
3. Build the effective assignment list:
   - Start with all live assignments
   - For each `DELETE` change: remove the matching live assignment
   - For each `MODIFY` change: replace fields on the matching live assignment
   - For each `ADD` change: create a virtual assignment and add it to the list
4. Run the existing capacity calculation logic against the effective list
5. Return the same `TeamCapacityResponse[]` structure (frontend needs no new DTOs for display)

Also add `getTeamDeveloperBreakdownWithScenario()` for developer-level drill-down.

### 2.7 New Controller: `ScenarioController`

At `/api/scenarios`:

| Method | Path | Description |
|---|---|---|
| GET | `/` | List all scenarios |
| POST | `/` | Create new scenario |
| GET | `/{id}` | Get scenario with changes |
| DELETE | `/{id}` | Delete scenario |
| POST | `/{id}/changes` | Add a change to the scenario |
| DELETE | `/{id}/changes/{changeId}` | Remove a change |
| POST | `/{id}/apply` | Apply scenario to live data |

At `/api/capacity` (extend existing):

| Method | Path | Description |
|---|---|---|
| GET | `/teams?scenarioId={id}&from=...&to=...` | Capacity with scenario overlay |
| GET | `/team/{teamId}/developers?scenarioId={id}&from=...&to=...` | Developer breakdown with scenario |

### 2.8 Work Items — Backend

- [ ] Create `Scenario` entity
- [ ] Create `ScenarioAssignment` entity
- [ ] Create `ScenarioRepository`
- [ ] Create `ScenarioAssignmentRepository`
- [ ] Create `ScenarioService` with CRUD + apply logic
- [ ] Extend `CapacityService` with scenario-aware capacity methods
- [ ] Extend `CapacityController` to accept optional `scenarioId` parameter
- [ ] Create `ScenarioController` with CRUD + apply endpoints
- [ ] Write unit tests for scenario-aware capacity calculation
- [ ] Write unit tests for apply scenario logic

---

## 3. Frontend — Scenario Planning UI

### 3.1 New Route

Add `/scenarios` route in `app.routes.ts` pointing to a new `ScenarioPlanner` component. Add a navigation link labeled "What-If" in the app header.

### 3.2 New Service: `scenario.service.ts`

In `core/services/`:

```typescript
export interface Scenario {
  scenarioId: number;
  name: string;
  description: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  status: 'DRAFT' | 'APPLIED' | 'ARCHIVED';
}

export interface ScenarioChange {
  scenarioAssignmentId: number;
  changeType: 'ADD' | 'MODIFY' | 'DELETE';
  originalAssignmentId: number | null;
  projectId: number | null;
  projectName: string | null;
  developerId: number | null;
  developerName: string | null;
  startDate: string | null;
  endDate: string | null;
  ratio: number | null;
}

export interface ScenarioDetail {
  scenario: Scenario;
  changes: ScenarioChange[];
}
```

Methods: `listScenarios()`, `createScenario()`, `getScenario()`, `deleteScenario()`, `addChange()`, `removeChange()`, `applyScenario()`, `getCapacityWithScenario()`.

### 3.3 New Component: `ScenarioPlanner`

Location: `features/scenarios/scenario-planner/`

**Layout — Three Sections:**

**Section A — Scenario Selector (top bar)**
- Dropdown of existing scenarios (filtered by DRAFT status by default)
- "New Scenario" button → opens inline name/description form
- "Delete Scenario" button with confirmation
- Active scenario name displayed prominently

**Section B — Change Editor (left panel, ~40% width)**
- List of current changes in the active scenario, grouped by type (ADD / MODIFY / DELETE)
- Each change shows: project name, developer name, dates, ratio, change type badge
- "Add Assignment" button → opens form with project/developer dropdowns, date pickers, ratio slider
- "Remove" button on each change to undo it
- For MODIFY: shows original values with strikethrough and new values highlighted
- For DELETE: shows the assignment being removed with a red badge

**Section C — Impact Preview (right panel, ~60% width)**
- Reuses the capacity grid layout from the Capacity Dashboard
- Shows two overlaid views:
  - **Baseline** (current live data) in muted colors
  - **With scenario** in bold colors
- Delta indicators on cells: ↑/↓ arrows or +/- hours showing the change from baseline
- Color coding follows the same thresholds as the Capacity Dashboard
- Expandable developer rows (same as Capacity Dashboard)
- A summary bar at top: "This scenario adds X hours, removes Y hours, net change: ±Z hours"

**Section D — Actions (bottom bar)**
- "Apply Scenario" button → confirmation dialog listing all changes → commits to live data
- "Compare" toggle → side-by-side baseline vs. scenario capacity grids
- Status indicator: DRAFT / APPLIED / ARCHIVED

### 3.4 Work Items — Frontend

- [ ] Create `scenario.service.ts` with interfaces and API methods
- [ ] Add `/scenarios` route to `app.routes.ts`
- [ ] Add "What-If" navigation link to app header
- [ ] Create `ScenarioPlanner` component (standalone)
- [ ] Implement scenario selector (Section A)
- [ ] Implement change editor with add/modify/delete forms (Section B)
- [ ] Implement impact preview reusing capacity grid layout (Section C)
- [ ] Implement apply scenario workflow with confirmation (Section D)
- [ ] Extend `CapacityService` frontend to accept optional `scenarioId`
- [ ] Style with SCSS following existing patterns

---

## 4. Implementation Order

### Phase 1: Database + Backend Core

Build the scenario storage and CRUD API. This is the foundation.

1. Create schema SQL script
2. Create `Scenario` and `ScenarioAssignment` entities
3. Create repositories
4. Create `ScenarioService` with CRUD operations
5. Create `ScenarioController`
6. Test CRUD with curl

### Phase 2: Scenario-Aware Capacity

Extend the capacity engine to overlay scenario deltas. This is the highest-risk piece.

1. Add scenario-aware methods to `CapacityService`
2. Extend `CapacityController` to accept `scenarioId`
3. Write unit tests for delta overlay logic
4. Test capacity with scenario via curl

### Phase 3: Frontend — Scenario Management

Build the scenario list/create/delete UI and change editor.

1. Create `scenario.service.ts`
2. Add route + nav link
3. Build scenario selector (Section A)
4. Build change editor (Section B) — forms for ADD, MODIFY, DELETE
5. Wire up API calls

### Phase 4: Frontend — Impact Preview

Build the capacity impact visualization and apply workflow.

1. Build impact preview panel reusing capacity grid (Section C)
2. Add baseline vs. scenario comparison
3. Add delta indicators
4. Build apply workflow with confirmation (Section D)
5. Polish styling and interactions

---

## Technical Notes

- **No new external dependencies.** Uses existing Spring Data JPA, Angular standalone components, and existing services.
- **Capacity calculation reuse.** The scenario-aware capacity method reuses the same `WeekCapacity` / `TeamCapacityResponse` DTOs. The frontend capacity grid component can be refactored into a shared sub-component used by both the Capacity Dashboard and the Scenario Planner impact preview.
- **Scenario isolation.** Each scenario is independent — changes in one don't affect another. Scenarios reference live assignment IDs, so if a live assignment is deleted, any MODIFY/DELETE changes referencing it become orphaned (the apply logic should handle this gracefully).
- **Apply is destructive.** Applying a scenario executes real CRUD operations on the `assignments` table. After applying, the scenario is marked APPLIED and becomes read-only. This should be behind a confirmation dialog.
- **No auth required.** The app has no authentication. `created_by` is an optional text field for tracking who created a scenario.
- **Performance.** Scenario-aware capacity calculation fetches one extra table (`scenario_assignments`) per request. With 5–15 changes per scenario, this adds negligible overhead.
- **Testing pattern.** Backend: JUnit 5 + Mockito (matches `CapacityServiceTest`). Frontend: Vitest (matches existing `.spec.ts` files).
- **CORS.** New controller follows existing `@CrossOrigin(origins = "http://localhost:4200")` pattern.
