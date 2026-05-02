package com.softy.be.chat.dto;

import java.util.List;

public record ChatRoomListData(
        List<ChatRoomListItemData> content,
        int size,
        Long nextCursor,
        boolean hasNext
) {
}
