## Existing Patterns to Follow

### Backend Patterns

  - **Entities:** Use Lombok `@Data`, `@Entity`, `@Table`. See `src/scheduler-api/src/main/java/com/example/scheduler/model/Assignment.java` as a reference.
  - **Relationships:** Use `@ManyToOne(fetch = FetchType.LAZY)` with `@JsonIgnoreProperties({"assignments", "hibernateLazyInitializer"})` to prevent circular serialization.
  - **Repositories:** Extend `JpaRepository`. Use `@Query` with `JOIN FETCH` for eager loading where needed. See `AssignmentRepository.java`.
  - **Controllers:** Annotate with `@RestController`, `@RequestMapping("/api/...")`, `@CrossOrigin(origins = "http://localhost:4200")`. Controllers inject repositories/services directly via `@Autowired`.
  - **Services:** Business logic lives in `@Service` classes. See `src/scheduler-api/src/main/java/com/example/scheduler/service/CapacityService.java` for the capacity calculation pattern.
  - **DTOs:** Simple POJOs with constructor + getters/setters. See `src/scheduler-api/src/main/java/com/example/scheduler/dto/` for examples (`WeekCapacity.java`, `TeamCapacityResponse.java`, `DeveloperCapacity.java`).
  - **Tests:** JUnit 5 + Mockito for unit tests. See `src/scheduler-api/src/test/java/com/example/scheduler/service/CapacityServiceTest.java`. Use `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`.
  - **Package:** All classes go under `com.example.scheduler` — entities in `model/`, repos in `repository/`, services in `service/`, controllers in `controller/`, DTOs in `dto/`.

### Frontend Patterns

  - **Components:** Angular standalone components (no NgModules). Use `standalone: true` in `@Component`.
  - **Services:** Injectable with `providedIn: 'root'`. Hardcode API URL as `http://localhost:8080/api/...`. Use `HttpClient` with `HttpParams`.
  - **Routing:** Lazy-loaded routes in `src/scheduler-ui/src/app/app.routes.ts` using `loadComponent`.
  - **Styling:** SCSS files following WorkloadHeatmap/CapacityDashboard patterns. Color thresholds: green (<70%), yellow (70-89%), red (≥90%).
  - **Change detection:** Components use `ChangeDetectorRef.detectChanges()` after async data loads.
  - **Imports:** Components import `CommonModule`, `FormsModule`, and any needed Angular modules directly in the `imports` array.
  - **Services location:** `src/scheduler-ui/src/app/core/services/`
  - **Components location:** `src/scheduler-ui/src/app/features/<feature-name>/<component-name>/`

### Database Patterns

  - Schema scripts go in `src/sql/` directory at the project root (e.g., `src/sql/scenario_planning_schema.sql`)
  - Use `INT IDENTITY(1,1)` for primary keys
  - Use `NVARCHAR` for strings, `DATE` for dates, `DECIMAL(5,2)` for ratios
  - Foreign keys reference existing table PKs (e.g., `assignments(assignments_id)`, `projects(projects_id)`, `developers(developers_id)`)
  - Column names use snake_case
