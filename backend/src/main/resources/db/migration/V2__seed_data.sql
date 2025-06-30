-- =============================================
-- FixMate Seed Data - V2
-- =============================================

-- =============================================
-- ADMIN USER (password: admin123)
-- BCrypt hash of 'admin123'
-- =============================================
INSERT INTO users (id, name, email, password_hash, role, is_active, created_at, updated_at)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Admin User',
    'admin@fixmate.com',
    '$2a$10$syDSrBt/B/3AA98xEgqWf.dho1ofeld0OkPyLKqpe0hsoFavVHElK',
    'ADMIN',
    TRUE,
    NOW(),
    NOW()
);

-- =============================================
-- STAFF USERS
-- =============================================
-- Staff 1: Electrician (password: staff123)
INSERT INTO users (id, name, email, password_hash, role, phone, is_active, created_at, updated_at)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'Rajesh Kumar',
    'rajesh@fixmate.com',
    '$2a$10$9P6nne8xJDMO7LaDdKFII.XZ8PiumdZWwiJPuMr/Ujc6CoCOpdxJW',
    'STAFF',
    '9876543210',
    TRUE,
    NOW(),
    NOW()
);

INSERT INTO staff_profiles (id, user_id, category, is_on_duty, shift_start, shift_end, avg_rating)
VALUES (
    'c0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    'ELECTRICIAN',
    TRUE,
    '08:00',
    '17:00',
    4.5
);

-- Staff 2: Plumber (password: staff123)
INSERT INTO users (id, name, email, password_hash, role, phone, is_active, created_at, updated_at)
VALUES (
    'b0000000-0000-0000-0000-000000000002',
    'Suresh Patel',
    'suresh@fixmate.com',
    '$2a$10$9P6nne8xJDMO7LaDdKFII.XZ8PiumdZWwiJPuMr/Ujc6CoCOpdxJW',
    'STAFF',
    '9876543211',
    TRUE,
    NOW(),
    NOW()
);

INSERT INTO staff_profiles (id, user_id, category, is_on_duty, shift_start, shift_end, avg_rating)
VALUES (
    'c0000000-0000-0000-0000-000000000002',
    'b0000000-0000-0000-0000-000000000002',
    'PLUMBER',
    TRUE,
    '08:00',
    '17:00',
    4.2
);

-- Staff 3: IT Technician (password: staff123)
INSERT INTO users (id, name, email, password_hash, role, phone, is_active, created_at, updated_at)
VALUES (
    'b0000000-0000-0000-0000-000000000003',
    'Amit Singh',
    'amit@fixmate.com',
    '$2a$10$9P6nne8xJDMO7LaDdKFII.XZ8PiumdZWwiJPuMr/Ujc6CoCOpdxJW',
    'STAFF',
    '9876543212',
    TRUE,
    NOW(),
    NOW()
);

INSERT INTO staff_profiles (id, user_id, category, is_on_duty, shift_start, shift_end, avg_rating)
VALUES (
    'c0000000-0000-0000-0000-000000000003',
    'b0000000-0000-0000-0000-000000000003',
    'IT_TECHNICIAN',
    TRUE,
    '09:00',
    '18:00',
    4.7
);

-- =============================================
-- STUDENT USERS
-- =============================================
-- Student 1 (password: student123)
INSERT INTO users (id, name, email, password_hash, role, room_number, block, phone, is_active, created_at, updated_at)
VALUES (
    'd0000000-0000-0000-0000-000000000001',
    'Priya Sharma',
    'priya@fixmate.com',
    '$2a$10$ZDYp8Hyd/b3yh4kT3DcS4.qNzZw9NvUdHw8Oakmf1gSwD0EFmYkrC',
    'STUDENT',
    '301',
    'Block A',
    '9123456789',
    TRUE,
    NOW(),
    NOW()
);

-- Student 2 (password: student123)
INSERT INTO users (id, name, email, password_hash, role, room_number, block, phone, is_active, created_at, updated_at)
VALUES (
    'd0000000-0000-0000-0000-000000000002',
    'Vikram Reddy',
    'vikram@fixmate.com',
    '$2a$10$ZDYp8Hyd/b3yh4kT3DcS4.qNzZw9NvUdHw8Oakmf1gSwD0EFmYkrC',
    'STUDENT',
    '205',
    'Block B',
    '9123456790',
    TRUE,
    NOW(),
    NOW()
);

-- =============================================
-- SAMPLE COMPLAINTS
-- =============================================
-- Complaint 1: SUBMITTED
INSERT INTO complaints (id, title, description, category, priority, status, location_block, location_floor, room_number, student_id, created_at, updated_at)
VALUES (
    'e0000000-0000-0000-0000-000000000001',
    'Broken light in room',
    'The ceiling light in my room has stopped working. It flickers occasionally but mostly stays off. This has been happening for 2 days now.',
    'ELECTRICAL',
    'MEDIUM',
    'SUBMITTED',
    'Block A',
    3,
    '301',
    'd0000000-0000-0000-0000-000000000001',
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days'
);

-- Complaint 2: ASSIGNED
INSERT INTO complaints (id, title, description, category, priority, status, location_block, location_floor, room_number, student_id, assigned_staff_id, sla_deadline, created_at, updated_at)
VALUES (
    'e0000000-0000-0000-0000-000000000002',
    'Water leakage in bathroom',
    'There is a continuous water leak from the bathroom tap. The washer seems damaged and water is dripping constantly, wasting a lot of water.',
    'PLUMBING',
    'HIGH',
    'ASSIGNED',
    'Block A',
    3,
    '301',
    'd0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000002',
    NOW() + INTERVAL '4 hours',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'
);

-- Complaint 3: IN_PROGRESS
INSERT INTO complaints (id, title, description, category, priority, status, location_block, location_floor, room_number, student_id, assigned_staff_id, sla_deadline, created_at, updated_at)
VALUES (
    'e0000000-0000-0000-0000-000000000003',
    'WiFi not working on 2nd floor',
    'The WiFi access point on the 2nd floor of Block B has been down since yesterday. Multiple students are affected and unable to attend online classes.',
    'WIFI',
    'CRITICAL',
    'IN_PROGRESS',
    'Block B',
    2,
    '205',
    'd0000000-0000-0000-0000-000000000002',
    'b0000000-0000-0000-0000-000000000003',
    NOW() + INTERVAL '2 hours',
    NOW() - INTERVAL '3 hours',
    NOW() - INTERVAL '1 hour'
);

-- Complaint 4: RESOLVED
INSERT INTO complaints (id, title, description, category, priority, status, location_block, location_floor, room_number, student_id, assigned_staff_id, sla_deadline, resolved_at, created_at, updated_at)
VALUES (
    'e0000000-0000-0000-0000-000000000004',
    'Broken chair in study room',
    'One of the chairs in the common study room on the 1st floor has a broken leg. It is unsafe to sit on and should be replaced or repaired.',
    'FURNITURE',
    'LOW',
    'RESOLVED',
    'Block B',
    1,
    'Common',
    'd0000000-0000-0000-0000-000000000002',
    'b0000000-0000-0000-0000-000000000001',
    NOW() + INTERVAL '72 hours',
    NOW() - INTERVAL '6 hours',
    NOW() - INTERVAL '5 days',
    NOW() - INTERVAL '6 hours'
);

-- Complaint 5: ESCALATED
INSERT INTO complaints (id, title, description, category, priority, status, location_block, location_floor, room_number, student_id, assigned_staff_id, sla_deadline, created_at, updated_at)
VALUES (
    'e0000000-0000-0000-0000-000000000005',
    'Power outage in Block A',
    'Complete power outage in the entire Block A ground floor since this morning. Emergency lights are on but no electricity for fans, lights, or charging.',
    'ELECTRICAL',
    'CRITICAL',
    'ESCALATED',
    'Block A',
    1,
    'All',
    'd0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    NOW() - INTERVAL '1 hour',
    NOW() - INTERVAL '4 hours',
    NOW() - INTERVAL '30 minutes'
);

-- =============================================
-- TIMELINE ENTRIES FOR SAMPLE COMPLAINTS
-- =============================================
INSERT INTO complaint_timeline (id, complaint_id, actor_id, action, old_status, new_status, note, created_at)
VALUES
    (uuid_generate_v4(), 'e0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001', 'Complaint submitted', NULL, 'SUBMITTED', 'New complaint created', NOW() - INTERVAL '2 days'),
    (uuid_generate_v4(), 'e0000000-0000-0000-0000-000000000002', 'd0000000-0000-0000-0000-000000000001', 'Complaint submitted', NULL, 'SUBMITTED', 'New complaint created', NOW() - INTERVAL '1 day'),
    (uuid_generate_v4(), 'e0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Staff assigned', 'SUBMITTED', 'ASSIGNED', 'Assigned to Suresh Patel', NOW() - INTERVAL '23 hours'),
    (uuid_generate_v4(), 'e0000000-0000-0000-0000-000000000003', 'd0000000-0000-0000-0000-000000000002', 'Complaint submitted', NULL, 'SUBMITTED', 'New complaint created', NOW() - INTERVAL '3 hours'),
    (uuid_generate_v4(), 'e0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'Staff assigned', 'SUBMITTED', 'ASSIGNED', 'Assigned to Amit Singh', NOW() - INTERVAL '2 hours'),
    (uuid_generate_v4(), 'e0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000003', 'Status updated', 'ASSIGNED', 'IN_PROGRESS', 'Started working on the WiFi issue', NOW() - INTERVAL '1 hour'),
    (uuid_generate_v4(), 'e0000000-0000-0000-0000-000000000005', 'd0000000-0000-0000-0000-000000000001', 'Complaint submitted', NULL, 'SUBMITTED', 'Emergency: Complete power outage', NOW() - INTERVAL '4 hours'),
    (uuid_generate_v4(), 'e0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'Auto-escalated', 'ASSIGNED', 'ESCALATED', 'SLA breached - Auto escalation triggered', NOW() - INTERVAL '30 minutes');

-- =============================================
-- SAMPLE ESCALATION
-- =============================================
INSERT INTO escalations (id, complaint_id, escalated_to, reason, escalated_at, acknowledged, acknowledged_at)
VALUES (
    uuid_generate_v4(),
    'e0000000-0000-0000-0000-000000000005',
    'a0000000-0000-0000-0000-000000000001',
    'SLA breached: Critical complaint not resolved within 2 hours',
    NOW() - INTERVAL '30 minutes',
    FALSE,
    NULL
);
