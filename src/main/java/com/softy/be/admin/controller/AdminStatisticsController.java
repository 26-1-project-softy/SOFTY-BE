package com.softy.be.admin.controller;

import com.softy.be.admin.dto.AdminPdfStatisticsData;
import com.softy.be.admin.dto.AdminRecommendationAdoptionData;
import com.softy.be.admin.dto.AdminRiskStatisticsData;
import com.softy.be.admin.service.AdminStatisticsService;
import com.softy.be.auth.service.TokenAuthService;
import com.softy.be.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final TokenAuthService tokenAuthService;
    private final AdminStatisticsService adminStatisticsService;

    @GetMapping("/pdfs")
    public ResponseEntity<ApiResponse<AdminPdfStatisticsData>> getPdfStatistics(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        Long userId = tokenAuthService.extractUserIdFromAuthorization(authorization);
        AdminPdfStatisticsData data = adminStatisticsService.getPdfStatistics(userId);

        ApiResponse<AdminPdfStatisticsData> response = ApiResponse.of(
                true,
                200,
                "PDF 조회에 성공했습니다.",
                data
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/risk")
    public ResponseEntity<ApiResponse<AdminRiskStatisticsData>> getRiskStatistics(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        Long userId = tokenAuthService.extractUserIdFromAuthorization(authorization);
        AdminRiskStatisticsData data = adminStatisticsService.getRiskStatistics(userId);

        ApiResponse<AdminRiskStatisticsData> response = ApiResponse.of(
                true,
                200,
                "리스크 건수 조회에 성공했습니다.",
                data
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recommendation-adoption")
    public ResponseEntity<ApiResponse<AdminRecommendationAdoptionData>> getRecommendationAdoptionStatistics(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        Long userId = tokenAuthService.extractUserIdFromAuthorization(authorization);
        AdminRecommendationAdoptionData data = adminStatisticsService.getRecommendationAdoptionStatistics(userId);

        ApiResponse<AdminRecommendationAdoptionData> response = ApiResponse.of(
                true,
                200,
                "채택률 조회에 성공했습니다.",
                data
        );
        return ResponseEntity.ok(response);
    }
}
