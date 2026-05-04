package com.softy.be.chat.controller;

import com.softy.be.auth.security.AuthenticatedUserPrincipal;
import com.softy.be.chat.dto.ChatRoomDetailData;
import com.softy.be.chat.dto.ChatRoomListData;
import com.softy.be.chat.dto.ChatRoomMessageListData;
import com.softy.be.chat.dto.ChatRoomReadData;
import com.softy.be.chat.dto.InitMessageIntentData;
import com.softy.be.chat.dto.InitMessageIntentRequest;
import com.softy.be.chat.dto.InitMessageSendData;
import com.softy.be.chat.dto.InitMessageSendRequest;
import com.softy.be.chat.dto.TeacherMessageAnalyzeData;
import com.softy.be.chat.dto.TeacherMessageAnalyzeRequest;
import com.softy.be.chat.dto.TeacherMessageSendData;
import com.softy.be.chat.dto.TeacherMessageSendRequest;
import com.softy.be.chat.dto.TeacherWorkingHoursStatusData;
import com.softy.be.chat.service.ChatRoomService;
import com.softy.be.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat-rooms")
@RequiredArgsConstructor
@Tag(name = "채팅방", description = "학부모 문의 채팅방 관련 API")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @GetMapping("/{chatRoomId}")
    @Operation(
            summary = "채팅방 상세 정보 조회",
            description = "로그인한 학부모 또는 교사가 참여 중인 특정 채팅방의 상세 정보를 조회합니다."
    )
    public ResponseEntity<ApiResponse<ChatRoomDetailData>> getChatRoomDetail(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("chatRoomId") Long chatRoomId
    ) {
        ChatRoomDetailData data = chatRoomService.getChatRoomDetail(principal.userId(), chatRoomId);

        ApiResponse<ChatRoomDetailData> response = ApiResponse.of(
                true,
                200,
                "채팅방 상세 정보 조회에 성공했습니다.",
                data
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{chatRoomId}/messages")
    @Operation(
            summary = "채팅방 메시지 목록 조회",
            description = "로그인한 학부모 또는 교사가 참여 중인 특정 채팅방의 메시지 목록을 커서 기반으로 조회합니다."
    )
    public ResponseEntity<ApiResponse<ChatRoomMessageListData>> getChatRoomMessages(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("chatRoomId") Long chatRoomId,
            @RequestParam(value = "cursor", required = false) Long cursor,
            @RequestParam(value = "size", defaultValue = "30") int size
    ) {
        ChatRoomMessageListData data = chatRoomService.getChatRoomMessages(principal.userId(), chatRoomId, cursor, size);

        ApiResponse<ChatRoomMessageListData> response = ApiResponse.of(
                true,
                200,
                "채팅 메시지 조회에 성공했습니다.",
                data
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{chatRoomId}/messages/analyze")
    @Operation(
            summary = "교사 메시지 분석",
            description = "교사가 전송 전 작성한 메시지를 AI가 분석하여 분쟁 가능성과 추천 문장을 반환합니다."
    )
    public ResponseEntity<ApiResponse<TeacherMessageAnalyzeData>> analyzeTeacherMessage(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("chatRoomId") Long chatRoomId,
            @RequestBody TeacherMessageAnalyzeRequest request
    ) {
        TeacherMessageAnalyzeData data = chatRoomService.analyzeTeacherMessage(principal.userId(), chatRoomId, request);

        ApiResponse<TeacherMessageAnalyzeData> response = ApiResponse.of(
                true,
                200,
                "메시지 분석이 완료되었습니다.",
                data
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{chatRoomId}/messages/teacher")
    @Operation(
            summary = "교사 최종 메시지 전송",
            description = "교사가 AI 분석 결과를 참고해 최종 메시지를 전송합니다."
    )
    public ResponseEntity<ApiResponse<TeacherMessageSendData>> sendTeacherMessage(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("chatRoomId") Long chatRoomId,
            @RequestBody TeacherMessageSendRequest request
    ) {
        TeacherMessageSendData data = chatRoomService.sendTeacherMessage(principal.userId(), chatRoomId, request);

        ApiResponse<TeacherMessageSendData> response = ApiResponse.of(
                true,
                201,
                "메시지가 전송되었습니다.",
                data
        );

        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/{chatRoomId}/read")
    @Operation(
            summary = "채팅방 읽음 처리",
            description = "로그인한 학부모 또는 교사가 특정 채팅방의 안 읽은 메시지를 모두 읽음 처리합니다."
    )
    public ResponseEntity<ApiResponse<ChatRoomReadData>> markChatRoomAsRead(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("chatRoomId") Long chatRoomId
    ) {
        ChatRoomReadData data = chatRoomService.markChatRoomAsRead(principal.userId(), chatRoomId);

        ApiResponse<ChatRoomReadData> response = ApiResponse.of(
                true,
                200,
                "채팅방 읽음 처리에 성공했습니다.",
                data
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "채팅방 목록 조회",
            description = "로그인한 학부모 또는 교사가 참여 중인 채팅방 목록을 페이지 단위로 조회합니다."
    )
    public ResponseEntity<ApiResponse<ChatRoomListData>> getChatRooms(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(value = "cursor", required = false) Long cursor,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        ChatRoomListData data = chatRoomService.getChatRooms(principal.userId(), cursor, size);

        ApiResponse<ChatRoomListData> response = ApiResponse.of(
                true,
                200,
                "채팅방 목록 조회에 성공했습니다.",
                data
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/init-messages")
    @Operation(
            summary = "초기 문의 의도 분석",
            description = "학부모가 작성 중인 첫 문의 메시지의 의도를 AI로 분석해 반환합니다. 채팅방이나 메시지는 저장하지 않습니다."
    )
    public ResponseEntity<ApiResponse<InitMessageIntentData>> analyzeInitMessageIntent(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestBody InitMessageIntentRequest request
    ) {
        InitMessageIntentData data = chatRoomService.analyzeInitMessageIntent(principal.userId(), request);

        String message = data.intentLabel() == null
                ? "의도 분석이 완료되었습니다."
                : "의도 분석에 성공했습니다.";

        ApiResponse<InitMessageIntentData> response = ApiResponse.of(
                true,
                200,
                message,
                data
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/working-hours")
    @Operation(
            summary = "교사 근무시간 상태 조회",
            description = "학부모에게 매핑된 교사가 현재 근무시간 이내인지 반환합니다."
    )
    public ResponseEntity<ApiResponse<TeacherWorkingHoursStatusData>> getTeacherWorkingHoursStatus(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        TeacherWorkingHoursStatusData data = chatRoomService.getTeacherWorkingHoursStatus(principal.userId());

        ApiResponse<TeacherWorkingHoursStatusData> response = ApiResponse.of(
                true,
                200,
                "교사 근무시간 정보 조회에 성공했습니다.",
                data
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/init-messages/send")
    @Operation(
            summary = "초기 문의 최종 전송",
            description = "학부모의 첫 문의를 최종 전송하고 채팅방과 첫 메시지를 생성합니다."
    )
    public ResponseEntity<ApiResponse<InitMessageSendData>> sendInitMessage(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestBody InitMessageSendRequest request
    ) {
        InitMessageSendData data = chatRoomService.sendInitMessage(principal.userId(), request);

        ApiResponse<InitMessageSendData> response = ApiResponse.of(
                true,
                201,
                "첫 메시지가 전송되었습니다.",
                data
        );

        return ResponseEntity.status(201).body(response);
    }
}
