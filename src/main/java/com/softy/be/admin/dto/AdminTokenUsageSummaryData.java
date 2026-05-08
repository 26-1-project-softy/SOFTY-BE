package com.softy.be.admin.dto;

public record AdminTokenUsageSummaryData(
        int inputTokens,
        int outputTokens,
        int totalTokens
) {
}
