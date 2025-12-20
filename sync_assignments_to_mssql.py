#!/usr/bin/env python3
"""
Sync Monday.com developer assignments to MS SQL Server tp database
Creates/updates records in the assignments table based on:
- Project assignments from Monday.com
- Developer Timeline (start/end dates)
- Dev Resource (assigned developers)
"""

import requests
import pymssql
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

monday_config = read_config('monday.txt')
db_config = read_config('tp-database.txt')

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
    'Harish Gundameedi': 'HARISHKUMAR GUNDAMEEDI',  # Short version
    'Krishna Karangula': 'KRISHNA KARANGULA',
    'Kunal Tamang': 'KUNAL TAMANG',
    'Romal Patel': 'ROMAL PATEL',
    'Sandhya Mankala': 'SANDHYA MANKALA',
    'Anusha Kallu': 'ANUSHA KALLU',
    'Anusha Reddy Kallu': 'ANUSHA KALLU',  # Full name version
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
    'Chandra Gottumukkala': 'CHANDRA GOTTUMUKKALA',  # Short version
    # Note: These developers are not in the database:
    # 'Tariq Islam', 'Frank Bobb', 'Jorge Abrantes', 'Archana Avvaru'
}

def get_developers_from_db():
    """Get all developers from database with their IDs"""
    conn = pymssql.connect(
        server=db_config['host'],
        port=int(db_config['port']),
        user=db_config['user'],
        password=db_config['password'],
        database='tp'
    )
    cursor = conn.cursor()
    
    cursor.execute("""
        SELECT developers_id, first_name, last_name 
        FROM dbo.developers 
        ORDER BY last_name, first_name
    """)
    
    developers = {}
    for row in cursor.fetchall():
        full_name = f"{row[1]} {row[2]}"
        developers[full_name] = row[0]
    
    cursor.close()
    conn.close()
    
    return developers

def get_projects_from_db():
    """Get all projects from database with their IDs"""
    conn = pymssql.connect(
        server=db_config['host'],
        port=int(db_config['port']),
        user=db_config['user'],
        password=db_config['password'],
        database='tp'
    )
    cursor = conn.cursor()
    
    cursor.execute("SELECT projects_id, project_name FROM dbo.projects")
    
    projects = {}
    for row in cursor.fetchall():
        projects[row[1]] = row[0]
    
    cursor.close()
    conn.close()
    
    return projects

def query_monday_assignments():
    """Query Monday.com for project assignments with developer timeline"""
    
    all_assignments = []
    
    for group_name in GROUPS:
        print(f"Querying group: {group_name}")
        
        cursor = None
        page_num = 1
        
        while True:
            # Query with people and timeline columns
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
                                column_values(ids: ["people", "timeline_1__1"]) {
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
                            print(f"  Page {page_num}: Processing {len(items)} items")
                            
                            for item in items:
                                project_name = item['name']
                                developers = []
                                timeline = None
                                
                                for col in item['column_values']:
                                    if col['id'] == 'people' and col['value']:
                                        # Parse people column
                                        try:
                                            people_data = json.loads(col['value'])
                                            # Get developer names from text field
                                            if col['text']:
                                                dev_names = [name.strip() for name in col['text'].split(',')]
                                                developers = dev_names
                                        except:
                                            pass
                                    
                                    elif col['id'] == 'timeline_1__1' and col['value']:
                                        # Parse timeline column
                                        try:
                                            timeline_data = json.loads(col['value'])
                                            if 'from' in timeline_data and 'to' in timeline_data:
                                                timeline = {
                                                    'start_date': timeline_data['from'],
                                                    'end_date': timeline_data['to']
                                                }
                                        except:
                                            pass
                                
                                # Create assignment records
                                if developers:
                                    assignment = {
                                        'project_name': project_name,
                                        'developers': developers,
                                        'timeline': timeline,
                                        'group': group_name
                                    }
                                    all_assignments.append(assignment)
                        
                        if items_page:
                            next_cursor = items_page.get('cursor')
                        break
            
            if found_items and next_cursor:
                cursor = next_cursor
                page_num += 1
            else:
                break
        
        print(f"  Completed group: {group_name}")
    
    return all_assignments

def sync_assignments_to_db(assignments, developers_db, projects_db):
    """Sync assignments to MS SQL Server database"""
    
    if not assignments:
        print("No assignments to sync")
        return
    
    conn = pymssql.connect(
        server=db_config['host'],
        port=int(db_config['port']),
        user=db_config['user'],
        password=db_config['password'],
        database='tp'
    )
    cursor = conn.cursor()
    
    print(f"\nSyncing {len(assignments)} project assignments...")
    
    inserted_count = 0
    updated_count = 0
    skipped_count = 0
    
    for assignment in assignments:
        project_name = assignment['project_name']
        developers = assignment['developers']
        timeline = assignment['timeline']
        
        # Get project ID
        project_id = projects_db.get(project_name)
        if not project_id:
            # Try to find project by partial match
            for proj_name, proj_id in projects_db.items():
                if proj_name.startswith(project_name[:50]):
                    project_id = proj_id
                    break
        
        if not project_id:
            skipped_count += 1
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
                FROM dbo.assignments 
                WHERE projects_id = %s AND developers_id = %s
            """, (project_id, developer_id))
            
            result = cursor.fetchone()
            
            if result:
                # Update existing assignment if dates changed
                existing_id = result[0]
                existing_start = result[1]
                existing_end = result[2]
                
                # Convert to strings for comparison
                existing_start_str = existing_start.strftime('%Y-%m-%d') if existing_start else None
                existing_end_str = existing_end.strftime('%Y-%m-%d') if existing_end else None
                
                if start_date != existing_start_str or end_date != existing_end_str:
                    cursor.execute("""
                        UPDATE dbo.assignments 
                        SET start_date = %s, end_date = %s 
                        WHERE assignments_id = %s
                    """, (start_date, end_date, existing_id))
                    updated_count += 1
            else:
                # Insert new assignment
                cursor.execute("""
                    INSERT INTO dbo.assignments 
                    (projects_id, developers_id, start_date, end_date, ratio) 
                    VALUES (%s, %s, %s, %s, 1.0)
                """, (project_id, developer_id, start_date, end_date))
                inserted_count += 1
    
    conn.commit()
    cursor.close()
    conn.close()
    
    print(f"\nSync Summary:")
    print(f"  Inserted: {inserted_count} assignments")
    print(f"  Updated: {updated_count} assignments")
    print(f"  Skipped: {skipped_count} projects (not found in DB)")
    print(f"  Total processed: {inserted_count + updated_count}")

def main():
    print("=" * 70)
    print("Monday.com Developer Assignments Sync")
    print("=" * 70)
    print()
    
    # Get reference data from database
    print("Loading developers from database...")
    developers_db = get_developers_from_db()
    print(f"  Loaded {len(developers_db)} developers")
    
    print("Loading projects from database...")
    projects_db = get_projects_from_db()
    print(f"  Loaded {len(projects_db)} projects")
    print()
    
    # Query Monday.com
    print("Querying Monday.com for assignments...")
    assignments = query_monday_assignments()
    print(f"\nRetrieved {len(assignments)} project assignments from Monday.com")
    
    if assignments:
        # Show sample
        print("\nSample assignments:")
        for i, assignment in enumerate(assignments[:3]):
            timeline_str = ""
            if assignment['timeline']:
                timeline_str = f" ({assignment['timeline']['start_date']} to {assignment['timeline']['end_date']})"
            print(f"  {i+1}. {assignment['project_name'][:50]}")
            print(f"     Developers: {', '.join(assignment['developers'])}")
            print(f"     Timeline: {timeline_str if timeline_str else 'Not set'}")
        
        # Sync to database
        print()
        sync_assignments_to_db(assignments, developers_db, projects_db)
    else:
        print("No assignments found")
    
    print("\n" + "=" * 70)
    print("Sync completed")
    print("=" * 70)

if __name__ == "__main__":
    main()
