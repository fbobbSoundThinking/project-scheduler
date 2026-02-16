# Scheduler Application — Improvement Recommendations

A comprehensive set of improvements to enhance project scheduling and developer utilization across 220+ projects and 5 teams.

---

## 🔴 High-Impact Improvements

### 1. Project Dependencies & Critical Path

There is no way to model that Project B can't start until Project A finishes. Adding a `project_dependencies` table (predecessor/successor with dependency type) would enable critical path analysis and prevent unrealistic scheduling.

### 2. Progress Tracking (Actual vs. Planned)

The app tracks estimated hours (`devHours`, `qaHours`) but has no `actualHours` or `percentComplete` fields. Without this, you can't measure velocity, detect slippage, or forecast completion. Adding these fields to `projects` and `subitems` would close the feedback loop.

### 3. Capacity Planning Dashboard

The WorkloadHeatmap shows current utilization, but there is no **forward-looking capacity view** — e.g., "Team X has 120 available hours in March." A team-level capacity planning view that rolls up developer availability minus existing assignments would help answer "can we take on this project?"

### 4. What-If / Scenario Planning

There is no way to test schedule changes without committing them. A sandbox mode where PMs can drag assignments around and see the impact on the heatmap and Gantt before saving would significantly improve planning.

---

## 🟡 Medium-Impact Improvements

### 5. Developer Availability / Time-Off Tracking

The heatmap assumes 40h/week (32h effective) for every developer every week. There is no way to mark vacations, holidays, or reduced availability. This leads to over-scheduling.

### 6. Backend Analytics Endpoints

All analytics (workload aggregation, utilization %) are computed client-side in Angular. Moving these to the API would enable:

- `/api/reports/utilization?team=X&from=...&to=...`
- `/api/reports/capacity?team=X&weeks=12`
- `/api/reports/overloaded-developers`
- Faster rendering and consistent calculations.

### 7. Priority-Based Scheduling Assistance

Projects have `priority` and `urgency` fields, but these aren't used in any scheduling logic. An auto-suggest feature that recommends developer assignments based on team, availability, and project priority would reduce manual work across 220+ projects.

### 8. Milestone Tracking

Subitems have dev/QA/deployment dates, but there is no explicit milestone concept. Adding milestones (e.g., "Design Complete," "Code Complete," "UAT Sign-off") with target vs. actual dates would improve visibility into where projects stand.

### 9. Risk / Blockers Flagging

No mechanism to flag at-risk projects. A risk indicator (auto-calculated from overdue dates or overloaded developers, or manually set) would help PMs focus attention. The Gantt and project list could highlight these visually.

---

## 🟢 Quick Wins

### 10. Gantt Drag-and-Drop

The Gantt view is read-only. Adding drag-to-reschedule and resize-to-change-duration on the bars would make it a true planning tool rather than just a visualization.

### 11. Export / Reporting

No export capability exists. Adding CSV/PDF export of the project list, Gantt, and heatmap would help in stakeholder reporting and status meetings.

### 12. Assignment Conflict Warnings

The heatmap shows overloads after the fact, but there is no warning when *creating* an assignment that would push a developer over capacity. An inline warning during assignment creation would prevent over-scheduling.

### 13. Historical Velocity Tracking

With `devHours` estimates and no actuals, there is no way to calibrate future estimates. Even simple tracking of "estimated vs. actual completion date" per project would build a velocity baseline over time.

### 14. Saved Views / Filters

The `FilterStateService` shares state across views but doesn't persist named filter presets. PMs managing 5 teams likely have recurring views ("my team's active projects") that shouldn't need re-filtering each session.

---

## Recommended Priority

**Top 3 for immediate implementation:**

1. **Project Dependencies (#1)** — addresses the biggest blind spot in scheduling accuracy
2. **Progress Tracking (#2)** — enables velocity measurement and slippage detection
3. **Assignment Conflict Warnings (#12)** — prevents over-scheduling at the point of action
