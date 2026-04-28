package com.quertimizer.judge.application.service;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.judge.infrastructure.execution.DbmsSqlDialects;
import com.quertimizer.problem.application.port.ProblemRepository;
import com.quertimizer.problem.application.port.ProblemSolveHistoryRepository;
import com.quertimizer.problem.application.port.ProblemSubmitHistoryRepository;
import com.quertimizer.problem.application.store.ProblemStore;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JudgeQueryServiceTest {

    @Mock
    private JudgeWorkspaceService judgeWorkspaceService;

    @Mock
    private ProblemSubmitHistoryRepository problemSubmitHistoryRepository;

    @Mock
    private ProblemSolveHistoryRepository problemSolveHistoryRepository;

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private ProblemStore problemStore;

    @Test
    @DisplayName("MySQL 인터랙티브 실행은 DBMS 미지원 분기로 막지 않는다")
    void executeInteractiveSqlDoesNotBlockMysqlBeforeWorkspaceExecution() {
        // given
        JudgeQueryService judgeQueryService = createJudgeQueryService();
        when(judgeWorkspaceService.openWorkspace("tester", "M00001-00001", "socket-1", DbmsType.MYSQL))
                .thenThrow(new IllegalStateException("workspace requested"));

        // when
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> judgeQueryService.executeInteractiveSql(
                "tester",
                "socket-1",
                "M00001-00001",
                "SELECT 1",
                DbmsType.MYSQL,
                1,
                10
        ));

        // then
        assertEquals("workspace requested", exception.getMessage());
        verify(judgeWorkspaceService).openWorkspace("tester", "M00001-00001", "socket-1", DbmsType.MYSQL);
    }

    @Test
    @DisplayName("MySQL 제출은 DBMS 미지원 분기로 막지 않고 제출 기록에 MySQL을 남긴다")
    void submitProblemSqlDoesNotBlockMysqlAndKeepsMysqlHistory() {
        // given
        JudgeQueryService judgeQueryService = createJudgeQueryService();
        when(judgeWorkspaceService.openWorkspace("tester", "M00001-00001", "socket-1", DbmsType.MYSQL))
                .thenThrow(new IllegalStateException("workspace requested"));

        // when
        JudgeQueryService.ProblemSubmitResult result = judgeQueryService.submitProblemSql(
                "tester",
                "socket-1",
                "M00001-00001",
                "SELECT 1",
                DbmsType.MYSQL,
                progress -> {
                }
        );

        // then
        assertFalse(result.success());
        assertEquals("workspace requested", result.message());

        ArgumentCaptor<ProblemSubmitHistory> historyCaptor = ArgumentCaptor.forClass(ProblemSubmitHistory.class);
        verify(problemSubmitHistoryRepository).save(historyCaptor.capture());
        assertEquals(DbmsType.MYSQL, historyCaptor.getValue().getDbmsType());
        verify(judgeWorkspaceService).openWorkspace(eq("tester"), eq("M00001-00001"), eq("socket-1"), eq(DbmsType.MYSQL));
    }

    private JudgeQueryService createJudgeQueryService() {
        return new JudgeQueryService(
                judgeWorkspaceService,
                new DbmsSqlDialects(),
                problemSubmitHistoryRepository,
                problemSolveHistoryRepository,
                problemRepository,
                problemStore
        );
    }
}
