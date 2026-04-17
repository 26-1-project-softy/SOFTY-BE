package com.softy.be.report.controller;

import com.softy.be.auth.service.TokenAuthService;
import com.softy.be.common.api.ApiResponse;
import com.softy.be.report.dto.ReportChatRoomListData;
import com.softy.be.report.dto.ReportPdfCreateData;
import com.softy.be.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final TokenAuthService tokenAuthService;
    private final ReportService reportService;

    @GetMapping("/chat-rooms")
    public ResponseEntity<ApiResponse<ReportChatRoomListData>> getChatRooms(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        Long userId = tokenAuthService.extractUserIdFromAuthorization(authorization);
        ReportChatRoomListData data = reportService.getChatRoomsForReport(userId, page, size);

        ApiResponse<ReportChatRoomListData> response = ApiResponse.of(
                true,
                200,
                "채팅방 목록 조회에 성공했습니다.",
                data
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat-rooms/{chatRoomId}/pdfs")
    public ResponseEntity<ApiResponse<ReportPdfCreateData>> createPdf(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("chatRoomId") Long chatRoomId
    ) {
        Long userId = tokenAuthService.extractUserIdFromAuthorization(authorization);
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
