package com.softy.be.admin.dto;

import java.util.List;

public record AdminTokenUsageData(
        AdminTokenUsageSummaryData totalUsage,
        List<AdminTokenUsageDetailData> details
) {
}
