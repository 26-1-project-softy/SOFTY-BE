package com.softy.be.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.net.URI;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class TeacherMessageAnalysisClient {

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${ai.intent.base-url}")
    private String aiServerBaseUrl;

    public String detectRisk(String content) {
        URI uri = URI.create(aiServerBaseUrl + "/ai/inference/risk-detection");
        try {
            ResponseEntity<RiskDetectionResponse> response = buildRestTemplate().exchange(
                    uri,
                    HttpMethod.POST,
                    buildRequestEntity(content),
                    RiskDetectionResponse.class
            );

            RiskDetectionResponse body = response.getBody();
            if (body == null || isBlank(body.prediction())) {
                return null;
            }
            return body.prediction().trim();
        } catch (HttpStatusCodeException e) {
            log.warn("Risk detection failed with status: {}", e.getStatusCode(), e);
            return null;
        } catch (ResourceAccessException | IllegalArgumentException e) {
            log.warn("Risk detection call failed.", e);
            return null;
        }
    }

    public String recommendAlternative(String content) {
        URI uri = URI.create(aiServerBaseUrl + "/ai/inference/recommend-alternative");
        try {
            ResponseEntity<RecommendAlternativeResponse> response = buildRestTemplate().exchange(
                    uri,
                    HttpMethod.POST,
                    buildRequestEntity(content),
                    RecommendAlternativeResponse.class
            );

            RecommendAlternativeResponse body = response.getBody();
            if (body == null || isBlank(body.recommended_sentence())) {
                return null;
            }
            return body.recommended_sentence().trim();
        } catch (HttpStatusCodeException e) {
            log.warn("Recommend alternative failed with status: {}", e.getStatusCode(), e);
            return null;
        } catch (ResourceAccessException | IllegalArgumentException e) {
            log.warn("Recommend alternative call failed.", e);
            return null;
        }
    }

    private HttpEntity<ContentRequest> buildRequestEntity(String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(new ContentRequest(content), headers);
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
        return value == null || value.trim().isEmpty();
    }

    private record ContentRequest(
            String content
    ) {
    }

    private record RiskDetectionResponse(
            String content_type,
            Integer result_code,
            String result_msg,
            String prediction
    ) {
    }

    private record RecommendAlternativeResponse(
            String content_type,
            Integer result_code,
            String result_msg,
            String recommended_sentence
    ) {
    }
}
