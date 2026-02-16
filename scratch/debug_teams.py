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
            monday_config['group_ids'] = [g.strip('"') for g in groups_str.split('","')]

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

# Connect to MSSQL
conn = pymssql.connect(
    server=db_config['server'],
    port=db_config['port'],
    user=db_config['username'],
    password=db_config['password'],
    database=db_config['database']
)
cursor = conn.cursor()

# Check team_name_map table
print("Teams in team_name_map:")
cursor.execute("SELECT * FROM team_name_map")
for row in cursor.fetchall():
    print(row)

# Monday.com API setup
API_URL = "https://api.monday.com/v2"
headers = {
    "Authorization": monday_config['api_token'],
    "Content-Type": "application/json"
}

# Get sample projects
query = """
query {
  boards(ids: %s) {
    groups(ids: %s) {
      items_page(limit: 5) {
        items {
          name
          column_values(ids: ["developer_team3"]) {
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

print("\nSample projects from Monday.com:")
for group in data['data']['boards'][0]['groups']:
    for item in group['items_page']['items']:
        dev_team = None
        for col in item['column_values']:
            if col['id'] == 'developer_team3':
                dev_team = col['text']
        print(f"  {item['name']}: {dev_team}")

cursor.close()
conn.close()
