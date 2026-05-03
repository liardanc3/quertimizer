package com.quertimizer.problem.application.service;

import com.quertimizer.problem.application.port.in.ExecuteProblemSqlUseCase;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.problem.application.input.ProblemExecutionInput;
import com.quertimizer.problem.application.output.ProblemExecutionOutput;
import com.quertimizer.problem.application.output.ProblemJudgeExecutionResult;
import com.quertimizer.problem.application.port.out.ProblemJudgePort;
import com.quertimizer.problem.domain.model.ProblemJudgeExecutionMode;
import com.quertimizer.problem.domain.model.ProblemQueryResultText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecuteProblemSql implements ExecuteProblemSqlUseCase {

    private final ProblemJudgePort problemJudgePort;
    private final ProblemDatasetResolver datasetResolver;
    private final ProblemExecutionSessionStore executionSessionStore;

    /**
     * 문제 SQL을 실행한다.
     *
     * <ol>
     *   <li>데이터셋과 실행 환경 준비
     *   <li>judge SQL 실행
     *   <li>문제 실행 응답 변환
     * </ol>
     *
     * @param input 문제 SQL 실행 입력
     * @return 문제 SQL 실행 결과
     */
    @Override
    public ProblemExecutionOutput execute(ProblemExecutionInput input) {
        log.info(
                "문제 SQL 실행 시작 problemId={}, handle={}, dbmsType={}, executionSessionId={}",
                input.getProblemId(), input.getHandle(), input.getDbmsType(), input.getExecutionSessionId()
        );
        ProblemDatasetResolver.ResolvedProblemDataset dataset =
                datasetResolver.resolve(input.getProblemId(), input.getDbmsType());
        log.info(
                "문제 SQL 실행 데이터셋 조회 완료 problemId={}, datasetId={}, dbmsType={}",
                input.getProblemId(), dataset.getDatasetId(), dataset.getDbmsType()
        );
        ProblemExecutionSessionStore.ProblemExecutionSession session = prepareEnvironment(input, dataset);

        String executionId = "interactive-" + UUID.randomUUID();
        executionSessionStore.markExecution(input.getExecutionSessionId(), executionId);
        try {
            log.info(
                    "문제 SQL 실행 요청 전달 executionId={}, environmentId={}, problemId={}",
                    executionId, session.getEnvironmentId(), input.getProblemId()
            );
            ProblemJudgeExecutionResult result = problemJudgePort.executeInteractiveSql(
                    executionId, session.getEnvironmentId(), input.getSql(),
                    normalizePage(input.getPage()), normalizePageSize(input.getPageSize())
            );
            log.info(
                    "문제 SQL 실행 완료 executionId={}, problemId={}, mode={}, rowCount={}, cost={}, executionTimeMs={}",
                    executionId, input.getProblemId(), result.getMode(), result.getRowCount(),
                    result.getCost(), result.getExecutionTimeMs()
            );

            return toProblemExecutionOutput(input.getProblemId(), result);
        } catch (IllegalArgumentException exception) {
            log.warn(
                    "문제 SQL 실행 검증 실패 executionId={}, problemId={}, reason={}",
                    executionId, input.getProblemId(), resolveUserErrorMessage(exception)
            );
            throw new BusinessException(resolveUserErrorMessage(exception), HttpStatus.BAD_REQUEST);
        } finally {
            executionSessionStore.clearExecution(input.getExecutionSessionId(), executionId);
        }
    }

    private synchronized ProblemExecutionSessionStore.ProblemExecutionSession prepareEnvironment(
            ProblemExecutionInput input, ProblemDatasetResolver.ResolvedProblemDataset dataset) {
        // 같은 문제 실행 세션과 문제/데이터셋 반복 실행 시 기존 실행 환경 재사용
        return executionSessionStore.findReusable(input.getExecutionSessionId(), input.getProblemId(), dataset.getDatasetId())
                .map(session -> {
                    log.info(
                            "문제 SQL 실행 환경 재사용 executionSessionId={}, environmentId={}, datasetId={}",
                            input.getExecutionSessionId(), session.getEnvironmentId(), dataset.getDatasetId()
                    );
                    return session;
                })
                .orElseGet(() -> createFreshEnvironment(input, dataset));
    }

    private ProblemExecutionSessionStore.ProblemExecutionSession createFreshEnvironment(
            ProblemExecutionInput input, ProblemDatasetResolver.ResolvedProblemDataset dataset) {
        // 문제 실행 세션의 다른 문제 이동 시 이전 실행 환경 선제 정리
        executionSessionStore.remove(input.getExecutionSessionId())
                .ifPresent(session -> {
                    log.info(
                            "문제 SQL 실행 이전 환경 정리 executionSessionId={}, environmentId={}",
                            input.getExecutionSessionId(), session.getEnvironmentId()
                    );
                    dropQuietly(input.getExecutionSessionId(), session.getEnvironmentId());
                });

        // 데이터셋 기준 새 judge 실행 환경 생성
        log.info(
                "문제 SQL 실행 환경 생성 시작 executionSessionId={}, problemId={}, datasetId={}",
                input.getExecutionSessionId(), input.getProblemId(), dataset.getDatasetId()
        );
        String environmentId = problemJudgePort.createInteractiveEnvironment(dataset.getDatasetId());
        ProblemExecutionSessionStore.ProblemExecutionSession session =
                executionSessionStore.save(input.getExecutionSessionId(), input.getProblemId(), dataset.getDatasetId(), environmentId);
        log.info(
                "문제 SQL 실행 환경 생성 완료 executionSessionId={}, environmentId={}, datasetId={}",
                input.getExecutionSessionId(), environmentId, dataset.getDatasetId()
        );
        return session;
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
        // 기존 인터랙티브 실행 기준 페이지 크기 최대 10개 제한
        if (pageSize == null || pageSize < 1) {
            return 10;
        }

        // 페이지 크기 상한 적용
        return Math.min(pageSize, 10);
    }

    private void dropQuietly(String executionSessionId, String environmentId) {
        // 정리 실패 시 세션 종료 흐름 보호용 로그 기록
        try {
            problemJudgePort.dropEnvironment(environmentId);
            log.info(
                    "문제 SQL 실행 환경 정리 완료 executionSessionId={}, environmentId={}",
                    executionSessionId, environmentId
            );
        } catch (Exception exception) {
            log.warn("judge 실행 환경 정리 실패 executionSessionId={}, environmentId={}", executionSessionId, environmentId, exception);
        }
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
}
