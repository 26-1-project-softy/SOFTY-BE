package com.softy.be.chat.controller;

import com.softy.be.auth.security.AuthenticatedUserPrincipal;
import com.softy.be.chat.dto.TeacherMessageAnalyzeFeedbackRequest;
import com.softy.be.chat.service.ChatRoomService;
import com.softy.be.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/message-analyses")
@RequiredArgsConstructor
@Tag(name = "메시지 분석", description = "교사 메시지 분석 결과 관련 API")
public class MessageAnalysisController {

    private final ChatRoomService chatRoomService;

    @PutMapping("/{analysisId}/feedback")
    @Operation(
            summary = "교사 메시지 분석 피드백 저장",
            description = "교사가 본인 메시지 분석 결과에 대해 1점부터 5점까지 피드백을 저장하거나 수정합니다."
    )
    public ResponseEntity<ApiResponse<Void>> saveTeacherMessageAnalyzeFeedback(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("analysisId") Long analysisId,
            @RequestBody TeacherMessageAnalyzeFeedbackRequest request
    ) {
        chatRoomService.saveTeacherMessageAnalyzeFeedback(principal.userId(), analysisId, request);

        ApiResponse<Void> response = ApiResponse.of(
                true,
                200,
                "피드백이 저장되었습니다.",
                null
        );

        return ResponseEntity.ok(response);
    }
}
