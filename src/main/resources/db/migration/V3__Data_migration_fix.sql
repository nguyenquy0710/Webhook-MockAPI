-- Data migration script to ensure existing data compatibility
-- This script runs after schema changes to fix any data inconsistencies

-- Update any NULL delay_ms values to 0 for backward compatibility
UPDATE api_configs 
SET delay_ms = 0 
WHERE delay_ms IS NULL;

-- Ensure all users have proper default values
UPDATE users 
SET role = 'USER' 
WHERE role IS NULL;

UPDATE users 
SET enabled = TRUE 
WHERE enabled IS NULL;