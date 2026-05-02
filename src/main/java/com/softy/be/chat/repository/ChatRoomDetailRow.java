package com.softy.be.chat.repository;

public interface ChatRoomDetailRow {
    Long getChatRoomId();
    String getCounterpartName();
    String getStudentName();
    String getIntentLabel();
    String getStatus();
}
