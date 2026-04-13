package com.softy.be.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KakaoOAuthClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String userInfoUri;

    public KakaoOAuthClient(
            @Value("${oauth.kakao.user-info-uri:https://kapi.kakao.com/v2/user/me}") String userInfoUri
    ) {
        this.userInfoUri = userInfoUri;
    }

    public KakaoUserProfile getUserProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(userInfoUri, HttpMethod.GET, requestEntity, JsonNode.class);

        JsonNode responseBody = response.getBody();
        if (responseBody == null || responseBody.path("id").isMissingNode()) {
            throw new IllegalStateException("Failed to fetch Kakao user profile.");
        }

        String providerUserId = responseBody.path("id").asText();
        String nickname = responseBody.path("properties").path("nickname").asText("kakao_" + providerUserId);

        return new KakaoUserProfile(providerUserId, nickname);
    }
}