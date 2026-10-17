--liquibase formatted sql

--changeset buta:902-01 context:demo
INSERT INTO users (id, email, email_canonical, username, first_name, last_name, phone, status, referral_code, created_at, updated_at) VALUES
('40000000-0000-0000-0000-000000000001', 'admin@buta.test', 'admin@buta.test', 'admin', 'Demo', 'Admin', NULL, 'ACTIVE', 'DEMOADMN', now() - interval '120 days', now()),
('40000000-0000-0000-0000-000000000002', 'aysel.free@buta.test', 'aysel.free@buta.test', 'aysel', 'Aysel', 'Məmmədova', NULL, 'ACTIVE', 'DEMOAYSE', now() - interval '120 days', now()),
('40000000-0000-0000-0000-000000000003', 'kamran.plus@buta.test', 'kamran.plus@buta.test', 'kamran', 'Kamran', 'Əliyev', NULL, 'ACTIVE', 'DEMOKAMR', now() - interval '120 days', now()),
('40000000-0000-0000-0000-000000000004', 'leyla.plus@buta.test', 'leyla.plus@buta.test', 'leyla', 'Leyla', 'Həsənova', NULL, 'ACTIVE', 'DEMOLEYA', now() - interval '120 days', now());
INSERT INTO user_roles (user_id, role, source, granted_at) VALUES
('40000000-0000-0000-0000-000000000001', 'CUSTOMER', 'SEED', now()),
('40000000-0000-0000-0000-000000000002', 'CUSTOMER', 'SEED', now()),
('40000000-0000-0000-0000-000000000003', 'CUSTOMER', 'SEED', now()),
('40000000-0000-0000-0000-000000000004', 'CUSTOMER', 'SEED', now()),
('40000000-0000-0000-0000-000000000001', 'ADMIN', 'SEED', now());
INSERT INTO user_preferences (user_id, locale, theme, personalization_enabled, personalization_decided_at, preferred_category_ids, updated_at) VALUES
('40000000-0000-0000-0000-000000000001', 'az', 'SYSTEM', FALSE, NULL, '[]'::jsonb, now()),
('40000000-0000-0000-0000-000000000002', 'az', 'SYSTEM', FALSE, NULL, '[]'::jsonb, now()),
('40000000-0000-0000-0000-000000000003', 'az', 'SYSTEM', TRUE, now() - interval '40 days', '["10000000-0000-0000-0000-000000000001"]'::jsonb, now()),
('40000000-0000-0000-0000-000000000004', 'en', 'SYSTEM', TRUE, now() - interval '80 days', '["10000000-0000-0000-0000-000000000002", "10000000-0000-0000-0000-000000000006"]'::jsonb, now());
--rollback DELETE FROM users WHERE id::text LIKE '40000000-%';
