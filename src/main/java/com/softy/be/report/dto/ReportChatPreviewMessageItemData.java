package com.softy.be.report.dto;

import java.time.LocalDateTime;

public record ReportChatPreviewMessageItemData(
        Long messageId,
        Long senderId,
        String senderRole,
        boolean isMine,
        String type,
        String content,
        LocalDateTime createdAt
) {
}
