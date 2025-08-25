-- Data migration script to ensure existing data compatibility
-- This script runs after schema changes to fix any data inconsistencies

-- Ensure NULL delay_ms are set to 0 (requires column to exist)
UPDATE api_configs 
SET delay_ms = 0 
WHERE delay_ms IS NULL;

-- Set default role for users missing it
UPDATE users 
SET role = 'USER' 
WHERE role IS NULL;

-- Enable users if enabled is NULL (assuming BOOLEAN type)
UPDATE users 
SET enabled = TRUE 
WHERE enabled IS NULL;