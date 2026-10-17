--liquibase formatted sql

--changeset buta:002-01
CREATE TABLE stored_files (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    purpose VARCHAR(30) NOT NULL CHECK (purpose IN ('AVATAR')),
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes >= 0),
    sha256 VARCHAR(64) NOT NULL,
    storage_provider VARCHAR(20) NOT NULL CHECK (storage_provider IN ('DATABASE', 'LOCAL')),
    storage_key VARCHAR(255),
    data BYTEA,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_stored_files_owner ON stored_files (owner_user_id);
--rollback DROP TABLE stored_files;

--changeset buta:002-02
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    email_canonical VARCHAR(254) NOT NULL,
    username VARCHAR(30),
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone VARCHAR(16),
    status VARCHAR(30) NOT NULL CHECK (status IN ('PROFILE_INCOMPLETE', 'ACTIVE', 'SUSPENDED')),
    avatar_file_id UUID REFERENCES stored_files (id) ON DELETE SET NULL,
    referral_code VARCHAR(12) NOT NULL,
    registration_ip_hash VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_referral_code UNIQUE (referral_code)
);
CREATE INDEX idx_users_email_canonical ON users (email_canonical);
--rollback DROP TABLE users;

--changeset buta:002-03
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('CUSTOMER', 'ADMIN')),
    source VARCHAR(20) NOT NULL CHECK (source IN ('SYSTEM', 'BOOTSTRAP', 'SEED')),
    granted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, role)
);
--rollback DROP TABLE user_roles;

--changeset buta:002-04
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL,
    idle_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    absolute_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoke_reason VARCHAR(40),
    user_agent VARCHAR(200),
    ip_hash VARCHAR(64),
    CONSTRAINT uq_user_sessions_token_hash UNIQUE (token_hash)
);
CREATE INDEX idx_user_sessions_user ON user_sessions (user_id);
CREATE INDEX idx_user_sessions_absolute_expiry ON user_sessions (absolute_expires_at);
--rollback DROP TABLE user_sessions;

--changeset buta:002-05
CREATE TABLE otp_challenges (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    purpose VARCHAR(20) NOT NULL CHECK (purpose IN ('SIGN_IN', 'EMAIL_CHANGE')),
    user_id UUID REFERENCES users (id) ON DELETE CASCADE,
    code_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    locked_at TIMESTAMP WITH TIME ZONE,
    superseded_at TIMESTAMP WITH TIME ZONE,
    request_ip_hash VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_otp_challenges_email ON otp_challenges (email, purpose, created_at DESC);
--rollback DROP TABLE otp_challenges;

--changeset buta:002-06
CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    locale VARCHAR(2) NOT NULL DEFAULT 'az' CHECK (locale IN ('az', 'en')),
    theme VARCHAR(10) NOT NULL DEFAULT 'SYSTEM' CHECK (theme IN ('LIGHT', 'DARK', 'SYSTEM')),
    personalization_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    personalization_decided_at TIMESTAMP WITH TIME ZONE,
    preferred_category_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    budget_min NUMERIC(12, 2),
    budget_max NUMERIC(12, 2),
    city VARCHAR(60),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
--rollback DROP TABLE user_preferences;

--changeset buta:002-07
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    actor_user_id UUID,
    actor_type VARCHAR(10) NOT NULL CHECK (actor_type IN ('USER', 'ADMIN', 'SYSTEM')),
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(40),
    target_id VARCHAR(64),
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    request_id VARCHAR(64),
    ip_hash VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_audit_logs_target ON audit_logs (target_type, target_id);
CREATE INDEX idx_audit_logs_actor ON audit_logs (actor_user_id, created_at);
CREATE INDEX idx_audit_logs_created ON audit_logs (created_at);
--rollback DROP TABLE audit_logs;

--changeset buta:002-08
CREATE TABLE business_settings (
    key VARCHAR(40) PRIMARY KEY CHECK (key IN ('pricing', 'loyalty', 'membership', 'recommendation')),
    value JSONB NOT NULL,
    updated_by UUID,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
--rollback DROP TABLE business_settings;
