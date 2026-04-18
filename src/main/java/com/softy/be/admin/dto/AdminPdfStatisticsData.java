package com.softy.be.admin.dto;

import java.util.List;

public record AdminPdfStatisticsData(
        long totalPdfCount,
        List<AdminTeacherPdfCountData> list
) {
}
