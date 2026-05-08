package com.softy.be.chat.dto;

import com.softy.be.chat.entity.ChatRoomStatus;

public record ChatRoomStatusUpdateRequest(
        ChatRoomStatus status
) {
}
