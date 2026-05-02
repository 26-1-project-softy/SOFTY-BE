package com.softy.be.chat.repository;

import java.time.LocalDateTime;

public interface ChatRoomListRow {
    Long getChatRoomId();
    String getCounterpartName();
    String getStudentName();
    String getLastMessage();
    LocalDateTime getLastMessageAt();
    Integer getUnreadCount();
    String getStatus();
    String getIntentLabel();
}
