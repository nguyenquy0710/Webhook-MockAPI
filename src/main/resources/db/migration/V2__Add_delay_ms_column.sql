-- Add delay_ms column to api_configs table
-- This migration adds support for response delay functionality

ALTER TABLE api_configs ADD COLUMN IF NOT EXISTS delay_ms INTEGER DEFAULT 0;

-- Update the description for the new column
COMMENT ON COLUMN api_configs.delay_ms IS 'Delay in milliseconds before sending the response (0-60000)';