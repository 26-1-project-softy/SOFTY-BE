package com.softy.be.admin.dto;

public record AdminTokenUsageDetailData(
        String modelName,
        int inputTokens,
        int outputTokens,
        int totalTokens
) {
}
