package com.travelmate.accommodation.ai;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Local LLM settings (travelmate.ai.ollama.*). */
@Getter
@Setter
@ConfigurationProperties(prefix = "travelmate.ai.ollama")
public class OllamaProperties {

    private boolean enabled = true;
    private String baseUrl = "http://localhost:11434";
    private String model = "llama3";
    private double temperature = 0.2;
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(60);
}
