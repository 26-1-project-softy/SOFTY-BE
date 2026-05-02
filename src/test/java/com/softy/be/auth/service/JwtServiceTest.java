package com.softy.be.auth.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-jwt-secret-for-local-tests-only",
            3600L,
            7200L
    );

    @Test
    void extractAccessUserIdAcceptsAccessToken() {
        String accessToken = jwtService.createAccessToken(7L, "tester", "TEACHER");

        Long userId = jwtService.extractAccessUserId(accessToken);

        assertThat(userId).isEqualTo(7L);
    }

    @Test
    void extractAccessUserIdRejectsRefreshToken() {
        String refreshToken = jwtService.createRefreshToken(7L, "TEACHER");

        assertThatThrownBy(() -> jwtService.extractAccessUserId(refreshToken))
                .isInstanceOf(IllegalStateException.class);
    }
}
