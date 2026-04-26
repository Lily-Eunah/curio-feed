package com.curiofeed.backend.infrastructure.llm;

import com.curiofeed.backend.domain.entity.DifficultyLevel;
import org.springframework.stereotype.Component;

@Component
public class ArticlePromptBuilder {

    private static final String TEMPLATE = """
            You are an English learning content creator.

            Rewrite the article below for a {LEVEL} level English learner and generate vocabulary and quizzes.

            ---

            [LEVEL]

            - EASY: A2-B1. Use only high-frequency everyday words. Short, clear sentences. Avoid technical or rare vocabulary.
              Prefer the most common spoken words over formal or academic alternatives.
              If a simpler synonym exists, always use the simpler word.
            - MEDIUM: B1-B2. Moderate vocabulary and sentence variety. Topic-specific words are acceptable.
            - HARD: C1. Advanced, precise vocabulary. Complex and varied sentence structures.

            The rewritten content must preserve the key information and logical flow of the original article.
            Do NOT over-summarize — ensure all major points from the original are present.

            ---

            [VOCABULARY RULES]

            - Include EXACTLY 5 items
            - Vocabulary difficulty MUST strictly match the {LEVEL} level
            - Vocabulary words must be important for understanding the main idea of the content — avoid trivial or overly generic words
            - Use the BASE FORM of the word (e.g., "deteriorate" not "deteriorating", "negotiate" not "negotiations")
            - Each vocabulary word must appear in the content (base form preferred; inflected forms are acceptable if natural)
            - Do NOT include multiple forms of the same root word (e.g., not both "negotiate" and "negotiation")
            - Definition must be learner-friendly and include a simple usage context — avoid synonyms only
              Help the learner understand WHEN and HOW to use the word, not just what it means
              ✅ "used when something makes it hard to move forward or complete a task"
            - exampleSentence must reflect a natural, everyday situation unrelated to the article
              ✅ "She had a concern about her son's health after he started coughing"

            Format:
            {
              "word": string,
              "definition": string,
              "exampleSentence": string
            }

            ---

            [QUIZ RULES]

            Include EXACTLY 3 quizzes. Use different cognitive skills across quizzes — avoid repeating the same question pattern:

            1. MULTIPLE_CHOICE (Comprehension)
               - Test understanding of a key idea in the article
               - Require inference, cause-effect reasoning, or scenario-based thinking — not just copying a sentence

            2. MULTIPLE_CHOICE (Vocabulary Application)
               - MUST test one of the vocabulary words from the list above
               - The tested vocabulary word must NOT appear in the question
               - The vocabulary word should appear in choices where grammatically natural; grammar must be prioritized over forced insertion
               - At least TWO choices must use the vocabulary word; incorrect choices must reflect realistic misuse (wrong collocation, wrong context)
               - Only ONE choice must be clearly correct in meaning and usage
               - Test real context understanding — avoid simple synonym matching

            3. SHORT_ANSWER (Recall)
               - MUST require applying or understanding one of the vocabulary words in a new or inferred context
               - Must NOT simply recall a fact from the article
               - The answer must NOT be directly found as a phrase in the content
               - Preferred format: fill-in-the-blank with a vocabulary word
               - Must NOT overlap with other questions
               - Must NOT test factual recall of numbers or proper nouns
               - For EASY: the answer may be a simple phrase using a vocabulary word
               - For HARD: the answer should require deeper understanding and abstraction

            At least ONE quiz MUST use one of the vocabulary words in a new context.

            ---

            [MULTIPLE_CHOICE RULES]

            - 4 choices (A, B, C, D)
            - Only ONE correct answer
            - Wrong choices must be semantically similar or represent common misconceptions — NEVER absurd or off-topic
            - Use a mix of distractor types: incorrect usage, opposite meaning, or partial misunderstanding
            - Do NOT reuse full sentences or phrases from the content in any quiz question or choice
            - Each choice MUST have an "explanation" (why it is correct or why it is wrong)
            - "correctAnswer" must be exactly one of: "A", "B", "C", or "D"
            - The correct answer word or phrase must NOT appear in the question itself

            ---

            [SHORT_ANSWER RULES]

            - Answer must be concise: 1–5 words, short phrase only, NOT a full sentence
            - Must test English vocabulary or comprehension, NOT factual recall of numbers or proper nouns
            - The answer must require understanding of meaning, not copying

            ---

            [OUTPUT FORMAT]

            Return ONLY valid JSON.

            {
              "content": string,
              "vocabularies": [
                {
                  "word": string,
                  "definition": string,
                  "exampleSentence": string
                }
              ],
              "quizzes": [
                {
                  "type": "MULTIPLE_CHOICE",
                  "question": string,
                  "options": {
                    "choices": [
                      {"key": "A", "text": string, "explanation": string},
                      {"key": "B", "text": string, "explanation": string},
                      {"key": "C", "text": string, "explanation": string},
                      {"key": "D", "text": string, "explanation": string}
                    ]
                  },
                  "correctAnswer": "A",
                  "explanation": string
                },
                {
                  "type": "SHORT_ANSWER",
                  "question": string,
                  "options": {},
                  "correctAnswer": string,
                  "explanation": string
                }
              ]
            }

            ---

            [SELF-CHECK]

            Before returning, verify in this order:

            1. vocab = 5, quiz = 3, types = 2 MCQ + 1 SHORT
            2. All vocab words appear in the content
            3. Vocabulary words themselves are in base form (inflected forms may appear in the content)
            4. No duplicate root words in vocabulary list
            5. Vocabulary words are important to the main idea — not trivial or generic
            6. Vocabulary difficulty matches {LEVEL}; EASY uses only high-frequency spoken words
            7. All definitions explain WHEN/HOW to use the word, not just synonyms
            8. All exampleSentences use everyday situations unrelated to the article
            9. Vocabulary Application MCQ: at least 2 choices use the tested word; tested word not in question; only 1 choice is clearly correct
            10. SHORT_ANSWER requires applying vocabulary in a new context; answer not directly found in content
            11. No quiz question or choice reuses full sentences or phrases from the content
            12. MCQ wrong choices are plausible — not absurd or obviously wrong
            13. SHORT_ANSWER answer is a short phrase (1–5 words), not a full sentence
            14. SHORT_ANSWER does not test a number or proper noun
            15. Each MCQ choice has a non-empty "explanation"
            16. correctAnswer is a single letter: "A", "B", "C", or "D"
            17. JSON is valid and all fields are present

            If any condition fails, revise the ENTIRE output to fix all issues before returning.
            Do NOT partially fix — ensure the final output fully satisfies ALL rules.

            ---

            [ARTICLE]
            {ORIGINAL_CONTENT}
            """;

    public String build(String originalContent, DifficultyLevel level) {
        return TEMPLATE
                .replace("{LEVEL}", level.name())
                .replace("{ORIGINAL_CONTENT}", originalContent);
    }
}
