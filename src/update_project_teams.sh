#!/bin/bash

# Read credentials
MONDAY_API_KEY=$(grep MONDAY_API_KEY monday.txt | cut -d'=' -f2 | tr -d ' ')
BOARD_ID=$(grep BOARD_ID monday.txt | cut -d'=' -f2 | tr -d ' ')
DB_PASSWORD=$(grep PASSWORD tp-database.txt | cut -d'=' -f2 | tr -d ' ')

echo "Fetching projects from Monday.com..."

# Fetch all projects with Developer Team from Monday.com
QUERY='query {
  boards(ids: ['$BOARD_ID']) {
    groups(ids: ["topics", "new_group49814"]) {
      title
      items_page(limit: 500) {
        cursor
        items {
          id
          name
          column_values {
            id
            text
            type
          }
        }
      }
    }
  }
}'

RESPONSE=$(curl -s "https://api.monday.com/v2" \
  -H "Authorization: $MONDAY_API_KEY" \
  -H "Content-Type: application/json" \
  -d "{\"query\": \"$QUERY\"}")

# Get teams mapping from database
echo "Fetching teams from database..."
TEAMS=$(sqlcmd -S "localhost,1433" -U sa -P "$DB_PASSWORD" -d tp -Q "SET NOCOUNT ON; SELECT team_id, team_name FROM teams;" -h -1 -s "|" -W)

echo "$RESPONSE" | python3 -c "
import json
import sys
import subprocess

response = json.load(sys.stdin)

# Parse teams from database
teams = {}
teams_output = '''$TEAMS'''
for line in teams_output.strip().split('\n'):
    if line.strip() and '|' in line:
        parts = line.strip().split('|')
        if len(parts) >= 2:
            team_id = parts[0].strip()
            team_name = parts[1].strip()
            teams[team_name] = team_id

print(f'Found {len(teams)} teams in database')

# Process projects
updates = []
for group in response['data']['boards'][0]['groups']:
    for item in group['items_page']['items']:
        project_name = item['name']
        developer_team = None
        
        for col in item['column_values']:
            if col['id'] == 'dropdown':  # Developer Team
                developer_team = col['text']
        
        if developer_team and developer_team in teams:
            team_id = teams[developer_team]
            updates.append((team_id, project_name))
            print(f'{project_name} -> {developer_team} (ID: {team_id})')

# Generate SQL
if updates:
    print(f'\nUpdating {len(updates)} projects...')
    for team_id, project_name in updates:
        escaped_name = project_name.replace(\"'\", \"''\")
        sql = f\"UPDATE projects SET primary_team_id = {team_id} WHERE project_name = '{escaped_name}';\"
        result = subprocess.run(['sqlcmd', '-S', 'localhost,1433', '-U', 'sa', '-P', '$DB_PASSWORD', '-d', 'tp', '-Q', sql], 
                              capture_output=True, text=True)
        if result.returncode != 0:
            print(f'Error updating {project_name}: {result.stderr}')
    print('Update complete!')
else:
    print('No projects to update')
"

echo ""
echo "Team update complete!"
