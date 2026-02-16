-- Scenario Planning Schema
-- Two new tables for draft assignment changes (deltas from live data)

-- Scenario metadata
CREATE TABLE scenarios (
    scenario_id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(500),
    created_by NVARCHAR(100),
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    status NVARCHAR(20) DEFAULT 'DRAFT'  -- DRAFT, APPLIED, ARCHIVED
);

-- Scenario assignment changes (deltas from live data)
CREATE TABLE scenario_assignments (
    scenario_assignment_id INT IDENTITY(1,1) PRIMARY KEY,
    scenario_id INT NOT NULL FOREIGN KEY REFERENCES scenarios(scenario_id) ON DELETE CASCADE,
    change_type NVARCHAR(10) NOT NULL,  -- ADD, MODIFY, DELETE
    -- For MODIFY/DELETE: references the live assignment being changed
    original_assignment_id INT NULL FOREIGN KEY REFERENCES assignments(assignments_id),
    -- Assignment fields (used by ADD and MODIFY)
    projects_id INT NULL FOREIGN KEY REFERENCES projects(projects_id),
    subitems_id INT NULL,
    developers_id INT NULL FOREIGN KEY REFERENCES developers(developers_id),
    start_date DATE NULL,
    end_date DATE NULL,
    ratio DECIMAL(5,2) NULL
);
