package com.quertimizer.judge.application.usecase;

import com.quertimizer.judge.application.input.SubmitProblemSqlInput;
import com.quertimizer.judge.application.service.JudgeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubmitProblemSql {

    private final JudgeQueryService judgeQueryService;

    /**
     * 문제 제출과 채점 진입점을 처리한다.
     *
     * @param input 제출 사용자, 소켓, SQL, DBMS, progress listener 입력
     */
    public JudgeQueryService.ProblemSubmitResult execute(SubmitProblemSqlInput input) {
        return judgeQueryService.submitProblemSql(
                input.getHandle(),
                input.getSocketId(),
                input.getProblemId(),
                input.getSql(),
                input.getDbmsType(),
                input.getProgressListener()
        );
    }
}
