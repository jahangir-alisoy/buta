--liquibase formatted sql

--changeset buta:011-01
ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMP WITH TIME ZONE;
UPDATE users SET email_verified_at = created_at;
--rollback ALTER TABLE users DROP COLUMN email_verified_at;

--changeset buta:011-02
INSERT INTO users (id, email, email_canonical, username, first_name, last_name, status, referral_code, email_verified_at, created_at, updated_at) VALUES
('a0000000-0000-0000-0000-000000000003', 'cahangiralisoy@icloud.com', 'cahangiralisoy@icloud.com', 'cahangir.alisoy', 'Cahangir', 'Alisoy', 'ACTIVE', 'CAHANG2R', now(), now(), now())
ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role, source, granted_at)
SELECT u.id, 'CUSTOMER', 'SEED', now()
FROM users u
WHERE u.email = 'cahangiralisoy@icloud.com'
ON CONFLICT (user_id, role) DO NOTHING;
INSERT INTO user_preferences (user_id, locale, theme, personalization_enabled, preferred_category_ids, updated_at)
SELECT u.id, 'az', 'SYSTEM', FALSE, '[]'::jsonb, now()
FROM users u
WHERE u.email = 'cahangiralisoy@icloud.com'
ON CONFLICT (user_id) DO NOTHING;
--rollback DELETE FROM users WHERE id = 'a0000000-0000-0000-0000-000000000003';
