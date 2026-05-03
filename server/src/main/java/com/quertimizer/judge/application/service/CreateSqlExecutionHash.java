package com.quertimizer.judge.application.service;

import com.quertimizer.judge.application.port.in.CreateSqlExecutionHashUseCase;
import com.quertimizer.judge.application.input.CreateSqlExecutionHashInput;
import com.quertimizer.judge.application.output.SqlExecutionHashResult;
import com.quertimizer.judge.application.port.out.JudgeRuntimePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateSqlExecutionHash implements CreateSqlExecutionHashUseCase {

    private final JudgeRuntimePort judgeRuntime;

    /**
     * judge SQL 실행 결과 해시를 생성한다.
     *
     * @param input SQL 실행 결과 해시 생성 입력
     * @return SQL 실행 결과 해시 생성 결과
     */
    @Override
    public SqlExecutionHashResult execute(CreateSqlExecutionHashInput input) {
        return judgeRuntime.createHash(input);
    }
}
