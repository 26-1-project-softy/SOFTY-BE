package com.softy.be.admin.dto;

import java.time.LocalDateTime;

public record AdminRiskFeedbackItemData(
        Long feedbackId,
        String teacherName,
        Integer feedbackResult,
        String riskLevel,
        String originalMessage,
        LocalDateTime createdAt
) {
}
