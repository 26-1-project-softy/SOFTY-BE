package com.softy.be.auth.dto;

public record KakaoLoginData(
        String accessToken,
        String refreshToken
) {
}
