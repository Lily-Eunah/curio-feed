import type { Article } from '../types';

export const ARTICLES: Article[] = [
  {
    id: 'art-001',
    category: 'Tech',
    date: 'Apr 25, 2026',
    readTime: '5 min read',
    title: 'The Age of Invisible Computers',
    excerpt: 'Technology is disappearing into the background of our lives — and that might change everything.',
    vocabulary: [
      { word: 'ambient', definition: 'Present in the surrounding environment in a quiet, unobtrusive way.', example: 'The ambient music in the café made everyone feel relaxed.' },
      { word: 'ubiquitous', definition: 'Present, appearing, or found everywhere.', example: 'Smartphones have become ubiquitous in modern society.' },
      { word: 'seamless', definition: 'Smooth and continuous, with no apparent gaps or interruptions.', example: 'The app provides a seamless experience across all devices.' },
      { word: 'embedded', definition: 'Fixed firmly and deeply within a surrounding environment.', example: 'The sensor is embedded in the wall and invisible to users.' },
      { word: 'latency', definition: 'A delay before data transfer begins; slowness in response.', example: 'Low latency is critical for real-time video calls.' },
    ],
    body: `There was a time when computers were room-sized machines that only scientists could operate. Then they shrank to desktops, then laptops, then phones. Now, they are {{ambient}}—woven into the fabric of everyday life in ways we rarely notice.

Smart thermostats learn your schedule. Your car adjusts its mirrors automatically. Your headphones sense when you put them down and pause the music. Computing has become {{ubiquitous}}, but its interfaces have nearly vanished.

This is the promise of ambient computing: technology that serves you without demanding your attention. The goal is a {{seamless}} experience where the digital and physical worlds merge so gracefully that the boundary between them disappears.

But this transformation comes with questions. When sensors are {{embedded}} in every surface, who owns the data they collect? When every interaction is monitored to reduce {{latency}} and improve predictions, what happens to privacy?

The invisible computer is powerful precisely because it is invisible. That invisibility is both its greatest feature and its most significant risk.`,
    quiz: {
      q1: {
        question: 'What is the main idea of this article?',
        options: [
          'Computers are getting smaller every year',
          'Technology is becoming invisible but raises privacy questions',
          'Scientists no longer use large computers',
          'Smartphones are replacing all other devices',
        ],
        correct: 1,
        explanation: 'The article argues that computing is becoming ambient—powerful but invisible—which creates both benefits and privacy risks.',
      },
      q2: {
        question: "Which best matches 'ambient' as used in this article?",
        options: ['Loud and obvious', 'Quietly present everywhere', 'Temporarily available', 'Centrally controlled'],
        correct: 1,
        explanation: "'Ambient' means present in the surrounding environment in a subtle, unobtrusive way.",
      },
      q3: {
        question: "Why does the author call invisibility both a 'feature' and a 'risk'? Use your own words.",
        modelAnswer: 'It is a feature because it makes technology easier and less intrusive to use. It is a risk because hidden technology can collect data without users noticing, raising serious privacy concerns.',
      },
    },
  },
  {
    id: 'art-002',
    category: 'Science',
    date: 'Apr 23, 2026',
    readTime: '4 min read',
    title: 'Why Your Brain Needs More Darkness',
    excerpt: "Light pollution isn't just an astronomy problem — it may be quietly disrupting human health at a cellular level.",
    vocabulary: [
      { word: 'circadian', definition: 'Relating to biological processes that repeat approximately every 24 hours.', example: "Jet lag disrupts the body's circadian rhythm." },
      { word: 'melatonin', definition: 'A hormone produced by the brain that regulates sleep-wake cycles.', example: 'Darkness triggers the release of melatonin in the brain.' },
      { word: 'cumulative', definition: 'Increasing or growing by successive additions over time.', example: 'The cumulative effect of poor sleep can harm your health.' },
      { word: 'suppress', definition: 'To prevent something from happening or being expressed.', example: 'Bright light can suppress the production of sleep hormones.' },
      { word: 'prevalence', definition: 'The fact or condition of being widespread.', example: 'The prevalence of screen use has increased in recent decades.' },
    ],
    body: `Every cell in your body contains a tiny clock. These clocks run on a {{circadian}} rhythm—a roughly 24-hour cycle that governs when you feel awake, when you get hungry, and when you heal. For most of human history, darkness was the signal that told those clocks to wind down.

That signal is disappearing. Artificial light is now so prevalent that two-thirds of the world's population cannot see the Milky Way at night. This is called light pollution, and its effects go far beyond ruined stargazing.

When you are exposed to bright light at night, your brain delays the release of {{melatonin}}, the hormone that prepares your body for sleep. The disruption is subtle but {{cumulative}}. Over weeks and months, it can interfere with metabolism, immune function, and mood.

Blue light—emitted by phones and screens—is particularly effective at suppressing melatonin. Research shows it can {{suppress}} melatonin production for up to three hours after exposure, pushing your sleep window back and reducing quality even when you do eventually rest.

The {{prevalence}} of sleep problems in modern societies may be, at least in part, a light problem. The fix sounds simple: more darkness. In practice, it requires rethinking how we design our cities, our homes, and our evenings.`,
    quiz: {
      q1: {
        question: 'What is the main effect of artificial light described?',
        options: [
          'It improves metabolism and mood',
          'It disrupts circadian rhythms and sleep hormones',
          'It has no effect on human health',
          'It helps people sleep deeper',
        ],
        correct: 1,
        explanation: 'Artificial light at night disrupts circadian rhythms and delays melatonin release, affecting sleep quality and overall health.',
      },
      q2: {
        question: 'Which type of light is described as most harmful to melatonin?',
        options: ['Red light', 'Yellow light', 'Blue light', 'Green light'],
        correct: 2,
        explanation: 'Blue light from phones and screens can suppress melatonin production for up to three hours after exposure.',
      },
      q3: {
        question: "The author says the fix 'sounds simple.' What do they mean, and why is it hard in practice?",
        modelAnswer: 'The fix (more darkness at night) is conceptually easy, but difficult because it requires changing city infrastructure, home design habits, and personal screen routines—all deeply embedded in modern life.',
      },
    },
  },
  {
    id: 'art-003',
    category: 'Business',
    date: 'Apr 22, 2026',
    readTime: '6 min read',
    title: 'The Hidden Economics of Your Morning Coffee',
    excerpt: 'From Ethiopian highlands to your cup — a global commodity chain shaped by weather, politics, and a century of financial engineering.',
    vocabulary: [
      { word: 'commodity', definition: 'A raw material or agricultural product that can be bought and sold.', example: "Oil is one of the world's most traded commodities." },
      { word: 'volatility', definition: 'Tendency to change rapidly and unpredictably.', example: 'Currency volatility makes international trade riskier.' },
      { word: 'speculation', definition: 'Engaging in risky financial transactions hoping to profit from price changes.', example: 'Speculation in housing markets contributed to the 2008 crisis.' },
      { word: 'margin', definition: 'The amount by which revenue exceeds costs; profit earned.', example: 'The café operates on thin margins despite high prices.' },
      { word: 'hedge', definition: 'To protect against financial loss by making balancing investments.', example: 'Farmers use futures contracts to hedge against price drops.' },
    ],
    body: `Coffee is the world's second most traded {{commodity}} after oil. Each morning, hundreds of millions of people participate in a global market whose complexity would astonish them.

The price you pay at your local café reflects decisions made by farmers in Ethiopia and Brazil, traders in New York and London, and financial instruments developed over a century of {{volatility}} management.

Coffee prices swing dramatically based on weather. A frost in Brazil can double futures prices overnight. This volatility invites {{speculation}}: hedge funds and commodity traders bet on price movements, sometimes amplifying the very swings they claim to be managing.

Meanwhile, the farmers at the beginning of this chain earn the smallest slice of the value. In a $5 cup of coffee, the grower typically receives less than five cents. The rest is absorbed by processing, shipping, roasting, branding, and retail {{margin}}.

Some producers {{hedge}} their exposure by selling forward contracts—locking in a price before harvest. But this protection cuts both ways: if prices rise dramatically, they miss the upside. If prices fall, they are protected, but only until the contract expires.

The next time you hold a warm cup, consider the system that produced it: fragile, interconnected, and wildly underappreciated.`,
    quiz: {
      q1: {
        question: 'Why do coffee prices change so dramatically?',
        options: [
          'Because of changing consumer tastes',
          'Because weather affects crops and invites speculation',
          'Because shipping costs are unpredictable',
          'Because roasters change recipes frequently',
        ],
        correct: 1,
        explanation: 'Weather events like frost can double prices overnight, and this volatility attracts speculators who may amplify price swings.',
      },
      q2: {
        question: "What does it mean for a farmer to 'hedge'?",
        options: [
          'Plant crops in multiple locations',
          'Sell coffee at a fixed price before harvest',
          'Hire extra workers during harvest season',
          'Invest in coffee shop businesses',
        ],
        correct: 1,
        explanation: 'To hedge means selling forward contracts, locking in a price before harvest to protect against future price drops.',
      },
      q3: {
        question: "The author says farmers earn 'the smallest slice.' How does the article explain this?",
        modelAnswer: "Of a $5 cup, farmers receive less than five cents because value is absorbed at every stage: processing, shipping, roasting, branding, and retail. Those closest to the raw material capture the least value in complex global supply chains.",
      },
    },
  },
  {
    id: 'art-004',
    category: 'Culture',
    date: 'Apr 21, 2026',
    readTime: '5 min read',
    title: 'What Jazz Invented That We Still Use Today',
    excerpt: "The improvisational logic of jazz didn't stay in music. It spread into architecture, software, and how we think about creativity.",
    vocabulary: [
      { word: 'improvisation', definition: 'Creating and performing spontaneously, without prior preparation.', example: 'Jazz musicians are celebrated for their skill at improvisation.' },
      { word: 'syncopation', definition: 'A musical rhythm that emphasizes the normally weak beat.', example: "The drummer's syncopation gave the song unexpected energy." },
      { word: 'canonical', definition: 'Conforming to an accepted standard; widely recognized.', example: "Miles Davis's Kind of Blue is considered a canonical jazz album." },
      { word: 'vernacular', definition: 'A style or manner particular to a specific group or time.', example: 'The building used a local vernacular style with brick and clay.' },
      { word: 'codify', definition: 'To arrange something into a systematic set of rules.', example: 'The team tried to codify their creative process into a framework.' },
    ],
    body: `When jazz emerged in New Orleans in the early twentieth century, critics called it noise. Within a decade, it had changed how the world thought about music. Within a half-century, its logic had spread far beyond music altogether.

The central innovation of jazz was {{improvisation}}. Unlike classical music, where performers execute a fixed score, jazz musicians create in real time, responding to each other's choices. Structure exists—harmonies, progressions, the form of the song—but within it, everything is negotiated live.

{{Syncopation}} was the sonic signature of this approach: placing rhythmic emphasis where it wasn't expected, creating tension and release that felt viscerally different from anything before it.

What jazz musicians were doing, philosophers and theorists would later {{codify}} as a model for collaborative creativity. The jazz ensemble became a metaphor for high-functioning teams: individuals with deep expertise, a shared framework, and the freedom to contribute within it.

This jazz {{vernacular}} entered architecture, software development, and even military strategy. The idea that a {{canonical}} form can enable rather than constrain creativity—that you can have both structure and spontaneity—is one of jazz's most enduring gifts.`,
    quiz: {
      q1: {
        question: "What was jazz's central innovation, according to the article?",
        options: [
          'A new type of musical instrument',
          'Real-time improvisation within shared structure',
          'A fixed notation system for rhythm',
          'Playing music louder than classical orchestras',
        ],
        correct: 1,
        explanation: "The article identifies improvisation—creating in real time while responding to other musicians—as jazz's central innovation.",
      },
      q2: {
        question: "What does the author mean when they say agile software methodology 'borrows from jazz thinking'?",
        options: [
          'Developers listen to jazz while coding',
          'Agile teams use musical notation to plan',
          'Agile combines structure with spontaneous collaboration',
          'Software is written using improvised code that changes daily',
        ],
        correct: 2,
        explanation: 'Like jazz, agile methodology combines a shared framework with freedom for individuals to contribute spontaneously.',
      },
      q3: {
        question: 'Give one example from the article where jazz logic spread beyond music. Explain why the jazz model fits.',
        modelAnswer: 'Agile software development. Like jazz ensembles, agile teams have a shared structure (sprints, standups) but individuals improvise and adapt in real time, responding to each other rather than following a fixed plan.',
      },
    },
  },
  {
    id: 'art-005',
    category: 'Tech',
    date: 'Apr 20, 2026',
    readTime: '4 min read',
    title: 'The Attention Economy Has a New Villain',
    excerpt: 'For years we blamed the algorithm. A new wave of research is pointing somewhere more uncomfortable.',
    vocabulary: [
      { word: 'algorithm', definition: 'A set of rules a computer follows to solve a problem or make decisions.', example: 'The recommendation algorithm shows content based on your history.' },
      { word: 'compulsive', definition: 'Resulting from an irresistible urge; done repeatedly and without control.', example: 'The compulsive need to check notifications disrupts focus.' },
      { word: 'dopamine', definition: 'A brain chemical linked to pleasure and reward-seeking behavior.', example: 'Likes and comments trigger dopamine responses in the brain.' },
      { word: 'friction', definition: 'In design, intentional resistance that slows down certain user actions.', example: 'Adding friction to checkout reduced impulsive purchases.' },
      { word: 'autonomy', definition: 'The right and ability to make your own independent choices.', example: 'Users want more autonomy over what their feed shows them.' },
    ],
    body: `For most of the past decade, the {{algorithm}} was cast as the villain of the attention economy. The feeds that kept us scrolling were, we were told, the product of optimization systems designed to maximize engagement at any cost.

That story was useful, and partly true. But new research is complicating it.

Studies from behavioral economists suggest that {{compulsive}} phone use is less about algorithmic manipulation and more about the underlying reward structure that humans bring to the device. The {{dopamine}} hit from a new notification isn't manufactured by an engineer; it's a feature of how human motivation has always worked, now pointed at a device that delivers rewards at superhuman speed.

This changes how we think about solutions. If the problem were purely algorithmic, the fix would be regulation: force platforms to add {{friction}}, limit recommendation systems, require chronological feeds.

But if the problem is partly human—if we are, in some sense, exploiting ourselves—then the solution requires something harder to legislate: a meaningful restoration of {{autonomy}} over how we spend our attention.

The algorithm didn't invent distraction. It just made it much, much easier.`,
    quiz: {
      q1: {
        question: 'What new villain does the article suggest beyond algorithms?',
        options: [
          'Social media companies',
          'Human reward-seeking behavior itself',
          'Smartphone manufacturers',
          'Behavioral economists',
        ],
        correct: 1,
        explanation: 'The article argues compulsive phone use stems partly from human dopamine-reward systems, not just algorithmic design.',
      },
      q2: {
        question: "What is 'friction' as used in this design context?",
        options: [
          'Disagreements between designers',
          'Technical bugs that slow apps',
          'Intentional resistance to slow certain user actions',
          'The speed at which notifications arrive',
        ],
        correct: 2,
        explanation: 'Friction in design means adding intentional resistance—extra steps or delays—to slow down behaviors like impulsive purchases.',
      },
      q3: {
        question: "The author says the real fix requires 'something harder to legislate.' What do they mean?",
        modelAnswer: "Regulations can change platform algorithms, but they cannot change individual behavior or attention habits. Restoring personal autonomy requires self-awareness and deliberate choice—which are inherently harder to mandate by law than a technical platform change.",
      },
    },
  },
  {
    id: 'art-006',
    category: 'Science',
    date: 'Apr 19, 2026',
    readTime: '3 min read',
    title: 'The Trees That Talk Underground',
    excerpt: 'Forests communicate through a network of fungi — and new research suggests they do so with surprising sophistication.',
    vocabulary: [
      { word: 'mycorrhizal', definition: 'Relating to a symbiotic relationship between fungi and plant roots.', example: 'Mycorrhizal networks allow trees to share nutrients underground.' },
      { word: 'symbiosis', definition: 'A close interaction between two organisms that benefits both parties.', example: 'The flowers and bees exist in a symbiosis that benefits both.' },
      { word: 'photosynthesis', definition: 'The process by which plants use sunlight to produce food.', example: 'Seedlings in deep shade cannot perform sufficient photosynthesis.' },
      { word: 'nutrient', definition: 'A substance that nourishes living things and supports growth.', example: 'The fungi transport carbon and nutrients between trees.' },
      { word: 'reciprocal', definition: 'Given or felt by each toward the other; mutual.', example: 'The relationship between trees and fungi is reciprocal.' },
    ],
    body: `Beneath any old-growth forest floor lies a network that ecologists have called the "wood wide web"—a vast web of {{mycorrhizal}} fungi connecting tree roots across hundreds of meters.

The relationship between trees and fungi is one of {{symbiosis}}: the fungi receive sugars that the trees produce through {{photosynthesis}}, and in exchange, dramatically increase the trees' ability to absorb water and {{nutrient}} from the soil.

But recent research suggests the exchange is more complex than a simple transaction. Older "mother trees" appear to send more carbon to younger seedlings growing in their shade—trees that cannot yet perform enough photosynthesis to sustain themselves. The support appears {{reciprocal}}: seedlings that are helped tend to root into the network and contribute as they mature.

What is driving this? Is it kin recognition—trees preferentially supporting their own offspring? Is it simply the physics of how carbon moves through a network? Researchers are divided.

What is less contested is the practical implication: healthy mycorrhizal networks make forests more resilient. Logging practices that sever these connections, or replanting programs using a single species, can disrupt the underground infrastructure forests depend on to survive drought and disease.`,
    quiz: {
      q1: {
        question: "What is the 'wood wide web'?",
        options: [
          'A type of internet signal through trees',
          'A fungal network connecting tree roots underground',
          'A research database about forests',
          'A network of roots trees grow to find water',
        ],
        correct: 1,
        explanation: "The 'wood wide web' is a mycorrhizal fungal network connecting tree roots, allowing trees to share nutrients and carbon.",
      },
      q2: {
        question: "What does 'reciprocal' mean as used in the article?",
        options: [
          'One-directional',
          'Mutual—given and received by both',
          'Occasional or random',
          'Competitive, where one benefits at the expense of the other',
        ],
        correct: 1,
        explanation: "'Reciprocal' means mutual. Seedlings that receive help eventually root into the network and contribute back as they mature.",
      },
      q3: {
        question: "Why might a logging practice using only a single tree species damage a forest's resilience?",
        modelAnswer: "A single-species forest has less network diversity. Older mother trees that normally support younger ones may be absent, and the mycorrhizal network's ability to help trees survive drought, disease, and stress may be significantly weakened.",
      },
    },
  },
  {
    id: 'art-007',
    category: 'Business',
    date: 'Apr 18, 2026',
    readTime: '4 min read',
    title: 'Why Luxury Brands Refuse to Go on Sale',
    excerpt: "There's a reason a Hermès bag never appears in a Black Friday email. It's not snobbery — it's economics.",
    vocabulary: [
      { word: 'scarcity', definition: 'The state of being scarce or in short supply.', example: 'Artificial scarcity keeps the price of limited-edition sneakers high.' },
      { word: 'prestige', definition: 'Widespread respect and admiration associated with quality or exclusivity.', example: "The brand's prestige depends entirely on its exclusivity." },
      { word: 'depreciation', definition: 'A reduction in the value of something over time.', example: 'Unlike most goods, some luxury items have negative depreciation.' },
      { word: 'aspirational', definition: 'Appealing to social ambition; representing a desired status.', example: 'The campaign was aspirational, showing the bag in elite settings.' },
      { word: 'dilute', definition: 'To weaken something by reducing its quality or concentration.', example: "Putting products on sale can dilute a brand's premium image." },
    ],
    body: `In 2021, Burberry destroyed £28 million worth of unsold inventory rather than discount it. This was not a mistake. It was a strategy.

Luxury economics operates on a principle that inverts normal market logic. For most goods, lower prices increase demand. For luxury goods, lower prices can destroy it. The value of a luxury product is partly its {{scarcity}} and partly its {{prestige}}—and both are undermined by ubiquity and discounting.

A luxury bag is not just a bag. It is a signal. When you carry it, you communicate something: purchasing power, taste, membership in a certain stratum. That signal depends entirely on the bag being difficult to obtain.

Unlike most goods, which suffer {{depreciation}} the moment they leave the store, well-maintained luxury goods often appreciate. A classic Hermès Birkin has outperformed many stock indices over the past two decades. This investment logic makes the brands more {{aspirational}}, which in turn justifies higher prices.

The strategy requires iron discipline. One sale, one warehouse outlet, one significant markdown, and the illusion cracks. Discounting signals desperation, and desperation would {{dilute}} the one thing luxury brands truly sell: the perception of limitless demand.

Burning the product is cheaper.`,
    quiz: {
      q1: {
        question: 'Why did Burberry destroy £28 million of inventory?',
        options: [
          'The products were defective',
          'To avoid paying import taxes',
          "To protect the brand's scarcity and prestige",
          'Because of a warehouse fire',
        ],
        correct: 2,
        explanation: 'Luxury brands maintain value through scarcity. Discounting goods would undermine brand prestige and the signal value of ownership.',
      },
      q2: {
        question: "What is 'depreciation' as used here?",
        options: [
          'The increase in value over time',
          'A reduction in value as time passes',
          'The cost of transporting luxury goods',
          'The process of pricing a new product',
        ],
        correct: 1,
        explanation: 'Depreciation means reduction in value over time. Luxury goods are unusual because they often appreciate rather than depreciate.',
      },
      q3: {
        question: "The article says luxury brands sell 'the perception of limitless demand.' What does this mean?",
        modelAnswer: 'Luxury brands project the idea that products are always desired and never need discounting—appearing above commercial pressure. One sale signals finite demand and breaks the fantasy of exclusivity that makes products desirable in the first place.',
      },
    },
  },
  {
    id: 'art-008',
    category: 'Culture',
    date: 'Apr 17, 2026',
    readTime: '5 min read',
    title: 'The Art That Was Never Meant to Be Seen',
    excerpt: 'For centuries, craftsmen carved intricate details into cathedral ceilings too high for any visitor to see. What does that tell us?',
    vocabulary: [
      { word: 'intricate', definition: 'Very complicated or detailed in design or structure.', example: 'The intricate carvings took years to complete.' },
      { word: 'secular', definition: 'Not connected to religious or spiritual matters; worldly.', example: 'The museum houses both secular and religious art.' },
      { word: 'transcendence', definition: 'The quality of exceeding ordinary limits; spiritual elevation.', example: 'Medieval builders sought transcendence through extreme height and light.' },
      { word: 'anonymous', definition: 'Having no known name; not identified.', example: 'Most medieval craftsmen remained anonymous, their names unrecorded.' },
      { word: 'intrinsic', definition: 'Belonging naturally; essential; having value in itself.', example: 'The craftsmen seemed to find intrinsic value in doing the work well.' },
    ],
    body: `High on the ceiling of many medieval cathedrals, in the shadowed spaces above the vaulted arches, there are {{intricate}} carvings that no visitor has ever clearly seen. The men who made them worked by candlelight, in difficult conditions, on details that would be invisible to every human eye below.

No one required this level of care. There were no quality inspectors for the roofline of a twelfth-century church. Yet the work was done with the same precision as the altar carvings visible to every worshipper.

The obvious explanation is religious: God could see the unseen work. But scholars note that many of these craftsmen were {{secular}} workers—paid laborers, not monks or mystics.

A second explanation may be more interesting: that the medieval concept of craft involved an idea of {{transcendence}} through excellence. The work was good because it had to be good. The carver's identity was {{anonymous}}, their name unrecorded, yet the work carried its own meaning.

What this challenges is a modern assumption: that quality requires an audience. We tend to produce work relative to how we will be perceived. The cathedral carvers suggest a different logic—that some value is {{intrinsic}}, that the unseen can still matter, and that excellence pursued privately might be the most honest kind.`,
    quiz: {
      q1: {
        question: 'What central observation does the author make about medieval craftsmen?',
        options: [
          'They worked too quickly and made many mistakes',
          'They carved intricate details no one would ever clearly see',
          'They were paid much more than other workers',
          'They signed their names on all their work',
        ],
        correct: 1,
        explanation: 'The article observes craftsmen carved highly detailed work in places too high for visitors to see—careful work for no visible audience.',
      },
      q2: {
        question: "What does 'intrinsic value' mean in this context?",
        options: [
          'Value that comes from external recognition',
          'Value that depends on the price paid',
          'Value that exists in itself, regardless of audience',
          'Value determined by religious authorities',
        ],
        correct: 2,
        explanation: 'Intrinsic value means value that exists inherently, regardless of whether anyone sees, praises, or rewards the work.',
      },
      q3: {
        question: "The author says cathedral carvers challenge a 'modern assumption.' What is that assumption?",
        modelAnswer: 'The modern assumption is that quality requires an audience—that we work well only when others will see and evaluate our output. The cathedral carvers produced excellent work in a place no one could see, suggesting excellence can exist independently of recognition.',
      },
    },
  },
];
