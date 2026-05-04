package com.softy.be.chat.dto;

public record TeacherMessageSendRequest(
        Long analysisId,
        String content
) {
}
