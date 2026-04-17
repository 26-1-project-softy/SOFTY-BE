package com.softy.be.report.dto;

import java.time.LocalDateTime;

public record ReportChatRoomItemData(
        Long chatRoomId,
        String parentName,
        String studentName,
        String intentLabel,
        String status,
        LocalDateTime lastMessageAt
) {
}
