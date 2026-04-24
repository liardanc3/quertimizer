package com.quertimizer.problem.presentation.controller;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.global.constant.OracleExecutionPlanElementIndex;
import com.quertimizer.global.constant.PostgreSqlExecutionPlanElementIndex;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.infrastructure.repository.ProblemJpaRepository;
import com.quertimizer.problem.infrastructure.repository.ProblemSolveHistoryJpaRepository;
import com.quertimizer.problem.application.store.ProblemStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class ProblemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProblemJpaRepository problemRepository;

    @Autowired
    private ProblemSolveHistoryJpaRepository problemSolveHistoryRepository;

    @Autowired
    private ProblemStore problemStore;

    @Test
    @DisplayName("/problems: 메모리에 적재된 문제 목록과 유저/DBMS별 최고 제출만 페이지 응답으로 반환")
    void getProblemsFromStoreAndReturnBestSubmittedHistories() throws Exception {
        // given
        clearProblemData();

        Problem problem = problemRepository.save(
                Problem.create(
                        "00001-00001",
                        "3월 고객별 주문 건수와 총 주문 금액 조회",
                        "2024년 3월 주문 데이터를 기준으로 고객별 주문 건수와 총 주문 금액을 조회하세요."
                )
        );
        problemSolveHistoryRepository.save(
                ProblemSolveHistory.create(
                        problem.getProblemId(),
                        "problemuser1",
                        DbmsType.POSTGRESQL,
                        "select 1",
                        180,
                        0,
                        bitMask(PostgreSqlExecutionPlanElementIndex.INDEX_SCAN, PostgreSqlExecutionPlanElementIndex.NESTED_LOOP),
                        LocalDateTime.of(2026, 4, 5, 10, 0, 0)
                )
        );
        problemSolveHistoryRepository.save(
                ProblemSolveHistory.create(
                        problem.getProblemId(),
                        "problemuser1",
                        DbmsType.POSTGRESQL,
                        "select 2",
                        120,
                        0,
                        bitMask(PostgreSqlExecutionPlanElementIndex.BITMAP_INDEX_SCAN, PostgreSqlExecutionPlanElementIndex.SORT),
                        LocalDateTime.of(2026, 4, 5, 10, 5, 0)
                )
        );
        problemSolveHistoryRepository.save(
                ProblemSolveHistory.create(
                        problem.getProblemId(),
                        "problemuser1",
                        DbmsType.ORACLE,
                        "select 3",
                        150,
                        0,
                        bitMask(OracleExecutionPlanElementIndex.ACCESS_FILTER, OracleExecutionPlanElementIndex.HINT),
                        LocalDateTime.of(2026, 4, 5, 10, 10, 0)
                )
        );
        problemSolveHistoryRepository.save(
                ProblemSolveHistory.create(
                        problem.getProblemId(),
                        "problemuser2",
                        DbmsType.POSTGRESQL,
                        "select 4",
                        90,
                        0,
                        bitMask(PostgreSqlExecutionPlanElementIndex.FULL_SCAN, PostgreSqlExecutionPlanElementIndex.HASH_JOIN),
                        LocalDateTime.of(2026, 4, 5, 10, 20, 0)
                )
        );
        problemSolveHistoryRepository.save(
                ProblemSolveHistory.create(
                        problem.getProblemId(),
                        "problemuser2",
                        DbmsType.POSTGRESQL,
                        "select 5",
                        90,
                        0,
                        bitMask(PostgreSqlExecutionPlanElementIndex.SEQ_SCAN, PostgreSqlExecutionPlanElementIndex.HASH_AGGREGATE),
                        LocalDateTime.of(2026, 4, 5, 10, 25, 0)
                )
        );

        // ProblemStore를 새로 적재한 뒤에는 이후 DB 변경사항이 바로 반영되지 않습니다.
        problemStore.loadProblems();

        problemRepository.save(
                Problem.create(
                        "00001-00002",
                        "스토어에 아직 없는 문제",
                        "메모리 적재 이후에 추가된 문제입니다."
                )
        );

        // when & then
        mockMvc.perform(get("/problems"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.spreadRateMin").value(25.0))
                .andExpect(jsonPath("$.spreadRateMax").value(25.0))
                .andExpect(jsonPath("$.problems.length()").value(1))
                .andExpect(jsonPath("$.problems[0].problemId").value("00001-00001"))
                .andExpect(jsonPath("$.problems[0].title").value("3월 고객별 주문 건수와 총 주문 금액 조회"))
                .andExpect(jsonPath("$.problems[0].submittedHistories.length()").value(3))
                .andExpect(jsonPath("$.problems[0].submittedHistories[0].dbms").value("postgresql"))
                .andExpect(jsonPath("$.problems[0].submittedHistories[0].handle").value("problemuser2"))
                .andExpect(jsonPath("$.problems[0].submittedHistories[0].executionTimeMs").value(90))
                .andExpect(jsonPath("$.problems[0].submittedHistories[0].executionPlanElement")
                        .value(bitMask(PostgreSqlExecutionPlanElementIndex.FULL_SCAN, PostgreSqlExecutionPlanElementIndex.HASH_JOIN)))
                .andExpect(jsonPath("$.problems[0].submittedHistories[1].dbms").value("postgresql"))
                .andExpect(jsonPath("$.problems[0].submittedHistories[1].handle").value("problemuser1"))
                .andExpect(jsonPath("$.problems[0].submittedHistories[1].executionTimeMs").value(120))
                .andExpect(jsonPath("$.problems[0].submittedHistories[1].executionPlanElement")
                        .value(bitMask(PostgreSqlExecutionPlanElementIndex.BITMAP_INDEX_SCAN, PostgreSqlExecutionPlanElementIndex.SORT)))
                .andExpect(jsonPath("$.problems[0].submittedHistories[2].dbms").value("oracle"))
                .andExpect(jsonPath("$.problems[0].submittedHistories[2].handle").value("problemuser1"))
                .andExpect(jsonPath("$.problems[0].submittedHistories[2].executionTimeMs").value(150))
                .andExpect(jsonPath("$.problems[0].submittedHistories[2].executionPlanElement")
                        .value(bitMask(OracleExecutionPlanElementIndex.ACCESS_FILTER, OracleExecutionPlanElementIndex.HINT)));
    }

    @Test
    @DisplayName("/problems?page=2: 문제 목록을 20개 단위로 페이징합니다")
    void getProblemsAndApplyPaging() throws Exception {
        // given
        clearProblemData();

        IntStream.rangeClosed(1, 21).forEach(index ->
                problemRepository.save(
                        Problem.create(
                                "00001-" + String.format("%05d", index),
                                "문제 " + index,
                                "설명 " + index
                        )
                )
        );

        problemStore.loadProblems();

        // when & then
        mockMvc.perform(get("/problems").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(2))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalCount").value(21))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.spreadRateMin").value(0.0))
                .andExpect(jsonPath("$.spreadRateMax").value(0.0))
                .andExpect(jsonPath("$.problems.length()").value(1))
                .andExpect(jsonPath("$.problems[0].problemId").value("00001-00021"));
    }

    private void clearProblemData() {
        problemSolveHistoryRepository.deleteAllInBatch();
        problemRepository.deleteAllInBatch();
    }

    private long bitMask(int... indexes) {
        long result = 0L;
        for (int index : indexes) {
            result += 1L << index;
        }
        return result;
    }

}
