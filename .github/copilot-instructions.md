# GitHub Copilot Instructions

This file provides guidance for GitHub Copilot when working with code in this repository.

## Project Overview

Full-stack project management/scheduling app that tracks ~220+ development projects across 5 teams. Data flows from Monday.com → SQL Server → Spring Boot API → Angular UI.

**Data flow:** Monday.com (GraphQL API) → Python scripts or Java sync service → SQL Server (`tp` database) → Spring Boot API (:8080) → Angular UI (:4200)

## Build, Test, and Run Commands

### Backend (Spring Boot 3.2.1 / Java 17)

**Working directory:** `src/scheduler-api/`

```bash
# Set JAVA_HOME (required on macOS with Homebrew JDK)
export JAVA_HOME="/opt/homebrew/Cellar/openjdk@21/21.0.9/libexec/openjdk.jdk/Contents/Home"

# Run backend
mvn spring-boot:run                    # Start on http://localhost:8080

# Build
mvn clean install                      # Full build

# Test
mvn test                               # Run all tests
mvn test -Dtest=AssignmentIntegrationTest   # Run single test class
mvn test -Dtest=AssignmentIntegrationTest#methodName  # Run single test method
```

### Frontend (Angular 21 / TypeScript 5.9)

**Working directory:** `src/scheduler-ui/`

```bash
# Install dependencies (--legacy-peer-deps is REQUIRED)
npm install --legacy-peer-deps

# Run frontend
ng serve                               # Start on http://localhost:4200

# Build
ng build                               # Production build

# Test
ng test                                # Run all tests (uses Vitest, NOT Karma)
ng test --run --reporter=verbose       # Run once with verbose output
ng test ProjectList                    # Run tests matching pattern
```

### Database and Startup

```bash
# From project root
./startup/azure-sql-edge-db-up.sh      # Start SQL Server Docker container
./startup/start-backend.sh             # Start backend (nohup, logs to api-output.log)
```

### Python Sync Scripts

```bash
# From project root
python3 src/sync_monday_to_mssql.py        # Sync projects from Monday.com
python3 src/sync_assignments_to_mssql.py   # Sync assignments from Monday.com
```

**Dependencies:** `requests`, `pymssql`  
**Config files:** `resources/monday.txt`, `resources/tp-database.txt` (gitignored)

## Architecture

### Directory Structure

- `src/scheduler-api/` — Spring Boot REST API (Maven project)
- `src/scheduler-ui/` — Angular frontend (npm project)
- `src/*.py` — Python sync/utility scripts for Monday.com integration
- `startup/` — Shell scripts for starting services
- `resources/` — Credential files (gitignored)
- `doc/` — Documentation
- `scratch/` — Debug/dev utility scripts

### Backend (com.example.scheduler)

Standard Spring Data JPA app. **Controllers inject repositories directly — no service layer for CRUD operations.** Services exist only for Monday.com sync logic (`MondayService`, `ProjectSyncService`).

**Package structure:**
- `controller/` — REST controllers at `/api/*` (ProjectController, DeveloperController, AssignmentController, TeamController, SyncController)
- `model/` — JPA entities with Lombok `@Data` (Project, Developer, Assignment, Team, TeamNameMap)
- `repository/` — Spring Data JPA repositories
- `service/` — Only for Monday.com sync logic
- `config/` — CORS configuration (`CorsConfig`) and other Spring configs
- `dto/` — Data transfer objects (if present)

**Key endpoint:** `POST /api/sync/monday` triggers in-app Monday.com sync (called from UI's "Update from Monday.com" button)

**CORS:** Configured both per-controller (`@CrossOrigin`) and globally via `CorsConfig` for `localhost:4200`

### Frontend (Angular Standalone Components)

Angular standalone components (no NgModules). Three routed views:

**Routes:**
- `/projects` → `ProjectList` — Main view: project grid with sorting, filtering, inline editing, developer assignment management
- `/gantt` → `GanttView` — Timeline visualization of assignments
- `/workload` → `WorkloadHeatmap` — Weekly developer utilization (32h/week capacity)

**Key patterns:**
- Services hardcode `http://localhost:8080/api/...` — no proxy config or environment files
- `FilterStateService` shares filter state across views via BehaviorSubject
- Components use manual `ChangeDetectorRef.detectChanges()` in several places
- `DeveloperList` and `ProjectDetail` components exist but are empty stubs (not routed)

**Code style:**
- Prettier configured inline in `package.json`: `printWidth: 100`, `singleQuote: true`

### Database (SQL Server / Azure SQL Edge)

Docker container on port 1433, database `tp`. Hibernate `ddl-auto=validate` — **schema is managed externally, not by Hibernate.**

**Key tables:**
- `projects` — Project data with `[group]` column (uses bracket escaping in JPA because it's a SQL reserved word)
- `developers` — Developer data with team associations
- `assignments` — Project-developer assignments with FK to projects and developers
- `teams` — Team definitions
- `team_name_map` — Maps Monday.com team labels to database team IDs

### Monday.com Integration

**Two parallel sync mechanisms** (both do the same thing):

1. **Java** (`MondayService` + `ProjectSyncService`) — Called from UI button, reads config from `application.properties`
2. **Python** (`sync_*.py` scripts) — Standalone CLI scripts, read config from `resources/monday.txt` and `resources/tp-database.txt`

Both use Monday.com GraphQL API with pagination. Developer names from Monday.com require normalization/mapping (handled differently in each implementation).

## Key Conventions

### Backend Patterns

- **No service layer for CRUD** — Controllers inject repositories directly via `@Autowired`. Only use services for complex business logic like capacity calculations or Monday.com sync.
- **Entities** — Use Lombok `@Data`, `@Entity`, `@Table`. See `Assignment.java` as a reference.
- **Relationships** — Use `@ManyToOne(fetch = FetchType.LAZY)` with `@JsonIgnoreProperties({"assignments", "hibernateLazyInitializer"})` to prevent circular serialization.
- **Repositories** — Extend `JpaRepository`. Use `@Query` with `JOIN FETCH` for eager loading to avoid N+1 queries.
- **Controllers** — Annotate with `@RestController`, `@RequestMapping("/api/...")`, `@CrossOrigin(origins = "http://localhost:4200")`.
- **DTOs** — Simple POJOs with constructors and getters/setters. See `src/scheduler-api/src/main/java/com/example/scheduler/dto/` for examples.
- **Tests** — JUnit 5 + Mockito. Use `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`.
- **Package structure** — All classes under `com.example.scheduler`: entities in `model/`, repositories in `repository/`, services in `service/`, controllers in `controller/`, DTOs in `dto/`.
- **SQL reserved word handling** — The `[group]` column on `projects` uses bracket escaping in JPA `@Column` annotation

### Frontend Patterns

- **Components** — Angular standalone components only (no NgModules). Use `standalone: true` in `@Component`.
- **Services** — Injectable with `providedIn: 'root'`. Hardcode API URLs as `http://localhost:8080/api/...` (no proxy config). Use `HttpClient` with `HttpParams`.
- **Routing** — Lazy-loaded routes in `app.routes.ts` using `loadComponent`.
- **File structure** — Services in `src/app/core/services/`, components in `src/app/features/<feature-name>/<component-name>/`.
- **Styling** — SCSS files following existing component patterns. Color thresholds for capacity: green (<70%), yellow (70-89%), red (≥90%).
- **Change detection** — Components use `ChangeDetectorRef.detectChanges()` after async data loads.
- **Imports** — Components import `CommonModule`, `FormsModule`, and required Angular modules directly in the `imports` array.
- **Shared filter state** — `FilterStateService` uses BehaviorSubject pattern to share state between views.

### Database Patterns

- **Schema scripts** — Store in `src/sql/` directory at project root (e.g., `src/sql/scenario_planning_schema.sql`)
- **Primary keys** — Use `INT IDENTITY(1,1)`
- **Data types** — `NVARCHAR` for strings, `DATE` for dates, `DECIMAL(5,2)` for ratios
- **Foreign keys** — Reference existing table PKs (e.g., `assignments(assignments_id)`, `projects(projects_id)`, `developers(developers_id)`)
- **Naming** — Use snake_case for column names
- **Assignment ratio format** — `DECIMAL(5,2)` stored as 0.1–1.0, displayed as 10%–100%
- **Developer capacity** — Workload calculations assume 32h/week capacity per developer
- **Team mapping** — Monday.com team labels must be mapped via `team_name_map` table before use

## Technologies

- **Backend:** Spring Boot 3.2.1, Java 17/21, Spring Data JPA, Lombok, Maven 3.9.12
- **Frontend:** Angular 21, TypeScript 5.9, RxJS, Vitest (not Karma)
- **Database:** SQL Server 2022 (Azure SQL Edge in Docker)
- **Integration:** Monday.com GraphQL API, Python 3.9+ scripts
