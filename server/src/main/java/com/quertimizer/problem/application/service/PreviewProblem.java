package com.quertimizer.problem.application.service;

import com.quertimizer.problem.application.port.in.PreviewProblemUseCase;
import com.quertimizer.problem.application.input.ProblemOutputPreviewInput;
import com.quertimizer.problem.application.output.ProblemJudgeExecutionResult;
import com.quertimizer.problem.application.output.ProblemOutputPreviewOutput;
import com.quertimizer.problem.application.port.out.ProblemJudgePort;
import com.quertimizer.problem.application.port.out.ProblemOutputPreviewRateLimitPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PreviewProblem implements PreviewProblemUseCase {

    private final ProblemJudgePort problemJudgePort;
    private final ProblemOutputPreviewRateLimitPort problemOutputPreviewRateLimitPort;

    /**
     * 문제 생성 화면의 출력 예시를 생성한다.
     *
     * <ol>
     *   <li>출력 예시 실행 요청 제한 확인
     *   <li>예시 데이터셋 등록
     *   <li>기준 SQL 실행
     *   <li>예시 데이터셋 정리
     *   <li>출력 예시 결과 변환
     * </ol>
     *
     * @param input 출력 예시 생성 입력
     */
    @Override
    public ProblemOutputPreviewOutput execute(ProblemOutputPreviewInput input) {
        problemOutputPreviewRateLimitPort.validate(input.getRequester(), input.getClientIp());

        String datasetId = createDataset(input);
        ProblemJudgeExecutionResult result;
        try {
            result = executeSql(datasetId, input);
        } finally {
            deleteDatasetQuietly(datasetId);
        }

        return ProblemOutputPreviewOutput.from(result);
    }

    private String createDataset(ProblemOutputPreviewInput input) {
        // 출력 예시 SQL 자료를 judge 데이터셋으로 등록
        return problemJudgePort.createDataset(input.getDbmsType(), input.getDdl(), input.getSampleDataSql());
    }

    private void deleteDatasetQuietly(String datasetId) {
        // 출력 예시 임시 데이터셋 제거 실패 로그 기록
        try {
            problemJudgePort.deleteDataset(datasetId);
        } catch (Exception exception) {
            log.warn("출력 예시 임시 judge 데이터셋 정리 실패 datasetId={}", datasetId, exception);
        }
    }

    private ProblemJudgeExecutionResult executeSql(String datasetId, ProblemOutputPreviewInput input) {
        // 출력 예시 기준 SQL 격리 실행
        return problemJudgePort.executeIsolatedOfficialSql(
                "problem-preview-" + UUID.randomUUID(), datasetId, input.getAnswerSql()
        );
    }

}
