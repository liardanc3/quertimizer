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

    public void execute(RefreshTemplateDatasetInput input) {
        // 문제셋 canonical DDL/data SQL로 template dataset을 생성 또는 갱신
        problemDefinitionSqlPolicy.validateDdl(input.ddl());
        problemDefinitionSqlPolicy.validateActualDataSql(input.actualDataSql());
        judgeTemplateDatasetPort.refreshTemplateDataset(input);
    }
}
