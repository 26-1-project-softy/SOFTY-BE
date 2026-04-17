package com.softy.be.chat.repository;

import java.time.LocalDateTime;

public interface ReportChatRoomRow {
    Long getChatRoomId();
    String getParentName();
    String getStudentName();
    String getIntentLabel();
    String getStatus();
    LocalDateTime getLastMessageAt();
}
