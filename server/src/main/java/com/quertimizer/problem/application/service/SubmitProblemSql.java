package com.quertimizer.problem.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.problem.application.port.in.SubmitProblemSqlUseCase;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.input.ProblemSubmissionInput;
import com.quertimizer.problem.application.output.ProblemJudgeExecutionResult;
import com.quertimizer.problem.application.output.ProblemSqlStatement;
import com.quertimizer.problem.application.output.ProblemSubmissionOutput;
import com.quertimizer.problem.application.output.ProblemSubmissionProgress;
import com.quertimizer.problem.application.port.out.ProblemJudgePort;
import com.quertimizer.problem.domain.model.ProblemJudgeExecutionMode;
import com.quertimizer.problem.domain.model.ProblemExecutionPlanAnalysis;
import com.quertimizer.problem.domain.model.ProblemPlanMeasurement;
import com.quertimizer.problem.domain.entity.ProblemAnswerCase;
import com.quertimizer.problem.domain.policy.ProblemExecutionPlanPolicy;
import com.quertimizer.problem.domain.policy.ProblemOfficialCostPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.UUID;
import java.util.function.Consumer;

import static com.quertimizer.problem.domain.model.ProblemQueryFailReason.*;
import static com.quertimizer.problem.domain.model.ProblemSubmitProgressStep.*;
import static com.quertimizer.problem.domain.model.ProblemSubmitProgressText.*;

@Component
@Slf4j
public class SubmitProblemSql implements SubmitProblemSqlUseCase {

    private static final String SELECTED_PLAN_ATTEMPT_PREFIX = "__selected_plan_attempt__:";

    private final ProblemJudgePort problemJudgePort;
    private final ProblemDatasetResolver datasetResolver;
    private final ProblemExecutionPlanPolicy executionPlanPolicy;
    private final ProblemOfficialCostPolicy officialCostPolicy;
    private final ProblemAnswerValidationService problemAnswerValidationService;
    private final ProblemSubmissionRecordService problemSubmissionRecordService;
    private final Executor problemSubmitTaskExecutor;

    public SubmitProblemSql(ProblemJudgePort problemJudgePort, ProblemDatasetResolver datasetResolver,
                            ProblemExecutionPlanPolicy executionPlanPolicy, ProblemOfficialCostPolicy officialCostPolicy,
                            ProblemAnswerValidationService problemAnswerValidationService,
                            ProblemSubmissionRecordService problemSubmissionRecordService,
                            @Qualifier("problemSubmitTaskExecutor") Executor problemSubmitTaskExecutor) {
        this.problemJudgePort = problemJudgePort;
        this.datasetResolver = datasetResolver;
        this.executionPlanPolicy = executionPlanPolicy;
        this.officialCostPolicy = officialCostPolicy;
        this.problemAnswerValidationService = problemAnswerValidationService;
        this.problemSubmissionRecordService = problemSubmissionRecordService;
        this.problemSubmitTaskExecutor = problemSubmitTaskExecutor;
    }

    /**
     * SQL을 제출하고 채점한다.
     *
     * <ol>
     *   <li>SQL 구문 정적 검사
     *   <li>출력 데이터 검증
     *   <li>실행계획 분석
     *   <li>제출 이력 저장
     * </ol>
     *
     * @param input SQL 제출 입력
     * @return SQL 제출 결과
     */
    @Override
    @Log("SQL 제출")
    public ProblemSubmissionOutput execute(ProblemSubmissionInput input) {
        // 제출 원문과 제출 시각 확정
        LocalDateTime submittedAt = LocalDateTime.now();
        String problemId = normalizeProblemId(input.getProblemId());
        String storedSubmittedSql = preserveSubmittedSql(mergeIndexSqls(input.getIndexSqls(), input.getSql()));

        try {
            // 제출 SQL의 설정 DDL과 기준 SELECT 분리 및 형식 검증 진행 상태 전송
            log.info("SQL 검증 시작 problem={}, sqlLength={}", problemId, storedSubmittedSql.length());
            acceptProgress(input, running(problemId, VALIDATE.getKey(), SQL_VALIDATE_RUNNING.getText()));
            List<SubmittedStatement> submittedStatements = parseSubmittedStatements(storedSubmittedSql);
            SubmittedStatement referenceStatement = resolveReferenceStatement(submittedStatements);
            List<SubmittedStatement> ddlStatements = resolveDdlStatements(submittedStatements);
            acceptProgress(input, success(problemId, VALIDATE.getKey(), SQL_VALIDATE_SUCCESS.getText()));
            log.info("SQL 검증 완료 problem={}", problemId);

            // 제출 전용 영속 실행 환경 생성을 위한 채점 데이터셋 조회
            ProblemDatasetResolver.ResolvedProblemDataset dataset = datasetResolver.resolve(problemId, input.getDbmsType());
            log.info(
                    "데이터셋 조회 완료 problem={}, dataset={}, dbmsType={}",
                    problemId, dataset.getDatasetId(), dataset.getDbmsType()
            );
            return submitInDataset(
                    input, problemId, storedSubmittedSql, submittedAt,
                    dataset, referenceStatement, ddlStatements
            );
        } catch (BusinessException exception) {
            log.warn(
                    "SQL 제출 비즈니스 예외 problem={}, handle={}, reason={}",
                    problemId, input.getHandle(), exception.getMessage()
            );
            throw exception;
        } catch (Exception exception) {
            String message = resolveErrorMessage(exception);
            log.warn(
                    "SQL 제출 실패 problem={}, handle={}, reason={}",
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

    private ProblemSubmissionOutput submitInDataset(ProblemSubmissionInput input, String problemId,
                                                    String storedSubmittedSql, LocalDateTime submittedAt,
                                                    ProblemDatasetResolver.ResolvedProblemDataset dataset,
                                                    SubmittedStatement referenceStatement, List<SubmittedStatement> ddlStatements) {
        // 숨김 케이스와 공개 케이스를 순차 실행해 출력 데이터 검증
        AnswerValidationResult answerValidationResult;
        try {
            answerValidationResult = validateAnswerCases(input, problemId, dataset, referenceStatement.sql, ddlStatements);
        } catch (Exception exception) {
            String message = resolveErrorMessage(exception);
            log.warn(
                    "SQL 제출 출력 데이터 검증 실패 problem={}, dataset={}, reason={}",
                    problemId, dataset.getDatasetId(), message, exception
            );
            saveFailedSubmission(input, problemId, dataset.getDbmsType(), storedSubmittedSql, message, submittedAt);
            return new ProblemSubmissionOutput(problemId, false, message, null);
        }

        if (!answerValidationResult.correct) {
            problemSubmissionRecordService.saveSubmission(
                    problemId,
                    input.getHandle(),
                    dataset.getDbmsType(),
                    storedSubmittedSql,
                    false,
                    INCORRECT_ANSWER.getText(),
                    resolveExecutionTime(answerValidationResult.answerResult),
                    null,
                    answerValidationResult.answerResult.getRowCount(),
                    0L,
                    submittedAt
            );
            return new ProblemSubmissionOutput(problemId, false, INCORRECT_ANSWER.getText(), null);
        }
        ProblemJudgeExecutionResult answerResult = answerValidationResult.answerResult;
        String environmentId = answerValidationResult.environmentId;

        // 설정 반영 실행 환경에서 기준 SELECT 실행 계획과 비용 측정
        ProblemJudgeExecutionResult planResult;
        ProblemExecutionPlanAnalysis planAnalysis;
        try {
            log.info(
                    "SQL 제출 실행 계획 측정 시작 problem={}, environment={}",
                    problemId, environmentId
            );
            acceptProgress(input, running(problemId, PLAN.getKey(), PLAN_RUNNING.getText()));
            OfficialPlanMeasurement officialPlanMeasurement = measureOfficialPlan(input, problemId, environmentId, referenceStatement.sql);
            planResult = officialPlanMeasurement.getPlanResult();
            planAnalysis = executionPlanPolicy.analyze(dataset.getDbmsType(), planResult.getPlanLines(), referenceStatement.sql);
            acceptProgress(input, success(
                    problemId,
                    PLAN.getKey(),
                    PLAN_SUCCESS.getText(),
                    buildPlanDetailLines(dataset.getDbmsType(), planResult, planAnalysis, officialPlanMeasurement)
            ));
            log.info(
                    "SQL 제출 실행 계획 측정 완료 problem={}, environment={}, cost={}, planElement={}",
                    problemId, environmentId, planResult.getCost(), planAnalysis.getExecutionPlanElement()
            );
        } catch (Exception exception) {
            String message = resolveErrorMessage(exception);
            log.warn(
                    "SQL 제출 실행 계획 측정 실패 problem={}, environment={}, reason={}",
                    problemId, environmentId, message, exception
            );
            acceptProgress(input, error(problemId, PLAN.getKey(), PLAN_FAILED.getText(), List.of(message)));
            problemSubmissionRecordService.saveSubmission(
                    problemId,
                    input.getHandle(),
                    dataset.getDbmsType(),
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
        } finally {
            dropQuietly(problemId, environmentId);
        }

        // 제출 이력과 최고 기록 저장 후 최종 제출 결과 반환
        String message = CORRECT_ANSWER.getText();
        long executionTimeMs = resolveExecutionTime(answerResult);
        Double cost = resolveCost(planResult, answerResult);
        problemSubmissionRecordService.saveSubmission(
                problemId,
                input.getHandle(),
                dataset.getDbmsType(),
                storedSubmittedSql,
                true,
                message,
                executionTimeMs,
                cost,
                answerResult.getRowCount(),
                planAnalysis.getExecutionPlanElement(),
                submittedAt
        );
        log.info(
                "SQL 제출 이력 저장 완료 problem={}, handle={}, succeeded={}, cost={}, executionTimeMs={}",
                problemId, input.getHandle(), true, cost, executionTimeMs
        );

        problemSubmissionRecordService.saveBestSolveHistory(
                problemId,
                input.getHandle(),
                dataset.getDbmsType(),
                storedSubmittedSql,
                executionTimeMs,
                cost,
                0,
                planAnalysis.getExecutionPlanElement(),
                submittedAt
        );
        log.info(
                "SQL 제출 최고 기록 저장 완료 problem={}, handle={}, cost={}, executionTimeMs={}",
                problemId, input.getHandle(), cost, executionTimeMs
        );
        return new ProblemSubmissionOutput(problemId, true, CORRECT_ANSWER.getText(), executionTimeMs);
    }

    private ProblemJudgeExecutionResult executeAnswer(String environmentId, String sql) {
        // 출력 데이터 비교용 SELECT 전체 결과 조회
        ProblemJudgeExecutionResult result = problemJudgePort.executeSubmissionAnswerSql(
                "submit-answer-" + UUID.randomUUID(), environmentId, sql
        );

        // 조회 결과 누락 방지
        if (result.getRowCount() > result.getRows().size()) {
            throw badRequest("SQL 실행 결과 행 수가 너무 많습니다.");
        }

        return result;
    }

    private AnswerValidationResult validateAnswerCases(ProblemSubmissionInput input, String problemId,
                                                       ProblemDatasetResolver.ResolvedProblemDataset dataset,
                                                       String sql, List<SubmittedStatement> ddlStatements) {
        // 출력 데이터 검증 대상 케이스와 진행 상태 저장소 준비
        List<ProblemAnswerCase> hiddenAnswerCases = problemAnswerValidationService.findHiddenAnswerCases(problemId);
        List<AnswerCaseDefinition> answerCases = createAnswerCaseDefinitions(hiddenAnswerCases, dataset);
        AnswerCaseProgress answerCaseProgress = new AnswerCaseProgress(input, problemId, answerCases);

        // 모든 케이스의 실행 환경 생성 요청을 큐에 먼저 등록
        List<AnswerCaseEnvironmentTask> environmentTasks = answerCases.stream()
                .map(answerCase -> new AnswerCaseEnvironmentTask(
                        answerCase,
                        CompletableFuture.supplyAsync(
                                () -> createAnswerCaseEnvironment(answerCaseProgress, answerCase),
                                problemSubmitTaskExecutor
                        )
                ))
                .toList();

        // 숨김 케이스와 공개 케이스를 표시 순서대로 검증
        String retainedEnvironmentId = null;
        try {
            for (AnswerCaseEnvironmentTask environmentTask : environmentTasks) {
                AnswerCaseEnvironment environment = awaitAnswerCaseEnvironment(environmentTask);
                ProblemJudgeExecutionResult result = executeAnswerCase(problemId, answerCaseProgress, environment, sql, ddlStatements);
                boolean correct;
                try {
                    correct = matchesAnswerCase(problemId, environment.getDefinition(), result);
                } catch (RuntimeException exception) {
                    answerCaseProgress.markError(environment.getDefinition(), resolveErrorMessage(exception));
                    throw exception;
                }

                if (!correct) {
                    answerCaseProgress.markIncorrect(environment.getDefinition());
                    log.info(
                            "SQL 제출 채점 데이터 비교 오답 problem={}, caseLabel={}, rowCount={}",
                            problemId, environment.getDefinition().getCaseLabel(), result.getRowCount()
                    );
                    return new AnswerValidationResult(false, result, null);
                }

                if (environment.getDefinition().isOpen()) {
                    retainedEnvironmentId = environment.getEnvironmentId();
                    answerCaseProgress.markCorrect(environment.getDefinition());
                    log.info(
                            "SQL 제출 공개 채점 데이터 검증 완료 problem={}, environment={}, rowCount={}",
                            problemId, environment.getEnvironmentId(), result.getRowCount()
                    );
                    return new AnswerValidationResult(true, result, retainedEnvironmentId);
                }

                answerCaseProgress.markCorrect(environment.getDefinition());
                dropQuietly(problemId, environment.getEnvironmentId());
                environment.markDropped();
                log.info(
                        "SQL 제출 숨김 채점 데이터 검증 완료 problem={}, caseLabel={}, rowCount={}",
                        problemId, environment.getDefinition().getCaseLabel(), result.getRowCount()
                );
            }
        } finally {
            dropUnretainedAnswerCaseEnvironments(problemId, environmentTasks, retainedEnvironmentId);
        }

        throw new IllegalStateException(ANSWER_CASE_NOT_FOUND.getMessage());
    }

    private List<AnswerCaseDefinition> createAnswerCaseDefinitions(List<ProblemAnswerCase> hiddenAnswerCases,
                                                                   ProblemDatasetResolver.ResolvedProblemDataset dataset) {
        // 숨김 케이스 정의와 공개 케이스 정의를 표시 순서대로 구성
        List<AnswerCaseDefinition> answerCases = new ArrayList<>();
        for (int index = 0; index < hiddenAnswerCases.size(); index++) {
            ProblemAnswerCase hiddenAnswerCase = hiddenAnswerCases.get(index);
            answerCases.add(AnswerCaseDefinition.hidden(
                    "Case " + (index + 1) + ": Hidden " + (index + 1),
                    hiddenAnswerCase.getDatasetId(), hiddenAnswerCase.getAnswerHash()
            ));
        }
        answerCases.add(AnswerCaseDefinition.open(
                "Case " + (hiddenAnswerCases.size() + 1) + ": Open",
                dataset.getDatasetId()
        ));

        return List.copyOf(answerCases);
    }

    private AnswerCaseEnvironment createAnswerCaseEnvironment(AnswerCaseProgress progress, AnswerCaseDefinition answerCase) {
        // 케이스별 제출 환경을 생성하며 queue와 LVM 세부 진행 상태 갱신
        log.info(
                "SQL 제출 채점 케이스 환경 생성 시작 caseLabel={}, dataset={}",
                answerCase.getCaseLabel(), answerCase.getDatasetId()
        );
        try {
            String environmentId = problemJudgePort.createSubmissionEnvironment(
                    answerCase.getDatasetId(),
                    remainingTasks -> progress.markWaiting(answerCase, remainingTasks),
                    detailLine -> progress.markRunning(answerCase)
            );
            log.info(
                    "SQL 제출 채점 케이스 환경 생성 완료 caseLabel={}, dataset={}, environment={}",
                    answerCase.getCaseLabel(), answerCase.getDatasetId(), environmentId
            );
            return new AnswerCaseEnvironment(answerCase, environmentId);
        } catch (RuntimeException exception) {
            progress.markError(answerCase, resolveErrorMessage(exception));
            throw exception;
        }
    }

    private AnswerCaseEnvironment awaitAnswerCaseEnvironment(AnswerCaseEnvironmentTask environmentTask) {
        // 비동기 환경 생성 결과를 현재 검증 순서에서 대기
        try {
            return environmentTask.getFuture().join();
        } catch (CompletionException exception) {
            throw unwrapCompletionException(exception);
        }
    }

    private ProblemJudgeExecutionResult executeAnswerCase(String problemId, AnswerCaseProgress progress,
                                                          AnswerCaseEnvironment environment,
                                                          String sql, List<SubmittedStatement> ddlStatements) {
        // 케이스 환경에 INDEX DDL 반영 후 제출 SELECT 실행
        progress.markRunning(environment.getDefinition());
        try {
            if (!ddlStatements.isEmpty()) {
                executeDdlStatements(problemId, environment.getEnvironmentId(), ddlStatements);
            }
            return executeAnswer(environment.getEnvironmentId(), sql);
        } catch (RuntimeException exception) {
            progress.markError(environment.getDefinition(), resolveErrorMessage(exception));
            throw exception;
        }
    }

    private boolean matchesAnswerCase(String problemId, AnswerCaseDefinition answerCase,
                                      ProblemJudgeExecutionResult result) {
        // 케이스 유형에 맞는 정답 해시 비교
        if (answerCase.isHidden()) {
            return problemAnswerValidationService.matches(answerCase.getAnswerHash(), result.getColumns(), result.getRows());
        }

        return problemAnswerValidationService.isCorrectAnswer(problemId, result.getColumns(), result.getRows());
    }

    private void dropUnretainedAnswerCaseEnvironments(String problemId, List<AnswerCaseEnvironmentTask> environmentTasks,
                                                      String retainedEnvironmentId) {
        // 실행계획 분석에 재사용하지 않는 케이스 환경 정리
        for (AnswerCaseEnvironmentTask environmentTask : environmentTasks) {
            try {
                AnswerCaseEnvironment environment = environmentTask.getFuture().join();
                if (!environment.isDropped() && !environment.hasEnvironmentId(retainedEnvironmentId)) {
                    dropQuietly(problemId, environment.getEnvironmentId());
                    environment.markDropped();
                }
            } catch (CompletionException exception) {
                log.warn(
                        "SQL 제출 채점 케이스 환경 생성 실패 후 정리 생략 caseLabel={}",
                        environmentTask.getDefinition().getCaseLabel(), exception
                );
            }
        }
    }

    private RuntimeException unwrapCompletionException(CompletionException exception) {
        // CompletableFuture 예외를 기존 비즈니스 예외 흐름에 맞게 복원
        Throwable cause = exception.getCause();
        if (cause instanceof BusinessException businessException) {
            return businessException;
        }
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }

        return new IllegalStateException(resolveErrorMessage(exception), exception);
    }

    private void executeDdlStatements(String problemId, String environmentId,
                                      List<SubmittedStatement> ddlStatements) {
        // 제출 SQL에 포함된 index DDL 순차 반영
        log.info(
                "SQL 제출 INDEX DDL 반영 시작 problem={}, environment={}, indexDdlCount={}",
                problemId, environmentId, ddlStatements.size()
        );
        if (ddlStatements.isEmpty()) {
            log.info("SQL 제출 INDEX DDL 반영 생략 problem={}, environment={}", problemId, environmentId);
            return;
        }

        try {
            for (SubmittedStatement ddlStatement : ddlStatements) {
                log.info(
                        "SQL 제출 INDEX DDL 실행 시작 problem={}, environment={}, statementIndex={}, preview={}",
                        problemId, environmentId, ddlStatement.index, buildSubmittedDdlPreview(ddlStatement.sql)
                );
                problemJudgePort.executeOfficialSql("submit-ddl-" + UUID.randomUUID(), environmentId, ddlStatement.sql);
                log.info(
                        "SQL 제출 INDEX DDL 실행 완료 problem={}, environment={}, statementIndex={}",
                        problemId, environmentId, ddlStatement.index
                );
            }
            log.info("SQL 제출 INDEX DDL 반영 완료 problem={}, environment={}", problemId, environmentId);
        } catch (Exception exception) {
            String message = resolveErrorMessage(exception);
            log.warn(
                    "SQL 제출 INDEX DDL 반영 실패 problem={}, environment={}, reason={}",
                    problemId, environmentId, message, exception
            );
            throw new IllegalStateException(message, exception);
        }
    }

    private ProblemJudgeExecutionResult executePlan(String environmentId, String sql) {
        // 기준 SELECT 실행 계획 조회
        return problemJudgePort.executeOfficialSql("submit-plan-" + UUID.randomUUID(), environmentId, "EXPLAIN " + sql);
    }

    private OfficialPlanMeasurement measureOfficialPlan(ProblemSubmissionInput input, String problemId,
                                                        String environmentId, String sql) {
        // Quertimizer 공식 제출 정책 기준 통계 갱신과 실행 계획 반복 측정 후 비용 중앙값 선택
        List<PlanMeasurement> measurements = new ArrayList<>();
        for (int attempt = 1; attempt <= officialCostPolicy.getMeasurementAttemptCount(); attempt++) {
            log.info(
                    "SQL 제출 통계 갱신 시작 problem={}, environment={}, attempt={}",
                    problemId, environmentId, attempt
            );
            problemJudgePort.analyzeOfficialEnvironment("submit-analyze-" + UUID.randomUUID(), environmentId);
            log.info(
                    "SQL 제출 통계 갱신 완료 problem={}, environment={}, attempt={}",
                    problemId, environmentId, attempt
            );

            ProblemJudgeExecutionResult planResult = executePlan(environmentId, sql);
            measurements.add(new PlanMeasurement(
                    new ProblemPlanMeasurement(attempt, planResult.getCost(), planResult.getPlanLines()),
                    planResult
            ));
            log.info(
                    "SQL 제출 실행 계획 측정 시도 완료 problem={}, environment={}, attempt={}, cost={}",
                    problemId, environmentId, attempt, planResult.getCost()
            );
            acceptProgress(input, running(
                    problemId, PLAN.getKey(), PLAN_RUNNING.getText(),
                    buildPlanMeasurementLines(measurements, null)
            ));
        }

        ProblemPlanMeasurement selectedMeasurement = officialCostPolicy.selectMedianCostMeasurement(
                measurements.stream()
                        .map(PlanMeasurement::getMeasurement)
                        .toList()
        );
        log.info(
                "SQL 제출 실행 계획 중앙값 선택 problem={}, environment={}, attempt={}, cost={}",
                problemId, environmentId, selectedMeasurement.getAttemptOrder(), selectedMeasurement.getCost()
        );
        ProblemJudgeExecutionResult selectedPlanResult = measurements.stream()
                .filter(measurement -> measurement.hasAttemptOrder(selectedMeasurement.getAttemptOrder()))
                .findFirst()
                .map(PlanMeasurement::getPlanResult)
                .orElseThrow(() -> new IllegalStateException(OFFICIAL_COST_SELECTION_FAILED.getMessage()));
        return new OfficialPlanMeasurement(measurements, selectedMeasurement, selectedPlanResult);
    }

    private void dropQuietly(String problemId, String environmentId) {
        // 제출 결과 응답 보호를 위한 정리 실패 로그 기록
        try {
            log.info("SQL 제출 실행 환경 정리 시작 problem={}, environment={}", problemId, environmentId);
            problemJudgePort.dropEnvironment(environmentId);
            log.info("SQL 제출 실행 환경 정리 완료 problem={}, environment={}", problemId, environmentId);
        } catch (Exception exception) {
            log.warn("judge 제출 환경 정리 실패 problem={}, environment={}", problemId, environmentId, exception);
        }
    }

    private List<SubmittedStatement> parseSubmittedStatements(String sql) {
        // 제출 SQL 문장 분리와 SELECT 위치 규칙 검증
        List<ProblemSqlStatement> judgeStatements;
        try {
            judgeStatements = problemJudgePort.parseStatements(normalizeSubmittedSql(sql));
        } catch (IllegalArgumentException exception) {
            throw badRequest(resolveErrorMessage(exception));
        }

        // 제출 SQL 문장 존재 여부 검증
        if (judgeStatements.isEmpty()) {
            throw badRequest(SUBMIT_SQL_REQUIRED.getMessage());
        }

        List<SubmittedStatement> submittedStatements = new ArrayList<>();
        int referenceStatementIndex = -1;
        for (ProblemSqlStatement judgeStatement : judgeStatements) {
            String statementSql = judgeStatement.getSql();
            ProblemJudgeExecutionMode mode = judgeStatement.getMode();
            if (mode == ProblemJudgeExecutionMode.EXPLAIN
                    || mode == ProblemJudgeExecutionMode.EXPLAIN_ANALYZE
                    || mode == ProblemJudgeExecutionMode.COMMAND) {
                throw badRequest(SUBMIT_SELECT_AND_INDEX_DDL_ONLY.getMessage());
            }

            int statementIndex = submittedStatements.size();
            if (mode == ProblemJudgeExecutionMode.SELECT) {
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
            if (statement.mode == ProblemJudgeExecutionMode.INDEX_COMMAND) {
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
        return running(problemId, stepKey, message, List.of());
    }

    private ProblemSubmissionProgress running(String problemId, String stepKey, String message, List<String> detailLines) {
        // 상세 정보 포함 진행 중 진행 상태 생성
        return new ProblemSubmissionProgress(
                problemId, stepKey, "running", message,
                detailLines, null, null, null, null, null
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
        return incorrect(problemId, stepKey, message, List.of());
    }

    private ProblemSubmissionProgress incorrect(String problemId, String stepKey, String message, List<String> detailLines) {
        // 상세 정보 포함 오답 진행 상태 생성
        return new ProblemSubmissionProgress(
                problemId, stepKey, "incorrect", message,
                detailLines, null, null, null, null, null
        );
    }

    private ProblemSubmissionProgress error(String problemId, String stepKey, String message, List<String> detailLines) {
        // 실패 진행 상태 생성
        return new ProblemSubmissionProgress(
                problemId, stepKey, "error", message,
                detailLines, null, null, null, null, null
        );
    }

    private ProblemSubmissionProgress skipped(String problemId, String stepKey, String message) {
        // 생략 진행 상태 생성
        return new ProblemSubmissionProgress(
                problemId, stepKey, "skipped", message,
                List.of(), null, null, null, null, null
        );
    }

    private void acceptProgress(ProblemSubmissionInput input, ProblemSubmissionProgress progress) {
        // 진행 상태 리스너 존재 시 진행 상태 전달
        Consumer<ProblemSubmissionProgress> listener = input.getProgressListener();
        if (listener != null) {
            listener.accept(progress);
        }
    }

    private List<String> buildPlanDetailLines(DbmsType dbmsType, ProblemJudgeExecutionResult planResult,
                                              ProblemExecutionPlanAnalysis planAnalysis,
                                              OfficialPlanMeasurement officialPlanMeasurement) {
        // 반복 측정 결과와 중앙값 선택 표시용 상세 라인 생성
        List<String> detailLines = new ArrayList<>(buildPlanMeasurementLines(
                officialPlanMeasurement.getMeasurements(),
                officialPlanMeasurement.getSelectedMeasurement().getAttemptOrder()
        ));
        detailLines.add(COST_PREFIX.getText() + formatCost(planResult.getCost()));
        detailLines.addAll(executionPlanPolicy.resolveDetailLines(dbmsType, planAnalysis.getExecutionPlanElement()));

        if (detailLines.size() == 1) {
            for (String summaryLine : planAnalysis.getSummaryLines()) {
                detailLines.add(CHECK_PREFIX.getText() + summaryLine);
            }
        }

        return List.copyOf(detailLines);
    }

    private List<String> buildPlanMeasurementLines(List<PlanMeasurement> measurements, Integer selectedAttemptOrder) {
        // 실행 계획 반복 측정 횟수와 비용 상세 라인 생성
        List<String> detailLines = new ArrayList<>();
        for (PlanMeasurement measurement : measurements) {
            detailLines.add("실행계획 분석 %d회 : Cost %s".formatted(
                    measurement.getMeasurement().getAttemptOrder(),
                    formatCost(measurement.getMeasurement().getCost())
            ));
        }
        if (selectedAttemptOrder != null) {
            detailLines.add(SELECTED_PLAN_ATTEMPT_PREFIX + selectedAttemptOrder);
        }

        return List.copyOf(detailLines);
    }

    private String formatCost(BigDecimal cost) {
        // 비용 표시 문자열 생성
        return cost == null ? "-" : "%.1f".formatted(cost.doubleValue());
    }

    private Double resolveCost(ProblemJudgeExecutionResult planResult, ProblemJudgeExecutionResult answerResult) {
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

    private long resolveExecutionTime(ProblemJudgeExecutionResult result) {
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

    private String mergeIndexSqls(List<String> indexSqls, String submittedSql) {
        // 별도 전달된 index DDL과 제출 SQL 병합
        List<String> sqls = new ArrayList<>(indexSqls != null ? indexSqls : List.of());
        sqls.add(submittedSql != null ? submittedSql.trim() : "");
        return sqls.stream()
                .filter(sql -> sql != null && !sql.isBlank())
                .distinct()
                .collect(java.util.stream.Collectors.joining(";\n"));
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
        return ProblemSqlErrorMessageResolver.resolve(exception, SUBMIT_FAILED.getMessage());
    }

    private BusinessException badRequest(String message) {
        // 제출 요청 형식 오류를 비즈니스 예외로 변환
        return new BusinessException(message, HttpStatus.BAD_REQUEST);
    }

    private final class AnswerCaseProgress {
        private final ProblemSubmissionInput input;
        private final String problemId;
        private final List<AnswerCaseDefinition> answerCases;
        private final Set<String> terminalStepKeys = new HashSet<>();

        private AnswerCaseProgress(ProblemSubmissionInput input, String problemId, List<AnswerCaseDefinition> answerCases) {
            this.input = input;
            this.problemId = problemId;
            this.answerCases = List.copyOf(answerCases);
            for (AnswerCaseDefinition answerCase : answerCases) {
                acceptProgress(input, running(problemId, answerCase.getStepKey(), answerCase.waitingMessage()));
            }
        }

        private void markWaiting(AnswerCaseDefinition answerCase, int remainingTasks) {
            // 케이스 대기열 순번을 제목에 반영
            synchronized (this) {
                if (terminalStepKeys.contains(answerCase.getStepKey())) {
                    return;
                }
                acceptProgress(input, running(
                        problemId, answerCase.getStepKey(),
                        answerCase.waitingMessage(remainingTasks)
                ));
            }
        }

        private void markRunning(AnswerCaseDefinition answerCase) {
            // 케이스 채점 중 제목으로 진행 상태 전환
            synchronized (this) {
                if (terminalStepKeys.contains(answerCase.getStepKey())) {
                    return;
                }
                acceptProgress(input, running(problemId, answerCase.getStepKey(), answerCase.runningMessage()));
            }
        }

        private void markCorrect(AnswerCaseDefinition answerCase) {
            // 케이스 정답 완료 progress 전달
            synchronized (this) {
                if (!terminalStepKeys.add(answerCase.getStepKey())) {
                    return;
                }
                acceptProgress(input, success(problemId, answerCase.getStepKey(), answerCase.correctMessage()));
            }
        }

        private void markIncorrect(AnswerCaseDefinition answerCase) {
            // 케이스 오답 완료와 후속 케이스 생략 progress 전달
            synchronized (this) {
                if (!terminalStepKeys.add(answerCase.getStepKey())) {
                    return;
                }
                acceptProgress(input, incorrect(problemId, answerCase.getStepKey(), answerCase.incorrectMessage()));
                markRemainingSkipped(answerCase);
            }
        }

        private void markError(AnswerCaseDefinition answerCase, String message) {
            // 케이스 실패와 후속 케이스 생략 progress 전달
            synchronized (this) {
                if (!terminalStepKeys.add(answerCase.getStepKey())) {
                    return;
                }
                acceptProgress(input, error(problemId, answerCase.getStepKey(), answerCase.failedMessage(), List.of(message)));
                markRemainingSkipped(answerCase);
            }
        }

        private void markRemainingSkipped(AnswerCaseDefinition failedAnswerCase) {
            // 실패 케이스 이후 미완료 케이스 생략 처리
            boolean skipTarget = false;
            for (AnswerCaseDefinition answerCase : answerCases) {
                if (skipTarget && terminalStepKeys.add(answerCase.getStepKey())) {
                    acceptProgress(input, skipped(problemId, answerCase.getStepKey(), answerCase.skippedMessage()));
                }
                if (answerCase == failedAnswerCase) {
                    skipTarget = true;
                }
            }
        }
    }

    private static final class AnswerCaseDefinition {
        private final String stepKey;
        private final String caseLabel;
        private final Long datasetId;
        private final String answerHash;
        private final boolean hidden;

        private static AnswerCaseDefinition hidden(String caseLabel, Long datasetId, String answerHash) {
            // 숨김 케이스 진행 상태 정의 생성
            int hiddenOrder = Integer.parseInt(caseLabel.replaceFirst(".*Hidden\\s+", "").trim());
            return new AnswerCaseDefinition("answer-hidden-" + hiddenOrder, "Case Hidden " + hiddenOrder, datasetId, answerHash, true);
        }

        private static AnswerCaseDefinition open(String caseLabel, Long datasetId) {
            // 공개 케이스 진행 상태 정의 생성
            return new AnswerCaseDefinition("answer-open", "Case Open", datasetId, null, false);
        }

        private AnswerCaseDefinition(String stepKey, String caseLabel, Long datasetId, String answerHash, boolean hidden) {
            this.stepKey = stepKey;
            this.caseLabel = caseLabel;
            this.datasetId = datasetId;
            this.answerHash = answerHash;
            this.hidden = hidden;
        }

        private String waitingMessage(int remainingTasks) {
            // 케이스 대기 중 제목 생성
            return caseLabel + " 채점 대기 중 - " + remainingTasks;
        }

        private String waitingMessage() {
            // 케이스 초기 대기 중 제목 생성
            return caseLabel + " 채점 대기 중";
        }

        private String runningMessage() {
            // 케이스 채점 중 제목 생성
            return caseLabel + " 채점 중";
        }

        private String correctMessage() {
            // 케이스 정답 완료 제목 생성
            return caseLabel + " 채점 완료 - 정답";
        }

        private String incorrectMessage() {
            // 케이스 오답 완료 제목 생성
            return caseLabel + " 채점 완료 - 오답";
        }

        private String failedMessage() {
            // 케이스 실패 제목 생성
            return caseLabel + " 채점 실패";
        }

        private String skippedMessage() {
            // 케이스 생략 제목 생성
            return caseLabel + " 채점 생략";
        }

        private String getStepKey() {
            return stepKey;
        }

        private String getCaseLabel() {
            return caseLabel;
        }

        private Long getDatasetId() {
            return datasetId;
        }

        private String getAnswerHash() {
            return answerHash;
        }

        private boolean isHidden() {
            return hidden;
        }

        private boolean isOpen() {
            return !hidden;
        }

    }

    private static final class AnswerCaseEnvironment {
        private final AnswerCaseDefinition definition;
        private final String environmentId;
        private boolean dropped;

        private AnswerCaseEnvironment(AnswerCaseDefinition definition, String environmentId) {
            this.definition = definition;
            this.environmentId = environmentId;
        }

        private AnswerCaseDefinition getDefinition() {
            return definition;
        }

        private String getEnvironmentId() {
            return environmentId;
        }

        private boolean isDropped() {
            return dropped;
        }

        private void markDropped() {
            this.dropped = true;
        }

        private boolean hasEnvironmentId(String targetEnvironmentId) {
            return environmentId != null && environmentId.equals(targetEnvironmentId);
        }
    }

    private static final class AnswerCaseEnvironmentTask {
        private final AnswerCaseDefinition definition;
        private final CompletableFuture<AnswerCaseEnvironment> future;

        private AnswerCaseEnvironmentTask(AnswerCaseDefinition definition,
                                          CompletableFuture<AnswerCaseEnvironment> future) {
            this.definition = definition;
            this.future = future;
        }

        private AnswerCaseDefinition getDefinition() {
            return definition;
        }

        private CompletableFuture<AnswerCaseEnvironment> getFuture() {
            return future;
        }
    }

    private static final class AnswerValidationResult {
        private final boolean correct;
        private final ProblemJudgeExecutionResult answerResult;
        private final String environmentId;

        private AnswerValidationResult(boolean correct, ProblemJudgeExecutionResult answerResult, String environmentId) {
            this.correct = correct;
            this.answerResult = answerResult;
            this.environmentId = environmentId;
        }
    }

    private static final class SubmittedStatement {
        private final String key;
        private final int index;
        private final String sql;
        private final ProblemJudgeExecutionMode mode;
        private final boolean referenceSelect;

        private SubmittedStatement(String key, int index, String sql, ProblemJudgeExecutionMode mode, boolean referenceSelect) {
            this.key = key;
            this.index = index;
            this.sql = sql;
            this.mode = mode;
            this.referenceSelect = referenceSelect;
        }
    }

    private static final class OfficialPlanMeasurement {
        private final List<PlanMeasurement> measurements;
        private final ProblemPlanMeasurement selectedMeasurement;
        private final ProblemJudgeExecutionResult planResult;

        private OfficialPlanMeasurement(List<PlanMeasurement> measurements,
                                        ProblemPlanMeasurement selectedMeasurement,
                                        ProblemJudgeExecutionResult planResult) {
            this.measurements = List.copyOf(measurements);
            this.selectedMeasurement = selectedMeasurement;
            this.planResult = planResult;
        }

        private List<PlanMeasurement> getMeasurements() {
            return measurements;
        }

        private ProblemPlanMeasurement getSelectedMeasurement() {
            return selectedMeasurement;
        }

        private ProblemJudgeExecutionResult getPlanResult() {
            return planResult;
        }
    }

    private static final class PlanMeasurement {
        private final ProblemPlanMeasurement measurement;
        private final ProblemJudgeExecutionResult planResult;

        private PlanMeasurement(ProblemPlanMeasurement measurement, ProblemJudgeExecutionResult planResult) {
            this.measurement = measurement;
            this.planResult = planResult;
        }

        private ProblemPlanMeasurement getMeasurement() {
            return measurement;
        }

        private ProblemJudgeExecutionResult getPlanResult() {
            return planResult;
        }

        private boolean hasAttemptOrder(int attemptOrder) {
            return measurement.getAttemptOrder() == attemptOrder;
        }
    }
}
