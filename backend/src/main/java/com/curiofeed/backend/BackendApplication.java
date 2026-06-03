package com.curiofeed.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.curiofeed.backend.config.GeminiProperties;
import com.curiofeed.backend.config.OllamaProperties;
import com.curiofeed.backend.config.PipelineProperties;

@SpringBootApplication
@EnableConfigurationProperties({OllamaProperties.class, GeminiProperties.class, PipelineProperties.class})
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
