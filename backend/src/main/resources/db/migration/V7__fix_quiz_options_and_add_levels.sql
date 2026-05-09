-- ============================================================
-- V7: Fix quiz options JSONB format for existing MEDIUM quizzes,
--     add missing MEDIUM quizzes, and add EASY + HARD content
--     for all 8 seed articles.
-- ============================================================

-- First, increase the length of correct_answer to support long answers in SHORT_ANSWER quizzes
ALTER TABLE quizzes ALTER COLUMN correct_answer TYPE TEXT;

-- ── Fix existing MEDIUM quiz options (articles 1-3 had old array format) ──────

UPDATE quizzes SET
  options = '{"choices":[{"key":"0","text":"Computers are getting smaller every year","explanation":null},{"key":"1","text":"Technology is becoming invisible but raises privacy questions","explanation":null},{"key":"2","text":"Scientists no longer use large computers","explanation":null},{"key":"3","text":"Smartphones are replacing all other devices","explanation":null}],"explanations":null}'::jsonb
WHERE article_content_id = 'c1c1c1c1-1111-1111-1111-111111111111';

UPDATE quizzes SET
  options = '{"choices":[{"key":"0","text":"It improves metabolism and mood","explanation":null},{"key":"1","text":"It disrupts circadian rhythms and sleep hormones","explanation":null},{"key":"2","text":"It has no effect on human health","explanation":null},{"key":"3","text":"It helps people sleep deeper","explanation":null}],"explanations":null}'::jsonb
WHERE article_content_id = 'c2c2c2c2-2222-2222-2222-222222222222';

UPDATE quizzes SET
  options = '{"choices":[{"key":"0","text":"Because of changing consumer tastes","explanation":null},{"key":"1","text":"Because weather affects crops and invites speculation","explanation":null},{"key":"2","text":"Because shipping costs are unpredictable","explanation":null},{"key":"3","text":"Because roasters change recipes frequently","explanation":null}],"explanations":null}'::jsonb
WHERE article_content_id = 'c3c3c3c3-3333-3333-3333-333333333333';

-- ── Add missing MEDIUM quizzes (2 MCQ + 1 SHORT_ANSWER per article) ───────────
-- Articles 1-3 already have 1 quiz; need 2 more each.
-- Articles 4-8 have 0 quizzes; need all 3.

-- Article 1 MEDIUM – extra quizzes
INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What does "ambient computing" aim to achieve?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Making computers faster","explanation":null},{"key":"1","text":"Making computers visible and obvious","explanation":null},{"key":"2","text":"Integrating technology seamlessly into daily life","explanation":null},{"key":"3","text":"Replacing smartphones with wearables","explanation":null}],"explanations":null}'::jsonb,
 '2', 'Ambient computing aims to weave technology into everyday life so naturally that users do not notice it.', 'c1c1c1c1-1111-1111-1111-111111111111'),
(uuid_generate_v4(), NOW(), NOW(), 'In your own words, what is the main tradeoff of invisible computing?', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'Invisible computing makes life more convenient but raises serious privacy concerns because embedded sensors collect personal data without users noticing.', 'Invisible computing offers seamless convenience while quietly collecting personal data, which raises privacy and ownership questions.', 'c1c1c1c1-1111-1111-1111-111111111111');

-- Article 2 MEDIUM – extra quizzes
INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'Which type of light is most effective at suppressing melatonin?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Red light","explanation":null},{"key":"1","text":"Yellow light","explanation":null},{"key":"2","text":"Blue light","explanation":null},{"key":"3","text":"Green light","explanation":null}],"explanations":null}'::jsonb,
 '2', 'Blue light from screens is most effective at suppressing melatonin and can delay sleep by up to three hours.', 'c2c2c2c2-2222-2222-2222-222222222222'),
(uuid_generate_v4(), NOW(), NOW(), 'Why does the article suggest that fixing sleep problems may require rethinking city design?', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'Because light pollution from cities disrupts circadian rhythms at scale, so meaningful change requires redesigning how artificial light is used in public spaces.', 'Light pollution affects entire populations, so addressing it requires systemic changes to how cities and buildings use artificial light at night.', 'c2c2c2c2-2222-2222-2222-222222222222');

-- Article 3 MEDIUM – extra quizzes
INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What percentage of a $5 cup of coffee does the grower typically receive?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Around 50 cents","explanation":null},{"key":"1","text":"Around $1","explanation":null},{"key":"2","text":"Less than 5 cents","explanation":null},{"key":"3","text":"Around 25 cents","explanation":null}],"explanations":null}'::jsonb,
 '2', 'Farmers typically receive less than five cents from a $5 cup; the rest goes to processing, shipping, roasting, branding, and retail.', 'c3c3c3c3-3333-3333-3333-333333333333'),
(uuid_generate_v4(), NOW(), NOW(), 'What is the main risk of a producer selling forward contracts?', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'If prices rise after the contract is signed, the producer misses out on the higher price and cannot benefit from the upside.',
 'The producer locks in a price and is protected from drops, but if market prices rise significantly they miss the extra profit.', 'c3c3c3c3-3333-3333-3333-333333333333');

-- Article 4 MEDIUM – all 3 quizzes (none existed before)
INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What was the central musical innovation of jazz?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Fixed written scores performed precisely","explanation":null},{"key":"1","text":"Real-time improvisation within a shared structure","explanation":null},{"key":"2","text":"Electronic amplification of instruments","explanation":null},{"key":"3","text":"Complex orchestral arrangements","explanation":null}],"explanations":null}'::jsonb,
 '1', 'Jazz musicians improvise in real time, responding to each other, rather than executing a predetermined score.', 'c4c4c4c4-4444-4444-4444-444444444444'),
(uuid_generate_v4(), NOW(), NOW(), 'How did jazz influence fields beyond music?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"It had no influence outside of music","explanation":null},{"key":"1","text":"It replaced classical music entirely","explanation":null},{"key":"2","text":"Its collaborative model was adopted in architecture, software, and strategy","explanation":null},{"key":"3","text":"It led to the invention of electronic instruments","explanation":null}],"explanations":null}'::jsonb,
 '2', 'The jazz ensemble became a metaphor for high-functioning teams, influencing fields like architecture, software development, and military strategy.', 'c4c4c4c4-4444-4444-4444-444444444444'),
(uuid_generate_v4(), NOW(), NOW(), 'What does jazz suggest about the relationship between structure and creativity?', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'Structure and creativity can coexist; a shared framework can enable spontaneity rather than constrain it.',
 'Jazz shows that having rules or a framework does not prevent creativity—it can actually enable it by giving performers a foundation to build on.', 'c4c4c4c4-4444-4444-4444-444444444444');

-- Article 5 MEDIUM – all 3 quizzes
INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What does new research suggest about compulsive phone use?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"It is entirely caused by algorithmic manipulation","explanation":null},{"key":"1","text":"It is partly driven by human reward-seeking behavior, not just algorithms","explanation":null},{"key":"2","text":"It only affects teenagers","explanation":null},{"key":"3","text":"It has no connection to dopamine","explanation":null}],"explanations":null}'::jsonb,
 '1', 'Research suggests compulsive use stems partly from human reward-seeking biology, not just platform design.', 'c5c5c5c5-5555-5555-5555-555555555555'),
(uuid_generate_v4(), NOW(), NOW(), 'Why is regulating algorithms alone insufficient to solve the attention problem?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Because algorithms cannot be changed by law","explanation":null},{"key":"1","text":"Because users would simply switch to other platforms","explanation":null},{"key":"2","text":"Because part of the problem lies in human psychology, not platform design","explanation":null},{"key":"3","text":"Because attention spans have not actually decreased","explanation":null}],"explanations":null}'::jsonb,
 '2', 'If distraction is partly a feature of human psychology, technical fixes like adding friction address only part of the problem.', 'c5c5c5c5-5555-5555-5555-555555555555'),
(uuid_generate_v4(), NOW(), NOW(), 'What does the article mean by "restoring autonomy" over attention?', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'It means helping people reclaim conscious control over how they spend their time and attention, rather than acting on automatic impulses reinforced by digital reward systems.',
 'Restoring autonomy means enabling people to make deliberate choices about their attention rather than reacting automatically to notifications and algorithmic prompts.', 'c5c5c5c5-5555-5555-5555-555555555555');

-- Article 6 MEDIUM – all 3 quizzes
INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What do mycorrhizal fungi provide to trees in exchange for sugar?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Sunlight and warmth","explanation":null},{"key":"1","text":"Enhanced water and nutrient absorption","explanation":null},{"key":"2","text":"Protection from insects","explanation":null},{"key":"3","text":"Carbon dioxide for photosynthesis","explanation":null}],"explanations":null}'::jsonb,
 '1', 'Fungi dramatically increase a tree''s ability to absorb water and nutrients in exchange for sugars produced by photosynthesis.', 'c6c6c6c6-6666-6666-6666-666666666666'),
(uuid_generate_v4(), NOW(), NOW(), 'What practical implication does the article draw from mycorrhizal research?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Forests should be logged more carefully to preserve fungal networks","explanation":null},{"key":"1","text":"Single-species replanting is the most efficient approach","explanation":null},{"key":"2","text":"Healthy underground networks make forests more resilient to drought and disease","explanation":null},{"key":"3","text":"Trees should be kept separate to prevent disease spread","explanation":null}],"explanations":null}'::jsonb,
 '2', 'Healthy mycorrhizal networks improve forest resilience; practices that sever them—like monoculture replanting—can weaken forests.', 'c6c6c6c6-6666-6666-6666-666666666666'),
(uuid_generate_v4(), NOW(), NOW(), 'Why are researchers divided about what drives carbon sharing between trees?', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'It is unclear whether older trees actively support their offspring through kin recognition or whether the carbon flow is a passive physical consequence of how nutrients move through fungal networks.',
 'Researchers cannot determine whether trees selectively support related seedlings (kin recognition) or whether carbon simply flows through the network according to natural gradients.', 'c6c6c6c6-6666-6666-6666-666666666666');

-- Article 7 MEDIUM – all 3 quizzes
INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'Why did Burberry destroy £28 million of unsold inventory?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"It was damaged in a warehouse fire","explanation":null},{"key":"1","text":"To avoid paying import duties","explanation":null},{"key":"2","text":"To protect brand prestige by preventing discounts","explanation":null},{"key":"3","text":"Government regulations required it","explanation":null}],"explanations":null}'::jsonb,
 '2', 'Luxury brands destroy unsold goods rather than discount them, because discounting signals desperation and undermines the brand''s image of limitless demand.', 'c7c7c7c7-7777-7777-7777-777777777777'),
(uuid_generate_v4(), NOW(), NOW(), 'How do some luxury goods behave differently from ordinary consumer products over time?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"They depreciate faster","explanation":null},{"key":"1","text":"They become worthless after a few years","explanation":null},{"key":"2","text":"Well-maintained items can appreciate in value","explanation":null},{"key":"3","text":"They lose prestige when widely owned","explanation":null}],"explanations":null}'::jsonb,
 '2', 'Unlike most consumer goods, well-maintained luxury items like Hermès bags can appreciate over time, performing better than some investments.', 'c7c7c7c7-7777-7777-7777-777777777777'),
(uuid_generate_v4(), NOW(), NOW(), 'What is the core logic of luxury pricing that inverts normal market economics?', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'For luxury goods, lower prices destroy demand by undermining the scarcity and exclusivity that make the product valuable as a status signal.',
 'Normal goods follow the law of demand (lower price = more buyers), but luxury goods derive value from their scarcity—discounting removes what buyers are actually paying for.', 'c7c7c7c7-7777-7777-7777-777777777777');

-- Article 8 MEDIUM – all 3 quizzes
INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'Why did medieval craftsmen carve details high on cathedral ceilings that no one could see?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"To show off their technical skills to other craftsmen","explanation":null},{"key":"1","text":"Because architects demanded it in their contracts","explanation":null},{"key":"2","text":"Possibly because of religious devotion or an ethic of intrinsic craftsmanship","explanation":null},{"key":"3","text":"To test apprentices on difficult working conditions","explanation":null}],"explanations":null}'::jsonb,
 '2', 'The exact motivation is debated, but possibilities include religious belief that God could see unseen work, and a craft ethic where quality mattered regardless of audience.', 'c8c8c8c8-8888-8888-8888-888888888888'),
(uuid_generate_v4(), NOW(), NOW(), 'What modern assumption does this article challenge?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"That quality requires expensive materials","explanation":null},{"key":"1","text":"That quality requires an audience to matter","explanation":null},{"key":"2","text":"That medieval craftsmen were poorly paid","explanation":null},{"key":"3","text":"That religious art is more valuable than secular art","explanation":null}],"explanations":null}'::jsonb,
 '1', 'The article challenges the assumption that quality work needs an audience; it suggests some value is intrinsic regardless of whether anyone sees it.', 'c8c8c8c8-8888-8888-8888-888888888888'),
(uuid_generate_v4(), NOW(), NOW(), 'What does the cathedral example suggest about the relationship between excellence and recognition?', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'Excellence pursued privately, without the expectation of recognition, may represent the most authentic form of quality because it is not shaped by how others will perceive it.',
 'The craftsmen suggest that doing excellent work has intrinsic value independent of recognition—quality pursued for its own sake may be more honest than quality performed for an audience.', 'c8c8c8c8-8888-8888-8888-888888888888');


-- ============================================================
-- EASY LEVEL CONTENT for all 8 articles
-- ============================================================

-- EASY Article 1: The Age of Invisible Computers
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('ea000001-0000-0000-0000-000000000001', NOW(), NOW(), 'EASY',
'Computers used to be very big machines. Over time, they got smaller and smaller. Today, they {{disappear}} into the objects around us. Your home, your car, and your headphones all have computers inside them—but you rarely see them.

This is called ambient computing. The idea is to make technology work in the {{background}} of your life. You should not have to think about using it. It just helps you, quietly and automatically.

For example, a {{smart}} thermostat learns your daily schedule and adjusts the temperature for you. Headphones sense when you put them down and pause the music. These things happen without you pressing a button.

But there is a catch. These hidden devices are always {{connected}} to the internet. They collect information about your habits, your home, and your movements. This creates important questions about who owns that information and who can see it.

Making technology invisible is a powerful idea. But it also means we need to think carefully about how to keep our lives {{private}} in a world full of unseen sensors.', NULL, 'a1a1a1a1-1111-1111-1111-111111111111')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'disappear', 'To become impossible to see or find.', 'The small camera was designed to disappear into the wall.', 'ea000001-0000-0000-0000-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'background', 'Behind the main focus; not obviously noticeable.', 'The app runs in the background even when you are not using it.', 'ea000001-0000-0000-0000-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'smart', 'Able to connect to the internet and respond to data automatically.', 'Smart speakers can answer questions using voice commands.', 'ea000001-0000-0000-0000-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'connected', 'Linked to a network or the internet.', 'All connected devices can share information with each other.', 'ea000001-0000-0000-0000-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'private', 'Kept secret; not shared with others.', 'Your location data should remain private unless you choose to share it.', 'ea000001-0000-0000-0000-000000000001');

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What is the main goal of ambient computing?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"To make computers faster","explanation":null},{"key":"1","text":"To make technology work invisibly in the background","explanation":null},{"key":"2","text":"To replace smartphones","explanation":null},{"key":"3","text":"To connect all devices to a single screen","explanation":null}],"explanations":null}'::jsonb,
 '1', 'Ambient computing aims to make technology work invisibly, helping people without demanding their attention.', 'ea000001-0000-0000-0000-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'What is one concern about hidden devices collecting data?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"They use too much electricity","explanation":null},{"key":"1","text":"They are expensive to buy","explanation":null},{"key":"2","text":"Personal information may not stay private","explanation":null},{"key":"3","text":"They make rooms look cluttered","explanation":null}],"explanations":null}'::jsonb,
 '2', 'Hidden devices collect data about users, raising concerns about who can access that information.', 'ea000001-0000-0000-0000-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'In simple terms, what is one good thing and one bad thing about invisible computers?', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'Good: they make life easier by working automatically. Bad: they collect personal data that may not be kept private.',
 'The good side is that invisible computers simplify daily tasks without requiring effort. The bad side is that they gather personal data, which raises privacy concerns.', 'ea000001-0000-0000-0000-000000000001');

-- EASY Article 2: Why Your Brain Needs More Darkness
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('ea000002-0000-0000-0000-000000000002', NOW(), NOW(), 'EASY',
'Your body has an internal clock. It tells you when to feel awake and when to feel sleepy. This clock runs on a {{cycle}} of roughly 24 hours. It is set by natural signals—especially light and darkness.

Darkness used to be a clear signal that it was time to sleep. But today, artificial light is everywhere. City lights, phone screens, and indoor lighting mean that many people never experience true darkness.

When you are exposed to bright light at night, your brain slows down the release of a {{hormone}} called melatonin. Melatonin is what makes you feel sleepy. Less of it means you stay awake longer and sleep less well.

Screens are especially problematic. The blue light from phones and computers is very good at {{blocking}} melatonin. Using your phone before bed can push your sleep back by hours.

The good news is that the {{solution}} is simple in theory: get more darkness. Put your phone away an hour before bed. Use dimmer, warmer lights in the evening. Give your brain the signal it needs to prepare for {{rest}}.', NULL, 'a2a2a2a2-2222-2222-2222-222222222222')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'cycle', 'A series of events that happen repeatedly in the same order.', 'The sleep cycle repeats throughout the night.', 'ea000002-0000-0000-0000-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'hormone', 'A chemical made by the body that controls how organs and cells work.', 'Melatonin is a hormone that helps control your sleep schedule.', 'ea000002-0000-0000-0000-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'blocking', 'Stopping something from happening or getting through.', 'The curtains are blocking the light from entering the room.', 'ea000002-0000-0000-0000-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'solution', 'A way to solve a problem.', 'The solution to poor sleep might be as simple as dimming the lights.', 'ea000002-0000-0000-0000-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'rest', 'A period of relaxation or sleep that restores energy.', 'Your body needs proper rest to function well.', 'ea000002-0000-0000-0000-000000000002');

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What does melatonin do in your body?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"It gives you energy","explanation":null},{"key":"1","text":"It helps you feel sleepy and prepares you for sleep","explanation":null},{"key":"2","text":"It makes you hungry","explanation":null},{"key":"3","text":"It controls body temperature","explanation":null}],"explanations":null}'::jsonb,
 '1', 'Melatonin is the hormone that signals your body to prepare for sleep—less of it means worse sleep.', 'ea000002-0000-0000-0000-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'Why is blue light from screens especially bad before bedtime?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"It makes your eyes hurt","explanation":null},{"key":"1","text":"It is very bright","explanation":null},{"key":"2","text":"It is very effective at stopping the brain from releasing melatonin","explanation":null},{"key":"3","text":"It causes headaches","explanation":null}],"explanations":null}'::jsonb,
 '2', 'Blue light suppresses melatonin more effectively than other types of light, delaying sleep onset.', 'ea000002-0000-0000-0000-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'What simple change could someone make to improve their sleep based on this article?', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'Put away screens at least an hour before bed and use dim, warm lights in the evening to signal the brain that it is time to sleep.',
 'Reducing light exposure—especially from screens—in the hour before bed allows the brain to release melatonin and prepare naturally for sleep.', 'ea000002-0000-0000-0000-000000000002');

-- EASY Article 3: The Hidden Economics of Your Morning Coffee
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('ea000003-0000-0000-0000-000000000003', NOW(), NOW(), 'EASY',
'Coffee is one of the most popular drinks in the world. Most people buy a cup without thinking about where it came from. But there is a long and complicated journey between the coffee {{farm}} and your morning cup.

Coffee beans are grown in countries like Brazil, Ethiopia, and Colombia. After being picked, the beans are processed, shipped, and roasted before they ever reach a café. Each step involves different people, companies, and costs.

The price of coffee changes a lot from year to year. Weather has a big effect. A cold {{frost}} in Brazil can kill coffee plants and cause prices to rise sharply around the world. This creates a lot of uncertainty for everyone in the coffee business.

What surprises many people is how little the {{farmer}} earns. Out of a $5 cup of coffee, the person who grew the beans often receives less than five cents. The rest goes to the many steps between the farm and the café.

This means that your morning coffee is connected to a large global {{system}} of trade, weather, and finance. The next time you take a sip, you are participating in one of the most complex {{markets}} in the world.', NULL, 'a3a3a3a3-3333-3333-3333-333333333333')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'farm', 'A piece of land used for growing crops or raising animals.', 'The coffee farm in Colombia produces some of the finest beans.', 'ea000003-0000-0000-0000-000000000003'),
(uuid_generate_v4(), NOW(), NOW(), 'frost', 'Ice crystals that form on surfaces when temperatures drop below freezing.', 'An unexpected frost destroyed a large portion of the coffee crop.', 'ea000003-0000-0000-0000-000000000003'),
(uuid_generate_v4(), NOW(), NOW(), 'farmer', 'A person who grows crops or raises animals for food or sale.', 'Coffee farmers spend months growing and harvesting their beans.', 'ea000003-0000-0000-0000-000000000003'),
(uuid_generate_v4(), NOW(), NOW(), 'system', 'A group of connected parts that work together.', 'The global coffee system involves growers, traders, roasters, and retailers.', 'ea000003-0000-0000-0000-000000000003'),
(uuid_generate_v4(), NOW(), NOW(), 'markets', 'Places or systems where goods are bought and sold.', 'Coffee markets react quickly to news about weather in major growing regions.', 'ea000003-0000-0000-0000-000000000003');

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'How much of a $5 coffee does the farmer typically receive?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"About $2.50","explanation":null},{"key":"1","text":"About $1","explanation":null},{"key":"2","text":"Less than 5 cents","explanation":null},{"key":"3","text":"About 50 cents","explanation":null}],"explanations":null}'::jsonb,
 '2', 'Coffee farmers typically receive less than five cents per cup—most of the price goes to processing, shipping, roasting, and retail.', 'ea000003-0000-0000-0000-000000000003'),
(uuid_generate_v4(), NOW(), NOW(), 'Why do coffee prices change so quickly?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Because coffee shops change their menus","explanation":null},{"key":"1","text":"Because weather events like frost can destroy crops","explanation":null},{"key":"2","text":"Because people drink more coffee each year","explanation":null},{"key":"3","text":"Because the government sets new prices regularly","explanation":null}],"explanations":null}'::jsonb,
 '1', 'A frost or drought in a major coffee-growing country can quickly reduce supply and cause global prices to rise sharply.', 'ea000003-0000-0000-0000-000000000003'),
(uuid_generate_v4(), NOW(), NOW(), 'Why does the article say that buying a cup of coffee connects you to a "global system"?', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'Because the coffee in your cup came from farms in other countries and passed through many steps—processing, shipping, trading, roasting—involving people and markets across the entire world.',
 'Coffee travels through an international supply chain involving growers, traders, shippers, roasters, and retailers, making it a product of global trade and finance.', 'ea000003-0000-0000-0000-000000000003');

-- EASY Article 4: What Jazz Invented
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('ea000004-0000-0000-0000-000000000004', NOW(), NOW(), 'EASY',
'Jazz music started in New Orleans about 100 years ago. When people first heard it, many did not like it. They called it noise. But jazz quickly spread around the world and changed music forever.

What made jazz different was {{improvisation}}. In most music, musicians play from a written score—they know exactly what to play. Jazz musicians make up the music as they go. They listen to each other and respond in the moment. Each performance is unique.

Jazz also had a different {{rhythm}}. The beats fell in unexpected places, creating a feeling of energy and surprise. This is called syncopation. It made people want to move.

The ideas of jazz spread far beyond music. The jazz band became a model for how groups of people can work together well. Each musician has expertise, follows a shared structure, but also has the {{freedom}} to contribute their own ideas.

This model influenced how companies {{organize}} teams, how architects design buildings, and even how software developers write code. Jazz showed the world that {{creativity}} and structure can work together.', NULL, 'a4a4a4a4-4444-4444-4444-444444444444')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'improvisation', 'Creating something—music, speech, or art—in the moment without preparation.', 'The guitarist''s improvisation surprised even his bandmates.', 'ea000004-0000-0000-0000-000000000004'),
(uuid_generate_v4(), NOW(), NOW(), 'rhythm', 'A strong, regular, repeated pattern of sound or movement.', 'The drummer kept a steady rhythm throughout the song.', 'ea000004-0000-0000-0000-000000000004'),
(uuid_generate_v4(), NOW(), NOW(), 'freedom', 'The power to act, speak, or think without restriction.', 'The musicians had the freedom to add their own style to the piece.', 'ea000004-0000-0000-0000-000000000004'),
(uuid_generate_v4(), NOW(), NOW(), 'organize', 'To arrange things or people in a planned and effective way.', 'Companies often organize teams around the jazz model of structured freedom.', 'ea000004-0000-0000-0000-000000000004'),
(uuid_generate_v4(), NOW(), NOW(), 'creativity', 'The ability to make new things or think of new ideas.', 'Jazz is celebrated as a powerful expression of human creativity.', 'ea000004-0000-0000-0000-000000000004');

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What made jazz different from most music at the time?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"It used written scores more precisely than classical music","explanation":null},{"key":"1","text":"Musicians made up the music live as they played together","explanation":null},{"key":"2","text":"It only used string instruments","explanation":null},{"key":"3","text":"It was always played very quietly","explanation":null}],"explanations":null}'::jsonb,
 '1', 'Jazz introduced real-time improvisation, where musicians respond to each other and create music spontaneously.', 'ea000004-0000-0000-0000-000000000004'),
(uuid_generate_v4(), NOW(), NOW(), 'How did jazz influence areas outside of music?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"It replaced classical music in schools","explanation":null},{"key":"1","text":"Its model of structured teamwork influenced business and design","explanation":null},{"key":"2","text":"It had no influence outside music","explanation":null},{"key":"3","text":"It led to new laws about copyright","explanation":null}],"explanations":null}'::jsonb,
 '1', 'The jazz ensemble model—deep expertise + shared framework + individual freedom—was adopted in many other fields.', 'ea000004-0000-0000-0000-000000000004'),
(uuid_generate_v4(), NOW(), NOW(), 'In your own words, what is the most important lesson from jazz according to this article?', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'Structure and creativity can work together—having rules or a framework does not have to prevent people from expressing themselves freely.',
 'Jazz demonstrates that a shared structure (chords, form) does not limit creativity but instead provides a foundation that makes spontaneous collaboration possible.', 'ea000004-0000-0000-0000-000000000004');

-- EASY Article 5: The Attention Economy
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('ea000005-0000-0000-0000-000000000005', NOW(), NOW(), 'EASY',
'Do you ever pick up your phone without really deciding to? Or check an app just seconds after putting it down? Many people do. For years, the explanation was simple: the {{algorithm}} is designed to keep you hooked.

An algorithm is a set of rules a computer follows to decide what to show you. Social media apps use algorithms to show content that keeps you watching and scrolling for as long as possible.

But new research suggests the story is more complicated. Part of the reason we keep checking our phones is not just the algorithm—it is how human brains work. When you get a notification, your brain releases a small amount of a chemical called {{dopamine}}. This feels good, so you want more.

This means that even if apps changed their algorithms, some people would still feel {{addicted}} to checking their phones. The problem is partly inside us.

So what can we do? One idea is to create more {{distance}} between yourself and your device—turning off notifications, keeping your phone in another room, or setting aside phone-free time. These small changes can help you take back {{control}} of your attention.', NULL, 'a5a5a5a5-5555-5555-5555-555555555555')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'algorithm', 'A set of rules a computer follows to make decisions or solve problems.', 'The algorithm decides which videos appear on your home page.', 'ea000005-0000-0000-0000-000000000005'),
(uuid_generate_v4(), NOW(), NOW(), 'dopamine', 'A brain chemical linked to feelings of pleasure and reward.', 'Getting a "like" on a photo triggers a small release of dopamine.', 'ea000005-0000-0000-0000-000000000005'),
(uuid_generate_v4(), NOW(), NOW(), 'addicted', 'Unable to stop doing something even when it is causing harm.', 'Some researchers say people can become addicted to social media.', 'ea000005-0000-0000-0000-000000000005'),
(uuid_generate_v4(), NOW(), NOW(), 'distance', 'Space or separation between two things.', 'Creating distance from your phone can help you focus better.', 'ea000005-0000-0000-0000-000000000005'),
(uuid_generate_v4(), NOW(), NOW(), 'control', 'The ability to decide what happens; power over something.', 'Taking control of your screen time starts with small daily habits.', 'ea000005-0000-0000-0000-000000000005');

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'Why do researchers say algorithms alone cannot explain compulsive phone use?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Algorithms are not powerful enough","explanation":null},{"key":"1","text":"Human brains are also wired to seek rewards like notifications","explanation":null},{"key":"2","text":"Most people do not use social media","explanation":null},{"key":"3","text":"Phones are used less now than before","explanation":null}],"explanations":null}'::jsonb,
 '1', 'Human reward-seeking biology plays a role alongside algorithmic design in making phone use feel compulsive.', 'ea000005-0000-0000-0000-000000000005'),
(uuid_generate_v4(), NOW(), NOW(), 'What practical step does the article suggest to reduce compulsive phone use?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Delete all social media apps","explanation":null},{"key":"1","text":"Use your phone only at work","explanation":null},{"key":"2","text":"Turn off notifications and keep the phone out of reach","explanation":null},{"key":"3","text":"Replace your phone with an older model","explanation":null}],"explanations":null}'::jsonb,
 '2', 'Creating physical and digital distance from your phone—turning off notifications, keeping it in another room—helps restore deliberate control.', 'ea000005-0000-0000-0000-000000000005'),
(uuid_generate_v4(), NOW(), NOW(), 'Why is it not enough to just change the algorithm to fix the attention problem?', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'Because part of the problem comes from how human brains respond to rewards—even with better algorithms, people might still seek out the dopamine reward of checking notifications.',
 'The brain''s reward system creates a pull toward checking phones that exists independently of platform design, so changing only the algorithm addresses just part of the issue.', 'ea000005-0000-0000-0000-000000000005');

-- EASY Article 6: The Trees That Talk Underground
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('ea000006-0000-0000-0000-000000000006', NOW(), NOW(), 'EASY',
'Forests are more connected than they look. Under the ground, the roots of trees are linked by a network of {{fungi}}—tiny living things that look like threads. This underground network is sometimes called the "wood wide web."

Here is how it works. Trees make food from sunlight through a process called photosynthesis. Some of that food is shared with the fungi through the roots. In return, the fungi help the trees {{absorb}} more water and minerals from the soil. Both sides benefit—this is called a {{symbiotic}} relationship.

But scientists have found something even more interesting. Older trees, sometimes called "mother trees," seem to send extra food through the network to younger {{seedlings}} growing in their shade. Young trees that cannot yet make enough food of their own receive help from established trees.

Why do older trees do this? One idea is that they can {{recognize}} their own offspring and support them. Another idea is that the food just flows naturally to where it is needed. Scientists are still studying this question.

What is clear is that these underground networks matter. When logging or replanting breaks the network, it weakens the forest. Healthy forests depend on healthy underground connections.', NULL, 'a6a6a6a6-6666-6666-6666-666666666666')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'fungi', 'Living things (like mushrooms and mold) that feed on organic matter.', 'Fungi break down dead leaves and return nutrients to the soil.', 'ea000006-0000-0000-0000-000000000006'),
(uuid_generate_v4(), NOW(), NOW(), 'absorb', 'To take in or soak up a substance.', 'Tree roots absorb water and minerals from the surrounding soil.', 'ea000006-0000-0000-0000-000000000006'),
(uuid_generate_v4(), NOW(), NOW(), 'symbiotic', 'Describing a relationship where both organisms benefit.', 'The bee and the flower have a symbiotic relationship.', 'ea000006-0000-0000-0000-000000000006'),
(uuid_generate_v4(), NOW(), NOW(), 'seedlings', 'Young plants that have recently sprouted from seeds.', 'Seedlings need enough light and water to grow into healthy trees.', 'ea000006-0000-0000-0000-000000000006'),
(uuid_generate_v4(), NOW(), NOW(), 'recognize', 'To identify someone or something from previous knowledge.', 'Some scientists believe trees can recognize and prefer their own offspring.', 'ea000006-0000-0000-0000-000000000006');

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What do fungi give trees in their underground partnership?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Extra sunlight","explanation":null},{"key":"1","text":"Protection from insects","explanation":null},{"key":"2","text":"Help absorbing water and minerals from the soil","explanation":null},{"key":"3","text":"Carbon dioxide","explanation":null}],"explanations":null}'::jsonb,
 '2', 'Fungi dramatically increase a tree''s ability to absorb water and minerals from the soil, in exchange for sugars the tree produces.', 'ea000006-0000-0000-0000-000000000006'),
(uuid_generate_v4(), NOW(), NOW(), 'What happens to forests when logging breaks the underground fungal network?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Trees grow faster without competition","explanation":null},{"key":"1","text":"The forest becomes weaker and less able to survive stress","explanation":null},{"key":"2","text":"New fungi immediately replace the old ones","explanation":null},{"key":"3","text":"There is no significant effect","explanation":null}],"explanations":null}'::jsonb,
 '1', 'Severing mycorrhizal networks reduces forest resilience, making trees more vulnerable to drought and disease.', 'ea000006-0000-0000-0000-000000000006'),
(uuid_generate_v4(), NOW(), NOW(), 'Why do you think the underground fungal network matters for forest health?', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'The network allows trees to share nutrients and support each other, especially young seedlings that cannot yet support themselves, making the whole forest stronger.',
 'By connecting trees and enabling the sharing of nutrients and water, the fungal network supports weaker trees and helps the whole forest survive difficult conditions.', 'ea000006-0000-0000-0000-000000000006');

-- EASY Article 7: Why Luxury Brands Refuse to Go on Sale
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('ea000007-0000-0000-0000-000000000007', NOW(), NOW(), 'EASY',
'Have you ever noticed that famous luxury brands almost never have sales? You cannot find a designer handbag or watch at a discount in a regular store. This is not an accident—it is part of a very careful strategy.

Luxury products are not just useful items. They are also {{status}} symbols. When someone carries a very expensive bag, they are communicating something about themselves: their wealth, their taste, or their place in society. This signal only works if the bag is {{rare}} and hard to get.

If a luxury brand had a sale, more people could afford the products. But this would mean the products were no longer exclusive. The status symbol would stop working, and the brand would lose its special value.

Luxury brands also understand that their products can work as {{investments}}. A well-kept designer bag can actually be worth more after five years than when it was bought. This only happens because the brand carefully maintains its {{image}} of high demand and limited supply.

The lesson is that luxury economics works differently from normal economics. For ordinary goods, lower prices attract more buyers. For luxury goods, lower prices can actually {{destroy}} demand—because what people are really buying is exclusivity.', NULL, 'a7a7a7a7-7777-7777-7777-777777777777')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'status', 'A person''s social position or level of importance in a group.', 'Wearing a luxury watch can signal high social status.', 'ea000007-0000-0000-0000-000000000007'),
(uuid_generate_v4(), NOW(), NOW(), 'rare', 'Not found or happening often; uncommon and therefore valuable.', 'The limited-edition sneakers were rare enough to sell for ten times their price.', 'ea000007-0000-0000-0000-000000000007'),
(uuid_generate_v4(), NOW(), NOW(), 'investments', 'Things you buy hoping they will be worth more money in the future.', 'Some collectors treat rare handbags as long-term investments.', 'ea000007-0000-0000-0000-000000000007'),
(uuid_generate_v4(), NOW(), NOW(), 'image', 'The public impression or reputation of a person or company.', 'A sale could damage the brand''s image of exclusivity.', 'ea000007-0000-0000-0000-000000000007'),
(uuid_generate_v4(), NOW(), NOW(), 'destroy', 'To damage something so badly that it can no longer exist or function.', 'Discounting can destroy the perception of a luxury brand.', 'ea000007-0000-0000-0000-000000000007');

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'Why do luxury brands refuse to put their products on sale?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Because their products never have leftover stock","explanation":null},{"key":"1","text":"Because discounts would make the products less exclusive and damage their status value","explanation":null},{"key":"2","text":"Because sales are illegal in the luxury industry","explanation":null},{"key":"3","text":"Because their products are always sold out immediately","explanation":null}],"explanations":null}'::jsonb,
 '1', 'Luxury brands depend on scarcity and exclusivity for their value; discounting removes these qualities and undermines the brand.', 'ea000007-0000-0000-0000-000000000007'),
(uuid_generate_v4(), NOW(), NOW(), 'How is luxury economics different from normal economics?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Luxury goods get cheaper over time","explanation":null},{"key":"1","text":"Lower prices increase demand for luxury goods just like normal goods","explanation":null},{"key":"2","text":"Lower prices can actually reduce demand for luxury goods","explanation":null},{"key":"3","text":"Luxury companies do not care about profit","explanation":null}],"explanations":null}'::jsonb,
 '2', 'Unlike normal goods where lower prices attract buyers, lower prices for luxury goods can destroy the exclusivity that makes them desirable.', 'ea000007-0000-0000-0000-000000000007'),
(uuid_generate_v4(), NOW(), NOW(), 'In your own words, what are people really buying when they purchase a luxury item?', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'They are buying exclusivity and status—the signal that they are part of a small group who can afford something rare and prestigious.',
 'Beyond the physical product, buyers purchase the social signal it sends: membership in an exclusive group defined by wealth and taste.', 'ea000007-0000-0000-0000-000000000007');

-- EASY Article 8: The Art That Was Never Meant to Be Seen
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('ea000008-0000-0000-0000-000000000008', NOW(), NOW(), 'EASY',
'In medieval Europe, builders constructed enormous stone cathedrals that took generations to complete. High up in the ceiling, far above where any {{visitor}} could see clearly, craftsmen carved beautiful and detailed sculptures. These carvings were invisible to the people below.

No one required this level of care. The craftsmen who worked high on the ceilings could have done simple, rough work and nobody would have known. But instead, they carved with the same precision and detail as work that was clearly visible to worshippers.

Why did they do this? One reason might be religious. Many medieval craftsmen believed that God could see their {{hidden}} work even if no human could. To do poor work where God could see it would have been wrong.

Another reason may be simpler: they believed that good work was {{worth}} doing for its own sake. The quality of the work mattered, regardless of who saw it. This is a kind of personal {{pride}} in craftsmanship.

This idea challenges something we often assume today: that work only matters if someone sees and {{judges}} it. The cathedral carvers suggest that excellence pursued privately might be the most honest kind.', NULL, 'a8a8a8a8-8888-8888-8888-888888888888')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'visitor', 'A person who goes to a place for a short time.', 'No visitor to the cathedral could clearly see the carvings high above.', 'ea000008-0000-0000-0000-000000000008'),
(uuid_generate_v4(), NOW(), NOW(), 'hidden', 'Not easily seen or known; concealed.', 'The hidden carvings were only discovered using modern photographic equipment.', 'ea000008-0000-0000-0000-000000000008'),
(uuid_generate_v4(), NOW(), NOW(), 'worth', 'Having value; deserving effort or attention.', 'The craftsmen believed doing excellent work was worth the effort regardless of recognition.', 'ea000008-0000-0000-0000-000000000008'),
(uuid_generate_v4(), NOW(), NOW(), 'pride', 'A feeling of satisfaction from doing something well.', 'The sculptor''s pride in his work showed in every carefully carved detail.', 'ea000008-0000-0000-0000-000000000008'),
(uuid_generate_v4(), NOW(), NOW(), 'judges', 'Evaluates and forms an opinion about the quality of something.', 'We often work harder when someone judges our output.', 'ea000008-0000-0000-0000-000000000008');

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'Why did medieval craftsmen carve detailed work that no one could see?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"They were testing new carving techniques","explanation":null},{"key":"1","text":"Possibly because of religious belief or personal pride in craftsmanship","explanation":null},{"key":"2","text":"They were paid extra for the hidden work","explanation":null},{"key":"3","text":"The carvings were originally at eye level before the ceiling was raised","explanation":null}],"explanations":null}'::jsonb,
 '1', 'Motivations may have included religious devotion and a craft ethic that valued quality for its own sake, regardless of audience.', 'ea000008-0000-0000-0000-000000000008'),
(uuid_generate_v4(), NOW(), NOW(), 'What modern assumption does this story challenge?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"That old buildings are better than modern ones","explanation":null},{"key":"1","text":"That religious art is more valuable","explanation":null},{"key":"2","text":"That work only matters if someone can see and judge it","explanation":null},{"key":"3","text":"That craftsmen should be paid more","explanation":null}],"explanations":null}'::jsonb,
 '2', 'The unseen carvings suggest that quality can have intrinsic value—it does not need an audience to matter.', 'ea000008-0000-0000-0000-000000000008'),
(uuid_generate_v4(), NOW(), NOW(), 'Do you think it is important to do good work even when nobody is watching? Explain your view.', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'Yes, because quality work reflects personal integrity and a genuine commitment to doing things right, which has value independent of whether others notice it.',
 'Doing good work when unobserved reflects genuine values rather than performance for others—it demonstrates that the standard comes from within rather than from external judgment.', 'ea000008-0000-0000-0000-000000000008');


-- ============================================================
-- HARD LEVEL CONTENT for all 8 articles
-- ============================================================

-- HARD Article 1: The Age of Invisible Computers
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('ba000001-0000-0000-0000-000000000001', NOW(), NOW(), 'HARD',
'The progression of computational miniaturization has reached its logical terminus: {{pervasive}} computing that dissolves entirely into the environment. Where computing was once organized around discrete, visible artifacts—the workstation, the laptop, the handset—it is now distributed across built environments in forms that resist easy identification as "devices."

The dominant design philosophy of this paradigm is {{proactive}} automation: systems that anticipate user needs through continuous environmental sensing rather than awaiting explicit commands. The locus of agency shifts from user to system, with the user receiving outputs without initiating inputs. This inversion of the traditional human-computer interaction model carries profound implications for comprehensibility and accountability.

A central tension in ambient systems is the relationship between personalization and {{surveillance}}. The same continuous data stream that enables a thermostat to predict when you want your home warmer also constitutes a detailed behavioral log accessible to third parties. The commercial incentive structures of platform capitalism create powerful pressures to maximize data retention regardless of user utility, generating an {{asymmetry}} between institutional knowledge and individual awareness.

Regulatory frameworks have struggled to adapt. The legal construct of informed consent was engineered for discrete, bounded interactions—a signature on a document, a click on a checkbox. It maps poorly onto environments in which the computational {{substrate}} is architectural: present in the walls, the floors, and the objects of daily life.

The fundamental challenge of ambient computing governance is not technical but political: how to preserve meaningful user {{autonomy}} in systems whose value proposition depends on invisibility, and how to enforce accountability in systems whose complexity makes attribution difficult.', NULL, 'a1a1a1a1-1111-1111-1111-111111111111')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'pervasive', 'Present and spreading through every part of something; ubiquitous.', 'Pervasive computing integrates technology into every aspect of the built environment.', 'ba000001-0000-0000-0000-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'proactive', 'Acting in anticipation of future problems or needs rather than reacting to them.', 'Proactive systems learn user patterns and act before being asked.', 'ba000001-0000-0000-0000-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'surveillance', 'Close observation of a person or group, especially by authorities or corporations.', 'Ambient sensors enable a form of passive surveillance without explicit monitoring.', 'ba000001-0000-0000-0000-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'asymmetry', 'A lack of equality or equivalence between two things; imbalance.', 'The data asymmetry between corporations and users is a defining feature of digital capitalism.', 'ba000001-0000-0000-0000-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'autonomy', 'The right or condition of self-governance; independence of action.', 'Preserving user autonomy requires transparency about how ambient data is used.', 'ba000001-0000-0000-0000-000000000001');

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'Why does the article argue that the traditional concept of informed consent is inadequate for ambient computing?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Because users are generally unwilling to read consent forms","explanation":null},{"key":"1","text":"Because ambient systems collect data continuously and invisibly, not through discrete bounded interactions","explanation":null},{"key":"2","text":"Because ambient computing is too new for any legal framework","explanation":null},{"key":"3","text":"Because consent forms are not legally binding for hardware","explanation":null}],"explanations":null}'::jsonb,
 '1', 'Informed consent assumes discrete, visible interactions; ambient systems collect data continuously and architecturally, making traditional consent mechanisms structurally inadequate.', 'ba000001-0000-0000-0000-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'What does the article identify as the "fundamental challenge" of governing ambient computing?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Developing faster processors for embedded systems","explanation":null},{"key":"1","text":"Designing sufficiently user-friendly interfaces","explanation":null},{"key":"2","text":"Preserving user autonomy in systems whose value depends on invisibility","explanation":null},{"key":"3","text":"Standardizing communication protocols between devices","explanation":null}],"explanations":null}'::jsonb,
 '2', 'The core governance challenge is political: how to ensure accountability and autonomy when the system''s value proposition is invisibility and seamlessness.', 'ba000001-0000-0000-0000-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'Explain the tension between personalization and surveillance in ambient computing systems.', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'The same data that enables useful personalization also constitutes a detailed behavioral log. Commercial incentives push for maximum data retention, creating an asymmetry where institutions have comprehensive knowledge of users who remain unaware of what is collected.',
 'Personalization and surveillance use identical data streams; the difference lies in institutional intent and transparency. Commercial pressures favor data maximization over user awareness, structurally embedding surveillance into the personalization value chain.', 'ba000001-0000-0000-0000-000000000001');

-- HARD Article 2: Why Your Brain Needs More Darkness
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('ba000002-0000-0000-0000-000000000002', NOW(), NOW(), 'HARD',
'The suprachiasmatic nucleus—a cluster of approximately 20,000 neurons in the hypothalamus—functions as the master circadian pacemaker, synchronizing cellular clocks throughout the body with the external light-dark cycle. This {{entrainment}} mechanism evolved over hundreds of millions of years in response to the reliable periodicity of the solar cycle, and it exhibits exquisite sensitivity to short-wavelength light in the 460–490 nm range.

The {{photoreceptors}} responsible for this non-visual light detection are intrinsically photosensitive retinal ganglion cells (ipRGCs), discovered only in 2002. Unlike the rod and cone cells that mediate vision, ipRGCs project directly to the suprachiasmatic nucleus via the retinohypothalamic tract, providing the brain''s circadian system with direct access to ambient light information.

Artificial light at night operates as a chronobiological {{disruptor}} by delivering high-intensity, blue-rich light to ipRGCs during hours when the evolutionary system expects darkness. The downstream effects extend well beyond melatonin suppression: disrupted circadian timing has been associated with {{metabolic}} dysregulation, impaired immune function, increased cancer risk in shift workers, and bidirectional relationships with mood disorders including depression and bipolar disorder.

The epidemiological scale of this disruption is underappreciated. Satellite measurements indicate that artificial light at night increases globally at approximately 2% per year, and that over 80% of the world''s population now lives under light-polluted skies. This represents one of the most rapid and large-scale {{anthropogenic}} alterations to a fundamental environmental signal in the history of life on Earth.

The implications extend from individual sleep hygiene to urban planning, regulatory frameworks for outdoor lighting, and the design of display technologies. Addressing light pollution at scale requires coordination across architectural, regulatory, and technological domains that has not yet materialized.', NULL, 'a2a2a2a2-2222-2222-2222-222222222222')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'entrainment', 'The synchronization of biological rhythms to external cycles, such as light-dark cycles.', 'Jet lag occurs when the body''s entrainment to local time has not yet occurred.', 'ba000002-0000-0000-0000-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'photoreceptors', 'Light-sensitive cells in the retina that detect and respond to light.', 'The newly discovered photoreceptors responsible for circadian entrainment were identified only in 2002.', 'ba000002-0000-0000-0000-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'disruptor', 'An agent or factor that interferes with a normal process or system.', 'Artificial light acts as a chronobiological disruptor, confusing the brain''s time-keeping system.', 'ba000002-0000-0000-0000-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'metabolic', 'Relating to the chemical processes that occur in living organisms to sustain life.', 'Disrupted sleep is associated with a range of metabolic disorders including insulin resistance.', 'ba000002-0000-0000-0000-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'anthropogenic', 'Originating from human activity.', 'Light pollution is one of the most widespread anthropogenic changes to natural environments.', 'ba000002-0000-0000-0000-000000000002');

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'Why were ipRGCs significant when discovered in 2002?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"They improved our understanding of color vision","explanation":null},{"key":"1","text":"They revealed a direct retinal pathway to the brain''s circadian pacemaker, separate from visual processing","explanation":null},{"key":"2","text":"They explained why humans can see in low light","explanation":null},{"key":"3","text":"They were the first photoreceptors identified in any mammal","explanation":null}],"explanations":null}'::jsonb,
 '1', 'ipRGCs project directly to the suprachiasmatic nucleus, revealing a dedicated non-visual light detection pathway that mediates circadian entrainment independently of visual processing.', 'ba000002-0000-0000-0000-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'Why does the article describe artificial light at night as an "anthropogenic" alteration?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Because light at night was previously caused by natural events","explanation":null},{"key":"1","text":"Because it is caused entirely by human activity and represents an unprecedented change to a fundamental environmental signal","explanation":null},{"key":"2","text":"Because it only affects people living in urban areas","explanation":null},{"key":"3","text":"Because the technology involved is man-made","explanation":null}],"explanations":null}'::jsonb,
 '1', 'The term emphasizes that this is a human-caused disruption of an environmental signal that has been stable for hundreds of millions of years.', 'ba000002-0000-0000-0000-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'Explain why solving the light pollution problem requires coordination across multiple domains, not just individual behavior change.', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'Because the primary sources of light pollution are infrastructure—street lighting, building illumination, display advertising—that are governed by municipal planning, building codes, and corporate practices rather than individual choice.',
 'Individual sleep hygiene addresses personal exposure but not the structural sources: urban lighting infrastructure, commercial signage, and building illumination require policy and design changes that operate at architectural and regulatory scales beyond individual control.', 'ba000002-0000-0000-0000-000000000002');

-- HARD Article 3: The Hidden Economics of Your Morning Coffee
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('ba000003-0000-0000-0000-000000000003', NOW(), NOW(), 'HARD',
'Coffee occupies a structurally distinctive position in global commodity markets: it is simultaneously an agricultural product subject to the full range of supply-side shocks—disease, frost, drought—and a {{financialized}} asset class subject to speculative capital flows that can amplify those shocks well beyond their physical significance.

The mechanism of amplification operates through futures markets. When weather events threaten Brazilian production, the informational signal propagates through commodity futures exchanges faster than physical supply chains can respond. The resulting {{price discovery}} process incorporates not only the genuine supply shock but also anticipatory speculation from hedge funds and commodity trading advisors, who may take positions that exaggerate price movements in either direction.

The structural consequence for smallholder farmers is a form of {{risk}} asymmetry. Farmers bear the full volatility of the commodity cycle in their operating costs—land, labor, inputs—but capture only a small fraction of the value chain. This asymmetry is partially addressed by forward contracting, which allows farmers to lock in prices ahead of harvest. However, forward contracting transfers price risk without necessarily reducing it: it shifts uncertainty from the farmer to the counterparty, which typically has greater financial {{capacity}} to bear it.

The concentration of value in downstream processing and retail reflects the dynamics of {{oligopolistic}} market structure. A handful of multinational roasters and retailer chains exercise substantial bargaining power over commodity buyers, enabling them to capture margins that are structurally unavailable to upstream producers.

Fair trade certification schemes represent one attempt to redistribute value toward farmers by introducing a price floor. Their effectiveness remains contested: critics argue that certification premiums are often insufficient to offset transaction costs and may create perverse incentives that reduce overall market efficiency.', NULL, 'a3a3a3a3-3333-3333-3333-333333333333')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'financialized', 'Converted into a financial instrument or subjected to the logic of financial markets.', 'When commodities become financialized, their prices are influenced as much by speculation as by supply and demand.', 'ba000003-0000-0000-0000-000000000003'),
(uuid_generate_v4(), NOW(), NOW(), 'price discovery', 'The process by which market participants determine the fair price of an asset through trading.', 'Futures markets enable rapid price discovery when new information about crop yields becomes available.', 'ba000003-0000-0000-0000-000000000003'),
(uuid_generate_v4(), NOW(), NOW(), 'risk', 'The possibility of loss or harm; exposure to uncertain outcomes.', 'Smallholder farmers face concentrated risk from price volatility with limited ability to hedge.', 'ba000003-0000-0000-0000-000000000003'),
(uuid_generate_v4(), NOW(), NOW(), 'capacity', 'The ability or power to do, experience, or understand something.', 'Large financial institutions have the capacity to absorb commodity price risk that would be ruinous for individual farmers.', 'ba000003-0000-0000-0000-000000000003'),
(uuid_generate_v4(), NOW(), NOW(), 'oligopolistic', 'Relating to a market dominated by a small number of large firms.', 'The roasting and retail sectors are oligopolistic, giving a few companies outsized bargaining power.', 'ba000003-0000-0000-0000-000000000003');

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'How do futures markets amplify physical supply shocks in coffee?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"By slowing the flow of information between growing regions and consumers","explanation":null},{"key":"1","text":"By incorporating speculative capital flows alongside genuine supply data, potentially exaggerating price movements","explanation":null},{"key":"2","text":"By requiring physical delivery of goods at contract expiry","explanation":null},{"key":"3","text":"By setting fixed prices for the entire growing season","explanation":null}],"explanations":null}'::jsonb,
 '1', 'Speculative positions taken in anticipation of a supply shock can amplify the price signal beyond what the physical disruption alone would warrant.', 'ba000003-0000-0000-0000-000000000003'),
(uuid_generate_v4(), NOW(), NOW(), 'Why does forward contracting not fully resolve the risk asymmetry facing smallholder farmers?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Because farmers cannot access futures markets directly","explanation":null},{"key":"1","text":"Because forward contracts shift but do not eliminate risk, and farmers miss upside when prices rise","explanation":null},{"key":"2","text":"Because the contracts are always denominated in foreign currencies","explanation":null},{"key":"3","text":"Because coffee prices never rise enough to matter","explanation":null}],"explanations":null}'::jsonb,
 '1', 'Forward contracts transfer price risk to a better-capitalized counterparty but eliminate upside exposure for farmers; they address downside protection at the cost of potential gains.', 'ba000003-0000-0000-0000-000000000003'),
(uuid_generate_v4(), NOW(), NOW(), 'Analyze why oligopolistic market structure in roasting and retail makes redistribution of value toward coffee farmers difficult.', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'Because firms with concentrated market power can set terms of trade that favor their own margins; farmers operating in fragmented, competitive conditions lack the bargaining power to negotiate higher prices or better contract terms.',
 'Oligopolistic downstream firms exploit the bargaining asymmetry: many small farmers compete against a few large buyers, structurally preventing farmers from capturing the margins available to well-positioned intermediaries.', 'ba000003-0000-0000-0000-000000000003');

-- HARD Article 4: What Jazz Invented
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('ba000004-0000-0000-0000-000000000004', NOW(), NOW(), 'HARD',
'Jazz emerged from the confluence of African rhythmic traditions, European harmonic structures, and the specific social conditions of early twentieth-century New Orleans. Its central innovation—real-time collective {{improvisation}} within a shared structural framework—represented not merely a musical development but a new epistemology of creative practice.

The jazz ensemble instantiated a model of distributed {{cognition}} that organizational theorists would spend decades attempting to formalize: multiple agents with deep individual expertise, operating within a shared but flexible framework, co-producing emergent outputs that no single agent could have produced alone. The rhythm section establishes the harmonic and metric parameters; soloists navigate within and against those parameters; the ensemble negotiates transitions collectively, without a conductor mediating.

What made this model {{generative}} beyond music was its resolution of a longstanding tension in design and organizational theory: the apparent incompatibility between structure and creativity. Classical music demonstrated that structure could enable precision; jazz demonstrated that structure could enable spontaneity. The insight—that the right framework liberates rather than constrains—was subsequently applied in architectural theory, software engineering methodologies like agile development, and organizational design.

The jazz {{vernacular}} entered management literature through concepts like "jamming" as a metaphor for collaborative innovation. While these applications are often superficial, the underlying insight holds: {{legitimate}} peripheral participation in a community of practice, where novices learn by contributing alongside experts within a shared framework, mirrors the apprenticeship model of jazz education, where musicians learn by playing, not merely studying.

The question jazz raises for contemporary knowledge work is whether the conditions it required—small ensemble size, deep expertise, intrinsic motivation, shared aesthetic commitment—are reproducible at organizational scale, or whether the jazz metaphor describes a particular form of creative collaboration that does not generalize beyond its conditions of origin.', NULL, 'a4a4a4a4-4444-4444-4444-444444444444')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'improvisation', 'Creating and performing without prior preparation, in real time.', 'In jazz, improvisation is not random but informed by deep knowledge of harmony and idiom.', 'ba000004-0000-0000-0000-000000000004'),
(uuid_generate_v4(), NOW(), NOW(), 'cognition', 'The mental processes involved in acquiring knowledge; thinking.', 'Jazz represents a form of distributed cognition where the ensemble thinks collectively.', 'ba000004-0000-0000-0000-000000000004'),
(uuid_generate_v4(), NOW(), NOW(), 'generative', 'Capable of producing or creating something new and varied.', 'The jazz model was generative beyond music, influencing management theory and software design.', 'ba000004-0000-0000-0000-000000000004'),
(uuid_generate_v4(), NOW(), NOW(), 'vernacular', 'The language, style, or mode of expression used by a particular group or community.', 'Jazz vernacular entered business culture through metaphors of jamming and improvisation.', 'ba000004-0000-0000-0000-000000000004'),
(uuid_generate_v4(), NOW(), NOW(), 'legitimate', 'Conforming to established rules or principles; sanctioned by custom.', 'Legitimate participation in a jazz ensemble requires mastery of shared musical conventions.', 'ba000004-0000-0000-0000-000000000004');

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What does the article mean by calling jazz a "new epistemology of creative practice"?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Jazz introduced a new way of notating music","explanation":null},{"key":"1","text":"Jazz represented a fundamentally different way of understanding how creative knowledge is produced and shared in real time","explanation":null},{"key":"2","text":"Jazz created new academic institutions for musical training","explanation":null},{"key":"3","text":"Jazz replaced classical music as the dominant form","explanation":null}],"explanations":null}'::jsonb,
 '1', 'An "epistemology of creative practice" means a theory of how creative knowledge works; jazz showed that knowledge could be co-produced emergently in real time, not pre-determined.', 'ba000004-0000-0000-0000-000000000004'),
(uuid_generate_v4(), NOW(), NOW(), 'According to the article, what was the key insight of jazz that was adopted in other fields?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"That music can be written without notation","explanation":null},{"key":"1","text":"That the right framework liberates rather than constrains creativity","explanation":null},{"key":"2","text":"That small groups are always more creative than large ones","explanation":null},{"key":"3","text":"That improvisation requires no formal training","explanation":null}],"explanations":null}'::jsonb,
 '1', 'The jazz resolution of the structure-creativity tension—showing that structure can enable spontaneity—was the insight that transferred to agile development, organizational design, and architectural theory.', 'ba000004-0000-0000-0000-000000000004'),
(uuid_generate_v4(), NOW(), NOW(), 'Critically evaluate whether the jazz model of collaboration can be applied effectively in large organizations.', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'The jazz model requires small ensemble size, deep mutual expertise, intrinsic motivation, and shared aesthetic values—conditions that are difficult to replicate at scale. Large organizations face coordination costs, heterogeneous expertise, and extrinsic incentive structures that undermine the conditions jazz requires.',
 'The jazz analogy is compelling but may not generalize: the conditions it requires (small group, deep expertise, shared commitment) are precisely the conditions that break down at organizational scale, suggesting the metaphor may describe a narrow domain of collaborative excellence rather than a generalizable management principle.', 'ba000004-0000-0000-0000-000000000004');

-- HARD Article 5: The Attention Economy
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('ba000005-0000-0000-0000-000000000005', NOW(), NOW(), 'HARD',
'The dominant analytical framework for understanding digital platform design—the "attention economy" thesis—holds that platforms compete for user attention as a scarce resource and optimize their systems accordingly. This framework has generated important policy prescriptions: addictive design patterns should be regulated, recommendation systems should be made more transparent, friction should be introduced into engagement loops.

What the attention economy framework underweights is the {{endogenous}} component of compulsive digital use—the portion attributable to human psychological architecture rather than platform manipulation. Variable ratio reinforcement schedules, which underpin casino gambling and slot machine design, are effective not because they were invented by platform designers but because they exploit a reward-seeking heuristic that is evolutionarily ancient. Platforms discovered and exploited this heuristic; they did not create it.

The {{behavioral}} economic literature on present bias and hyperbolic discounting offers a more complete account. Users consistently prefer immediate, small rewards (a notification, a like) over larger but delayed returns (a coherent evening, a good night''s sleep). This preference reversal is {{systematic}} and relatively impervious to information—people who understand their present bias do not automatically overcome it.

This framing shifts the locus of intervention. If the problem is partly architectural—a misalignment between the reward structures of digital environments and long-term human flourishing—then regulatory solutions targeting platform design address a necessary but insufficient condition. {{Cognitive}} interventions that strengthen deliberative override of automatic reward responses may be required alongside structural changes.

The deeper political question is whether {{autonomy}}-preserving digital environments can be commercially viable. The current economic model of attention platforms depends on maximizing engagement, which is structurally in tension with user welfare. Regulatory mandates for "digital wellbeing" features may shift competitive dynamics without resolving the underlying tension between platform incentives and user interests.', NULL, 'a5a5a5a5-5555-5555-5555-555555555555')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'endogenous', 'Originating from within; arising from internal factors rather than external causes.', 'The endogenous components of addiction relate to brain chemistry, not just environmental triggers.', 'ba000005-0000-0000-0000-000000000005'),
(uuid_generate_v4(), NOW(), NOW(), 'behavioral', 'Relating to observable actions and responses, especially as studied in psychology.', 'Behavioral economics explains why people make choices that appear irrational from a classical perspective.', 'ba000005-0000-0000-0000-000000000005'),
(uuid_generate_v4(), NOW(), NOW(), 'systematic', 'Done or acting according to a plan; methodical; occurring consistently.', 'Present bias is a systematic cognitive pattern, not a random error in judgment.', 'ba000005-0000-0000-0000-000000000005'),
(uuid_generate_v4(), NOW(), NOW(), 'cognitive', 'Relating to mental processes such as perception, memory, and reasoning.', 'Cognitive interventions target the thinking patterns that contribute to compulsive behaviors.', 'ba000005-0000-0000-0000-000000000005'),
(uuid_generate_v4(), NOW(), NOW(), 'autonomy', 'Self-governance; the capacity to make informed, uncoerced decisions.', 'Digital autonomy means users can genuinely choose how they interact with platforms.', 'ba000005-0000-0000-0000-000000000005');

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What does the article identify as the limitation of the "attention economy" framework?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"It overestimates the role of algorithmic recommendation systems","explanation":null},{"key":"1","text":"It underweights the endogenous psychological component of compulsive use","explanation":null},{"key":"2","text":"It focuses too much on user behavior and not enough on platform design","explanation":null},{"key":"3","text":"It ignores the economic model of digital platforms","explanation":null}],"explanations":null}'::jsonb,
 '1', 'The attention economy thesis correctly identifies platform manipulation but underweights the degree to which compulsive use is driven by evolutionarily ancient reward-seeking heuristics.', 'ba000005-0000-0000-0000-000000000005'),
(uuid_generate_v4(), NOW(), NOW(), 'Why does understanding present bias complicate the case for purely regulatory solutions to digital addiction?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Because present bias is unique to young users and not a general phenomenon","explanation":null},{"key":"1","text":"Because regulations cannot be enforced effectively on global platforms","explanation":null},{"key":"2","text":"Because even well-informed users systematically prefer immediate rewards, suggesting regulation alone cannot overcome psychological architecture","explanation":null},{"key":"3","text":"Because present bias disappears when users understand it","explanation":null}],"explanations":null}'::jsonb,
 '2', 'Present bias is systematic and relatively impervious to information; knowing about it does not reliably produce behavior change, suggesting structural interventions address necessary but not sufficient conditions.', 'ba000005-0000-0000-0000-000000000005'),
(uuid_generate_v4(), NOW(), NOW(), 'Analyze the fundamental tension between commercial viability and user wellbeing in attention platforms.', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'Attention platforms derive revenue from engagement time, which creates a structural incentive to maximize usage. User wellbeing often requires reduced engagement, creating a direct conflict between the platform''s revenue model and users'' long-term interests.',
 'The business model of attention platforms monetizes time-on-platform through advertising, creating a commercial incentive that is structurally misaligned with user welfare: platforms profit from the same engagement patterns that research links to diminished wellbeing.', 'ba000005-0000-0000-0000-000000000005');

-- HARD Article 6: The Trees That Talk Underground
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('ba000006-0000-0000-0000-000000000006', NOW(), NOW(), 'HARD',
'The characterization of forest ecosystems as collections of competing individuals has been fundamentally revised by research into mycorrhizal networks. Ectomycorrhizal and arbuscular mycorrhizal fungi form {{obligate}} symbioses with the root systems of the vast majority of terrestrial plant species, creating persistent underground networks that mediate resource transfer across substantial distances.

The physiological basis of the symbiosis is well established: plants provide photosynthetically derived carbon to fungal partners, which in return facilitate water and mineral nutrient uptake by dramatically expanding the effective surface area of root systems. What has proved more {{contentious}} is the question of whether these networks function as pathways for directed, ecologically meaningful resource transfer between trees—particularly the hypothesis that established "source" trees subsidize young "sink" trees with insufficient photosynthetic capacity.

Evidence for source-sink carbon transfer comes primarily from isotopic tracer studies, in which labeled carbon administered to mature trees has been detected in neighboring seedlings via the fungal network. However, the {{interpretation}} of these results remains contested. The passive hydraulic gradient hypothesis holds that carbon flows according to concentration gradients without requiring any mechanism of kin recognition or directed allocation. The kin selection hypothesis proposes that trees preferentially support genetically related neighbors via a mechanism yet to be fully characterized.

The ecological implications of mycorrhizal network integrity are less contested than the mechanism. Silvicultural practices that disrupt fungal networks—intensive monoculture forestry, aggressive site preparation—consistently produce forests with reduced resilience to abiotic stress and {{pathogen}} pressure. The network functions as a form of ecological {{infrastructure}}: invisible, distributed, and difficult to replace once degraded.

Management strategies that protect mycorrhizal continuity—retention forestry, mixed-species planting, minimization of soil disturbance—may prove essential for maintaining forest function under the increasing stress of climate-driven shifts in precipitation and temperature regimes.', NULL, 'a6a6a6a6-6666-6666-6666-666666666666')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'obligate', 'Required by nature; unable to survive without a specific condition or partner.', 'Most forest trees form obligate symbioses with mycorrhizal fungi and cannot thrive without them.', 'ba000006-0000-0000-0000-000000000006'),
(uuid_generate_v4(), NOW(), NOW(), 'contentious', 'Causing disagreement or argument; disputed.', 'The question of whether trees can recognize kin remains contentious among ecologists.', 'ba000006-0000-0000-0000-000000000006'),
(uuid_generate_v4(), NOW(), NOW(), 'interpretation', 'The way in which something is understood or explained.', 'The interpretation of isotopic tracer data is central to the debate about tree communication.', 'ba000006-0000-0000-0000-000000000006'),
(uuid_generate_v4(), NOW(), NOW(), 'pathogen', 'A microorganism that causes disease in its host.', 'Mycorrhizal networks may help trees resist soil-borne pathogens by activating immune responses.', 'ba000006-0000-0000-0000-000000000006'),
(uuid_generate_v4(), NOW(), NOW(), 'infrastructure', 'The underlying systems and structures that support a larger system or society.', 'Mycorrhizal networks function as invisible ecological infrastructure for forest health.', 'ba000006-0000-0000-0000-000000000006');

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What is the key point of contention in mycorrhizal network research?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Whether mycorrhizal fungi can photosynthesize","explanation":null},{"key":"1","text":"Whether carbon transfer between trees is directed and meaningful or simply passive hydraulic flow","explanation":null},{"key":"2","text":"Whether the fungal networks are present in temperate forests","explanation":null},{"key":"3","text":"Whether mycorrhizal symbiosis benefits the fungus at all","explanation":null}],"explanations":null}'::jsonb,
 '1', 'The central debate is whether resource transfer between trees reflects directed allocation (including possible kin recognition) or passive physical processes driven by concentration gradients.', 'ba000006-0000-0000-0000-000000000006'),
(uuid_generate_v4(), NOW(), NOW(), 'Why does the article describe mycorrhizal networks as "infrastructure"?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Because they are built by humans to manage forests","explanation":null},{"key":"1","text":"Because they are large enough to be mapped with standard surveying equipment","explanation":null},{"key":"2","text":"Because they are invisible, distributed systems that support forest function and are difficult to replace once degraded","explanation":null},{"key":"3","text":"Because they conduct electricity like power cables","explanation":null}],"explanations":null}'::jsonb,
 '2', 'Like built infrastructure, mycorrhizal networks are foundational, largely invisible, distributed across large areas, and their degradation has cascading consequences that are difficult to reverse.', 'ba000006-0000-0000-0000-000000000006'),
(uuid_generate_v4(), NOW(), NOW(), 'Explain why the distinction between the passive hydraulic gradient hypothesis and the kin selection hypothesis matters for forest management.', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'If transfer is passive (hydraulic gradient), protecting network integrity matters but species composition is less critical. If kin selection operates, then management decisions about genetic diversity and mixed-species planting become ecologically significant, as they affect whether trees can support their offspring.',
 'The mechanism determines what management practices are most important: passive transfer means maintaining network connectivity is the priority; kin selection means genetic and species diversity of plantings is also critical to support inter-tree resource allocation.', 'ba000006-0000-0000-0000-000000000006');

-- HARD Article 7: Why Luxury Brands Refuse to Go on Sale
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('ba000007-0000-0000-0000-000000000007', NOW(), NOW(), 'HARD',
'Luxury goods exhibit what economists classify as {{Veblen}} goods behavior: demand increases rather than decreasing as price rises, at least within relevant ranges, because the good''s price is itself a component of its utility. Consumers derive value not merely from the physical attributes of the product but from the social signal its cost communicates—membership in an exclusive group defined by the ability to afford the unaffordable.

This inverts the standard demand curve and creates the structural paradox of luxury pricing strategy: lowering price to capture additional market share undermines the signal that generated demand in the first place. The rational response is to maintain or raise prices even during downturns, destroy excess inventory rather than discount it, and manage supply tightly enough that secondary market prices remain elevated—all of which function to reinforce the perception of {{scarcity}} even when the underlying supply is more elastic than pricing implies.

The investment thesis for top-tier luxury goods rests on a more complex foundation than simple scarcity. The {{appreciation}} of certain handbag and watch categories outperforming equities reflects a combination of genuinely constrained production—Hermès, for instance, limits annual bag production and maintains waiting lists—and a global expansion of high-net-worth individuals with the capacity and motivation to acquire hard assets with status properties. These buyers are not simply paying for an object; they are paying for membership in a global status market with favorable {{liquidity}} characteristics.

The strategic discipline required to maintain this model is extreme. A single significant markdown—an outlet sale, an online discount, a warehouse clearance—constitutes a credible signal of demand weakness that can permanently impair brand equity. Burberry''s policy of destroying unsold inventory represents not waste but {{rational}} asset protection: the cost of destruction is smaller than the cost of damaged brand equity.

The deeper economic logic is that luxury brands sell a {{perception}} as much as a product. The perception—of limitless demand, of exclusive access, of uncompromising quality—is the primary asset, and the physical product is merely the carrier of that perception. Protecting the perception requires protecting the price, even at the cost of the product.', NULL, 'a7a7a7a7-7777-7777-7777-777777777777')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'Veblen', 'Relating to goods whose demand increases as price rises because high price signals status.', 'Designer handbags exhibit Veblen good characteristics: higher prices increase their desirability.', 'ba000007-0000-0000-0000-000000000007'),
(uuid_generate_v4(), NOW(), NOW(), 'scarcity', 'The state of being in short supply; limited availability relative to demand.', 'Luxury brands engineer scarcity by limiting production and maintaining waiting lists.', 'ba000007-0000-0000-0000-000000000007'),
(uuid_generate_v4(), NOW(), NOW(), 'appreciation', 'An increase in value over time.', 'The appreciation of certain watches and handbags has outpaced returns on many financial assets.', 'ba000007-0000-0000-0000-000000000007'),
(uuid_generate_v4(), NOW(), NOW(), 'liquidity', 'The ease with which an asset can be converted to cash without losing value.', 'Top-tier luxury items have become attractive partly because of their favorable liquidity compared to other hard assets.', 'ba000007-0000-0000-0000-000000000007'),
(uuid_generate_v4(), NOW(), NOW(), 'rational', 'Based on logic and reason; consistent with a clear objective.', 'Destroying inventory is a rational strategy when the cost of discounting exceeds the cost of destruction.', 'ba000007-0000-0000-0000-000000000007');

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What defines a Veblen good, according to this article?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"A good made exclusively for government procurement","explanation":null},{"key":"1","text":"A good whose demand increases as price rises because price itself is part of the product''s utility","explanation":null},{"key":"2","text":"A good that depreciates in value as soon as it is purchased","explanation":null},{"key":"3","text":"A good produced by a single monopoly supplier","explanation":null}],"explanations":null}'::jsonb,
 '1', 'Veblen goods invert the normal demand curve because the high price is part of what buyers are purchasing—the social signal of being able to afford something exclusive.', 'ba000007-0000-0000-0000-000000000007'),
(uuid_generate_v4(), NOW(), NOW(), 'Why does the article describe Burberry''s inventory destruction as "rational asset protection" rather than waste?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Because the destroyed goods were defective and could not be sold","explanation":null},{"key":"1","text":"Because destroying goods generates tax benefits greater than their sale value","explanation":null},{"key":"2","text":"Because the cost of brand equity damage from discounting exceeds the monetary value of the destroyed inventory","explanation":null},{"key":"3","text":"Because it generates positive publicity from environmental campaigns","explanation":null}],"explanations":null}'::jsonb,
 '2', 'The calculation is: discounted sale price minus brand equity damage from signaling weakness exceeds the destruction cost, making destruction the profit-maximizing choice.', 'ba000007-0000-0000-0000-000000000007'),
(uuid_generate_v4(), NOW(), NOW(), 'Explain how the investment thesis for top-tier luxury goods differs from the Veblen good thesis for ordinary luxury consumption.', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'Veblen good consumption is driven by signaling—buyers pay a premium for status communication. Investment-grade luxury adds a financial logic: constrained supply, growing demand from expanding global wealth, and favorable secondary market liquidity create genuine asset appreciation that is independent of (though reinforced by) the status signal.',
 'Ordinary Veblen consumption is primarily status-driven; investment-grade luxury adds genuine scarcity (production limits, waiting lists), a global pool of high-net-worth buyers, and documented secondary market appreciation—converting the status premium into a verifiable financial return.', 'ba000007-0000-0000-0000-000000000007');

-- HARD Article 8: The Art That Was Never Meant to Be Seen
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('ba000008-0000-0000-0000-000000000008', NOW(), NOW(), 'HARD',
'The medieval practice of executing fine carving work at heights and in locations {{inaccessible}} to ordinary observation raises a question that cuts across art history, philosophy of action, and organizational theory: under what conditions does quality work require an audience?

The phenomenon is well documented. Gargoyles, bosses, and corbels in Romanesque and Gothic cathedrals were frequently executed with the same technical {{precision}} as work at eye level, despite being invisible without telescopic equipment. The craftsmen responsible—journeymen and master masons working within guild structures—were secular wage laborers as often as they were religious devotees. The religious explanation—that God witnessed the unseen work—provides a motivation that is historically plausible but difficult to verify.

A more {{tractable}} interpretation draws on craft theory: the notion that quality is constitutive of craftsmanship rather than contingent on external evaluation. In this framework, the carver who reduces quality when unobserved is not simply cutting corners—they are failing to be a craftsman. The standard is internal to the practice, maintained by practitioners as an expression of professional identity rather than by external monitoring or incentive structures.

This has {{implications}} for contemporary debates about work quality in the context of remote and distributed organizations, algorithmic monitoring, and performance management. The question of whether quality work requires observation or incentives is partly a question about what kind of workers are doing it—those whose standards are external (maintained by surveillance and reward) versus those whose standards are {{intrinsic}} (maintained by professional identity and craft ethics).

The cathedral evidence suggests that intrinsic motivation can sustain quality even in the complete absence of external verification. It also suggests that the contemporary tendency to design work systems around external monitoring may crowd out the intrinsic motivations on which high-quality work ultimately depends.', NULL, 'a8a8a8a8-8888-8888-8888-888888888888')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'inaccessible', 'Unable to be reached or entered; not accessible.', 'The finest carvings were placed in inaccessible locations high in the cathedral vaulting.', 'ba000008-0000-0000-0000-000000000008'),
(uuid_generate_v4(), NOW(), NOW(), 'precision', 'The quality of being exact and accurate; exactness.', 'The precision of the stonework was maintained regardless of whether it would ever be seen.', 'ba000008-0000-0000-0000-000000000008'),
(uuid_generate_v4(), NOW(), NOW(), 'tractable', 'Easy to deal with or manage; able to be resolved or handled.', 'A tractable interpretation of the phenomenon focuses on craft identity rather than religious motivation.', 'ba000008-0000-0000-0000-000000000008'),
(uuid_generate_v4(), NOW(), NOW(), 'implications', 'The possible effects or results of an action or decision.', 'The implications for management theory are significant: intrinsic motivation may be more reliable than external monitoring.', 'ba000008-0000-0000-0000-000000000008'),
(uuid_generate_v4(), NOW(), NOW(), 'intrinsic', 'Belonging naturally to something; essential and inherent.', 'Intrinsic motivation comes from within and is more durable than external rewards or punishments.', 'ba000008-0000-0000-0000-000000000008');

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'How does craft theory explain the quality of unseen medieval carvings differently from the religious explanation?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"Craft theory holds that masters were paid more for hidden work","explanation":null},{"key":"1","text":"Craft theory locates the quality standard in professional identity, not divine observation—reducing quality would mean failing to be a craftsman","explanation":null},{"key":"2","text":"Craft theory argues the work was never actually unseen during construction","explanation":null},{"key":"3","text":"Craft theory denies that the carvings were done with unusual care","explanation":null}],"explanations":null}'::jsonb,
 '1', 'Craft theory holds that standards are internal to the practice: a craftsman who reduces quality when unobserved is failing their own identity, independent of whether God or anyone else is watching.', 'ba000008-0000-0000-0000-000000000008'),
(uuid_generate_v4(), NOW(), NOW(), 'What does the article suggest about the relationship between external monitoring and intrinsic motivation in work systems?', 'MULTIPLE_CHOICE',
 '{"choices":[{"key":"0","text":"External monitoring always improves work quality","explanation":null},{"key":"1","text":"Intrinsic motivation and external monitoring are equivalent in their effects","explanation":null},{"key":"2","text":"Designing work systems around external monitoring may crowd out the intrinsic motivations that sustain quality","explanation":null},{"key":"3","text":"Workers with external standards always outperform those with internal standards","explanation":null}],"explanations":null}'::jsonb,
 '2', 'The cathedral evidence suggests that intrinsic motivation can sustain quality without observation; over-relying on monitoring may undermine the very motivations that high-quality work depends on.', 'ba000008-0000-0000-0000-000000000008'),
(uuid_generate_v4(), NOW(), NOW(), 'Apply the distinction between intrinsic and extrinsic quality standards to the design of a remote work policy.', 'SHORT_ANSWER',
 '{"choices":null,"explanations":null}'::jsonb,
 'A policy relying on surveillance and output metrics assumes external quality standards; it may undermine workers who would maintain quality intrinsically. Policies that select for craft identity, invest in professional development, and build shared standards may produce better results with less monitoring overhead.',
 'Remote work policy design faces a version of the cathedral question: intrinsically motivated workers need latitude and trust; extrinsically motivated workers need structure and monitoring. The cathedral evidence suggests over-monitoring may crowd out intrinsic motivation, but the appropriate balance depends on the composition and culture of the workforce.', 'ba000008-0000-0000-0000-000000000008');
