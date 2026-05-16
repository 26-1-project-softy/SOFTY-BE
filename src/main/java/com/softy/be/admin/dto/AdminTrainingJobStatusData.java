package com.softy.be.admin.dto;

public record AdminTrainingJobStatusData(
        String jobId,
        String modelName,
        String modelVersion,
        String datasetVersion,
        String status,
        Integer progressPercent,
        String startedAt,
        String finishedAt
) {
}
