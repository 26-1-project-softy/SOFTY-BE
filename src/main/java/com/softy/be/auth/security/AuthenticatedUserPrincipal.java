package com.softy.be.auth.security;

public record AuthenticatedUserPrincipal(
        Long userId,
        String activeRole,
        String name
) {
}

