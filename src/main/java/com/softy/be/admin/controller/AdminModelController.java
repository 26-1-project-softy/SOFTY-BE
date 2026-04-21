package com.softy.be.admin.controller;

import com.softy.be.admin.dto.AdminEvaluationRerunData;
import com.softy.be.admin.dto.AdminEvaluationRerunRequest;
import com.softy.be.admin.dto.AdminPerformanceStatisticsData;
import com.softy.be.admin.service.AdminStatisticsService;
import com.softy.be.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/models")
@RequiredArgsConstructor
public class AdminModelController {

    private final AdminStatisticsService adminStatisticsService;

    @GetMapping("/latest/evaluation")
    public ResponseEntity<ApiResponse<AdminPerformanceStatisticsData>> getLatestModelEvaluation(
            @RequestParam(value = "evaluationId", required = false) String evaluationId
    ) {
        AdminPerformanceStatisticsData data = adminStatisticsService.getPerformanceStatistics(evaluationId);

        ApiResponse<AdminPerformanceStatisticsData> response = ApiResponse.of(
                true,
                200,
                "성능 평가 조회에 성공했습니다.",
                data
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/latest/evaluation/re-run")
    public ResponseEntity<ApiResponse<AdminEvaluationRerunData>> rerunLatestModelEvaluation(
            @RequestBody(required = false) AdminEvaluationRerunRequest request
    ) {
        AdminEvaluationRerunData data = adminStatisticsService.requestEvaluationRerun(request);

        ApiResponse<AdminEvaluationRerunData> response = ApiResponse.of(
                true,
                200,
                "재평가 요청에 성공했습니다.",
                data
        );
        return ResponseEntity.ok(response);
    }
}
