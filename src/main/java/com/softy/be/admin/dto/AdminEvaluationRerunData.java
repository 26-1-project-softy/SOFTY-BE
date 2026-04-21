package com.softy.be.admin.dto;

public record AdminEvaluationRerunData(
        String evaluationId,
        String status,
        Integer resultCode,
        String resultMessage,
        String contentType,
        String version,
        String datasetVersion
) {
}
