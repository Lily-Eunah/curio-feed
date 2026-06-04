package com.curiofeed.backend.infrastructure.tts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class GoogleTranslateTtsClient {

    private static final String TTS_URL = "https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=en-US&q=";

    public byte[] generateTts(String text) {
        if (text == null || text.isBlank()) {
            return new byte[0];
        }

        // Split text into chunks of max 200 chars to avoid Google TTS limit
        String[] chunks = text.split("(?<=[.?!])\\s+");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for (String chunk : chunks) {
            String cleanChunk = chunk.replaceAll("<[^>]*>", "").trim();
            if (cleanChunk.isBlank()) continue;

            // If a chunk is still too long, we might need further splitting, but sentences are usually < 200 chars
            if (cleanChunk.length() > 200) {
                cleanChunk = cleanChunk.substring(0, 199);
            }

            try {
                String encodedText = URLEncoder.encode(cleanChunk, StandardCharsets.UTF_8);
                URL url = URI.create(TTS_URL + encodedText).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    try (InputStream is = connection.getInputStream()) {
                        is.transferTo(outputStream);
                    }
                } else {
                    log.warn("Failed to fetch TTS for chunk: {}. HTTP code: {}", cleanChunk, connection.getResponseCode());
                }
            } catch (Exception e) {
                log.error("Exception fetching TTS for chunk: {}", cleanChunk, e);
            }
        }

        return outputStream.toByteArray();
    }
}
