package com.softy.be.chat.repository;

import com.softy.be.chat.entity.AiFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

    Optional<AiFeedback> findFirstByMessageAnalysisId(Long messageAnalysisId);

    @Query("""
            SELECT
                f.id AS feedbackId,
                t.name AS teacherName,
                f.actualRiskScore AS feedbackResult,
                m.riskLevel AS riskLevel,
                m.originalContent AS originalMessage,
                f.createdAt AS createdAt
            FROM AiFeedback f
            JOIN f.messageAnalysis m
            JOIN m.teacher t
            ORDER BY f.createdAt DESC, f.id DESC
            """)
    Page<AiFeedbackListRow> findRiskFeedbacks(Pageable pageable);
}
