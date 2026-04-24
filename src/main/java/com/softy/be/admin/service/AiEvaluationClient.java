package com.softy.be.admin.service;

import com.softy.be.admin.service.dto.AiEvaluationApiResponse;
import com.softy.be.admin.service.dto.AiEvaluationRerunApiRequest;
import com.softy.be.admin.service.dto.AiEvaluationRerunApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

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
        String normalizedEvaluationId = normalizeOptionalValue(evaluationId);
        URI uri = buildEvaluationUri(normalizedEvaluationId);

        RestTemplate restTemplate = buildRestTemplate();

        try {
            ResponseEntity<AiEvaluationApiResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    null,
                    AiEvaluationApiResponse.class
            );

            AiEvaluationApiResponse body = response.getBody();
            if (body == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "AI 평가 응답이 비어 있습니다.");
            }

            return new AiEvaluationResult(
                    resolveResponseEvaluationId(normalizedEvaluationId, body.evaluationId),
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
            throw new ResponseStatusException(BAD_GATEWAY, "AI 평가 API 호출에 실패했습니다. 상태코드: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(GATEWAY_TIMEOUT, "AI 평가 API 호출이 시간 초과되었습니다.", e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, "AI 서버 URL 또는 evaluationId가 올바르지 않습니다.", e);
        }
    }

    public AiEvaluationRerunResult requestRiskDetectionEvaluation(String version, String datasetVersion) {
        String normalizedVersion = normalizeOptionalValue(version);
        String normalizedDatasetVersion = normalizeOptionalValue(datasetVersion);

        URI uri = URI.create(aiServerBaseUrl + "/ai/evaluations/risk-detection");
        RestTemplate restTemplate = buildRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        AiEvaluationRerunApiRequest requestBody = new AiEvaluationRerunApiRequest(
                normalizedVersion,
                normalizedDatasetVersion
        );
        HttpEntity<AiEvaluationRerunApiRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<AiEvaluationRerunApiResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    requestEntity,
                    AiEvaluationRerunApiResponse.class
            );

            AiEvaluationRerunApiResponse body = response.getBody();
            if (body == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "AI 재평가 응답이 비어 있습니다.");
            }

            return new AiEvaluationRerunResult(
                    body.evaluationId,
                    body.status,
                    body.resultCode,
                    body.resultMessage,
                    body.contentType
            );
        } catch (HttpStatusCodeException e) {
            throw new ResponseStatusException(BAD_GATEWAY, "AI 재평가 API 호출에 실패했습니다. 상태코드: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(GATEWAY_TIMEOUT, "AI 재평가 API 호출이 시간 초과되었습니다.", e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, "AI 서버 URL이 올바르지 않습니다.", e);
        }
    }

    private RestTemplate buildRestTemplate() {
        return restTemplateBuilder
                .requestFactory(() -> {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
                    factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
                    return factory;
                })
                .build();
    }

    private String normalizeOptionalValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private URI buildEvaluationUri(String evaluationId) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(aiServerBaseUrl)
                .path("/ai/evaluations");

        if (evaluationId != null) {
            builder.queryParam("evaluation_id", evaluationId);
        }

        return builder.build(true).toUri();
    }

    private String resolveResponseEvaluationId(String requestEvaluationId, String responseEvaluationId) {
        if (responseEvaluationId != null && !responseEvaluationId.isBlank()) {
            return responseEvaluationId;
        }
        return requestEvaluationId == null ? "" : requestEvaluationId;
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

    public record AiEvaluationRerunResult(
            String evaluationId,
            String status,
            Integer resultCode,
            String resultMessage,
            String contentType
    ) {
    }
}
