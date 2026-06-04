package com.curiofeed.backend.domain.service;

import com.curiofeed.backend.domain.entity.ArticleContent;
import com.curiofeed.backend.domain.entity.DifficultyLevel;
import com.curiofeed.backend.domain.repository.ArticleContentRepository;
import com.curiofeed.backend.infrastructure.tts.GoogleTranslateTtsClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArticleAudioService {

    private final ArticleContentRepository articleContentRepository;
    private final GoogleTranslateTtsClient ttsClient;

    @Transactional
    public byte[] getOrGenerateAudio(UUID articleId, DifficultyLevel level) {
        ArticleContent content = articleContentRepository.findByArticleIdAndLevel(articleId, level)
                .orElseThrow(() -> new IllegalArgumentException("Article content not found"));

        if (content.getAudioData() != null && content.getAudioData().length > 0) {
            return content.getAudioData();
        }

        // Generate audio
        String textToRead = content.getArticle().getTitle() + ".\n\n" + content.getContent();
        byte[] newAudioData = ttsClient.generateTts(textToRead);

        if (newAudioData.length > 0) {
            content.updateAudioData(newAudioData);
            articleContentRepository.save(content);
        }

        return newAudioData;
    }
}
