package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.config.OllamaProperties;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.model.GenerationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live Ollama cross-topic validation test — disabled by default.
 *
 * Run manually:
 *   Git Bash : RUN_OLLAMA_TESTS=true ./gradlew test --tests "*OllamaCrossTopicValidationTest"
 *   PowerShell: $env:RUN_OLLAMA_TESTS="true"; ./gradlew test --tests "*OllamaCrossTopicValidationTest*"
 */
@Tag("ollama")
@EnabledIfEnvironmentVariable(named = "RUN_OLLAMA_TESTS", matches = "true")
class OllamaCrossTopicValidationTest {

    private static final Logger log = LoggerFactory.getLogger(OllamaCrossTopicValidationTest.class);

    private static final String BASE_URL = "http://192.168.45.100:11434";
    private static final String MODEL = "gemma4:e4b";

    // Article A: Geopolitics (existing Hormuz article)
    private static final String ARTICLE_GEOPOLITICS = """
        # Strait of Hormuz closed again, Iran says, as ships attacked
        Iran says it is closing the Strait of Hormuz again to commercial vessels and that any ship that approaches it will be targeted.
        The closure came as reports emerged of vessels in or near the strait, including a tanker, were targeted by Tehran on Saturday.
        The Islamic Revolution Guard Corps (IRGC) blamed a continuing US blockade for its decision, which comes a day after Iran's foreign minister announced the key global shipping channel had been temporarily reopened.
        US President Donald Trump said Iran cannot "blackmail" the US with threats regarding the waterway, which Tehran has effectively blocked for nearly two months - causing global energy prices to soar.
        The Islamic Revolutionary Guard Corps (IRGC) Navy warned in a statement on Saturday that "no vessel is to move from its anchorage in the Persian Gulf or the Sea of Oman".
        It said a number of vessels had passed through the strait under its management since Friday night, but that it would shut again until the US stopped its blockade of Iranian ports.
        "Approaching the Strait of Hormuz will be considered co-operation with the enemy, and the offending vessel will be targeted," the IRGC added.
        Trump said on Friday that a naval blockade of Iranian ports would continue until a peace deal was agreed between the two countries. A two-week ceasefire currently in effect is due to expire on 22 April.
        The US said it had turned away 23 ships since it began enforcing the blockade on 13 April.
        Iran's Supreme National Security Council (SNSC) said this was a violation of the ceasefire agreement and that it would stop the reopening of the strait while it was still in place.
        On negotiations to bring about an end to the war, the SNSC said new proposals had been put forward by the US, which Tehran was "currently reviewing and has not yet responded to". Peace talks held earlier this month ended without an agreement.
        "We have very good conversations going on. It's working out very well," Trump said about the state of negotiations with Tehran on Saturday.
        There have been several reports of vessels being attacked by Iran on Saturday.
        Two Iranian gunboats opened fire on a tanker in the strait, the UK Maritime Trade Operations (UKMTO) said.
        A container ship was also hit by "an unknown projectile" off the north-eastern coast of Oman, damaging some containers, according to the UKMTO.
        About 20% of the world's oil and liquefied natural gas (LNG) is usually transported through the strait, but the number of ships making the journey has dramatically decreased during the recent conflict.
        The narrow chokepoint connects the Gulf to the Arabian Sea and is the only way to reach several oil-producing states by sea. The crisis has seen the price of a barrel of oil surge above $100 at points.
        Iran has previously threatened to attack tankers and other ships, as well as warning it had laid mines.
        """;

    // Article B: Health / Science
    private static final String ARTICLE_HEALTH = """
        # Ultra-processed foods linked to higher risk of early death, major study finds
        People who regularly eat ultra-processed foods — such as packaged snacks, ready meals, and sugary drinks — have a significantly higher risk of dying early from heart disease and cancer, according to a major new study published in the British Medical Journal.
        Researchers tracked more than 100,000 adults in France over 14 years, recording their diets and monitoring their health outcomes. Those who consumed the highest amounts of ultra-processed foods were 26% more likely to develop cardiovascular disease and 23% more likely to die from any cause compared with those who ate the least.
        Ultra-processed foods are typically made from industrial ingredients like hydrogenated oils, high-fructose corn syrup, artificial flavours, colours, and preservatives. They are often engineered to be hyper-palatable — meaning they are designed to override natural satiety signals and encourage overconsumption.
        "The more ultra-processed food someone eats, the greater the health risk," said Dr Marie Fiolet, the study's lead author at the Sorbonne in Paris. "Even small reductions in intake can make a meaningful difference."
        Critics of the study point out that people who eat a lot of ultra-processed food may also have other unhealthy habits — such as smoking, low physical activity, and poor sleep — making it difficult to isolate the specific effect of diet alone. The researchers attempted to control for these confounding factors, but acknowledged that residual effects could still influence the results.
        Public health experts say the findings add to a growing body of evidence that food quality, not just calorie count, matters for long-term health. Several countries, including Brazil and Chile, have already introduced front-of-pack warning labels on ultra-processed products, and campaigners in the UK and US are calling for similar measures.
        The food industry contested the study, arguing that many ultra-processed foods meet current nutritional guidelines and are consumed safely by millions. Industry representatives said that blaming a broad category of food manufacturing ignores the nutritional diversity within that group.
        Scientists emphasise that not all processed food is harmful — minimally processed items like frozen vegetables or canned fish can be part of a healthy diet. The concern centres specifically on the highly processed, additive-heavy products that now account for more than half of calories consumed in many high-income countries.
        """;

    @ParameterizedTest(name = "topic={0} level={1}")
    @CsvSource({
        "GEOPOLITICS, EASY",
        "GEOPOLITICS, MEDIUM",
        "GEOPOLITICS, HARD",
        "HEALTH, EASY",
        "HEALTH, MEDIUM",
        "HEALTH, HARD"
    })
    void crossTopicValidation(String topic, DifficultyLevel level) {
        OllamaProperties properties = new OllamaProperties(BASE_URL, MODEL, null, 5, 180, 16384, 0.3);
        OllamaLlmClient client = new OllamaLlmClient(properties, MODEL, RestClient.builder());
        ArticlePromptBuilder promptBuilder = new ArticlePromptBuilder();
        DefaultLlmResponseParser parser = new DefaultLlmResponseParser(new ObjectMapper());

        String article = topic.equals("GEOPOLITICS") ? ARTICLE_GEOPOLITICS : ARTICLE_HEALTH;
        String prompt = promptBuilder.build(article, level);
        String raw = client.generate(prompt);
        GenerationResult result = parser.parse(raw, GenerationResult.class);

        assertThat(result.hasContent()).isTrue();
        assertThat(result.vocabularies()).hasSize(5);
        assertThat(result.quizzes()).hasSize(3);

        String contentLower = result.content().toLowerCase();
        var vocabWords = result.vocabularies().stream().map(v -> v.word().toLowerCase()).toList();

        log.info("=== [{} {}] CONTENT (first 300 chars) ===\n{}", topic, level,
                result.content().substring(0, Math.min(300, result.content().length())));

        log.info("=== [{} {}] VOCABULARIES ===", topic, level);
        long missingCount = 0;
        for (var vocab : result.vocabularies()) {
            boolean inContent = contentLower.contains(vocab.word().toLowerCase());
            if (!inContent) missingCount++;
            log.info("  word={} inContent={} | def={} | ex={}",
                    vocab.word(), inContent, vocab.definition(), vocab.exampleSentence());
        }
        log.info("=== [{} {}] VOCAB-IN-CONTENT: {}/5 ({} missing) ===",
                topic, level, 5 - missingCount, missingCount);

        log.info("=== [{} {}] QUIZZES ===", topic, level);
        result.quizzes().forEach(q -> {
            log.info("  [{}] Q: {}", q.type(), q.question());
            log.info("    answer={}", q.correctAnswer());
            if (q.options() != null && q.options().getChoices() != null) {
                log.info("    choices={} allHaveExplanation={}",
                        q.options().getChoices().size(),
                        q.options().getChoices().stream().allMatch(c -> c.getExplanation() != null && !c.getExplanation().isBlank()));
            }
        });

        boolean q3InVocab = result.quizzes().size() >= 3 &&
                vocabWords.stream().anyMatch(w ->
                        result.quizzes().get(2).correctAnswer().toLowerCase().contains(w));
        boolean q2HasVocab = result.quizzes().size() >= 2 &&
                result.quizzes().get(1).options() != null &&
                result.quizzes().get(1).options().getChoices() != null &&
                result.quizzes().get(1).options().getChoices().stream().anyMatch(c ->
                        vocabWords.stream().anyMatch(w -> c.getText() != null && c.getText().toLowerCase().contains(w)));

        log.info("=== [{} {}] CROSS-CHECK: vocabInContent={}/5 q2HasVocab={} q3InVocab={} ===",
                topic, level, 5 - missingCount, q2HasVocab, q3InVocab);
    }
}
