package com.softy.be.admin.controller;

import com.softy.be.admin.dto.AdminRiskFeedbackListData;
import com.softy.be.admin.service.AdminStatisticsService;
import com.softy.be.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/risk-feedbacks")
@RequiredArgsConstructor
@Tag(name = "관리자 오류 검토", description = "관리자 오류 검토 조회 API")
public class AdminRiskFeedbackController {

    private final AdminStatisticsService adminStatisticsService;

    @GetMapping
    @Operation(
            summary = "리스크 피드백 목록 조회",
            description = "관리자 화면에서 교사 메시지 분석 피드백 목록을 페이지 단위로 조회합니다."
    )
    public ResponseEntity<ApiResponse<AdminRiskFeedbackListData>> getRiskFeedbacks(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        AdminRiskFeedbackListData data = adminStatisticsService.getRiskFeedbacks(page, size);

        ApiResponse<AdminRiskFeedbackListData> response = ApiResponse.of(
                true,
                200,
                "피드백 목록 조회에 성공했습니다.",
                data
        );
        return ResponseEntity.ok(response);
    }
}
