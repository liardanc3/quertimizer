package com.quertimizer.problem.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.global.lock.Lock;
import com.quertimizer.global.lock.LockKey;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.input.ProblemCreateInput;
import com.quertimizer.problem.application.output.ProblemCreateOutput;
import com.quertimizer.problem.application.output.ProblemCreateProgress;
import com.quertimizer.problem.application.port.in.CreateProblemUseCase;
import com.quertimizer.problem.application.port.out.ProblemAnswerCaseRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemJudgePort;
import com.quertimizer.problem.application.port.out.ProblemRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemSetHiddenCaseRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemSetRepositoryPort;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemAnswerCase;
import com.quertimizer.problem.domain.entity.ProblemSet;
import com.quertimizer.problem.domain.entity.ProblemSetHiddenCase;
import com.quertimizer.problem.domain.model.ProblemCreateProgressStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.HIDDEN_DATA_REQUIRED;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_CREATE_FAILED;
import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_SET_NOT_FOUND;
import static com.quertimizer.problem.domain.model.ProblemCreateProgressStatus.ERROR;
import static com.quertimizer.problem.domain.model.ProblemCreateProgressStatus.RUNNING;
import static com.quertimizer.problem.domain.model.ProblemCreateProgressStatus.SUCCESS;
import static com.quertimizer.problem.domain.model.ProblemCreateProgressStep.ANSWER_HASH;
import static com.quertimizer.problem.domain.model.ProblemCreateProgressStep.DATA_EXAMPLE;
import static com.quertimizer.problem.domain.model.ProblemCreateProgressStep.ERD_INFO;
import static com.quertimizer.problem.domain.model.ProblemCreateProgressStep.OPEN_DATA;
import static com.quertimizer.problem.domain.model.ProblemCreateProgressStep.OUTPUT_EXAMPLE;
import static com.quertimizer.problem.domain.model.ProblemCreateProgressStep.TABLE_INFO;
import static com.quertimizer.problem.domain.model.ProblemCreateProgressText.hiddenDataKey;
import static com.quertimizer.problem.domain.model.ProblemCreateProgressText.hiddenDataRunningMessage;
import static com.quertimizer.problem.domain.model.ProblemCreateProgressText.hiddenDataSuccessMessage;
import static org.springframework.util.StringUtils.hasText;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateProblem implements CreateProblemUseCase {

    private final ProblemRepositoryPort problemRepository;
    private final ProblemSetRepositoryPort problemSetRepository;
    private final ProblemAnswerCaseRepositoryPort problemAnswerCaseRepository;
    private final ProblemSetHiddenCaseRepositoryPort problemSetHiddenCaseRepository;
    private final ProblemJudgePort problemJudgePort;
    private final ProblemExampleService problemExampleService;

    /**
     * 문제를 생성한다.
     *
     * <ol>
     *   <li>ProblemSet 조회 또는 DBMS별 신규 번호 생성 후 데이터셋 연결
     *   <li>Problem 조회 또는 테이블셋별 신규 번호 생성 후 예시와 정답 해시 연결
     *   <li>ProblemSet, Problem, Hidden AnswerCase 저장
     *   <li>문제 번호 반환
     * </ol>
     *
     * @param input 문제 생성 요청 입력
     */
    @Transactional
    @Lock(prefix = LockKey.CREATE_PROBLEM, timeout = 5000)
    @Override
    @Log("문제 생성")
    public ProblemCreateOutput execute(ProblemCreateInput input) {
        List<Long> createdDatasetIds = new ArrayList<>();
        AtomicBoolean createdProblem = new AtomicBoolean(false);
        boolean createdProblemSet = !hasText(input.getProblemSetId());

        try {
            ProblemSet problemSet = !createdProblemSet
                    ? problemSetRepository.findByProblemSetId(input.getProblemSetId())
                            .orElseThrow(() -> new BusinessException(PROBLEM_SET_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND))
                    : problemSetRepository.save(createProblemSet(input)
                            .validateSql()
                            .updateDatasetId(createProblemSetDataset(input, createdDatasetIds)));
            ProblemSet resolvedProblemSet = problemSet;

            Problem problem = problemRepository.findByProblemId(input.getProblemId())
                    .map(p -> p.update(input.getTitle(), input.getDescription(), input.getCondition(), input.getOutput()))
                    .orElseGet(() -> {
                        createdProblem.set(true);
                        return createProblem(input, resolvedProblemSet).validateSql();
                    });

            problemSet = problemSetRepository.save(resolvedProblemSet);
            problem = problemRepository.save(problem);
            if (createdProblem.get()) {
                List<ProblemSetHiddenCase> hiddenCases = createdProblemSet
                        ? saveProblemSetHiddenCases(problemSet, input, createdDatasetIds)
                        : findProblemSetHiddenCases(problemSet.getProblemSetId());
                saveProblemAnswerCases(problem, problemSet, input, hiddenCases);
            }

            return new ProblemCreateOutput(problem.getProblemId());
        } catch (RuntimeException exception) {
            log.error("문제 생성 실패", exception);

            if (!createdDatasetIds.isEmpty()) {
                deleteDatasetsQuietly(createdDatasetIds);
                log.info("데이터셋 {} 롤백 완료", createdDatasetIds);
            }

            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(PROBLEM_CREATE_FAILED.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Problem createProblem(ProblemCreateInput input, ProblemSet problemSet) {
        // 실제 데이터셋 기준 정답 해시와 표시용 예시 생성
        String answerHash = executeProgressStep(input, ANSWER_HASH,
                () -> createAnswerHash(problemSet.getDatasetId(), input.getAnswerSql()));
        String environmentId = problemJudgePort.createSubmissionEnvironment(problemSet.getDatasetId());
        try {
            String schemaMetadata = executeProgressStep(input, TABLE_INFO,
                    () -> problemExampleService.createSchemaMetadata(environmentId, input.getProblemDdl(), input.getDbmsType()));
            runProgressStep(input, ERD_INFO, () -> {
            });
            String dataExample = executeProgressStep(input, DATA_EXAMPLE,
                    () -> problemExampleService.createDataExample(environmentId, input.getProblemDdl(), input.getDbmsType()));
            String outputExample = executeProgressStep(input, OUTPUT_EXAMPLE,
                    () -> problemExampleService.createOutputExample(environmentId, input.getAnswerSql()));

            // 신규 문제 기본 정보 생성
            return Problem.create(
                    createNextProblemId(problemSet.getProblemSetId()), problemSet.getProblemSetId(),
                    input.getTitle(), input.getDescription(), input.getProblemDdl(), input.getDbmsType(),
                    input.getCondition(), input.getOutput(),
                    dataExample, outputExample, schemaMetadata, answerHash, input.getAnswerSql()
            );
        } finally {
            dropEnvironmentQuietly(environmentId);
        }
    }

    private ProblemSet createProblemSet(ProblemCreateInput input) {
        // 신규 문제셋 기본 정보 생성
        return ProblemSet.create(
                createNextProblemSetId(input.getDbmsType()), input.getDdl(),
                input.getActualDataSql(), input.getDbmsType()
        );
    }

    private String createNextProblemSetId(DbmsType dbmsType) {
        // DBMS별 마지막 문제 테이블셋 번호 기준 다음 번호 생성
        int nextSequence = problemSetRepository.findLatestProblemSetIdByDbmsType(dbmsType)
                .map(this::extractProblemSetSequence)
                .map(sequence -> sequence + 1)
                .orElse(1);
        return dbmsType.getIdPrefix() + formatFiveDigits(nextSequence);
    }

    private String createNextProblemId(String problemSetId) {
        // 문제 테이블셋별 마지막 문제 번호 기준 다음 번호 생성
        int nextSequence = problemRepository.findLatestProblemIdByProblemSetId(problemSetId)
                .map(this::extractProblemSequence)
                .map(sequence -> sequence + 1)
                .orElse(1);
        return problemSetId + "-" + formatFiveDigits(nextSequence);
    }

    private int extractProblemSetSequence(String problemSetId) {
        // 문제 테이블셋 번호에서 숫자 영역 추출
        return Integer.parseInt(problemSetId.substring(1));
    }

    private int extractProblemSequence(String problemId) {
        // 문제 번호에서 테이블셋 내부 순번 추출
        String[] tokens = problemId.split("-");
        return Integer.parseInt(tokens[tokens.length - 1]);
    }

    private String formatFiveDigits(int sequence) {
        // 다섯 자리 문제 번호 문자열 생성
        return "%05d".formatted(sequence);
    }

    private Long createProblemSetDataset(ProblemCreateInput input, List<Long> createdDatasetIds) {
        // 문제셋 데이터셋 생성 후 롤백 대상 ID 보관
        return executeProgressStep(input, OPEN_DATA, () -> {
            Long datasetId = createDataset(input.getDbmsType(), input.getDdl(), input.getActualDataSql());
            createdDatasetIds.add(datasetId);
            return datasetId;
        });
    }

    private List<ProblemSetHiddenCase> saveProblemSetHiddenCases(ProblemSet problemSet, ProblemCreateInput input,
                                                                 List<Long> createdDatasetIds) {
        // 숨김 채점 데이터 존재 여부 검사
        if (input.getHiddenDataSqls().isEmpty()) {
            throw new BusinessException(HIDDEN_DATA_REQUIRED.getMessage(), HttpStatus.BAD_REQUEST);
        }

        // 문제 테이블셋 기준 숨김 채점 데이터셋 생성
        List<ProblemSetHiddenCase> hiddenCases = new ArrayList<>();
        for (int index = 0; index < input.getHiddenDataSqls().size(); index++) {
            String hiddenDataSql = input.getHiddenDataSqls().get(index);
            int sequence = index + 1;
            int stepOrder = index + 7;
            Long datasetId = executeProgressStep(
                    input, hiddenDataKey(sequence), hiddenDataRunningMessage(sequence), hiddenDataSuccessMessage(sequence), stepOrder,
                    () -> {
                        Long createdDatasetId = createInlineDataset(input.getDbmsType(), input.getDdl(), hiddenDataSql);
                        createdDatasetIds.add(createdDatasetId);
                        return createdDatasetId;
                    }
            );
            hiddenCases.add(ProblemSetHiddenCase.create(problemSet.getProblemSetId(), datasetId, sequence));
        }
        return problemSetHiddenCaseRepository.saveAll(hiddenCases);
    }

    private List<ProblemSetHiddenCase> findProblemSetHiddenCases(String problemSetId) {
        // 문제 테이블셋 기준 숨김 채점 케이스 존재 여부 검사
        List<ProblemSetHiddenCase> hiddenCases = problemSetHiddenCaseRepository.findAllByProblemSetIdOrderByCaseOrderAsc(problemSetId);
        if (hiddenCases.isEmpty()) {
            throw new BusinessException(HIDDEN_DATA_REQUIRED.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return hiddenCases;
    }

    private void saveProblemAnswerCases(Problem problem, ProblemSet problemSet,
                                        ProblemCreateInput input, List<ProblemSetHiddenCase> hiddenCases) {
        // 실제 채점 케이스와 공유 숨김 채점 케이스 기준 정답 해시 생성
        List<ProblemAnswerCase> answerCases = new ArrayList<>();
        answerCases.add(ProblemAnswerCase.actual(problem.getProblemId(), problemSet.getDatasetId(), problem.getAnswerHash()));
        for (ProblemSetHiddenCase hiddenCase : hiddenCases) {
            answerCases.add(ProblemAnswerCase.hidden(
                    problem.getProblemId(), hiddenCase.getDatasetId(),
                    createAnswerHash(hiddenCase.getDatasetId(), input.getAnswerSql()),
                    hiddenCase.getCaseOrder()
            ));
        }

        // 문제 번호 기준 정답 케이스 교체 저장
        problemAnswerCaseRepository.deleteAllByProblemId(problem.getProblemId());
        problemAnswerCaseRepository.saveAll(answerCases);
    }

    private String createAnswerHash(Long datasetId, String answerSql) {
        // 문제 테이블셋 데이터셋 기준 정답 SQL 해시 생성
        return problemJudgePort.createAnswerHash(datasetId, answerSql);
    }

    private Long createDataset(DbmsType dbmsType, String ddl, String dataSql) {
        // SQL 자료를 judge 데이터셋 생성 입력으로 변환 후 키 반환
        return problemJudgePort.createDataset(dbmsType, ddl, dataSql);
    }

    private Long createInlineDataset(DbmsType dbmsType, String ddl, String dataSql) {
        // 문제셋 원본 행 없이 보관할 SQL 자료를 judge 데이터셋으로 변환 후 키 반환
        return problemJudgePort.createInlineDataset(dbmsType, ddl, dataSql);
    }

    private void dropEnvironmentQuietly(String environmentId) {
        // 예시 생성 실행 환경 제거 실패 로그 기록
        try {
            problemJudgePort.dropEnvironment(environmentId);
        } catch (Exception exception) {
            log.warn("문제 예시 실행 환경 정리 실패 environment={}", environmentId, exception);
        }
    }

    private void deleteDatasetsQuietly(List<Long> datasetIds) {
        // 데이터셋 목록 제거 실패 로그 기록
        for (Long datasetId : datasetIds) {
            try {
                problemJudgePort.deleteDataset(datasetId);
            } catch (Exception exception) {
                log.error("데이터셋 정리 실패 dataset={}", datasetId, exception);
            }
        }
    }

    private ProblemCreateProgress running(String stepKey, String message, Integer stepOrder) {
        // 문제 생성 진행 상태 생성 중 메시지 생성
        return new ProblemCreateProgress(stepKey, RUNNING.getValue(), message, stepOrder);
    }

    private ProblemCreateProgress success(String stepKey, String message, Integer stepOrder) {
        // 문제 생성 진행 상태 생성 완료 메시지 생성
        return new ProblemCreateProgress(stepKey, SUCCESS.getValue(), message, stepOrder);
    }

    private ProblemCreateProgress error(String stepKey, String runningMessage, Integer stepOrder) {
        // 문제 생성 진행 상태 생성 실패 메시지 생성
        return new ProblemCreateProgress(stepKey, ERROR.getValue(), failMessage(runningMessage), stepOrder);
    }

    private <T> T executeProgressStep(ProblemCreateInput input, ProblemCreateProgressStep step, Supplier<T> supplier) {
        // 고정 진행 단계를 메시지 전달 가능 단계로 변환
        return executeProgressStep(input, step.getKey(), step.getRunningMessage(), step.getSuccessMessage(), step.getOrder(), supplier);
    }

    private <T> T executeProgressStep(ProblemCreateInput input, String stepKey,
                                      String runningMessage, String successMessage,
                                      Integer stepOrder, Supplier<T> supplier) {
        // 진행 단계 시작 후 성공 또는 실패 상태 전달
        acceptProgress(input, running(stepKey, runningMessage, stepOrder));
        try {
            T result = supplier.get();
            acceptProgress(input, success(stepKey, successMessage, stepOrder));
            return result;
        } catch (RuntimeException exception) {
            acceptProgress(input, error(stepKey, runningMessage, stepOrder));
            throw exception;
        }
    }

    private void runProgressStep(ProblemCreateInput input, ProblemCreateProgressStep step, Runnable runnable) {
        // 반환값 없는 고정 진행 단계를 값 반환 단계로 변환
        executeProgressStep(input, step, () -> {
            runnable.run();
            return null;
        });
    }

    private String failMessage(String runningMessage) {
        // 진행 중 메시지를 실패 메시지로 변환
        if (runningMessage.endsWith(" 중")) {
            return runningMessage.substring(0, runningMessage.length() - 2) + " 실패";
        }
        return runningMessage + " 실패";
    }

    private void acceptProgress(ProblemCreateInput input, ProblemCreateProgress progress) {
        // 문제 생성 진행 상태 수신자 있으면 전달
        Consumer<ProblemCreateProgress> listener = input.getProgressListener();
        if (listener != null) {
            listener.accept(progress);
        }
    }

}
