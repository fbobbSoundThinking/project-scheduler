# Monday.com Sync Implementation

## Overview
Implemented a complete Monday.com to database sync feature that allows users to update projects and assignments with one button click.

## Architecture

### Backend (Java/Spring Boot)

#### 1. **Configuration** (`MondayConfig.java`)
- Loads Monday.com API key, board ID, and groups from `application.properties`
- Contains developer name mappings (Monday display name → Database full name)
- Provides `getNormalizedDeveloperName()` helper method

#### 2. **DTOs**
- **MondayProject.java**: Represents project data from Monday.com
  - Item ID, project name, status, developer team
  - Developers list, timeline (start/end dates), project number
- **SyncResponse.java**: API response with sync results
  - Success flag, counts (updated/inserted), error messages

#### 3. **Models**
- **TeamNameMap.java**: Maps Monday.com team names to database team IDs
  - Fields: `id`, `mondayTeamName`, `teamsId`
  - Table: `team_name_map`

#### 4. **Repositories**
- **TeamNameMapRepository.java**: JPA repository for team name mapping
  - `findByMondayTeamName(String)` - lookup team ID by Monday name
- **ProjectRepository**: Added `findByItemId(String)` for upsert logic
- **AssignmentRepository**: Added `findByProjectIdAndDeveloperId()` for assignment matching

#### 5. **Services**

**MondayService.java** - Monday.com API Client
- `fetchProjectsFromMonday()` - Main method to retrieve all projects
- Uses GraphQL API with pagination (cursor-based)
- Filters by configured groups
- Parses column values:
  - `status_150` → Developer Team
  - `people` → Developers
  - `timeline_1__1` → Start/End dates
  - `link_to_item__1` → Project number
- Returns `List<MondayProject>`

**ProjectSyncService.java** - Sync Orchestration
- `syncProjectsFromMonday()` - Main sync method (transactional)
- **Team Mapping**: Uses `team_name_map` table instead of `teams` table
  - `buildTeamMap()` - Creates map: Monday team name → teams_id
  - Uses `monday_team_name` column for lookups
- **Project Sync Logic**:
  - Upsert by `item_id` (Monday.com unique identifier)
  - Updates: project name, status, team, project number
  - Inserts: new projects with default values (level='TBD', primary_app_id=26)
- **Assignment Sync Logic**:
  - Matches assignments by project ID + developer ID
  - Updates timeline dates if changed
  - Creates new assignments with ratio=1.0
  - Uses normalized developer names from config
- **Developer Mapping**: 
  - `buildDeveloperMap()` - Maps "FIRSTNAME LASTNAME" → Developer entity
  - Case-insensitive matching via uppercase conversion
- Returns detailed `SyncResponse` with counts

#### 6. **Controller** (`SyncController.java`)
- `POST /api/sync/monday` - Trigger sync endpoint
- CORS enabled for localhost:4200
- Returns HTTP 200 on success, 500 on error
- Response body contains sync statistics

### Frontend (Angular)

#### 1. **UI Changes**
- **Removed**: "Refresh" button from controls section
- **Added**: "🔄 Update from Monday.com" button in page header
- Position: Next to "Project Scheduler" title
- States: Normal / Syncing (with ⏳ icon)

#### 2. **Component** (`project-list.ts`)
- Added `syncing: boolean` property for loading state
- Added `HttpClient` dependency injection
- **syncFromMonday() Method**:
  - Prevents duplicate clicks (guard with `syncing` flag)
  - Calls `POST http://localhost:8080/api/sync/monday`
  - Shows detailed alert with counts on success
  - Shows error alert on failure
  - Auto-refreshes project list after successful sync
  - Triggers change detection for UI update

#### 3. **Styling** (`project-list.scss`)
- Header with flexbox layout (title left, button right)
- Green sync button with hover effects
- Disabled state (grey) when syncing
- Responsive design

## Data Flow

```
User Click → Frontend
             ↓
    POST /api/sync/monday
             ↓
    ProjectSyncService.syncProjectsFromMonday()
             ↓
    ┌─────────────────────────────────────┐
    │ 1. MondayService.fetchProjects()    │
    │    - GraphQL API calls              │
    │    - Parse Monday.com data          │
    └─────────────────────────────────────┘
             ↓
    ┌─────────────────────────────────────┐
    │ 2. Build Mappings                   │
    │    - team_name_map → teams_id       │
    │    - Developer names → entities     │
    └─────────────────────────────────────┘
             ↓
    ┌─────────────────────────────────────┐
    │ 3. Sync Projects                    │
    │    - Upsert by item_id              │
    │    - Update/Insert with team mapping│
    └─────────────────────────────────────┘
             ↓
    ┌─────────────────────────────────────┐
    │ 4. Sync Assignments                 │
    │    - Match by project + developer   │
    │    - Update timelines               │
    │    - Create new assignments         │
    └─────────────────────────────────────┘
             ↓
    Return SyncResponse with counts
             ↓
    Frontend shows alert & refreshes
```

## Key Implementation Details

### Team Mapping Strategy
**Original Python Script**: Used `teams` table directly
**New Java Implementation**: Uses `team_name_map` table

**Why the change?**
- Monday.com team names don't always match database team names
- `team_name_map` provides explicit Monday → Database mapping
- Allows flexibility for team name variations
- Example: "Dev Team 1" (Monday) → Team ID 5 (Database)

**Table Structure**:
```sql
CREATE TABLE team_name_map (
    id INT IDENTITY PRIMARY KEY,
    monday_team_name VARCHAR(100),
    teams_id INT
);
```

### Upsert Logic
- **Match Key**: `item_id` (Monday.com unique identifier)
- **If Exists**: Update project name, status, team, project number
- **If New**: Insert with defaults (level='TBD', primary_app_id=26)

### Developer Name Normalization
- Monday.com: "John Doe"
- Config Map: "John Doe" → "JOHN DOE"
- Database Match: "JOHN DOE" (FIRSTNAME + " " + LASTNAME uppercase)

### Error Handling
- Backend: Try-catch per project/assignment (continues on error)
- Frontend: Alert user with error message
- Logging: All errors logged to console/logs

## Configuration

### application.properties
```properties
# Monday.com Configuration
monday.api-key=eyJhbGciOiJIUzI1NiJ9...
monday.board-id=3423917288
monday.groups=In Progress/Scheduled,Backlog,Internal Tracking,Pending Authorization,Closed,Removed/Cancelled/Duplicate
```

### Developer Name Mappings (in MondayConfig.java)
```java
developerNameMap.put("Arkopal Saha", "ARKOPAL SAHA");
developerNameMap.put("Harishkumar Gundameedi", "HARISHKUMAR GUNDAMEEDI");
// ... etc
```

## Testing

### Manual Test Steps
1. Start backend: `mvn spring-boot:run` (Java 17)
2. Start frontend: `npm start`
3. Navigate to http://localhost:4200
4. Click "🔄 Update from Monday.com" button in header
5. Wait for sync to complete (5-30 seconds depending on data size)
6. Check alert for success message with counts
7. Verify projects table refreshes with new/updated data

### Verify Sync Results
```sql
-- Check synced projects
SELECT item_id, project_name, status, primary_team_id 
FROM projects 
WHERE item_id IS NOT NULL;

-- Check team mappings used
SELECT * FROM team_name_map;

-- Check synced assignments
SELECT p.project_name, d.first_name, d.last_name, a.start_date, a.end_date
FROM assignments a
JOIN projects p ON a.projects_id = p.projects_id
JOIN developers d ON a.developers_id = d.developers_id
WHERE p.item_id IS NOT NULL;
```

## Future Enhancements
- [ ] Schedule automatic sync (e.g., every hour via cron)
- [ ] Add sync history/audit table
- [ ] Show last sync timestamp in UI
- [ ] Add "Sync in Progress" modal with progress bar
- [ ] Email notifications for sync errors
- [ ] Webhook integration (Monday.com → Auto-sync)
- [ ] Sync dry-run mode (preview changes)
- [ ] Selective sync (specific groups/projects only)
- [ ] Conflict resolution UI (when local changes exist)

## Files Modified/Created

### Backend
- ✅ `dto/MondayProject.java` (new)
- ✅ `dto/SyncResponse.java` (new)
- ✅ `config/MondayConfig.java` (new)
- ✅ `model/TeamNameMap.java` (new)
- ✅ `repository/TeamNameMapRepository.java` (new)
- ✅ `repository/ProjectRepository.java` (added findByItemId)
- ✅ `repository/AssignmentRepository.java` (added findByProjectIdAndDeveloperId)
- ✅ `service/MondayService.java` (new)
- ✅ `service/ProjectSyncService.java` (new)
- ✅ `controller/SyncController.java` (new)
- ✅ `model/Project.java` (added itemId field)
- ✅ `application.properties` (added Monday config)

### Frontend
- ✅ `project-list.html` (removed refresh button, added sync button)
- ✅ `project-list.ts` (added syncFromMonday method, HttpClient)
- ✅ `project-list.scss` (added header/button styles)

### Database
- ⚠️ Required: `item_id` column in `projects` table (VARCHAR(50))
- ⚠️ Required: `team_name_map` table with mappings

## Summary
The Monday.com sync feature is now fully functional, using the `team_name_map` table for proper team mapping from Monday.com to the database. Users can click one button to sync all projects and assignments with comprehensive error handling and user feedback.
