package com.softy.be.chat.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EmbeddingUpdateRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void updateRecommendationEmbedding(Long recommendationId, String recommendationEmbeddingVector) {
        String sql = """
                UPDATE ai_recommendation
                SET embedding = CAST(:embedding AS vector),
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", recommendationId)
                .addValue("embedding", recommendationEmbeddingVector);

        jdbcTemplate.update(sql, params);
    }

    public void updateMessageEmbeddingAndSimilarity(
            Long messageId,
            String contentEmbeddingVector,
            String modifyEmbeddingVector,
            Double similarityOriginal,
            Double similarityModified
    ) {
        String sql = """
                UPDATE message
                SET content_embedding = CAST(:contentEmbedding AS vector),
                    modify_content_embedding = CASE
                        WHEN :modifyEmbedding IS NULL THEN modify_content_embedding
                        ELSE CAST(:modifyEmbedding AS vector)
                    END,
                    similarity_original = :similarityOriginal,
                    similarity_modified = :similarityModified,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = :id
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", messageId)
                .addValue("contentEmbedding", contentEmbeddingVector)
                .addValue("modifyEmbedding", modifyEmbeddingVector)
                .addValue("similarityOriginal", similarityOriginal)
                .addValue("similarityModified", similarityModified);

        jdbcTemplate.update(sql, params);
    }
}
