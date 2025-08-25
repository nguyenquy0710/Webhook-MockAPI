-- Add delay_ms column to api_configs table
-- This migration adds support for response delay functionality

ALTER TABLE api_configs ADD COLUMN IF NOT EXISTS delay_ms INTEGER DEFAULT 0;

-- Update the description for the new column
COMMENT ON COLUMN api_configs.delay_ms IS 'Delay in milliseconds before sending the response (0-60000)';

-- Add missing columns to users table
ALTER TABLE users ADD COLUMN role VARCHAR(50);
ALTER TABLE users ADD COLUMN enabled BOOLEAN DEFAULT TRUE;

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
