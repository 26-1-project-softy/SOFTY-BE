package com.softy.be.admin.service;

import com.softy.be.chat.repository.EmbeddingUpdateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AdminEmbeddingWriter {

    private final EmbeddingUpdateRepository embeddingUpdateRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateEmbeddingRow(
            Long recommendationId,
            Long messageId,
            String recommendationEmbeddingVector,
            String contentEmbeddingVector,
            String modifyEmbeddingVector,
            Double similarityOriginal,
            Double similarityModified
    ) {
        embeddingUpdateRepository.updateRecommendationEmbedding(recommendationId, recommendationEmbeddingVector);
        embeddingUpdateRepository.updateMessageEmbeddingAndSimilarity(
                messageId,
                contentEmbeddingVector,
                modifyEmbeddingVector,
                similarityOriginal,
                similarityModified
        );
    }
}
