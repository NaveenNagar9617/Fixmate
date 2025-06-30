-- =============================================
-- FixMate Database Schema - V1 Init
-- =============================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================
-- USERS TABLE
-- =============================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('STUDENT', 'STAFF', 'ADMIN')),
    room_number VARCHAR(20),
    block VARCHAR(50),
    phone VARCHAR(20),
    profile_photo_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- =============================================
-- STAFF PROFILES TABLE
-- =============================================
CREATE TABLE staff_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(30) NOT NULL CHECK (category IN ('ELECTRICIAN', 'PLUMBER', 'IT_TECHNICIAN', 'CARPENTER', 'CLEANER', 'SECURITY', 'GENERAL')),
    is_on_duty BOOLEAN NOT NULL DEFAULT TRUE,
    shift_start TIME,
    shift_end TIME,
    avg_rating DOUBLE PRECISION DEFAULT 0.0
);

CREATE INDEX idx_staff_profiles_user_id ON staff_profiles(user_id);
CREATE INDEX idx_staff_profiles_category ON staff_profiles(category);

-- =============================================
-- COMPLAINTS TABLE
-- =============================================
CREATE TABLE complaints (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(30) NOT NULL CHECK (category IN ('ELECTRICAL', 'PLUMBING', 'WIFI', 'FURNITURE', 'CLEANING', 'PEST_CONTROL', 'SECURITY', 'OTHER')),
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'ESCALATED', 'REOPENED')),
    location_block VARCHAR(50) NOT NULL,
    location_floor INT NOT NULL,
    room_number VARCHAR(20) NOT NULL,
    student_id UUID NOT NULL REFERENCES users(id),
    assigned_staff_id UUID REFERENCES users(id),
    parent_complaint_id UUID REFERENCES complaints(id),
    sla_deadline TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    reopened_count INT NOT NULL DEFAULT 0,
    upvote_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_complaints_status ON complaints(status);
CREATE INDEX idx_complaints_student_id ON complaints(student_id);
CREATE INDEX idx_complaints_assigned_staff_id ON complaints(assigned_staff_id);
CREATE INDEX idx_complaints_created_at ON complaints(created_at);
CREATE INDEX idx_complaints_category ON complaints(category);
CREATE INDEX idx_complaints_priority ON complaints(priority);
CREATE INDEX idx_complaints_sla_deadline ON complaints(sla_deadline);

-- =============================================
-- COMPLAINT PHOTOS TABLE
-- =============================================
CREATE TABLE complaint_photos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    photo_url VARCHAR(500) NOT NULL,
    photo_type VARCHAR(10) NOT NULL CHECK (photo_type IN ('BEFORE', 'AFTER')),
    uploaded_by UUID NOT NULL REFERENCES users(id),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_complaint_photos_complaint_id ON complaint_photos(complaint_id);

-- =============================================
-- COMPLAINT TIMELINE TABLE
-- =============================================
CREATE TABLE complaint_timeline (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_complaint_timeline_complaint_id ON complaint_timeline(complaint_id);

-- =============================================
-- ESCALATIONS TABLE
-- =============================================
CREATE TABLE escalations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    escalated_to UUID NOT NULL REFERENCES users(id),
    reason TEXT NOT NULL,
    escalated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_at TIMESTAMPTZ
);

CREATE INDEX idx_escalations_complaint_id ON escalations(complaint_id);
CREATE INDEX idx_escalations_escalated_to ON escalations(escalated_to);

-- =============================================
-- COMMENTS TABLE
-- =============================================
CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_complaint_id ON comments(complaint_id);

-- =============================================
-- UPVOTES TABLE
-- =============================================
CREATE TABLE upvotes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    complaint_id UUID NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_upvote_complaint_student UNIQUE (complaint_id, student_id)
);

CREATE INDEX idx_upvotes_complaint_id ON upvotes(complaint_id);

-- =============================================
-- RATINGS TABLE
-- =============================================
CREATE TABLE ratings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    complaint_id UUID NOT NULL UNIQUE REFERENCES complaints(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id),
    stars INT NOT NULL CHECK (stars >= 1 AND stars <= 5),
    feedback_text TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ratings_student_id ON ratings(student_id);

-- =============================================
-- NOTIFICATIONS TABLE
-- =============================================
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL CHECK (type IN ('COMPLAINT_ASSIGNED', 'STATUS_CHANGED', 'ESCALATION', 'NEW_COMMENT', 'SLA_WARNING', 'ANNOUNCEMENT')),
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    complaint_id UUID REFERENCES complaints(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id_is_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);

-- =============================================
-- ANNOUNCEMENTS TABLE
-- =============================================
CREATE TABLE announcements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    admin_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    target_audience VARCHAR(20) NOT NULL CHECK (target_audience IN ('ALL', 'STUDENTS', 'STAFF')),
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================
-- AUDIT LOG TABLE
-- =============================================
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    old_value JSONB,
    new_value JSONB,
    ip_address VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);

-- =============================================
-- MAINTENANCE SCHEDULES TABLE
-- =============================================
CREATE TABLE maintenance_schedules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(200) NOT NULL,
    category VARCHAR(30) NOT NULL CHECK (category IN ('ELECTRICAL', 'PLUMBING', 'WIFI', 'FURNITURE', 'CLEANING', 'PEST_CONTROL', 'SECURITY', 'OTHER')),
    block VARCHAR(50) NOT NULL,
    recurrence_days INT NOT NULL,
    next_due DATE NOT NULL,
    assigned_staff_id UUID REFERENCES users(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_maintenance_schedules_next_due ON maintenance_schedules(next_due);
