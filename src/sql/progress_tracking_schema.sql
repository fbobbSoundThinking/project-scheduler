-- Progress Tracking Schema
-- Add progress tracking columns to projects and subitems tables

-- Project-level progress
ALTER TABLE projects ADD actual_dev_hours   DECIMAL(10,2) NULL;
ALTER TABLE projects ADD actual_wf_hours    DECIMAL(10,2) NULL;
ALTER TABLE projects ADD actual_qa_hours    DECIMAL(10,2) NULL;
ALTER TABLE projects ADD percent_complete   TINYINT NULL DEFAULT 0;
ALTER TABLE projects ADD actual_start_date  DATE NULL;
ALTER TABLE projects ADD actual_end_date    DATE NULL;

-- Subitem-level progress
ALTER TABLE subitems ADD actual_days        DECIMAL(10,2) NULL;
ALTER TABLE subitems ADD percent_complete   TINYINT NULL DEFAULT 0;
