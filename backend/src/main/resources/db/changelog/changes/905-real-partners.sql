--liquibase formatted sql

--changeset buta:905-01 context:demo
UPDATE partners SET slug = 'cinemastercard', name = 'CineMastercard',
    description = '{"az": "Azərbaycanın premium kinoteatr şəbəkəsi; 2024-cü ilin martında Mastercard ilə tərəfdaşlığa qədər CinemaPlus adı ilə tanınırdı. Bakıdakı kinoteatrları arasında 28 Mall, Deniz Mall və Gənclik Mall var.", "en": "Premium cinema chain in Azerbaijan, known as CinemaPlus until its March 2024 partnership with Mastercard. Its Baku cinemas include 28 Mall, Deniz Mall and Ganjlik Mall."}'::jsonb,
    city = 'Baku', address = '28 Mall, Baku', website_url = 'https://cinemastercard.az', updated_at = now()
WHERE id = '20000000-0000-0000-0000-000000000001';
UPDATE partners SET slug = 'national-drama-theatre', name = 'Azerbaijan State Academic National Drama Theatre',
    description = '{"az": "Bakıda Azərbaycanın milli dram teatrı. Tarixi 10 mart 1873-cü ildə Mirzə Fətəli Axundovun “Lənkəran xanının veziri” komediyasının tamaşaya qoyulması ilə başlayır; 1960-cı ildən Füzuli meydanındakı binada fəaliyyət göstərir.", "en": "Azerbaijan’s national drama theatre in Baku. It traces its history to the staging of Mirza Fatali Akhundov’s comedy “The Vizier of Lankaran Khanate” on 10 March 1873 and has performed on Fuzuli Square since 1960."}'::jsonb,
    city = 'Baku', address = 'Fuzuli Square, Baku', website_url = NULL, updated_at = now()
WHERE id = '20000000-0000-0000-0000-000000000002';
UPDATE partners SET slug = 'tusi-bohm-planetarium', name = 'Tusi-Bohm Planetarium',
    description = '{"az": "Neftçilər prospektindəki Park Bulvar mərkəzinin dördüncü mərtəbəsində yerləşən rəqəmsal planetari; 10 metrlik günbəzi və 58 yeri var.", "en": "Digital planetarium on the fourth floor of the Park Bulvar centre on Neftchilar Avenue, with a 10-metre dome and 58 seats."}'::jsonb,
    city = 'Baku', address = 'Park Bulvar, Neftchilar Avenue, Baku', website_url = NULL, updated_at = now()
WHERE id = '20000000-0000-0000-0000-000000000003';
UPDATE partners SET slug = 'yarat', name = 'YARAT Contemporary Art Space',
    description = '{"az": "2011-ci ildə rəssam Aida Mahmudova tərəfindən yaradılmış qeyri-kommersiya müasir incəsənət təşkilatı. Bayılda, Dövlət Bayrağı Meydanının yaxınlığındakı əsas məkanı 2015-ci ildə açılıb.", "en": "Non-profit contemporary art organisation founded in 2011 by artist Aida Mahmudova. Its main venue in Bayil, near National Flag Square, opened in 2015."}'::jsonb,
    city = 'Baku', address = 'Bayil, near National Flag Square, Baku', website_url = 'https://yarat.az', updated_at = now()
WHERE id = '20000000-0000-0000-0000-000000000004';
UPDATE partners SET slug = 'ali-and-nino', name = 'Ali and Nino',
    description = '{"az": "Bakının ticarət mərkəzlərində mağazaları və onlayn mağazası olan Azərbaycan kitab mağazaları şəbəkəsi və nəşriyyatı. 2009-cu ildə ölkədə ilk kitab-kafeni açdığını bildirir.", "en": "Azerbaijani bookstore chain and publishing house with stores in Baku shopping centres and an online shop. In 2009 it opened what it calls the country’s first book café."}'::jsonb,
    city = 'Baku', address = NULL, website_url = 'https://alinino.az', updated_at = now()
WHERE id = '20000000-0000-0000-0000-000000000005';
UPDATE partners SET slug = 'icherisheher', name = 'Icherisheher (Old City)',
    description = '{"az": "Qız qalası və Şirvanşahlar sarayının yerləşdiyi, qala divarları ilə əhatələnmiş qədim Bakı. 1985-ci ildən dövlət tarix-memarlıq qoruğu, 2000-ci ildən UNESCO-nun Ümumdünya İrsi obyektidir.", "en": "Baku’s walled Old City, home to the Maiden Tower and the Palace of the Shirvanshahs. It has been a state historical-architectural reserve since 1985 and a UNESCO World Heritage Site since 2000."}'::jsonb,
    city = 'Baku', address = 'Icherisheher, Baku', website_url = 'https://icherisheher.gov.az', updated_at = now()
WHERE id = '20000000-0000-0000-0000-000000000006';
UPDATE partners SET slug = 'baku-jazz-festival', name = 'Baku Jazz Festival',
    description = '{"az": "Caz ifaçısı Rain Sultanovun təsis etdiyi və bədii rəhbəri olduğu Bakı beynəlxalq caz festivalı.", "en": "International jazz festival in Baku, founded by jazz musician Rain Sultanov, who is also its artistic director."}'::jsonb,
    city = 'Baku', address = NULL, website_url = 'https://bakujazzfestival.com', updated_at = now()
WHERE id = '20000000-0000-0000-0000-000000000007';
UPDATE partners SET slug = 'bakubus', name = 'BakuBus',
    description = '{"az": "2014-cü il aprelin 3-də Bakıda yaradılmış ictimai avtobus operatoru; Gəncədə də şəhər marşrutlarına xidmət edir.", "en": "Public bus operator established in Baku on 3 April 2014; it also runs city routes in Ganja."}'::jsonb,
    city = 'Baku', address = NULL, website_url = NULL, updated_at = now()
WHERE id = '20000000-0000-0000-0000-000000000008';
UPDATE partners SET slug = 'cafecity', name = 'CafeCity',
    description = '{"az": "Şəhərin müxtəlif yerlərində filialları olan, səhər yeməyi ilə tanınan köhnə Bakı kafe şəbəkəsi.", "en": "Long-running Baku café chain with several branches across the city, popular for breakfast."}'::jsonb,
    city = 'Baku', address = NULL, website_url = NULL, updated_at = now()
WHERE id = '20000000-0000-0000-0000-000000000010';
--rollback SELECT 1;

--changeset buta:905-02 context:demo
UPDATE partner_products SET venue_name = 'CineMastercard 28 Mall', venue_address = '28 Mall, Baku', updated_at = now()
WHERE partner_id = '20000000-0000-0000-0000-000000000001' AND venue_name IS NOT NULL;
UPDATE partner_products SET venue_name = 'National Drama Theatre', venue_address = 'Fuzuli Square, Baku', updated_at = now()
WHERE partner_id = '20000000-0000-0000-0000-000000000002' AND venue_name IS NOT NULL;
UPDATE partner_products SET venue_name = 'Tusi-Bohm Planetarium', venue_address = 'Park Bulvar, Neftchilar Avenue, Baku', updated_at = now()
WHERE partner_id = '20000000-0000-0000-0000-000000000003' AND venue_name IS NOT NULL;
UPDATE partner_products SET venue_name = 'YARAT Contemporary Art Space', venue_address = 'Bayil, near National Flag Square, Baku', updated_at = now()
WHERE partner_id = '20000000-0000-0000-0000-000000000004' AND venue_name IS NOT NULL;
UPDATE partner_products SET venue_name = 'Icherisheher (Old City)', venue_address = 'Icherisheher, Baku', updated_at = now()
WHERE partner_id = '20000000-0000-0000-0000-000000000006' AND venue_name IS NOT NULL;
UPDATE partner_products SET venue_name = 'Baku Jazz Festival', venue_address = NULL, updated_at = now()
WHERE partner_id = '20000000-0000-0000-0000-000000000007' AND venue_name IS NOT NULL;
--rollback SELECT 1;

--changeset buta:905-03 context:demo
INSERT INTO partners (id, slug, name, description, city, address, website_url, logo_path, demo, status, created_at, updated_at) VALUES
('20000000-0000-0000-0000-000000000011', 'park-cinema', 'Park Cinema', '{"az": "2010-cu ildə yaradılmış kinoteatr şəbəkəsi; ilk kinoteatrı Park Bulvar mərkəzində açılıb. Məkanları arasında Park Bulvar, Metro Park və Flame Towers var.", "en": "Cinema chain established in 2010, whose first cinema opened in the Park Bulvar centre. Its venues include Park Bulvar, Metro Park and Flame Towers."}'::jsonb, 'Baku', 'Park Bulvar, 78 Neftchilar Avenue, Baku', 'https://parkcinema.az', '/images/demo/partner-cinema.svg', TRUE, 'ACTIVE', now(), now()),
('20000000-0000-0000-0000-000000000012', 'nizami-cinema-center', 'Nizami Cinema Center', '{"az": "1940-cı ildə açılmış və şair Nizami Gəncəvinin adını daşıyan Azərbaycanın ən qədim kinoteatrlarından biri. 2011-ci ildə təmirdən sonra yenidən açılıb.", "en": "One of the oldest cinemas in Azerbaijan, opened in 1940 and named after the poet Nizami Ganjavi. It reopened in 2011 after renovation."}'::jsonb, 'Baku', NULL, NULL, '/images/demo/partner-cinema.svg', TRUE, 'ACTIVE', now(), now()),
('20000000-0000-0000-0000-000000000013', 'opera-and-ballet-theatre', 'Azerbaijan State Academic Opera and Ballet Theatre', '{"az": "1911-ci ildə Nizami küçəsindəki tarixi binada açılmış Bakı opera və balet teatrı.", "en": "Baku’s opera and ballet house, opened in 1911 in its historic building on Nizami Street."}'::jsonb, 'Baku', '95 Nizami Street, Baku', 'https://operabalet.az', '/images/demo/partner-theatre.svg', TRUE, 'ACTIVE', now(), now()),
('20000000-0000-0000-0000-000000000014', 'state-philharmonic-hall', 'Azerbaijan State Philharmonic Hall', '{"az": "1912-ci ildə tikilmiş binada yerləşən, 1937-ci ildən bəstəkar Müslüm Maqomayevin adını daşıyan konsert zalı.", "en": "Concert hall in a building completed in 1912, named after the composer Muslim Magomayev since 1937."}'::jsonb, 'Baku', NULL, 'https://filarmoniya.az', '/images/demo/partner-festivals.svg', TRUE, 'ACTIVE', now(), now()),
('20000000-0000-0000-0000-000000000015', 'international-mugham-center', 'International Mugham Center', '{"az": "Bakı bulvarında muğama həsr olunmuş konsert məkanı. 27 dekabr 2008-ci ildə açılıb; binası tar formasındadır və 350 yerlik konsert zalı var.", "en": "Concert venue for mugham on Baku Boulevard, opened on 27 December 2008. Its building is shaped like a tar and has a 350-seat concert hall."}'::jsonb, 'Baku', 'Baku Boulevard, Baku', NULL, '/images/demo/partner-festivals.svg', TRUE, 'ACTIVE', now(), now()),
('20000000-0000-0000-0000-000000000016', 'state-puppet-theatre', 'Azerbaijan State Puppet Theatre', '{"az": "Yazıçı Abdulla Şaiqin adını daşıyan kukla teatrı; Neftçilər prospekti 36-da, 1910-cu ildə açılmış binada yerləşir. Tamaşalar Azərbaycan və rus dillərində göstərilir.", "en": "Puppet theatre named after the writer Abdulla Shaig, in a building on Neftchilar Avenue that opened in 1910. It performs in Azerbaijani and Russian."}'::jsonb, 'Baku', '36 Neftchilar Avenue, Baku', NULL, '/images/demo/partner-theatre.svg', TRUE, 'ACTIVE', now(), now()),
('20000000-0000-0000-0000-000000000017', 'heydar-aliyev-center', 'Heydar Aliyev Center', '{"az": "Zaha Hadidin layihələndirdiyi mədəniyyət mərkəzi; auditoriyası, sərgi qalereyaları və muzeyi var. Burada Yayoi Kusama və Endi Uorhol kimi rəssamların sərgiləri keçirilib.", "en": "Cultural centre designed by Zaha Hadid, with an auditorium, exhibition galleries and a museum. It has hosted exhibitions of artists such as Yayoi Kusama and Andy Warhol."}'::jsonb, 'Baku', '1 Heydar Aliyev Avenue, Baku', 'https://heydaraliyevcenter.az', '/images/demo/partner-exhibitions.svg', TRUE, 'ACTIVE', now(), now()),
('20000000-0000-0000-0000-000000000018', 'national-carpet-museum', 'Azerbaijan National Carpet Museum', '{"az": "Bakı dənizkənarı bulvarında yerləşən Azərbaycan xalçaçılıq muzeyi. 2014-cü ildən avstriyalı memar Frans Yanzın layihələndirdiyi, bükülmüş xalça formasında binada fəaliyyət göstərir.", "en": "Museum of Azerbaijani carpet weaving on the Baku seafront. Since 2014 it has occupied a building shaped like a rolled-up carpet, designed by the Austrian architect Franz Janz."}'::jsonb, 'Baku', '28 Mikayil Huseynov Street, Baku', 'https://azcarpetmuseum.az', '/images/demo/partner-museums.svg', TRUE, 'ACTIVE', now(), now()),
('20000000-0000-0000-0000-000000000019', 'museum-of-modern-art', 'Baku Museum of Modern Art', '{"az": "2009-cu ilin martında açılmış, 800-dən çox əsərdən ibarət kolleksiyası olan XX əsr və müasir incəsənət muzeyi.", "en": "Museum of 20th-century and contemporary art, opened in March 2009, with a collection of more than 800 works."}'::jsonb, 'Baku', '5 Yusif Safarov Street, Baku', NULL, '/images/demo/partner-exhibitions.svg', TRUE, 'ACTIVE', now(), now()),
('20000000-0000-0000-0000-000000000020', 'national-art-museum', 'National Art Museum of Azerbaijan', '{"az": "Bakının mərkəzində, Niyazi küçəsində yerləşən milli təsviri sənət muzeyi; kolleksiyasında Azərbaycan rəngkarlığı, qrafikası və heykəltəraşlığı var.", "en": "National museum of fine art on Niyazi Street in central Baku, with collections of Azerbaijani painting, graphics and sculpture."}'::jsonb, 'Baku', '11/9 Niyazi Street, Baku', 'https://nationalartmuseum.az', '/images/demo/partner-museums.svg', TRUE, 'ACTIVE', now(), now()),
('20000000-0000-0000-0000-000000000021', 'gabala-music-festival', 'Gabala International Music Festival', '{"az": "2009-cu ildən Qəbələdə keçirilən illik yay klassik musiqi festivalı; konsertlər Konqres Mərkəzində və açıq səhnədə təşkil olunur.", "en": "Annual summer classical music festival held in Gabala since 2009, with concerts at the Congress Centre and on an open-air stage."}'::jsonb, 'Gabala', NULL, NULL, '/images/demo/partner-festivals.svg', TRUE, 'ACTIVE', now(), now()),
('20000000-0000-0000-0000-000000000022', 'libraff', 'Libraff', '{"az": "TEAS Press nəşriyyatına məxsus kitab mağazaları şəbəkəsi; mağazaları Bakının ticarət mərkəzlərində, hava limanlarında və Gəncədədir.", "en": "Bookstore chain owned by the publisher TEAS Press, with stores in Baku shopping centres and airports, as well as in Ganja."}'::jsonb, 'Baku', NULL, 'https://libraff.az', '/images/demo/partner-book-coupons.svg', TRUE, 'ACTIVE', now(), now())
ON CONFLICT (id) DO NOTHING;
--rollback DELETE FROM partners WHERE id IN ('20000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000012', '20000000-0000-0000-0000-000000000013', '20000000-0000-0000-0000-000000000014', '20000000-0000-0000-0000-000000000015', '20000000-0000-0000-0000-000000000016', '20000000-0000-0000-0000-000000000017', '20000000-0000-0000-0000-000000000018', '20000000-0000-0000-0000-000000000019', '20000000-0000-0000-0000-000000000020', '20000000-0000-0000-0000-000000000021', '20000000-0000-0000-0000-000000000022');
