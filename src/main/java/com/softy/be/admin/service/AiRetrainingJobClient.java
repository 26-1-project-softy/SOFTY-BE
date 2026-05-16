package com.softy.be.admin.service;

import com.softy.be.admin.service.dto.AiRetrainingJobApiRequest;
import com.softy.be.admin.service.dto.AiRetrainingJobApiResponse;
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

import java.net.URI;
import java.time.Duration;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;

@Component
@RequiredArgsConstructor
public class AiRetrainingJobClient {

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${ai.evaluation.base-url}")
    private String aiServerBaseUrl;

    public AiRetrainingJobResult requestRiskDetectionRetraining() {
        URI uri = URI.create(aiServerBaseUrl + "/ai/retraining-jobs/risk-detection");
        RestTemplate restTemplate = buildRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AiRetrainingJobApiRequest> requestEntity = new HttpEntity<>(new AiRetrainingJobApiRequest(), headers);

        try {
            ResponseEntity<AiRetrainingJobApiResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    requestEntity,
                    AiRetrainingJobApiResponse.class
            );

            AiRetrainingJobApiResponse body = response.getBody();
            if (body == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "AI 재학습 응답이 비어 있습니다.");
            }

            return new AiRetrainingJobResult(
                    nullToEmpty(body.jobId),
                    nullToEmpty(body.status),
                    body.progressPercent
            );
        } catch (HttpStatusCodeException e) {
            throw new ResponseStatusException(BAD_GATEWAY, "AI 재학습 API 호출에 실패했습니다. 상태코드: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(GATEWAY_TIMEOUT, "AI 재학습 API 호출이 시간 초과되었습니다.", e);
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record AiRetrainingJobResult(
            String jobId,
            String status,
            Integer progressPercent
    ) {
    }
}
