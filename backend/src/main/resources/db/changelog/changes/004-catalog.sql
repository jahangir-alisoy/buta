--liquibase formatted sql

--changeset buta:004-01
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    slug VARCHAR(60) NOT NULL,
    name JSONB NOT NULL CHECK (nullif(name ->> 'az', '') IS NOT NULL AND nullif(name ->> 'en', '') IS NOT NULL),
    icon_key VARCHAR(40) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_categories_slug UNIQUE (slug)
);
--rollback DROP TABLE categories;

--changeset buta:004-02
CREATE TABLE partners (
    id UUID PRIMARY KEY,
    slug VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description JSONB NOT NULL CHECK (nullif(description ->> 'az', '') IS NOT NULL AND nullif(description ->> 'en', '') IS NOT NULL),
    city VARCHAR(60) NOT NULL,
    address VARCHAR(200),
    website_url VARCHAR(300),
    logo_path VARCHAR(300),
    demo BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(10) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_partners_slug UNIQUE (slug)
);
--rollback DROP TABLE partners;

--changeset buta:004-03
CREATE TABLE partner_products (
    id UUID PRIMARY KEY,
    partner_id UUID NOT NULL REFERENCES partners (id),
    category_id UUID NOT NULL REFERENCES categories (id),
    product_type VARCHAR(30) NOT NULL CHECK (product_type IN ('EVENT_TICKET', 'ADMISSION', 'COUPON', 'TRANSPORT_PASS', 'DIGITAL_SUBSCRIPTION', 'SERVICE')),
    title JSONB NOT NULL CHECK (nullif(title ->> 'az', '') IS NOT NULL AND nullif(title ->> 'en', '') IS NOT NULL),
    description JSONB NOT NULL CHECK (nullif(description ->> 'az', '') IS NOT NULL AND nullif(description ->> 'en', '') IS NOT NULL),
    terms JSONB NOT NULL CHECK (nullif(terms ->> 'az', '') IS NOT NULL AND nullif(terms ->> 'en', '') IS NOT NULL),
    redemption_instructions JSONB NOT NULL CHECK (nullif(redemption_instructions ->> 'az', '') IS NOT NULL AND nullif(redemption_instructions ->> 'en', '') IS NOT NULL),
    eligibility_note JSONB CHECK (eligibility_note IS NULL OR (nullif(eligibility_note ->> 'az', '') IS NOT NULL AND nullif(eligibility_note ->> 'en', '') IS NOT NULL)),
    retail_price NUMERIC(12, 2) NOT NULL CHECK (retail_price > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'AZN' CHECK (currency = 'AZN'),
    cost_model VARCHAR(20) NOT NULL CHECK (cost_model IN ('WHOLESALE', 'COMMISSION')),
    unit_net_cost NUMERIC(12, 2),
    commission_percent NUMERIC(5, 2),
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
    event_starts_at TIMESTAMP WITH TIME ZONE,
    event_ends_at TIMESTAMP WITH TIME ZONE,
    venue_name VARCHAR(120),
    venue_address VARCHAR(200),
    city VARCHAR(60) NOT NULL,
    redemption_method VARCHAR(30) NOT NULL CHECK (redemption_method IN ('VOUCHER_CODE', 'SHOW_CODE_AT_VENUE', 'PARTNER_WEBSITE_CODE')),
    refundable BOOLEAN NOT NULL DEFAULT TRUE,
    refund_deadline_hours INTEGER NOT NULL DEFAULT 24 CHECK (refund_deadline_hours >= 0),
    restock_on_refund BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(10) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_products_cost_model CHECK (
        (cost_model = 'WHOLESALE' AND unit_net_cost IS NOT NULL AND unit_net_cost >= 0)
        OR (cost_model = 'COMMISSION' AND commission_percent IS NOT NULL AND commission_percent BETWEEN 0 AND 100)),
    CONSTRAINT ck_products_validity CHECK (valid_until > valid_from)
);
CREATE INDEX idx_products_partner ON partner_products (partner_id);
CREATE INDEX idx_products_category ON partner_products (category_id);
CREATE INDEX idx_products_valid_until ON partner_products (valid_until);
CREATE INDEX idx_products_event_starts ON partner_products (event_starts_at);
--rollback DROP TABLE partner_products;

--changeset buta:004-04
CREATE TABLE product_inventory (
    product_id UUID PRIMARY KEY REFERENCES partner_products (id) ON DELETE CASCADE,
    total_quantity INTEGER NOT NULL CHECK (total_quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    sold_quantity INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_inventory_consistency CHECK (reserved_quantity >= 0 AND sold_quantity >= 0 AND reserved_quantity + sold_quantity <= total_quantity)
);
--rollback DROP TABLE product_inventory;

--changeset buta:004-05
CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES partner_products (id),
    movement_type VARCHAR(20) NOT NULL CHECK (movement_type IN ('ADJUSTMENT', 'RESERVE', 'RELEASE', 'SELL', 'RESTOCK')),
    quantity INTEGER NOT NULL,
    order_id UUID,
    reason VARCHAR(200),
    actor_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_inventory_movements_product ON inventory_movements (product_id, created_at);
--rollback DROP TABLE inventory_movements;

--changeset buta:004-06
CREATE TABLE offers (
    id UUID PRIMARY KEY,
    slug VARCHAR(100) NOT NULL,
    offer_type VARCHAR(10) NOT NULL CHECK (offer_type IN ('SINGLE', 'BUNDLE')),
    primary_category_id UUID NOT NULL REFERENCES categories (id),
    title JSONB NOT NULL CHECK (nullif(title ->> 'az', '') IS NOT NULL AND nullif(title ->> 'en', '') IS NOT NULL),
    subtitle JSONB NOT NULL CHECK (nullif(subtitle ->> 'az', '') IS NOT NULL AND nullif(subtitle ->> 'en', '') IS NOT NULL),
    description JSONB NOT NULL CHECK (nullif(description ->> 'az', '') IS NOT NULL AND nullif(description ->> 'en', '') IS NOT NULL),
    image_path VARCHAR(300),
    selling_price NUMERIC(12, 2) NOT NULL CHECK (selling_price > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'AZN' CHECK (currency = 'AZN'),
    visibility VARCHAR(10) NOT NULL CHECK (visibility IN ('PUBLIC', 'PLUS_ONLY')),
    status VARCHAR(10) NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'PAUSED', 'ARCHIVED')),
    sale_starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sale_ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    max_per_order INTEGER NOT NULL DEFAULT 10 CHECK (max_per_order BETWEEN 1 AND 10),
    business_priority INTEGER NOT NULL DEFAULT 0 CHECK (business_priority BETWEEN 0 AND 100),
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    search_key VARCHAR(1000) NOT NULL DEFAULT '',
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_offers_slug UNIQUE (slug),
    CONSTRAINT ck_offers_sale_window CHECK (sale_ends_at > sale_starts_at)
);
CREATE INDEX idx_offers_status_sale_end ON offers (status, sale_ends_at);
CREATE INDEX idx_offers_category ON offers (primary_category_id);
CREATE INDEX idx_offers_featured ON offers (featured) WHERE featured;
--rollback DROP TABLE offers;

--changeset buta:004-07
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT count(*) FROM pg_extension WHERE extname = 'pg_trgm'
CREATE INDEX idx_offers_search_key_trgm ON offers USING gin (search_key gin_trgm_ops);
--rollback DROP INDEX IF EXISTS idx_offers_search_key_trgm;

--changeset buta:004-08
CREATE TABLE offer_components (
    id UUID PRIMARY KEY,
    offer_id UUID NOT NULL REFERENCES offers (id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES partner_products (id),
    quantity INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 10),
    negotiated_unit_cost NUMERIC(12, 2) CHECK (negotiated_unit_cost IS NULL OR negotiated_unit_cost >= 0),
    sort_order INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_offer_components UNIQUE (offer_id, product_id)
);
CREATE INDEX idx_offer_components_product ON offer_components (product_id);
--rollback DROP TABLE offer_components;

--changeset buta:004-09
CREATE TABLE campaigns (
    id UUID PRIMARY KEY,
    slug VARCHAR(80) NOT NULL,
    name JSONB NOT NULL CHECK (nullif(name ->> 'az', '') IS NOT NULL AND nullif(name ->> 'en', '') IS NOT NULL),
    banner_text JSONB NOT NULL CHECK (nullif(banner_text ->> 'az', '') IS NOT NULL AND nullif(banner_text ->> 'en', '') IS NOT NULL),
    audience VARCHAR(10) NOT NULL CHECK (audience IN ('ALL', 'PLUS')),
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    bonus_points_per_unit INTEGER NOT NULL DEFAULT 0 CHECK (bonus_points_per_unit >= 0),
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(10) NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_campaigns_slug UNIQUE (slug),
    CONSTRAINT ck_campaigns_window CHECK (ends_at > starts_at)
);
CREATE TABLE campaign_offers (
    campaign_id UUID NOT NULL REFERENCES campaigns (id) ON DELETE CASCADE,
    offer_id UUID NOT NULL REFERENCES offers (id) ON DELETE CASCADE,
    PRIMARY KEY (campaign_id, offer_id)
);
CREATE INDEX idx_campaign_offers_offer ON campaign_offers (offer_id);
--rollback DROP TABLE campaign_offers; DROP TABLE campaigns;
