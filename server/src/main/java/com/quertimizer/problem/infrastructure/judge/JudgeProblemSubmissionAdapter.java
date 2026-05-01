package com.quertimizer.problem.infrastructure.judge;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.problem.application.input.ProblemSubmissionInput;
import com.quertimizer.problem.application.output.ProblemSubmissionOutput;
import com.quertimizer.problem.application.output.ProblemSubmissionProgress;
import com.quertimizer.problem.application.port.ProblemSubmissionPort;
import com.quertimizer.problem.application.service.ProblemAnswerValidationService;
import com.quertimizer.problem.application.service.ProblemSubmissionRecordService;
import com.quertimizer.problem.domain.model.ProblemExecutionPlanAnalysis;
import com.quertimizer.problem.domain.model.ProblemPlanMeasurement;
import com.quertimizer.problem.domain.policy.ProblemExecutionPlanPolicy;
import com.quertimizer.problem.domain.policy.ProblemOfficialCostPolicy;
import com.quertimizer.judge.application.input.AnalyzeJudgeEnvironmentInput;
import com.quertimizer.judge.application.input.CreateJudgeEnvironmentInput;
import com.quertimizer.judge.application.input.ExecuteJudgeSqlInput;
import com.quertimizer.judge.application.output.ExecutionMode;
import com.quertimizer.judge.application.output.JudgeSqlStatement;
import com.quertimizer.judge.application.output.SqlExecutionResult;
import com.quertimizer.judge.application.port.JudgePort;
import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.ids.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.ids.JudgeExecutionId;
import com.quertimizer.judge.domain.model.EnvironmentPolicy;
import com.quertimizer.judge.domain.model.ExecutionOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static com.quertimizer.problem.domain.model.ProblemQueryFailReason.*;
import static com.quertimizer.problem.domain.model.ProblemSubmitProgressStep.*;
import static com.quertimizer.problem.domain.model.ProblemSubmitProgressText.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class JudgeProblemSubmissionAdapter implements ProblemSubmissionPort {

    private final JudgePort judgePort;
    private final JudgeProblemDatasetResolver datasetResolver;
    private final ProblemExecutionPlanPolicy executionPlanPolicy;
    private final ProblemOfficialCostPolicy officialCostPolicy;
    private final ProblemAnswerValidationService problemAnswerValidationService;
    private final ProblemSubmissionRecordService problemSubmissionRecordService;

    @Override
    public ProblemSubmissionOutput submit(ProblemSubmissionInput input) {
        // 제출 원문과 제출 시각 확정
        LocalDateTime submittedAt = LocalDateTime.now();
        String problemId = normalizeProblemId(input.getProblemId());
        String storedSubmittedSql = preserveSubmittedSql(input.getSql());
        log.info(
                "문제 SQL 제출 시작 problemId={}, handle={}, dbmsType={}",
                problemId, input.getHandle(), input.getDbmsType()
        );

        try {
            // 제출 SQL의 설정 DDL과 기준 SELECT 분리 및 형식 검증 진행 상태 전송
            log.info("문제 SQL 제출 검증 시작 problemId={}, sqlLength={}", problemId, storedSubmittedSql.length());
            List<SubmittedStatement> submittedStatements = parseSubmittedStatements(input.getSql());
            SubmittedStatement referenceStatement = resolveReferenceStatement(submittedStatements);
            List<SubmittedStatement> ddlStatements = resolveDdlStatements(submittedStatements);
            acceptProgress(input, running(problemId, VALIDATE.getKey(), SQL_VALIDATE_RUNNING.getText()));
            acceptProgress(input, success(problemId, VALIDATE.getKey(), SQL_VALIDATE_SUCCESS.getText()));
            log.info(
                    "문제 SQL 제출 검증 완료 problemId={}, statementCount={}, indexDdlCount={}, referenceStatementIndex={}",
                    problemId, submittedStatements.size(), ddlStatements.size(), referenceStatement.index
            );

            // 제출 전용 영속 실행 환경 생성과 정답, DDL, 실행 계획 단계 수행
            JudgeProblemDatasetResolver.ResolvedProblemDataset dataset = datasetResolver.resolve(problemId, input.getDbmsType());
            log.info(
                    "문제 SQL 제출 데이터셋 조회 완료 problemId={}, datasetId={}, dbmsType={}",
                    problemId, dataset.getDatasetId(), dataset.getDbmsType()
            );
            log.info(
                    "문제 SQL 제출 실행 환경 생성 시작 problemId={}, datasetId={}",
                    problemId, dataset.getDatasetId()
            );
            JudgeEnvironmentId environmentId = judgePort.createEnvironment(new CreateJudgeEnvironmentInput(
                    new JudgeDatasetId(dataset.getDatasetId()),
                    new EnvironmentPolicy(true, true, false)
            ));
            log.info(
                    "문제 SQL 제출 실행 환경 생성 완료 problemId={}, environmentId={}, datasetId={}",
                    problemId, environmentId, dataset.getDatasetId()
            );
            try {
                return submitInEnvironment(
                        input, problemId, storedSubmittedSql, submittedAt,
                        dataset.getDbmsType(), environmentId, referenceStatement, ddlStatements
                );
            } finally {
                dropQuietly(problemId, environmentId);
            }
        } catch (BusinessException exception) {
            log.warn(
                    "문제 SQL 제출 비즈니스 예외 problemId={}, handle={}, reason={}",
                    problemId, input.getHandle(), exception.getMessage()
            );
            throw exception;
        } catch (Exception exception) {
            String message = resolveErrorMessage(exception);
            log.warn(
                    "문제 SQL 제출 실패 problemId={}, handle={}, reason={}",
                    problemId, input.getHandle(), message, exception
            );
            acceptProgress(input, error(problemId, VALIDATE.getKey(), SQL_VALIDATE_FAILED.getText(), List.of(message)));
            problemSubmissionRecordService.saveSubmission(
                    problemId,
                    input.getHandle(),
                    input.getDbmsType(),
                    storedSubmittedSql,
                    false,
                    message,
                    0,
                    null,
                    0,
                    0L,
                    submittedAt
            );
            return new ProblemSubmissionOutput(problemId, false, message, null);
        }
    }

    private ProblemSubmissionOutput submitInEnvironment(ProblemSubmissionInput input, String problemId,
                                                        String storedSubmittedSql, LocalDateTime submittedAt,
                                                        DbmsType dbmsType, JudgeEnvironmentId environmentId,
                                                        SubmittedStatement referenceStatement, List<SubmittedStatement> ddlStatements) {
        // 기준 SELECT 실행 결과와 등록 정답 일치 확인
        log.info(
                "문제 SQL 제출 정답 SELECT 실행 시작 problemId={}, environmentId={}",
                problemId, environmentId
        );
        acceptProgress(input, running(problemId, ANSWER.getKey(), ANSWER_VALIDATE_RUNNING.getText()));
        SqlExecutionResult answerResult;
        try {
            answerResult = executeAnswer(environmentId, referenceStatement.sql);
            log.info(
                    "문제 SQL 제출 정답 SELECT 실행 완료 problemId={}, environmentId={}, rowCount={}, executionTimeMs={}",
                    problemId, environmentId, answerResult.getRowCount(), answerResult.getExecutionTimeMs()
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            String message = resolveErrorMessage(exception);
            log.warn(
                    "문제 SQL 제출 정답 SELECT 실행 실패 problemId={}, environmentId={}, reason={}",
                    problemId, environmentId, message, exception
            );
            acceptProgress(input, error(problemId, ANSWER.getKey(), ANSWER_VALIDATE_FAILED.getText(), List.of(message)));
            saveFailedSubmission(input, problemId, dbmsType, storedSubmittedSql, message, submittedAt);
            return new ProblemSubmissionOutput(problemId, false, message, null);
        }

        if (!problemAnswerValidationService.isCorrectAnswer(problemId, answerResult.getColumns(), answerResult.getRows())) {
            log.info(
                    "문제 SQL 제출 정답 비교 오답 problemId={}, environmentId={}, rowCount={}",
                    problemId, environmentId, answerResult.getRowCount()
            );
            acceptProgress(input, incorrect(problemId, ANSWER.getKey(), ANSWER_INCORRECT.getText()));
            problemSubmissionRecordService.saveSubmission(
                    problemId,
                    input.getHandle(),
                    dbmsType,
                    storedSubmittedSql,
                    false,
                    INCORRECT_ANSWER.getText(),
                    resolveExecutionTime(answerResult),
                    toDouble(answerResult.getCost()),
                    answerResult.getRowCount(),
                    0L,
                    submittedAt
            );
            return new ProblemSubmissionOutput(problemId, false, INCORRECT_ANSWER.getText(), null);
        }
        log.info(
                "문제 SQL 제출 정답 비교 성공 problemId={}, environmentId={}, rowCount={}",
                problemId, environmentId, answerResult.getRowCount()
        );
        acceptProgress(input, success(problemId, ANSWER.getKey(), ANSWER_CORRECT.getText()));

        // 설정 DDL 반영으로 실행 계획 측정 조건 생성
        String ddlFailureMessage = executeDdlStatements(input, problemId, environmentId, ddlStatements);

        // 설정 반영 실행 환경에서 기준 SELECT 실행 계획과 비용 측정
        SqlExecutionResult planResult;
        ProblemExecutionPlanAnalysis planAnalysis;
        try {
            log.info(
                    "문제 SQL 제출 실행 계획 측정 시작 problemId={}, environmentId={}",
                    problemId, environmentId
            );
            acceptProgress(input, running(problemId, PLAN.getKey(), PLAN_RUNNING.getText()));
            planResult = measureOfficialPlan(problemId, environmentId, referenceStatement.sql);
            planAnalysis = executionPlanPolicy.analyze(dbmsType, planResult.getPlanLines(), referenceStatement.sql);
            acceptProgress(input, success(
                    problemId,
                    PLAN.getKey(),
                    PLAN_SUCCESS.getText(),
                    buildPlanDetailLines(dbmsType, planResult, planAnalysis)
            ));
            log.info(
                    "문제 SQL 제출 실행 계획 측정 완료 problemId={}, environmentId={}, cost={}, planElement={}",
                    problemId, environmentId, planResult.getCost(), planAnalysis.getExecutionPlanElement()
            );
        } catch (Exception exception) {
            String message = resolveErrorMessage(exception);
            log.warn(
                    "문제 SQL 제출 실행 계획 측정 실패 problemId={}, environmentId={}, reason={}",
                    problemId, environmentId, message, exception
            );
            acceptProgress(input, error(problemId, PLAN.getKey(), PLAN_FAILED.getText(), List.of(message)));
            problemSubmissionRecordService.saveSubmission(
                    problemId,
                    input.getHandle(),
                    dbmsType,
                    storedSubmittedSql,
                    false,
                    message,
                    resolveExecutionTime(answerResult),
                    null,
                    answerResult.getRowCount(),
                    0L,
                    submittedAt
            );
            return new ProblemSubmissionOutput(problemId, false, message, null);
        }

        // 제출 이력과 최고 기록 저장 후 최종 제출 결과 반환
        boolean succeeded = ddlFailureMessage == null;
        String message = succeeded ? CORRECT_ANSWER.getText() : ddlFailureMessage;
        long executionTimeMs = resolveExecutionTime(answerResult);
        Double cost = resolveCost(planResult, answerResult);
        problemSubmissionRecordService.saveSubmission(
                problemId,
                input.getHandle(),
                dbmsType,
                storedSubmittedSql,
                succeeded,
                message,
                executionTimeMs,
                cost,
                answerResult.getRowCount(),
                planAnalysis.getExecutionPlanElement(),
                submittedAt
        );
        log.info(
                "문제 SQL 제출 이력 저장 완료 problemId={}, handle={}, succeeded={}, cost={}, executionTimeMs={}",
                problemId, input.getHandle(), succeeded, cost, executionTimeMs
        );

        if (succeeded) {
            problemSubmissionRecordService.saveBestSolveHistory(
                    problemId,
                    input.getHandle(),
                    dbmsType,
                    storedSubmittedSql,
                    executionTimeMs,
                    cost,
                    0,
                    planAnalysis.getExecutionPlanElement(),
                    submittedAt
            );
            log.info(
                    "문제 SQL 제출 최고 기록 저장 완료 problemId={}, handle={}, cost={}, executionTimeMs={}",
                    problemId, input.getHandle(), cost, executionTimeMs
            );
            return new ProblemSubmissionOutput(problemId, true, CORRECT_ANSWER.getText(), executionTimeMs);
        }

        return new ProblemSubmissionOutput(problemId, false, message, null);
    }

    private SqlExecutionResult executeAnswer(JudgeEnvironmentId environmentId, String sql) {
        // 출력 데이터 비교용 최대 제출 검증 행 수 SELECT 결과 조회
        SqlExecutionResult result = judgePort.execute(new ExecuteJudgeSqlInput(
                new JudgeExecutionId("submit-answer-" + UUID.randomUUID()),
                environmentId,
                sql,
                ExecutionOptions.submissionAnswer()
        ));
        if (result.getRowCount() > result.getRows().size()) {
            throw badRequest("SQL 실행 결과 행 수가 너무 많습니다.");
        }

        return result;
    }

    private String executeDdlStatements(ProblemSubmissionInput input, String problemId,
                                        JudgeEnvironmentId environmentId,
                                        List<SubmittedStatement> ddlStatements) {
        // 제출 SQL에 포함된 index DDL 순차 반영
        log.info(
                "문제 SQL 제출 INDEX DDL 반영 시작 problemId={}, environmentId={}, indexDdlCount={}",
                problemId, environmentId, ddlStatements.size()
        );
        acceptProgress(input, running(problemId, DDL.getKey(), DDL_RUNNING.getText()));
        if (ddlStatements.isEmpty()) {
            acceptProgress(input, success(problemId, DDL.getKey(), DDL_EMPTY.getText()));
            log.info("문제 SQL 제출 INDEX DDL 반영 생략 problemId={}, environmentId={}", problemId, environmentId);
            return null;
        }

        List<String> detailLines = new ArrayList<>();
        try {
            for (SubmittedStatement ddlStatement : ddlStatements) {
                log.info(
                        "문제 SQL 제출 INDEX DDL 실행 시작 problemId={}, environmentId={}, statementIndex={}, preview={}",
                        problemId, environmentId, ddlStatement.index, buildSubmittedDdlPreview(ddlStatement.sql)
                );
                judgePort.execute(new ExecuteJudgeSqlInput(
                        new JudgeExecutionId("submit-ddl-" + UUID.randomUUID()),
                        environmentId,
                        ddlStatement.sql,
                        ExecutionOptions.officialCost()
                ));
                detailLines.add(CHECK_PREFIX.getText() + buildSubmittedDdlPreview(ddlStatement.sql));
                log.info(
                        "문제 SQL 제출 INDEX DDL 실행 완료 problemId={}, environmentId={}, statementIndex={}",
                        problemId, environmentId, ddlStatement.index
                );
            }
            acceptProgress(input, success(problemId, DDL.getKey(), DDL_SUCCESS.getText(), detailLines));
            log.info("문제 SQL 제출 INDEX DDL 반영 완료 problemId={}, environmentId={}", problemId, environmentId);
            return null;
        } catch (Exception exception) {
            String message = resolveErrorMessage(exception);
            log.warn(
                    "문제 SQL 제출 INDEX DDL 반영 실패 problemId={}, environmentId={}, reason={}",
                    problemId, environmentId, message, exception
            );
            acceptProgress(input, error(problemId, DDL.getKey(), DDL_FAILED.getText(), List.of(message)));
            return message;
        }
    }

    private SqlExecutionResult executePlan(JudgeEnvironmentId environmentId, String sql) {
        // 기준 SELECT 실행 계획 조회
        return judgePort.execute(new ExecuteJudgeSqlInput(
                new JudgeExecutionId("submit-plan-" + UUID.randomUUID()),
                environmentId,
                "EXPLAIN " + sql,
                ExecutionOptions.officialCost()
        ));
    }

    private SqlExecutionResult measureOfficialPlan(String problemId, JudgeEnvironmentId environmentId, String sql) {
        // Quertimizer 공식 제출 정책 기준 통계 갱신과 실행 계획 반복 측정 후 비용 중앙값 선택
        List<PlanMeasurement> measurements = new ArrayList<>();
        for (int attempt = 1; attempt <= officialCostPolicy.getMeasurementAttemptCount(); attempt++) {
            log.info(
                    "문제 SQL 제출 통계 갱신 시작 problemId={}, environmentId={}, attempt={}",
                    problemId, environmentId, attempt
            );
            judgePort.analyze(new AnalyzeJudgeEnvironmentInput(
                    new JudgeExecutionId("submit-analyze-" + UUID.randomUUID()),
                    environmentId,
                    ExecutionOptions.officialCost()
            ));
            log.info(
                    "문제 SQL 제출 통계 갱신 완료 problemId={}, environmentId={}, attempt={}",
                    problemId, environmentId, attempt
            );

            SqlExecutionResult planResult = executePlan(environmentId, sql);
            measurements.add(new PlanMeasurement(
                    new ProblemPlanMeasurement(attempt, planResult.getCost(), planResult.getPlanLines()),
                    planResult
            ));
            log.info(
                    "문제 SQL 제출 실행 계획 측정 시도 완료 problemId={}, environmentId={}, attempt={}, cost={}",
                    problemId, environmentId, attempt, planResult.getCost()
            );
        }

        ProblemPlanMeasurement selectedMeasurement = officialCostPolicy.selectMedianCostMeasurement(
                measurements.stream()
                        .map(PlanMeasurement::getMeasurement)
                        .toList()
        );
        log.info(
                "문제 SQL 제출 실행 계획 중앙값 선택 problemId={}, environmentId={}, attempt={}, cost={}",
                problemId, environmentId, selectedMeasurement.getAttemptOrder(), selectedMeasurement.getCost()
        );
        return measurements.stream()
                .filter(measurement -> measurement.hasAttemptOrder(selectedMeasurement.getAttemptOrder()))
                .findFirst()
                .map(PlanMeasurement::getPlanResult)
                .orElseThrow(() -> new IllegalStateException("공식 비용 측정 결과를 선택하지 못했다."));
    }

    private void dropQuietly(String problemId, JudgeEnvironmentId environmentId) {
        // 제출 결과 응답 보호를 위한 정리 실패 로그 기록
        try {
            log.info("문제 SQL 제출 실행 환경 정리 시작 problemId={}, environmentId={}", problemId, environmentId);
            judgePort.drop(environmentId);
            log.info("문제 SQL 제출 실행 환경 정리 완료 problemId={}, environmentId={}", problemId, environmentId);
        } catch (Exception exception) {
            log.warn("judge 제출 환경 정리 실패 problemId={}, environmentId={}", problemId, environmentId, exception);
        }
    }

    private List<SubmittedStatement> parseSubmittedStatements(String sql) {
        // 제출 SQL 문장 분리와 SELECT 위치 규칙 검증
        List<JudgeSqlStatement> judgeStatements;
        try {
            judgeStatements = judgePort.parseStatements(normalizeSubmittedSql(sql));
        } catch (IllegalArgumentException exception) {
            throw badRequest(resolveErrorMessage(exception));
        }

        // 제출 SQL 문장 존재 여부 검증
        if (judgeStatements.isEmpty()) {
            throw badRequest(SUBMIT_SQL_REQUIRED.getMessage());
        }

        List<SubmittedStatement> submittedStatements = new ArrayList<>();
        int referenceStatementIndex = -1;
        for (JudgeSqlStatement judgeStatement : judgeStatements) {
            String statementSql = judgeStatement.getSql();
            ExecutionMode mode = judgeStatement.getMode();
            if (mode == ExecutionMode.EXPLAIN || mode == ExecutionMode.EXPLAIN_ANALYZE || mode == ExecutionMode.COMMAND) {
                throw badRequest(SUBMIT_SELECT_AND_INDEX_DDL_ONLY.getMessage());
            }

            int statementIndex = submittedStatements.size();
            if (mode == ExecutionMode.SELECT) {
                if (referenceStatementIndex >= 0) {
                    throw badRequest(SUBMIT_SELECT_ONLY.getMessage());
                }
                referenceStatementIndex = statementIndex;
            } else if (referenceStatementIndex >= 0) {
                throw badRequest(SUBMIT_SELECT_FOLLOWED_BY_STATEMENTS.getMessage());
            }

            submittedStatements.add(new SubmittedStatement(
                    createSubmittedStatementKey(statementIndex), statementIndex,
                    statementSql, mode, false
            ));
        }

        if (referenceStatementIndex < 0) {
            throw badRequest(SUBMIT_SELECT_REQUIRED.getMessage());
        }

        return markReferenceStatement(submittedStatements, referenceStatementIndex);
    }

    private List<SubmittedStatement> markReferenceStatement(List<SubmittedStatement> statements, int referenceStatementIndex) {
        // 기준 SELECT 표시 부여
        List<SubmittedStatement> resolvedStatements = new ArrayList<>();
        for (SubmittedStatement statement : statements) {
            resolvedStatements.add(new SubmittedStatement(
                    statement.key,
                    statement.index,
                    statement.sql,
                    statement.mode,
                    statement.index == referenceStatementIndex
            ));
        }

        return resolvedStatements;
    }

    private SubmittedStatement resolveReferenceStatement(List<SubmittedStatement> statements) {
        // 기준 SELECT 문장 조회
        for (SubmittedStatement statement : statements) {
            if (statement.referenceSelect) {
                return statement;
            }
        }

        throw badRequest(SUBMIT_REFERENCE_SELECT_NOT_FOUND.getMessage());
    }

    private List<SubmittedStatement> resolveDdlStatements(List<SubmittedStatement> statements) {
        // 제출 SQL 중 설정 DDL 추출
        List<SubmittedStatement> ddlStatements = new ArrayList<>();
        for (SubmittedStatement statement : statements) {
            if (statement.mode == ExecutionMode.INDEX_COMMAND) {
                ddlStatements.add(statement);
            }
        }

        return List.copyOf(ddlStatements);
    }

    private void saveFailedSubmission(ProblemSubmissionInput input, String problemId,
                                      DbmsType dbmsType, String submittedSql,
                                      String message, LocalDateTime submittedAt) {
        // 실행 전 실패 제출 이력 저장
        problemSubmissionRecordService.saveSubmission(
                problemId,
                input.getHandle(),
                dbmsType,
                submittedSql,
                false,
                message,
                0,
                null,
                0,
                0L,
                submittedAt
        );
    }

    private ProblemSubmissionProgress running(String problemId, String stepKey, String message) {
        // 진행 중 진행 상태 생성
        return new ProblemSubmissionProgress(
                problemId, stepKey, "running", message,
                List.of(), null, null, null, null, null
        );
    }

    private ProblemSubmissionProgress success(String problemId, String stepKey, String message) {
        // 성공 진행 상태 생성
        return success(problemId, stepKey, message, List.of());
    }

    private ProblemSubmissionProgress success(String problemId, String stepKey, String message, List<String> detailLines) {
        // 상세 정보 포함 성공 진행 상태 생성
        return new ProblemSubmissionProgress(
                problemId, stepKey, "success", message,
                detailLines, null, null, null, null, null
        );
    }

    private ProblemSubmissionProgress incorrect(String problemId, String stepKey, String message) {
        // 오답 진행 상태 생성
        return new ProblemSubmissionProgress(
                problemId, stepKey, "incorrect", message,
                List.of(), null, null, null, null, null
        );
    }

    private ProblemSubmissionProgress error(String problemId, String stepKey, String message, List<String> detailLines) {
        // 실패 진행 상태 생성
        return new ProblemSubmissionProgress(
                problemId, stepKey, "error", message,
                detailLines, null, null, null, null, null
        );
    }

    private void acceptProgress(ProblemSubmissionInput input, ProblemSubmissionProgress progress) {
        // 진행 상태 리스너 존재 시 진행 상태 전달
        Consumer<ProblemSubmissionProgress> listener = input.getProgressListener();
        if (listener != null) {
            listener.accept(progress);
        }
    }

    private List<String> buildPlanDetailLines(DbmsType dbmsType,
                                              SqlExecutionResult planResult,
                                              ProblemExecutionPlanAnalysis planAnalysis) {
        // 기존 진행 상태 형식용 비용과 대표 실행 계획 상세 라인 생성
        List<String> detailLines = new ArrayList<>();
        detailLines.add(COST_PREFIX.getText() + formatCost(planResult.getCost()));
        detailLines.addAll(executionPlanPolicy.resolveDetailLines(dbmsType, planAnalysis.getExecutionPlanElement()));

        if (detailLines.size() == 1) {
            for (String summaryLine : planAnalysis.getSummaryLines()) {
                detailLines.add(CHECK_PREFIX.getText() + summaryLine);
            }
        }

        return List.copyOf(detailLines);
    }

    private String formatCost(BigDecimal cost) {
        // 비용 표시 문자열 생성
        return cost == null ? "-" : "%.1f".formatted(cost.doubleValue());
    }

    private Double resolveCost(SqlExecutionResult planResult, SqlExecutionResult answerResult) {
        // 실행 계획 비용 우선 사용과 정답 실행 비용 대체 사용
        if (planResult.getCost() != null) {
            return planResult.getCost().doubleValue();
        }

        return toDouble(answerResult.getCost());
    }

    private Double toDouble(BigDecimal value) {
        // BigDecimal 비용 값을 이력 저장 모델에 맞춤
        return value != null ? value.doubleValue() : null;
    }

    private long resolveExecutionTime(SqlExecutionResult result) {
        // null 실행 시간 0 정리
        return result.getExecutionTimeMs() != null ? result.getExecutionTimeMs() : 0L;
    }

    private String normalizeProblemId(String problemId) {
        // 문제 ID 정리
        return problemId != null ? problemId.trim() : "";
    }

    private String normalizeSubmittedSql(String sql) {
        // 파서 전달용 제출 SQL 문자열 정리
        return sql != null ? sql.trim().replaceFirst(";\\s*$", "") : "";
    }

    private String preserveSubmittedSql(String sql) {
        // 제출 SQL 원문 줄바꿈 보존
        return sql != null ? sql.replace("\r\n", "\n") : "";
    }

    private String createSubmittedStatementKey(int statementIndex) {
        // 제출 문장 키 생성
        return "submit-statement-" + statementIndex;
    }

    private String buildSubmittedDdlPreview(String sql) {
        // DDL 진행 상태 표시용 짧은 미리보기 생성
        String preview = sql.replaceAll("\\s+", " ").trim();
        if (preview.length() <= 20) {
            return preview;
        }

        return preview.substring(0, 20) + "...";
    }

    private String resolveErrorMessage(Exception exception) {
        // 예외 체인 내 사용자 전달 메시지 조회
        Throwable cause = exception;
        while (cause != null) {
            if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
                return cause.getMessage();
            }

            cause = cause.getCause();
        }

        return "SQL 제출에 실패했다.";
    }

    private BusinessException badRequest(String message) {
        // 제출 요청 형식 오류를 비즈니스 예외로 변환
        return new BusinessException(message, HttpStatus.BAD_REQUEST);
    }

    private static final class SubmittedStatement {
        private final String key;
        private final int index;
        private final String sql;
        private final ExecutionMode mode;
        private final boolean referenceSelect;

        private SubmittedStatement(String key, int index, String sql, ExecutionMode mode, boolean referenceSelect) {
            this.key = key;
            this.index = index;
            this.sql = sql;
            this.mode = mode;
            this.referenceSelect = referenceSelect;
        }
    }

    private static final class PlanMeasurement {
        private final ProblemPlanMeasurement measurement;
        private final SqlExecutionResult planResult;

        private PlanMeasurement(ProblemPlanMeasurement measurement, SqlExecutionResult planResult) {
            this.measurement = measurement;
            this.planResult = planResult;
        }

        private ProblemPlanMeasurement getMeasurement() {
            return measurement;
        }

        private SqlExecutionResult getPlanResult() {
            return planResult;
        }

        private boolean hasAttemptOrder(int attemptOrder) {
            return measurement.getAttemptOrder() == attemptOrder;
        }
    }
}
