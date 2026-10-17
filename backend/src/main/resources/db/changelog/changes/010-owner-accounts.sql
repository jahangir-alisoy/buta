--liquibase formatted sql

--changeset buta:010-01
INSERT INTO users (id, email, email_canonical, username, first_name, last_name, status, referral_code, created_at, updated_at) VALUES
('a0000000-0000-0000-0000-000000000001', 'jahangir.alisoy@gmail.com', 'jahangiralisoy@gmail.com', 'jahangir', 'Jahangir', 'Alisoy', 'ACTIVE', 'JAHANG1R', now(), now()),
('a0000000-0000-0000-0000-000000000002', 'cahangiralisoy313@gmail.com', 'cahangiralisoy313@gmail.com', 'cahangir', 'Cahangir', 'Alisoy', 'ACTIVE', 'CAHANG1R', now(), now())
ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role, source, granted_at)
SELECT u.id, r.role, 'SEED', now()
FROM users u
CROSS JOIN (VALUES ('CUSTOMER'), ('ADMIN')) AS r (role)
WHERE u.email IN ('jahangir.alisoy@gmail.com', 'cahangiralisoy313@gmail.com')
ON CONFLICT (user_id, role) DO UPDATE SET source = 'SEED' WHERE user_roles.role = 'ADMIN';
INSERT INTO user_preferences (user_id, locale, theme, personalization_enabled, preferred_category_ids, updated_at)
SELECT u.id, 'az', 'SYSTEM', FALSE, '[]'::jsonb, now()
FROM users u
WHERE u.email IN ('jahangir.alisoy@gmail.com', 'cahangiralisoy313@gmail.com')
ON CONFLICT (user_id) DO NOTHING;
--rollback DELETE FROM users WHERE id IN ('a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002');
