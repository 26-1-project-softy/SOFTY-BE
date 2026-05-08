package com.softy.be.admin.service;

import com.softy.be.admin.service.dto.AiTrainingHistoryApiResponse;
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
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;

@Component
@RequiredArgsConstructor
public class AiTrainingHistoryClient {

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${ai.evaluation.base-url}")
    private String aiServerBaseUrl;

    public AiTrainingHistoryResult getTrainingHistory(int page, int size) {
        URI uri = UriComponentsBuilder
                .fromUriString(aiServerBaseUrl)
                .path("/ai/training-history")
                .queryParam("page", page)
                .queryParam("page_size", size)
                .build(true)
                .toUri();

        RestTemplate restTemplate = buildRestTemplate();

        try {
            ResponseEntity<AiTrainingHistoryApiResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    null,
                    AiTrainingHistoryApiResponse.class
            );

            AiTrainingHistoryApiResponse body = response.getBody();
            if (body == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "AI 학습 이력 응답이 비어 있습니다.");
            }
            if (body.pagination == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "AI 학습 이력 페이지 정보가 없습니다.");
            }

            List<AiTrainingHistoryItemResult> items = body.data == null
                    ? List.of()
                    : body.data.stream()
                    .map(item -> new AiTrainingHistoryItemResult(
                            nullToEmpty(item.trainingDate),
                            nullToEmpty(item.version),
                            nullToEmpty(item.datasetVersion),
                            item.f1Score,
                            nullToEmpty(item.status)
                    ))
                    .toList();

            return new AiTrainingHistoryResult(
                    items,
                    nullToZero(body.pagination.page),
                    nullToZero(body.pagination.pageSize),
                    nullToZero(body.pagination.totalCount),
                    nullToZero(body.pagination.totalPages)
            );
        } catch (HttpStatusCodeException e) {
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

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record AiTrainingHistoryResult(
            List<AiTrainingHistoryItemResult> items,
            int page,
            int size,
            int totalCount,
            int totalPages
    ) {
    }

    public record AiTrainingHistoryItemResult(
            String trainedAt,
            String version,
            String datasetVersion,
            Double f1Score,
            String status
    ) {
    }
}
