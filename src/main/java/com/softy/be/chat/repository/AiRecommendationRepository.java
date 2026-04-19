package com.softy.be.chat.repository;

import com.softy.be.chat.entity.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, Long> {

    @Query("""
            SELECT COUNT(ar)
            FROM AiRecommendation ar
            JOIN ar.message m
            JOIN m.sender s
            WHERE UPPER(s.role) = 'TEACHER'
            """)
    long countTeacherRecommendations();

    @Query("""
            SELECT COUNT(ar)
            FROM AiRecommendation ar
            JOIN ar.message m
            JOIN m.sender s
            WHERE UPPER(s.role) = 'TEACHER'
              AND ar.isRecommendationUsed = true
              AND m.similarityModified >= :usedAsIsThreshold
            """)
    long countTeacherRecommendationsUsedAsIs(@Param("usedAsIsThreshold") double usedAsIsThreshold);

    @Query("""
            SELECT COUNT(ar)
            FROM AiRecommendation ar
            JOIN ar.message m
            JOIN m.sender s
            WHERE UPPER(s.role) = 'TEACHER'
              AND ar.isRecommendationUsed = true
              AND (m.similarityModified IS NULL OR m.similarityModified < :usedAsIsThreshold)
            """)
    long countTeacherRecommendationsModified(@Param("usedAsIsThreshold") double usedAsIsThreshold);

    @Query("""
            SELECT COUNT(ar)
            FROM AiRecommendation ar
            JOIN ar.message m
            JOIN m.sender s
            WHERE UPPER(s.role) = 'TEACHER'
              AND (ar.isRecommendationUsed IS NULL OR ar.isRecommendationUsed = false)
            """)
    long countTeacherRecommendationsNotUsed();
}

