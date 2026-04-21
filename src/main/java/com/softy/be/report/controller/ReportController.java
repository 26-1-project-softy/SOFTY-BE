package com.softy.be.report.controller;

import com.softy.be.auth.security.AuthenticatedUserPrincipal;
import com.softy.be.common.api.ApiResponse;
import com.softy.be.report.dto.ReportChatPreviewData;
import com.softy.be.report.dto.ReportChatRoomListData;
import com.softy.be.report.dto.ReportPdfCreateData;
import com.softy.be.report.service.ReportService;
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
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/chat-rooms")
    public ResponseEntity<ApiResponse<ReportChatRoomListData>> getChatRooms(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Long userId = principal.userId();
        ReportChatRoomListData data = reportService.getChatRoomsForReport(userId, page, size);

        ApiResponse<ReportChatRoomListData> response = ApiResponse.of(
                true,
                200,
                "Chat room list retrieved successfully.",
                data
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/chat-rooms/{chatRoomId}/preview")
    public ResponseEntity<ApiResponse<ReportChatPreviewData>> getChatPreview(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("chatRoomId") Long chatRoomId,
            @RequestParam(value = "cursor", required = false) Long cursor,
            @RequestParam(value = "size", defaultValue = "30") int size
    ) {
        Long userId = principal.userId();
        ReportChatPreviewData data = reportService.getChatPreview(userId, chatRoomId, cursor, size);

        ApiResponse<ReportChatPreviewData> response = ApiResponse.of(
                true,
                200,
                "Chat preview retrieved successfully.",
                data
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat-rooms/{chatRoomId}/pdfs")
    public ResponseEntity<ApiResponse<ReportPdfCreateData>> createPdf(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("chatRoomId") Long chatRoomId
    ) {
        Long userId = principal.userId();
        ReportPdfCreateData data = reportService.createPdf(userId, chatRoomId);

        ApiResponse<ReportPdfCreateData> response = ApiResponse.of(
                true,
                201,
                "PDF 리포트가 생성되었습니다.",
                data
        );
        return ResponseEntity.status(201).body(response);
    }
}
