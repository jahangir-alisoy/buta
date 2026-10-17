--liquibase formatted sql

--changeset buta:008-01
INSERT INTO membership_plans (code, name, description, features, price, billing_period, purchasable, updated_at) VALUES
('FREE', '{"az": "Free", "en": "Free"}'::jsonb,
 '{"az": "Bütün ictimai təklifləri görün və alın.", "en": "Browse and buy every public offer."}'::jsonb,
 '[]'::jsonb, NULL, NULL, FALSE, now()),
('PLUS', '{"az": "Plus", "en": "Plus"}'::jsonb,
 '{"az": "Xallar, səviyyələr, tapşırıqlar, şəxsi tövsiyələr və eksklüziv təkliflər.", "en": "Points, tiers, tasks, personal recommendations and exclusive offers."}'::jsonb,
 '["LOYALTY_POINTS", "LOYALTY_TASKS", "TIERS", "POINTS_REDEMPTION", "PERSONALIZED_RECOMMENDATIONS", "EXCLUSIVE_OFFERS"]'::jsonb, NULL, NULL, FALSE, now())
ON CONFLICT (code) DO NOTHING;
--rollback DELETE FROM membership_plans WHERE code IN ('FREE', 'PLUS');

--changeset buta:008-02
INSERT INTO loyalty_tiers (code, name, threshold_points, earn_multiplier, benefits, sort_order) VALUES
('BRONZE', '{"az": "Bürünc", "en": "Bronze"}'::jsonb, 0, 1.00,
 '{"az": ["Hər 1 ₼ üçün 1 xal"], "en": ["1 point for every 1 ₼"]}'::jsonb, 1),
('SILVER', '{"az": "Gümüş", "en": "Silver"}'::jsonb, 100, 1.10,
 '{"az": ["Xallarda 10% bonus", "Gümüş üzvlər üçün kampaniyalar"], "en": ["10% bonus points", "Silver member campaigns"]}'::jsonb, 2),
('GOLD', '{"az": "Qızıl", "en": "Gold"}'::jsonb, 500, 1.25,
 '{"az": ["Xallarda 25% bonus", "Eksklüziv Plus təkliflərinə ilk giriş"], "en": ["25% bonus points", "First access to exclusive Plus offers"]}'::jsonb, 3)
ON CONFLICT (code) DO NOTHING;
--rollback DELETE FROM loyalty_tiers WHERE code IN ('BRONZE', 'SILVER', 'GOLD');
