package com.softy.be.chat.dto;

public record InitMessageSendRequest(
        String content,
        String intentLabel
) {
}
