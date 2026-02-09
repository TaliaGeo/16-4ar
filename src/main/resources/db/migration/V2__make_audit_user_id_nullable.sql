-- Migration: Make user_id nullable in audit_logs
-- Date: 2026-02-02
-- Purpose: Fix UnexpectedRollbackException when userId is null

ALTER TABLE audit_logs 
ALTER COLUMN user_id DROP NOT NULL;

-- Add comment
COMMENT ON COLUMN audit_logs.user_id IS 'User who performed the action (nullable for system actions)';
