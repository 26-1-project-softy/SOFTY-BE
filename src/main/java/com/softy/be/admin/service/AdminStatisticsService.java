package com.softy.be.admin.service;

import com.softy.be.admin.dto.AdminPdfStatisticsData;
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

    private static final double USED_AS_IS_THRESHOLD = 0.99;

    private final PdfFileRepository pdfFileRepository;
    private final MessageRepository messageRepository;
    private final AiRecommendationRepository aiRecommendationRepository;

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
        long totalRecommendationCount = aiRecommendationRepository.countTeacherRecommendations();
        long totalUsedAsIs = aiRecommendationRepository.countTeacherRecommendationsUsedAsIs(USED_AS_IS_THRESHOLD);
        long totalModified = aiRecommendationRepository.countTeacherRecommendationsModified(USED_AS_IS_THRESHOLD);
        long totalNotUsed = aiRecommendationRepository.countTeacherRecommendationsNotUsed();

        double adoptionRate = calculateAdoptionRate(totalRecommendationCount, totalUsedAsIs, totalModified);
        return new AdminRecommendationAdoptionData(adoptionRate, totalUsedAsIs, totalModified, totalNotUsed);
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
}