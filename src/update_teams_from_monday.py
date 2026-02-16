import requests
import pymssql
import json

# Load Monday.com credentials
monday_config = {}
with open('./resources/monday.txt', 'r') as f:
    for line in f:
        line = line.strip()
        if line.startswith('api_key:'):
            monday_config['api_token'] = line.split('api_key:')[1]
        elif line.startswith('mms-board-id:'):
            monday_config['board_id'] = line.split('mms-board-id:')[1]
        elif line.startswith('groups:'):
            groups_str = line.split('groups:')[1]
            # Map friendly names to actual group IDs
            group_map = {
                'In Progress/Scheduled': 'new_group29179',
                'Backlog': 'new_group94838',
                'Internal Tracking': 'new_group47254',
                'Pending Authorization': 'new_group'
            }
            friendly_names = [g.strip('"') for g in groups_str.split('","')]
            monday_config['group_ids'] = [group_map.get(name, name) for name in friendly_names]

# Load MSSQL credentials
db_config = {}
with open('./resources/tp-database.txt', 'r') as f:
    for line in f:
        line = line.strip()
        if line.startswith('host:'):
            db_config['server'] = line.split('host:', 1)[1]
        elif line.startswith('port:'):
            db_config['port'] = line.split('port:', 1)[1]
        elif line.startswith('user:'):
            db_config['username'] = line.split('user:', 1)[1]
        elif line.startswith('password:'):
            db_config['password'] = line.split('password:', 1)[1]

db_config['database'] = 'tp'

# Monday.com API setup
API_URL = "https://api.monday.com/v2"
headers = {
    "Authorization": monday_config['api_token'],
    "Content-Type": "application/json"
}

# Get all projects with Developer Team from Monday.com
query = """
query {
  boards(ids: %s) {
    groups(ids: %s) {
      items_page(limit: 500) {
        items {
          id
          name
          column_values(ids: ["text", "status_150"]) {
            id
            text
          }
        }
      }
    }
  }
}
""" % (monday_config['board_id'], json.dumps(monday_config['group_ids']))

response = requests.post(API_URL, json={'query': query}, headers=headers)
data = response.json()

# Connect to MSSQL
conn = pymssql.connect(
    server=db_config['server'],
    port=db_config['port'],
    user=db_config['username'],
    password=db_config['password'],
    database=db_config['database']
)
cursor = conn.cursor()

# Process each project
updates = []
developer_teams_found = set()
for group in data['data']['boards'][0]['groups']:
    for item in group['items_page']['items']:
        project_name = item['name']
        developer_team = None
        
        for col in item['column_values']:
            if col['id'] == 'status_150' and col['text']:
                # Get first team if multiple
                teams = col['text'].split(',')
                developer_team = teams[0].strip()
                developer_teams_found.add(developer_team)
                break
        
        if developer_team:
            # Get team_id from team_name_map
            cursor.execute("""
                SELECT teams_id 
                FROM team_name_map 
                WHERE monday_team_name = %s
            """, (developer_team,))
            
            result = cursor.fetchone()
            if result:
                team_id = result[0]
                updates.append((team_id, project_name))
                print(f"Will update '{project_name}' -> Team ID: {team_id} ({developer_team})")
            else:
                print(f"No mapping found for team: '{developer_team}' (Project: {project_name})")

# Display all unique Developer Team values found
print(f"\n=== Unique Developer Team values found ({len(developer_teams_found)}) ===")
for team in sorted(developer_teams_found):
    print(f"  - {team}")

# Execute updates
print(f"\nUpdating {len(updates)} projects...")
for team_id, project_name in updates:
    cursor.execute("""
        UPDATE projects 
        SET primary_team_id = %s 
        WHERE project_name = %s
    """, (team_id, project_name))

conn.commit()
print(f"Successfully updated {len(updates)} projects!")

cursor.close()
conn.close()
