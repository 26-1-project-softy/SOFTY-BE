package com.softy.be.user.service;

public record UserMeResult(
        String activeRole,
        String name,
        Integer grade,
        Integer classNumber
) {
}
