package com.softy.be.admin.dto;

import java.time.LocalDateTime;

public record AdminEmbeddingRunData(
        String trigger,
        int totalCandidates,
        int successCount,
        int failedCount,
        int skippedCount,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
