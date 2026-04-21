package com.softy.be.report.dto;

import java.util.List;

public record ReportChatPreviewData(
        Long chatRoomId,
        List<ReportChatPreviewMessageItemData> messages,
        Long nextCursor,
        boolean hasNext
) {
}
