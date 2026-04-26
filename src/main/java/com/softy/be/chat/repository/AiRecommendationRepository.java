package com.softy.be.chat.repository;

import com.softy.be.chat.entity.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AiRecommendationRepository extends JpaRepository<AiRecommendation, Long> {

    @Query("""
            SELECT COUNT(ar)
            FROM AiRecommendation ar
            JOIN ar.message m
            JOIN m.sender s
            WHERE UPPER(s.role) = 'TEACHER'
              AND ar.content IS NOT NULL
            """)
    long countTeacherRecommendations();

    @Query(value = """
            SELECT COUNT(ar.id)
            FROM ai_recommendation ar
            JOIN message m ON m.id = ar.message_id
            JOIN users u ON u.id = m.sender_id
            WHERE UPPER(u.role) = 'TEACHER'
              AND ar.content IS NOT NULL
              AND ar.embedding IS NOT NULL
            """, nativeQuery = true)
    long countTeacherEmbeddedRecommendations();

    @Query(value = """
            SELECT COUNT(ar.id)
            FROM ai_recommendation ar
            JOIN message m ON m.id = ar.message_id
            JOIN users u ON u.id = m.sender_id
            WHERE UPPER(u.role) = 'TEACHER'
              AND ar.content IS NOT NULL
              AND ar.embedding IS NOT NULL
              AND COALESCE(ar.is_recommendation_used, FALSE) = TRUE
              AND m.modify_content IS NOT NULL
              AND m.modify_content = ar.content
            """, nativeQuery = true)
    long countTeacherRecommendationsUsedAsIs();

    @Query(value = """
            SELECT COUNT(ar.id)
            FROM ai_recommendation ar
            JOIN message m ON m.id = ar.message_id
            JOIN users u ON u.id = m.sender_id
            WHERE UPPER(u.role) = 'TEACHER'
              AND ar.content IS NOT NULL
              AND ar.embedding IS NOT NULL
              AND COALESCE(ar.is_recommendation_used, FALSE) = TRUE
              AND NOT (
                    m.modify_content IS NOT NULL
                    AND m.modify_content = ar.content
              )
              AND m.similarity_modified IS NOT NULL
              AND m.similarity_original IS NOT NULL
              AND m.similarity_modified >= m.similarity_original
            """, nativeQuery = true)
    long countTeacherRecommendationsModified();

    @Query(value = """
            SELECT COUNT(ar.id)
            FROM ai_recommendation ar
            JOIN message m ON m.id = ar.message_id
            JOIN users u ON u.id = m.sender_id
            WHERE UPPER(u.role) = 'TEACHER'
              AND ar.content IS NOT NULL
              AND ar.embedding IS NOT NULL
              AND NOT (
                    COALESCE(ar.is_recommendation_used, FALSE) = TRUE
                    AND m.modify_content IS NOT NULL
                    AND m.modify_content = ar.content
              )
              AND (
                    COALESCE(ar.is_recommendation_used, FALSE) = FALSE
                    OR (
                        COALESCE(ar.is_recommendation_used, FALSE) = TRUE
                        AND m.similarity_modified IS NOT NULL
                        AND m.similarity_original IS NOT NULL
                        AND m.similarity_modified < m.similarity_original
                    )
              )
              AND NOT (
                    COALESCE(ar.is_recommendation_used, FALSE) = TRUE
                    AND m.similarity_modified IS NOT NULL
                    AND m.similarity_original IS NOT NULL
                    AND m.similarity_modified >= m.similarity_original
              )
            """, nativeQuery = true)
    long countTeacherRecommendationsNotUsed();

    @Query(value = """
            SELECT
                ar.id AS recommendationId,
                ar.content AS recommendationContent,
                m.id AS messageId,
                m.content AS messageContent,
                m.modify_content AS messageModifyContent
            FROM ai_recommendation ar
            JOIN message m ON m.id = ar.message_id
            JOIN users u ON u.id = m.sender_id
            WHERE UPPER(u.role) = 'TEACHER'
              AND (
                    ar.embedding IS NULL
                    OR m.content_embedding IS NULL
                    OR (m.modify_content IS NOT NULL AND m.modify_content_embedding IS NULL)
                    OR m.similarity_original IS NULL
                    OR (m.modify_content IS NOT NULL AND m.similarity_modified IS NULL)
              )
            ORDER BY ar.id
            """, nativeQuery = true)
    List<EmbeddingCandidateRow> findTeacherEmbeddingCandidates();
}

