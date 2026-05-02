package com.softy.be.chat.dto;

public record ChatRoomDetailData(
        Long chatRoomId,
        String counterpartName,
        String studentName,
        String intentLabel,
        String status
) {
}
