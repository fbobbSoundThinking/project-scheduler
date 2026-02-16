# AI Agent Instructions: Scheduler Application Improvement Plan

## Purpose

You are planning improvements to a project scheduling application. Read `CLAUDE.md` at the project root first for full architecture context. Then study the files referenced below before proposing changes.

There are two goals. Plan each goal as a separate phase, with concrete implementation steps. For each step, identify the specific files to modify and the changes required. Call out any decisions that need human input before proceeding.

---

## Goal 1: Add Monday.com Subitems to the Data Model

### Context

The application syncs projects from a Monday.com board (board ID in `application.properties`). Currently it only syncs top-level items as projects and their column values (status, team, developer, timeline, etc.). Monday.com boards also have **subitems** — child items nested under each parent item — which are not currently captured.

### What to study

Read these files to understand the current sync pipeline:

- **GraphQL query construction:** `src/scheduler-api/src/main/java/com/example/scheduler/service/MondayService.java` — see `buildGraphQLQuery()` and `parseProjectItem()`. The current query fetches `items_page { items { id, name, group, column_values } }` but does not request `subitems`.
- **Sync/upsert logic:** `src/scheduler-api/src/main/java/com/example/scheduler/service/ProjectSyncService.java` — see `syncProjectsFromMonday()`. Projects are upserted by `item_id`, assignments by project+developer pair.
- **Data model (Java):** `src/scheduler-api/src/main/java/com/example/scheduler/model/Project.java` — has `itemId`, `projectName`, and a `OneToMany` relationship to `Assignment`. There is no subitem entity.
- **Data model (TypeScript):** `src/scheduler-ui/src/app/core/models/project.model.ts` and `assignment.model.ts` — frontend interfaces mirror the backend.
- **DTO:** `src/scheduler-api/src/main/java/com/example/scheduler/dto/MondayProject.java` — the intermediate object between Monday.com JSON and the database model.
- **Monday.com config:** `src/scheduler-api/src/main/java/com/example/scheduler/config/MondayConfig.java` — API key, board ID, group filter, developer name mapping.

### What to plan

1. **Monday.com GraphQL query** — Extend the query in `MondayService.buildGraphQLQuery()` to fetch subitems. Monday.com's API exposes subitems via `subitems { id name column_values { id text value } }` on each item. Research the current Monday.com API (2024-10 version) to confirm the correct subitem query syntax, as the API has changed over time.

2. **Database schema** — Design a new `subitems` table (or similarly named) with a foreign key to `projects`. Decide what columns to include based on what subitem data is useful for scheduling. At minimum: `subitem_id` (Monday.com ID), `projects_id` (FK), `name`, `status`, `assignee`, `timeline_start`, `timeline_end`. Remember: Hibernate is set to `ddl-auto=validate`, so the schema must be created via SQL migration, not auto-generated.

3. **JPA entity** — Create a `Subitem` entity class in `src/scheduler-api/src/main/java/com/example/scheduler/model/`. Add a `OneToMany` relationship from `Project` to `Subitem`. Add the corresponding repository.

4. **Sync logic** — Extend `ProjectSyncService.syncProjectsFromMonday()` to parse and upsert subitems after syncing each project. Follow the existing upsert pattern (find by Monday.com ID, update if exists, insert if new).

5. **REST endpoints** — Decide whether subitems need their own controller or should be nested under the existing project endpoints (e.g., `GET /api/projects/{id}/subitems`). Consider that the frontend already fetches projects with details via `GET /api/projects/details` which eager-loads assignments.

6. **Frontend model and display** — Add a `Subitem` interface in the frontend models. Decide where subitems should appear in the UI (expandable rows under projects in the project list? a detail panel?). The project list component is at `src/scheduler-ui/src/app/features/projects/project-list/`.

7. **SQL migration script** — Provide the `CREATE TABLE` statement for the new table. Place it in `src/` or `resources/` alongside `team_update.sql`.

### Decisions needed from the user

- What subitem columns from Monday.com are relevant? (status, person, timeline, date, text, numbers?)
- Should subitems have their own assignments/developer associations, or just inherit from the parent project?
- Should subitems be displayed in the Gantt view and workload heatmap, or only in the project list?

---

## Goal 2: Improve Schedule and Workload Planning Functionality

### Context

The application currently has three views:
- **Project List** (`/projects`) — sortable/filterable grid with inline editing of assignments (dates, ratio, add/remove developers). This is the primary working view.
- **Gantt View** (`/gantt`) — timeline visualization of assignments grouped by project. Read-only, no interaction beyond filtering.
- **Workload Heatmap** (`/workload`) — weekly grid showing developer utilization as colored cells. Uses 32 hours/week capacity (80% of 40). Read-only.

### What to study

Read these files to understand current planning capabilities:

- **Project list (main view):** `src/scheduler-ui/src/app/features/projects/project-list/project-list.ts` and its `.html` template — handles inline editing, developer assignment CRUD, filtering, sorting.
- **Gantt chart:** `src/scheduler-ui/src/app/features/gantt/gantt-view/gantt-view.ts` — builds timeline from assignments, groups by project, color-codes by developer. Currently display-only.
- **Workload heatmap:** `src/scheduler-ui/src/app/features/workload-heatmap/workload-heatmap.ts` — calculates weekly hours per developer from assignment data. Identifies overloaded (>100%) and underutilized (<50%) weeks. Currently display-only.
- **Filter state service:** `src/scheduler-ui/src/app/core/services/filter-state.service.ts` — shared filter state (search keyword, selected groups, sort direction) that persists across view navigation.
- **Assignment service:** `src/scheduler-ui/src/app/core/services/assignment.service.ts` — CRUD operations against the backend API.
- **Backend assignment controller:** `src/scheduler-api/src/main/java/com/example/scheduler/controller/AssignmentController.java` — REST endpoints including the custom `POST` that accepts `{projectId, developerId, ratio}` as a Map.

### What to plan

Propose improvements in order of impact. For each, describe the specific changes to existing files. Prioritize enhancements that help a project manager answer:

- "Which developers are overloaded next month?"
- "Where can I fit this new project?"
- "What happens if I move this project's timeline?"
- "Which projects are at risk of missing their target date?"

Consider these areas:

1. **Interactive Gantt chart** — Make assignment bars draggable (to change dates) and resizable (to change duration). Save changes back to the API. This transforms the Gantt from display-only into a planning tool. Note the current `getItemPosition()` method calculates pixel positions from dates — the inverse calculation (pixels → dates) will be needed for drag operations.

2. **Workload-aware scheduling** — When adding or moving an assignment, show a warning or visual indicator if the developer would be overloaded during that period. This could reuse the calculation logic already in `WorkloadHeatmap.calculateWorkload()`.

3. **What-if scenarios** — Allow the user to make tentative changes to the schedule (draft mode) and see the impact on the workload heatmap before committing. This could be frontend-only state that overlays on top of the saved data.

4. **Target date risk indicators** — On the project list, highlight projects where the latest assignment end date exceeds the `targetProdDate`. The data is already available (`Project.targetProdDate` and `Assignment.endDate`).

5. **Developer availability view** — A view or panel showing when each developer has open capacity, making it easy to find slots for new work. This is the inverse of the heatmap — showing gaps instead of load.

6. **Cross-view interaction** — Clicking a developer in the heatmap navigates to a filtered Gantt view showing only that developer's assignments. Clicking a project in the Gantt navigates to the project list filtered to that project.

7. **Bulk operations** — Allow shifting multiple assignments at once (e.g., "push all assignments for project X out by 2 weeks"). This would need a new backend endpoint for batch updates.

### Decisions needed from the user

- Which of these improvements is highest priority?
- For interactive Gantt: should changes save immediately (optimistic update) or require an explicit save/confirm action?
- For what-if scenarios: is a simple "unsaved changes" indicator sufficient, or do you want named scenarios that can be saved and compared?
- Should the workload capacity (currently hardcoded at 32 hours/week) be configurable per developer?

---

## General Guidelines for the Agent

- Study the existing code patterns before proposing changes. The backend uses Lombok `@Data`, direct repository injection in controllers, and `@Transactional` on service methods. The frontend uses Angular standalone components, `ChangeDetectorRef` for manual change detection, and hardcoded API URLs.
- Database schema changes require manual SQL — Hibernate is in `validate` mode.
- The frontend test runner is Vitest, not Karma.
- All frontend API base URLs are hardcoded to `http://localhost:8080` in the service files.
- The `[group]` column in the projects table requires bracket escaping in SQL and `@Column` annotations because `group` is a reserved word.
- Keep proposals practical and incremental — each step should be independently deployable and testable.
