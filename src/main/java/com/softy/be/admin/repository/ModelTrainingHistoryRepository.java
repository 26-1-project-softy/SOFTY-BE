package com.softy.be.admin.repository;

import com.softy.be.admin.entity.ModelTrainingHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ModelTrainingHistoryRepository extends JpaRepository<ModelTrainingHistory, Long> {

    @Query("""
            SELECT m.evaluationId
            FROM ModelTrainingHistory m
            WHERE m.evaluationId IS NOT NULL
              AND TRIM(m.evaluationId) <> ''
            ORDER BY m.trainedAt DESC, m.id DESC
            """)
    Page<String> findLatestEvaluationIds(Pageable pageable);
}
