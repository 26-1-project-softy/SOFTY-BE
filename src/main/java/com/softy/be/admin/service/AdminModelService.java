package com.softy.be.admin.service;

import com.softy.be.admin.dto.AdminLatestModelData;
import com.softy.be.admin.dto.AdminRetrainingJobData;
import com.softy.be.admin.dto.AdminTrainingJobHistoryData;
import com.softy.be.admin.dto.AdminTrainingJobHistoryItemData;
import com.softy.be.admin.dto.AdminTrainingJobStatusData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class AdminModelService {

    private final AiTrainingJobClient aiTrainingJobClient;
    private final AiTrainingHistoryClient aiTrainingHistoryClient;
    private final AiRetrainingJobClient aiRetrainingJobClient;

    @Transactional(readOnly = true)
    public AdminLatestModelData getLatestModel() {
        AiTrainingJobClient.AiTrainingJobResult result = aiTrainingJobClient.getLatestTrainingJob();
        String lastTrainedAt = nullToEmpty(result.startedAt());

        return new AdminLatestModelData(
                nullToEmpty(result.jobId()),
                nullToEmpty(result.modelName()),
                nullToEmpty(result.modelVersion()),
                nullIfBlank(result.datasetVersion()),
                nullToEmpty(result.status()),
                lastTrainedAt
        );
    }

    @Transactional(readOnly = true)
    public AdminTrainingJobStatusData getTrainingJobStatus(String jobId) {
        AiTrainingJobClient.AiTrainingJobResult result = aiTrainingJobClient.getTrainingJob(jobId);

        return new AdminTrainingJobStatusData(
                nullToEmpty(result.jobId()),
                nullToEmpty(result.modelName()),
                nullToEmpty(result.modelVersion()),
                nullIfBlank(result.datasetVersion()),
                normalizeStatus(result.status()),
                result.progressPercent(),
                nullToEmpty(result.startedAt()),
                nullToEmpty(result.finishedAt())
        );
    }

    @Transactional(readOnly = true)
    public AdminTrainingJobHistoryData getTrainingHistory(int page, int size) {
        if (page < 1) {
            throw new ResponseStatusException(BAD_REQUEST, "page는 1 이상이어야 합니다.");
        }
        if (size < 1) {
            throw new ResponseStatusException(BAD_REQUEST, "size는 1 이상이어야 합니다.");
        }

        AiTrainingHistoryClient.AiTrainingHistoryResult result = aiTrainingHistoryClient.getTrainingHistory(page, size);
        List<AdminTrainingJobHistoryItemData> items = result.items().stream()
                .map(item -> new AdminTrainingJobHistoryItemData(
                        nullToEmpty(item.trainedAt()),
                        nullToEmpty(item.version()),
                        nullToEmpty(item.datasetVersion()),
                        item.f1Score(),
                        normalizeStatus(item.status())
                ))
                .toList();

        return new AdminTrainingJobHistoryData(
                items,
                result.page(),
                result.size(),
                result.totalCount(),
                result.totalPages()
        );
    }

    @Transactional(readOnly = true)
    public AdminRetrainingJobData requestRiskDetectionRetraining() {
        AiRetrainingJobClient.AiRetrainingJobResult result = aiRetrainingJobClient.requestRiskDetectionRetraining();

        return new AdminRetrainingJobData(
                nullToEmpty(result.jobId()),
                normalizeStatus(result.status())
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String nullIfBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
