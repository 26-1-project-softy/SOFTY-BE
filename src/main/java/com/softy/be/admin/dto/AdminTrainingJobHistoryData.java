package com.softy.be.admin.dto;

import java.util.List;

public record AdminTrainingJobHistoryData(
        List<AdminTrainingJobHistoryItemData> items,
        int page,
        int size,
        int totalCount,
        int totalPages
) {
}
