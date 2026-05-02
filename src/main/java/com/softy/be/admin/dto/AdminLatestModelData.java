package com.softy.be.admin.dto;

public record AdminLatestModelData(
        String jobId,
        String modelName,
        String modelVersion,
        String datasetVersion,
        String status,
        String lastTrainedAt
) {
}
