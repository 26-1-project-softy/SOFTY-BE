package com.softy.be.chat.dto;

import java.time.LocalDateTime;

public record ChatRoomMessageItemData(
        Long messageId,
        boolean isMine,
        String senderName,
        String senderRole,
        String content,
        LocalDateTime createdAt
) {
}
