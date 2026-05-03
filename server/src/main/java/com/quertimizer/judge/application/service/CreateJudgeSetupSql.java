package com.quertimizer.judge.application.service;

import com.quertimizer.judge.application.port.in.CreateJudgeSetupSqlUseCase;
import com.quertimizer.judge.application.input.CreateJudgeSetupSqlInput;
import com.quertimizer.judge.application.port.out.JudgeRuntimePort;
import com.quertimizer.judge.domain.entity.JudgeSetupSqlId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateJudgeSetupSql implements CreateJudgeSetupSqlUseCase {

    private final JudgeRuntimePort judgeRuntime;

    /**
     * judge 설정 SQL 묶음을 생성한다.
     *
     * @param input 설정 SQL 생성 입력
     * @return 생성된 설정 SQL ID
     */
    @Override
    public JudgeSetupSqlId execute(CreateJudgeSetupSqlInput input) {
        return judgeRuntime.createSetupSql(input);
    }
}
