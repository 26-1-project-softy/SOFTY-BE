package com.softy.be.admin.controller;

import com.softy.be.admin.dto.AdminTrainingJobHistoryData;
import com.softy.be.admin.service.AdminModelService;
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
@RequestMapping("/admin/training-jobs")
@RequiredArgsConstructor
@Tag(name = "관리자 학습 이력", description = "관리자 모델 학습 이력 API")
public class AdminTrainingJobController {

    private final AdminModelService adminModelService;

    @GetMapping
    @Operation(
            summary = "학습 이력 조회",
            description = "AI 서버의 모델 학습 이력 목록을 페이지 단위로 조회합니다."
    )
    public ResponseEntity<ApiResponse<AdminTrainingJobHistoryData>> getTrainingHistory(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        AdminTrainingJobHistoryData data = adminModelService.getTrainingHistory(page, size);

        ApiResponse<AdminTrainingJobHistoryData> response = ApiResponse.of(
                true,
                200,
                "학습 이력 조회에 성공했습니다.",
                data
        );
        return ResponseEntity.ok(response);
    }
}
