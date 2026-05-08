package com.softy.be.chat.repository;

import java.time.LocalDateTime;

public interface AiFeedbackListRow {

    Long getFeedbackId();

    String getTeacherName();

    Integer getFeedbackResult();

    String getOriginalMessage();

    LocalDateTime getCreatedAt();
}
