package com.softy.be.chat.dto;

public record ChatRoomStatusUpdateData(
        Long chatRoomId,
        String status
) {
}
