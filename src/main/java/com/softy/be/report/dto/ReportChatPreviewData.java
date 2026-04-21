package com.softy.be.report.dto;

import java.util.List;

public record ReportChatPreviewData(
        Long chatRoomId,
        String parentName,
        String studentName,
        String intentLabel,
        String status,
        List<ReportChatPreviewMessageItemData> messages,
        Long nextCursor,
        boolean hasNext
) {
}
