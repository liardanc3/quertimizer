package com.quertimizer.server.store;

import com.quertimizer.constant.DbmsType;
import com.quertimizer.constant.PostgreSqlExecutionPlanElementIndex;
import com.quertimizer.entity.Problem;
import com.quertimizer.entity.ProblemSolveHistory;
import com.quertimizer.repository.ProblemRepository;
import com.quertimizer.repository.ProblemSolveHistoryRepository;
import com.quertimizer.store.ProblemStore;
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
    private ProblemRepository problemRepository;

    @Mock
    private ProblemSolveHistoryRepository problemSolveHistoryRepository;

    @Test
    @DisplayName("findProblemPage : 문제 목록 필터링 + 유저별 최고 제출 유지")
    void findProblemPageAndKeepBestSubmittedHistories() {
        // given
        Problem secondProblem = Problem.create("00001-00002", "두 번째 문제", "설명 2");
        Problem firstProblem = Problem.create("00001-00001", "첫 번째 문제", "설명 1");

        when(problemRepository.findAll()).thenReturn(List.of(secondProblem, firstProblem));
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
        ProblemStore.ProblemPage problemPage = problemStore.findProblemPage(1, null, "all", "tester-a", false);

        // then
        assertEquals(2, problemPage.totalCount());
        assertEquals(List.of("00001-00001", "00001-00002"), problemPage.problems().stream()
                .map(problemEntry -> problemEntry.problem().getProblemId())
                .toList());
        assertEquals(2, problemPage.problems().get(0).solvedUserCount());
        assertEquals(2, problemPage.problems().get(0).submittedHistories().size());
        assertEquals(110, problemPage.problems().get(0).submittedHistories().get(1).getExecutionTimeMs());
    }

    private long maskOf(int... indexes) {
        long mask = 0L;
        for (int index : indexes) {
            mask += 1L << index;
        }
        return mask;
    }

}
