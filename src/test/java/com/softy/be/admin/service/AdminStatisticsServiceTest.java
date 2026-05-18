package com.softy.be.admin.service;

import com.softy.be.admin.dto.AdminRiskFeedbackListData;
import com.softy.be.chat.repository.AiFeedbackListRow;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
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
    void getRiskFeedbacksReturnsPagedResult() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 18, 12, 30);
        AiFeedbackListRow row = new TestAiFeedbackListRow(
                7L,
                "김선생",
                4,
                "UNSAFE",
                "위험 표현이 포함된 메시지입니다.",
                createdAt
        );

        when(aiFeedbackRepository.findRiskFeedbacks(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any()
        )).thenReturn(
                new PageImpl<>(
                        List.of(row),
                        PageRequest.of(1, 10),
                        11
                )
        );

        AdminRiskFeedbackListData result = adminStatisticsService.getRiskFeedbacks(2, 10, null, null, null, null, null);

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(11);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).feedbackId()).isEqualTo(7L);
        assertThat(result.items().get(0).teacherName()).isEqualTo("김선생");
        assertThat(result.items().get(0).feedbackResult()).isEqualTo(4);
        assertThat(result.items().get(0).riskLevel()).isEqualTo("UNSAFE");
        assertThat(result.items().get(0).originalMessage()).isEqualTo("위험 표현이 포함된 메시지입니다.");
        assertThat(result.items().get(0).createdAt()).isEqualTo(createdAt);
    }

    @Test
    void getRiskFeedbacksAppliesFilters() {
        when(aiFeedbackRepository.findRiskFeedbacks(
                eq("UNSAFE"),
                eq(2),
                eq("김"),
                eq(LocalDateTime.of(2026, 5, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 5, 19, 0, 0)),
                any()
        )).thenReturn(
                new PageImpl<>(
                        List.of(),
                        PageRequest.of(0, 20),
                        0
                )
        );

        AdminRiskFeedbackListData result = adminStatisticsService.getRiskFeedbacks(
                1,
                20,
                "unsafe",
                2,
                " 김 ",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 18)
        );

        assertThat(result.items()).isEmpty();
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
    }

    @Test
    void getRiskFeedbacksRejectsInvalidPage() {
        assertThatThrownBy(() -> adminStatisticsService.getRiskFeedbacks(0, 20, null, null, null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(aiFeedbackRepository);
    }

    @Test
    void getRiskFeedbacksRejectsInvalidSize() {
        assertThatThrownBy(() -> adminStatisticsService.getRiskFeedbacks(1, 0, null, null, null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(aiFeedbackRepository);
    }

    @Test
    void getRiskFeedbacksRejectsInvalidDateRange() {
        assertThatThrownBy(() -> adminStatisticsService.getRiskFeedbacks(
                1,
                20,
                null,
                null,
                null,
                LocalDate.of(2026, 5, 19),
                LocalDate.of(2026, 5, 18)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(aiFeedbackRepository);
    }

    @Test
    void getRiskFeedbacksRejectsInvalidFeedbackResult() {
        assertThatThrownBy(() -> adminStatisticsService.getRiskFeedbacks(1, 20, null, 6, null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verifyNoInteractions(aiFeedbackRepository);
    }

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

    private record TestAiFeedbackListRow(
            Long feedbackId,
            String teacherName,
            Integer feedbackResult,
            String riskLevel,
            String originalMessage,
            LocalDateTime createdAt
    ) implements AiFeedbackListRow {

        @Override
        public Long getFeedbackId() {
            return feedbackId;
        }

        @Override
        public String getTeacherName() {
            return teacherName;
        }

        @Override
        public Integer getFeedbackResult() {
            return feedbackResult;
        }

        @Override
        public String getRiskLevel() {
            return riskLevel;
        }

        @Override
        public String getOriginalMessage() {
            return originalMessage;
        }

        @Override
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }
}
