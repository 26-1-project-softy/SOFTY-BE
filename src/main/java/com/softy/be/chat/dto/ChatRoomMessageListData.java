package com.softy.be.chat.dto;

import java.util.List;

public record ChatRoomMessageListData(
        Long chatRoomId,
        List<ChatRoomMessageItemData> messages,
        Long nextCursor,
        boolean hasNext
) {
}
