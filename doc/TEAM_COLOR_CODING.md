# Team Color Coding Implementation

## Feature Overview
Added **dynamic** color-coded backgrounds for the "Primary Team" column. Colors are automatically assigned based on team names loaded from the API, ensuring consistent visual identification across the application.

## Dynamic Color Generation

### How It Works
1. **API-Driven**: Team names are fetched from `/api/teams` endpoint
2. **Automatic Assignment**: Each unique team name gets a color from a predefined palette
3. **Consistent**: Same team always gets the same color across sessions
4. **Scalable**: Automatically handles any number of teams

### Color Palette (15 colors)
| Order | Color Name   | Hex Code | Visual |
|-------|--------------|----------|--------|
| 1     | Blue         | #2196F3  | 🔵     |
| 2     | Green        | #4CAF50  | 🟢     |
| 3     | Orange       | #FF9800  | 🟠     |
| 4     | Purple       | #9C27B0  | 🟣     |
| 5     | Red          | #F44336  | 🔴     |
| 6     | Cyan         | #00BCD4  | 🔷     |
| 7     | Pink         | #E91E63  | 🩷     |
| 8     | Blue Grey    | #607D8B  | ⚫     |
| 9     | Brown        | #795548  | 🟤     |
| 10    | Indigo       | #3F51B5  | 💙     |
| 11    | Teal         | #009688  | 💚     |
| 12    | Amber        | #FFC107  | 🟡     |
| 13    | Light Green  | #8BC34A  | 💚     |
| 14    | Deep Orange  | #FF5722  | 🧡     |
| 15    | Deep Purple  | #673AB7  | 💜     |

**Note**: Colors cycle if there are more than 15 teams. Unassigned projects show grey (#9e9e9e).

## Implementation Details

### Files Modified
1. **project-list.html** - Added `[ngStyle]="getTeamStyle(project.primaryTeamId)"`
2. **project-list.ts** - Added dynamic color generation and style methods
3. **project-list.scss** - Simplified to use dynamic inline styles

### Code Changes

#### TypeScript Methods
```typescript
// Generate color map when teams load
generateTeamColors(teamNames: string[]): void {
  const colors = ['#2196F3', '#4CAF50', '#FF9800', ...];
  teamNames.forEach((teamName, index) => {
    this.teamColorMap[teamName] = colors[index % colors.length];
  });
}

// Get CSS class for team
getTeamClass(teamId?: number): string {
  const teamName = this.getTeamName(teamId);
  return teamName === '-' ? 'team-none' : 'team-dynamic';
}

// Get inline styles for team badge
getTeamStyle(teamId?: number): { [key: string]: string } {
  const teamName = this.getTeamName(teamId);
  if (teamName === '-') return {};
  
  const color = this.teamColorMap[teamName] || '#795548';
  return {
    'background-color': color,
    'color': 'white'
  };
}
```

#### HTML Template
```html
<span class="team-name" 
      [ngClass]="getTeamClass(project.primaryTeamId)" 
      [ngStyle]="getTeamStyle(project.primaryTeamId)">
  {{getTeamName(project.primaryTeamId)}}
</span>
```

## Benefits
- ✅ **Fully Dynamic**: No hardcoded team names - adapts to any team structure
- ✅ **API-Driven**: Single source of truth from backend database
- ✅ **Automatic**: Colors assigned automatically when teams are loaded
- ✅ **Consistent**: Same team always gets the same color
- ✅ **Scalable**: Works with unlimited number of teams
- ✅ **Quick Visual ID**: Instantly identify team ownership
- ✅ **Material Design**: Uses Google's Material color palette
- ✅ **Grey Default**: Unassigned teams clearly visible in grey

## Usage
Colors are automatically applied when the application loads. No configuration or user interaction required.

## Technical Notes
- Colors are stored in `teamColorMap: { [key: string]: string }`
- Color assignment happens in `loadTeams()` method
- Uses modulo arithmetic to cycle through colors if teams exceed palette size
- Falls back to brown (#795548) if team not in color map

## Future Enhancements
- [ ] Add team legend/key showing all teams and their colors
- [ ] Allow admin users to customize team colors
- [ ] Persist color preferences to user settings
- [ ] Add color-coded team filtering
- [ ] Export team color scheme configuration

