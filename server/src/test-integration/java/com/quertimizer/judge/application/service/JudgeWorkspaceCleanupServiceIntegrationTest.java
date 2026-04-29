package com.quertimizer.judge.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("JudgeWorkspaceCleanupService")
class JudgeWorkspaceCleanupServiceIntegrationTest {

    @Autowired private JudgeWorkspaceCleanupService judgeWorkspaceCleanupService;

    @Nested
    @DisplayName("PostConstruct cleanupResidualWorkspaces")
    class CleanupResidualWorkspaces {

        @Test
        @DisplayName("성공 (잔여 workspace 없음)")
        void successWhenJudgeDatabaseNotConfigured() {
            // given

            // when
            var result = assertThatCode(() -> judgeWorkspaceCleanupService.cleanupResidualWorkspaces());

            // then
            result.doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Scheduled cleanupInactiveWorkspaces")
    class CleanupInactiveWorkspaces {

        @Test
        @DisplayName("성공 (비활성 workspace 없음)")
        void successWhenInactiveWorkspaceMissing() {
            // given

            // when
            var result = assertThatCode(() -> judgeWorkspaceCleanupService.cleanupInactiveWorkspaces());

            // then
            result.doesNotThrowAnyException();
        }
    }
}
