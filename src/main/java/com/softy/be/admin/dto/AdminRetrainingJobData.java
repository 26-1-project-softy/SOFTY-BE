package com.softy.be.admin.dto;

public record AdminRetrainingJobData(
        String jobId,
        String status,
        Integer progressPercent
) {
}
