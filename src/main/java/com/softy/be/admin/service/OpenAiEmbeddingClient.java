package com.softy.be.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiEmbeddingClient {

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${embedding.api-key:${EMBEDDING_API_KEY}}")
    private String apiKey;

    @Value("${embedding.model:${EMBEDDING_MODEL}}")
    private String model;

    @Value("${embedding.url:${EMBEDDING_API_URL}}")
    private String embeddingUrl;

    @Value("${embedding.request-interval-ms:100}")
    private long requestIntervalMs;

    @Value("${embedding.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${embedding.retry.initial-delay-ms:300}")
    private long retryInitialDelayMs;

    @Value("${embedding.retry.backoff-multiplier:2.0}")
    private double retryBackoffMultiplier;

    private final Object rateLimitLock = new Object();
    private volatile long lastRequestAtMs = 0L;

    public List<Double> createEmbedding(String input) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("EMBEDDING_API_KEY가 누락되었습니다.");
        }
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("임베딩 입력값이 비어 있습니다.");
        }

        RestTemplate restTemplate = restTemplateBuilder.build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        EmbeddingRequest request = new EmbeddingRequest(model, input);
        HttpEntity<EmbeddingRequest> entity = new HttpEntity<>(request, headers);

        long delayMs = Math.max(0, retryInitialDelayMs);
        int attempts = Math.max(1, maxRetryAttempts);

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                applyRateLimitIfNeeded();
                ResponseEntity<EmbeddingResponse> response = restTemplate.postForEntity(
                        embeddingUrl,
                        entity,
                        EmbeddingResponse.class
                );

                EmbeddingResponse body = response.getBody();
                if (body == null || body.data == null || body.data.isEmpty()) {
                    throw new IllegalStateException("임베딩 API 응답이 비어 있습니다.");
                }

                List<Double> embedding = body.data.get(0).embedding;
                if (embedding == null || embedding.isEmpty()) {
                    throw new IllegalStateException("임베딩 벡터가 비어 있습니다.");
                }

                return embedding.stream()
                        .map(value -> Objects.requireNonNullElse(value, 0.0))
                        .toList();
            } catch (HttpStatusCodeException e) {
                if (!isRetryableStatus(e.getStatusCode()) || attempt == attempts) {
                    throw e;
                }
                log.warn("임베딩 API 재시도 예정(status={}, attempt={}/{}).", e.getStatusCode(), attempt, attempts);
                sleepForRetry(delayMs);
                delayMs = (long) (delayMs * Math.max(1.0, retryBackoffMultiplier));
            } catch (ResourceAccessException e) {
                if (attempt == attempts) {
                    throw e;
                }
                log.warn("임베딩 API 타임아웃/연결 오류 재시도 예정(attempt={}/{}).", attempt, attempts);
                sleepForRetry(delayMs);
                delayMs = (long) (delayMs * Math.max(1.0, retryBackoffMultiplier));
            }
        }

        throw new IllegalStateException("임베딩 API 호출 재시도 횟수를 초과했습니다.");
    }

    private boolean isRetryableStatus(HttpStatusCode statusCode) {
        int status = statusCode.value();
        return status == 429 || status == 503 || status == 504;
    }

    private void applyRateLimitIfNeeded() {
        long interval = Math.max(0, requestIntervalMs);
        if (interval == 0) {
            return;
        }

        synchronized (rateLimitLock) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRequestAtMs;
            long waitMs = interval - elapsed;
            if (waitMs > 0) {
                sleepForRetry(waitMs);
            }
            lastRequestAtMs = System.currentTimeMillis();
        }
    }

    private void sleepForRetry(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("재시도 대기 중 인터럽트가 발생했습니다.", e);
        }
    }

    private record EmbeddingRequest(
            String model,
            String input
    ) {
    }

    @SuppressWarnings("unused")
    private static class EmbeddingResponse {
        public List<EmbeddingData> data;
    }

    @SuppressWarnings("unused")
    private static class EmbeddingData {
        public List<Double> embedding;
    }
}
