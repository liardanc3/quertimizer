package com.quertimizer.problem.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.problem.application.port.in.ExecuteProblemSqlUseCase;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.problem.application.input.ProblemExecutionInput;
import com.quertimizer.problem.application.output.ProblemExecutionProgress;
import com.quertimizer.problem.application.output.ProblemExecutionOutput;
import com.quertimizer.problem.application.output.ProblemJudgeExecutionResult;
import com.quertimizer.problem.application.output.ProblemSqlStatement;
import com.quertimizer.problem.application.port.out.ProblemJudgePort;
import com.quertimizer.problem.domain.model.ProblemJudgeExecutionMode;
import com.quertimizer.problem.domain.model.ProblemQueryResultText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.quertimizer.problem.domain.model.ProblemExecutionPageConstant.DEFAULT_PAGE_SIZE;
import static com.quertimizer.problem.domain.model.ProblemExecutionPageConstant.MAX_PAGE_SIZE;
import static com.quertimizer.problem.domain.model.ProblemQueryFailReason.EXECUTE_SQL_WITH_INDEX_DDL_SINGLE_ONLY;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecuteProblemSql implements ExecuteProblemSqlUseCase {

    private final ProblemJudgePort problemJudgePort;
    private final ProblemDatasetResolver datasetResolver;
    private final ProblemExecutionSessionStore executionSessionStore;

    /**
     * SQL을 실행한다.
     *
     * <ol>
     *   <li>실행 SQL 분리와 데이터셋 조회
     *   <li>새 실행 환경 생성
     *   <li>judge SQL 실행
     *   <li>문제 실행 응답 변환
     *   <li>실행 환경 정리
     * </ol>
     *
     * @param input SQL 실행 입력
     * @return SQL 실행 결과
     */
    @Override
    @Log("SQL 실행")
    public ProblemExecutionOutput execute(ProblemExecutionInput input) {
        long startedAt = System.nanoTime();
        String executionId = "interactive-" + UUID.randomUUID();
        log.info(
                "[SQL 실행] 시작 problem={} session={} execution={} page={} pageSize={} sqlLength={} indexDdlCount={}",
                input.getProblemId(), input.getExecutionSessionId(), executionId,
                normalizePage(input.getPage()), normalizePageSize(input.getPageSize()),
                resolveLength(input.getSql()), resolveSize(input.getIndexSqls())
        );
        executionSessionStore.markExecution(input.getExecutionSessionId(), executionId);
        ProblemExecutionSessionStore.ProblemExecutionSession session = null;
        try {
            ResolvedExecutionSql executionSql = resolveExecutionSql(input);
            ProblemDatasetResolver.ResolvedProblemDataset dataset =
                    datasetResolver.resolve(input.getProblemId(), input.getDbmsType());
            log.info(
                    "[SQL 실행] 데이터셋 조회 완료 problem={} dataset={} dbms={}",
                    input.getProblemId(), dataset.getDatasetId(), dataset.getDbmsType()
            );
            session = createFreshEnvironment(input, dataset);
            applyIndexSqls(executionSql.indexSqls, session, executionId);
            analyzeExecutionEnvironment(session, executionId);
            long selectStartedAt = System.nanoTime();
            log.info(
                    "[SQL 실행] SELECT 실행 시작 problem={} execution={} environment={} page={} pageSize={}",
                    input.getProblemId(), executionId, session.getEnvironmentId(),
                    normalizePage(input.getPage()), normalizePageSize(input.getPageSize())
            );
            ProblemJudgeExecutionResult result = problemJudgePort.executeInteractiveSql(
                    executionId, session.getEnvironmentId(), executionSql.sql,
                    normalizePage(input.getPage()), normalizePageSize(input.getPageSize())
            );
            log.info(
                    "[SQL 실행] SELECT 실행 완료 problem={} execution={} environment={} mode={} rowCount={} cost={} executionTimeMs={} elapsedMs={} totalElapsedMs={}",
                    input.getProblemId(), executionId, session.getEnvironmentId(), result.getMode(),
                    result.getRowCount(), result.getCost(), result.getExecutionTimeMs(),
                    elapsedMillis(selectStartedAt), elapsedMillis(startedAt)
            );

            return toProblemExecutionOutput(input.getProblemId(), result);
        } catch (IllegalArgumentException exception) {
            log.warn(
                    "[SQL 실행] 검증 실패 problem={} execution={} reason={} elapsedMs={}",
                    input.getProblemId(), executionId, resolveUserErrorMessage(exception), elapsedMillis(startedAt)
            );
            throw new BusinessException(resolveUserErrorMessage(exception), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException exception) {
            String message = ProblemSqlErrorMessageResolver.resolveUserSqlMessage(exception);
            if (message == null) {
                throw exception;
            }

            log.warn(
                    "[SQL 실행] DB 오류 problem={} execution={} reason={} elapsedMs={}",
                    input.getProblemId(), executionId, message, elapsedMillis(startedAt), exception
            );
            throw new BusinessException(message, HttpStatus.BAD_REQUEST);
        } finally {
            executionSessionStore.clearExecution(input.getExecutionSessionId(), executionId);
            if (session != null) {
                cleanupEnvironment(input.getExecutionSessionId(), session);
            }
        }
    }

    private ResolvedExecutionSql resolveExecutionSql(ProblemExecutionInput input) {
        // 실행 SQL 문장 분리와 기존 index DDL 목록 준비
        List<ProblemSqlStatement> statements = problemJudgePort.parseStatements(input.getSql());
        if (statements.isEmpty()) {
            throw new IllegalArgumentException(ProblemQueryResultText.PROBLEM_EXECUTION_FAILED.getText());
        }

        // 단일 문장이면 별도 index DDL 목록만 유지
        if (statements.size() == 1) {
            ProblemSqlStatement statement = statements.get(0);
            return new ResolvedExecutionSql(statement.getSql(), input.getIndexSqls());
        }

        // 마지막 실행문 이전의 INDEX DDL을 실행 환경 선반영 대상으로 분리
        List<String> indexSqls = new ArrayList<>(input.getIndexSqls());
        for (int statementIndex = 0; statementIndex < statements.size() - 1; statementIndex++) {
            ProblemSqlStatement statement = statements.get(statementIndex);
            if (statement.getMode() != ProblemJudgeExecutionMode.INDEX_COMMAND) {
                throw new IllegalArgumentException(EXECUTE_SQL_WITH_INDEX_DDL_SINGLE_ONLY.getMessage());
            }
            indexSqls.add(statement.getSql());
        }

        // 마지막 문장을 실제 실행 대상으로 반환
        ProblemSqlStatement statement = statements.get(statements.size() - 1);
        return new ResolvedExecutionSql(statement.getSql(), indexSqls);
    }

    private void applyIndexSqls(List<String> indexSqls,
                                ProblemExecutionSessionStore.ProblemExecutionSession session,
                                String executionId) {
        // 실행 전 반영할 index DDL 없으면 생략
        if (indexSqls.isEmpty()) {
            log.info(
                    "[SQL 실행] 인덱스 DDL 반영 생략 execution={} environment={}",
                    executionId, session.getEnvironmentId()
            );
            return;
        }

        // 같은 실행 환경에 index DDL 순차 반영
        long startedAt = System.nanoTime();
        log.info(
                "[SQL 실행] 인덱스 DDL 반영 시작 execution={} environment={} indexDdlCount={}",
                executionId, session.getEnvironmentId(), indexSqls.size()
        );
        for (String indexSql : indexSqls) {
            log.info(
                    "[SQL 실행] 인덱스 DDL 실행 시작 execution={} environment={} sqlLength={}",
                    executionId, session.getEnvironmentId(), indexSql.length()
            );
            problemJudgePort.executeInteractiveSql(executionId, session.getEnvironmentId(), indexSql, 1, 1);
        }
        log.info(
                "[SQL 실행] 인덱스 DDL 반영 완료 execution={} environment={} elapsedMs={}",
                executionId, session.getEnvironmentId(), elapsedMillis(startedAt)
        );
    }

    private void analyzeExecutionEnvironment(ProblemExecutionSessionStore.ProblemExecutionSession session,
                                             String executionId) {
        // 실행 직전 통계 1회 갱신
        long startedAt = System.nanoTime();
        log.info(
                "[SQL 실행] 통계 갱신 시작 execution={} environment={}",
                executionId, session.getEnvironmentId()
        );
        problemJudgePort.analyzeOfficialEnvironment(executionId, session.getEnvironmentId());
        log.info(
                "[SQL 실행] 통계 갱신 완료 execution={} environment={} elapsedMs={}",
                executionId, session.getEnvironmentId(), elapsedMillis(startedAt)
        );
    }

    private synchronized ProblemExecutionSessionStore.ProblemExecutionSession createFreshEnvironment(
            ProblemExecutionInput input, ProblemDatasetResolver.ResolvedProblemDataset dataset) {
        // 같은 실행 세션에 남아 있는 이전 실행 환경 선제 정리
        executionSessionStore.remove(input.getExecutionSessionId())
                .ifPresent(session -> {
                    log.info(
                            "[SQL 실행] 이전 환경 정리 시작 session={} environment={}",
                            input.getExecutionSessionId(), session.getEnvironmentId()
                    );
                    dropQuietly(input.getExecutionSessionId(), session.getEnvironmentId());
                });

        // 데이터셋 기준 새 judge 실행 환경 생성
        long startedAt = System.nanoTime();
        log.info(
                "[SQL 실행] 실행 환경 생성 시작 session={} problem={} dataset={}",
                input.getExecutionSessionId(), input.getProblemId(), dataset.getDatasetId()
        );
        String environmentId = problemJudgePort.createInteractiveEnvironment(
                dataset.getDatasetId(),
                remainingTasks -> sendWaitingProgress(input, remainingTasks),
                detail -> sendRunningProgress(input)
        );
        ProblemExecutionSessionStore.ProblemExecutionSession session =
                executionSessionStore.save(input.getExecutionSessionId(), input.getProblemId(), dataset.getDatasetId(), environmentId);
        log.info(
                "[SQL 실행] 실행 환경 생성 완료 session={} environment={} dataset={} elapsedMs={}",
                input.getExecutionSessionId(), environmentId, dataset.getDatasetId(), elapsedMillis(startedAt)
        );
        return session;
    }

    private void sendWaitingProgress(ProblemExecutionInput input, int remainingTasks) {
        // 실행 환경 대기열 순번 progress와 로그 전파
        log.info(
                "[SQL 실행] 실행 환경 생성 대기 session={} problem={} remainingTasks={}",
                input.getExecutionSessionId(), input.getProblemId(), remainingTasks
        );
        input.getProgressListener().accept(ProblemExecutionProgress.waiting(input.getProblemId(), remainingTasks));
    }

    private void sendRunningProgress(ProblemExecutionInput input) {
        // 실행 환경 생성 시작 이후 SQL 실행 중 progress 전파
        input.getProgressListener().accept(ProblemExecutionProgress.running(input.getProblemId()));
    }

    private void cleanupEnvironment(String executionSessionId,
                                    ProblemExecutionSessionStore.ProblemExecutionSession session) {
        // 실행 완료 후 세션 상태 제거와 judge 실행 환경 반납
        executionSessionStore.remove(executionSessionId, session);
        dropQuietly(executionSessionId, session.getEnvironmentId());
    }

    private ProblemExecutionOutput toProblemExecutionOutput(String problemId, ProblemJudgeExecutionResult result) {
        // judge 실행 결과를 문제 실행 응답 모델에 맞춤
        return new ProblemExecutionOutput(
                problemId, toResponseMode(result.getMode()), toResponseMessage(result.getMode()),
                result.getColumns(), result.getRows(), result.getPlanLines(),
                result.getRowCount(), result.getCurrentPage(), result.getPageSize(),
                result.getExecutionTimeMs() != null ? result.getExecutionTimeMs() : 0L,
                toDouble(result.getCost())
        );
    }

    private String toResponseMode(ProblemJudgeExecutionMode mode) {
        // 문제 실행 응답 모드 문자열 변환
        return switch (mode) {
            case SELECT -> "select";
            case EXPLAIN -> "explain";
            case EXPLAIN_ANALYZE -> "explain_analyze";
            case ANALYZE -> "analyze";
            case INDEX_COMMAND -> "index";
            case COMMAND -> "command";
        };
    }

    private String toResponseMessage(ProblemJudgeExecutionMode mode) {
        // 문제 실행 응답 사용자 메시지 변환
        return switch (mode) {
            case SELECT -> ProblemQueryResultText.SELECT_RESULT_RETURNED.getText();
            case EXPLAIN, EXPLAIN_ANALYZE -> ProblemQueryResultText.PLAN_RESULT_RETURNED.getText();
            case ANALYZE, INDEX_COMMAND, COMMAND -> ProblemQueryResultText.COMMAND_EXECUTED.getText();
        };
    }

    private int normalizePage(Integer page) {
        // 페이지 번호 기본값 정리
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        // 인터랙티브 실행 결과 청크 크기 기본값 적용
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }

        // 실행 결과 청크 크기 상한 적용
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private void dropQuietly(String executionSessionId, String environmentId) {
        // 정리 실패 시 세션 종료 흐름 보호용 로그 기록
        try {
            problemJudgePort.dropEnvironment(environmentId);
            log.info(
                    "[SQL 실행] 실행 환경 정리 완료 session={} environment={}",
                    executionSessionId, environmentId
            );
        } catch (Exception exception) {
            log.warn("[SQL 실행] 실행 환경 정리 실패 session={} environment={}", executionSessionId, environmentId, exception);
        }
    }

    private int resolveLength(String value) {
        // 로그 표시용 문자열 길이 계산
        return value == null ? 0 : value.length();
    }

    private int resolveSize(List<?> values) {
        // 로그 표시용 목록 크기 계산
        return values == null ? 0 : values.size();
    }

    private long elapsedMillis(long startedAt) {
        // 로그 표시용 경과 시간 계산
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private Double toDouble(BigDecimal value) {
        // 응답 DTO용 Double 비용 값 변환
        return value != null ? value.doubleValue() : null;
    }

    private String resolveUserErrorMessage(Exception exception) {
        // 사용자에게 전달할 검증 오류 메시지 조회
        return exception.getMessage() != null && !exception.getMessage().isBlank()
                ? exception.getMessage()
                : ProblemQueryResultText.PROBLEM_EXECUTION_FAILED.getText();
    }

    private static final class ResolvedExecutionSql {

        private final String sql;
        private final List<String> indexSqls;

        private ResolvedExecutionSql(String sql, List<String> indexSqls) {
            this.sql = sql;
            this.indexSqls = List.copyOf(indexSqls);
        }
    }
}
