-- Failure simulation: an endpoint can answer the first N calls of a client with an error before returning its configured response
ALTER TABLE api_configs ADD COLUMN IF NOT EXISTS failure_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE api_configs ADD COLUMN IF NOT EXISTS failure_count INTEGER DEFAULT 3;
ALTER TABLE api_configs ADD COLUMN IF NOT EXISTS failure_status_codes VARCHAR(100) DEFAULT '503';
ALTER TABLE api_configs ADD COLUMN IF NOT EXISTS retry_after_seconds INTEGER DEFAULT 1;
