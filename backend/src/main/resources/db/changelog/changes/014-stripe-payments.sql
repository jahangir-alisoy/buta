--liquibase formatted sql

--changeset buta:014-01
ALTER TABLE payments DROP CONSTRAINT payments_provider_check;
ALTER TABLE payments ADD CONSTRAINT payments_provider_check CHECK (provider IN ('SANDBOX', 'STRIPE'));
ALTER TABLE membership_purchases ADD COLUMN provider_reference VARCHAR(80);
CREATE UNIQUE INDEX uq_membership_purchases_provider_reference ON membership_purchases (provider_reference);
--rollback DROP INDEX uq_membership_purchases_provider_reference; ALTER TABLE membership_purchases DROP COLUMN provider_reference; ALTER TABLE payments DROP CONSTRAINT payments_provider_check; ALTER TABLE payments ADD CONSTRAINT payments_provider_check CHECK (provider IN ('SANDBOX'));
