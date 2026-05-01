package com.quertimizer.judge.application.usecase;

import com.quertimizer.judge.application.input.CreateJudgeSetupSqlInput;
import com.quertimizer.judge.application.port.JudgeRuntime;
import com.quertimizer.judge.domain.entity.ids.JudgeSetupSqlId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * judge 설정 SQL 묶음을 생성한다.
 */
@Component
@RequiredArgsConstructor
public class CreateJudgeSetupSql {

    private final JudgeRuntime judgeRuntime;

    /**
     * judge 설정 SQL 묶음을 생성한다.
     *
     * @param input 설정 SQL 생성 입력
     * @return 생성된 설정 SQL ID
     */
    public JudgeSetupSqlId execute(CreateJudgeSetupSqlInput input) {
        return judgeRuntime.createSetupSql(input);
    }
}
