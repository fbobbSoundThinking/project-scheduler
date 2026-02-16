# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Full-stack project management/scheduling app that tracks ~220+ development projects across 5 teams. Data is sourced from a Monday.com board, stored in SQL Server, served by a Spring Boot REST API, and displayed in an Angular SPA.

**Data flow:** Monday.com (GraphQL API) → Python scripts or Java sync service → SQL Server (`tp` database) → Spring Boot API (:8080) → Angular UI (:4200)

## Development Commands

### Backend (Spring Boot 3.2.1 / Java 17)

Working directory: `src/scheduler-api/`

```bash
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@21/21.0.9/libexec/openjdk.jdk/Contents/Home"
mvn spring-boot:run          # Start on http://localhost:8080
mvn test                     # Run tests (JUnit 5 + MockMvc)
mvn clean install            # Full build
```

### Frontend (Angular 21 / TypeScript 5.9)

Working directory: `src/scheduler-ui/`

```bash
npm install --legacy-peer-deps   # Initial setup (required flag)
ng serve                         # Start on http://localhost:4200
ng test                          # Run tests (Vitest, NOT Karma)
ng build                         # Production build
```

### Startup Scripts (from project root)

```bash
./startup/azure-sql-edge-db-up.sh   # Start SQL Server Docker container
./startup/start-backend.sh          # Start backend (nohup, logs to api-output.log)
```

### Python Sync Scripts (from project root)

```bash
python3 src/sync_monday_to_mssql.py        # Sync projects from Monday.com
python3 src/sync_assignments_to_mssql.py   # Sync assignments from Monday.com
```

Requires `requests` and `pymssql`. Config read from `resources/monday.txt` and `resources/tp-database.txt` (gitignored).

## Architecture

### Directory Structure

- `src/scheduler-api/` — Spring Boot REST API
- `src/scheduler-ui/` — Angular frontend
- `src/*.py` — Python sync/utility scripts
- `startup/` — Shell scripts for starting services
- `resources/` — Credential files (gitignored)
- `doc/` — Documentation
- `scratch/` — Debug/dev utility scripts

### Backend (com.example.scheduler)

Standard Spring Data JPA app. Controllers inject repositories directly (no service layer for CRUD). Services exist only for Monday.com sync logic.

- **Controllers:** `ProjectController`, `DeveloperController`, `AssignmentController`, `TeamController`, `SyncController` — all at `/api/*`
- **Models/Entities:** `Project`, `Developer`, `Assignment`, `Team`, `TeamNameMap` — Lombok `@Data` on most
- **Key endpoint:** `POST /api/sync/monday` triggers in-app Monday.com sync (called from UI's "Update from Monday.com" button)
- **CORS:** Configured both per-controller (`@CrossOrigin`) and globally via `CorsConfig` for `localhost:4200`

### Frontend

Angular standalone components (no NgModules). Three routed views:

- `/projects` — `ProjectList` (main view: project grid with sorting, filtering, inline editing, developer assignment management)
- `/gantt` — `GanttView` (timeline visualization of assignments)
- `/workload` — `WorkloadHeatmap` (weekly developer utilization, 32h/week capacity)

Services hardcode `http://localhost:8080/api/...` (no proxy config or environment files). `FilterStateService` shares filter state across views via BehaviorSubject.

### Database (SQL Server / Azure SQL Edge)

Docker container on port 1433, database `tp`. Hibernate `ddl-auto=validate` — schema is managed externally.

**Key tables:** `projects`, `developers`, `assignments` (with FK to projects and developers), `teams`, `team_name_map` (maps Monday.com team labels to DB team IDs).

The `[group]` column on `projects` uses bracket escaping in JPA `@Column` because `group` is a SQL reserved word.

### Monday.com Integration

Two parallel sync mechanisms doing the same thing:
1. **Java** (`MondayService` + `ProjectSyncService`) — called from UI, reads config from `application.properties`
2. **Python** (`sync_*.py`) — standalone CLI scripts, reads config from `resources/monday.txt` and `resources/tp-database.txt`

Both use Monday.com GraphQL API with pagination. Developer names from Monday.com need normalization/mapping (handled differently in each).

## Notable Patterns

- Assignment `ratio` is `BigDecimal(5,2)`, stored as 0.1–1.0, displayed as 10%–100%
- `AssignmentRepository` uses `@Query` with `JOIN FETCH` for eager loading to avoid N+1
- `@JsonIgnoreProperties({"assignments", "hibernateLazyInitializer"})` on Assignment's relations prevents circular serialization
- Frontend components use manual `ChangeDetectorRef.detectChanges()` in several places
- `DeveloperList` and `ProjectDetail` components are empty stubs (not routed)
- Prettier configured inline in `package.json` (printWidth: 100, singleQuote: true)
