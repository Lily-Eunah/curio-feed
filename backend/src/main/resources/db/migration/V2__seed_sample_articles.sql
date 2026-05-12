-- V2: Seed initial categories and sample articles with EASY/MEDIUM/HARD content.
-- All IDs are deterministic for idempotency.

-- Seed Categories
INSERT INTO categories (id, created_at, updated_at, name, display_name, active, sort_order) VALUES
('d1c1c1c1-1111-1111-1111-111111111111', NOW(), NOW(), 'Tech', 'Tech', true, 1),
('d1c1c1c1-2222-2222-2222-222222222222', NOW(), NOW(), 'Science', 'Science', true, 2),
('d1c1c1c1-3333-3333-3333-333333333333', NOW(), NOW(), 'Business', 'Business', true, 3),
('d1c1c1c1-4444-4444-4444-444444444444', NOW(), NOW(), 'Culture', 'Culture', true, 4)
ON CONFLICT (id) DO NOTHING;

-- Seed Articles
INSERT INTO articles (id, created_at, updated_at, original_title, source_name, source_url, original_published_at, title, slug, category_id, published_at, status, original_content) VALUES
('a9a9a9a9-9999-9999-9999-999999999999', NOW(), NOW(), 'The Future of Quantum Computing', 'Quantum Daily', 'https://example.com/art-009', NOW(), 'The Future of Quantum Computing', 'quantum-future', 'd1c1c1c1-1111-1111-1111-111111111111', NOW(), 'PUBLISHED', 'Quantum computing is no longer just theoretical. Here is what to expect in the next decade.'),
('aa000010-1010-1010-1010-101010101010', NOW(), NOW(), 'The Psychology of Minimalism', 'Mindful Living', 'https://example.com/art-010', NOW() - INTERVAL '1 day', 'The Psychology of Minimalism', 'psychology-minimalism', 'd1c1c1c1-4444-4444-4444-444444444444', NOW() - INTERVAL '1 day', 'PUBLISHED', 'Why owning less can lead to more happiness according to recent psychological studies.'),
('ab000011-1111-1111-1111-111111111111', NOW(), NOW(), 'The Science of Better Sleep', 'Health Journal', 'https://example.com/art-011', NOW() - INTERVAL '7 days', 'The Science of Better Sleep', 'science-better-sleep', 'd1c1c1c1-2222-2222-2222-222222222222', NOW() - INTERVAL '7 days', 'PUBLISHED', 'How understanding your biological clock can lead to deeper, more restorative rest.')
ON CONFLICT (id) DO NOTHING;

---------------------------------------------------------------------------------------
-- Article 9: Quantum Computing
---------------------------------------------------------------------------------------

-- EASY
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('d9e90009-0009-0009-0009-000000000001', NOW(), NOW(), 'EASY',
E'Quantum computers are a new kind of computer. Normal computers use {{bits}}, which are either 0 or 1. Quantum computers use {{qubits}}, which can be both 0 and 1 at the same time.\n\nThis makes quantum computers very {{powerful}}. They can solve certain problems much faster than any normal computer. Scientists hope they will help us find new medicines and create better materials.\n\nThe hardest part is preventing {{errors}}. Qubits are fragile and break easily when disturbed by heat or noise. This is why quantum computers must be kept colder than outer space.\n\nScientists are working to make quantum computers more {{reliable}}. One day, they may solve problems that are impossible for computers today.',
NULL, 'a9a9a9a9-9999-9999-9999-999999999999') ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'bits', 'The basic unit of information in a classical computer, either 0 or 1.', 'A computer stores all data as sequences of bits.', 'd9e90009-0009-0009-0009-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'qubits', 'The basic unit of quantum information, which can be 0, 1, or both at the same time.', 'Unlike bits, qubits can represent many values simultaneously.', 'd9e90009-0009-0009-0009-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'powerful', 'Able to do a great deal or produce strong effects.', 'The new processor was so powerful it ran the simulation instantly.', 'd9e90009-0009-0009-0009-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'errors', 'Mistakes or incorrect results caused by a fault in a system.', 'Engineers reduced errors in the chip by lowering the temperature.', 'd9e90009-0009-0009-0009-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'reliable', 'Consistently working correctly and not likely to fail.', 'A reliable machine produces the same result every time you test it.', 'd9e90009-0009-0009-0009-000000000001') ON CONFLICT (id) DO NOTHING;

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What is special about qubits compared to regular bits?', 'MULTIPLE_CHOICE', '{"choices":[{"key":"0","text":"They are made of gold","explanation":null},{"key":"1","text":"They can be both 0 and 1 at the same time","explanation":null},{"key":"2","text":"They store pictures instead of numbers","explanation":null},{"key":"3","text":"They work at room temperature","explanation":null}],"explanations":null}'::jsonb, '1', 'Qubits exploit quantum superposition to represent 0 and 1 simultaneously, enabling parallel computation.', 'd9e90009-0009-0009-0009-000000000001') ON CONFLICT (id) DO NOTHING;

-- MEDIUM
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('c9c9c9c9-9999-9999-9999-999999999999', NOW(), NOW(), 'MEDIUM',
E'Quantum computing uses {{qubits}} instead of bits. While bits are either 0 or 1, qubits can exist in multiple states at once. This allows quantum computers to solve {{complex}} problems that would take traditional computers millions of years.\n\nWe are currently in the "noisy intermediate-scale quantum" (NISQ) era. These machines have errors but are already showing {{potential}} in areas like chemistry and cryptography.\n\nOne of the biggest challenges is {{decoherence}}—the tendency of qubits to lose their quantum state due to environmental noise. To prevent this, quantum computers must be kept at temperatures colder than outer space.\n\nIn the coming decade, we expect to see {{breakthroughs}} in error correction, which will lead to more stable and powerful machines. This could revolutionize how we design new materials and medicines.',
NULL, 'a9a9a9a9-9999-9999-9999-999999999999') ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'qubits', 'The basic unit of quantum information.', 'Qubits can represent many values simultaneously.', 'c9c9c9c9-9999-9999-9999-999999999999'),
(uuid_generate_v4(), NOW(), NOW(), 'complex', 'Consisting of many different and connected parts.', 'The algorithm solves complex mathematical equations.', 'c9c9c9c9-9999-9999-9999-999999999999'),
(uuid_generate_v4(), NOW(), NOW(), 'potential', 'Having or showing the capacity to become or develop into something in the future.', 'Quantum computers have the potential to break modern encryption.', 'c9c9c9c9-9999-9999-9999-999999999999'),
(uuid_generate_v4(), NOW(), NOW(), 'decoherence', 'The loss of quantum coherence, typically due to interaction with the environment.', 'Decoherence remains a major hurdle for quantum hardware.', 'c9c9c9c9-9999-9999-9999-999999999999'),
(uuid_generate_v4(), NOW(), NOW(), 'breakthroughs', 'Significant steps forward in knowledge or development.', 'Scientists are waiting for breakthroughs in qubit stability.', 'c9c9c9c9-9999-9999-9999-999999999999') ON CONFLICT (id) DO NOTHING;

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What is the main difference between bits and qubits?', 'MULTIPLE_CHOICE', '{"choices":[{"key":"0","text":"Qubits are smaller than bits","explanation":null},{"key":"1","text":"Qubits can exist in multiple states at once, while bits are 0 or 1","explanation":null},{"key":"2","text":"Bits are faster than qubits","explanation":null},{"key":"3","text":"There is no difference","explanation":null}],"explanations":null}'::jsonb, '1', 'Qubits utilize quantum superposition to exist in multiple states simultaneously, enabling parallel computation.', 'c9c9c9c9-9999-9999-9999-999999999999') ON CONFLICT (id) DO NOTHING;

-- HARD
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('d9f90009-0009-0009-0009-000000000002', NOW(), NOW(), 'HARD',
E'Quantum computing represents a fundamental {{paradigm}} shift in information processing. By exploiting quantum mechanical properties—specifically {{superposition}} and {{entanglement}}—quantum processors can evaluate exponentially many computational paths simultaneously, yielding speedups for specific problem classes that are intractable on classical hardware.\n\nThe central engineering obstacle is {{decoherence}}, the loss of quantum coherence through unwanted environmental interaction. Contemporary noisy intermediate-scale quantum (NISQ) devices operate with gate fidelities insufficient for deep circuits, necessitating error mitigation strategies. Fault-tolerant computation requires surface codes imposing substantial physical qubit overhead per logical qubit.\n\nThe implications for {{cryptography}} are profound. Shor''s algorithm demonstrates polynomial-time integer factorization on a fault-tolerant quantum processor, threatening RSA and elliptic-curve schemes. NIST has consequently standardized post-quantum cryptographic algorithms based on lattice and hash problems.\n\nNear-term quantum advantage is anticipated from variational algorithms applied to molecular simulation and combinatorial optimization. These hybrid quantum-classical approaches leverage quantum hardware as coprocessors within classical pipelines, bridging the gap before full fault tolerance is achieved.',
NULL, 'a9a9a9a9-9999-9999-9999-999999999999') ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'paradigm', 'A fundamental model or framework that defines how a field approaches problems.', 'The discovery of DNA represented a paradigm shift in biology.', 'd9f90009-0009-0009-0009-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'superposition', 'A quantum state in which a system simultaneously exists in multiple states until measured.', 'Superposition allows a qubit to encode more information than a classical bit.', 'd9f90009-0009-0009-0009-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'entanglement', 'A quantum phenomenon where two particles become correlated so that measuring one instantly affects the other.', 'Entanglement enables faster information transfer between qubits in a processor.', 'd9f90009-0009-0009-0009-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'decoherence', 'The degradation of quantum states due to interaction with the surrounding environment.', 'Decoherence is why quantum computers must operate near absolute zero.', 'd9f90009-0009-0009-0009-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'cryptography', 'The practice and study of securing information using mathematical algorithms.', 'Modern cryptography underpins the security of online banking.', 'd9f90009-0009-0009-0009-000000000002') ON CONFLICT (id) DO NOTHING;

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'Why does decoherence impose a fundamental limit on current quantum computation?', 'MULTIPLE_CHOICE', '{"choices":[{"key":"0","text":"It prevents qubits from being manufactured at scale","explanation":null},{"key":"1","text":"It causes quantum states to collapse prematurely, limiting circuit depth","explanation":null},{"key":"2","text":"It increases the speed of computation beyond useful limits","explanation":null},{"key":"3","text":"It makes post-quantum cryptography impossible","explanation":null}],"explanations":null}'::jsonb, '1', 'Decoherence collapses quantum states through environmental noise, restricting the number of gate operations before fidelity degrades below acceptable thresholds.', 'd9f90009-0009-0009-0009-000000000002') ON CONFLICT (id) DO NOTHING;

---------------------------------------------------------------------------------------
-- Article 10: Minimalism
---------------------------------------------------------------------------------------

-- EASY
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('d10e0010-0010-0010-0010-000000000001', NOW(), NOW(), 'EASY',
E'Minimalism means choosing to own fewer things. Many people find that having too many {{possessions}} makes them feel overwhelmed and unhappy. When you own less, it is easier to find what you need and keep your home tidy.\n\nStudies show that a messy or crowded space can cause {{stress}}. When we clear away things we do not need, we often feel calmer and more focused. This is because our brains do not have to work as hard to process a busy environment.\n\nMinimalism also helps us spend money on things that really {{matter}}. Instead of buying lots of cheap items, people who practice minimalism save up for experiences or things they truly love.\n\nYou do not have to own almost nothing to be a minimalist. The idea is simply to make {{intentional}} choices about what you keep. Start small by clearing one drawer or shelf, and notice how it makes you feel.',
NULL, 'aa000010-1010-1010-1010-101010101010') ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'possessions', 'Things that you own.', 'She donated many of her old possessions to charity.', 'd10e0010-0010-0010-0010-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'stress', 'A feeling of worry or pressure caused by difficult situations.', 'Too much clutter in the home can lead to feelings of stress.', 'd10e0010-0010-0010-0010-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'matter', 'To be important or have value.', 'Focus on the things that truly matter to you.', 'd10e0010-0010-0010-0010-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'intentional', 'Done on purpose and with careful thought.', 'She made intentional choices about what to keep in her home.', 'd10e0010-0010-0010-0010-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'overwhelmed', 'Feeling like there is too much to deal with at once.', 'He felt overwhelmed by the piles of unopened mail.', 'd10e0010-0010-0010-0010-000000000001') ON CONFLICT (id) DO NOTHING;

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'According to the article, why can owning fewer things make us feel calmer?', 'MULTIPLE_CHOICE', '{"choices":[{"key":"0","text":"Because it saves us money","explanation":null},{"key":"1","text":"Because our brains do not have to work as hard in a less cluttered space","explanation":null},{"key":"2","text":"Because minimalism is a popular trend","explanation":null},{"key":"3","text":"Because fewer things means fewer decisions about colour","explanation":null}],"explanations":null}'::jsonb, '1', 'A less cluttered environment reduces the cognitive load on the brain, resulting in lower stress and better focus.', 'd10e0010-0010-0010-0010-000000000001') ON CONFLICT (id) DO NOTHING;

-- MEDIUM
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('ca101010-1010-1010-1010-101010101010', NOW(), NOW(), 'MEDIUM',
E'Minimalism is often misunderstood as just having few possessions. However, it is a {{deliberate}} practice of focusing on what adds value to your life. Recent studies show that reducing physical {{clutter}} can significantly lower cortisol levels, the hormone associated with stress.\n\nThe "endowment effect" is a psychological bias where we value things more simply because we own them. Minimalism helps us {{overcome}} this bias by encouraging us to evaluate objects based on their utility and joy rather than attachment.\n\nBy choosing to own less, we free up mental energy for {{meaningful}} experiences and relationships. This shift from consumption to intention can lead to greater long-term {{wellbeing}}.\n\nIt is not about living in an empty room, but about making sure every object you own has a purpose or a place.',
NULL, 'aa000010-1010-1010-1010-101010101010') ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'deliberate', 'Done consciously and intentionally.', 'She made a deliberate choice to live with fewer things.', 'ca101010-1010-1010-1010-101010101010'),
(uuid_generate_v4(), NOW(), NOW(), 'clutter', 'A collection of things lying about in an untidy mass.', 'Removing clutter from the desk improved his focus.', 'ca101010-1010-1010-1010-101010101010'),
(uuid_generate_v4(), NOW(), NOW(), 'overcome', 'Succeed in dealing with a problem or difficulty.', 'Minimalists try to overcome the urge to buy more stuff.', 'ca101010-1010-1010-1010-101010101010'),
(uuid_generate_v4(), NOW(), NOW(), 'meaningful', 'Having a serious, relevant, or useful purpose or value.', 'He spent more time on meaningful hobbies after selling his TV.', 'ca101010-1010-1010-1010-101010101010'),
(uuid_generate_v4(), NOW(), NOW(), 'wellbeing', 'The state of being comfortable, healthy, or happy.', 'Minimalism can contribute to a sense of mental wellbeing.', 'ca101010-1010-1010-1010-101010101010') ON CONFLICT (id) DO NOTHING;

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What is the "endowment effect"?', 'MULTIPLE_CHOICE', '{"choices":[{"key":"0","text":"A preference for expensive items","explanation":null},{"key":"1","text":"Valuing things more simply because you own them","explanation":null},{"key":"2","text":"The urge to buy new things","explanation":null},{"key":"3","text":"A feeling of sadness when losing something","explanation":null}],"explanations":null}'::jsonb, '1', 'The endowment effect describes the psychological tendency to assign more value to an object just because of ownership.', 'ca101010-1010-1010-1010-101010101010') ON CONFLICT (id) DO NOTHING;

-- HARD
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('d10f0010-0010-0010-0010-000000000002', NOW(), NOW(), 'HARD',
E'Minimalism, far from a mere aesthetic preference, engages deep psychological mechanisms governing human well-being. The practice of {{intentional}} curation of one''s environment directly counters the {{hedonic}} treadmill—the tendency for elevated material acquisition to yield only transient satisfaction before baseline happiness is restored. Research in environmental psychology demonstrates that physical clutter elevates {{cortisol}} levels, impairing executive function and amplifying anxiety.\n\nThe "endowment effect," a well-documented {{cognitive}} bias, compels individuals to overvalue owned objects independent of their utility. Minimalism functions as a systematic override of this bias, training practitioners to evaluate possessions on the basis of genuine contribution to life quality rather than mere ownership. This process has been linked to improvements in emotional regulation and decision-making efficiency.\n\nFrom a self-determination theory perspective, minimalism enhances perceived {{autonomy}} by eliminating the subtle psychological burden imposed by superfluous possessions. Each unused object represents an implicit, unmade decision, consuming attentional resources. Removing such items restores agency and reduces what researchers term "decision fatigue."\n\nLongitudinal studies indicate that practitioners of intentional minimalism report higher life satisfaction scores, attributable not to deprivation but to the reallocation of financial and attentional resources toward experiences and relationships—domains consistently shown to yield more durable well-being than material acquisition.',
NULL, 'aa000010-1010-1010-1010-101010101010') ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'intentional', 'Deliberate and purposefully chosen rather than accidental.', 'Intentional design choices made the workspace more productive.', 'd10f0010-0010-0010-0010-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'hedonic', 'Relating to pleasure as the primary source of good or motivation.', 'Hedonic adaptation explains why lottery winners are not permanently happier.', 'd10f0010-0010-0010-0010-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'cortisol', 'A steroid hormone released in response to stress that affects metabolism and immune function.', 'Chronic elevated cortisol is associated with weight gain and cognitive decline.', 'd10f0010-0010-0010-0010-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'cognitive', 'Relating to mental processes such as thinking, learning, and perception.', 'Sleep deprivation causes measurable cognitive impairment.', 'd10f0010-0010-0010-0010-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'autonomy', 'The capacity to make one''s own independent decisions and govern oneself.', 'Greater workplace autonomy is linked to higher employee satisfaction.', 'd10f0010-0010-0010-0010-000000000002') ON CONFLICT (id) DO NOTHING;

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'How does minimalism engage the "endowment effect" as described in the article?', 'MULTIPLE_CHOICE', '{"choices":[{"key":"0","text":"It encourages buying higher-quality possessions","explanation":null},{"key":"1","text":"It trains practitioners to override the bias of overvaluing owned objects","explanation":null},{"key":"2","text":"It eliminates all ownership of physical goods","explanation":null},{"key":"3","text":"It reduces the number of financial decisions one must make","explanation":null}],"explanations":null}'::jsonb, '1', 'The endowment effect causes us to overvalue things simply because we own them; minimalism counters this by requiring evaluation of objects based on actual utility and contribution to well-being.', 'd10f0010-0010-0010-0010-000000000002') ON CONFLICT (id) DO NOTHING;

---------------------------------------------------------------------------------------
-- Article 11: Science of Sleep
---------------------------------------------------------------------------------------

-- EASY
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('d11e0011-0011-0011-0011-000000000001', NOW(), NOW(), 'EASY',
E'Sleep is one of the most important things your body needs. When you sleep, your {{brain}} clears away waste products that build up during the day. It also saves new memories and repairs your body.\n\nYour body follows a daily {{rhythm}} that tells you when to feel sleepy and when to wake up. This internal clock works best when you go to bed and wake up at the same time every day. Bright {{light}} in the evening can confuse this clock and make it harder to fall asleep.\n\nMany people do not get enough sleep because of phones, late-night work, or irregular schedules. This leads to feeling {{tired}} and having trouble thinking clearly. Over time, poor sleep can affect your health in serious ways.\n\nThe good news is that small changes can help a lot. Turning off screens an hour before bed and keeping a regular {{schedule}} are two of the most effective things you can do to sleep better.',
NULL, 'ab000011-1111-1111-1111-111111111111') ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'brain', 'The organ inside your head that controls your body and stores your thoughts and memories.', 'Your brain continues to process information even while you sleep.', 'd11e0011-0011-0011-0011-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'rhythm', 'A regular, repeated pattern of events or actions.', 'Her daily rhythm of waking early helped her feel more energized.', 'd11e0011-0011-0011-0011-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'light', 'Electromagnetic radiation that allows us to see; bright light signals daytime to the brain.', 'Exposure to bright light in the morning helps reset your sleep rhythm.', 'd11e0011-0011-0011-0011-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'tired', 'Feeling a need for rest or sleep due to exertion or lack of sleep.', 'She felt tired all day because she had stayed up too late the night before.', 'd11e0011-0011-0011-0011-000000000001'),
(uuid_generate_v4(), NOW(), NOW(), 'schedule', 'A plan that shows when activities are supposed to happen.', 'Keeping a consistent sleep schedule improved his energy levels.', 'd11e0011-0011-0011-0011-000000000001') ON CONFLICT (id) DO NOTHING;

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'Why does bright light in the evening make it harder to fall asleep?', 'MULTIPLE_CHOICE', '{"choices":[{"key":"0","text":"It heats up the room","explanation":null},{"key":"1","text":"It confuses your body''s internal clock, which expects darkness at night","explanation":null},{"key":"2","text":"It makes your eyes hurt","explanation":null},{"key":"3","text":"It increases your appetite","explanation":null}],"explanations":null}'::jsonb, '1', 'The body''s internal clock uses light cues to regulate sleep timing; bright light at night signals that it is still daytime, suppressing the release of sleep hormones.', 'd11e0011-0011-0011-0011-000000000001') ON CONFLICT (id) DO NOTHING;

-- MEDIUM
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('cb000011-1111-1111-1111-111111111111', NOW(), NOW(), 'MEDIUM',
E'Sleep is not a passive state but an active period of {{restoration}}. Our brains use this time to clear out metabolic waste and consolidate memories. Central to this process is the {{circadian}} rhythm, an internal clock that coordinates our biological processes with the 24-hour light-dark cycle.\n\nModern environments often {{disrupt}} this rhythm with artificial light and irregular schedules. This leads to "social jetlag," where our internal clock is out of sync with our daily requirements. Long-term disruption is linked to various {{ailments}}, from weight gain to cognitive decline.\n\nTo improve sleep quality, experts recommend maintaining a {{consistent}} schedule and reducing blue light exposure in the evening. By respecting our biological needs, we can unlock the full benefits of rest.',
NULL, 'ab000011-1111-1111-1111-111111111111') ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'restoration', 'The action of returning something to a former owner, place, or condition.', 'Sleep provides essential physical and mental restoration.', 'cb000011-1111-1111-1111-111111111111'),
(uuid_generate_v4(), NOW(), NOW(), 'circadian', 'Relating to biological processes that repeat approximately every 24 hours.', 'Disruption of the circadian rhythm can cause fatigue.', 'cb000011-1111-1111-1111-111111111111'),
(uuid_generate_v4(), NOW(), NOW(), 'disrupt', 'Interrupt by causing a disturbance or problem.', 'Loud noises can disrupt your sleep cycle.', 'cb000011-1111-1111-1111-111111111111'),
(uuid_generate_v4(), NOW(), NOW(), 'ailments', 'A physical disorder or illness, especially a minor one.', 'Lack of sleep is connected to many common ailments.', 'cb000011-1111-1111-1111-111111111111'),
(uuid_generate_v4(), NOW(), NOW(), 'consistent', 'Acting or done in the same way over time.', 'A consistent bedtime routine helps children fall asleep faster.', 'cb000011-1111-1111-1111-111111111111') ON CONFLICT (id) DO NOTHING;

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'What is "social jetlag"?', 'MULTIPLE_CHOICE', '{"choices":[{"key":"0","text":"Traveling across multiple time zones for work","explanation":null},{"key":"1","text":"Being out of sync with your internal clock due to social schedules","explanation":null},{"key":"2","text":"A feeling of exhaustion after a social event","explanation":null},{"key":"3","text":"The inability to sleep in a new place","explanation":null}],"explanations":null}'::jsonb, '1', 'Social jetlag occurs when the requirements of our social and work lives clash with our biological sleep needs.', 'cb000011-1111-1111-1111-111111111111') ON CONFLICT (id) DO NOTHING;

-- HARD
INSERT INTO article_contents (id, created_at, updated_at, level, content, audio_url, article_id) VALUES
('d11f0011-0011-0011-0011-000000000002', NOW(), NOW(), 'HARD',
E'Sleep is orchestrated by two interacting biological processes: the {{circadian}} clock and sleep homeostasis. The circadian system, governed by the suprachiasmatic nucleus (SCN), coordinates physiological rhythms with the 24-hour light-dark cycle primarily through the light-sensitive suppression of {{melatonin}} secretion from the pineal gland. Simultaneously, sleep homeostatic pressure accumulates via the progressive buildup of {{adenosine}}, a sleep-promoting neuromodulator, during wakefulness.\n\nModern environments systematically undermine both processes. Artificial light—particularly the short-wavelength blue spectrum emitted by LED screens—suppresses melatonin secretion at intensities far below those encountered in natural settings. Social obligations impose irregular sleep schedules that induce "social jetlag," a chronic misalignment between endogenous circadian phase and behavioral sleep timing. This {{homeostasis}} disruption elevates allostatic load and accelerates biological aging markers.\n\nThe neurological consequences of chronic sleep restriction are well-documented. Inadequate sleep impairs prefrontal cortical function, degrading executive control, emotional regulation, and working memory. Longitudinal data associate habitual short sleep duration with elevated risk of metabolic syndrome, cardiovascular disease, and neurodegenerative pathology. The glymphatic system, which clears neurotoxic proteins including amyloid-beta during slow-wave sleep, is particularly vulnerable to sleep curtailment.\n\nEvidence-based interventions include maintaining {{consistent}} sleep-wake timing, restricting blue-light exposure in the two hours preceding sleep, and optimizing thermal environment. Cognitive behavioral therapy for insomnia (CBT-I) outperforms pharmacological intervention for chronic sleep disturbance, addressing the hyperarousal and dysfunctional sleep cognitions that perpetuate the disorder.',
NULL, 'ab000011-1111-1111-1111-111111111111') ON CONFLICT (id) DO NOTHING;

INSERT INTO vocabularies (id, created_at, updated_at, word, definition, example_sentence, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'circadian', 'Relating to biological processes that follow an approximately 24-hour cycle.', 'Shift workers often suffer circadian disruption that affects their health.', 'd11f0011-0011-0011-0011-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'melatonin', 'A hormone secreted by the pineal gland that regulates the sleep-wake cycle.', 'Melatonin levels rise in the evening to prepare the body for sleep.', 'd11f0011-0011-0011-0011-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'adenosine', 'A neurotransmitter that accumulates during wakefulness and promotes sleep pressure.', 'Caffeine blocks adenosine receptors, temporarily suppressing sleepiness.', 'd11f0011-0011-0011-0011-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'homeostasis', 'The self-regulating process by which a biological system maintains internal stability.', 'Sleep homeostasis ensures we recover sleep debt by sleeping longer after deprivation.', 'd11f0011-0011-0011-0011-000000000002'),
(uuid_generate_v4(), NOW(), NOW(), 'consistent', 'Unchanging in behaviour or performance over time.', 'A consistent sleep schedule aligns with circadian biology for optimal rest.', 'd11f0011-0011-0011-0011-000000000002') ON CONFLICT (id) DO NOTHING;

INSERT INTO quizzes (id, created_at, updated_at, question, type, options, correct_answer, explanation, article_content_id) VALUES
(uuid_generate_v4(), NOW(), NOW(), 'How does artificial light disrupt sleep?', 'MULTIPLE_CHOICE', '{"choices":[{"key":"0","text":"It increases adenosine clearance during wakefulness","explanation":null},{"key":"1","text":"Short-wavelength light suppresses melatonin secretion, delaying circadian phase","explanation":null},{"key":"2","text":"It activates the glymphatic system prematurely","explanation":null},{"key":"3","text":"It raises core body temperature above the threshold for sleep onset","explanation":null}],"explanations":null}'::jsonb, '1', 'Blue-spectrum light from screens activates ipRGCs in the retina, inhibiting SCN-mediated melatonin secretion and delaying the circadian phase, thereby postponing sleep onset.', 'd11f0011-0011-0011-0011-000000000002') ON CONFLICT (id) DO NOTHING;
