package com.softy.be.admin.controller;

import com.softy.be.admin.dto.AdminEmbeddingRunData;
import com.softy.be.admin.service.AdminEmbeddingService;
import com.softy.be.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/embedding")
@RequiredArgsConstructor
public class AdminEmbeddingController {

    private final AdminEmbeddingService adminEmbeddingService;

    @PostMapping("/run")
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