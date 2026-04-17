package com.softy.be.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class KakaoOAuthClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String userInfoUri;
    private final String unlinkUri;
    private final String adminKey;

    public KakaoOAuthClient(
            @Value("${oauth.kakao.user-info-uri:https://kapi.kakao.com/v2/user/me}") String userInfoUri,
            @Value("${oauth.kakao.unlink-uri:https://kapi.kakao.com/v1/user/unlink}") String unlinkUri,
            @Value("${oauth.kakao.admin-key:}") String adminKey
    ) {
        this.userInfoUri = userInfoUri;
        this.unlinkUri = unlinkUri;
        this.adminKey = adminKey;
    }

    public KakaoUserProfile getUserProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<JsonNode> response = restTemplate.exchange(userInfoUri, HttpMethod.GET, requestEntity, JsonNode.class);

        JsonNode responseBody = response.getBody();
        if (responseBody == null || responseBody.path("id").isMissingNode()) {
            throw new IllegalStateException("카카오 사용자 정보를 가져오지 못했습니다.");
        }

        String providerUserId = responseBody.path("id").asText();
        String nickname = responseBody.path("properties").path("nickname").asText("kakao_" + providerUserId);

        return new KakaoUserProfile(providerUserId, nickname);
    }

    public void unlinkUserByAdminKey(String providerUserId) {
        if (providerUserId == null || providerUserId.trim().isEmpty()) {
            throw new IllegalArgumentException("카카오 providerUserId가 비어 있습니다.");
        }
        if (adminKey == null || adminKey.trim().isEmpty()) {
            throw new IllegalStateException("카카오 Admin Key가 설정되지 않았습니다.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.AUTHORIZATION, "KakaoAK " + adminKey.trim());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("target_id_type", "user_id");
        body.add("target_id", providerUserId.trim());

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        restTemplate.exchange(unlinkUri, HttpMethod.POST, requestEntity, JsonNode.class);
    }
}
