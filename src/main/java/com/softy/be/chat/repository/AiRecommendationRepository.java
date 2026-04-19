package com.softy.be.chat.repository;

import com.softy.be.chat.entity.AiRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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

