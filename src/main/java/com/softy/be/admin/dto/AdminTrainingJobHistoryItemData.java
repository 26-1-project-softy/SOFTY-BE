package com.softy.be.admin.dto;

public record AdminTrainingJobHistoryItemData(
        String trainedAt,
        String version,
        String datasetVersion,
        Double f1Score,
        String status
) {
}
