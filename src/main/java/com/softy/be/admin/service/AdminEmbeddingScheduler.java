package com.softy.be.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminEmbeddingScheduler {

    private final AdminEmbeddingService adminEmbeddingService;

    @Scheduled(
            cron = "${embedding.schedule.cron:0 0 3 * * *}",
            zone = "${embedding.schedule.zone:Asia/Seoul}"
    )
    public void runDailyEmbeddingJob() {
        adminEmbeddingService.runScheduled();
        log.info("임베딩 스케줄러(매일 새벽 3시)가 실행되었습니다.");
    }
}
