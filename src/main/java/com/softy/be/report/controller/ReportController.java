package com.softy.be.report.controller;

import com.softy.be.auth.security.AuthenticatedUserPrincipal;
import com.softy.be.common.api.ApiResponse;
import com.softy.be.report.dto.ReportChatPreviewData;
import com.softy.be.report.dto.ReportChatRoomListData;
import com.softy.be.report.dto.ReportPdfCreateData;
import com.softy.be.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "리포트", description = "리포트 생성 및 미리보기 API")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/chat-rooms")
    @Operation(
            summary = "리포트용 채팅방 목록 조회",
            description = "리포트 생성에 사용할 수 있는 채팅방 목록을 페이지 단위로 반환합니다."
    )
    public ResponseEntity<ApiResponse<ReportChatRoomListData>> getChatRooms(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Long userId = principal.userId();
        ReportChatRoomListData data = reportService.getChatRoomsForReport(userId, principal.activeRole(), page, size);

        ApiResponse<ReportChatRoomListData> response = ApiResponse.of(
                true,
                200,
                "채팅방 목록 조회에 성공했습니다.",
                data
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/chat-rooms/{chatRoomId}/preview")
    @Operation(
            summary = "채팅 미리보기 조회",
            description = "리포트 검토를 위해 채팅방 메시지 미리보기를 커서 기반으로 반환합니다."
    )
    public ResponseEntity<ApiResponse<ReportChatPreviewData>> getChatPreview(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("chatRoomId") Long chatRoomId,
            @RequestParam(value = "cursor", required = false) Long cursor,
            @RequestParam(value = "size", defaultValue = "30") int size
    ) {
        Long userId = principal.userId();
        ReportChatPreviewData data = reportService.getChatPreview(userId, principal.activeRole(), chatRoomId, cursor, size);

        ApiResponse<ReportChatPreviewData> response = ApiResponse.of(
                true,
                200,
                "채팅 미리보기 조회에 성공했습니다.",
                data
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat-rooms/{chatRoomId}/pdfs")
    @Operation(
            summary = "리포트 PDF 생성",
            description = "채팅방 리포트 PDF를 생성하고 다운로드 가능한 메타데이터를 반환합니다."
    )
    public ResponseEntity<ApiResponse<ReportPdfCreateData>> createPdf(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("chatRoomId") Long chatRoomId
    ) {
        Long userId = principal.userId();
        ReportPdfCreateData data = reportService.createPdf(userId, principal.activeRole(), chatRoomId);

        ApiResponse<ReportPdfCreateData> response = ApiResponse.of(
                true,
                201,
                "PDF 리포트가 생성되었습니다.",
                data
        );
        return ResponseEntity.status(201).body(response);
    }
}
