package com.quertimizer.judge.application.usecase;

import com.quertimizer.judge.application.input.RefreshTemplateDatasetInput;
import com.quertimizer.judge.application.port.JudgeTemplateDatasetPort;
import com.quertimizer.judge.domain.policy.ProblemDefinitionSqlPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTemplateDataset {

    private final JudgeTemplateDatasetPort judgeTemplateDatasetPort;
    private final ProblemDefinitionSqlPolicy problemDefinitionSqlPolicy;

    /**
     * 문제셋 canonical DDL/data SQL로 template dataset을 갱신한다.
     *
     * <ol>
     *   <li>문제 정의 SQL 검증
     *   <li>template dataset 생성 또는 갱신
     * </ol>
     *
     * @param input template dataset 갱신 입력
     */
    public void execute(RefreshTemplateDatasetInput input) {
        problemDefinitionSqlPolicy.validateDdl(input.ddl());
        problemDefinitionSqlPolicy.validateActualDataSql(input.actualDataSql());

        judgeTemplateDatasetPort.refreshTemplateDataset(input);
    }
}
