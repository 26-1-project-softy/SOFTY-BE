package com.softy.be.admin.service;

import com.softy.be.admin.dto.AdminEvaluationRerunData;
import com.softy.be.admin.dto.AdminEvaluationRerunRequest;
import com.softy.be.admin.dto.AdminPdfStatisticsData;
import com.softy.be.admin.dto.AdminPerformanceStatisticsData;
import com.softy.be.admin.dto.AdminRecommendationAdoptionData;
import com.softy.be.admin.dto.AdminRiskStatisticsData;
import com.softy.be.admin.dto.AdminTeacherPdfCountData;
import com.softy.be.chat.repository.AiRecommendationRepository;
import com.softy.be.chat.repository.MessageRepository;
import com.softy.be.report.repository.PdfFileRepository;
import com.softy.be.report.repository.TeacherPdfCountRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private final PdfFileRepository pdfFileRepository;
    private final MessageRepository messageRepository;
    private final AiRecommendationRepository aiRecommendationRepository;
    private final AiEvaluationClient aiEvaluationClient;

    @Transactional(readOnly = true)
    public AdminPdfStatisticsData getPdfStatistics() {
        long totalPdfCount = pdfFileRepository.count();
        List<AdminTeacherPdfCountData> list = pdfFileRepository.findTeacherPdfCounts()
                .stream()
                .map(this::toTeacherPdfCountData)
                .toList();

        return new AdminPdfStatisticsData(totalPdfCount, list);
    }

    @Transactional(readOnly = true)
    public AdminRiskStatisticsData getRiskStatistics() {
        long totalMessageCount = messageRepository.countTeacherMessages();
        long detectedConflictCount = messageRepository.countTeacherDisputeRiskMessages();
        double conflictDetectionRate = calculateDetectionRate(totalMessageCount, detectedConflictCount);

        return new AdminRiskStatisticsData(
                totalMessageCount,
                detectedConflictCount,
                conflictDetectionRate
        );
    }

    @Transactional(readOnly = true)
    public AdminRecommendationAdoptionData getRecommendationAdoptionStatistics() {
        long totalRecommendationCount = aiRecommendationRepository.countTeacherEmbeddedRecommendations();
        long totalUsedAsIs = aiRecommendationRepository.countTeacherRecommendationsUsedAsIs();
        long totalModified = aiRecommendationRepository.countTeacherRecommendationsModified();
        long totalNotUsed = aiRecommendationRepository.countTeacherRecommendationsNotUsed();

        double adoptionRate = calculateAdoptionRate(totalRecommendationCount, totalUsedAsIs, totalModified);
        return new AdminRecommendationAdoptionData(adoptionRate, totalUsedAsIs, totalModified, totalNotUsed);
    }

    @Transactional(readOnly = true)
    public AdminPerformanceStatisticsData getPerformanceStatistics(String evaluationId) {
        AiEvaluationClient.AiEvaluationResult result = aiEvaluationClient.getEvaluation(evaluationId);

        return new AdminPerformanceStatisticsData(
                result.evaluationId(),
                nullToZero(result.precision()),
                nullToZero(result.recall()),
                nullToZero(result.f1Score()),
                nullToEmpty(result.status()),
                result.passed(),
                nullToEmpty(result.version()),
                result.resultCode(),
                nullToEmpty(result.resultMessage())
        );
    }

    @Transactional(readOnly = true)
    public AdminEvaluationRerunData requestEvaluationRerun(AdminEvaluationRerunRequest request) {
        String version = normalizeOptionalValue(request == null ? null : request.version());
        String datasetVersion = normalizeOptionalValue(request == null ? null : request.datasetVersion());

        AiEvaluationClient.AiEvaluationRerunResult result = aiEvaluationClient.requestRiskDetectionEvaluation(
                version,
                datasetVersion
        );

        return new AdminEvaluationRerunData(
                nullToEmpty(result.evaluationId()),
                nullToEmpty(result.status()),
                result.resultCode(),
                nullToEmpty(result.resultMessage()),
                nullToEmpty(result.contentType()),
                version,
                datasetVersion
        );
    }

    private String normalizeOptionalValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private AdminTeacherPdfCountData toTeacherPdfCountData(TeacherPdfCountRow row) {
        long teacherId = row.getTeacherId() == null ? 0L : row.getTeacherId();
        long pdfCount = row.getPdfCount() == null ? 0L : row.getPdfCount();
        return new AdminTeacherPdfCountData(
                teacherId,
                row.getTeacherName(),
                pdfCount
        );
    }

    private double calculateDetectionRate(long totalMessageCount, long detectedConflictCount) {
        if (totalMessageCount <= 0) {
            return 0.0;
        }
        double rawRate = (detectedConflictCount * 100.0) / totalMessageCount;
        return Math.round(rawRate * 100.0) / 100.0;
    }

    private double calculateAdoptionRate(long totalRecommendationCount, long totalUsedAsIs, long totalModified) {
        if (totalRecommendationCount <= 0) {
            return 0.0;
        }
        double rawRate = ((totalUsedAsIs + totalModified) * 100.0) / totalRecommendationCount;
        return Math.round(rawRate * 100.0) / 100.0;
    }

    private double nullToZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
