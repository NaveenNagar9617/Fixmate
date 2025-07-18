-- ==========================================================
-- FixMate Migration - V3 Priority & Auto-Assignment Enhancements
-- ==========================================================

-- 1. Add priority and audit enhancement columns to complaints
ALTER TABLE complaints
    ADD COLUMN IF NOT EXISTS requested_priority VARCHAR(20),
    ADD COLUMN IF NOT EXISTS priority_reason TEXT,
    ADD COLUMN IF NOT EXISTS priority_source VARCHAR(30) DEFAULT 'SYSTEM',
    ADD COLUMN IF NOT EXISTS priority_updated_at TIMESTAMPTZ;

-- Backfill requested_priority for existing complaints
UPDATE complaints
SET requested_priority = priority
WHERE requested_priority IS NULL;

-- 2. Add last_assigned_at to staff_profiles for round-robin tie-breaking
ALTER TABLE staff_profiles
    ADD COLUMN IF NOT EXISTS last_assigned_at TIMESTAMPTZ;

-- 3. Seed additional staff users and profiles for full category coverage
-- Staff 4: Carpenter (password: staff123)
INSERT INTO users (id, name, email, password_hash, role, phone, is_active, created_at, updated_at)
VALUES (
    'b0000000-0000-0000-0000-000000000004',
    'Ramesh Sharma',
    'ramesh.carpenter@fixmate.com',
    '$2a$10$9P6nne8xJDMO7LaDdKFII.XZ8PiumdZWwiJPuMr/Ujc6CoCOpdxJW',
    'STAFF',
    '9876543213',
    TRUE,
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

INSERT INTO staff_profiles (id, user_id, category, is_on_duty, shift_start, shift_end, avg_rating, last_assigned_at)
VALUES (
    'c0000000-0000-0000-0000-000000000004',
    'b0000000-0000-0000-0000-000000000004',
    'CARPENTER',
    TRUE,
    '08:00',
    '17:00',
    4.6,
    NOW() - INTERVAL '1 day'
) ON CONFLICT (user_id) DO NOTHING;

-- Staff 5: Cleaner (password: staff123)
INSERT INTO users (id, name, email, password_hash, role, phone, is_active, created_at, updated_at)
VALUES (
    'b0000000-0000-0000-0000-000000000005',
    'Sunita Devi',
    'sunita.cleaner@fixmate.com',
    '$2a$10$9P6nne8xJDMO7LaDdKFII.XZ8PiumdZWwiJPuMr/Ujc6CoCOpdxJW',
    'STAFF',
    '9876543214',
    TRUE,
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

INSERT INTO staff_profiles (id, user_id, category, is_on_duty, shift_start, shift_end, avg_rating, last_assigned_at)
VALUES (
    'c0000000-0000-0000-0000-000000000005',
    'b0000000-0000-0000-0000-000000000005',
    'CLEANER',
    TRUE,
    '07:00',
    '16:00',
    4.8,
    NOW() - INTERVAL '1 day'
) ON CONFLICT (user_id) DO NOTHING;

-- Staff 6: Security Officer (password: staff123)
INSERT INTO users (id, name, email, password_hash, role, phone, is_active, created_at, updated_at)
VALUES (
    'b0000000-0000-0000-0000-000000000006',
    'Vikram Guard',
    'vikram.security@fixmate.com',
    '$2a$10$9P6nne8xJDMO7LaDdKFII.XZ8PiumdZWwiJPuMr/Ujc6CoCOpdxJW',
    'STAFF',
    '9876543215',
    TRUE,
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

INSERT INTO staff_profiles (id, user_id, category, is_on_duty, shift_start, shift_end, avg_rating, last_assigned_at)
VALUES (
    'c0000000-0000-0000-0000-000000000006',
    'b0000000-0000-0000-0000-000000000006',
    'SECURITY',
    TRUE,
    '00:00',
    '23:59',
    4.9,
    NOW() - INTERVAL '1 day'
) ON CONFLICT (user_id) DO NOTHING;

-- Staff 7: General Maintenance (password: staff123)
INSERT INTO users (id, name, email, password_hash, role, phone, is_active, created_at, updated_at)
VALUES (
    'b0000000-0000-0000-0000-000000000007',
    'Anil Verma',
    'anil.general@fixmate.com',
    '$2a$10$9P6nne8xJDMO7LaDdKFII.XZ8PiumdZWwiJPuMr/Ujc6CoCOpdxJW',
    'STAFF',
    '9876543216',
    TRUE,
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;

INSERT INTO staff_profiles (id, user_id, category, is_on_duty, shift_start, shift_end, avg_rating, last_assigned_at)
VALUES (
    'c0000000-0000-0000-0000-000000000007',
    'b0000000-0000-0000-0000-000000000007',
    'GENERAL',
    TRUE,
    '08:00',
    '18:00',
    4.4,
    NOW() - INTERVAL '1 day'
) ON CONFLICT (user_id) DO NOTHING;
