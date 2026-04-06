package com.quertimizer.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProblemWorkspaceCleanupService {

    private final ProblemWorkspaceService problemWorkspaceService;

    @PostConstruct
    public void cleanupResidualWorkspaces() {

        // 서버 재기동 후 남은 작업용 스키마 정리
        problemWorkspaceService.cleanupResidualWorkspaces();
    }

    @Scheduled(fixedDelay = 60_000L)
    public void cleanupInactiveWorkspaces() {

        // 비활성 작업용 스키마 정리
        problemWorkspaceService.cleanupInactiveWorkspaces();
    }
}
