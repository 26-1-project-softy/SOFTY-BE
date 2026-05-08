package com.softy.be.admin.controller;

import com.softy.be.admin.dto.AdminPdfStatisticsData;
import com.softy.be.admin.dto.AdminRecommendationAdoptionData;
import com.softy.be.admin.dto.AdminRiskStatisticsData;
import com.softy.be.admin.dto.AdminTokenUsageData;
import com.softy.be.admin.service.AdminStatisticsService;
import com.softy.be.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
@Tag(name = "관리자 통계", description = "관리자 대시보드 통계 API")
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    @GetMapping("/pdfs")
    @Operation(
            summary = "PDF 생성 통계 조회",
            description = "생성된 리포트 PDF에 대한 집계 지표를 반환합니다."
    )
    public ResponseEntity<ApiResponse<AdminPdfStatisticsData>> getPdfStatistics() {
        AdminPdfStatisticsData data = adminStatisticsService.getPdfStatistics();

        ApiResponse<AdminPdfStatisticsData> response = ApiResponse.of(
                true,
                200,
                "PDF 조회에 성공했습니다.",
                data
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/risk")
    @Operation(
            summary = "위험도 통계 조회",
            description = "모니터링 데이터의 위험도 분포 및 관련 지표를 반환합니다."
    )
    public ResponseEntity<ApiResponse<AdminRiskStatisticsData>> getRiskStatistics() {
        AdminRiskStatisticsData data = adminStatisticsService.getRiskStatistics();

        ApiResponse<AdminRiskStatisticsData> response = ApiResponse.of(
                true,
                200,
                "리스크 건수 조회에 성공했습니다.",
                data
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recommendation-adoption")
    @Operation(
            summary = "추천문장 채택 통계 조회",
            description = "AI 추천문장에 대한 채택 지표를 반환합니다."
    )
    public ResponseEntity<ApiResponse<AdminRecommendationAdoptionData>> getRecommendationAdoptionStatistics() {
        AdminRecommendationAdoptionData data = adminStatisticsService.getRecommendationAdoptionStatistics();

        ApiResponse<AdminRecommendationAdoptionData> response = ApiResponse.of(
                true,
                200,
                "채택률 조회에 성공했습니다.",
                data
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/token-usage")
    @Operation(
            summary = "LLM 토큰 사용량 조회",
            description = "AI 서버의 엔드포인트별 토큰 사용량 집계 결과를 조회합니다."
    )
    public ResponseEntity<ApiResponse<AdminTokenUsageData>> getTokenUsage() {
        AdminTokenUsageData data = adminStatisticsService.getTokenUsage();

        ApiResponse<AdminTokenUsageData> response = ApiResponse.of(
                true,
                200,
                "토큰 사용량 조회에 성공했습니다.",
                data
        );
        return ResponseEntity.ok(response);
    }
}
