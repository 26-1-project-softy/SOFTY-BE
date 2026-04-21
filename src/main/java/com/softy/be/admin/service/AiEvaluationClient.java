package com.softy.be.admin.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Duration;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;

@Component
@RequiredArgsConstructor
public class AiEvaluationClient {

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${ai.evaluation.base-url}")
    private String aiServerBaseUrl;

    public AiEvaluationResult getEvaluation(String evaluationId) {
        if (evaluationId == null || evaluationId.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "evaluationId is required.");
        }

        String normalizedEvaluationId = evaluationId.trim();
        URI uri = URI.create(aiServerBaseUrl + "/ai/evaluations/" + normalizedEvaluationId);

        RestTemplate restTemplate = restTemplateBuilder
                .requestFactory(() -> {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
                    factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
                    return factory;
                })
                .build();

        try {
            ResponseEntity<AiEvaluationApiResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    null,
                    AiEvaluationApiResponse.class
            );

            AiEvaluationApiResponse body = response.getBody();
            if (body == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "AI evaluation response is empty.");
            }

            return new AiEvaluationResult(
                    normalizedEvaluationId,
                    body.precision,
                    body.recall,
                    body.f1Score,
                    body.status,
                    body.passed,
                    body.version,
                    body.resultCode,
                    body.resultMessage
            );
        } catch (HttpStatusCodeException e) {
            throw new ResponseStatusException(BAD_GATEWAY, "AI evaluation API call failed with status: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(GATEWAY_TIMEOUT, "AI evaluation API call timed out.", e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid AI server URL or evaluationId.", e);
        }
    }

    public record AiEvaluationResult(
            String evaluationId,
            Double precision,
            Double recall,
            Double f1Score,
            String status,
            Boolean passed,
            String version,
            Integer resultCode,
            String resultMessage
    ) {
    }

    @SuppressWarnings("unused")
    private static class AiEvaluationApiResponse {
        @JsonProperty("result_code")
        public Integer resultCode;

        @JsonProperty("result_msg")
        public String resultMessage;

        public String version;
        public String status;
        public Double precision;
        public Double recall;

        @JsonProperty("f1_score")
        public Double f1Score;

        public Boolean passed;
    }
}
