package com.team6.backend.ai.infrastructure;

import com.team6.backend.ai.domain.AiErrorCode;
import com.team6.backend.ai.infrastructure.dto.GeminiGenerateRequest;
import com.team6.backend.ai.infrastructure.dto.GeminiGenerateResponse;
import com.team6.backend.global.infrastructure.exception.ApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class GeminiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiClient(
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${gemini.api-key:${GEMINI_API_KEY:}}") String apiKey,
            @Value("${gemini.model:${GEMINI_MODEL:gemini-2.5-flash}}") String model
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.apiKey = apiKey;
        this.model = model;
    }

    // Gemini 프롬프트 응답 생성
    public String generate(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApplicationException(AiErrorCode.AI_API_KEY_MISSING);
        }

        try {
            GeminiGenerateResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(GeminiGenerateRequest.from(text))
                    .retrieve()
                    .body(GeminiGenerateResponse.class);

            if (response == null || response.extractText().isBlank()) {
                throw new ApplicationException(AiErrorCode.AI_EMPTY_RESPONSE);
            }

            return response.extractText();
        } catch (ApplicationException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("[AI] Gemini API 요청 실패. model={}", model, e);
            throw new ApplicationException(AiErrorCode.AI_REQUEST_FAILED);
        }
    }
}