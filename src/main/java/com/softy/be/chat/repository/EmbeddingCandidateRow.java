package com.softy.be.chat.repository;

public interface EmbeddingCandidateRow {

    Long getRecommendationId();

    String getRecommendationContent();

    Long getMessageId();

    String getMessageContent();

    String getMessageModifyContent();
}
