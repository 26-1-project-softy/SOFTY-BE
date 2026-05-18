package com.softy.be.admin.service;

import com.softy.be.admin.dto.AdminEvaluationRerunData;
import com.softy.be.admin.dto.AdminEvaluationRerunRequest;
import com.softy.be.admin.dto.AdminPdfStatisticsData;
import com.softy.be.admin.dto.AdminPerformanceStatisticsData;
import com.softy.be.admin.dto.AdminRecommendationAdoptionData;
import com.softy.be.admin.dto.AdminRiskFeedbackItemData;
import com.softy.be.admin.dto.AdminRiskFeedbackListData;
import com.softy.be.admin.dto.AdminRiskStatisticsData;
import com.softy.be.admin.dto.AdminTokenUsageData;
import com.softy.be.admin.dto.AdminTokenUsageDetailData;
import com.softy.be.admin.dto.AdminTokenUsageSummaryData;
import com.softy.be.admin.dto.AdminTeacherPdfCountData;
import com.softy.be.chat.repository.AiFeedbackListRow;
import com.softy.be.chat.repository.AiFeedbackRepository;
import com.softy.be.chat.repository.AiRecommendationRepository;
import com.softy.be.chat.repository.MessageRepository;
import com.softy.be.report.repository.PdfFileRepository;
import com.softy.be.report.repository.TeacherPdfCountRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private final PdfFileRepository pdfFileRepository;
    private final MessageRepository messageRepository;
    private final AiFeedbackRepository aiFeedbackRepository;
    private final AiRecommendationRepository aiRecommendationRepository;
    private final AiEvaluationClient aiEvaluationClient;
    private final AiTokenUsageClient aiTokenUsageClient;

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
    public AdminRiskFeedbackListData getRiskFeedbacks(
            int page,
            int size,
            String riskLevel,
            Integer feedbackResult,
            String teacherName,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (page < 1) {
            throw new ResponseStatusException(BAD_REQUEST, "page는 1 이상이어야 합니다.");
        }
        if (size < 1) {
            throw new ResponseStatusException(BAD_REQUEST, "size는 1 이상이어야 합니다.");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(BAD_REQUEST, "startDate는 endDate보다 늦을 수 없습니다.");
        }
        if (feedbackResult != null && (feedbackResult < 1 || feedbackResult > 5)) {
            throw new ResponseStatusException(BAD_REQUEST, "feedbackResult는 1 이상 5 이하여야 합니다.");
        }

        String normalizedRiskLevel = normalizeOptionalUppercaseValue(riskLevel);
        String normalizedTeacherNamePattern = toContainsPattern(teacherName);
        LocalDateTime startDateTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate == null ? null : endDate.plusDays(1).atStartOfDay();

        Page<AiFeedbackListRow> feedbackPage = aiFeedbackRepository.findRiskFeedbacks(
                normalizedRiskLevel,
                feedbackResult,
                normalizedTeacherNamePattern,
                startDateTime,
                endDateTime,
                PageRequest.of(page - 1, size)
        );

        List<AdminRiskFeedbackItemData> items = feedbackPage.getContent()
                .stream()
                .map(this::toRiskFeedbackItemData)
                .toList();

        return new AdminRiskFeedbackListData(
                items,
                page,
                size,
                feedbackPage.getTotalElements(),
                feedbackPage.getTotalPages()
        );
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
                result.progressPercent(),
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

    @Transactional(readOnly = true)
    public AdminTokenUsageData getTokenUsage() {
        AiTokenUsageClient.AiTokenUsageResult result = aiTokenUsageClient.getTokenUsage();

        return new AdminTokenUsageData(
                new AdminTokenUsageSummaryData(
                        result.totalUsage().inputTokens(),
                        result.totalUsage().outputTokens(),
                        result.totalUsage().totalTokens()
                ),
                result.details().stream()
                        .map(detail -> new AdminTokenUsageDetailData(
                                nullToEmpty(detail.endpoint()),
                                detail.inputTokens(),
                                detail.outputTokens(),
                                detail.totalTokens()
                        ))
                        .toList()
        );
    }

    private String normalizeOptionalValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeOptionalUppercaseValue(String value) {
        String normalized = normalizeOptionalValue(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalLowercaseValue(String value) {
        String normalized = normalizeOptionalValue(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String toContainsPattern(String value) {
        String normalized = normalizeOptionalLowercaseValue(value);
        return normalized == null ? null : "%" + normalized + "%";
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

    private AdminRiskFeedbackItemData toRiskFeedbackItemData(AiFeedbackListRow row) {
        return new AdminRiskFeedbackItemData(
                row.getFeedbackId(),
                row.getTeacherName(),
                row.getFeedbackResult(),
                row.getRiskLevel(),
                row.getOriginalMessage(),
                row.getCreatedAt()
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
