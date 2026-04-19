package com.softy.be.admin.dto;

public record AdminRecommendationAdoptionData(
        double adoptionRate,
        long totalUsedAsIs,
        long totalModified,
        long totalNotUsed
) {
}

