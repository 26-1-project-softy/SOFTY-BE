package com.softy.be.admin.controller;

import com.softy.be.admin.dto.AdminEmbeddingRunData;
import com.softy.be.admin.service.AdminEmbeddingService;
import com.softy.be.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/embedding")
@RequiredArgsConstructor
@Tag(name = "관리자 임베딩", description = "관리자 임베딩 배치 관리 API")
public class AdminEmbeddingController {

    private final AdminEmbeddingService adminEmbeddingService;

    @PostMapping("/run")
    @Operation(
            summary = "임베딩 작업 수동 실행",
            description = "임베딩 배치 프로세스를 즉시 실행하고 실행 결과 정보를 반환합니다."
    )
    public ResponseEntity<ApiResponse<AdminEmbeddingRunData>> runEmbedding() {
        AdminEmbeddingRunData data = adminEmbeddingService.runManually();

        ApiResponse<AdminEmbeddingRunData> response = ApiResponse.of(
                true,
                200,
                "임베딩 배치 실행이 완료되었습니다.",
                data
        );
        return ResponseEntity.ok(response);
    }
}
