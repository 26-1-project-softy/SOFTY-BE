package com.softy.be.admin.service;

import com.softy.be.admin.service.dto.AiTrainingJobApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
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

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Component
@RequiredArgsConstructor
public class AiTrainingJobClient {

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${ai.evaluation.base-url}")
    private String aiServerBaseUrl;

    public AiTrainingJobResult getLatestTrainingJob() {
        URI uri = UriComponentsBuilder
                .fromUriString(aiServerBaseUrl)
                .path("/ai/training-jobs")
                .build(true)
                .toUri();

        RestTemplate restTemplate = buildRestTemplate();

        try {
            ResponseEntity<AiTrainingJobApiResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    null,
                    AiTrainingJobApiResponse.class
            );

            AiTrainingJobApiResponse body = response.getBody();
            if (body == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "AI 학습 이력 응답이 비어 있습니다.");
            }
            if (isBlank(body.jobId)) {
                throw new ResponseStatusException(NOT_FOUND, "최신 모델 정보를 찾을 수 없습니다.");
            }

            return new AiTrainingJobResult(
                    body.jobId,
                    body.modelName,
                    body.version,
                    body.datasetVersion,
                    body.status,
                    body.startedAt,
                    body.finishedAt,
                    body.resultCode,
                    body.resultMessage
            );
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 404) {
                throw new ResponseStatusException(NOT_FOUND, "최신 모델 정보를 찾을 수 없습니다.", e);
            }
            throw new ResponseStatusException(BAD_GATEWAY, "AI 학습 이력 API 호출에 실패했습니다. 상태코드: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(GATEWAY_TIMEOUT, "AI 학습 이력 API 호출이 시간 초과되었습니다.", e);
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record AiTrainingJobResult(
            String jobId,
            String modelName,
            String modelVersion,
            String datasetVersion,
            String status,
            String startedAt,
            String finishedAt,
            Integer resultCode,
            String resultMessage
    ) {
    }
}
