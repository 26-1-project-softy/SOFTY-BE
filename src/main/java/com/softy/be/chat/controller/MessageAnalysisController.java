package com.softy.be.chat.controller;

import com.softy.be.auth.security.AuthenticatedUserPrincipal;
import com.softy.be.chat.dto.TeacherMessageAnalyzeFeedbackRequest;
import com.softy.be.chat.dto.TeacherMessageAnalyzeData;
import com.softy.be.chat.dto.TeacherMessageAnalyzeRequest;
import com.softy.be.chat.service.ChatRoomService;
import com.softy.be.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teacher-message-analyses")
@RequiredArgsConstructor
@Tag(name = "교사 메시지 분석", description = "교사 메시지 분석 결과 관련 API")
public class MessageAnalysisController {

    private final ChatRoomService chatRoomService;

    @PutMapping("/{analysisId}/feedback")
    @Operation(
            summary = "교사 메시지 분석 피드백 저장",
            description = "교사가 본인 메시지 분석 결과에 대해 1점부터 5점까지 피드백을 남기거나 수정합니다."
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

    @PostMapping("/{analysisId}/recommendation-adoption")
    @Operation(
            summary = "교사 추천문장 적용 기록",
            description = "교사가 본인 메시지 분석 결과의 추천문장을 입력창에 적용했다는 행위를 기록합니다."
    )
    public ResponseEntity<ApiResponse<Void>> saveTeacherMessageRecommendationAdoption(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("analysisId") Long analysisId
    ) {
        chatRoomService.saveTeacherMessageRecommendationAdoption(principal.userId(), analysisId);

        ApiResponse<Void> response = ApiResponse.of(
                true,
                200,
                "추천문장 적용이 저장되었습니다.",
                null
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{analysisId}/recheck")
    @Operation(
            summary = "교사 수정 메시지 재분석",
            description = "교사가 추천문장을 수정한 뒤 최종 전송 전에 수정된 문장을 다시 AI로 분석합니다."
    )
    public ResponseEntity<ApiResponse<TeacherMessageAnalyzeData>> recheckTeacherMessage(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("analysisId") Long analysisId,
            @RequestBody TeacherMessageAnalyzeRequest request
    ) {
        TeacherMessageAnalyzeData data = chatRoomService.recheckTeacherMessage(principal.userId(), analysisId, request);

        ApiResponse<TeacherMessageAnalyzeData> response = ApiResponse.of(
                true,
                200,
                "메시지 재분석이 완료되었습니다.",
                data
        );

        return ResponseEntity.ok(response);
    }
}
