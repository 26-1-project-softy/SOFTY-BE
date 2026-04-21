package com.softy.be.admin.dto;

public record AdminPerformanceStatisticsData(
        String evaluationId,
        double precision,
        double recall,
        double f1Score,
        String status,
        Boolean passed,
        String version,
        Integer resultCode,
        String resultMessage
) {
}
