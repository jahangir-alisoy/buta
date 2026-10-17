--liquibase formatted sql

--changeset buta:007-01
CREATE TABLE membership_plans (
    code VARCHAR(10) PRIMARY KEY CHECK (code IN ('FREE', 'PLUS')),
    name JSONB NOT NULL CHECK (nullif(name ->> 'az', '') IS NOT NULL AND nullif(name ->> 'en', '') IS NOT NULL),
    description JSONB NOT NULL CHECK (nullif(description ->> 'az', '') IS NOT NULL AND nullif(description ->> 'en', '') IS NOT NULL),
    features JSONB NOT NULL DEFAULT '[]'::jsonb,
    price NUMERIC(12, 2),
    billing_period VARCHAR(10),
    purchasable BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE memberships (
    user_id UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    plan_code VARCHAR(10) NOT NULL REFERENCES membership_plans (code),
    source VARCHAR(20) NOT NULL CHECK (source IN ('DEFAULT', 'EARNED', 'ADMIN', 'SUBSCRIPTION')),
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE,
    granted_by UUID,
    reason VARCHAR(200),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_memberships_plan_end ON memberships (plan_code, ends_at);
--rollback DROP TABLE memberships; DROP TABLE membership_plans;

--changeset buta:007-02
CREATE TABLE loyalty_tiers (
    code VARCHAR(10) PRIMARY KEY,
    name JSONB NOT NULL CHECK (nullif(name ->> 'az', '') IS NOT NULL AND nullif(name ->> 'en', '') IS NOT NULL),
    threshold_points BIGINT NOT NULL CHECK (threshold_points >= 0),
    earn_multiplier NUMERIC(4, 2) NOT NULL CHECK (earn_multiplier >= 1),
    benefits JSONB NOT NULL CHECK (nullif(benefits ->> 'az', '') IS NOT NULL AND nullif(benefits ->> 'en', '') IS NOT NULL),
    sort_order INTEGER NOT NULL,
    CONSTRAINT uq_loyalty_tiers_threshold UNIQUE (threshold_points)
);
CREATE TABLE loyalty_accounts (
    user_id UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    available_points BIGINT NOT NULL DEFAULT 0,
    lifetime_earned_points BIGINT NOT NULL DEFAULT 0,
    lifetime_redeemed_points BIGINT NOT NULL DEFAULT 0,
    tier_qualifying_points BIGINT NOT NULL DEFAULT 0,
    tier_code VARCHAR(10) NOT NULL REFERENCES loyalty_tiers (code),
    flagged_for_review BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE TABLE loyalty_ledger_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    entry_type VARCHAR(20) NOT NULL CHECK (entry_type IN ('EARN_PURCHASE', 'EARN_TASK', 'REDEEM', 'REVERSAL', 'ADJUSTMENT')),
    points BIGINT NOT NULL CHECK (points <> 0),
    idempotency_key VARCHAR(160) NOT NULL,
    source_type VARCHAR(20) NOT NULL CHECK (source_type IN ('ORDER_ITEM', 'ORDER', 'TASK', 'ADMIN')),
    source_id UUID,
    reverses_entry_id UUID REFERENCES loyalty_ledger_entries (id),
    description_key VARCHAR(80) NOT NULL,
    description_params JSONB NOT NULL DEFAULT '{}'::jsonb,
    actor_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_ledger_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_ledger_sign CHECK (
        (entry_type IN ('EARN_PURCHASE', 'EARN_TASK') AND points > 0)
        OR (entry_type = 'REDEEM' AND points < 0)
        OR entry_type IN ('REVERSAL', 'ADJUSTMENT'))
);
CREATE INDEX idx_ledger_user_created ON loyalty_ledger_entries (user_id, created_at DESC);
--rollback DROP TABLE loyalty_ledger_entries; DROP TABLE loyalty_accounts; DROP TABLE loyalty_tiers;

--changeset buta:007-03
CREATE TABLE loyalty_tasks (
    id UUID PRIMARY KEY,
    code VARCHAR(60) NOT NULL,
    task_type VARCHAR(30) NOT NULL CHECK (task_type IN ('DISTINCT_PARTNERS', 'BUNDLE_PURCHASE', 'EVENT_ATTENDANCE', 'NEW_CATEGORY', 'REFERRAL')),
    title JSONB NOT NULL CHECK (nullif(title ->> 'az', '') IS NOT NULL AND nullif(title ->> 'en', '') IS NOT NULL),
    description JSONB NOT NULL CHECK (nullif(description ->> 'az', '') IS NOT NULL AND nullif(description ->> 'en', '') IS NOT NULL),
    terms JSONB NOT NULL CHECK (nullif(terms ->> 'az', '') IS NOT NULL AND nullif(terms ->> 'en', '') IS NOT NULL),
    target_value INTEGER NOT NULL CHECK (target_value >= 1),
    reward_points BIGINT NOT NULL CHECK (reward_points > 0),
    period_type VARCHAR(20) NOT NULL CHECK (period_type IN ('ONE_TIME', 'CALENDAR_MONTH')),
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    min_order_amount NUMERIC(12, 2) NOT NULL DEFAULT 5.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_loyalty_tasks_code UNIQUE (code)
);
CREATE TABLE task_progress (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    task_id UUID NOT NULL REFERENCES loyalty_tasks (id) ON DELETE CASCADE,
    period_key VARCHAR(10) NOT NULL,
    progress_value INTEGER NOT NULL DEFAULT 0,
    target_value INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    completed_at TIMESTAMP WITH TIME ZONE,
    awarded BOOLEAN NOT NULL DEFAULT FALSE,
    award_seq INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_task_progress UNIQUE (user_id, task_id, period_key)
);
CREATE TABLE referrals (
    id UUID PRIMARY KEY,
    referrer_user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    referee_user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code_used VARCHAR(12) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'QUALIFIED', 'UNDER_REVIEW', 'REJECTED', 'REVERSED')),
    qualifying_order_id UUID,
    qualified_at TIMESTAMP WITH TIME ZONE,
    review_note VARCHAR(200),
    reviewed_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_referrals_referee UNIQUE (referee_user_id),
    CONSTRAINT ck_referrals_not_self CHECK (referrer_user_id <> referee_user_id)
);
CREATE INDEX idx_referrals_referrer ON referrals (referrer_user_id, status);
--rollback DROP TABLE referrals; DROP TABLE task_progress; DROP TABLE loyalty_tasks;
