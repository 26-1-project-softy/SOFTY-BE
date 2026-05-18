package com.softy.be.chat.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AiFeedbackRepositoryCustom {

    Page<AiFeedbackListRow> findRiskFeedbacks(
            String riskLevel,
            Integer feedbackResult,
            String teacherName,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Pageable pageable
    );
}
