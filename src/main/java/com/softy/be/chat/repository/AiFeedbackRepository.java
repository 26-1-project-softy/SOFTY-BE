package com.softy.be.chat.repository;

import com.softy.be.chat.entity.AiFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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
            WHERE (:riskLevel IS NULL OR m.riskLevel = :riskLevel)
              AND (:feedbackResult IS NULL OR f.actualRiskScore = :feedbackResult)
              AND (:teacherName IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :teacherName, '%')))
              AND (:startDateTime IS NULL OR f.createdAt >= :startDateTime)
              AND (:endDateTime IS NULL OR f.createdAt < :endDateTime)
            ORDER BY f.createdAt DESC, f.id DESC
            """)
    Page<AiFeedbackListRow> findRiskFeedbacks(
            @Param("riskLevel") String riskLevel,
            @Param("feedbackResult") Integer feedbackResult,
            @Param("teacherName") String teacherName,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            Pageable pageable
    );
}
