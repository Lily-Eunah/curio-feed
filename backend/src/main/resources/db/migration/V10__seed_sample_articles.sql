-- V10: Seed sample articles with relative dates for Today, Yesterday, and Older
-- Article 9: Today (Tech)
INSERT INTO articles (id, created_at, updated_at, original_title, source_name, source_url, original_published_at, title, slug, category_id, published_at, status, original_content) VALUES
('a9a9a9a9-9999-9999-9999-999999999999', NOW(), NOW(), 'The Future of Quantum Computing', 'Quantum Daily', 'https://example.com/art-009', NOW(), 'The Future of Quantum Computing', 'quantum-future', 'd1c1c1c1-1111-1111-1111-111111111111', NOW(), 'PUBLISHED', 'Quantum computing is no longer just theoretical. Here is what to expect in the next decade.')
ON CONFLICT (id) DO NOTHING;

-- Article 10: Yesterday (Culture)
INSERT INTO articles (id, created_at, updated_at, original_title, source_name, source_url, original_published_at, title, slug, category_id, published_at, status, original_content) VALUES
('aa000010-1010-1010-1010-101010101010', NOW(), NOW(), 'The Psychology of Minimalism', 'Mindful Living', 'https://example.com/art-010', NOW() - INTERVAL '1 day', 'The Psychology of Minimalism', 'psychology-minimalism', 'd1c1c1c1-4444-4444-4444-444444444444', NOW() - INTERVAL '1 day', 'PUBLISHED', 'Why owning less can lead to more happiness according to recent psychological studies.')
ON CONFLICT (id) DO NOTHING;

-- Article 11: Older (Science)
INSERT INTO articles (id, created_at, updated_at, original_title, source_name, source_url, original_published_at, title, slug, category_id, published_at, status, original_content) VALUES
('ab000011-1111-1111-1111-111111111111', NOW(), NOW(), 'The Science of Better Sleep', 'Health Journal', 'https://example.com/art-011', NOW() - INTERVAL '7 days', 'The Science of Better Sleep', 'science-better-sleep', 'd1c1c1c1-2222-2222-2222-222222222222', NOW() - INTERVAL '7 days', 'PUBLISHED', 'How understanding your biological clock can lead to deeper, more restorative rest.')
ON CONFLICT (id) DO NOTHING;

-- Add MEDIUM content for Article 9
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('c9c9c9c9-9999-9999-9999-999999999999', NOW(), NOW(), 'MEDIUM', 'Quantum computing uses {{qubits}} instead of bits. While bits are either 0 or 1, qubits can exist in multiple states at once. This allows quantum computers to solve {{complex}} problems that would take traditional computers millions of years.\n\nWe are currently in the "noisy intermediate-scale quantum" (NISQ) era. These machines have errors but are already showing {{potential}} in areas like chemistry and cryptography.\n\nOne of the biggest challenges is {{decoherence}}—the tendency of qubits to lose their quantum state due to environmental noise. To prevent this, quantum computers must be kept at temperatures colder than outer space.\n\nIn the coming decade, we expect to see {{breakthroughs}} in error correction, which will lead to more stable and powerful machines. This could revolutionize how we design new materials and medicines.', NULL, 'a9a9a9a9-9999-9999-9999-999999999999')
ON CONFLICT (id) DO NOTHING;

-- Add Vocab for Article 9
INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'qubits', 'The basic unit of quantum information.', 'Qubits can represent many values simultaneously.', 'c9c9c9c9-9999-9999-9999-999999999999'),
(uuid_generate_v4(), NOW(), NOW(), 'complex', 'Consisting of many different and connected parts.', 'The algorithm solves complex mathematical equations.', 'c9c9c9c9-9999-9999-9999-999999999999'),
(uuid_generate_v4(), NOW(), NOW(), 'potential', 'Having or showing the capacity to become or develop into something in the future.', 'Quantum computers have the potential to break modern encryption.', 'c9c9c9c9-9999-9999-9999-999999999999'),
(uuid_generate_v4(), NOW(), NOW(), 'decoherence', 'The loss of quantum coherence, typically due to interaction with the environment.', 'Decoherence remains a major hurdle for quantum hardware.', 'c9c9c9c9-9999-9999-9999-999999999999'),
(uuid_generate_v4(), NOW(), NOW(), 'breakthroughs', 'Significant steps forward in knowledge or development.', 'Scientists are waiting for breakthroughs in qubit stability.', 'c9c9c9c9-9999-9999-9999-999999999999')
ON CONFLICT (id) DO NOTHING;

-- Add Quiz for Article 9
INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What is the main difference between bits and qubits?', 'MULTIPLE_CHOICE', '{"choices":[{"key":"0","text":"Qubits are smaller than bits","explanation":null},{"key":"1","text":"Qubits can exist in multiple states at once, while bits are 0 or 1","explanation":null},{"key":"2","text":"Bits are faster than qubits","explanation":null},{"key":"3","text":"There is no difference","explanation":null}],"explanations":null}'::jsonb, '1', 'Qubits utilize quantum superposition to exist in multiple states simultaneously, enabling parallel computation.', 'c9c9c9c9-9999-9999-9999-999999999999')
ON CONFLICT (id) DO NOTHING;

-- Add MEDIUM content for Article 10
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('ca101010-1010-1010-1010-101010101010', NOW(), NOW(), 'MEDIUM', 'Minimalism is often misunderstood as just having few possessions. However, it is a {{deliberate}} practice of focusing on what adds value to your life. Recent studies show that reducing physical {{clutter}} can significantly lower cortisol levels, the hormone associated with stress.\n\nThe "endowment effect" is a psychological bias where we value things more simply because we own them. Minimalism helps us {{overcome}} this bias by encouraging us to evaluate objects based on their utility and joy rather than attachment.\n\nBy choosing to own less, we free up mental energy for {{meaningful}} experiences and relationships. This shift from consumption to intention can lead to greater long-term {{wellbeing}}.\n\nIt is not about living in an empty room, but about making sure every object you own has a purpose or a place.', NULL, 'aa000010-1010-1010-1010-101010101010')
ON CONFLICT (id) DO NOTHING;

-- Add Vocab for Article 10
INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'deliberate', 'Done consciously and intentionally.', 'She made a deliberate choice to live with fewer things.', 'ca101010-1010-1010-1010-101010101010'),
(uuid_generate_v4(), NOW(), NOW(), 'clutter', 'A collection of things lying about in an untidy mass.', 'Removing clutter from the desk improved his focus.', 'ca101010-1010-1010-1010-101010101010'),
(uuid_generate_v4(), NOW(), NOW(), 'overcome', 'Succeed in dealing with a problem or difficulty.', 'Minimalists try to overcome the urge to buy more stuff.', 'ca101010-1010-1010-1010-101010101010'),
(uuid_generate_v4(), NOW(), NOW(), 'meaningful', 'Having a serious, relevant, or useful purpose or value.', 'He spent more time on meaningful hobbies after selling his TV.', 'ca101010-1010-1010-1010-101010101010'),
(uuid_generate_v4(), NOW(), NOW(), 'wellbeing', 'The state of being comfortable, healthy, or happy.', 'Minimalism can contribute to a sense of mental wellbeing.', 'ca101010-1010-1010-1010-101010101010')
ON CONFLICT (id) DO NOTHING;

-- Add Quiz for Article 10
INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What is the "endowment effect"?', 'MULTIPLE_CHOICE', '{"choices":[{"key":"0","text":"A preference for expensive items","explanation":null},{"key":"1","text":"Valuing things more simply because you own them","explanation":null},{"key":"2","text":"The urge to buy new things","explanation":null},{"key":"3","text":"A feeling of sadness when losing something","explanation":null}],"explanations":null}'::jsonb, '1', 'The endowment effect describes the psychological tendency to assign more value to an object just because of ownership.', 'ca101010-1010-1010-1010-101010101010')
ON CONFLICT (id) DO NOTHING;

-- Add MEDIUM content for Article 11
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('cb000011-1111-1111-1111-111111111111', NOW(), NOW(), 'MEDIUM', 'Sleep is not a passive state but an active period of {{restoration}}. Our brains use this time to clear out metabolic waste and consolidate memories. Central to this process is the {{circadian}} rhythm, an internal clock that coordinates our biological processes with the 24-hour light-dark cycle.\n\nModern environments often {{disrupt}} this rhythm with artificial light and irregular schedules. This leads to "social jetlag," where our internal clock is out of sync with our daily requirements. Long-term disruption is linked to various {{ailments}}, from weight gain to cognitive decline.\n\nTo improve sleep quality, experts recommend maintaining a {{consistent}} schedule and reducing blue light exposure in the evening. By respecting our biological needs, we can unlock the full benefits of rest.', NULL, 'ab000011-1111-1111-1111-111111111111')
ON CONFLICT (id) DO NOTHING;

-- Add Vocab for Article 11
INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'restoration', 'The action of returning something to a former owner, place, or condition.', 'Sleep provides essential physical and mental restoration.', 'cb000011-1111-1111-1111-111111111111'),
(uuid_generate_v4(), NOW(), NOW(), 'circadian', 'Relating to biological processes that repeat approximately every 24 hours.', 'Disruption of the circadian rhythm can cause fatigue.', 'cb000011-1111-1111-1111-111111111111'),
(uuid_generate_v4(), NOW(), NOW(), 'disrupt', 'Interrupt by causing a disturbance or problem.', 'Loud noises can disrupt your sleep cycle.', 'cb000011-1111-1111-1111-111111111111'),
(uuid_generate_v4(), NOW(), NOW(), 'ailments', 'A physical disorder or illness, especially a minor one.', 'Lack of sleep is connected to many common ailments.', 'cb000011-1111-1111-1111-111111111111'),
(uuid_generate_v4(), NOW(), NOW(), 'consistent', 'Acting or done in the same way over time.', 'A consistent bedtime routine helps children fall asleep faster.', 'cb000011-1111-1111-1111-111111111111')
ON CONFLICT (id) DO NOTHING;

-- Add Quiz for Article 11
INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What is "social jetlag"?', 'MULTIPLE_CHOICE', '{"choices":[{"key":"0","text":"Traveling across multiple time zones for work","explanation":null},{"key":"1","text":"Being out of sync with your internal clock due to social schedules","explanation":null},{"key":"2","text":"A feeling of exhaustion after a social event","explanation":null},{"key":"3","text":"The inability to sleep in a new place","explanation":null}],"explanations":null}'::jsonb, '1', 'Social jetlag occurs when the requirements of our social and work lives clash with our biological sleep needs.', 'cb000011-1111-1111-1111-111111111111')
ON CONFLICT (id) DO NOTHING;
