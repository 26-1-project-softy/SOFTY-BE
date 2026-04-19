package com.softy.be.auth.security;

public record AuthenticatedUserPrincipal(
        Long userId,
        String role,
        String name
) {
}

