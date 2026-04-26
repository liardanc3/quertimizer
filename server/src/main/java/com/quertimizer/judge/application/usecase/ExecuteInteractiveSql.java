package com.quertimizer.judge.application.usecase;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.judge.application.service.JudgeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExecuteInteractiveSql {

    private final JudgeQueryService judgeQueryService;

    public JudgeQueryService.QueryExecutionResult execute(String handle,
                                                          String socketId,
                                                          String problemId,
                                                          String sql,
                                                          DbmsType dbmsType,
                                                          Integer page,
                                                          Integer pageSize) {
        // 인터랙티브 SQL 실행을 judge 도메인 use case로 처리
        return judgeQueryService.executeInteractiveSql(handle, socketId, problemId, sql, dbmsType, page, pageSize);
    }
}
