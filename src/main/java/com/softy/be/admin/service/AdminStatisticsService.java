package com.softy.be.admin.service;

import com.softy.be.admin.dto.AdminPdfStatisticsData;
import com.softy.be.admin.dto.AdminEvaluationRerunData;
import com.softy.be.admin.dto.AdminEvaluationRerunRequest;
import com.softy.be.admin.dto.AdminPerformanceStatisticsData;
import com.softy.be.admin.dto.AdminRecommendationAdoptionData;
import com.softy.be.admin.dto.AdminRiskStatisticsData;
import com.softy.be.admin.dto.AdminTeacherPdfCountData;
import com.softy.be.admin.repository.LatestModelVersionRow;
import com.softy.be.admin.repository.ModelTrainingHistoryRepository;
import com.softy.be.chat.repository.AiRecommendationRepository;
import com.softy.be.chat.repository.MessageRepository;
import com.softy.be.report.repository.PdfFileRepository;
import com.softy.be.report.repository.TeacherPdfCountRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private final PdfFileRepository pdfFileRepository;
    private final MessageRepository messageRepository;
    private final AiRecommendationRepository aiRecommendationRepository;
    private final AiEvaluationClient aiEvaluationClient;
    private final ModelTrainingHistoryRepository modelTrainingHistoryRepository;

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
        long totalUsedAsIs = aiRecommendationRepository.countTeacherRecommendationsUsedAsIs();
        long totalModified = aiRecommendationRepository.countTeacherRecommendationsModified();
        long totalNotUsed = aiRecommendationRepository.countTeacherRecommendationsNotUsed();
        long totalRecommendationCount = totalUsedAsIs + totalModified + totalNotUsed;

        double adoptionRate = calculateAdoptionRate(totalRecommendationCount, totalUsedAsIs, totalModified);
        return new AdminRecommendationAdoptionData(adoptionRate, totalUsedAsIs, totalModified, totalNotUsed);
    }

    @Transactional(readOnly = true)
    public AdminPerformanceStatisticsData getPerformanceStatistics(String evaluationId) {
        String resolvedEvaluationId = resolveEvaluationId(evaluationId);
        AiEvaluationClient.AiEvaluationResult result = aiEvaluationClient.getEvaluation(resolvedEvaluationId);

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
        LatestModelVersionRow latestModelVersion = resolveLatestModelVersionIfNeeded(request);
        String resolvedVersion = resolveVersion(request, latestModelVersion);
        String resolvedDatasetVersion = resolveDatasetVersion(request, latestModelVersion);

        AiEvaluationClient.AiEvaluationRerunResult result = aiEvaluationClient.requestRiskDetectionEvaluation(
                resolvedVersion,
                resolvedDatasetVersion
        );

        return new AdminEvaluationRerunData(
                nullToEmpty(result.evaluationId()),
                nullToEmpty(result.status()),
                result.resultCode(),
                nullToEmpty(result.resultMessage()),
                nullToEmpty(result.contentType()),
                resolvedVersion,
                resolvedDatasetVersion
        );
    }

    private String resolveEvaluationId(String evaluationId) {
        if (evaluationId != null && !evaluationId.isBlank()) {
            return evaluationId.trim();
        }

        return modelTrainingHistoryRepository.findLatestEvaluationIds(PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(
                () -> new ResponseStatusException(NOT_FOUND, "성능 통계 조회에 사용할 evaluationId가 없습니다.")
                );
    }

    private String resolveVersion(AdminEvaluationRerunRequest request, LatestModelVersionRow latestModelVersion) {
        if (request != null && request.version() != null && !request.version().isBlank()) {
            return request.version().trim();
        }
        return latestModelVersion.getModelVersion();
    }

    private String resolveDatasetVersion(AdminEvaluationRerunRequest request, LatestModelVersionRow latestModelVersion) {
        if (request != null && request.datasetVersion() != null && !request.datasetVersion().isBlank()) {
            return request.datasetVersion().trim();
        }
        return latestModelVersion.getDatasetVersion();
    }

    private LatestModelVersionRow resolveLatestModelVersionIfNeeded(AdminEvaluationRerunRequest request) {
        boolean versionProvided = request != null && request.version() != null && !request.version().isBlank();
        boolean datasetVersionProvided = request != null && request.datasetVersion() != null && !request.datasetVersion().isBlank();
        if (versionProvided && datasetVersionProvided) {
            return null;
        }
        return resolveLatestModelVersion();
    }

    private LatestModelVersionRow resolveLatestModelVersion() {
        return modelTrainingHistoryRepository.findLatestModelVersions(PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new ResponseStatusException(NOT_FOUND, "재평가에 사용할 모델 버전 정보가 없습니다.")
                );
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
