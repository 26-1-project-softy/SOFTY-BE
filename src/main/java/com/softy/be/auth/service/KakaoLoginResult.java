package com.softy.be.auth.service;

public record KakaoLoginResult(
        String accessToken,
        String refreshToken,
        boolean registrationRequired
) {
}
