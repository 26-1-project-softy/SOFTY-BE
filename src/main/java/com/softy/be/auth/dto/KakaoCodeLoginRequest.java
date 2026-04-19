package com.softy.be.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoCodeLoginRequest(
        @JsonProperty("code")
        String authorizationCode,
        String redirectUri
) {
}
