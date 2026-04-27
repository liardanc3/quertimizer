package com.quertimizer.problem.infrastructure.mock;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.global.constant.MySqlExecutionPlanElementIndex;
import com.quertimizer.global.constant.PostgreSqlExecutionPlanElementIndex;
import com.quertimizer.problem.application.port.ProblemRepository;
import com.quertimizer.problem.application.port.ProblemSolveHistoryRepository;
import com.quertimizer.problem.application.port.ProblemSubmitHistoryRepository;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import com.quertimizer.user.application.port.UserRepository;
import com.quertimizer.user.domain.entity.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component("problemSubmitHistoryMockData")
@DependsOn({"problemMockData", "userMockData"})
@RequiredArgsConstructor
public class ProblemSubmitHistoryMockData {

    private static final int TARGET_SUBMISSIONS_PER_PROBLEM = 1_100;
    private static final LocalDateTime BASE_SUBMITTED_AT = LocalDateTime.of(2026, 1, 1, 9, 0);

    private final ProblemRepository problemRepository;
    private final ProblemSubmitHistoryRepository problemSubmitHistoryRepository;
    private final ProblemSolveHistoryRepository problemSolveHistoryRepository;
    private final UserRepository userRepository;

    @PostConstruct
    public void seed() {
        // 문제별 제출 이력 수를 기준으로 부족한 Mock 데이터를 채운다.
        Map<String, Long> submittedCountsByProblemId = problemSubmitHistoryRepository.findAll().stream()
                .collect(Collectors.groupingBy(ProblemSubmitHistory::getProblemId, Collectors.counting()));
        List<String> handles = userRepository.findAllByOrderByHandleAsc().stream()
                .map(User::getHandle)
                .filter(handle -> handle != null && !handle.isBlank())
                .toList();

        for (Problem problem : problemRepository.findAll()) {
            saveProblemSubmitHistories(problem, handles, submittedCountsByProblemId.getOrDefault(problem.getProblemId(), 0L));
        }
    }

    private void saveProblemSubmitHistories(Problem problem, List<String> handles, long submittedCount) {
        // 목표 제출 수를 이미 만족한 문제는 추가 적재하지 않는다.
        if (handles.isEmpty() || submittedCount >= TARGET_SUBMISSIONS_PER_PROBLEM) {
            return;
        }

        // 부족한 제출 이력과 유저별 최고 정답 이력을 함께 적재한다.
        int remainingCount = (int) (TARGET_SUBMISSIONS_PER_PROBLEM - submittedCount);
        Map<String, BestSolvedSubmission> bestSolvedSubmissions = new LinkedHashMap<>();
        for (int index = 0; index < remainingCount; index++) {
            int attempt = (int) submittedCount + index + 1;
            String handle = handles.get(index % handles.size());
            ProblemSubmitHistory submitHistory = createProblemSubmitHistory(problem, handle, attempt, index, index / handles.size());
            problemSubmitHistoryRepository.save(submitHistory);

            if (submitHistory.isSuccess()) {
                bestSolvedSubmissions.merge(handle, BestSolvedSubmission.from(submitHistory), BestSolvedSubmission::chooseBetter);
            }
        }

        // 유저별 최고 정답 제출 이력을 문제 풀이 이력으로 저장한다.
        bestSolvedSubmissions.values().forEach(bestSolvedSubmission ->
                problemSolveHistoryRepository.save(bestSolvedSubmission.toProblemSolveHistory())
        );
    }

    private ProblemSubmitHistory createProblemSubmitHistory(Problem problem, String handle, int attempt, int sequence, int handleRound) {
        // 제출 이력 Mock 데이터 생성
        DbmsType dbmsType = problem.getDbmsType();
        boolean success = isSuccessSubmit(attempt, sequence, handleRound);
        long executionPlanElement = createExecutionPlanElement(dbmsType, attempt, sequence);
        double cost = createCost(success, attempt, sequence);
        long executionTimeMs = createExecutionTimeMs(success, attempt, sequence);
        long rowCount = success ? 5 + attempt % 20 : attempt % 3;

        return ProblemSubmitHistory.create(
                problem.getProblemId(), handle, dbmsType, createSubmittedSql(problem.getProblemId(), handle, attempt, success),
                success, success ? "정답" : "오답", executionTimeMs, cost, rowCount, executionPlanElement,
                BASE_SUBMITTED_AT.plusMinutes(sequence)
        );
    }

    private boolean isSuccessSubmit(int attempt, int sequence, int handleRound) {
        // 유저별 오답 제출이 섞이도록 성공 여부를 분산한다.
        return (attempt + handleRound) % 5 != 0 && (sequence + handleRound) % 7 != 0;
    }

    private double createCost(boolean success, int attempt, int sequence) {
        // 정답과 오답 제출의 Cost 범위를 다르게 분산한다.
        double baseCost = success ? 0.2 : 1.4;
        double cost = baseCost + (attempt % 13) * 0.17 + (sequence % 11) * 0.03;
        return Math.round(cost * 100.0) / 100.0;
    }

    private long createExecutionTimeMs(boolean success, int attempt, int sequence) {
        // 실행시간 Mock 데이터 생성
        long baseTime = success ? 35 : 90;
        return baseTime + (attempt % 80) * 3L + (sequence % 17);
    }

    private String createSubmittedSql(String problemId, String handle, int attempt, boolean success) {
        // 제출 SQL Mock 데이터 생성
        if (!success) {
            return "SELECT customer_id FROM customers WHERE customer_id = %d; -- %s %s".formatted(attempt, problemId, handle);
        }

        return """
                SELECT c.customer_id, c.customer_name, COUNT(*) AS order_count
                FROM customers c
                JOIN orders o ON o.customer_id = c.customer_id
                WHERE o.ordered_at >= '2024-03-01'
                GROUP BY c.customer_id, c.customer_name
                ORDER BY order_count DESC;
                -- %s %s %d
                """.formatted(problemId, handle, attempt);
    }

    private long createExecutionPlanElement(DbmsType dbmsType, int attempt, int sequence) {
        // DBMS별 실행계획 요소 Mock 데이터 생성
        if (dbmsType == DbmsType.MYSQL) {
            return createMySqlExecutionPlanElement(attempt, sequence);
        }

        return createPostgreSqlExecutionPlanElement(attempt, sequence);
    }

    private long createPostgreSqlExecutionPlanElement(int attempt, int sequence) {
        // PostgreSQL 실행계획 요소 조합 생성
        long element = switch (attempt % 6) {
            case 0 -> bit(PostgreSqlExecutionPlanElementIndex.FULL_SCAN) | bit(PostgreSqlExecutionPlanElementIndex.SEQ_SCAN);
            case 1 -> bit(PostgreSqlExecutionPlanElementIndex.INDEX_SCAN);
            case 2 -> bit(PostgreSqlExecutionPlanElementIndex.BITMAP_INDEX_SCAN) | bit(PostgreSqlExecutionPlanElementIndex.BITMAP_HEAP_SCAN);
            case 3 -> bit(PostgreSqlExecutionPlanElementIndex.TID_SCAN);
            case 4 -> bit(PostgreSqlExecutionPlanElementIndex.INDEX_ONLY_SCAN);
            default -> bit(PostgreSqlExecutionPlanElementIndex.SUBQUERY_SCAN);
        };

        element |= switch ((attempt + sequence) % 4) {
            case 0 -> bit(PostgreSqlExecutionPlanElementIndex.NESTED_LOOP);
            case 1 -> bit(PostgreSqlExecutionPlanElementIndex.HASH_JOIN);
            case 2 -> bit(PostgreSqlExecutionPlanElementIndex.MERGE_JOIN);
            default -> 0L;
        };
        element |= attempt % 3 == 0 ? bit(PostgreSqlExecutionPlanElementIndex.FILTER) : 0L;
        element |= attempt % 4 == 0 ? bit(PostgreSqlExecutionPlanElementIndex.SORT) : 0L;
        element |= attempt % 6 == 0 ? bit(PostgreSqlExecutionPlanElementIndex.INCREMENTAL_SORT) : 0L;
        element |= attempt % 5 == 0 ? bit(PostgreSqlExecutionPlanElementIndex.HASH_AGGREGATE) : 0L;
        element |= attempt % 8 == 0 ? bit(PostgreSqlExecutionPlanElementIndex.GROUP_AGGREGATE) : 0L;
        element |= attempt % 9 == 0 ? bit(PostgreSqlExecutionPlanElementIndex.HINT) : 0L;
        return element;
    }

    private long createMySqlExecutionPlanElement(int attempt, int sequence) {
        // MySQL 실행계획 요소 조합 생성
        long element = switch (attempt % 6) {
            case 0 -> bit(MySqlExecutionPlanElementIndex.FULL_TABLE_SCAN);
            case 1 -> bit(MySqlExecutionPlanElementIndex.INDEX_SCAN);
            case 2 -> bit(MySqlExecutionPlanElementIndex.RANGE_SCAN);
            case 3 -> bit(MySqlExecutionPlanElementIndex.REF_SCAN);
            case 4 -> bit(MySqlExecutionPlanElementIndex.EQ_REF_SCAN);
            default -> bit(MySqlExecutionPlanElementIndex.DERIVED_TABLE);
        };

        element |= switch ((attempt + sequence) % 4) {
            case 0 -> bit(MySqlExecutionPlanElementIndex.NESTED_LOOP_JOIN);
            case 1 -> bit(MySqlExecutionPlanElementIndex.HASH_JOIN);
            case 2 -> bit(MySqlExecutionPlanElementIndex.USING_JOIN_BUFFER);
            default -> 0L;
        };
        element |= attempt % 3 == 0 ? bit(MySqlExecutionPlanElementIndex.INDEX_CONDITION) : 0L;
        element |= attempt % 4 == 0 ? bit(MySqlExecutionPlanElementIndex.ATTACHED_CONDITION) : 0L;
        element |= attempt % 5 == 0 ? bit(MySqlExecutionPlanElementIndex.FILESORT) : 0L;
        element |= attempt % 7 == 0 ? bit(MySqlExecutionPlanElementIndex.TEMPORARY_TABLE) : 0L;
        element |= attempt % 8 == 0 ? bit(MySqlExecutionPlanElementIndex.GROUPING_OPERATION) : 0L;
        element |= attempt % 9 == 0 ? bit(MySqlExecutionPlanElementIndex.AGGREGATE) : 0L;
        element |= attempt % 10 == 0 ? bit(MySqlExecutionPlanElementIndex.HINT) : 0L;
        return element;
    }

    private long bit(int index) {
        // 실행계획 요소 비트 생성
        return 1L << index;
    }

    private record BestSolvedSubmission(String problemId,
                                        String handle,
                                        DbmsType dbmsType,
                                        String submittedSql,
                                        long executionTimeMs,
                                        double cost,
                                        long rowCount,
                                        long executionPlanElement,
                                        LocalDateTime submittedAt) {

        private static BestSolvedSubmission from(ProblemSubmitHistory submitHistory) {
            // 정답 제출 이력 기반 최고 기록 후보 생성
            return new BestSolvedSubmission(
                    submitHistory.getProblemId(), submitHistory.getHandle(), submitHistory.getDbmsType(),
                    submitHistory.getSubmittedSql(), submitHistory.getExecutionTimeMs(), submitHistory.getCost(),
                    submitHistory.getRowCount(), submitHistory.getExecutionPlanElement(), submitHistory.getSubmittedAt()
            );
        }

        private static BestSolvedSubmission chooseBetter(BestSolvedSubmission current, BestSolvedSubmission candidate) {
            // Cost가 낮거나 실행시간이 짧은 제출을 최고 기록으로 선택
            if (candidate.cost < current.cost || (candidate.cost == current.cost && candidate.executionTimeMs < current.executionTimeMs)) {
                return candidate;
            }

            return current;
        }

        private ProblemSolveHistory toProblemSolveHistory() {
            // 최고 정답 제출 이력을 문제 풀이 이력으로 변환
            return ProblemSolveHistory.create(
                    problemId, handle, dbmsType, submittedSql, executionTimeMs,
                    cost, rowCount, executionPlanElement, submittedAt
            );
        }
    }
}
