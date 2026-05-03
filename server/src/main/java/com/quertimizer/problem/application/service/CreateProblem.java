package com.quertimizer.problem.application.service;

import com.quertimizer.problem.application.port.in.CreateProblemUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.global.lock.Lock;
import com.quertimizer.global.lock.LockKey;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.input.ProblemCreateInput;
import com.quertimizer.problem.application.output.ProblemJudgeExecutionResult;
import com.quertimizer.problem.application.output.ProblemCreateOutput;
import com.quertimizer.problem.application.output.ProblemOutputPreviewOutput;
import com.quertimizer.problem.application.port.out.ProblemJudgePort;
import com.quertimizer.problem.application.port.out.ProblemRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemSetRepositoryPort;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_CREATE_FAILED;
import static org.springframework.util.StringUtils.hasText;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateProblem implements CreateProblemUseCase {

    private final ProblemRepositoryPort problemRepository;
    private final ProblemSetRepositoryPort problemSetRepository;
    private final ProblemJudgePort problemJudgePort;
    private final ObjectMapper objectMapper;

    /**
     * 문제를 생성한다.
     *
     * <ol>
     *   <li>ProblemSet 조회(생성) 후 업데이트(dataSetId 생성)
     *   <li>Problem 조회(생성) 후 업데이트(AnswerHash, SampleOutput 생성)
     *   <li>ProblemSet, Problem 저장
     *   <li>문제 번호 반환
     * </ol>
     *
     * @param input 문제 생성 요청 입력
     */
    @Transactional
    @Lock(prefix = LockKey.CREATE_PROBLEM, timeout = 5000)
    @Override
    public ProblemCreateOutput execute(ProblemCreateInput input) {
        AtomicReference<String> createdDatasetId = new AtomicReference<>();

        try {
            ProblemSet problemSet = problemSetRepository.findByProblemSetId(input.getProblemSetId())
                    .orElseGet(() ->
                            problemSetRepository.save(createProblemSet(input)
                                    .validateSql()
                                    .updateDatasetId(createProblemSetDataset(input, createdDatasetId)))
                    );
            ProblemSet resolvedProblemSet = problemSet;

            Problem problem = problemRepository.findByProblemId(input.getProblemId())
                    .map(p -> p.update(input.getTitle(), input.getDescription(), input.getCondition(), input.getOutput()))
                    .orElseGet(() ->
                            createProblem(input, resolvedProblemSet)
                                    .validateSql()
                                    .updateAnswerHash(createAnswerHash(resolvedProblemSet.getDatasetId(), input.getAnswerSql()))
                                    .updateSampleOutput(createSampleOutput(input))
                    );

            problemSet = problemSetRepository.save(resolvedProblemSet);
            problem = problemRepository.save(problem);

            return new ProblemCreateOutput(problem.getProblemId());
        } catch (RuntimeException exception) {
            log.error("문제 생성 실패");

            String rollbackDatasetId = createdDatasetId.get();
            if (hasText(rollbackDatasetId)) {
                deleteDatasetQuietly(rollbackDatasetId);
                log.info("데이터셋 {} 롤백 완료", rollbackDatasetId);
            }

            throw new BusinessException(PROBLEM_CREATE_FAILED.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Problem createProblem(ProblemCreateInput input, ProblemSet problemSet) {
        // 신규 문제 기본 정보 생성
        return Problem.create(
                problemSet.getProblemSetId(),
                input.getTitle(), input.getDescription(), input.getDdl(), input.getDbmsType(),
                input.getCondition(), input.getOutput(), input.getSampleDataSql(), input.getAnswerSql()
        );
    }

    private ProblemSet createProblemSet(ProblemCreateInput input) {
        // 신규 문제셋 기본 정보 생성
        return ProblemSet.create(input.getDdl(), input.getActualDataSql(), input.getDbmsType());
    }

    private String createProblemSetDataset(ProblemCreateInput input, AtomicReference<String> createdDatasetId) {
        // 문제셋 데이터셋 생성 후 롤백 대상 ID 보관
        String datasetId = createDataset(input.getDbmsType(), input.getDdl(), input.getActualDataSql());
        createdDatasetId.set(datasetId);
        return datasetId;
    }

    private String createAnswerHash(String datasetId, String answerSql) {
        // 문제 테이블셋 데이터셋 기준 정답 SQL 해시 생성
        return problemJudgePort.createAnswerHash(datasetId, answerSql);
    }

    private String createSampleOutput(ProblemCreateInput input) {
        // 예시 데이터셋 생성
        String sampleDatasetId = createDataset(input.getDbmsType(), input.getDdl(), input.getSampleDataSql());

        // 예시 데이터셋에서 정답 SQL 실행 후 예시 출력 직렬화, 종료 시 임시 데이터셋 제거
        try {
            ProblemJudgeExecutionResult output = problemJudgePort.executeIsolatedOfficialSql(
                    "problem-create-" + UUID.randomUUID(), sampleDatasetId, input.getAnswerSql()
            );
            return objectMapper.writeValueAsString(ProblemOutputPreviewOutput.from(output));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("예시 출력 직렬화에 실패했다.", exception);
        } finally {
            deleteDatasetQuietly(sampleDatasetId);
        }
    }

    private String createDataset(DbmsType dbmsType, String ddl, String dataSql) {
        // 문제 SQL 자료를 judge 데이터셋 생성 입력으로 변환 후 키 반환
        return problemJudgePort.createDataset(dbmsType, ddl, dataSql);
    }

    private void deleteDatasetQuietly(String datasetId) {
        // 데이터셋 제거 실패 로그 기록
        try {
            problemJudgePort.deleteDataset(datasetId);
        } catch (Exception exception) {
            log.error("데이터셋 정리 실패 datasetId={}", datasetId, exception);
        }
    }

}
