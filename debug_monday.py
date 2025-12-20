#!/usr/bin/env python3
"""Debug script to understand Monday.com API structure"""

import requests
import json

# Read config
config = {}
with open('monday.txt', 'r') as f:
    for line in f:
        line = line.strip()
        if ':' in line:
            key, value = line.split(':', 1)
            config[key] = value

API_KEY = config['api_key']
BOARD_ID = config['mms-board-id']

MONDAY_API_URL = "https://api.monday.com/v2"
HEADERS = {
    "Authorization": API_KEY,
    "Content-Type": "application/json"
}

# Test query to see the structure
query = """
query ($boardId: ID!) {
    boards(ids: [$boardId]) {
        name
        groups {
            id
            title
            items_page(limit: 5) {
                cursor
                items {
                    id
                    name
                }
            }
        }
    }
}
"""

response = requests.post(
    MONDAY_API_URL,
    json={"query": query, "variables": {"boardId": BOARD_ID}},
    headers=HEADERS
)

print("Status:", response.status_code)
print()
data = response.json()
print(json.dumps(data, indent=2))

if 'data' in data and 'boards' in data['data']:
    board = data['data']['boards'][0]
    print("\n" + "="*70)
    print(f"Board: {board['name']}")
    print("="*70)
    for group in board['groups']:
        item_count = len(group['items_page']['items']) if group['items_page'] else 0
        print(f"\nGroup: {group['title']} (ID: {group['id']})")
        print(f"  Items on first page: {item_count}")
        if group['items_page'] and group['items_page']['cursor']:
            print(f"  Has more pages: Yes (cursor: {group['items_page']['cursor'][:20]}...)")
