package com.softy.be.admin.service;

import com.softy.be.admin.service.dto.AiTokenUsageApiResponse;
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
public class AiTokenUsageClient {

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${ai.evaluation.base-url}")
    private String aiServerBaseUrl;

    public AiTokenUsageResult getTokenUsage() {
        URI uri = UriComponentsBuilder
                .fromUriString(aiServerBaseUrl)
                .path("/ai/token-usage")
                .build(true)
                .toUri();

        RestTemplate restTemplate = buildRestTemplate();

        try {
            ResponseEntity<AiTokenUsageApiResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    null,
                    AiTokenUsageApiResponse.class
            );

            AiTokenUsageApiResponse body = response.getBody();
            if (body == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "AI 토큰 사용량 응답이 비어 있습니다.");
            }
            if (body.totalUsage == null) {
                throw new ResponseStatusException(BAD_GATEWAY, "AI 토큰 사용량 합계 정보가 없습니다.");
            }

            List<AiTokenUsageDetailResult> details = body.details == null
                    ? List.of()
                    : body.details.stream()
                    .map(detail -> new AiTokenUsageDetailResult(
                            nullToEmpty(detail.endpoint),
                            nullToZero(detail.inputTokens),
                            nullToZero(detail.outputTokens),
                            nullToZero(detail.totalTokens)
                    ))
                    .toList();

            return new AiTokenUsageResult(
                    nullToEmpty(body.contentType),
                    body.resultCode,
                    nullToEmpty(body.resultMessage),
                    new AiTokenUsageSummaryResult(
                            nullToZero(body.totalUsage.inputTokens),
                            nullToZero(body.totalUsage.outputTokens),
                            nullToZero(body.totalUsage.totalTokens)
                    ),
                    details
            );
        } catch (HttpStatusCodeException e) {
            throw new ResponseStatusException(BAD_GATEWAY, "AI 토큰 사용량 API 호출에 실패했습니다. 상태코드: " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(GATEWAY_TIMEOUT, "AI 토큰 사용량 API 호출이 시간 초과되었습니다.", e);
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

    public record AiTokenUsageResult(
            String contentType,
            Integer resultCode,
            String resultMessage,
            AiTokenUsageSummaryResult totalUsage,
            List<AiTokenUsageDetailResult> details
    ) {
    }

    public record AiTokenUsageSummaryResult(
            int inputTokens,
            int outputTokens,
            int totalTokens
    ) {
    }

    public record AiTokenUsageDetailResult(
            String endpoint,
            int inputTokens,
            int outputTokens,
            int totalTokens
    ) {
    }
}
