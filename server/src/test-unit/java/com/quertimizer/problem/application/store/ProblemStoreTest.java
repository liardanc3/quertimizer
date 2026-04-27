package com.quertimizer.problem.application.store;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.global.constant.PostgreSqlExecutionPlanElementIndex;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.infrastructure.repository.ProblemJpaRepository;
import com.quertimizer.problem.infrastructure.repository.ProblemSetJpaRepository;
import com.quertimizer.problem.infrastructure.repository.ProblemSolveHistoryJpaRepository;
import com.quertimizer.problem.application.store.ProblemStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemStoreTest {

    @InjectMocks
    private ProblemStore problemStore;

    @Mock
    private ProblemJpaRepository problemRepository;

    @Mock
    private ProblemSolveHistoryJpaRepository problemSolveHistoryRepository;

    @Mock
    private ProblemSetJpaRepository problemSetRepository;

    @Test
    @DisplayName("findProblemPage : 문제 목록 필터링 + 유저별 최고 제출 유지")
    void findProblemPageAndKeepBestSubmittedHistories() {
        // given
        Problem secondProblem = Problem.create("00001-00002", "두 번째 문제", "설명 2", DbmsType.POSTGRESQL);
        Problem firstProblem = Problem.create("00001-00001", "첫 번째 문제", "설명 1", DbmsType.POSTGRESQL);

        when(problemRepository.findAll()).thenReturn(List.of(secondProblem, firstProblem));
        when(problemSetRepository.findAll()).thenReturn(List.of());
        when(problemSolveHistoryRepository.findAll()).thenReturn(List.of(
                ProblemSolveHistory.create(
                        "00001-00001",
                        "tester-a",
                        DbmsType.POSTGRESQL,
                        "select 1",
                        120,
                        0,
                        maskOf(PostgreSqlExecutionPlanElementIndex.FULL_SCAN, PostgreSqlExecutionPlanElementIndex.HINT),
                        LocalDateTime.now()
                ),
                ProblemSolveHistory.create(
                        "00001-00001",
                        "tester-b",
                        DbmsType.POSTGRESQL,
                        "select 1",
                        140,
                        0,
                        maskOf(PostgreSqlExecutionPlanElementIndex.INDEX_SCAN),
                        LocalDateTime.now()
                ),
                ProblemSolveHistory.create(
                        "00001-00001",
                        "tester-b",
                        DbmsType.POSTGRESQL,
                        "select 1",
                        110,
                        0,
                        maskOf(PostgreSqlExecutionPlanElementIndex.INDEX_SCAN, PostgreSqlExecutionPlanElementIndex.SORT),
                        LocalDateTime.now()
                )
        ));

        // when
        problemStore.loadProblems();
        ProblemStore.ProblemPage problemPage = problemStore.findProblemPage(1, null, "all", "tester-a", false, "none", null, null);

        // then
        assertEquals(2, problemPage.totalCount());
        assertEquals(0.0, problemPage.spreadRateMin());
        assertEquals(0.0, problemPage.spreadRateMax());
        assertEquals(List.of("00001-00001", "00001-00002"), problemPage.problems().stream()
                .map(problemEntry -> problemEntry.problem().getProblemId())
                .toList());
        assertEquals(2, problemPage.problems().get(0).solvedUserCount());
        assertEquals(2, problemPage.problems().get(0).submittedHistories().size());
        assertEquals(110, problemPage.problems().get(0).submittedHistories().get(0).getExecutionTimeMs());
    }

    @Test
    @DisplayName("findProblemPage : 속도 편차 범위 필터와 정렬을 적용한다")
    void findProblemPageAndApplySpreadRateFilterAndSort() {
        // given
        Problem highSpreadProblem = Problem.create("00001-00002", "편차 큰 문제", "설명 2", DbmsType.POSTGRESQL);
        Problem lowSpreadProblem = Problem.create("00001-00001", "편차 작은 문제", "설명 1", DbmsType.POSTGRESQL);

        when(problemRepository.findAll()).thenReturn(List.of(highSpreadProblem, lowSpreadProblem));
        when(problemSetRepository.findAll()).thenReturn(List.of());
        when(problemSolveHistoryRepository.findAll()).thenReturn(List.of(
                ProblemSolveHistory.create(
                        "00001-00001",
                        "tester-a",
                        DbmsType.POSTGRESQL,
                        "select 1",
                        100,
                        0,
                        maskOf(PostgreSqlExecutionPlanElementIndex.INDEX_SCAN),
                        LocalDateTime.now()
                ),
                ProblemSolveHistory.create(
                        "00001-00001",
                        "tester-b",
                        DbmsType.POSTGRESQL,
                        "select 1",
                        105,
                        0,
                        maskOf(PostgreSqlExecutionPlanElementIndex.INDEX_SCAN),
                        LocalDateTime.now()
                ),
                ProblemSolveHistory.create(
                        "00001-00001",
                        "tester-c",
                        DbmsType.POSTGRESQL,
                        "select 1",
                        110,
                        0,
                        maskOf(PostgreSqlExecutionPlanElementIndex.INDEX_SCAN),
                        LocalDateTime.now()
                ),
                ProblemSolveHistory.create(
                        "00001-00002",
                        "tester-a",
                        DbmsType.POSTGRESQL,
                        "select 1",
                        100,
                        0,
                        maskOf(PostgreSqlExecutionPlanElementIndex.FULL_SCAN),
                        LocalDateTime.now()
                ),
                ProblemSolveHistory.create(
                        "00001-00002",
                        "tester-b",
                        DbmsType.POSTGRESQL,
                        "select 1",
                        200,
                        0,
                        maskOf(PostgreSqlExecutionPlanElementIndex.FULL_SCAN),
                        LocalDateTime.now()
                ),
                ProblemSolveHistory.create(
                        "00001-00002",
                        "tester-c",
                        DbmsType.POSTGRESQL,
                        "select 1",
                        300,
                        0,
                        maskOf(PostgreSqlExecutionPlanElementIndex.FULL_SCAN),
                        LocalDateTime.now()
                )
        ));

        // when
        problemStore.loadProblems();
        ProblemStore.ProblemPage problemPage = problemStore.findProblemPage(1, null, "all", null, false, "desc", 10d, 60d);

        // then
        assertEquals(1, problemPage.totalCount());
        assertEquals(4.8, problemPage.spreadRateMin());
        assertEquals(50.0, problemPage.spreadRateMax());
        assertEquals(List.of("00001-00002"), problemPage.problems().stream()
                .map(problemEntry -> problemEntry.problem().getProblemId())
                .toList());
        assertEquals(50.0, problemPage.problems().get(0).spreadRate());
    }

    private long maskOf(int... indexes) {
        long mask = 0L;
        for (int index : indexes) {
            mask += 1L << index;
        }
        return mask;
    }

}
