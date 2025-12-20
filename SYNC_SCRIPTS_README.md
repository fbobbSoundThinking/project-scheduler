# Monday.com to MS SQL Server Sync Scripts

## Overview
These scripts synchronize data from Monday.com to the MS SQL Server `tp` database running in Docker.

## Configuration Files

### monday.txt
Contains Monday.com API credentials and board configuration:
- `api_key`: Monday.com API authentication token
- `mms-board-id`: The board ID to sync from (3423917288)
- `groups`: Comma-separated list of groups to sync

### tp-database.txt
Contains MS SQL Server connection details:
- `host`: localhost
- `port`: 1433
- `user`: sa
- `password`: karst-habit-&01

## Scripts

### 1. sync_monday_to_mssql.py
**Purpose**: Syncs project information from Monday.com to the `projects` table

**What it does**:
- Retrieves all projects from specified Monday.com groups
- Handles pagination to get ALL projects (not just first 25)
- Updates project status based on Monday.com group
- Creates new projects if they don't exist in database

**Usage**:
```bash
python3 sync_monday_to_mssql.py
```

**Latest Run Results**:
- Retrieved: 208 projects across 4 groups
  - In Progress/Scheduled: 65 projects
  - Backlog: 126 projects
  - Internal Tracking: 8 projects
  - Pending Authorization: 9 projects
- Updated: 194 existing projects
- Inserted: 14 new projects

### 2. sync_assignments_to_mssql.py
**Purpose**: Syncs developer assignments from Monday.com to the `assignments` table

**What it does**:
- Retrieves project-developer assignments from Monday.com
- Extracts "Dev Resource" (assigned developers)
- Extracts "Developer Timeline" (start/end dates)
- Creates/updates assignment records linking projects to developers
- Maps Monday.com developer names to database developer records

**Usage**:
```bash
python3 sync_assignments_to_mssql.py
```

**Latest Run Results**:
- Retrieved: 143 project assignments
- Inserted: 103 new assignments
- Updated: 0 assignments
- Skipped developers not in database: Frank Bobb, Tariq Islam, Jorge Abrantes, Archana Avvaru

**Developer Mapping**:
The script includes name mapping for variations:
- "Harish Gundameedi" → "HARISHKUMAR GUNDAMEEDI"
- "Anusha Reddy Kallu" → "ANUSHA KALLU"
- "Chandra Sekhar Gottumukkala" → "CHANDRA GOTTUMUKKALA"

## Database Tables Updated

### projects
- `project_name`: Project title from Monday.com
- `status`: Mapped from Monday.com group name
- Other fields: maintained from existing data

### assignments
- `projects_id`: Foreign key to projects table
- `developers_id`: Foreign key to developers table
- `start_date`: From "Developer Timeline" start date
- `end_date`: From "Developer Timeline" end date
- `ratio`: Set to 1.0 (full assignment)

## Running the Sync

### Full Sync Workflow
```bash
# 1. Sync projects first
python3 sync_monday_to_mssql.py

# 2. Then sync assignments
python3 sync_assignments_to_mssql.py
```

### Scheduled Sync
To run automatically, create a cron job or scheduled task:
```bash
# Example cron (runs daily at 6 AM)
0 6 * * * cd ~/Development/scheduler && python3 sync_monday_to_mssql.py && python3 sync_assignments_to_mssql.py
```

## Pagination Handling

Both scripts properly handle Monday.com pagination:
- Uses a limit of 500 items per page (maximum allowed)
- Follows cursor-based pagination
- Continues until no more pages are available
- Ensures ALL items are retrieved, not just the first page

## Error Handling

The scripts handle:
- Missing developers (logged as warnings, skipped)
- Projects not found in database (skipped with count)
- Empty timelines (assignments created without dates)
- Name variations in developer names

## Verification Queries

After running sync, verify with:

```sql
-- Count projects by status
SELECT status, COUNT(*) as count 
FROM dbo.projects 
GROUP BY status 
ORDER BY count DESC;

-- Count assignments
SELECT COUNT(*) FROM dbo.assignments;

-- Assignments by developer
SELECT 
    d.first_name + ' ' + d.last_name AS developer,
    COUNT(*) as assignment_count
FROM dbo.assignments a
JOIN dbo.developers d ON a.developers_id = d.developers_id
GROUP BY d.first_name, d.last_name
ORDER BY COUNT(*) DESC;

-- Recent assignments with details
SELECT TOP 10
    p.project_name,
    d.first_name + ' ' + d.last_name AS developer,
    a.start_date,
    a.end_date
FROM dbo.assignments a
JOIN dbo.projects p ON a.projects_id = p.projects_id
JOIN dbo.developers d ON a.developers_id = d.developers_id
ORDER BY a.assignments_id DESC;
```

## Dependencies

```bash
pip3 install requests pymssql
```

## Troubleshooting

### "Login failed" Error
- Verify password in `tp-database.txt` matches Docker container
- Check if SQL Server container is running: `docker ps | grep mssql`

### "Unknown developer" Warnings
- Check if developer exists in `developers` table
- Add mapping to `DEVELOPER_NAME_MAP` in script if name variation

### No Projects Retrieved
- Verify API key in `monday.txt` is valid
- Check group names match exactly (case-sensitive)
- Confirm board ID is correct

## Monday.com Board Structure

**Board**: MMS Dependency Board (ID: 3423917288)

**Key Columns**:
- `people` (Dev Resource): Assigned developers
- `timeline_1__1` (Developer Timeline): Start and end dates
- `status`: Project status
- Various other tracking fields

**Groups Synced**:
1. In Progress/Scheduled
2. Backlog
3. Internal Tracking
4. Pending Authorization

## Notes

- Assignments without timelines will have NULL start_date and end_date
- The `ratio` field is set to 1.0 for all assignments (full-time)
- Project matching is done by exact name comparison
- Database must be accessible at localhost:1433
