package com.softy.be.chat.dto;

import java.time.LocalDateTime;

public record ChatRoomMessageSendData(
        Long messageId,
        Long roomId,
        String content,
        LocalDateTime createdAt
) {
}
