import json
import requests

# Read Monday.com credentials
with open('./resources/monday.txt', 'r') as f:
    for line in f:
        if 'MONDAY_API_KEY' in line:
            MONDAY_API_KEY = line.split('=')[1].strip()
        elif 'BOARD_ID' in line:
            BOARD_ID = line.split('=')[1].strip()

print("-- SQL Script to Update Project Teams")
print("-- Generated from Monday.com data\n")

print("-- Fetch projects from Monday.com...")
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

updates = {}
for group in data['data']['boards'][0]['groups']:
    for item in group['items_page']['items']:
        project_name = item['name']
        developer_team = None
        
        for col in item['column_values']:
            if col['id'] == 'dropdown':
                developer_team = col['text']
        
        if developer_team:
            updates[project_name] = developer_team

print(f"-- Found {len(updates)} projects with developer teams\n")
print("-- Batch update statement:")
print("UPDATE p")
print("SET p.primary_team_id = t.team_id")
print("FROM projects p")
print("INNER JOIN teams t ON t.team_name = (")
print("    CASE p.project_name")
for project_name, team_name in sorted(updates.items()):
    escaped_name = project_name.replace("'", "''")
    escaped_team = team_name.replace("'", "''")
    print(f"        WHEN '{escaped_name}' THEN '{escaped_team}'")
print("    END")
print(");")
