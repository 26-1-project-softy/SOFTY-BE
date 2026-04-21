package com.softy.be.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class OpenAiEmbeddingClient {

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.embedding-model:text-embedding-3-small}")
    private String model;

    @Value("${openai.embedding-url:https://api.openai.com/v1/embeddings}")
    private String embeddingUrl;

    public List<Double> createEmbedding(String input) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY가 누락되었습니다.");
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

        ResponseEntity<EmbeddingResponse> response = restTemplate.postForEntity(
                embeddingUrl,
                entity,
                EmbeddingResponse.class
        );

        EmbeddingResponse body = response.getBody();
        if (body == null || body.data == null || body.data.isEmpty()) {
            throw new IllegalStateException("OpenAI 임베딩 응답이 비어 있습니다.");
        }

        List<Double> embedding = body.data.get(0).embedding;
        if (embedding == null || embedding.isEmpty()) {
            throw new IllegalStateException("OpenAI 임베딩 벡터가 비어 있습니다.");
        }
        return embedding.stream().map(value -> Objects.requireNonNullElse(value, 0.0)).toList();
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
