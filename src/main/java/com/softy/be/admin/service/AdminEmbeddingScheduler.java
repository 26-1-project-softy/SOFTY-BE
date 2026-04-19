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
            cron = "${embedding.schedule.cron:0 0 3 * * SUN}",
            zone = "${embedding.schedule.zone:Asia/Seoul}"
    )
    public void runWeeklyEmbeddingJob() {
        adminEmbeddingService.runScheduled();
        log.info("Weekly embedding scheduler executed.");
    }
}
