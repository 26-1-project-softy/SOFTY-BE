package com.softy.be.report.dto;

import java.util.List;

public record ReportChatRoomListData(
        List<ReportChatRoomItemData> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}

