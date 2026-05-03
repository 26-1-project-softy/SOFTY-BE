package com.softy.be.chat.dto;

public record TeacherMessageAnalyzeData(
        Long analysisId,
        String riskLevel,
        String recommendedMessage
) {
}
