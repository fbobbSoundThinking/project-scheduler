-- Project Dependencies Schema
-- Create table for managing project predecessor/successor relationships

CREATE TABLE project_dependencies (
    project_dependencies_id INT IDENTITY(1,1) PRIMARY KEY,
    predecessor_id          INT NOT NULL,
    successor_id            INT NOT NULL,
    dependency_type         VARCHAR(20) NOT NULL DEFAULT 'FINISH_TO_START',
    CONSTRAINT FK_dep_predecessor FOREIGN KEY (predecessor_id) REFERENCES projects(projects_id),
    CONSTRAINT FK_dep_successor   FOREIGN KEY (successor_id) REFERENCES projects(projects_id),
    CONSTRAINT UQ_dependency      UNIQUE (predecessor_id, successor_id),
    CONSTRAINT CHK_no_self_dep    CHECK (predecessor_id <> successor_id)
);
