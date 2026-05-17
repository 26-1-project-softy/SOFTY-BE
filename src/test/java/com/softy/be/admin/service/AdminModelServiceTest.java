package com.softy.be.admin.service;

import com.softy.be.admin.dto.AdminTrainingJobHistoryData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminModelServiceTest {

    @Mock
    private AiTrainingJobClient aiTrainingJobClient;

    @Mock
    private AiTrainingHistoryClient aiTrainingHistoryClient;

    @Mock
    private AiRetrainingJobClient aiRetrainingJobClient;

    @InjectMocks
    private AdminModelService adminModelService;

    @Test
    void getTrainingHistoryMapsAiResponseAndNormalizesStatus() {
        when(aiTrainingHistoryClient.getTrainingHistory(1, 20)).thenReturn(
                new AiTrainingHistoryClient.AiTrainingHistoryResult(
                        List.of(
                                new AiTrainingHistoryClient.AiTrainingHistoryItemResult(
                                        "2026-05-06T18:57:07",
                                        "v1.2",
                                        "v1.1",
                                        null,
                                        "completed"
                                ),
                                new AiTrainingHistoryClient.AiTrainingHistoryItemResult(
                                        "2026-05-06T10:10:40",
                                        "v1.1.1",
                                        "v1.0",
                                        0.8044,
                                        "failed"
                                )
                        ),
                        1,
                        20,
                        38,
                        2
                )
        );

        AdminTrainingJobHistoryData result = adminModelService.getTrainingHistory(1, 20);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalCount()).isEqualTo(38);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).f1Score()).isNull();
        assertThat(result.items().get(0).status()).isEqualTo("COMPLETED");
        assertThat(result.items().get(1).f1Score()).isEqualTo(0.8044);
        assertThat(result.items().get(1).status()).isEqualTo("FAILED");
    }

    @Test
    void getTrainingHistoryRejectsInvalidPage() {
        assertThatThrownBy(() -> adminModelService.getTrainingHistory(0, 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(aiTrainingHistoryClient);
    }

    @Test
    void getTrainingHistoryRejectsInvalidSize() {
        assertThatThrownBy(() -> adminModelService.getTrainingHistory(1, 0))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(aiTrainingHistoryClient);
    }

    @Test
    void requestRiskDetectionRetrainingReturnsJobIdAndNormalizedStatus() {
        when(aiRetrainingJobClient.requestRiskDetectionRetraining()).thenReturn(
                new AiRetrainingJobClient.AiRetrainingJobResult("retrain_20260326_001", "queued", 15)
        );

        var result = adminModelService.requestRiskDetectionRetraining();

        assertThat(result.jobId()).isEqualTo("retrain_20260326_001");
        assertThat(result.status()).isEqualTo("QUEUED");
    }

    @Test
    void getTrainingJobStatusReturnsPollingFields() {
        when(aiTrainingJobClient.getTrainingJob("retrain_20260516_6D8")).thenReturn(
                new AiTrainingJobClient.AiTrainingJobResult(
                        "retrain_20260516_6D8",
                        "kanana-risk-detector",
                        "v1.2.9",
                        "v1.1",
                        "failed",
                        0,
                        "2026-05-16T12:15:36",
                        "",
                        200,
                        "success"
                )
        );

        var result = adminModelService.getTrainingJobStatus("retrain_20260516_6D8");

        assertThat(result.jobId()).isEqualTo("retrain_20260516_6D8");
        assertThat(result.modelName()).isEqualTo("kanana-risk-detector");
        assertThat(result.modelVersion()).isEqualTo("v1.2.9");
        assertThat(result.datasetVersion()).isEqualTo("v1.1");
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.progressPercent()).isEqualTo(0);
        assertThat(result.startedAt()).isEqualTo("2026-05-16T12:15:36");
        assertThat(result.finishedAt()).isEqualTo("");
    }
}
