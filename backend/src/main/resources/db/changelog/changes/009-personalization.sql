--liquibase formatted sql

--changeset buta:009-01
CREATE TABLE interaction_events (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users (id) ON DELETE CASCADE,
    event_type VARCHAR(30) NOT NULL CHECK (event_type IN ('DETAIL_VIEW', 'SEARCH', 'CATEGORY_VIEW', 'CART_ADD', 'FAVORITE_ADD', 'FAVORITE_REMOVE', 'PURCHASE', 'RECOMMENDATION_CLICK')),
    offer_id UUID REFERENCES offers (id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories (id) ON DELETE CASCADE,
    query_text VARCHAR(100),
    recommendation_request_id VARCHAR(64),
    placement VARCHAR(20),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_interactions_user_time ON interaction_events (user_id, occurred_at DESC);
CREATE INDEX idx_interactions_offer_type ON interaction_events (offer_id, event_type, occurred_at);
CREATE INDEX idx_interactions_request ON interaction_events (recommendation_request_id);
--rollback DROP TABLE interaction_events;

--changeset buta:009-02
CREATE TABLE user_interest_profiles (
    user_id UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    category_affinity JSONB NOT NULL DEFAULT '{}'::jsonb,
    partner_affinity JSONB NOT NULL DEFAULT '{}'::jsonb,
    purchased_category_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    price_band_low NUMERIC(12, 2),
    price_band_high NUMERIC(12, 2),
    price_sensitivity NUMERIC(5, 2) NOT NULL DEFAULT 0,
    signals_count INTEGER NOT NULL DEFAULT 0,
    computed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE recommendation_impressions (
    id UUID PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL,
    user_id UUID REFERENCES users (id) ON DELETE CASCADE,
    placement VARCHAR(20) NOT NULL,
    algorithm_version VARCHAR(20) NOT NULL,
    variant VARCHAR(30) NOT NULL,
    offer_id UUID NOT NULL REFERENCES offers (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    score NUMERIC(6, 2) NOT NULL,
    components JSONB NOT NULL DEFAULT '{}'::jsonb,
    reason_code VARCHAR(40),
    served_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_impressions_user_offer ON recommendation_impressions (user_id, offer_id, served_at);
CREATE INDEX idx_impressions_request ON recommendation_impressions (request_id);
--rollback DROP TABLE recommendation_impressions; DROP TABLE user_interest_profiles;
