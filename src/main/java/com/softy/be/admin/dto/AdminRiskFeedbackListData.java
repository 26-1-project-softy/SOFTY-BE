package com.softy.be.admin.dto;

import java.util.List;

public record AdminRiskFeedbackListData(
        List<AdminRiskFeedbackItemData> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
