package com.softy.be.chat.repository;

public interface ReportChatPreviewMetaRow {
    Long getChatRoomId();
    String getParentName();
    String getStudentName();
    String getIntentLabel();
    String getStatus();
}
