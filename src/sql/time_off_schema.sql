-- Time Off and Company Holidays Schema
-- Tracks developer absences and company-wide holidays for capacity calculations

-- Developer time-off entries (vacations, PTO, sick days)
CREATE TABLE developer_time_off (
    developer_time_off_id INT IDENTITY(1,1) PRIMARY KEY,
    developers_id INT NOT NULL FOREIGN KEY REFERENCES developers(developers_id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    type NVARCHAR(20) NOT NULL,  -- PTO, VACATION, SICK, OTHER
    note NVARCHAR(200) NULL,
    CONSTRAINT chk_time_off_dates CHECK (end_date >= start_date)
);

-- Company holidays (applied to all developers)
CREATE TABLE company_holidays (
    company_holiday_id INT IDENTITY(1,1) PRIMARY KEY,
    holiday_date DATE NOT NULL UNIQUE,
    name NVARCHAR(100) NOT NULL
);

-- Index for efficient queries by developer and date range
CREATE INDEX idx_developer_time_off_developer_dates 
    ON developer_time_off(developers_id, start_date, end_date);

-- Index for efficient holiday queries by date
CREATE INDEX idx_company_holidays_date 
    ON company_holidays(holiday_date);
