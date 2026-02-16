-- =========================================
-- Add Subitems Support
-- =========================================

-- Create subitems table
CREATE TABLE subitems (
    subitems_id INT IDENTITY(1,1) PRIMARY KEY,
    subitem_id NVARCHAR(50) NOT NULL UNIQUE,  -- Monday.com subitem ID
    projects_id INT NOT NULL,                  -- FK to projects table
    subitem_name NVARCHAR(256),
    status NVARCHAR(50),
    [group] NVARCHAR(50),                      -- Inherited from parent project
    CONSTRAINT fk_subitems_project FOREIGN KEY (projects_id) 
        REFERENCES projects(projects_id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_subitems_project ON subitems(projects_id);
CREATE INDEX idx_subitems_monday_id ON subitems(subitem_id);

-- Add subitems_id to assignments table (nullable, for subitem assignments)
ALTER TABLE assignments ADD subitems_id INT NULL;
ALTER TABLE assignments ADD CONSTRAINT fk_assignments_subitems 
    FOREIGN KEY (subitems_id) REFERENCES subitems(subitems_id) ON DELETE CASCADE;
CREATE INDEX idx_assignments_subitem ON assignments(subitems_id);

-- Add constraint: assignment must belong to either project OR subitem (not both, not neither)
-- Note: This is enforced at application level in Java, not via CHECK constraint
-- because SQL Server CHECK constraints can be complex with nullable FKs
