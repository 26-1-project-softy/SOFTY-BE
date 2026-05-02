package com.softy.be.chat.dto;

public record InitMessageSendData(
        Long chatRoomId,
        Long messageId
) {
}
