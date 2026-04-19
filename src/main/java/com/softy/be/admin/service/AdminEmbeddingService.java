package com.softy.be.admin.service;

import com.softy.be.admin.dto.AdminEmbeddingRunData;
import com.softy.be.chat.repository.AiRecommendationRepository;
import com.softy.be.chat.repository.EmbeddingCandidateRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEmbeddingService {

    private final AiRecommendationRepository aiRecommendationRepository;
    private final OpenAiEmbeddingClient openAiEmbeddingClient;
    private final AdminEmbeddingWriter adminEmbeddingWriter;

    private final AtomicBoolean running = new AtomicBoolean(false);

    public AdminEmbeddingRunData runManually() {
        return run("MANUAL");
    }

    public AdminEmbeddingRunData runScheduled() {
        return run("SCHEDULED");
    }

    private AdminEmbeddingRunData run(String trigger) {
        LocalDateTime startedAt = LocalDateTime.now();

        if (!running.compareAndSet(false, true)) {
            log.info("Embedding job skipped because another run is in progress. trigger={}", trigger);
            return new AdminEmbeddingRunData(trigger, 0, 0, 0, 0, startedAt, LocalDateTime.now());
        }

        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;

        try {
            List<EmbeddingCandidateRow> candidates = aiRecommendationRepository.findTeacherEmbeddingCandidates();
            Map<String, List<Double>> embeddingCache = new HashMap<>();

            for (EmbeddingCandidateRow candidate : candidates) {
                try {
                    if (!hasText(candidate.getRecommendationContent()) || !hasText(candidate.getMessageContent())) {
                        skippedCount++;
                        continue;
                    }

                    List<Double> recommendationEmbedding = getOrCreateEmbedding(candidate.getRecommendationContent(), embeddingCache);
                    List<Double> contentEmbedding = getOrCreateEmbedding(candidate.getMessageContent(), embeddingCache);
                    List<Double> modifyEmbedding = hasText(candidate.getMessageModifyContent())
                            ? getOrCreateEmbedding(candidate.getMessageModifyContent(), embeddingCache)
                            : null;

                    Double similarityOriginal = cosineSimilarity(recommendationEmbedding, contentEmbedding);
                    Double similarityModified = modifyEmbedding == null
                            ? null
                            : cosineSimilarity(recommendationEmbedding, modifyEmbedding);

                    adminEmbeddingWriter.updateEmbeddingRow(
                            candidate.getRecommendationId(),
                            candidate.getMessageId(),
                            toVectorLiteral(recommendationEmbedding),
                            toVectorLiteral(contentEmbedding),
                            modifyEmbedding == null ? null : toVectorLiteral(modifyEmbedding),
                            similarityOriginal,
                            similarityModified
                    );
                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    log.warn(
                            "Embedding row failed. recommendationId={}, messageId={}",
                            candidate.getRecommendationId(),
                            candidate.getMessageId(),
                            e
                    );
                }
            }

            return new AdminEmbeddingRunData(
                    trigger,
                    candidates.size(),
                    successCount,
                    failedCount,
                    skippedCount,
                    startedAt,
                    LocalDateTime.now()
            );
        } finally {
            running.set(false);
        }
    }

    private List<Double> getOrCreateEmbedding(String text, Map<String, List<Double>> embeddingCache) {
        return embeddingCache.computeIfAbsent(text, openAiEmbeddingClient::createEmbedding);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String toVectorLiteral(List<Double> vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector.get(i));
        }
        sb.append(']');
        return sb.toString();
    }

    private Double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) {
            throw new IllegalStateException("Embedding dimension mismatch");
        }

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            double x = a.get(i);
            double y = b.get(i);
            dot += x * y;
            normA += x * x;
            normB += y * y;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
