-- Add missing columns to users table
ALTER TABLE users ADD COLUMN role VARCHAR(50);
ALTER TABLE users ADD COLUMN enabled BOOLEAN DEFAULT TRUE;
