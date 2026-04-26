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

    public ProblemOutputPreviewOutput execute(ProblemOutputPreviewInput input) {
        // 문제 생성용 출력 예시 preview를 judge 실행 환경에서 생성
        problemDefinitionSqlPolicy.validateDdl(input.ddl());
        problemDefinitionSqlPolicy.validateSampleDataSql(input.sampleDataSql());
        problemDefinitionSqlPolicy.validateAnswerSql(input.answerSql());
        return judgeExecutionOrchestratorPort.executeProblemOutputPreview(input);
    }
}
