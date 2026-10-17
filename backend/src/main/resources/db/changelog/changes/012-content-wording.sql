--liquibase formatted sql

--changeset buta:012-01
UPDATE offers SET
    title = replace(title::text, ' (demo)', '')::jsonb,
    subtitle = replace(subtitle::text, ' (demo)', '')::jsonb,
    description = replace(replace(replace(replace(description::text,
        'Demo offer. ', ''), 'Nümayiş təklifi. ', ''),
        ' Price and availability are illustrative.', ''), ' Qiymət və mövcudluq illüstrativdir.', '')::jsonb;
UPDATE partner_products SET
    description = replace(replace(replace(replace(description::text,
        'Demo product: partner, price and availability are illustrative. ', ''), 'Nümayiş məhsuludur: tərəfdaş, qiymət və mövcudluq illüstrativdir. ', ''),
        ' Price and availability are illustrative.', ''), ' Qiymət və mövcudluq illüstrativdir.', '')::jsonb,
    terms = replace(replace(terms::text,
        'Demo product: partner, price and availability are illustrative. ', ''), 'Nümayiş məhsuludur: tərəfdaş, qiymət və mövcudluq illüstrativdir. ', '')::jsonb,
    redemption_instructions = replace(replace(redemption_instructions::text,
        'Demo product: partner, price and availability are illustrative. ', ''), 'Nümayiş məhsuludur: tərəfdaş, qiymət və mövcudluq illüstrativdir. ', '')::jsonb,
    venue_address = CASE WHEN venue_address LIKE '%demo%' THEN NULL ELSE venue_address END;
UPDATE campaigns SET
    name = replace(replace(name::text, ' (demo)', ''), ' (Demo)', '')::jsonb,
    banner_text = replace(banner_text::text, ' (demo)', '')::jsonb;
UPDATE partners SET address = NULL WHERE address LIKE '%demo%';
UPDATE partners SET description = '{"az": "Yeni filmlər, klassik seanslar və rahat zallar.", "en": "New releases, classic screenings and comfortable halls."}'::jsonb WHERE slug = 'lumen-cinema';
UPDATE partners SET description = '{"az": "Dram, komediya və ailə tamaşaları səhnəsi.", "en": "A stage for drama, comedy and family shows."}'::jsonb WHERE slug = 'blue-curtain-theatre';
UPDATE partners SET description = '{"az": "İnteraktiv zallar və planetarium şouları.", "en": "Interactive halls and planetarium shows."}'::jsonb WHERE slug = 'orbit-science-museum';
UPDATE partners SET description = '{"az": "Müasir incəsənət sərgiləri və axşam turları.", "en": "Contemporary art exhibitions and evening tours."}'::jsonb WHERE slug = 'linen-art-gallery';
UPDATE partners SET description = '{"az": "Kitablar, kuponlar və oxu axşamları.", "en": "Books, gift coupons and reading evenings."}'::jsonb WHERE slug = 'paper-lantern-books';
UPDATE partners SET description = '{"az": "Keramika, xəttatlıq və digər master-klaslar.", "en": "Pottery, calligraphy and other hands-on workshops."}'::jsonb WHERE slug = 'maker-loft-workshops';
UPDATE partners SET description = '{"az": "Dəniz kənarında açıq havada musiqi festivalı.", "en": "An open-air music festival by the sea."}'::jsonb WHERE slug = 'shoreline-music-festival';
UPDATE partners SET description = '{"az": "Şəhər nəqliyyatı üçün endirimli gediş kartları.", "en": "Discounted ride passes for city transport."}'::jsonb WHERE slug = 'citymove-transit';
UPDATE partners SET description = '{"az": "Reklamsız musiqi yayımı abunəlikləri.", "en": "Ad-free music streaming subscriptions."}'::jsonb WHERE slug = 'melodia-stream';
UPDATE partners SET description = '{"az": "Təzə qəhvə və şirniyyat.", "en": "Fresh coffee and pastries."}'::jsonb WHERE slug = 'morning-roast-cafe';
--rollback SELECT 1;
