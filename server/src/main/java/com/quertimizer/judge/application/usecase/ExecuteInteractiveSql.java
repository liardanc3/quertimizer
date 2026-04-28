package com.quertimizer.judge.application.usecase;

import com.quertimizer.judge.application.input.InteractiveSqlInput;
import com.quertimizer.judge.application.service.JudgeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExecuteInteractiveSql {

    private final JudgeQueryService judgeQueryService;

    /**
     * 인터랙티브 SQL을 실행한다.
     *
     * @param input 인터랙티브 SQL 실행 입력
     */
    public JudgeQueryService.QueryExecutionResult execute(InteractiveSqlInput input) {
        return judgeQueryService.executeInteractiveSql(
                input.handle(),
                input.socketId(),
                input.problemId(),
                input.sql(),
                input.dbmsType(),
                input.page(),
                input.pageSize()
        );
    }
}
