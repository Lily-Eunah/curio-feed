package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.config.OllamaProperties;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.model.GenerationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class OllamaLlmPipelineIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(OllamaLlmPipelineIntegrationTest.class);

    private static final String BASE_URL = "http://192.168.45.100:11434";
    private static final String MODEL = "gemma4:e4b";
    // private static final String MODEL = "qwen3:14b-q4_K_M";

    // Source: https://www.bbc.com/news/articles/cx2631x6nelo
    private static final String ARTICLE_CONTENT = """
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
        Separately, at least two merchant vessels said they were hit by gunfire as they attempted to cross the strait, sources told news agency Reuters.
        India's foreign ministry said it had summoned the Iranian ambassador to convey its "deep concern at the shooting incident earlier today involving two Indian-flagged ships in the Strait of Hormuz".
        Data from tracking site MarineTraffic showed some vessels were able to make it through the strait while it was briefly open. Others were forced to change their route after the IRGC denied them access.
        About 20% of the world's oil and liquefied natural gas (LNG) is usually transported through the strait, but the number of ships making the journey has dramatically decreased during the recent conflict, which began when the US and Israel attacked Iran on 28 February.
        The narrow chokepoint connects the Gulf to the Arabian Sea and is the only way to reach several oil-producing states by sea. The crisis has seen the price of a barrel of oil surge above $100 ($74) at points.
        Iran has previously threatened to attack tankers and other ships, as well as warning it had laid mines.
        """;

    private ArticlePromptBuilder promptBuilder;
    private OllamaLlmClient llmClient;
    private DefaultLlmResponseParser responseParser;

    @BeforeEach
    void setUp() {
        OllamaProperties properties = new OllamaProperties(BASE_URL, MODEL, null, 5, 180);
        llmClient = new OllamaLlmClient(properties, MODEL, RestClient.builder());
        promptBuilder = new ArticlePromptBuilder();
        responseParser = new DefaultLlmResponseParser(new ObjectMapper());
    }

    @ParameterizedTest(name = "{0} 난이도로 기사 생성 파이프라인이 완전한 결과를 반환한다")
    @EnumSource(DifficultyLevel.class)
    void pipeline_allLevels_returnsFullGenerationResult(DifficultyLevel level) {
        String prompt = promptBuilder.build(ARTICLE_CONTENT, level);
        String rawResponse = llmClient.generate(prompt);
        GenerationResult result = responseParser.parse(rawResponse, GenerationResult.class);

        assertThat(result.hasContent()).isTrue();
        assertThat(result.vocabularies()).hasSize(5);
        assertThat(result.quizzes()).hasSize(3);

        log.info("=== [{}] CONTENT ===\n{}", level, result.content());

        log.info("=== [{}] VOCABULARIES ===", level);
        result.vocabularies().forEach(vocab -> {
            assertThat(vocab.word()).isNotBlank();
            assertThat(vocab.definition()).isNotBlank();
            assertThat(vocab.exampleSentence()).isNotBlank();
            log.info("  word={} | definition={} | example={}", vocab.word(), vocab.definition(), vocab.exampleSentence());
        });

        log.info("=== [{}] QUIZZES ===", level);
        result.quizzes().forEach(quiz -> {
            assertThat(quiz.type()).isNotNull();
            assertThat(quiz.question()).isNotBlank();
            assertThat(quiz.correctAnswer()).isNotBlank();
            log.info("  [{}] {}", quiz.type(), quiz.question());
            log.info("    answer={} | explanation={}", quiz.correctAnswer(), quiz.explanation());
            if (quiz.options() != null && quiz.options().getChoices() != null) {
                quiz.options().getChoices().forEach(choice ->
                    log.info("    {} - {}", choice.getKey(), choice.getText())
                );
            }
        });
    }
}
