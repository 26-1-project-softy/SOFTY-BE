package com.softy.be.admin.service;

import com.softy.be.admin.dto.AdminLatestModelData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminModelService {

    private final AiTrainingJobClient aiTrainingJobClient;

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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String nullIfBlank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
