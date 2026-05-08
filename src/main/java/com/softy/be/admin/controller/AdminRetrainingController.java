package com.softy.be.admin.controller;

import com.softy.be.admin.dto.AdminRetrainingJobData;
import com.softy.be.admin.service.AdminModelService;
import com.softy.be.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/retraining")
@RequiredArgsConstructor
@Tag(name = "관리자 재학습", description = "관리자 모델 재학습 API")
public class AdminRetrainingController {

    private final AdminModelService adminModelService;

    @PostMapping
    @Operation(
            summary = "재학습 요청",
            description = "AI 서버에 위험 탐지 모델 재학습 작업 생성을 요청합니다."
    )
    public ResponseEntity<ApiResponse<AdminRetrainingJobData>> requestRetraining() {
        AdminRetrainingJobData data = adminModelService.requestRiskDetectionRetraining();

        ApiResponse<AdminRetrainingJobData> response = ApiResponse.of(
                true,
                200,
                "재학습 요청에 성공했습니다.",
                data
        );
        return ResponseEntity.ok(response);
    }
}
