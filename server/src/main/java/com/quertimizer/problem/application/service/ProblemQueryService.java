package com.quertimizer.problem.application.service;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.judge.application.service.JudgeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ProblemQueryService {

    private final JudgeQueryService judgeQueryService;

    public JudgeQueryService.QueryExecutionResult executeInteractiveSql(String handle,
                                                                       String socketId,
                                                                       String problemId,
                                                                       String sql,
                                                                       DbmsType dbmsType,
                                                                       Integer page,
                                                                       Integer pageSize) {
        // judge 도메인으로 이동한 인터랙티브 실행 로직을 호환 경로로 위임
        return judgeQueryService.executeInteractiveSql(handle, socketId, problemId, sql, dbmsType, page, pageSize);
    }

    public JudgeQueryService.ProblemSubmitResult submitProblemSql(String handle,
                                                                 String socketId,
                                                                 String problemId,
                                                                 String sql,
                                                                 DbmsType dbmsType,
                                                                 Consumer<JudgeQueryService.ProblemSubmitProgress> progressListener) {
        // judge 도메인으로 이동한 제출 로직을 호환 경로로 위임
        return judgeQueryService.submitProblemSql(handle, socketId, problemId, sql, dbmsType, progressListener);
    }

    public void cancelInteractiveExecution(String socketId) {
        // judge 도메인으로 이동한 실행 취소 로직을 호환 경로로 위임
        judgeQueryService.cancelInteractiveExecution(socketId);
    }
}
