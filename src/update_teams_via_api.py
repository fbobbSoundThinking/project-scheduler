import json
import requests
from datetime import datetime

# Read Monday.com credentials
with open('./resources/monday.txt', 'r') as f:
    for line in f:
        if 'MONDAY_API_KEY' in line:
            MONDAY_API_KEY = line.split('=')[1].strip()
        elif 'BOARD_ID' in line:
            BOARD_ID = line.split('=')[1].strip()

API_URL = "http://localhost:8080/api"

print("Fetching teams from database...")
teams_response = requests.get(f"{API_URL}/teams")
teams_dict = {team['teamName']: team['teamId'] for team in teams_response.json()}
print(f"Found {len(teams_dict)} teams")

print("\nFetching projects from Monday.com...")
query = f'''
query {{
  boards(ids: [{BOARD_ID}]) {{
    groups(ids: ["topics", "new_group49814"]) {{
      title
      items_page(limit: 500) {{
        items {{
          id
          name
          column_values {{
            id
            text
          }}
        }}
      }}
    }}
  }}
}}
'''

response = requests.post(
    'https://api.monday.com/v2',
    json={'query': query},
    headers={'Authorization': MONDAY_API_KEY}
)

data = response.json()

updates = []
for group in data['data']['boards'][0]['groups']:
    for item in group['items_page']['items']:
        project_name = item['name']
        developer_team = None
        
        for col in item['column_values']:
            if col['id'] == 'dropdown':
                developer_team = col['text']
        
        if developer_team and developer_team in teams_dict:
            updates.append((project_name, developer_team, teams_dict[developer_team]))

print(f"\nUpdating {len(updates)} projects...")
for project_name, team_name, team_id in updates:
    # Get project from API
    projects_response = requests.get(f"{API_URL}/projects")
    project = next((p for p in projects_response.json() if p['projectName'] == project_name), None)
    
    if project:
        project['primaryTeamId'] = team_id
        update_response = requests.put(f"{API_URL}/projects/{project['projectId']}", json=project)
        if update_response.status_code == 200:
            print(f"✓ {project_name} -> {team_name}")
        else:
            print(f"✗ Failed to update {project_name}: {update_response.text}")

print("\nUpdate complete!")
