#!/usr/bin/env python3
"""
Comprehensive Monday.com to MySQL Sync Script
Syncs projects and assignments from Monday.com to MySQL tp database
- Uses item_id for project matching
- Updates primary_team_id using team_name_map
- Syncs developer assignments with timeline dates
"""

import requests
import pyodbc
import json
from datetime import datetime

# Read configuration files
def read_config(filename):
    config = {}
    with open(filename, 'r') as f:
        for line in f:
            line = line.strip()
            if ':' in line:
                key, value = line.split(':', 1)
                config[key] = value
    return config

monday_config = read_config('./resources/monday.txt')
db_config = read_config('./resources/tp-database.txt')

# Parse Monday.com config
API_KEY = monday_config['api_key']
BOARD_ID = monday_config['mms-board-id']
GROUPS = [g.strip().strip('"') for g in monday_config['groups'].split(',')]

print("Configuration:")
print(f"  Board ID: {BOARD_ID}")
print(f"  Groups: {GROUPS}")
print()

# Monday.com API endpoint
MONDAY_API_URL = "https://api.monday.com/v2"
HEADERS = {
    "Authorization": API_KEY,
    "Content-Type": "application/json"
}

# Developer name mapping (Monday.com display name -> database full name)
DEVELOPER_NAME_MAP = {
    'Arkopal Saha': 'ARKOPAL SAHA',
    'Harishkumar Gundameedi': 'HARISHKUMAR GUNDAMEEDI',
    'Harish Gundameedi': 'HARISHKUMAR GUNDAMEEDI',
    'Krishna Karangula': 'KRISHNA KARANGULA',
    'Kunal Tamang': 'KUNAL TAMANG',
    'Romal Patel': 'ROMAL PATEL',
    'Sandhya Mankala': 'SANDHYA MANKALA',
    'Anusha Kallu': 'ANUSHA KALLU',
    'Anusha Reddy Kallu': 'ANUSHA KALLU',
    'Gireesh Namburi': 'GIREESH NAMBURI',
    'Sanjeev Singh': 'SANJEEV SINGH',
    'Srinivas Muga': 'SRINIVAS MUGA',
    'Yash Patel': 'YASH PATEL',
    'Shashi Nareguda': 'SHASHI NAREGUDA',
    'Anusha Muppaneni': 'ANUSHA MUPPANENI',
    'Hemalatha Rengaramanujam': 'HEMALATHA RENGARAMANUJAM',
    'Himabindu Chilakalapudi': 'HIMABINDU CHILAKALAPUDI',
    'Sundar Rana': 'SUNDAR RANA',
    'Chandra Sekhar Gottumukkala': 'CHANDRA GOTTUMUKKALA',
    'Yogesh Patel': 'YOGESH PATEL',
    'Chandra Gottumukkala': 'CHANDRA GOTTUMUKKALA',
}

def connect_to_mssql():
    """Connect to MS SQL Server"""
    server = 'localhost,1433'
    database = 'tp'
    username = 'sa'
    password = 'karst-habit-&01'
    
    conn_str = f'DRIVER={{ODBC Driver 18 for SQL Server}};SERVER={server};DATABASE={database};UID={username};PWD={password};Encrypt=yes;TrustServerCertificate=yes;Connection Timeout=30;'
    return pyodbc.connect(conn_str)

def get_developers_from_db():
    """Get all developers from database with their IDs"""
    conn = connect_to_mssql()
    cursor = conn.cursor()
    
    cursor.execute("""
        SELECT developers_id, first_name, last_name 
        FROM developers 
        ORDER BY last_name, first_name
    """)
    
    developers = {}
    for row in cursor.fetchall():
        full_name = f"{row[1]} {row[2]}"
        developers[full_name] = row[0]
    
    cursor.close()
    conn.close()
    
    return developers

def get_team_name_map():
    """Get team name mapping from database"""
    conn = connect_to_mssql()
    cursor = conn.cursor()
    
    cursor.execute("""
        SELECT monday_team_name, teams_id 
        FROM team_name_map
    """)
    
    team_map = {}
    for row in cursor.fetchall():
        team_map[row[0]] = row[1]
    
    cursor.close()
    conn.close()
    
    return team_map

def query_monday_projects_and_assignments():
    """Query Monday.com for projects with all relevant data"""
    
    all_data = []
    
    for group_name in GROUPS:
        print(f"\nQuerying group: {group_name}")
        
        cursor = None
        page_num = 1
        
        while True:
            # Query with all needed columns
            query = """
            query ($boardId: ID!, $limit: Int!, $cursor: String) {
                boards(ids: [$boardId]) {
                    groups {
                        id
                        title
                        items_page(limit: $limit, cursor: $cursor) {
                            cursor
                            items {
                                id
                                name
                                column_values {
                                    id
                                    text
                                    value
                                }
                            }
                        }
                    }
                }
            }
            """
            
            variables = {
                "boardId": BOARD_ID,
                "limit": 500,
                "cursor": cursor
            }
            
            response = requests.post(
                MONDAY_API_URL,
                json={"query": query, "variables": variables},
                headers=HEADERS
            )
            
            if response.status_code != 200:
                print(f"  Error: {response.status_code}")
                print(response.text)
                break
            
            data = response.json()
            
            found_items = False
            next_cursor = None
            
            if 'data' in data and 'boards' in data['data'] and len(data['data']['boards']) > 0:
                board = data['data']['boards'][0]
                
                for group in board['groups']:
                    if group['title'] == group_name:
                        items_page = group['items_page']
                        items = items_page['items'] if items_page else []
                        
                        if items:
                            found_items = True
                            print(f"  Page {page_num}: Retrieved {len(items)} items")
                            
                            for item in items:
                                project_data = {
                                    'item_id': item['id'],
                                    'project_name': item['name'],
                                    'group': group_name,
                                    'developer_team': None,
                                    'developers': [],
                                    'timeline': None,
                                    'prj_number': None,
                                    'columns': {}
                                }
                                
                                # Extract all column values
                                for col in item['column_values']:
                                    col_id = col['id']
                                    
                                    # Store text values
                                    if col['text']:
                                        project_data['columns'][col_id] = col['text']
                                    
                                    # Developer Team (status_150)
                                    if col_id == 'status_150' and col['text']:
                                        teams = col['text'].split(',')
                                        project_data['developer_team'] = teams[0].strip()
                                    
                                    # Dev Resource (people column)
                                    elif col_id == 'people' and col['value']:
                                        try:
                                            if col['text']:
                                                dev_names = [name.strip() for name in col['text'].split(',')]
                                                project_data['developers'] = dev_names
                                        except:
                                            pass
                                    
                                    # Developer Timeline (timeline_1__1)
                                    elif col_id == 'timeline_1__1' and col['value']:
                                        try:
                                            timeline_data = json.loads(col['value'])
                                            if 'from' in timeline_data and 'to' in timeline_data:
                                                project_data['timeline'] = {
                                                    'start_date': timeline_data['from'],
                                                    'end_date': timeline_data['to']
                                                }
                                        except:
                                            pass
                                    
                                    # Project # - SN (Link) (link_to_item__1)
                                    elif col_id == 'link_to_item__1' and col['text']:
                                        # Extract first 10 characters
                                        project_data['prj_number'] = col['text'][:10]
                                
                                all_data.append(project_data)
                        
                        if items_page:
                            next_cursor = items_page.get('cursor')
                        break
            
            if found_items and next_cursor:
                cursor = next_cursor
                page_num += 1
            else:
                break
        
        print(f"  Total items in '{group_name}': {len([d for d in all_data if d['group'] == group_name])}")
    
    return all_data

def sync_projects_to_db(projects_data, team_map):
    """Sync projects to MySQL database using item_id as key"""
    
    if not projects_data:
        print("No projects to sync")
        return {}
    
    conn = connect_to_mssql()
    cursor = conn.cursor()
    
    print(f"\nSyncing {len(projects_data)} projects to database...")
    
    updated_count = 0
    inserted_count = 0
    project_id_map = {}  # Maps item_id to projects_id
    
    for project in projects_data:
        item_id = project['item_id']
        project_name = project['project_name']
        developer_team = project['developer_team']
        
        # Map group to status
        status = project['group']
        if project['group'] == 'In Progress/Scheduled':
            status = 'In Progress'
        elif project['group'] == 'Backlog':
            status = 'Backlog'
        elif project['group'] == 'Pending Authorization':
            status = 'Pending Authorization'
        elif project['group'] == 'Internal Tracking':
            status = 'Internal Tracking'
        elif project['group'] == 'Closed':
            status = 'Closed'
        elif project['group'] == 'Removed/Cancelled/Duplicate':
            status = 'Removed'
        
        # Get team_id from mapping
        team_id = None
        if developer_team:
            team_id = team_map.get(developer_team)
            if not team_id:
                print(f"  Warning: No mapping for team '{developer_team}'")
        
        # Check if project exists by item_id
        cursor.execute(
            "SELECT projects_id FROM projects WHERE item_id = ?",
            (item_id,)
        )
        result = cursor.fetchone()
        
        if result:
            # Update existing project
            projects_id = result[0]
            project_id_map[item_id] = projects_id
            
            update_sql = "UPDATE projects SET status = ?, project_name = ?"
            params = [status, project_name]
            
            if team_id:
                update_sql += ", primary_team_id = ?"
                params.append(team_id)
            
            # Add prj_number if present
            if project.get('prj_number'):
                update_sql += ", prj_number = ?"
                params.append(project['prj_number'])
            
            update_sql += " WHERE projects_id = ?"
            params.append(projects_id)
            
            cursor.execute(update_sql, params)
            updated_count += 1
            
            team_str = f" (Team: {developer_team})" if developer_team else ""
            prj_str = f" [#{project.get('prj_number', '')}]" if project.get('prj_number') else ""
            print(f"  Updated: {project_name[:50]}{prj_str} -> {status}{team_str}")
        else:
            # Insert new project
            prj_number = project.get('prj_number')
            
            insert_sql = """
                INSERT INTO projects 
                (item_id, project_name, status, level, primary_team_id, primary_app_id, prj_number) 
                VALUES (?, ?, ?, 'TBD', ?, 26, ?)
            """
            cursor.execute(insert_sql, (item_id, project_name, status, team_id or 0, prj_number))
            
            cursor.execute("SELECT @@IDENTITY AS id")
            projects_id = cursor.fetchone()[0]
            project_id_map[item_id] = projects_id
            inserted_count += 1
            
            team_str = f" (Team: {developer_team})" if developer_team else ""
            prj_str = f" [#{prj_number}]" if prj_number else ""
            print(f"  Inserted: {project_name[:50]}{prj_str} -> {status}{team_str}")
    
    conn.commit()
    cursor.close()
    conn.close()
    
    print(f"\nProject Sync Summary:")
    print(f"  Updated: {updated_count} projects")
    print(f"  Inserted: {inserted_count} projects")
    print(f"  Total processed: {updated_count + inserted_count}")
    
    return project_id_map

def sync_assignments_to_db(projects_data, project_id_map, developers_db):
    """Sync developer assignments to MSSQL database"""
    
    conn = connect_to_mssql()
    cursor = conn.cursor()
    
    print(f"\nSyncing developer assignments...")
    
    inserted_count = 0
    updated_count = 0
    skipped_count = 0
    
    for project in projects_data:
        item_id = project['item_id']
        developers = project['developers']
        timeline = project['timeline']
        
        # Get project ID
        project_id = project_id_map.get(item_id)
        if not project_id:
            skipped_count += 1
            continue
        
        if not developers:
            continue
        
        # Process each developer
        for dev_name in developers:
            # Map Monday.com name to database name
            db_name = DEVELOPER_NAME_MAP.get(dev_name)
            if not db_name:
                print(f"  Warning: Unknown developer '{dev_name}'")
                continue
            
            developer_id = developers_db.get(db_name)
            if not developer_id:
                print(f"  Warning: Developer not found in DB: {db_name}")
                continue
            
            # Parse dates
            start_date = None
            end_date = None
            if timeline:
                start_date = timeline['start_date']
                end_date = timeline['end_date']
            
            # Check if assignment already exists
            cursor.execute("""
                SELECT assignments_id, start_date, end_date 
                FROM assignments 
                WHERE projects_id = ? AND developers_id = ?
            """, (project_id, developer_id))
            
            result = cursor.fetchone()
            
            if result:
                # Update existing assignment if no date in the tp database
                existing_id = result[0]
                existing_start = result[1]
                existing_end = result[2]
                
                # Convert to strings for comparison
                existing_start_str = existing_start.strftime('%Y-%m-%d') if existing_start else None
                existing_end_str = existing_end.strftime('%Y-%m-%d') if existing_end else None
                
                #if start_date != existing_start_str or end_date != existing_end_str:
                if not existing_start_str and start_date
                    cursor.execute("""
                        UPDATE assignments 
                        SET start_date = ?, end_date = ? 
                        WHERE assignments_id = ?
                    """, (start_date, end_date, existing_id))
                    updated_count += 1
            else:
                # Insert new assignment
                cursor.execute("""
                    INSERT INTO assignments 
                    (projects_id, developers_id, start_date, end_date, ratio) 
                    VALUES (?, ?, ?, ?, 1.0)
                """, (project_id, developer_id, start_date, end_date))
                inserted_count += 1
    
    conn.commit()
    cursor.close()
    conn.close()
    
    print(f"\nAssignment Sync Summary:")
    print(f"  Inserted: {inserted_count} assignments")
    print(f"  Updated: {updated_count} assignments")
    print(f"  Skipped: {skipped_count} projects (not found)")
    print(f"  Total processed: {inserted_count + updated_count}")

def main():
    print("=" * 70)
    print("Monday.com to MySQL Comprehensive Sync")
    print("=" * 70)
    print()
    
    # Get reference data from database
    print("Loading developers from database...")
    developers_db = get_developers_from_db()
    print(f"  Loaded {len(developers_db)} developers")
    
    print("Loading team name mappings...")
    team_map = get_team_name_map()
    print(f"  Loaded {len(team_map)} team mappings")
    print()
    
    # Query Monday.com
    print("Querying Monday.com for projects and assignments...")
    projects_data = query_monday_projects_and_assignments()
    print(f"\nRetrieved {len(projects_data)} projects from Monday.com")
    
    if projects_data:
        # Show sample
        print("\nSample project data:")
        for i, proj in enumerate(projects_data[:3]):
            timeline_str = ""
            if proj['timeline']:
                timeline_str = f" ({proj['timeline']['start_date']} to {proj['timeline']['end_date']})"
            print(f"  {i+1}. {proj['project_name'][:50]}")
            print(f"     Item ID: {proj['item_id']}")
            print(f"     Team: {proj['developer_team'] or 'None'}")
            print(f"     Developers: {', '.join(proj['developers']) if proj['developers'] else 'None'}")
            print(f"     Timeline: {timeline_str if timeline_str else 'Not set'}")
        
        # Sync projects
        print()
        project_id_map = sync_projects_to_db(projects_data, team_map)
        
        # Sync assignments
        sync_assignments_to_db(projects_data, project_id_map, developers_db)
    else:
        print("No projects found")
    
    print("\n" + "=" * 70)
    print("Sync completed")
    print("=" * 70)

if __name__ == "__main__":
    main()
