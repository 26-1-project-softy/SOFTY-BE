package com.softy.be.report.dto;

import java.time.LocalDateTime;

public record ReportChatPreviewMessageItemData(
        Long messageId,
        boolean isMine,
        String content,
        LocalDateTime createdAt
) {
}
