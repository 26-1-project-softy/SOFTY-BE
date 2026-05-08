package com.softy.be.admin.service;

import com.softy.be.admin.dto.AdminTokenUsageData;
import com.softy.be.chat.repository.AiFeedbackRepository;
import com.softy.be.chat.repository.AiRecommendationRepository;
import com.softy.be.chat.repository.MessageRepository;
import com.softy.be.report.repository.PdfFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatisticsServiceTest {

    @Mock
    private PdfFileRepository pdfFileRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private AiFeedbackRepository aiFeedbackRepository;

    @Mock
    private AiRecommendationRepository aiRecommendationRepository;

    @Mock
    private AiEvaluationClient aiEvaluationClient;

    @Mock
    private AiTokenUsageClient aiTokenUsageClient;

    @InjectMocks
    private AdminStatisticsService adminStatisticsService;

    @Test
    void getTokenUsageMapsAiResponseToAdminDto() {
        when(aiTokenUsageClient.getTokenUsage()).thenReturn(
                new AiTokenUsageClient.AiTokenUsageResult(
                        "json",
                        200,
                        "success",
                        new AiTokenUsageClient.AiTokenUsageSummaryResult(9106, 205, 9311),
                        List.of(
                                new AiTokenUsageClient.AiTokenUsageDetailResult("classify-intent", 8996, 174, 9170),
                                new AiTokenUsageClient.AiTokenUsageDetailResult("recommend-alternative", 110, 31, 141)
                        )
                )
        );

        AdminTokenUsageData result = adminStatisticsService.getTokenUsage();

        assertThat(result.totalUsage().inputTokens()).isEqualTo(9106);
        assertThat(result.totalUsage().outputTokens()).isEqualTo(205);
        assertThat(result.totalUsage().totalTokens()).isEqualTo(9311);
        assertThat(result.details()).hasSize(2);
        assertThat(result.details().get(0).modelName()).isEqualTo("classify-intent");
        assertThat(result.details().get(1).totalTokens()).isEqualTo(141);
    }
}
