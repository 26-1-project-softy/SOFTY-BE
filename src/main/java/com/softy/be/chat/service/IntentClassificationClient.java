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
public class IntentClassificationClient {

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${ai.intent.base-url}")
    private String aiServerBaseUrl;

    public String classifyIntent(String content) {
        RestTemplate restTemplate = buildRestTemplate();
        URI uri = URI.create(aiServerBaseUrl + "/ai/inference/classify-intent");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<IntentClassificationRequest> requestEntity = new HttpEntity<>(
                new IntentClassificationRequest(content),
                headers
        );

        try {
            ResponseEntity<IntentClassificationResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    requestEntity,
                    IntentClassificationResponse.class
            );

            IntentClassificationResponse body = response.getBody();
            if (body == null || isBlank(body.intent())) {
                return null;
            }

            return body.intent().trim();
        } catch (HttpStatusCodeException e) {
            log.warn("Intent classification failed with status: {}", e.getStatusCode(), e);
            return null;
        } catch (ResourceAccessException e) {
            log.warn("Intent classification timed out or was unreachable.", e);
            return null;
        } catch (IllegalArgumentException e) {
            log.warn("Intent classification URL is invalid.", e);
            return null;
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
        return value == null || value.trim().isEmpty();
    }

    private record IntentClassificationRequest(
            String content
    ) {
    }

    private record IntentClassificationResponse(
            String content_type,
            Integer result_code,
            String result_msg,
            String intent
    ) {
    }
}
