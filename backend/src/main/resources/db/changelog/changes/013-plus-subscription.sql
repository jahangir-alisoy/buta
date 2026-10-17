--liquibase formatted sql

--changeset buta:013-01
UPDATE membership_plans SET price = 4.99, billing_period = 'MONTH', purchasable = TRUE, updated_at = now() WHERE code = 'PLUS';
CREATE TABLE membership_purchases (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    plan_code VARCHAR(10) NOT NULL REFERENCES membership_plans (code),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL,
    period_starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    period_ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    payment_provider VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_membership_purchases_user ON membership_purchases (user_id, created_at);
--rollback DROP TABLE membership_purchases; UPDATE membership_plans SET price = NULL, billing_period = NULL, purchasable = FALSE WHERE code = 'PLUS';
