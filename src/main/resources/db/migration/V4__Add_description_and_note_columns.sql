-- Add description (short purpose, max 500) and note (free-form, max 2000) to api_configs (issue #35)
ALTER TABLE api_configs ADD COLUMN IF NOT EXISTS description VARCHAR(500);
ALTER TABLE api_configs ADD COLUMN IF NOT EXISTS note VARCHAR(2000);
