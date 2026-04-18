package com.softy.be.admin.service;

import com.softy.be.admin.dto.AdminPdfStatisticsData;
import com.softy.be.admin.dto.AdminRiskStatisticsData;
import com.softy.be.admin.dto.AdminTeacherPdfCountData;
import com.softy.be.chat.repository.MessageRepository;
import com.softy.be.report.repository.PdfFileRepository;
import com.softy.be.report.repository.TeacherPdfCountRow;
import com.softy.be.user.entity.User;
import com.softy.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final UserRepository userRepository;
    private final PdfFileRepository pdfFileRepository;
    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public AdminPdfStatisticsData getPdfStatistics(Long authenticatedUserId) {
        validateAdminOrThrow(authenticatedUserId);

        long totalPdfCount = pdfFileRepository.count();
        List<AdminTeacherPdfCountData> list = pdfFileRepository.findTeacherPdfCounts()
                .stream()
                .map(this::toTeacherPdfCountData)
                .toList();

        return new AdminPdfStatisticsData(totalPdfCount, list);
    }

    @Transactional(readOnly = true)
    public AdminRiskStatisticsData getRiskStatistics(Long authenticatedUserId) {
        validateAdminOrThrow(authenticatedUserId);

        long totalMessageCount = messageRepository.countTeacherMessages();
        long detectedConflictCount = messageRepository.countTeacherDisputeRiskMessages();
        double conflictDetectionRate = calculateDetectionRate(totalMessageCount, detectedConflictCount);

        return new AdminRiskStatisticsData(
                totalMessageCount,
                detectedConflictCount,
                conflictDetectionRate
        );
    }

    private void validateAdminOrThrow(Long authenticatedUserId) {
        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (!ADMIN_ROLE.equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 계정만 통계를 조회할 수 있습니다.");
        }
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
}
