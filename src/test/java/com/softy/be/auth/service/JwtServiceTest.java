package com.softy.be.auth.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String ROLE_TEACHER = "TEACHER";

    private final JwtService jwtService = new JwtService(
            "test-jwt-secret-for-local-tests-only",
            3600L,
            7200L
    );

    @Test
    void extractAccessUserIdAcceptsAccessToken() {
        String accessToken = jwtService.createAccessToken(7L, "tester", ROLE_TEACHER);

        Long userId = jwtService.extractAccessUserId(accessToken);

        assertThat(userId).isEqualTo(7L);
        assertThat(jwtService.extractActiveRole(accessToken)).isEqualTo(ROLE_TEACHER);
    }

    @Test
    void extractAccessUserIdRejectsRefreshToken() {
        String refreshToken = jwtService.createRefreshToken(7L, ROLE_TEACHER);

        assertThatThrownBy(() -> jwtService.extractAccessUserId(refreshToken))
                .isInstanceOf(IllegalStateException.class);
    }
}
