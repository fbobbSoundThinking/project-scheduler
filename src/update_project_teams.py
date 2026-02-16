import requests
import pyodbc
import json

# Load Monday.com credentials
with open('./resources/monday.txt', 'r') as f:
    lines = f.readlines()
    api_token = lines[0].split('=')[1].strip()
    board_id = lines[1].split('=')[1].strip()

# Load MSSQL credentials
with open('./resources/tp-database.txt', 'r') as f:
    lines = f.readlines()
    server = lines[0].split('=')[1].strip()
    database = lines[1].split('=')[1].strip()
    username = lines[2].split('=')[1].strip()
    password = lines[3].split('=')[1].strip()

# Monday.com API setup
monday_url = "https://api.monday.com/v2"
headers = {
    "Authorization": api_token,
    "Content-Type": "application/json"
}

# Get teams from MSSQL
print("Fetching teams from MSSQL...")
conn_str = f'DRIVER={{ODBC Driver 18 for SQL Server}};SERVER={server};DATABASE={database};UID={username};PWD={password};TrustServerCertificate=yes;'
conn = pyodbc.connect(conn_str)
cursor = conn.cursor()

cursor.execute("SELECT team_id, team_name FROM teams")
teams = {row.team_name.strip().lower(): row.team_id for row in cursor.fetchall()}
print(f"Found {len(teams)} teams in database")

# Fetch projects from Monday.com with pagination
def fetch_all_items(board_id, group_ids):
    all_items = []
    cursor = None
    
    while True:
        cursor_param = f', cursor: "{cursor}"' if cursor else ''
        
        query = f'''
        {{
          boards(ids: {board_id}) {{
            groups(ids: {json.dumps(group_ids)}) {{
              id
              title
              items_page(limit: 100{cursor_param}) {{
                cursor
                items {{
                  id
                  name
                  column_values {{
                    id
                    text
                    value
                  }}
                }}
              }}
            }}
          }}
        }}
        '''
        
        response = requests.post(monday_url, json={'query': query}, headers=headers)
        data = response.json()
        
        for group in data['data']['boards'][0]['groups']:
            items = group['items_page']['items']
            all_items.extend(items)
            cursor = group['items_page']['cursor']
            print(f"Fetched {len(items)} items from {group['title']}, total: {len(all_items)}")
        
        if not cursor:
            break
    
    return all_items

# Fetch all projects from both groups
group_ids = ["topics", "new_group"]
print("\nFetching projects from Monday.com...")
items = fetch_all_items(board_id, group_ids)
print(f"Total projects fetched: {len(items)}")

# Update projects with team IDs
updated_count = 0
not_found_teams = set()
no_team_assigned = 0

for item in items:
    project_name = item['name']
    monday_id = item['id']
    
    # Find developer team column
    developer_team = None
    for col in item['column_values']:
        if col['id'] == 'dropdown':  # Developer Team column
            developer_team = col['text']
            break
    
    if not developer_team or developer_team.strip() == '':
        no_team_assigned += 1
        continue
    
    # Find matching team_id
    team_name_lower = developer_team.strip().lower()
    
    if team_name_lower in teams:
        team_id = teams[team_name_lower]
        
        # Update project in database
        cursor.execute("""
            UPDATE projects 
            SET primary_team_id = ? 
            WHERE monday_id = ?
        """, (team_id, monday_id))
        
        updated_count += 1
        print(f"✓ Updated '{project_name}' with team '{developer_team}' (ID: {team_id})")
    else:
        not_found_teams.add(developer_team)
        print(f"✗ Team '{developer_team}' not found for project '{project_name}'")

conn.commit()

print(f"\n{'='*60}")
print(f"Update Summary:")
print(f"{'='*60}")
print(f"Projects updated: {updated_count}")
print(f"Projects with no team assigned: {no_team_assigned}")
print(f"Teams not found in database: {len(not_found_teams)}")

if not_found_teams:
    print(f"\nMissing teams:")
    for team in sorted(not_found_teams):
        print(f"  - {team}")

cursor.close()
conn.close()

print("\nTeam update complete!")
