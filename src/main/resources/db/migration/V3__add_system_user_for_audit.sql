-- Migration: Add SYSTEM user for audit logs
-- Date: 2026-02-03
-- Purpose: Support audit logging when no authenticated user (system operations)

-- Insert system user if not exists (ID = -1)
INSERT INTO users (id, email, password_hash, full_name, active, created_at, updated_at)
VALUES (
    -1,
    'system@internal',
    '$2a$10$dummyHashForSystemUserThatWillNeverBeUsedForLogin',
    'SYSTEM',
    false, -- System user should not be able to login
    NOW(),
    NOW()
)
ON CONFLICT (id) DO NOTHING;

-- Update sequence to avoid conflicts (start from 1000 for regular users)
SELECT setval('users_id_seq', GREATEST(1000, (SELECT MAX(id) FROM users WHERE id > 0)), true);

-- Add comment
COMMENT ON TABLE audit_logs IS 'Audit trail for all system operations. user_id = -1 indicates system operations without authenticated user';
