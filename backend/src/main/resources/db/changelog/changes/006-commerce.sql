--liquibase formatted sql

--changeset buta:006-01
CREATE TABLE carts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_carts_user UNIQUE (user_id)
);
CREATE TABLE cart_items (
    id UUID PRIMARY KEY,
    cart_id UUID NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    offer_id UUID NOT NULL REFERENCES offers (id),
    quantity INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 10),
    unit_price_at_add NUMERIC(12, 2) NOT NULL,
    recommendation_request_id VARCHAR(64),
    added_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_cart_items UNIQUE (cart_id, offer_id)
);
--rollback DROP TABLE cart_items; DROP TABLE carts;

--changeset buta:006-02
CREATE SEQUENCE order_number_seq START WITH 1000 INCREMENT BY 1;
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    order_number VARCHAR(20) NOT NULL,
    user_id UUID NOT NULL REFERENCES users (id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING_PAYMENT', 'FULFILLED', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'PARTIALLY_REFUNDED', 'REFUNDED')),
    currency VARCHAR(3) NOT NULL DEFAULT 'AZN' CHECK (currency = 'AZN'),
    subtotal_amount NUMERIC(12, 2) NOT NULL CHECK (subtotal_amount >= 0),
    points_redeemed BIGINT NOT NULL DEFAULT 0 CHECK (points_redeemed >= 0),
    points_discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (points_discount_amount >= 0),
    total_amount NUMERIC(12, 2) NOT NULL CHECK (total_amount >= 0),
    checkout_idempotency_key VARCHAR(80) NOT NULL,
    checkout_fingerprint VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    fulfilled_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancel_reason VARCHAR(40),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_orders_number UNIQUE (order_number),
    CONSTRAINT uq_orders_idempotency UNIQUE (user_id, checkout_idempotency_key)
);
CREATE INDEX idx_orders_user_created ON orders (user_id, created_at DESC);
CREATE INDEX idx_orders_status_expires ON orders (status, expires_at);
--rollback DROP TABLE orders; DROP SEQUENCE order_number_seq;

--changeset buta:006-03
CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    offer_id UUID NOT NULL REFERENCES offers (id),
    offer_type VARCHAR(10) NOT NULL CHECK (offer_type IN ('SINGLE', 'BUNDLE')),
    category_id UUID NOT NULL REFERENCES categories (id),
    title_snapshot JSONB NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 10),
    unit_price NUMERIC(12, 2) NOT NULL,
    line_subtotal NUMERIC(12, 2) NOT NULL,
    points_discount_allocated NUMERIC(12, 2) NOT NULL DEFAULT 0,
    cash_paid NUMERIC(12, 2) NOT NULL,
    partner_cost_total NUMERIC(12, 2) NOT NULL,
    payment_fee_allocated NUMERIC(12, 2) NOT NULL DEFAULT 0,
    expected_loyalty_cost NUMERIC(12, 2) NOT NULL DEFAULT 0,
    contribution_margin NUMERIC(12, 2) NOT NULL DEFAULT 0,
    points_earned BIGINT NOT NULL DEFAULT 0,
    refunded_quantity INTEGER NOT NULL DEFAULT 0 CHECK (refunded_quantity >= 0),
    recommendation_request_id VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_offer ON order_items (offer_id);
CREATE TABLE order_item_components (
    id UUID PRIMARY KEY,
    order_item_id UUID NOT NULL REFERENCES order_items (id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES partner_products (id),
    partner_id UUID NOT NULL REFERENCES partners (id),
    quantity INTEGER NOT NULL CHECK (quantity >= 1),
    allocated_price NUMERIC(12, 2) NOT NULL,
    partner_cost NUMERIC(12, 2) NOT NULL
);
CREATE INDEX idx_order_item_components_item ON order_item_components (order_item_id);
CREATE INDEX idx_order_item_components_partner ON order_item_components (partner_id);
--rollback DROP TABLE order_item_components; DROP TABLE order_items;

--changeset buta:006-04
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    provider VARCHAR(20) NOT NULL CHECK (provider IN ('SANDBOX')),
    provider_reference VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'CAPTURED', 'FAILED', 'PARTIALLY_REFUNDED', 'REFUNDED')),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'AZN' CHECK (currency = 'AZN'),
    refunded_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (refunded_amount >= 0),
    captured_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    failure_reason VARCHAR(80),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_payments_order UNIQUE (order_id),
    CONSTRAINT ck_payments_refund CHECK (refunded_amount <= amount)
);
CREATE TABLE refunds (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    payment_id UUID NOT NULL REFERENCES payments (id),
    amount NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    reason VARCHAR(200) NOT NULL,
    initiated_by VARCHAR(10) NOT NULL CHECK (initiated_by IN ('CUSTOMER', 'ADMIN')),
    actor_user_id UUID NOT NULL,
    status VARCHAR(10) NOT NULL CHECK (status IN ('SUCCEEDED', 'FAILED')),
    provider_reference VARCHAR(80),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_refunds_order ON refunds (order_id);
--rollback DROP TABLE refunds; DROP TABLE payments;

--changeset buta:006-05
CREATE TABLE redemptions (
    id UUID PRIMARY KEY,
    order_item_id UUID NOT NULL REFERENCES order_items (id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES partner_products (id),
    code VARCHAR(12) NOT NULL,
    status VARCHAR(10) NOT NULL CHECK (status IN ('ISSUED', 'REDEEMED', 'EXPIRED', 'VOIDED')),
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
    redeemed_at TIMESTAMP WITH TIME ZONE,
    redeemed_by UUID,
    voided_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_redemptions_code UNIQUE (code)
);
CREATE INDEX idx_redemptions_item ON redemptions (order_item_id);
CREATE INDEX idx_redemptions_status_valid ON redemptions (status, valid_until);
CREATE TABLE favorites (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    offer_id UUID NOT NULL REFERENCES offers (id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, offer_id)
);
--rollback DROP TABLE favorites; DROP TABLE redemptions;
