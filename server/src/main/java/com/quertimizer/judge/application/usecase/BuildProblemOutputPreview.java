package com.quertimizer.judge.application.usecase;

import com.quertimizer.judge.application.input.ProblemOutputPreviewInput;
import com.quertimizer.judge.application.output.ProblemOutputPreviewOutput;
import com.quertimizer.judge.application.port.JudgeExecutionOrchestratorPort;
import com.quertimizer.judge.domain.policy.ProblemDefinitionSqlPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BuildProblemOutputPreview {

    private final JudgeExecutionOrchestratorPort judgeExecutionOrchestratorPort;
    private final ProblemDefinitionSqlPolicy problemDefinitionSqlPolicy;

    /**
     * 문제 생성용 출력 예시를 judge 실행 환경에서 생성한다.
     *
     * <ol>
     *   <li>문제 정의 SQL 검증
     *   <li>출력 예시 실행
     * </ol>
     *
     * @param input 출력 예시 생성 입력
     */
    public ProblemOutputPreviewOutput execute(ProblemOutputPreviewInput input) {
        problemDefinitionSqlPolicy.validateDdl(input.ddl());
        problemDefinitionSqlPolicy.validateSampleDataSql(input.sampleDataSql());
        problemDefinitionSqlPolicy.validateAnswerSql(input.answerSql());

        return judgeExecutionOrchestratorPort.executeProblemOutputPreview(input);
    }
}
