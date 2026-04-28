package com.quertimizer.judge.application.usecase;

import com.quertimizer.judge.application.input.GenerateAnswerHashInput;
import com.quertimizer.judge.application.output.ProblemOutputPreviewOutput;
import com.quertimizer.judge.application.port.JudgeExecutionOrchestratorPort;
import com.quertimizer.judge.domain.policy.ProblemDefinitionSqlPolicy;
import com.quertimizer.judge.domain.service.JudgeAnswerHashSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GenerateProblemAnswerHash {

    private final JudgeExecutionOrchestratorPort judgeExecutionOrchestratorPort;
    private final ProblemDefinitionSqlPolicy problemDefinitionSqlPolicy;

    /**
     * 실제 채점 데이터셋의 정답 결과를 canonical hash로 생성한다.
     *
     * <ol>
     *   <li>문제 정의 SQL 검증
     *   <li>정답 결과 실행 후 hash 생성
     * </ol>
     *
     * @param input 정답 hash 생성 입력
     */
    public String execute(GenerateAnswerHashInput input) {
        problemDefinitionSqlPolicy.validateDdl(input.ddl());
        problemDefinitionSqlPolicy.validateActualDataSql(input.actualDataSql());
        problemDefinitionSqlPolicy.validateAnswerSql(input.answerSql());

        ProblemOutputPreviewOutput source = judgeExecutionOrchestratorPort.executeAnswerHashSource(input);
        return JudgeAnswerHashSupport.hashResult(source.columns(), source.rows());
    }
}
