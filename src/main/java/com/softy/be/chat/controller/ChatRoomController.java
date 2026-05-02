package com.softy.be.chat.controller;

import com.softy.be.auth.security.AuthenticatedUserPrincipal;
import com.softy.be.chat.dto.InitMessageIntentData;
import com.softy.be.chat.dto.InitMessageIntentRequest;
import com.softy.be.chat.service.ChatRoomService;
import com.softy.be.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat-rooms")
@RequiredArgsConstructor
@Tag(name = "채팅방", description = "학부모 문의 채팅방 관련 API")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

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
                ? "의도 분석을 완료했습니다."
                : "의도 분석에 성공했습니다.";

        ApiResponse<InitMessageIntentData> response = ApiResponse.of(
                true,
                200,
                message,
                data
        );

        return ResponseEntity.ok(response);
    }
}
