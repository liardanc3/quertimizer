package com.quertimizer.problem.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.problem.application.input.ProblemExamplePreviewInput;
import com.quertimizer.problem.application.output.ProblemDataExampleOutput;
import com.quertimizer.problem.application.output.ProblemExamplePreviewOutput;
import com.quertimizer.problem.application.output.ProblemOutputPreviewOutput;
import com.quertimizer.problem.application.port.in.PreviewProblemExamplesUseCase;
import com.quertimizer.problem.application.port.out.ProblemJudgePort;
import com.quertimizer.problem.application.port.out.ProblemOutputPreviewRateLimitPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PreviewProblemExamples implements PreviewProblemExamplesUseCase {

    private final ProblemJudgePort problemJudgePort;
    private final ProblemOutputPreviewRateLimitPort problemOutputPreviewRateLimitPort;
    private final ProblemExampleService problemExampleService;

    /**
     * 문제 생성 화면의 데이터 예시와 출력 예시를 함께 생성한다.
     *
     * <ol>
     *   <li>예시 실행 요청 제한 확인
     *   <li>예시용 임시 데이터셋과 환경 생성
     *   <li>테이블별 데이터 예시와 정답 SQL 출력 예시 생성
     *   <li>예시용 임시 데이터셋과 환경 정리
     * </ol>
     *
     * @param input 예시 생성 입력
     */
    @Override
    @Log("데이터 예시 미리보기")
    public ProblemExamplePreviewOutput execute(ProblemExamplePreviewInput input) {
        problemOutputPreviewRateLimitPort.validate(input.getRequester(), input.getClientIp());

        String datasetId = createDataset(input);
        String environmentId = problemJudgePort.createSubmissionEnvironment(datasetId);
        try {
            ProblemDataExampleOutput dataExample = problemExampleService.createDataPreview(environmentId, input.getProblemDdl(), input.getDbmsType());
            ProblemOutputPreviewOutput outputExample = problemExampleService.createOutputPreview(environmentId, input.getAnswerSql());
            return new ProblemExamplePreviewOutput(dataExample, outputExample);
        } finally {
            dropEnvironmentQuietly(environmentId);
            deleteDatasetQuietly(datasetId);
        }
    }

    private String createDataset(ProblemExamplePreviewInput input) {
        // 예시 SQL 자료를 judge 데이터셋으로 등록
        return problemJudgePort.createTemporaryDataset(input.getDbmsType(), input.getDdl(), input.getActualDataSql());
    }

    private void dropEnvironmentQuietly(String environmentId) {
        // 예시 임시 실행 환경 제거 실패 로그 기록
        try {
            problemJudgePort.dropEnvironment(environmentId);
        } catch (Exception exception) {
            log.warn("문제 예시 임시 judge 실행 환경 정리 실패 environmentId={}", environmentId, exception);
        }
    }

    private void deleteDatasetQuietly(String datasetId) {
        // 예시 임시 데이터셋 제거 실패 로그 기록
        try {
            problemJudgePort.deleteDataset(datasetId);
        } catch (Exception exception) {
            log.warn("문제 예시 임시 judge 데이터셋 정리 실패 datasetId={}", datasetId, exception);
        }
    }
}
