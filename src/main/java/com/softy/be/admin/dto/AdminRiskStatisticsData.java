package com.softy.be.admin.dto;

public record AdminRiskStatisticsData(
        long totalMessageCount,
        long detectedConflictCount,
        double conflictDetectionRate
) {
}

