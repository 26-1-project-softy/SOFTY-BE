package com.softy.be.chat.dto;

import java.time.LocalDateTime;

public record ChatRoomReadData(
        Long chatRoomId,
        int unreadCount,
        LocalDateTime lastReadAt
) {
}
