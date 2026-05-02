package com.softy.be.chat.dto;

import java.time.LocalDateTime;

public record ChatRoomListItemData(
        Long chatRoomId,
        String counterpartName,
        String studentName,
        String lastMessage,
        LocalDateTime lastMessageAt,
        int unreadCount,
        String status,
        String intentLabel
) {
}
