package com.quertimizer.problem.application.service;

import com.quertimizer.problem.application.port.in.PreviewProblemUseCase;
import com.quertimizer.problem.application.input.ProblemOutputPreviewInput;
import com.quertimizer.problem.application.output.ProblemOutputPreviewOutput;
import com.quertimizer.problem.application.port.out.ProblemJudgePort;
import com.quertimizer.problem.application.port.out.ProblemOutputPreviewRateLimitPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PreviewProblem implements PreviewProblemUseCase {

    private final ProblemJudgePort problemJudgePort;
    private final ProblemOutputPreviewRateLimitPort problemOutputPreviewRateLimitPort;
    private final ProblemExampleService problemExampleService;

    /**
     * 문제 생성 화면의 출력 예시를 생성한다.
     *
     * <ol>
     *   <li>출력 예시 실행 요청 제한 확인
     *   <li>출력 예시용 임시 데이터셋과 환경 생성
     *   <li>기준 SQL 실행과 결과 변환
     *   <li>출력 예시용 임시 데이터셋과 환경 정리
     * </ol>
     *
     * @param input 출력 예시 생성 입력
     */
    @Override
    public ProblemOutputPreviewOutput execute(ProblemOutputPreviewInput input) {
        problemOutputPreviewRateLimitPort.validate(input.getRequester(), input.getClientIp());

        String datasetId = createDataset(input);
        String environmentId = problemJudgePort.createSubmissionEnvironment(datasetId);
        try {
            return problemExampleService.createOutputPreview(environmentId, input.getAnswerSql());
        } finally {
            dropEnvironmentQuietly(environmentId);
            deleteDatasetQuietly(datasetId);
        }
    }

    private String createDataset(ProblemOutputPreviewInput input) {
        // 출력 예시 SQL 자료를 judge 데이터셋으로 등록
        return problemJudgePort.createTemporaryDataset(input.getDbmsType(), input.getDdl(), input.getActualDataSql());
    }

    private void dropEnvironmentQuietly(String environmentId) {
        // 출력 예시 임시 실행 환경 제거 실패 로그 기록
        try {
            problemJudgePort.dropEnvironment(environmentId);
        } catch (Exception exception) {
            log.warn("출력 예시 임시 judge 실행 환경 정리 실패 environmentId={}", environmentId, exception);
        }
    }

    private void deleteDatasetQuietly(String datasetId) {
        // 출력 예시 임시 데이터셋 제거 실패 로그 기록
        try {
            problemJudgePort.deleteDataset(datasetId);
        } catch (Exception exception) {
            log.warn("출력 예시 임시 judge 데이터셋 정리 실패 datasetId={}", datasetId, exception);
        }
    }

}
