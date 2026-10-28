package com.leasepilot.backend.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

// One shared HTTP client for every Meta Model API call (Muse Spark, Voice Transcribe, SAM).
// It already knows the base URL and sends the API key on every request.
@Configuration
public class MetaApiConfig {

    @Bean
    public RestClient metaApiRestClient(@Value("${meta.api.base-url}") String baseUrl,
                                        @Value("${meta.api.key}") String apiKey) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }
}
