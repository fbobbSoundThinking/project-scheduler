#!/usr/bin/env python3
"""
Sync Monday.com projects to MS SQL Server tp database
Reads configuration from monday.txt and tp-database.txt
"""

import requests
import pymssql
import json
from datetime import datetime

# Read Monday.com configuration
def read_config(filename):
    config = {}
    with open(filename, 'r') as f:
        for line in f:
            line = line.strip()
            if ':' in line:
                key, value = line.split(':', 1)
                config[key] = value
    return config

# Read configurations
monday_config = read_config('./resources/monday.txt')
db_config = read_config('./resources/tp-database.txt')

# Parse Monday.com config
API_KEY = monday_config['api_key']
BOARD_ID = monday_config['mms-board-id']
GROUPS = [g.strip().strip('"') for g in monday_config['groups'].split(',')]

print("Monday.com Configuration:")
print(f"  Board ID: {BOARD_ID}")
print(f"  Groups: {GROUPS}")
print()

# Monday.com API endpoint
MONDAY_API_URL = "https://api.monday.com/v2"
HEADERS = {
    "Authorization": API_KEY,
    "Content-Type": "application/json"
}

def query_monday_projects():
    """Query Monday.com for projects in specified groups with pagination"""
    
    all_projects = []
    
    # For each group, paginate through all items
    for group_name in GROUPS:
        print(f"Querying group: {group_name}")
        
        cursor = None
        group_projects = []
        page_num = 1
        
        while True:
            # Query all groups and filter, with pagination
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
                "limit": 500,  # Max items per page
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
                
                # Find the specific group
                for group in board['groups']:
                    if group['title'] == group_name:
                        items_page = group['items_page']
                        items = items_page['items'] if items_page else []
                        
                        if items:
                            found_items = True
                            print(f"  Page {page_num}: Retrieved {len(items)} items")
                            
                            for item in items:
                                project = {
                                    'monday_id': item['id'],
                                    'project_name': item['name'],
                                    'group': group_name,
                                    'columns': {}
                                }
                                
                                # Extract column values
                                for col in item['column_values']:
                                    if col['text']:
                                        project['columns'][col['id']] = col['text']
                                
                                group_projects.append(project)
                        
                        # Get the cursor for next page
                        if items_page:
                            next_cursor = items_page.get('cursor')
                        break
            
            # Continue if we found items and there's a next cursor
            if found_items and next_cursor:
                cursor = next_cursor
                page_num += 1
            else:
                break
        
        print(f"  Total items in '{group_name}': {len(group_projects)}")
        all_projects.extend(group_projects)
    
    return all_projects

def connect_to_mssql():
    """Connect to MS SQL Server"""
    conn = pymssql.connect(
        server=db_config['host'],
        port=int(db_config['port']),
        user=db_config['user'],
        password=db_config['password'],
        database='tp'
    )
    return conn

def update_projects_in_db(projects):
    """Update projects in MS SQL Server"""
    
    if not projects:
        print("No projects to update")
        return
    
    conn = connect_to_mssql()
    cursor = conn.cursor()
    
    print(f"\nUpdating {len(projects)} projects in database...")
    
    updated_count = 0
    inserted_count = 0
    
    for project in projects:
        project_name = project['project_name']
        status = project['columns'].get('status', project['group'])
        
        # Map Monday.com group to status
        if project['group'] == 'In Progress/Scheduled':
            status = 'In Progress'
        elif project['group'] == 'Backlog':
            status = 'Backlog'
        elif project['group'] == 'Pending Authorization':
            status = 'Pending Authorization'
        elif project['group'] == 'Internal Tracking':
            status = 'Internal Tracking'
        
        # Check if project exists by name
        cursor.execute(
            "SELECT projects_id FROM dbo.projects WHERE project_name = %s",
            (project_name,)
        )
        result = cursor.fetchone()
        
        if result:
            # Update existing project
            cursor.execute(
                """UPDATE dbo.projects 
                   SET status = %s 
                   WHERE projects_id = %s""",
                (status, result[0])
            )
            updated_count += 1
            print(f"  Updated: {project_name[:60]} -> {status}")
        else:
            # Insert new project
            cursor.execute(
                """INSERT INTO dbo.projects 
                   (project_name, status, level, primary_team_id, primary_app_id) 
                   VALUES (%s, %s, 'TBD', 0, 26)""",
                (project_name, status)
            )
            inserted_count += 1
            print(f"  Inserted: {project_name[:60]} -> {status}")
    
    conn.commit()
    cursor.close()
    conn.close()
    
    print(f"\nSummary:")
    print(f"  Updated: {updated_count} projects")
    print(f"  Inserted: {inserted_count} projects")
    print(f"  Total processed: {updated_count + inserted_count}")

def main():
    print("=" * 70)
    print("Monday.com to MS SQL Server Sync")
    print("=" * 70)
    print()
    
    # Query Monday.com
    print("Querying Monday.com...")
    projects = query_monday_projects()
    print(f"\nRetrieved {len(projects)} projects from Monday.com")
    
    if projects:
        print("\nSample project data:")
        for i, proj in enumerate(projects[:3]):
            print(f"  {i+1}. {proj['project_name'][:60]} - Group: {proj['group']}")
        
        # Update database
        print()
        update_projects_in_db(projects)
    else:
        print("No projects found in specified groups")
    
    print("\n" + "=" * 70)
    print("Sync completed")
    print("=" * 70)

if __name__ == "__main__":
    main()
