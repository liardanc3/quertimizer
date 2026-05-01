package com.quertimizer.judge.application.usecase;

import com.quertimizer.judge.application.input.CreateJudgeReferenceInput;
import com.quertimizer.judge.application.output.SqlReferenceResult;
import com.quertimizer.judge.application.port.JudgeRuntime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * judge 기준 SQL을 생성한다.
 */
@Component
@RequiredArgsConstructor
public class CreateJudgeReference {

    private final JudgeRuntime judgeRuntime;

    /**
     * judge 기준 SQL을 생성한다.
     *
     * @param input 기준 SQL 생성 입력
     * @return 기준 SQL 생성 결과
     */
    public SqlReferenceResult execute(CreateJudgeReferenceInput input) {
        return judgeRuntime.createReference(input);
    }
}
