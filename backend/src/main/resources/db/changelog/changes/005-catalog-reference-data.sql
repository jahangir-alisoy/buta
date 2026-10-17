--liquibase formatted sql

--changeset buta:005-01
INSERT INTO categories (id, slug, name, icon_key, sort_order, active, created_at, updated_at) VALUES
('10000000-0000-0000-0000-000000000001', 'cinema', '{"az": "Kino", "en": "Cinema"}'::jsonb, 'film', 1, TRUE, now(), now()),
('10000000-0000-0000-0000-000000000002', 'theatre', '{"az": "Teatr", "en": "Theatre"}'::jsonb, 'drama', 2, TRUE, now(), now()),
('10000000-0000-0000-0000-000000000003', 'festivals', '{"az": "Festivallar", "en": "Festivals"}'::jsonb, 'music', 3, TRUE, now(), now()),
('10000000-0000-0000-0000-000000000004', 'workshops', '{"az": "Master-klaslar", "en": "Workshops"}'::jsonb, 'palette', 4, TRUE, now(), now()),
('10000000-0000-0000-0000-000000000005', 'museums', '{"az": "Muzeylər", "en": "Museums"}'::jsonb, 'landmark', 5, TRUE, now(), now()),
('10000000-0000-0000-0000-000000000006', 'exhibitions', '{"az": "Sərgilər", "en": "Exhibitions"}'::jsonb, 'frame', 6, TRUE, now(), now()),
('10000000-0000-0000-0000-000000000007', 'book-coupons', '{"az": "Kitab kuponları", "en": "Book coupons"}'::jsonb, 'book', 7, TRUE, now(), now()),
('10000000-0000-0000-0000-000000000008', 'transport', '{"az": "Nəqliyyat endirimləri", "en": "Transport discounts"}'::jsonb, 'bus', 8, TRUE, now(), now()),
('10000000-0000-0000-0000-000000000009', 'services', '{"az": "Digər xidmətlər", "en": "Other services"}'::jsonb, 'sparkles', 9, TRUE, now(), now())
ON CONFLICT (id) DO NOTHING;
--rollback DELETE FROM categories WHERE id::text LIKE '10000000-0000-0000-0000-00000000000%';
