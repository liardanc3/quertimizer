package com.quertimizer.judge.application.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JudgeWorkspaceCleanupService {

    private final JudgeWorkspaceService judgeWorkspaceService;

    @PostConstruct
    public void cleanupResidualWorkspaces() {
        // 서버 재기동 후 남은 작업용 스키마 정리
        judgeWorkspaceService.cleanupResidualWorkspaces();
    }

    @Scheduled(fixedDelay = 60_000L)
    public void cleanupInactiveWorkspaces() {
        // 비활성 작업용 스키마 정리
        judgeWorkspaceService.cleanupInactiveWorkspaces();
    }
}
