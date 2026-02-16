-- Add date columns to subitems table to match project fields
-- Run this after subitems_schema.sql

ALTER TABLE subitems
ADD estimated_days INT NULL;

ALTER TABLE subitems
ADD dev_start_date DATE NULL;

ALTER TABLE subitems
ADD dev_end_date DATE NULL;

ALTER TABLE subitems
ADD qa_start_date DATE NULL;

ALTER TABLE subitems
ADD qa_end_date DATE NULL;

ALTER TABLE subitems
ADD target_deployment_date DATE NULL;

GO
