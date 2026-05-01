package com.quertimizer.problem.infrastructure.judge;

import com.quertimizer.judge.application.input.CreateJudgeEnvironmentInput;
import com.quertimizer.judge.application.input.ExecuteJudgeSqlInput;
import com.quertimizer.judge.application.output.ExecutionMode;
import com.quertimizer.judge.application.output.SqlExecutionResult;
import com.quertimizer.judge.application.port.JudgePort;
import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.ids.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.ids.JudgeExecutionId;
import com.quertimizer.judge.domain.model.EnvironmentPolicy;
import com.quertimizer.judge.domain.model.ExecutionOptions;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.problem.application.input.ProblemExecutionInput;
import com.quertimizer.problem.application.output.ProblemExecutionOutput;
import com.quertimizer.problem.application.port.ProblemExecutionPort;
import com.quertimizer.problem.domain.model.ProblemQueryResultText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class JudgeProblemExecutionAdapter implements ProblemExecutionPort {

    private final JudgePort judgePort;
    private final JudgeProblemDatasetResolver datasetResolver;
    private final ConcurrentHashMap<String, InteractiveEnvironmentContext> contextByExecutionSessionId = new ConcurrentHashMap<>();

    @Override
    public ProblemExecutionOutput execute(ProblemExecutionInput input) {
        // 실행 세션과 문제에 대응되는 영속 실행 환경 준비
        log.info(
                "문제 SQL 실행 시작 problemId={}, handle={}, dbmsType={}, executionSessionId={}",
                input.getProblemId(), input.getHandle(), input.getDbmsType(), input.getExecutionSessionId()
        );
        JudgeProblemDatasetResolver.ResolvedProblemDataset dataset =
                datasetResolver.resolve(input.getProblemId(), input.getDbmsType());
        log.info(
                "문제 SQL 실행 데이터셋 조회 완료 problemId={}, datasetId={}, dbmsType={}",
                input.getProblemId(), dataset.getDatasetId(), dataset.getDbmsType()
        );
        InteractiveEnvironmentContext context = prepareEnvironment(input, dataset);

        // 실행 요청별 새 실행 ID 생성과 취소 조회용 보관
        JudgeExecutionId executionId = new JudgeExecutionId("interactive-" + UUID.randomUUID());
        context.lastExecutionId = executionId;
        try {
            log.info(
                    "문제 SQL 실행 요청 전달 executionId={}, environmentId={}, problemId={}",
                    executionId, context.environmentId, input.getProblemId()
            );
            SqlExecutionResult result = judgePort.execute(new ExecuteJudgeSqlInput(
                    executionId,
                    context.environmentId,
                    input.getSql(),
                    new ExecutionOptions(
                            60,
                            normalizePage(input.getPage()),
                            normalizePageSize(input.getPageSize()),
                            true,
                            false
                    )
            ));
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
            context.clearExecution(executionId);
        }
    }

    @Override
    public void cancel(String executionSessionId) {
        // 문제 실행 세션의 마지막 실행 ID 기준 judge cancel API 호출
        InteractiveEnvironmentContext context = contextByExecutionSessionId.get(executionSessionId);
        if (context == null || context.lastExecutionId == null) {
            log.info("문제 SQL 실행 취소 대상 없음 executionSessionId={}", executionSessionId);
            return;
        }

        log.info(
                "문제 SQL 실행 취소 요청 executionSessionId={}, executionId={}",
                executionSessionId, context.lastExecutionId
        );
        judgePort.cancel(context.lastExecutionId);
    }

    @Override
    public void closeSession(String executionSessionId) {
        // 세션 종료 또는 명시적 이탈 시 judge 실행 환경 정리
        InteractiveEnvironmentContext context = contextByExecutionSessionId.remove(executionSessionId);
        if (context == null) {
            log.info("문제 SQL 실행 세션 정리 대상 없음 executionSessionId={}", executionSessionId);
            return;
        }

        log.info(
                "문제 SQL 실행 세션 정리 시작 executionSessionId={}, environmentId={}",
                executionSessionId, context.environmentId
        );
        dropQuietly(executionSessionId, context.environmentId);
    }

    private synchronized InteractiveEnvironmentContext prepareEnvironment(ProblemExecutionInput input,
                                                                          JudgeProblemDatasetResolver.ResolvedProblemDataset dataset) {
        // 같은 문제 실행 세션과 문제/데이터셋 반복 실행 시 기존 실행 환경 재사용
        InteractiveEnvironmentContext current = contextByExecutionSessionId.get(input.getExecutionSessionId());
        if (current != null && current.matches(input.getProblemId(), dataset.getDatasetId())) {
            log.info(
                    "문제 SQL 실행 환경 재사용 executionSessionId={}, environmentId={}, datasetId={}",
                    input.getExecutionSessionId(), current.environmentId, dataset.getDatasetId()
            );
            return current;
        }

        // 문제 실행 세션의 다른 문제 이동 시 이전 실행 환경 선제 정리
        if (current != null) {
            log.info(
                    "문제 SQL 실행 이전 환경 정리 executionSessionId={}, environmentId={}",
                    input.getExecutionSessionId(), current.environmentId
            );
            dropQuietly(input.getExecutionSessionId(), current.environmentId);
        }

        log.info(
                "문제 SQL 실행 환경 생성 시작 executionSessionId={}, problemId={}, datasetId={}",
                input.getExecutionSessionId(), input.getProblemId(), dataset.getDatasetId()
        );
        JudgeEnvironmentId environmentId = judgePort.createEnvironment(new CreateJudgeEnvironmentInput(
                new JudgeDatasetId(dataset.getDatasetId()),
                EnvironmentPolicy.interactive()
        ));
        InteractiveEnvironmentContext next = new InteractiveEnvironmentContext(input.getProblemId(), dataset.getDatasetId(), environmentId);
        contextByExecutionSessionId.put(input.getExecutionSessionId(), next);
        log.info(
                "문제 SQL 실행 환경 생성 완료 executionSessionId={}, environmentId={}, datasetId={}",
                input.getExecutionSessionId(), environmentId, dataset.getDatasetId()
        );
        return next;
    }

    private ProblemExecutionOutput toProblemExecutionOutput(String problemId, SqlExecutionResult result) {
        // judge 실행 결과를 문제 실행 응답 모델에 맞춤
        return new ProblemExecutionOutput(
                problemId,
                toResponseMode(result.getMode()),
                toResponseMessage(result.getMode()),
                result.getColumns(),
                result.getRows(),
                result.getPlanLines(),
                result.getRowCount(),
                result.getCurrentPage(),
                result.getPageSize(),
                result.getExecutionTimeMs() != null ? result.getExecutionTimeMs() : 0L,
                toDouble(result.getCost())
        );
    }

    private String toResponseMode(ExecutionMode mode) {
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

    private String toResponseMessage(ExecutionMode mode) {
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

        return Math.min(pageSize, 10);
    }

    private void dropQuietly(String executionSessionId, JudgeEnvironmentId environmentId) {
        // 정리 실패 시 세션 종료 흐름 보호용 로그 기록
        try {
            judgePort.drop(environmentId);
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

    private static final class InteractiveEnvironmentContext {
        private final String problemId;
        private final String datasetId;
        private final JudgeEnvironmentId environmentId;
        private JudgeExecutionId lastExecutionId;

        private InteractiveEnvironmentContext(String problemId, String datasetId, JudgeEnvironmentId environmentId) {
            this.problemId = problemId;
            this.datasetId = datasetId;
            this.environmentId = environmentId;
        }

        private boolean matches(String problemId, String datasetId) {
            return this.problemId.equals(problemId) && this.datasetId.equals(datasetId);
        }

        private void clearExecution(JudgeExecutionId executionId) {
            if (executionId.equals(lastExecutionId)) {
                lastExecutionId = null;
            }
        }
    }
}
